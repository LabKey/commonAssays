package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;

/**
 * User: kevink
 * Date: May 4, 2008 5:36:47 PM
 */
public abstract class FlowJob extends PipelineJob
{
    private static Logger _log = getJobLogger(FlowJob.class);

    private volatile Date _start;
    private volatile Date _end;
    private volatile boolean _errors;

    private transient ActionURL _statusHref;

    public FlowJob(String provider, ViewBackgroundInfo info)
            throws SQLException
    {
        super(provider, info);
    }

    public FlowJob(PipelineJob job)
    {
        super(job);
    }

    abstract protected void doRun() throws Throwable;

    public void run()
    {
        _start = new Date();
        setStatus("Running");
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
            _end = new Date();
            addStatus("Job completed at " + DateUtil.formatDateTime(_end));
            long duration = Math.max(0, _end.getTime() - _start.getTime());
            addStatus("Elapsed time " + DateUtil.formatDuration(duration));
        }
        if (checkInterrupted())
        {
            setStatus(INTERRUPTED_STATUS);
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
        return _errors;
    }

    public void addError(String lsid, String propertyURI, String message)
    {
        _errors = true;
        addStatus(message);
    }

    public boolean isStarted()
    {
        return _start != null;
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
            if (_errors)
            {
                return "Errors";
            }
            return "Complete";
        }
        if (_errors)
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
            _statusHref = PageFlowUtil.urlFor(FlowController.Action.showStatusJob, getContainer());
            _statusHref.addParameter(FlowParam.statusFile.toString(), PipelineJobService.statusPathOf(statusFile.toString()));
        }
        return _statusHref;
    }

    public String getStatusFilePath()
    {
        return PipelineJobService.statusPathOf(getStatusFile().getAbsolutePath());
    }

    public ActionURL urlCancel()
    {
        ActionURL ret = PageFlowUtil.urlFor(FlowController.Action.cancelJob, getContainer());
        ret.addParameter(FlowParam.statusFile.toString(), getStatusFilePath());
        return ret;
    }

    public void handleException(Throwable e)
    {
        _log.error("Error", e);
        addError(null, null, e.toString());
    }


}
