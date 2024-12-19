/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.pipeline.TaskFactorySettings;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * User: brendanx
 * Date: Nov 11, 2007
 */
public abstract class AbstractMS2SearchPipelineJob extends AbstractFileAnalysisJob
        implements MS2SearchJobSupport, TPPTask.JobSupport
{
    private static String DATATYPE_SAMPLES = "Samples"; // Default
    private static String DATATYPE_FRACTIONS = "Fractions";
    private static String DATATYPE_BOTH = "Both";

    public static String getRawPepXMLSuffix()
    {
        PepXMLFileType pepxft = new PepXMLFileType();
        return "_raw." + pepxft.getDefaultRole();
    }

    // useful for constructing a filename for creation, will append .gz if indicated
    public static File getPepXMLConvertFile(File dirAnalysis, String baseName, FileType.gzSupportLevel gzSupport)
    {
        FileType ft = new FileType(getRawPepXMLSuffix(),
                gzSupport);
        String name = ft.getName(dirAnalysis,baseName);
        return new File(dirAnalysis, name);
    }

    // useful for locating an existing file that may or may not be .gz
    public static File getPepXMLConvertFile(File dirAnalysis, String baseName)
    {
        return getPepXMLConvertFile(dirAnalysis,baseName, FileType.gzSupportLevel.SUPPORT_GZ);
    }

    protected final File _dirSequenceRoot;
    protected boolean _fractions;

    @JsonCreator
    protected AbstractMS2SearchPipelineJob(@JsonProperty("_dirSequenceRoot") File dirSequenceRoot)
    {
        super();
        _dirSequenceRoot = dirSequenceRoot;
    }

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchProtocol<?> protocol,
                                        String providerName,
                                        ViewBackgroundInfo info,
                                        PipeRoot root,
                                        String protocolName,
                                        Path fileParameters,
                                        List<Path> filesInput) throws IOException
    {
        super(protocol, providerName, info, root, protocolName, fileParameters, filesInput, true, false);

        _dirSequenceRoot = MS2PipelineManager.getSequenceDatabaseRoot(info.getContainer(), false);

        // Make sure a sequence file is specified.
        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase == null)
            throw new IOException("Missing required input parameter 'pipeline, database'");

        // Set the fractions attribute correctly.
        _fractions = (filesInput.size() > 1);
        if (_fractions)
        {
            String paramDataType = getParameters().get("pipeline, data type");
            if (!DATATYPE_BOTH.equalsIgnoreCase(paramDataType))
            {
                _fractions = DATATYPE_FRACTIONS.equalsIgnoreCase(paramDataType);
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

    protected void writeInputFilesToLog()
    {
        for (File file : getInputFiles())
        {
            info(file.getName());
        }
    }

    @Override
    public File findInputFile(String name)
    {
        for (File fileInput : getInputFiles())
        {
            if (name.equals(fileInput.getName()))
                return fileInput;
        }

        // Check if there's an analysis-specific copy of the file
        File analysisFile = FileUtil.appendName(getAnalysisDirectory(), name);
        if (NetworkDrive.exists(analysisFile))
        {
            return analysisFile;
        }
        // If not, check if there's a shared copy of the file in the data directory
        File dataFile = FileUtil.appendName(getDataDirectory(), name);
        if (NetworkDrive.exists(dataFile))
        {
            return dataFile;
        }
        // Fall back to the analysis-specific file even if it doesn't exist
        return analysisFile;
    }

    @Override
    public File findOutputFile(String name)
    {
        // Look through all the tasks in this pipeline
        for (TaskId taskId : getTaskPipeline().getTaskProgression())
        {
            TaskFactory<? extends TaskFactorySettings> factory = PipelineJobService.get().getTaskFactory(taskId);
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
                        return FileUtil.appendName(getDataDirectory(), name);
                    }
                }
            }
        }
        
        return FileUtil.appendName(getAnalysisDirectory(), name);
    }

    /**
     * Override to turn off PeptideProphet and ProteinProphet analysis.
     * @return true if Prophets should run.
     */
    @Override
    public boolean isProphetEnabled()
    {
        return true;
    }

    /**
     * Override to turn on RefreshParser during TPP analysis.
     * @return true if RefreshParser should run.
     */
    @Override
    public boolean isRefreshRequired()
    {
        return false;
    }

    @Override
    public List<File> getInteractInputFiles()
    {
        List<File> files = new ArrayList<>();
        for (File fileSpectra : getInputFiles())
        {
            files.add(getPepXMLConvertFile(getAnalysisDirectory(),
                    FileUtil.getBaseName(fileSpectra),
                    getGZPreference()));
        }
        return files;
    }

    @Override
    public List<File> getInteractSpectraFiles()
    {
        // Default to looking for just mzXML files
        List<FileType> types = Collections.singletonList(AbstractMS2SearchProtocol.FT_MZXML);

        for (TaskId taskId : getTaskPipeline().getTaskProgression())
        {
            TaskFactory<?> factory = PipelineJobService.get().getTaskFactory(taskId);
            // Try to find one that does an MS2 search
            if (factory instanceof AbstractMS2SearchTaskFactory)
            {
                // Use the input types for the MS2 search, which allows for things like msPrefix processed files
                types = factory.getInputTypes();
                break;
            }
        }

        ArrayList<File> files = new ArrayList<>();
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
        return files;
    }

    @Override
    public File getSearchNativeSpectraFile()
    {
        return null;    // No spectra conversion by default.
    }

    @Override
    public File getSequenceRootDirectory()
    {
        return _dirSequenceRoot;
    }

    @Override
    public File[] getSequenceFiles()
    {
        ArrayList<File> arrFiles = new ArrayList<>();

        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase != null)
        {
            String[] databases = AbstractMS2SearchProtocolFactory.splitSequenceFiles(paramDatabase);
            for (String path : databases)
                arrFiles.add(MS2PipelineManager.getSequenceDBFile(_dirSequenceRoot, path));
        }

        return arrFiles.toArray(new File[0]);
    }

    @Override
    public boolean isFractions()
    {
        return _fractions;
    }

    @Override
    public boolean isSamples()
    {
        return !_fractions || DATATYPE_BOTH.equalsIgnoreCase(getParameters().get("pipeline, data type"));
    }
}
