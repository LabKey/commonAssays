/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.apache.commons.lang3.mutable.MutableInt;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DetailsColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.UpdateColumn;
import org.labkey.api.util.DOM.Renderable;
import org.labkey.api.util.HtmlString;
import org.labkey.api.writer.HtmlWriter;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.data.FlowCompensationControl;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.labkey.api.util.DOM.Attribute.colspan;
import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.Attribute.title;
import static org.labkey.api.util.DOM.TD;
import static org.labkey.api.util.DOM.TR;
import static org.labkey.api.util.DOM.at;
import static org.labkey.api.util.DOM.cl;

public class ChooseRunsRegion extends DataRegion
{
    ChooseRunsToAnalyzeForm _form;
    
    public ChooseRunsRegion(ChooseRunsToAnalyzeForm form)
    {
        _form = form;
    }


    @Override
    protected void renderFormBegin(RenderContext ctx, HtmlWriter out, int mode)
    {
        renderHiddenFormFields(ctx, out, mode);
    }

    @Override
    protected HtmlString getNoRowsMessage()
    {
        return HtmlString.of("No runs available. Please import some FCS files or import a FlowJo workspace associated with FCS files.");
    }

    @Override
    protected boolean isRecordSelectorEnabled(RenderContext ctx)
    {
        return getDisabledReason(ctx) == null;
    }

    // Allows subclasses to do pre-row and post-row processing
    // CONSIDER: Separate as renderTableRow and renderTableRowContents?
    @Override
    protected void renderTableRow(RenderContext ctx, HtmlWriter out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex)
    {
        String disabledReason = getDisabledReason(ctx);
        DisplayColumn detailsColumn = getDetailsUpdateColumn(ctx, renderers, true);
        DisplayColumn updateColumn = getDetailsUpdateColumn(ctx, renderers, false);
        MutableInt visibleCount = new MutableInt(0);
        MutableInt nameColumn = new MutableInt(0);

        TR(
            disabledReason != null ? cl("disabledRow").at(title, disabledReason) : null,
            (Renderable) ret -> {
                if (showRecordSelectors || (detailsColumn != null || updateColumn != null))
                {
                    visibleCount.increment();
                    renderActionColumn(ctx, out, rowIndex, showRecordSelectors, detailsColumn, updateColumn);
                }

                for (int i = 0, renderersSize = renderers.size(); i < renderersSize; i++)
                {
                    DisplayColumn renderer = renderers.get(i);
                    if (renderer.isVisible(ctx))
                    {
                        if (renderer instanceof DetailsColumn || renderer instanceof UpdateColumn)
                            continue;

                        if (renderer.getColumnInfo() != null && "name".equalsIgnoreCase(renderer.getColumnInfo().getName()))
                            nameColumn.setValue(i + 1);
                        visibleCount.increment();
                        renderer.renderGridDataCell(ctx, out);
                    }
                }
                return ret;
            }
        ).appendTo(out);

        if (disabledReason != null)
        {
            TR(
                cl("disabledRow"),
                TD(
                    at(style, "border-right:0;", colspan, nameColumn.getValue()),
                    HtmlString.NBSP
                ),
                TD(
                    at(style, "font-size:smaller;", colspan, visibleCount.getValue()),
                    disabledReason
                )
            ).appendTo(out);
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
