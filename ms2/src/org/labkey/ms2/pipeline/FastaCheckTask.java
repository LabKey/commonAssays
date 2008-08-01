/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.common.tools.FastaValidator;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Collections;

/**
 * <code>FastaCheckTask</code>
 */
public class FastaCheckTask extends PipelineJob.Task<FastaCheckTask.Factory>
{
    private static final String ACTION_NAME = "Check FASTA";

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings>
    {
        public Factory()
        {
            super(FastaCheckTask.class);

            setJoin(true);  // Do this once per file-set.
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new FastaCheckTask(this, job);
        }

        public FileType[] getInputTypes()
        {
            // CONSIDER: Not really the input type, but the input type for the search.
            //           Should it be null or FASTA?
            return new FileType[] { AbstractMS2SearchProtocol.FT_MZXML };
        }

        public String getStatusName()
        {
            return "CHECK FASTA";
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            // No way of knowing.
            return false;
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

    public List<RecordedAction> run() throws PipelineJobException
    {
        try
        {
            getJob().header("Check FASTA validity");

            RecordedAction action = new RecordedAction(ACTION_NAME);

            for (File sequenceFile : getJobSupport().getSequenceFiles())
            {
                // todo: NetworkDrive access on PipelineJobService
                // If the file does not exist, assume something else will fail fairly quickly.
                if (!sequenceFile.exists())
                    continue;
                getJob().info("Checking sequence file validity of " + sequenceFile);

                action.addInput(sequenceFile, "FASTA");

                FastaValidator validator = new FastaValidator(sequenceFile);
                String errors = StringUtils.join(validator.validate(), "\n");
                if (errors.length() > 0)
                {
                    getJob().error(errors);
                    return Collections.emptyList();
                }
            }

            getJob().info("");  // blank line
            return Collections.singletonList(action);
        }
        catch (IOException e)
        {
            throw new PipelineJobException("Failed to check FASTA file(s)", e);
        }
    }
}
