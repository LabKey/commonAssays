/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.ms2.protein.fasta.FastaValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <code>FastaCheckTask</code>
 */
public class FastaCheckTask extends PipelineJob.Task<FastaCheckTask.Factory>
{
    private static final String ACTION_NAME = "Check FASTA";

    public static final String DECOY_DATABASE_PARAM_NAME = "pipeline, decoy database"; 

    private static List<String> DECOY_FILE_SUFFIXES = new ArrayList<>(Arrays.asList("-reverse", "-decoy", "-rev"));

    public static class Factory extends AbstractTaskFactory<FastaCheckTaskFactorySettings, Factory>
    {
        private boolean _requireDecoyDatabase;

        public Factory()
        {
            super(FastaCheckTask.class);

            setJoin(true);  // Do this once per file-set.
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new FastaCheckTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            // CONSIDER: Not really the input type, but the input type for the search.
            //           Should it be null or FASTA?
            return Collections.singletonList(AbstractMS2SearchProtocol.FT_MZXML);
        }

        @Override
        protected void configure(FastaCheckTaskFactorySettings settings)
        {
            super.configure(settings);

            if (settings.getRequireDecoyDatabase() != null)
            {
                _requireDecoyDatabase = settings.getRequireDecoyDatabase().booleanValue();
            }

            if (settings.getDecoyFileSuffixes() != null)
            {
                DECOY_FILE_SUFFIXES = settings.getDecoyFileSuffixes();
            }
        }

        public String getStatusName()
        {
            return "CHECK FASTA";
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            // No way of knowing.
            return false;
        }

        public String getGroupParameterName()
        {
            return "fasta check";
        }
    }

    protected FastaCheckTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public MS2SearchJobSupport getJobSupport()
    {
        return getJob().getJobSupport(MS2SearchJobSupport.class);
    }

    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            getJob().header("Check FASTA validity");

            RecordedAction action = new RecordedAction(ACTION_NAME);
            boolean success = true;

            FastaValidator validator = new FastaValidator();
            for (File sequenceFile : getJobSupport().getSequenceFiles())
            {
                action.addInput(sequenceFile, "FASTA");
                
                // todo: NetworkDrive access on PipelineJobService
                // If the file does not exist, assume something else will fail fairly quickly.
                if (!sequenceFile.exists())
                    continue;
                getJob().info("Checking sequence file validity of " + sequenceFile);

                success &= validateSequenceFile(validator, sequenceFile);
            }

            if (_factory._requireDecoyDatabase)
            {
                for (File decoyFile : getDecoySequenceFiles(getJobSupport()))
                {
                    getJob().info("Checking decoy file: " + decoyFile);
                    success &= validateSequenceFile(validator, decoyFile);
                }
            }

            if (!success)
            {
                throw new PipelineJobException("FASTA errors found");
            }

            getJob().info("");  // blank line
            return new RecordedActionSet(action);
        }
        // IllegalArgumentException is sometimes thrown by the checker.
        catch (IOException | IllegalArgumentException e)
        {
            throw new PipelineJobException("Failed to check FASTA file(s)", e);
        }

    }

    public static List<File> getDecoySequenceFiles(MS2SearchJobSupport job) throws IOException
    {
        List<File> result = new ArrayList<>();
        if (job.getParameters().get(DECOY_DATABASE_PARAM_NAME) != null)
        {
            String decoyPath = job.getParameters().get(DECOY_DATABASE_PARAM_NAME);
            result.add(MS2PipelineManager.getSequenceDBFile(job.getSequenceRootDirectory(), decoyPath));
        }
        else
        {
            for (File sequenceFile : job.getSequenceFiles())
            {
                String basename = FileUtil.getBaseName(sequenceFile);
                String extension = FileUtil.getExtension(sequenceFile);
                if (DECOY_FILE_SUFFIXES.isEmpty())
                {
                    throw new IllegalStateException("No decoy file suffixes configured!");
                }
                int i = 0;
                File decoyFile = new File(sequenceFile.getParentFile(), basename + DECOY_FILE_SUFFIXES.get(i++) + (extension == null ? "" : "." + extension));
                while (!decoyFile.exists() && i < DECOY_FILE_SUFFIXES.size())
                {
                    decoyFile = new File(sequenceFile.getParentFile(), basename + DECOY_FILE_SUFFIXES.get(i++) + (extension == null ? "" : "." + extension));
                }
                result.add(decoyFile);
            }
        }
        return result;
    }

    private boolean validateSequenceFile(FastaValidator validator, File fastaFile)
    {
        if (!fastaFile.exists())
        {
            getJob().error("Sequence file not found: " + fastaFile);
            return false;
        }

        String errors = StringUtils.join(validator.validate(fastaFile), "\n");
        if (errors.length() > 0)
        {
            getJob().error(errors);
            return false;
        }
        return true;
    }
}
