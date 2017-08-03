package org.labkey.flow.script;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.util.FileType;

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

        support.getInputFiles().forEach(file -> {
            job.info("Selected file: " + file.getAbsolutePath());
        });

        // TODO implementation from KeywordsJob to import files

        return new RecordedActionSet();
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
