/*
 * Copyright (c) 2007-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.ms2.pipeline.sequest;

import org.apache.commons.io.FileUtils;
import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * <code>SequestSearchTask</code>
 */
public class SequestSearchTask extends AbstractMS2SearchTask<SequestSearchTask.Factory>
{
    private static final String SEQUEST_PARAMS = "sequest.params";

    private static final String ACTION_NAME = "Sequest Search";

    // useful for creating an output filename that honors config preference for gzipped output
    public static File getNativeOutputFile(File dirAnalysis, String baseName,
                                           FileType.gzSupportLevel gzSupport)
    {
        return AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis,baseName,gzSupport);
    }

    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        private String _executable;
        private List<String> _sequestOptions = new ArrayList<String>();

        public Factory()
        {
            super(SequestSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new SequestSearchTask(this, job);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            SequestPipelineJob support = (SequestPipelineJob) job;
            String baseName = support.getBaseName();
            String baseNameJoined = support.getJoinedBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // Fraction roll-up, completely analyzed sample pepXML, or the raw pepXML exist
            return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseNameJoined)) ||
                   NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
                   NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
        }


        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public String getGroupParameterName()
        {
            return "sequest";
        }

        public String getExecutable()
        {
            return _executable;
        }

        public void setExecutable(String executable)
        {
            _executable = executable;
        }

        public List<String> getSequestOptions()
        {
            return _sequestOptions;
        }

        public void setSequestOptions(List<String> sequestOptions)
        {
            _sequestOptions = sequestOptions;
        }
    }

    protected SequestSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public SequestPipelineJob getJobSupport()
    {
        return (SequestPipelineJob)getJob();
    }

    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            Map<String, String> params = getJobSupport().getParameters();
            params.put("list path, sequest parameters", SEQUEST_PARAMS);
            params.put("search, useremail", params.get("pipeline, email address"));
            params.put("search, username", "CPAS User");

            RecordedAction action = new RecordedAction(ACTION_NAME);

            File dirOutputDta = new File(_wd.getDir(), getJobSupport().getBaseName());
            File fileMzXML = _factory.findInputFile(getJobSupport().getDataDirectory(), getJobSupport().getBaseName());
            String tppVersion = TPPTask.getTPPVersion(getJob());

            // Translate the mzXML file to dta using MzXML2Search
            convertToDTA(params, dirOutputDta, fileMzXML, tppVersion);
            File dtaListFile = writeDtaList(dirOutputDta);

            // Write out sequest.params file
            File fileWorkParams = _wd.newFile(SEQUEST_PARAMS);
            writeSequestV2ParamFile(fileWorkParams, params);
            // Have a copy in both the work directory to retain with the results, and in the dta subdirectory for
            // Sequest to use
            FileUtils.copyFileToDirectory(fileWorkParams, dirOutputDta);

            // Perform Sequest search
            List<String> sequestArgs = new ArrayList<String>();
            sequestArgs.add(_factory.getExecutable());
            sequestArgs.addAll(_factory.getSequestOptions());
            sequestArgs.add("-R" + dtaListFile.getAbsolutePath());
            sequestArgs.add("-F" + dirOutputDta.getAbsolutePath());
            // Trailing argument that makes Sequest not barf
            sequestArgs.add("x");
            ProcessBuilder sequestPB = new ProcessBuilder(sequestArgs);
            getJob().runSubProcess(sequestPB, dirOutputDta);

            // Convert to pepXML using out2xml
            List<String> out2XMLArgs = new ArrayList<String>();
            out2XMLArgs.add(PipelineJobService.get().getExecutablePath("out2xml", "tpp", tppVersion, getJob().getLogger()));
            String enzyme =
                new SequestParamsV2Builder(params, null).getSupportedEnzyme(params.get("protein, cleavage site"));
            Out2XmlParams out2XmlParams = new Out2XmlParams();
            out2XMLArgs.add(dirOutputDta.getName());
            out2XMLArgs.add("1");
            out2XMLArgs.add("-all");
            out2XmlParams.getParam("-E").setValue(enzyme);
            out2XMLArgs.addAll(convertParams(out2XmlParams.getParams(), params));
            ProcessBuilder out2XMLPB = new ProcessBuilder(out2XMLArgs);
            getJob().runSubProcess(out2XMLPB, _wd.getDir());

            File pepXmlFile = TPPTask.getPepXMLFile(_wd.getDir(), getJobSupport().getBaseName());
            if (!pepXmlFile.exists())
                throw new IOException("Failed running out2xml or Sequest - could not find expected file: " + pepXmlFile);

            if (!FileUtil.deleteDir(dirOutputDta))
                throw new IOException("Failed to delete DTA directory " + dirOutputDta.getAbsolutePath());

            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(),
                    getJobSupport().getBaseName(),
                    getJobSupport().getGZPreference());

            if (!pepXmlFile.renameTo(fileWorkPepXMLRaw))
            {
                throw new PipelineJobException("Failed to rename " + pepXmlFile + " to " + fileWorkPepXMLRaw);
            }

            // TODO: TGZ file is only required to get spectra loaded into CPAS.  Fix to use mzXML instead.
            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = _wd.ensureCopyingLock();
                action.addOutput(_wd.outputFile(fileWorkParams), "SequestParams", true);
                action.addOutput(_wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }

            for (File file : getJobSupport().getSequenceFiles())
            {
                action.addInput(file, FASTA_INPUT_ROLE);
            }
            action.addInput(fileMzXML, SPECTRA_INPUT_ROLE);
            
            return new RecordedActionSet(action);
        }
        catch(IOException e)
        {
            throw new PipelineJobException(e);
        }
        catch (SequestParamsException e)
        {
            throw new PipelineJobException(e);
        }
    }

    private void convertToDTA(Map<String, String> params, File dirOutputDta, File fileMzXML, String tppVersion)
            throws IOException, SequestParamsException, PipelineJobException
    {
        if (!dirOutputDta.mkdir())
            throw new IOException("Failed to create output directory for DTA files '" + dirOutputDta + "'.");
        ArrayList<String> mzXML2SearchArgs = new ArrayList<String>();
        mzXML2SearchArgs.add(PipelineJobService.get().getExecutablePath("MzXML2Search", "tpp", tppVersion, getJob().getLogger()));
        mzXML2SearchArgs.add("-dta");
        mzXML2SearchArgs.add("-O" + dirOutputDta.getName());
        Mzxml2SearchParams mzXml2SearchParams = new Mzxml2SearchParams();
        Collection<String> inputXmlParams = convertParams(mzXml2SearchParams.getParams(), params);
        mzXML2SearchArgs.addAll(inputXmlParams);
        mzXML2SearchArgs.add(fileMzXML.getAbsolutePath());

        getJob().runSubProcess(new ProcessBuilder(mzXML2SearchArgs), _wd.getDir());
    }

    private File writeDtaList(File dirOutputDta) throws IOException
    {
        File[] dtaFiles = dirOutputDta.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".dta");
            }
        });
        File result = new File(dirOutputDta, "DtaFiles.txt");
        OutputStream out = null;
        try
        {
            out = new FileOutputStream(result);
            PrintWriter writer = new PrintWriter(out);
            for (File dtaFile : dtaFiles)
            {
                writer.println(dtaFile.getName());
            }
        }
        finally
        {
            if (out != null) { try { out.close(); } catch (IOException e) {} }
        }
        return result;
    }

    Collection<String> convertParams(Collection<Param> converters, Map<String, String> paramsXml) throws SequestParamsException
    {
        ArrayList<String> paramsCmd = new ArrayList<String>();
        for (Param conv : converters)
        {
            String value = paramsXml.get(conv.getInputXmlLabels().get(0));
            if (value == null || value.equals(""))
            {
                if(conv.getValue() == null || conv.getValue().equals(""))
                    continue;
            }
            else
            {
                conv.setValue(value);
            }

            String parserError = conv.validate();
            if (!"".equals(parserError))
                throw new SequestParamsException(parserError);
            if(!conv.convert().equals(""))
                paramsCmd.add(conv.convert());
        }

        return paramsCmd;
    }

    private void writeSequestV2ParamFile(File fileParams, Map<String, String> params) throws SequestParamsException
    {
        File sequenceRoot = getJobSupport().getSequenceRootDirectory();
        SequestParamsBuilder builder = new SequestParamsV2Builder(params, sequenceRoot);
        String parseError = builder.initXmlValues();
        if (!"".equals(parseError))
            throw new SequestParamsException(parseError);

        builder.writeFile(fileParams);
    }

}
