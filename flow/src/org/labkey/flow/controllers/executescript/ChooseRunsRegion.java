/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;

public class ChooseRunsRegion extends DataRegion
{
    ChooseRunsToAnalyzeForm _form;
    
    public ChooseRunsRegion(ChooseRunsToAnalyzeForm form)
    {
        _form = form;
    }


    protected void renderFormEnd(RenderContext ctx, Writer out) throws IOException
    {
        return;
    }

    protected void renderFormHeader(Writer out, int mode) throws IOException
    {
        renderHiddenFormFields(out, mode);
    }

    protected String getNoRowsMessage()
    {
        return "No runs available.  Please import some FCS files or import a FlowJo workspace associated with FCS files.";
    }

    protected boolean isRecordSelectorEnabled(RenderContext ctx)
    {
        return getDisabledReason(ctx) == null;
    }

    // Allows subclasses to do pre-row and post-row processing
    // CONSIDER: Separate as renderTableRow and renderTableRowContents?
    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        out.write("<tr");
        String disabledReason = getDisabledReason(ctx);
        if (disabledReason != null)
        {
            out.write(" title=\"" + PageFlowUtil.filter(disabledReason) + "\"");
            out.write(" class=\"disabledRow\"");
        }
        out.write(">");


        if (_showRecordSelectors)
        {
            renderRecordSelector(ctx, out);
        }

        String style = null;
        for (DisplayColumn renderer : renderers)
            if (renderer.isVisible(ctx))
                renderer.renderGridDataCell(ctx, out, style);

        out.write("</tr>\n");
    }

    String getDisabledReason(RenderContext ctx)
    {
        try
        {
            FlowRun run = FlowRun.fromRunId((Integer)ctx.getRow().get("RowId"));
            FlowExperiment experiment = _form.getTargetExperiment();
            if (run.getPath() == null)
            {
                return null;
            }
            if (experiment != null && experiment.hasRun(new File(run.getPath()), _form.getProtocolStep()))
            {
                return "The '" + experiment.getName() + "' analysis folder already contains this run.";
            }
            if (_form.getProtocol().requiresCompensationMatrix(_form.getProtocolStep()))
            {
                if (_form.getCompensationExperimentLSID() != null)
                {
                    FlowExperiment expComp = FlowExperiment.fromLSID(_form.getCompensationExperimentLSID());
                    if (expComp.findCompensationMatrix(run) == null)
                    {
                        return "There is no compensation matrix for this run in the '" + expComp.getName() + "' analysis folder";
                    }
                }
            }
            return null;
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }

    }
}
