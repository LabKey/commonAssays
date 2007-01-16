package org.labkey.flow.view;

import org.apache.log4j.Logger;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.script.ScriptJob;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.pipeline.PipelineStatusFile;

import java.io.PrintWriter;
import java.io.File;
import java.util.Map;

public class JobStatusView extends HttpView
{
    private static Logger _log = Logger.getLogger(JobStatusView.class);

    PipelineStatusFile _psf;
    ScriptJob _job;
    String _status;

    public JobStatusView(PipelineStatusFile psf, ScriptJob job)
    {
        _psf = psf;
        _job = job;
        if (_job == null)
        {
            _status = "This job is completed.";
        }
        else
        {
            if (!_job.isStarted())
            {
                _status = "This job has not started yet.";
            }
            else
            {
                _status = "This job has been running for " + (_job.getElapsedTime() / 1000) + " seconds."; 
            }
        }
    }

    @Override
    protected void renderInternal(Object model, PrintWriter out) throws Exception
    {
        out.write("<p>");
        out.write(PageFlowUtil.filter(_psf.getDescription()));
        out.write("</p>");
        out.write("<p>");
        out.write(PageFlowUtil.filter(_status));
        out.write("</p>");
        out.write("<div style=\"height:300px;width:500px;overflow:auto;\"><code>");
        String log = PageFlowUtil.getFileContentsAsString(new File(_psf.getFilePath()));
        for (int ich = 0; ich < log.length(); ich ++)
        {
            char c = log.charAt(ich);
            switch (c)
            {
                case '<':
                    out.write("&lt;");
                    break;
                case '&':
                    out.write("&amp;");
                case '\n':
                    out.write("<br>");
                default:
                    out.write(c);
            }
        }
        out.write("<a name=\"end\">&nbsp;</a>");
        out.write("</code></div>");
        if (_job != null)
        {
            String[] runLSIDs = _job.getProcessedRunLSIDs();
            out.write(runLSIDs.length + " runs have been processed.");
            if (runLSIDs.length > 0)
            {
                try
                {
                    FlowRun lastRun = FlowRun.fromLSID(runLSIDs[runLSIDs.length - 1]);

                    out.write("<br><a href=\"" + lastRun.urlShow() + "\">Last run processed</a>");
                }
                catch (Throwable t)
                {
                    _log.error("Error", t);
                }
            }

            ViewURLHelper cancelURL = new ViewURLHelper("Flow", "cancelJob", getViewContext().getContainer());
            cancelURL.addParameter("statusFile", _psf.getFilePath());
            out.write("<br>" + PageFlowUtil.buttonLink("Cancel Job", cancelURL));
        }
    }
}
