/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
import org.labkey.api.data.DetailsColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.UpdateColumn;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.data.FlowCompensationControl;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;

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


    @Override
    protected void renderFormBegin(RenderContext ctx, Writer out, int mode) throws IOException
    {
        renderHiddenFormFields(ctx, out, mode);
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
    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        out.write("<tr");
        String disabledReason = getDisabledReason(ctx);
        if (disabledReason != null)
        {
            out.write(" title=\"" + PageFlowUtil.filter(disabledReason) + "\"");
            out.write(" class=\"disabledRow\"");
        }
        out.write(">");

        DisplayColumn detailsColumn = getDetailsUpdateColumn(ctx, renderers, true);
        DisplayColumn updateColumn = getDetailsUpdateColumn(ctx, renderers, false);

        int visibleCount = 0;
        if (showRecordSelectors || (detailsColumn != null || updateColumn != null))
        {
            visibleCount++;
            renderActionColumn(ctx, out, rowIndex, showRecordSelectors, detailsColumn, updateColumn);
        }

        int nameColumn = 0;
        for (int i = 0, renderersSize = renderers.size(); i < renderersSize; i++)
        {
            DisplayColumn renderer = renderers.get(i);
            if (renderer.isVisible(ctx))
            {
                if (renderer instanceof DetailsColumn || renderer instanceof UpdateColumn)
                    continue;

                if (renderer.getColumnInfo() != null && "name".equalsIgnoreCase(renderer.getColumnInfo().getName()))
                    nameColumn = i+1;
                visibleCount++;
                renderer.renderGridDataCell(ctx, out);
            }
        }

        out.write("</tr>\n");

        if (disabledReason != null)
        {
            out.write("<tr class='disabledRow'>");
            out.write("<td style='border-right:0;' colspan='" + nameColumn + "'>&nbsp;</td>");
            out.write("<td style='font-size:smaller;' colspan='" + visibleCount + "'>");
            out.write(PageFlowUtil.filter(disabledReason));
            out.write("</td>");
            out.write("</tr>");
        }
    }

    String getDisabledReason(RenderContext ctx)
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
            else if (_form.useSpillCompensationMatrix())
            {
                for (FlowWell well : run.getWells())
                {
                    if (well instanceof FlowCompensationControl)
                        continue;

                    FlowFCSFile fcsFile = well.getFCSFileInput();
                    CompensationMatrix matrix = CompensationMatrix.fromSpillKeyword(fcsFile.getKeywords());
                    if (matrix != null)
                        return null;
                }

                return "There are no FCSFile wells with a spill matrix in this run.";
            }
        }
        return null;
    }
}
