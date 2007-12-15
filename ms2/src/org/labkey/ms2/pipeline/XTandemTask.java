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

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>XTandemTask</code> PipelineJob task that runs X! Tandem on an mzXML
 * file, and converts the native output to pepXML.
 */
public class XTandemTask extends PipelineJob.Task
{
    private static final String INPUT_XML = "input.xml";
    private static final String OUTPUT_XML = "output.xml";
    private static final String TAXONOMY_XML = "taxonomy.xml";
    private static final String TAXON_NAME = "sequences";

    private static final String EXT_XTAN_XML = ".xtan.xml";

    public static File getNativeOutputFile(File dirAnalysis, String baseName)
    {
        return FileUtil.newFile(dirAnalysis, baseName, EXT_XTAN_XML);
    }

    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
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

        // X! Tandem native output
        if (!NetworkDrive.exists(getNativeOutputFile(dirAnalysis, baseName)))
            return false;

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
            String baseName = getJobSupport().getOutputBasename();
            File dirAnalysis = getJobSupport().getAnalysisDirectory();
            File dirWork = MS2PipelineManager.createWorkingDirectory(dirAnalysis, baseName);

            File fileWorkOutputXML = getNativeOutputFile(dirWork, baseName);
            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirWork, baseName);
            File fileParameters = new File(dirWork, INPUT_XML);
            File fileTaxonomy = new File(dirWork, TAXONOMY_XML);

            writeRunParameters(fileParameters, fileTaxonomy, fileWorkOutputXML);

            getJob().runSubProcess(new ProcessBuilder("tandem.exe",
                    INPUT_XML),
                    dirWork);

            // Remove parameters files.
            MS2PipelineManager.removeWorkFile(fileParameters);
            MS2PipelineManager.removeWorkFile(fileTaxonomy);

            getJob().runSubProcess(new ProcessBuilder("Tandem2XML",
                    fileWorkOutputXML.getName(),
                    fileWorkPepXMLRaw.getName()),
                    dirWork);

            // Move final outputs to analysis directory.
            MS2PipelineManager.moveWorkToParent(fileWorkOutputXML);
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
        catch (IOException e)
        {
            getJob().error(e.getMessage(), e);
        }
    }

    public void writeRunParameters(File fileParameters, File fileTaxonomy, File fileWorkOutputXML) throws IOException
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
        params.put("spectrum, path", getJobSupport().getSearchSpectraFile().getAbsolutePath());
        params.put("output, path", fileWorkOutputXML.getName());
        params.put("output, path hashing", "no");        

        // Default parameters are just written into this parameters file, so don't need to
        // specify them again.
        params.remove("list path, default parameters");

        // CONSIDER: If we remove these, they will not end up in the pepXML file.
        for (String key : params.keySet().toArray(new String[params.size()]))
        {
            if (key.startsWith("pipeline"))
                params.remove(key);
        }

        try
        {
            BioMLInputParser.writeFromMap(params, fileParameters);
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
