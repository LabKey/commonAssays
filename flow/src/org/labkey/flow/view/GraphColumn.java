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
