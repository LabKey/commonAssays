package org.labkey.flow.data;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.flow.script.FlowJob;

import java.util.Date;

/**
 * Created by Nick Arnold on 7/14/2014.
 */
public class FlowStatus
{
    private PipelineStatusFile _pipelineStatusFile;
    private FlowJob _job;
    public PipelineStatusFile getPipelineStatusFile()
    {
        return _pipelineStatusFile;
    }

    public void setPipelineStatusFile(PipelineStatusFile pipelineStatusFile)
    {
        _pipelineStatusFile = pipelineStatusFile;
    }

    public FlowJob getJob()
    {
        return _job;
    }

    public void setJob(FlowJob job)
    {
        _job = job;
    }

    public String getStatus()
    {
        String status;

        if (_pipelineStatusFile == null || PipelineJob.TaskStatus.complete.matches(_pipelineStatusFile.getStatus()))
        {
            status = "This job is completed";
        }
        else if (PipelineJob.TaskStatus.error.matches(_pipelineStatusFile.getStatus()))
        {
            status = "This job encountered an error.";
        }
        else if (PipelineJob.TaskStatus.cancelled.matches(_pipelineStatusFile.getStatus()))
        {
            status = "This job was cancelled at " + _pipelineStatusFile.getModified();
        }
        else if (PipelineJob.TaskStatus.waiting.matches(_pipelineStatusFile.getStatus()))
        {
            status = "This job has not started yet.";
        }
        else
        {
            long sec = (new Date().getTime() - _pipelineStatusFile.getCreated().getTime()) / 1000;
            status = "This job has been running for " + sec + " seconds.";
        }

        return status;
    }
}
