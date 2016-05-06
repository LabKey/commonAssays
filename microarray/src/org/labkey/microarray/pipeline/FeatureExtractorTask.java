/*
 * Copyright (c) 2010-2016 LabKey Corporation
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
package org.labkey.microarray.pipeline;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.pipeline.cmd.TaskPath;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.util.ConfigurationException;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.microarray.MicroarrayModule;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * User: jeckels
 * Date: Dec 28, 2010
 */
public class FeatureExtractorTask extends WorkDirectoryTask<FeatureExtractorTask.Factory>
{
    private static final String ACTION_NAME = "Feature Extraction";

    public FeatureExtractorTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public AbstractFileAnalysisJob getJob()
    {
        return (AbstractFileAnalysisJob)super.getJob();
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            _factory.validate();
        }
        catch (ConfigurationException e)
        {
            throw new PipelineJobException(e);
        }

        RecordedAction action = new RecordedAction(ACTION_NAME);

        try
        {
            File protocolFile = _wd.newFile("FeatureExtraction-" + DateUtil.formatDateTime(new Date(), "yyyy-MM-dd-HH-mm") + ".fep");

            for (File file : getJob().getInputFiles())
            {
                if (!NetworkDrive.exists(file) || !file.isFile())
                {
                    throw new PipelineJobException("Could not find file: " + file);
                }
            }

            ProtocolFileBuilder builder = new ProtocolFileBuilder();
            builder.build(protocolFile, _factory, getJob().getInputFiles());

            List<String> args = new ArrayList<>();
            args.add(_factory.getExecutable());
            args.add(protocolFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(args);

            getJob().runSubProcess(pb, _wd.getDir());

            File archiveDir = null;
            if (_factory.getArchiveDir() != null)
            {
                archiveDir = new File(_factory.getArchiveDir());
                if (!NetworkDrive.exists(archiveDir))
                {
                    throw new PipelineJobException("Archive directory '" + _factory.getArchiveDir() + "' does not exist");
                }
            }

            for (File inputFile : getJob().getInputFiles())
            {
                if (archiveDir != null)
                {
                    // Copy the input image file to the archive directory
                    FileUtils.copyFileToDirectory(inputFile, archiveDir);
                }
                action.addInput(inputFile, "Image");
            }

            if (archiveDir != null)
            {
                // Move all .shp files to the archive directory
                File[] shpFiles = _wd.getDir().listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File dir, String name)
                    {
                        return name.toLowerCase().endsWith(".shp");
                    }
                });
                for (File shpFile : shpFiles)
                {
                    // Delete existing file if present
                    FileUtils.deleteQuietly(new File(archiveDir, shpFile.getName()));
                    FileUtils.moveFileToDirectory(shpFile, archiveDir, false);
                }
            }

            _wd.acceptFilesAsOutputs(Collections.emptyMap(), action);
            return new RecordedActionSet(action);
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        private String _archiveDir;
        private String _executable;
        private String _jdbcURL;
        private String _jdbcUser;
        private String _jdbcPassword;

        public Factory()
        {
            super(FeatureExtractorTask.class);
            setJoin(true);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new FeatureExtractorTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            return Collections.singletonList(MicroarrayModule.TIFF_INPUT_TYPE.getFileType());
        }

        public String getStatusName()
        {
            return "FEATURE EXTRACTION";
        }

        public boolean isJobComplete(PipelineJob job)
        {
            FileAnalysisJobSupport support = job.getJobSupport(FileAnalysisJobSupport.class);
            File dirAnalysis = support.getAnalysisDirectory();
            // Make sure the drive is mounted
            NetworkDrive.exists(dirAnalysis);

            for (File inputFile : support.getInputFiles())
            {
                String baseName = MicroarrayModule.TIFF_INPUT_TYPE.getFileType().getBaseName(inputFile);
                // The exact name of the MageML depends on the protocol name in the FeatureExtractor database, so look
                // for a prefix and suffix match
                File[] existingFiles = dirAnalysis.listFiles();
                boolean foundMageML = false;
                if (existingFiles != null)
                {
                    for (File existingFile : existingFiles)
                    {
                        if (existingFile.getName().startsWith(baseName) && MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType().isType(existingFile))
                        {
                            foundMageML = true;
                        }
                    }
                }
                if (!foundMageML)
                {
                    return false;
                }
            }
            return true;
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public String getGroupParameterName()
        {
            return "featureextraction";
        }

        public String getArchiveDir()
        {
            return _archiveDir;
        }

        public void setArchiveDir(String archiveDir)
        {
            _archiveDir = archiveDir;
        }

        public String getExecutable()
        {
            return _executable;
        }

        public void setExecutable(String executable)
        {
            _executable = executable;
        }

        public void setJdbcURL(String jdbcURL)
        {
            _jdbcURL = jdbcURL;
        }

        public String getJdbcURL()
        {
            return _jdbcURL;
        }

        public void setJdbcUser(String jdbcUser)
        {
            _jdbcUser = jdbcUser;
        }

        public String getJdbcUser()
        {
            return _jdbcUser;
        }

        public void setJdbcPassword(String jdbcPassword)
        {
            _jdbcPassword = jdbcPassword;
        }

        public String getJdbcPassword()
        {
            return _jdbcPassword;
        }

        public void validate() throws ConfigurationException
        {
            List<String> missingProperties = new ArrayList<>();
            if (_jdbcURL == null)
            {
                missingProperties.add("jdbcURL");
            }
            if (_jdbcUser == null)
            {
                missingProperties.add("jdbcUser");
            }
            if (_jdbcPassword == null)
            {
                missingProperties.add("jdbcPassword");
            }
            if (_executable == null)
            {
                missingProperties.add("executable");
            }

            if (!missingProperties.isEmpty())
            {
                throw new ConfigurationException("Missing required properties on " + getClass().getName() + ": " + StringUtils.join(missingProperties, ","));
            }
        }
    }
}
