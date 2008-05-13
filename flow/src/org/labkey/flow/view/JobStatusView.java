/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.view;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.script.FlowJob;
import org.labkey.flow.script.ScriptJob;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

public class JobStatusView extends HttpView
{
    private static Logger _log = Logger.getLogger(JobStatusView.class);

    PipelineStatusFile _psf;
    FlowJob _job;
    String _status;

    public JobStatusView(PipelineStatusFile psf, FlowJob job)
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
        out.write("<table><tr><td>");
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
        out.write("</code></div></td>");
        if (_job != null && _job instanceof ScriptJob)
        {
            ScriptJob scriptJob = (ScriptJob)_job;
            Map<FlowProtocolStep, String[]> processedRuns = scriptJob.getProcessedRunLSIDs();
            if (!processedRuns.isEmpty())
            {
                out.write("<td valign=\"top\">");
                out.write("<br>Completed runs:<br>");
                for (Map.Entry<FlowProtocolStep, String[]> entry : processedRuns.entrySet())
                {
                    out.write("<p>" + entry.getKey().getLabel() + " step<br>");
                    for (String lsid : entry.getValue())
                    {
                        FlowRun run = FlowRun.fromLSID(lsid);
                        if (run == null)
                        {
                            out.write("Run '" + PageFlowUtil.filter(lsid) + "' not found");
                        }
                        else
                        {
                            out.write("<a href=\"" + PageFlowUtil.filter(run.urlShow()) + "\">");
                            out.write(PageFlowUtil.filter(run.getLabel()));
                            out.write("</a><br>");
                        }
                    }
                    out.write("</p>");
                }
                out.write("</td>");
            }
        }
        out.write("</tr></table>");
        if (_job != null)
        {
            ActionURL cancelURL = new ActionURL("Flow", "cancelJob", getViewContext().getContainer());
            cancelURL.addParameter("statusFile", _psf.getFilePath());
            out.write("<br>" + PageFlowUtil.buttonLink("Cancel Job", cancelURL));
        }
    }
}
