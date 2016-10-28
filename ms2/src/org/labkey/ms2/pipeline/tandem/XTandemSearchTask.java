/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.AbstractMS2SearchTaskFactory;
import org.labkey.ms2.pipeline.MS2SearchJobSupport;
import org.labkey.ms2.pipeline.TPPTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>XTandemSearchTask</code> PipelineJob task that runs X! Tandem on an mzXML
 * file, and converts the native output to pepXML.
 */
public class XTandemSearchTask extends AbstractMS2SearchTask<XTandemSearchTask.Factory>
{
    private static final FileType FILE_TYPE_INPUT_XML = new FileType(".input.xml");

    private static final String INPUT_XML = "input.xml";
    private static final String TAXONOMY_XML = "taxonomy.xml";
    private static final String TAXON_NAME = "sequences";

    private static final String X_TANDEM_ACTION_NAME = "X!Tandem";
    public static final String TANDEM2_XML_ACTION_NAME = "Tandem2XML";
    
    // TPP's xtandem build treats xml.gz as a native format
    public static FileType getNativeFileType(FileType.gzSupportLevel gzSupport)
    {
        return new FileType(".xtan.xml", gzSupport);
    }
    // useful for naming an output file while honoring config preference for gzip output
    public static File getNativeOutputFile(File dirAnalysis, String baseName,
                                           FileType.gzSupportLevel gzSupport)
    {
        return getNativeFileType(gzSupport).newFile(dirAnalysis, baseName);
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

        public boolean isJobComplete(PipelineJob job)
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // X! Tandem native output
            if (!NetworkDrive.exists(getNativeOutputFile(dirAnalysis, baseName, FileType.gzSupportLevel.SUPPORT_GZ)))
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

    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            JobSupport support = getJobSupport();
            String baseName = support.getBaseName();

            // Avoid re-running an X! Tandem search, if the .xtan.xml already exists.
            // Several labs soft-link or copy .xtan.xml files to reduce processing time.
            ProcessBuilder xTandemPB = null;
            File fileOutputXML = getNativeFileType(support.getGZPreference()).newFile(support.getAnalysisDirectory(), baseName);
            File fileWorkOutputXML = null;
            File fileJobTandemXML = null;
            boolean searchComplete = NetworkDrive.exists(fileOutputXML);

            File fileMzXML = _factory.findInputFile(getJobSupport());
            File fileInputSpectra;
            try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
            {
                fileInputSpectra = _wd.inputFile(fileMzXML, false);
                if (searchComplete)
                    fileWorkOutputXML = _wd.inputFile(fileOutputXML, false);
            }

            if (!searchComplete)
            {
                fileWorkOutputXML = _wd.newFile(getNativeFileType(support.getGZPreference()));
                File fileWorkParameters = _wd.newFile(INPUT_XML);
                File fileWorkTaxonomy = _wd.newFile(TAXONOMY_XML);

                // CONSIDER: If the file stays in its original location, the absolute path
                //           is used, to ensure the loader can find it.  Better way?
                String pathSpectra;
                if (fileInputSpectra.equals(fileMzXML))
                    pathSpectra = fileInputSpectra.getAbsolutePath();
                else
                    pathSpectra = _wd.getRelativePath(fileInputSpectra);

                writeRunParameters(pathSpectra, fileWorkParameters, fileWorkTaxonomy, fileWorkOutputXML);

                String ver = getJob().getParameters().get("pipeline tandem, version");
                String exePath;
                try
                {
                    exePath = PipelineJobService.get().getExecutablePath("tandem.exe", null, "xtandem", ver, getJob().getLogger());
                }
                catch (FileNotFoundException e)
                {
                    // Issue 18203 - XTandem binary no longer called "tandem.exe" in more recent TPP non-Windows builds
                    // Try it again without the file extension
                    exePath = PipelineJobService.get().getExecutablePath("tandem", null, "xtandem", ver, getJob().getLogger());
                }
                xTandemPB = new ProcessBuilder(exePath, INPUT_XML);

                getJob().runSubProcess(xTandemPB, _wd.getDir());

                // Keep the merged parameters file
                fileJobTandemXML = _wd.outputFile(fileWorkParameters, FILE_TYPE_INPUT_XML.getDefaultName(baseName));

                // Remove parameters files.
                _wd.discardFile(fileWorkParameters);
                _wd.discardFile(fileWorkTaxonomy);
            }

            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(), baseName, support.getGZPreference());

            String ver = TPPTask.getTPPVersion(getJob());
            String exePath = PipelineJobService.get().getExecutablePath("Tandem2XML", null, "tpp", ver, getJob().getLogger());
            ProcessBuilder tandem2XmlPB = new ProcessBuilder(exePath,
                _wd.getRelativePath(fileWorkOutputXML),
                fileWorkPepXMLRaw.getName());
            getJob().runSubProcess(tandem2XmlPB,
                    _wd.getDir());

            // Move final outputs to analysis directory.
            File filePepXMLRaw;
            try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
            {
                if (!searchComplete)
                    fileOutputXML = _wd.outputFile(fileWorkOutputXML);
                filePepXMLRaw = _wd.outputFile(fileWorkPepXMLRaw);
            }

            List<RecordedAction> actions = new ArrayList<>();
            if (!searchComplete)
            {
                RecordedAction xtandemAction = new RecordedAction(X_TANDEM_ACTION_NAME);
                xtandemAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(xTandemPB.command(), ' '));
                xtandemAction.addInput(fileMzXML, SPECTRA_INPUT_ROLE);
                for (File sequenceFile : getJobSupport().getSequenceFiles())
                {
                    xtandemAction.addInput(sequenceFile, FASTA_INPUT_ROLE);
                }
                xtandemAction.addOutput(fileOutputXML, "TandemXML", false);
                xtandemAction.addInput(fileJobTandemXML, JOB_ANALYSIS_PARAMETERS_ROLE_NAME);
                actions.add(xtandemAction);
            }

            RecordedAction tandem2XmlAction = new RecordedAction(TANDEM2_XML_ACTION_NAME);
            tandem2XmlAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(tandem2XmlPB.command(), ' '));
            tandem2XmlAction.addInput(fileOutputXML, "TandemXML");
            tandem2XmlAction.addOutput(filePepXMLRaw, "RawPepXML", true);
            actions.add(tandem2XmlAction);

            return new RecordedActionSet(actions);
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    public void writeRunParameters(String pathSpectra, File fileParameters, File fileTaxonomy, File fileWorkOutputXML) throws IOException
    {
        Map<String, String> params = new HashMap<>(getJobSupport().getParameters());
        
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
        //  ... which is a bad thing, since we currently rely on "pipeline, import spectra"
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
        StringBuilder taxonomyBuffer = new StringBuilder();
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

        try (BufferedWriter taxonomyWriter = new BufferedWriter(new FileWriter(fileTaxonomy)))
        {
            String[] lines = taxonomyText.split("\n");
            for (String line : lines)
            {
                taxonomyWriter.write(line);
                taxonomyWriter.newLine();
            }
        }
    }
}
