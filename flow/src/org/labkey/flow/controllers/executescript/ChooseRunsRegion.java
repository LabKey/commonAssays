package org.labkey.flow.controllers.executescript;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowExperiment;

import java.io.Writer;
import java.io.IOException;
import java.io.File;
import java.sql.SQLException;

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
        return;
    }

    protected boolean isRecordSelectorEnabled(RenderContext ctx)
    {
        return getDisabledReason(ctx) == null;
    }

    // Allows subclasses to do pre-row and post-row processing
    // CONSIDER: Separate as renderTableRow and renderTableRowContents?
    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
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
            if (renderer.getVisible(ctx))
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
            if (experiment != null && experiment.findRun(new File(run.getPath()), _form.getProtocolStep()).length > 0)
            {
                return "The '" + experiment.getName() + "' analysis already contains this run.";
            }
            if (_form.getProtocol().requiresCompensationMatrix(_form.getProtocolStep()))
            {
                if (_form.getCompensationExperimentLSID() != null)
                {
                    FlowExperiment expComp = FlowExperiment.fromLSID(_form.getCompensationExperimentLSID());
                    if (expComp.findCompensationMatrix(run) == null)
                    {
                        return "There is no compensation matrix for this run in the '" + expComp.getName() + "' analysis";
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
