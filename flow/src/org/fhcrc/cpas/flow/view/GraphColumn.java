package org.fhcrc.cpas.flow.view;

import org.labkey.api.data.*;
import org.fhcrc.cpas.flow.util.PFUtil;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.AppProps;
import org.apache.log4j.Logger;
import com.labkey.flow.web.GraphSpec;

import Flow.Well.WellController;
import Flow.FlowParam;

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
        ViewURLHelper urlGraph = PFUtil.urlFor(WellController.Action.showGraph, ctx.getContainer());
        urlGraph.addParameter(FlowParam.objectId.toString(), boundValue.toString());
        urlGraph.addParameter(FlowParam.graph.toString(), displayValue.toString());
        out.write("<img class=\"flow-graph\" src=\"");
        out.write(PageFlowUtil.filter(urlGraph));
        out.write("\">");
    }
}
