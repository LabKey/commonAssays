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
package org.labkey.ms2.pipeline.tandem;

import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.*;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>XTandemSearchTask</code> PipelineJob task that runs X! Tandem on an mzXML
 * file, and converts the native output to pepXML.
 */
public class XTandemSearchTask extends PipelineJob.Task<XTandemSearchTask.Factory>
{
    private static final String INPUT_XML = "input.xml";
    private static final String TAXONOMY_XML = "taxonomy.xml";
    private static final String TAXON_NAME = "sequences";

    private static final String X_TANDEM_ACTION_NAME = "X!Tandem";
    private static final String TANDEM2_XML_ACTION_NAME = "Tandem2XML";
    
    private static final FileType FT_XTAN_XML = new FileType(".xtan.xml");

    public static File getNativeOutputFile(File dirAnalysis, String baseName)
    {
        return FT_XTAN_XML.newFile(dirAnalysis, baseName);
    }

    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
    }

    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        public Factory()
        {
            super(XTandemSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new XTandemSearchTask(this, job);
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // X! Tandem native output
            if (!NetworkDrive.exists(getNativeOutputFile(dirAnalysis, baseName)))
                return false;

            String baseNameJoined = support.getJoinedBaseName();

            // Fraction roll-up, completely analyzed sample pepXML, or the raw pepXML exist
            return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseNameJoined)) ||
                   NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
                   NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(X_TANDEM_ACTION_NAME, TANDEM2_XML_ACTION_NAME);
        }

        public String getGroupParameterName()
        {
            return "xtandem";
        }
    }

    protected XTandemSearchTask(Factory factory, PipelineJob job)
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
            JobSupport support = getJobSupport();
            String baseName = support.getBaseName();

            WorkDirFactory factory = PipelineJobService.get().getWorkDirFactory();
            WorkDirectory wd = factory.createWorkDirectory(getJob().getJobGUID(), support, getJob().getLogger());

            // Avoid re-running an X! Tandem search, if the .xtan.xml alreayd exists.
            // Several labs soft-link or copy .xtan.xml files to reduce processing time.
            ProcessBuilder xTandemPB = null;
            File fileOutputXML = FT_XTAN_XML.newFile(support.getAnalysisDirectory(), baseName);
            File fileWorkOutputXML = null;
            boolean searchComplete = NetworkDrive.exists(fileOutputXML);

            File fileDataSpectra = support.getSearchSpectraFile();
            File fileInputSpectra;
            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = wd.ensureCopyingLock();
                fileInputSpectra = wd.inputFile(fileDataSpectra, false);
                if (searchComplete)
                    fileWorkOutputXML = wd.inputFile(fileOutputXML, false);
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }

            if (!searchComplete)
            {
                fileWorkOutputXML = wd.newFile(FT_XTAN_XML);

                File fileWorkParameters = wd.newFile(INPUT_XML);
                File fileWorkTaxonomy = wd.newFile(TAXONOMY_XML);

                // CONSIDER: If the file stays in its original location, the absolute path
                //           is used, to ensure the loader can find it.  Better way?
                String pathSpectra;
                if (fileInputSpectra.equals(fileDataSpectra))
                    pathSpectra = fileInputSpectra.getAbsolutePath();
                else
                    pathSpectra = wd.getRelativePath(fileInputSpectra);

                writeRunParameters(pathSpectra, fileWorkParameters, fileWorkTaxonomy, fileWorkOutputXML);

                String ver = getJob().getParameters().get("pipeline, xtandem version");
                String exePath = PipelineJobService.get().getExecutablePath("tandem.exe", "xtandem", ver);
                xTandemPB = new ProcessBuilder(exePath, INPUT_XML);

                getJob().runSubProcess(xTandemPB, wd.getDir());

                // Remove parameters files.
                wd.discardFile(fileWorkParameters);
                wd.discardFile(fileWorkTaxonomy);
            }

            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(wd.getDir(), baseName);

            String ver = getJob().getParameters().get("pipeline, tpp version");
            String exePath = PipelineJobService.get().getExecutablePath("Tandem2XML", "tpp", ver);
            ProcessBuilder tandem2XmlPB = new ProcessBuilder(exePath,
                wd.getRelativePath(fileWorkOutputXML),
                fileWorkPepXMLRaw.getName());
            getJob().runSubProcess(tandem2XmlPB,
                    wd.getDir());

            // Move final outputs to analysis directory.
            File filePepXMLRaw;
            lock = null;
            try
            {
                lock = wd.ensureCopyingLock();
                if (!searchComplete)
                    fileOutputXML = wd.outputFile(fileWorkOutputXML);
                filePepXMLRaw = wd.outputFile(fileWorkPepXMLRaw);
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }
            wd.remove();

            List<RecordedAction> actions = new ArrayList<RecordedAction>();
            if (!searchComplete)
            {
                RecordedAction action1 = new RecordedAction(X_TANDEM_ACTION_NAME);
                action1.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(xTandemPB.command(), ' '));
                action1.addInput(fileDataSpectra, "mzXML");
                action1.addInput(getJobSupport().getParametersFile(), "SearchConfig");
                for (File sequenceFile : getJobSupport().getSequenceFiles())
                {
                    action1.addInput(sequenceFile, "FASTA");
                }
                action1.addOutput(fileOutputXML, "TandemXML", false);
                actions.add(action1);
            }

            RecordedAction action2 = new RecordedAction(TANDEM2_XML_ACTION_NAME);
            action2.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(tandem2XmlPB.command(), ' '));
            action2.addInput(fileOutputXML, "TandemXML");
            action2.addOutput(filePepXMLRaw, "RawPepXML", true);
            actions.add(action2);

            return actions;
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    public void writeRunParameters(String pathSpectra, File fileParameters, File fileTaxonomy, File fileWorkOutputXML) throws IOException
    {
        Map<String, String> params = new HashMap<String, String>(getJobSupport().getParameters());
        
        try
        {
            writeTaxonomy(fileTaxonomy, TAXON_NAME, getJobSupport().getSequenceFiles());
        }
        catch (IOException e)
        {
            throw new IOException("Failed to write X! Tandem taxonomy file '" + fileTaxonomy + "'.\n" +
                    e.getMessage());
        }

        params.put("list path, taxonomy information", TAXONOMY_XML);
        params.put("protein, taxon", TAXON_NAME);
        params.put("spectrum, path", pathSpectra);
        params.put("output, path", fileWorkOutputXML.getName());
        params.put("output, path hashing", "no");        

        // Default parameters are just written into this parameters file, so don't need to
        // specify them again.
        params.remove("list path, default parameters");

        // CONSIDER: If we remove these, they will not end up in the pepXML file.
        //  ... which is a bad thing, since we currently rely on "pipline, import spectra"
        //  to keep from loading all spectra into the database.
/*        for (String key : params.keySet().toArray(new String[params.size()]))
        {
            if (key.startsWith("pipeline"))
                params.remove(key);
        }
*/

        try
        {
            getJobSupport().createParamParser().writeFromMap(params, fileParameters);
        }
        catch (IOException e)
        {
            throw new IOException("Failed to write X!Tandem input file '" + fileParameters + "'.\n" +
                    e.getMessage());
        }
    }

    public void writeTaxonomy(File fileTaxonomy, String taxonName, File[] fileDatabases) throws IOException
    {
        StringBuffer taxonomyBuffer = new StringBuffer();
        taxonomyBuffer.append("<?xml version=\"1.0\"?>\n");
        taxonomyBuffer.append("<bioml label=\"x! taxon-to-file matching list\">\n");
        taxonomyBuffer.append("  <taxon label=\"").append(taxonName).append("\">\n");
        for (File fileDatabase : fileDatabases)
        {
            taxonomyBuffer.append("    <file format=\"peptide\" URL=\"");
            taxonomyBuffer.append(fileDatabase.getAbsolutePath());
            taxonomyBuffer.append("\"/>\n");
        }
        taxonomyBuffer.append("  </taxon>\n");
        taxonomyBuffer.append("</bioml>\n");
        String taxonomyText = taxonomyBuffer.toString();

        BufferedWriter taxonomyWriter = null;
        try
        {
            taxonomyWriter = new BufferedWriter(new FileWriter(fileTaxonomy));
            String[] lines = taxonomyText.split("\n");
            for (String line : lines)
            {
                taxonomyWriter.write(line);
                taxonomyWriter.newLine();
            }
        }
        finally
        {
            if (taxonomyWriter != null)
                taxonomyWriter.close();
        }
    }
}
