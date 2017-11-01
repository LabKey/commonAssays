/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.flow.script;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.util.FileType;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class KeywordsTask extends PipelineJob.Task<KeywordsTask.Factory>
{
    public KeywordsTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        PipelineJob job = getJob();
        FileAnalysisJobSupport support = job.getJobSupport(FileAnalysisJobSupport.class);

        try
        {
            FlowProtocol protocol = FlowProtocol.ensureForContainer(job.getUser(), job.getContainer());
            importFlowRuns(job, protocol, support.getInputFiles(), null);
        }
        catch(Exception e)
        {
            job.error("FCS Directory import failed: ", e);
        }

        return new RecordedActionSet();
    }

    public static List<FlowRun> importFlowRuns(PipelineJob job, FlowProtocol protocol, List<File> paths, Container targetStudyContainer) throws IOException, SQLException
    {
        PipeRoot pr = PipelineService.get().findPipelineRoot(job.getContainer());

        KeywordsJob keywordsJob = new KeywordsJob(job.getInfo(), protocol, paths, targetStudyContainer, pr);
        keywordsJob.setLogFile(job.getLogFile());
        keywordsJob.setLogLevel(job.getLogLevel());
        keywordsJob.setSubmitted();

        List<FlowRun> runs = keywordsJob.go();
        if (keywordsJob.hasErrors())
        {
            job.error("Failed to import keywords.");
            job.setStatus(PipelineJob.TaskStatus.error);
        }
        else
        {
            for (FlowRun run : runs)
            {
                String originalSourcePath = job.getParameters().get("OriginalSourcePath");
                if (null != originalSourcePath)
                {
                    job.info("Created keywords run '" + run.getName() + "' for path '" + run.getPath() + "' having original source path '" + originalSourcePath + "'");
                    run.setProperty(job.getUser(), FlowProperty.OriginalSourcePath.getPropertyDescriptor(), originalSourcePath);
                    for (FlowFCSFile fcsFile : run.getFCSFiles())
                    {
                        String fcsFileName;
                        fcsFileName = fcsFile.getKeyword("$FIL");
                        fcsFile.setProperty(job.getUser(), FlowProperty.OriginalSourceFile.getPropertyDescriptor(), originalSourcePath + File.separatorChar + fcsFileName);
                    }
                }
                else
                    job.info("Created keywords run '" + run.getName() + "' for path '" + run.getPath() + "'");
            }
        }

        return runs;
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(KeywordsTask.class);
        }

        public List<FileType> getInputTypes()
        {
            return Collections.emptyList();
        }

        public String getStatusName()
        {
            return "FCS KEYWORDS";
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.emptyList();
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new KeywordsTask(this, job);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }
}
