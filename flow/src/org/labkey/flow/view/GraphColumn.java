/*
 * Copyright (c) 2006-2010 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;

import java.io.IOException;
import java.io.Writer;

public class GraphColumn extends DataColumn
{
    private static final String INCLUDE_UTIL_SCRIPT = "~~~Flow/util.js~~~";
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
        if (!ctx.containsKey(INCLUDE_UTIL_SCRIPT))
        {
            out.write("<script type='text/javascript' src='" + AppProps.getInstance().getContextPath() + "/Flow/util.js'></script>");
            ctx.put(INCLUDE_UTIL_SCRIPT, true);
        }

        Object boundValue = getColumnInfo().getValue(ctx);
        String graphSize = FlowPreference.graphSize.getValue(ctx.getRequest()) + "px";
        Object displayValue = getColumnInfo().getDisplayField().getValue(ctx);
        String graphTitle = PageFlowUtil.filter(displayValue.toString());

        out.write("<span style=\"display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize + ";\">");
        if (boundValue == null)
        {
            out.write("<span class=\"labkey-disabled labkey-flow-graph\">No graph for:<br>" + graphTitle + "</span>");
        }
        else
        {
            ActionURL urlGraph = PageFlowUtil.urlFor(WellController.Action.showGraph, ctx.getContainer());
            urlGraph.addParameter(FlowParam.objectId.toString(), boundValue.toString());
            urlGraph.addParameter(FlowParam.graph.toString(), displayValue.toString());
            out.write("<img alt=\"Graph of: " + graphTitle + "\" title=\"" + graphTitle + "\"");
            out.write(" style=\"height: " + graphSize + "; width: " + graphSize + ";\" class=\"labkey-flow-graph\" src=\"");
            out.write(PageFlowUtil.filter(urlGraph));
            out.write("\" onerror=\"flowImgError(this);\">");
        }
        out.write("</span><wbr>");
    }
}
