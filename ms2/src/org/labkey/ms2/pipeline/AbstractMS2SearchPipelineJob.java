/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

import org.labkey.api.exp.pipeline.XarTemplateSubstitutionId;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brendanx
 * Date: Nov 11, 2007
 */
public abstract class AbstractMS2SearchPipelineJob extends AbstractFileAnalysisJob
        implements MS2SearchJobSupport, TPPTask.JobSupport, XarTemplateSubstitutionId.JobSupport
{
    private static String DATATYPE_SAMPLES = "Samples"; // Default
    private static String DATATYPE_FRACTIONS = "Fractions";
    private static String DATATYPE_BOTH = "Both";

    public static final String RAW_PEP_XML_SUFFIX = "_raw.pep.xml";

    public static File getPepXMLConvertFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + RAW_PEP_XML_SUFFIX);
    }

    protected File _dirSequenceRoot;
    protected boolean _fractions;

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchProtocol protocol,
                                        String providerName,
                                        ViewBackgroundInfo info,
                                        String protocolName,
                                        File dirSequenceRoot,
                                        File fileParameters,
                                        File filesInput[]
    ) throws SQLException, IOException
    {
        super(protocol, providerName, info, protocolName, fileParameters, filesInput);

        _dirSequenceRoot = dirSequenceRoot;

        // Make sure a sequence file is specified.
        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase == null)
            throw new IOException("Missing required input parameter 'pipeline, database'");

        // Set the fractions attribute correctly.
        _fractions = (filesInput.length > 1);
        if (_fractions)
        {
            String paramDataType = getParameters().get("pipeline, data type");
            if (!DATATYPE_BOTH.equalsIgnoreCase(paramDataType))
            {
                if (!NetworkDrive.exists(MS2PipelineManager.getAnnotationFile(getDataDirectory())) &&
                        !NetworkDrive.exists(MS2PipelineManager.getLegacyAnnotationFile(getDataDirectory())))
                {
                    _fractions = DATATYPE_FRACTIONS.equalsIgnoreCase(paramDataType);
                }
                else
                {
                    _fractions = !DATATYPE_SAMPLES.equalsIgnoreCase(paramDataType);
                }
            }
        }
    }

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);

        // Copy some parameters from the parent job.
        _dirSequenceRoot = job._dirSequenceRoot;
        _fractions = job._fractions;
    }

    public File findInputFile(String name)
    {
        for (File fileInput : getInputFiles())
        {
            if (name.equals(fileInput.getName()))
                return fileInput;
        }

        // Check if there's an analysis-specific copy of the file
        File analysisFile = new File(getAnalysisDirectory(), name);
        if (NetworkDrive.exists(analysisFile))
        {
            return analysisFile;
        }
        // If not, check if there's a shared copy of the file in the data directory
        File dataFile = new File(getDataDirectory(), name);
        if (NetworkDrive.exists(dataFile))
        {
            return dataFile;
        }
        // Fall back to the analysis-specific file even if it doesn't exist
        return analysisFile;
    }

    public File findOutputFile(String name)
    {
        // Look through all of the tasks in this pipeline
        for (TaskId taskId : getTaskPipeline().getTaskProgression())
        {
            TaskFactory factory = PipelineJobService.get().getTaskFactory(taskId);
            // Try to find one that does an MS2 search
            if (factory instanceof AbstractMS2SearchTaskFactory)
            {
                for (FileType fileType : factory.getInputTypes())
                {
                    // If this file is an input to the search (usually .mzXML) it should go in the data directory,
                    // not the analysis directory. This supports scenarios like msPrefix, where the rewritten
                    // mzXML should be in the same directory as the mzXML and RAW files.
                    if (fileType.isType(name))
                    {
                        return new File(getDataDirectory(), name);
                    }
                }
            }
        }
        
        return new File(getAnalysisDirectory(), name);
    }

    abstract public String getSearchEngine();

    /**
     * Override to turn off PeptideProphet and ProteinProphet analysis.
     * @return true if Prophets should run.
     */
    public boolean isProphetEnabled()
    {
        return true;
    }

    /**
     * Override to turn on RefreshParser during TPP analysis.
     * @return true if RefreshParser should run.
     */
    public boolean isRefreshRequired()
    {
        return false;
    }

    public File[] getInteractInputFiles()
    {
        ArrayList<File> files = new ArrayList<File>();
        for (File fileSpectra : getInputFiles())
        {
            files.add(getPepXMLConvertFile(getAnalysisDirectory(),
                    FileUtil.getBaseName(fileSpectra)));
        }
        return files.toArray(new File[files.size()]);
    }

    public File[] getInteractSpectraFiles()
    {
        // Default to looking for just mzXML files
        FileType[] types = new FileType[] { AbstractMS2SearchProtocol.FT_MZXML };

        for (TaskId taskId : getTaskPipeline().getTaskProgression())
        {
            TaskFactory factory = PipelineJobService.get().getTaskFactory(taskId);
            // Try to find one that does an MS2 search
            if (factory instanceof AbstractMS2SearchTaskFactory)
            {
                // Use the input types for the MS2 search, which allows for things like msPrefix processed files
                types = factory.getInputTypes();
                break;
            }
        }

        ArrayList<File> files = new ArrayList<File>();
        for (File fileSpectra : getInputFiles())
        {
            // Look at the different types in priority order
            for (FileType type : types)
            {
                File f = type.newFile(getDataDirectory(), FileUtil.getBaseName(fileSpectra));
                if (NetworkDrive.exists(f))
                {
                    files.add(f);
                    // Once we found a match, don't try to add any other versions of this file name
                    break;
                }
            }
        }
        return files.toArray(new File[files.size()]);
    }

    public File getSearchNativeSpectraFile()
    {
        return null;    // No spectra conversion by default.
    }

    public File getSequenceRootDirectory()
    {
        return _dirSequenceRoot;
    }

    public File[] getSequenceFiles()
    {
        ArrayList<File> arrFiles = new ArrayList<File>();

        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase != null)
        {
            String[] databases = paramDatabase.split(";");
            for (String path : databases)
                arrFiles.add(MS2PipelineManager.getSequenceDBFile(_dirSequenceRoot.toURI(), path));
        }

        return arrFiles.toArray(new File[arrFiles.size()]);
    }

    public boolean isXPressQuantitation()
    {
        return "xpress".equalsIgnoreCase(getParameters().get("pipeline quantitation, algorithm"));
    }

    public boolean isFractions()
    {
        return _fractions;
    }

    public boolean isSamples()
    {
        return !_fractions || DATATYPE_BOTH.equalsIgnoreCase(getParameters().get("pipeline, data type"));
    }

