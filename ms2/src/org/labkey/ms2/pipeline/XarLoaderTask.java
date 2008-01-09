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

import org.labkey.api.exp.ExperimentPipelineJob;
import org.labkey.api.exp.FileXarSource;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.PipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * <code>XarLoaderTask</code>
 */
public class XarLoaderTask extends PipelineJob.Task
{
    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
        /**
         * Sets the rowId of the loaded experiment on the PipelineJob.
         */
        void setExperimentRowId(int rowId);
    }

    public static class Factory extends AbstractTaskFactory
    {
        public Factory()
        {
            super(XarLoaderTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new XarLoaderTask(job);
        }

        public String getStatusName()
        {
            return "LOAD EXPERIMENT";
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            // Check parameters to see if loading is required.
            return ("no".equalsIgnoreCase(job.getParameters().get("pipeline, load")));
        }
    }

    protected XarLoaderTask(PipelineJob job)
    {
        super(job);
    }

    public JobSupport getJobSupport()
    {
        return (JobSupport) getJob();
    }

    public void run()
    {
        String baseName = getJobSupport().getFileBasename();
        File dirAnalysis = getJobSupport().getAnalysisDirectory();

        File fileExperimentXML = XarGeneratorTask.FT_SEARCH_XAR.newFile(dirAnalysis, baseName);

        FileXarSource source = new FileXarSource(fileExperimentXML);
        if (ExperimentPipelineJob.loadExperiment(getJob(), source, false))
        {
            Integer rowId = source.getExperimentRowId();
            assert rowId != null;   // Status was successful
            getJobSupport().setExperimentRowId(rowId.intValue());
        }
    }
}
