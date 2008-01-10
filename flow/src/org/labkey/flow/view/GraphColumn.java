package org.labkey.flow.view;

import org.labkey.api.data.*;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.AppProps;
import org.apache.log4j.Logger;

import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.controllers.FlowParam;

import java.io.Writer;
import java.io.IOException;

public class GraphColumn extends DataColumn
{
    private Logger _log = Logger.getLogger(GraphColumn.class);

    public GraphColumn(ColumnInfo colinfo)
    {
        super(colinfo);
    }


    public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
    {
        renderGraph(ctx, out);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        renderGraph(ctx, out);
    }

    public void renderGraph(RenderContext ctx, Writer out) throws IOException
    {
        Object boundValue = getColumnInfo().getValue(ctx);
        if (boundValue == null)
        {
            out.write("<img class=\"flow-graph\" src=\"");
            out.write(AppProps.getInstance().getContextPath() + "/_.gif\">");
            return;
        }

        Object displayValue = getColumnInfo().getDisplayField().getValue(ctx);
        ActionURL urlGraph = PageFlowUtil.urlFor(WellController.Action.showGraph, ctx.getContainer());
        urlGraph.addParameter(FlowParam.objectId.toString(), boundValue.toString());
        urlGraph.addParameter(FlowParam.graph.toString(), displayValue.toString());
        out.write("<img class=\"flow-graph\" src=\"");
        out.write(PageFlowUtil.filter(urlGraph));
        out.write("\">");
    }
}
