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

import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileType;
import org.labkey.common.tools.FastaValidator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * <code>FastaCheckTask</code>
 */
public class FastaCheckTask extends PipelineJob.Task
{
    public static class Factory extends AbstractTaskFactory
    {
        public Factory()
        {
            super(FastaCheckTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new FastaCheckTask(job);
        }

        public FileType getInputType()
        {
            // CONSIDER: Not really the input type, but the input type for the search.
            //           Should it be null or FASTA?
            return AbstractMS2SearchProtocol.FT_MZXML;
        }

        public String getStatusName()
        {
            return "CHECK FASTA";
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            // No way of knowing.
            return false;
        }
    }

    protected FastaCheckTask(PipelineJob job)
    {
        super(job);
    }

    public MS2SearchJobSupport getJobSupport()
    {
        return getJob().getJobSupport(MS2SearchJobSupport.class);
    }

    public void run()
    {
        try
        {
            getJob().header("Check FASTA validity");
            
            for (File sequenceFile : getJobSupport().getSequenceFiles())
            {
                // todo: NetworkDrive access on PipelineJobService
                // If the file does not exist, assume something else will fail fairly quickly.
                if (!sequenceFile.exists())
                    continue;
                getJob().info("Checking sequence file validity of " + sequenceFile);
                
                FastaValidator validator = new FastaValidator(sequenceFile);
                for (String error : validator.validate())
                    getJob().error(error);
            }

            getJob().info("");  // blank line
        }
        catch (IOException e)
        {
            getJob().error(e.getMessage(), e);
        }
    }
}
