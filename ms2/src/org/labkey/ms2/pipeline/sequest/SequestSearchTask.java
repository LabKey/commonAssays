/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * <code>SequestSearchTask</code>
 */
public class SequestSearchTask extends AbstractMS2SearchTask<SequestSearchTask.Factory>
{
    private static final String SEQUEST_PARAMS = "sequest.params";
    private static final String REMOTE_PARAMS = "remote.params";

    private static final FileType FT_RAW_XML = new FileType("_raw.pep.xml");
    private static final FileType FT_SPECTRA_ARCHIVE = new FileType(".pep.tgz");

    private static final String ACTION_NAME = "Sequest Search";

    public static File getNativeOutputFile(File dirAnalysis, String baseName)
    {
        return FT_RAW_XML.newFile(dirAnalysis, baseName);
    }
    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
        /**
         * Returns Sequest server name.
         */
        String getSequestServer();
    }

    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        public Factory()
        {
            super(SequestSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new SequestSearchTask(this, job);
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            JobSupport support = (JobSupport) job;
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
    }

    protected SequestSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public JobSupport getJobSupport()
    {
        return getJob().getJobSupport(JobSupport.class);
    }

    public List<RecordedAction> run() throws PipelineJobException
    {
        try
        {
            Map<String, String> params = getJobSupport().getParameters();
            params.put("list path, sequest parameters", SEQUEST_PARAMS);
            params.put("search, useremail", params.get("pipeline, email address"));
            params.put("search, username", "CPAS User");

            WorkDirectory wd = _factory.createWorkDirectory(getJob().getJobGUID(), getJobSupport(), getJob().getLogger());

            RecordedAction action = new RecordedAction(ACTION_NAME);

            File fileParamsLocal = new File(getJobSupport().getAnalysisDirectory(), SEQUEST_PARAMS);
            File fileWorkParamsLocal = wd.newFile(SEQUEST_PARAMS);
            if (!NetworkDrive.exists(fileParamsLocal))
            {
                // Never write directly to the results directory.  Always write to
                // a working directory, and rename to results, to avoid file truncation in
                // case of failure.
                writeSequestV1ParamFile(fileWorkParamsLocal, params);
                wd.outputFile(fileWorkParamsLocal);
            }
            
            File fileWorkParamsRemote = wd.newFile(REMOTE_PARAMS);
            writeSequestV2ParamFile(fileWorkParamsRemote, params);

            File dirOutputDta = new File(wd.getDir(), getJobSupport().getBaseName());
            File fileWorkTgz = wd.newFile(FT_SPECTRA_ARCHIVE);
            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(wd.getDir(),
                    getJobSupport().getBaseName());

            /*
            0. pre-Sequest search: c) translate the mzXML file to dta for Sequest (MzXML2Search)
            */
            if (!dirOutputDta.mkdir())
                throw new IOException("Failed to create output directory for DTA files '" + dirOutputDta + "'.");

            ArrayList<String> command = new ArrayList<String>();
            String ver = TPPTask.getTPPVersion(getJob());
            command.add(PipelineJobService.get().getExecutablePath("MzXML2Search", "tpp", ver));
            command.add("-dta");
            command.add("-O" + dirOutputDta.getName());
            Mzxml2SearchParams mzXml2SearchParams = new Mzxml2SearchParams();
            Collection<String> inputXmlParams = convertParams(mzXml2SearchParams.getParams(), params);
            command.addAll(inputXmlParams);
            File fileMzXML = _factory.findInputFile(getJobSupport().getDataDirectory(), getJobSupport().getBaseName());
            command.add(fileMzXML.getAbsolutePath());

            getJob().runSubProcess(new ProcessBuilder(command), wd.getDir());

            String enzyme =
                SequestParamsBuilderFactory.createVersion1Builder(params, null).getSupportedEnzyme(params.get("protein, cleavage site"));
            Out2XmlParams out2XmlParams = new Out2XmlParams();
            out2XmlParams.getParam("-E").setValue(enzyme);
            inputXmlParams.addAll(convertParams(out2XmlParams.getParams(), params));

            /*
            1. perform Sequest search
            */
            SequestClientImpl sequestClient = new SequestClientImpl(getJobSupport().getSequestServer(),
                    getJob().getLogger());
            String sequenceRoot = getJobSupport().getSequenceRootDirectory().getAbsolutePath() + File.separator;
            int iReturn = sequestClient.search(sequenceRoot,
                    fileWorkParamsRemote.getAbsolutePath(),
                    fileMzXML.getAbsolutePath(),
                    fileWorkPepXMLRaw.getAbsolutePath(),
                    inputXmlParams);

            if (iReturn != 0 || !fileWorkPepXMLRaw.exists())
                throw new IOException("Failed running Sequest.");

            // TODO: This limits SequestSearchTask to running only on LabKey Server
            String exePath = PipelineJobService.get().getExecutablePath("bsdtar.exe", null, null);
            getJob().runSubProcess(new ProcessBuilder(exePath,
                    "czf", fileWorkTgz.getAbsolutePath(), "*"), dirOutputDta);

            if (!FileUtil.deleteDir(dirOutputDta))
                throw new IOException("Failed to delete DTA directory " + dirOutputDta.getAbsolutePath());

            // TODO: TGZ file is only required to get spectra loaded into CPAS.  Fix to use mzXML instead.
            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = wd.ensureCopyingLock();
                action.addOutput(wd.outputFile(fileWorkTgz), "TGZ", false);
                action.addOutput(wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }

            wd.discardFile(fileWorkParamsLocal);
            wd.discardFile(fileWorkParamsRemote);
            wd.remove();

            for (File file : getJobSupport().getSequenceFiles())
            {
                action.addInput(file, "FASTA");
            }
            action.addInput(fileMzXML, "mzXML");
            
            return Collections.singletonList(action);
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

    private void writeSequestV1ParamFile(File fileParams, Map<String, String> params) throws SequestParamsException
    {
        URI uriSequenceRoot = getJobSupport().getSequenceRootDirectory().toURI();
        writeSequestParamFile(fileParams,
            SequestParamsBuilderFactory.createVersion1Builder(params, uriSequenceRoot));
    }

    private void writeSequestV2ParamFile(File fileParams, Map<String, String> params) throws SequestParamsException
    {
        URI uriSequenceRoot = getJobSupport().getSequenceRootDirectory().toURI();
        writeSequestParamFile(fileParams,
            SequestParamsBuilderFactory.createVersion2Builder(params, uriSequenceRoot));
    }

    private void writeSequestParamFile(File fileParams, SequestParamsBuilder builder)
            throws SequestParamsException
    {
        String parseError = builder.initXmlValues();
        if (!"".equals(parseError))
            throw new SequestParamsException(parseError);

        builder.writeFile(fileParams);
    }
}
