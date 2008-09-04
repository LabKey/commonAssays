/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;

import java.io.IOException;
import java.io.Writer;

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
        String graphSize = FlowPreference.graphSize.getValue(ctx.getRequest());
        Object displayValue = getColumnInfo().getDisplayField().getValue(ctx);
        String graphTitle = PageFlowUtil.filter(displayValue.toString());
        if (boundValue == null)
        {
            out.write("<span style=\"display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize + ";\" class=\"labkey-disabled labkey-flow-graph\">");
            out.write("No graph for:<br>" + graphTitle);
            out.write("</span><wbr>");
            return;
        }

        ActionURL urlGraph = PageFlowUtil.urlFor(WellController.Action.showGraph, ctx.getContainer());
        urlGraph.addParameter(FlowParam.objectId.toString(), boundValue.toString());
        urlGraph.addParameter(FlowParam.graph.toString(), displayValue.toString());
        out.write("<img alt=\"Graph of: " + graphTitle + "\" title=\"" + graphTitle + "\"");
        out.write(" style=\"height: " + graphSize + "; width: " + graphSize + ";\" class=\"labkey-flow-graph\" src=\"");
        out.write(PageFlowUtil.filter(urlGraph));
        out.write("\"><wbr>");
    }
}
