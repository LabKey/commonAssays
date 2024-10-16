/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.action.UrlProvider;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.SampleKey;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.reports.FlowReportJob;
import org.labkey.flow.reports.FlowReportManager;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: May 4, 2008 5:36:47 PM
 */
public abstract class FlowJob extends PipelineJob
{
    private static final Logger _log = getJobLogger(FlowJob.class);

    private volatile Date _start;
    private volatile Date _end;

    private transient ActionURL _statusHref;

    private transient Map<SampleKey, ExpMaterial> _sampleMap;
    FlowProtocol _protocol;

    // For serialization
    protected FlowJob() {}

    public FlowJob(String provider, ViewBackgroundInfo info, PipeRoot root)
    {
        super(provider, info, root);
    }

    abstract protected void doRun() throws Throwable;

    @Override
    public void run()
    {
        _start = new Date();
        if (!setStatus(TaskStatus.running))
        {
            return;
        }
        addStatus("Job started at " + DateUtil.formatDateTime(getContainer(), _start));
        try
        {
            doRun();
        }
        catch (Throwable e)
        {
            _log.error("Exception", e);
            addStatus("Error " + e.toString());
            setStatus(TaskStatus.error, e.toString());
            return;
        }
        finally
        {
            FlowManager.get().flowObjectModified();
            _end = new Date();
            addStatus("Job completed at " + DateUtil.formatDateTime(getContainer(), _end));
            long duration = Math.max(0, _end.getTime() - _start.getTime());
            addStatus("Elapsed time " + DateUtil.formatDuration(duration));
        }
        if (hasErrors())
        {
            setStatus(TaskStatus.error);
        }
        else
        {
            setStatus(TaskStatus.complete);
        }
    }


    synchronized public void addStatus(String status)
    {
        info(status);
    }

    public boolean hasErrors()
    {
        return getErrors() > 0;
    }

    @Override
    public void error(String message, Throwable t)
    {
        super.error(message, t);
        setStatus(TaskStatus.error);
    }

    @Override
    protected boolean canInterrupt()
    {
        return true;
    }

    @Override
    public synchronized boolean interrupt()
    {
        addStatus("Job Interrupted");
        return super.interrupt();
    }

    public String getStatusText()
    {
        if (_start == null)
            return "Pending";
        if (_end != null)
        {
            if (hasErrors())
            {
                return "Errors";
            }
            return "Complete";
        }
        if (hasErrors())
        {
            return "Running (errors)";
        }
        return "Running";
    }

    @Override
    public ActionURL getStatusHref()
    {
        if (hasErrors())
            return null;
        return urlData();
    }

    /** Link to imported data once job has completed */
    public abstract ActionURL urlData();

    /** Link to pipeline status details */
    public ActionURL urlStatus()
    {
        if (_statusHref == null)
        {
            Integer jobId = PipelineService.get().getJobId(getUser(), getContainer(), getJobGUID());
            if (jobId != null)
            {
                _statusHref = PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), jobId);
            }
        }
        return _statusHref;
    }

    public String getStatusFilePath()
    {
        return PipelineJobService.statusPathOf(getLogFile().getAbsolutePath());
    }

    protected void runPostAnalysisJobs()
    {
        // disable post analysis jobs
        if (1==1)
            return;

        if (checkInterrupted() || hasErrors())
            return;

        // URL may not be set on root-less pipeline jobs (uploading a FlowJoWorkspace for analysis without a pipeline root set)
        // See AnalysisScriptController.stepConfirm().
        ViewBackgroundInfo info = getInfo();
        if (info.getURL() == null)
            info = new ViewBackgroundInfo(info.getContainer(), info.getUser(), new ActionURL(FlowController.BeginAction.class, info.getContainer()));

        List<FlowReportJob> jobs = FlowReportManager.createReportJobs(info, getPipeRoot());
        if (jobs.size() > 0)
            info("Running post-analysis jobs...");
        for (FlowReportJob job : jobs)
        {
            job.setLogFile(getLogFile());
            job.setLogLevel(getLogLevel());
            job.setSubmitted();
            job.run();
            if (job.getErrors() > 0)
                error("Error running post-analysis report job: " + job.getDescription());

            if (checkInterrupted() || hasErrors())
                break;
        }
    }

    public FlowProtocol getProtocol()
    {
        return _protocol;
    }

    public Map<SampleKey, ExpMaterial> getSampleMap()
    {
        if (_sampleMap == null)
        {
            _sampleMap = getProtocol().getSampleMap(getUser());
        }
        return _sampleMap;
    }
}