/////////////////////////////////////////////////////////////////////////////
//  Experiment writing
    
    public Map<String, String> getXarTemplateReplacements() throws IOException
    {
        Map<String, String> replaceMap = new HashMap<String, String>();

        File dirRoot = getRootDir();
        File dirAnalysis = getAnalysisDirectory();
        File fileParameters = getParametersFile();
        String baseName = getBaseName();

        replaceMap.put("SEARCH_NAME", getDescription());

        File[] databaseFiles = getSequenceFiles();
        StringBuilder databaseSB = new StringBuilder();
        for (File fileDatabase : databaseFiles)
            databaseSB.append(getStartingInputDataSnippet(fileDatabase));

        replaceMap.put("PROTEIN_DATABASES", databaseSB.toString());
        replaceMap.put("PROTEIN_DATABASE_DATALSIDS", getDataLSIDSnippet(databaseFiles, dirAnalysis, "FASTA"));

        StringBuilder mzxmlStartingInputsSB = new StringBuilder();
        StringBuilder instanceDetailsSB = new StringBuilder();

        File[] spectraFiles = getInputFiles();
        for (File fileSpectra : spectraFiles)
        {
            mzxmlStartingInputsSB.append(getStartingInputDataSnippet(fileSpectra));
            instanceDetailsSB.append(getInstanceDetailsSnippet(fileSpectra));
        }

        replaceMap.put("INSTANCE_DETAILS", instanceDetailsSB.toString());
        replaceMap.put("MZXML_DATALSIDS", getDataLSIDSnippet(spectraFiles, dirAnalysis, "mzXML"));
        replaceMap.put("MZXML_STARTING_INPUTS", mzxmlStartingInputsSB.toString());
        replaceMap.put("MZXML_PATHS", getSpectraFilePaths(dirAnalysis, spectraFiles));
        replaceMap.put("INPUT_XML_FILE_PATH", getXarPath(fileParameters));
        if (spectraFiles.length == 1)
        {
            File f = getSearchNativeSpectraFile();
            if (f != null)
                replaceMap.put("SPECTRA_CONVERT_FILE_PATH",  getXarPath(f));

            f = getSearchNativeOutputFile();
            if (f != null)
                replaceMap.put("SEARCH_OUTPUT_FILE_PATH", getXarPath(f));
        }

        replaceMap.put("PEP_XML_FILE_PATH",
                TPPTask.getPepXMLFile(getAnalysisDirectory(), getBaseName()).getName());

        File fileProtXml = TPPTask.getProtXMLFile(dirAnalysis, baseName);
        if (!NetworkDrive.exists(fileProtXml))
        {
            File fileProtXMLInt = TPPTask.getProtXMLIntermediateFile(dirAnalysis, baseName);
            if (NetworkDrive.exists(fileProtXMLInt))
                fileProtXml = fileProtXMLInt;
        }
        replaceMap.put("PEP_PROT_XML_FILE_PATH", fileProtXml.getName());
        replaceMap.put("RUN-UNIQUIFIER", getExperimentRunUniquifier());
        return replaceMap;
    }

    protected String getExtraDataSnippets() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (File seqFile : getSequenceFiles())
            sb.append(getAutoFileLSID(seqFile));
        return sb.toString();
    }

    protected String getDataLSIDSnippet(File[] files, File analysisDir, String baseRoleName) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (File file : files)
        {
            sb.append("                                <exp:DataLSID DataFileUrl=\"");
            sb.append(FileUtil.relativizeUnix(analysisDir, file, true));
            sb.append("\" RoleName=\"");
            sb.append(baseRoleName);
            sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        }
        return sb.toString();
    }

    protected String getSpectraFilePaths(File analysisDir, File[] jobSpectraFiles)
            throws IOException
    {
        // CONSIDER(brendanx): look at extension of passed in spectra files to one day support mzData.
        StringBuilder result = new StringBuilder();
        File dirData = getDataDirectory();
        File[] allSpectraFiles = dirData.listFiles(new FileFilter()
        {
            public boolean accept(File f)
            {
                return AbstractMS2SearchProtocol.FT_MZXML.isType(f);
            }
        });
        Set<File> fileSet1 = new HashSet<File>(Arrays.asList(allSpectraFiles));
        Set<File> fileSet2 = new HashSet<File>(Arrays.asList(jobSpectraFiles));
        if (fileSet1.equals(fileSet2))
        {
            result.append(FileUtil.relativizeUnix(analysisDir, dirData, true));
            result.append("/*.mzxml");
        }
        else
        {
            for (File f : jobSpectraFiles)
            {
                if (result.length() > 0)
                {
                    result.append(";");
                }
                result.append(FileUtil.relativizeUnix(analysisDir, f, true));
            }
        }

        return result.toString();
    }
}
