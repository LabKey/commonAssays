/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms2.pipeline;

import org.apache.commons.io.FileUtils;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <code>SequestSearchTask</code>
 */
public class SequestSearchTask extends PipelineJob.Task
{
    private static final String SEQUEST_PARAMS = "sequest.params";
    private static final String REMOTE_PARAMS = "remote.params";

    private static final String EXT_RAW_XML = ".xml";
    private static final String EXT_SUMMARY = ".html";
    private static final String EXT_SPECTRA_ARCHIVE = ".pep.tgz";

    public static File getNativeOutputFile(File dirData, String baseName)
    {
        return FileUtil.newFile(dirData, baseName, EXT_SUMMARY);
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

    public JobSupport getJobSupport()
    {
        return (JobSupport) getJob();
    }

    public String getStatusName()
    {
        return "SEARCH";
    }

    public boolean isComplete() throws IOException, SQLException
    {
        String baseName = getJobSupport().getOutputBasename();
        File dirAnalysis = getJobSupport().getAnalysisDirectory();

        // Either raw converted pepXML from Tandem2XML, or completely analyzed pepXML
        if (!NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) &&
                !NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName)))
            return false;

        return true;
    }

    public void run()
    {
        try
        {
            Map<String, String> params = getJobSupport().getParameters();
            params.put("list path, sequest parameters", SEQUEST_PARAMS);
            params.put("search, useremail", params.get("pipeline, email address"));
            params.put("search, username", "CPAS User");

            String baseName = getJobSupport().getOutputBasename();
            File dirAnalysis = getJobSupport().getAnalysisDirectory();
            File dirWork = MS2PipelineManager.createWorkingDirectory(dirAnalysis, baseName);

            File fileParamsLocal = new File(dirAnalysis, SEQUEST_PARAMS);
            File fileWorkParamsLocal = new File(dirWork, SEQUEST_PARAMS);
            if (!NetworkDrive.exists(fileParamsLocal))
            {
                // Never write directly to the results directory.  Always write to
                // a working directory, and rename to results, to avoid file truncation in
                // case of failure.
                writeSequestV1ParamFile(fileWorkParamsLocal, params);
                MS2PipelineManager.moveWorkToParent(fileWorkParamsLocal);
            }
            
            File fileWorkParamsRemote = new File(dirWork, REMOTE_PARAMS);
            writeSequestV2ParamFile(fileWorkParamsRemote, params);

            File dirOutputDta = new File(dirWork, baseName);
            File fileTgz = FileUtil.newFile(dirWork, baseName, EXT_SPECTRA_ARCHIVE);
            File fileSequestSummary =  getNativeOutputFile(dirWork, baseName);
            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirWork, baseName);

            /*
            0. pre-Sequest search: c) translate the mzXML file to dta for Sequest (MzXML2Search)
            */
            if (!dirOutputDta.mkdir())
                throw new IOException("Failed to create output directory for DTA files '" + dirOutputDta + "'.");

            ArrayList<String> command = new ArrayList<String>();
            command.add("MzXML2Search");
            command.add("-dta");
            command.add("-O" + dirOutputDta.getName());
            Collection<String> inputXmlParams = convertParams(new Mzxml2SearchParams().getParams(), params);
            command.addAll(inputXmlParams);
            command.add(getJobSupport().getSearchSpectraFile().getAbsolutePath());

            getJob().runSubProcess(new ProcessBuilder(command), dirWork);

            /*
            1. perform Sequest search
            */
            SequestClientImpl sequestClient = new SequestClientImpl(getJobSupport().getSequestServer(),
                    getJob().getLogger());
            String sequenceRoot = getJobSupport().getSequenceRootDirectory().getAbsolutePath() + File.separator;
            int iReturn = sequestClient.search(sequenceRoot,
                    fileWorkParamsRemote.getAbsolutePath(),
                    getJobSupport().getSearchSpectraFile().getAbsolutePath(),
                    fileSequestSummary.getAbsolutePath(),
                    inputXmlParams);

            if (iReturn != 0 || !fileSequestSummary.exists())
                throw new IOException("Failed running Sequest.");

            // TODO: Make Sequest server return pepXML using Out2XML
            FileUtils.copyFileToDirectory(fileParamsLocal, dirWork);
            getJob().runSubProcess(new ProcessBuilder("Sequest2Xml", fileSequestSummary.getName()), dirWork);

            File fileOutputPepXML = FileUtil.newFile(dirWork, baseName, EXT_RAW_XML);
            if (!fileOutputPepXML.renameTo(fileWorkPepXMLRaw))
                throw new IOException("Failed to rename " + fileOutputPepXML + " to " + fileWorkPepXMLRaw);

            getJob().runSubProcess(new ProcessBuilder("bsdtar.exe", "czf", fileTgz.getAbsolutePath(), "*"), dirOutputDta);

            if (!FileUtil.deleteDir(dirOutputDta))
                throw new IOException("Failed to delete DTA directory " + dirOutputDta.getAbsolutePath());

            MS2PipelineManager.removeWorkFile(fileWorkParamsLocal);
            MS2PipelineManager.removeWorkFile(fileWorkParamsRemote);
            MS2PipelineManager.removeWorkFile(fileSequestSummary);

            // TODO: TGZ file is only required to get spectra loaded into CPAS.  Fix to use mzXML instead.
            MS2PipelineManager.moveWorkToParent(fileTgz);
            MS2PipelineManager.moveWorkToParent(fileWorkPepXMLRaw);

            MS2PipelineManager.removeWorkingDirectory(dirWork);
        }
        catch (PipelineJob.RunProcessException e)
        {
            // Handled in runSubProcess
        }
        catch (InterruptedException e)
        {
            // Handled in runSubProcess
        }
        catch(IOException e)
        {
            getJob().error(e.getMessage(), e);
        }
        catch (SequestParamsException e)
        {
            getJob().error(e.getMessage(), e);
        }
    }

    Collection<String> convertParams(Collection<Param> converters, Map<String, String> paramsXml) throws SequestParamsException
    {
        ArrayList<String> paramsCmd = new ArrayList<String>();
        for (Param conv : converters)
        {
            String value = paramsXml.get(conv.getInputXmlLabels().get(0));
            if (value == null || value.equals(""))
                continue;
            conv.setValue(value);

            String parserError = conv.validate();
            if (!"".equals(parserError))
                throw new SequestParamsException(parserError);

            paramsCmd.add(conv.convert());
        }

        return paramsCmd;
    }

    private void writeSequestV1ParamFile(File fileParams, Map<String, String> params) throws SequestParamsException
    {
        URI uriSequenceRoot = getJobSupport().getSequenceRootDirectory().toURI();
        writeSequestParamFile(fileParams, params,
                SequestParamsBuilderFactory.createVersion1Builder(params, uriSequenceRoot));
    }

    private void writeSequestV2ParamFile(File fileParams, Map<String, String> params) throws SequestParamsException
    {
        URI uriSequenceRoot = getJobSupport().getSequenceRootDirectory().toURI();
        writeSequestParamFile(fileParams, params,
                SequestParamsBuilderFactory.createVersion2Builder(params, uriSequenceRoot));
    }

    private void writeSequestParamFile(File fileParams, Map<String, String> params, SequestParamsBuilder builder)
            throws SequestParamsException
    {
        String parseError = builder.initXmlValues();
        if (!"".equals(parseError))
            throw new SequestParamsException(parseError);

        builder.writeFile(fileParams);
    }
}
