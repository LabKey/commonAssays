/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.reports.FlowReportJob;
import org.labkey.flow.reports.FlowReportManager;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * User: kevink
 * Date: May 4, 2008 5:36:47 PM
 */
public abstract class FlowJob extends PipelineJob
{
    private static Logger _log = getJobLogger(FlowJob.class);

    private volatile Date _start;
    private volatile Date _end;

    private transient ActionURL _statusHref;

    public FlowJob(String provider, ViewBackgroundInfo info, PipeRoot root)
            throws SQLException
    {
        super(provider, info, root);
    }

    abstract protected void doRun() throws Throwable;

    public void run()
    {
        _start = new Date();
        if (!setStatus("Running"))
        {
            return;
        }
        addStatus("Job started at " + DateUtil.formatDateTime(_start));
        try
        {
            doRun();
        }
        catch (Throwable e)
        {
            _log.error("Exception", e);
            addStatus("Error " + e.toString());
            setStatus(ERROR_STATUS, e.toString());
            return;
        }
        finally
        {
            FlowManager.get().flowObjectModified();
            _end = new Date();
            addStatus("Job completed at " + DateUtil.formatDateTime(_end));
            long duration = Math.max(0, _end.getTime() - _start.getTime());
            addStatus("Elapsed time " + DateUtil.formatDuration(duration));
        }
        if (checkInterrupted())
        {
            setStatus(INTERRUPTED_STATUS);
        }
        else if (hasErrors())
        {
            setStatus(ERROR_STATUS);
        }
        else
        {
            setStatus(COMPLETE_STATUS);
        }
    }


    public int getElapsedTime()
    {
        Date start = _start;
        if (start == null)
            return 0;
        Date end = _end;
        if (end == null)
        {
            end = new Date();
        }
        return (int) (end.getTime() - start.getTime());
    }

    synchronized public void addStatus(String status)
    {
        info(status);
    }

    public boolean hasErrors()
    {
        return getErrors() > 0;
    }

    public void addError(String lsid, String propertyURI, String message)
    {
        error(message);
        setStatus(ERROR_STATUS);
    }

    public boolean isComplete()
    {
        return _end != null;
    }

    protected boolean canInterrupt()
    {
        return true;
    }

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

    public ActionURL getStatusHref()
    {
        ActionURL ret = urlRedirect();
        if (ret != null)
        {
            return ret;
        }
        return urlStatus().clone();
    }

    public ActionURL urlRedirect()
    {
        if (!isComplete())
            return null;
        if (hasErrors())
            return null;
        return urlData();
    }

    public abstract ActionURL urlData();

    public ActionURL urlStatus()
    {
        if (_statusHref == null)
        {
            File statusFile = getLogFile();
            _statusHref = new ActionURL(FlowController.ShowStatusJobAction.class, getContainer());
            _statusHref.addParameter(FlowParam.statusFile.toString(), PipelineJobService.statusPathOf(statusFile.toString()));
        }
        return _statusHref;
    }

    public String getStatusFilePath()
    {
        return PipelineJobService.statusPathOf(getLogFile().getAbsolutePath());
    }

    public ActionURL urlCancel()
    {
        ActionURL ret = new ActionURL(FlowController.CancelJobAction.class, getContainer());
        ret.addParameter(FlowParam.statusFile.toString(), getStatusFilePath());
        return ret;
    }

    public void handleException(Throwable e)
    {
        _log.error("Error", e);
        addError(null, null, e.toString());
    }

    protected void runPostAnalysisJobs() throws Exception
    {
        // UNDONE: execute post analysis scripts after importing workspace
//        if (checkInterrupted() || hasErrors())
//            return;
//
//        List<FlowReportJob> jobs = FlowReportManager.createReportJobs(getInfo(), getPipeRoot());
//        for (FlowReportJob job : jobs)
//        {
//            job.setLogFile(getLogFile());
//            job.setLogLevel(getLogLevel());
//            job.setSubmitted();
//            job.run();
//            if (job.getErrors() > 0)
//                error("Error running post-analysis report job: " + job.getDescription());
//
//            if (checkInterrupted() || hasErrors())
//                break;
//        }
    }

}
