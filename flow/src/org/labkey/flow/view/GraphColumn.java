/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.query.FlowQuerySettings;

import java.io.IOException;
import java.io.Writer;

public class GraphColumn extends DataColumn
{
    public static final String SEP = "~~~";

    private static final String INCLUDE_UTIL_SCRIPT = "~~~Flow/util.js~~~";
    private Logger _log = Logger.getLogger(GraphColumn.class);
    private FlowQuerySettings.ShowGraphs _showGraphs;

    public GraphColumn(ColumnInfo colinfo)
    {
        super(colinfo);
    }

    /**
     * Parse the column value formatted as objectId~~~graphSpec into parts.  The objectId may be null, graphSpec will not be null.
     */
    @NotNull
    static public Pair<Integer, String> parseObjectIdGraph(@NotNull String objectIdGraph)
    {
        Integer objectId = null;
        String graphSpec = null;

        String[] parts = objectIdGraph.split(SEP, 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("error parsing graph spec: expected pair of values: " + objectIdGraph);

        if (parts[0].length() > 0)
        {
            try
            {
                objectId = Integer.parseInt(parts[0]);
            }
            catch (NumberFormatException nfe)
            {
                throw new IllegalArgumentException("error parsing graph spec: expected first part to be integer value: " + objectIdGraph);
            }
        }

        graphSpec = parts[1];
        if (graphSpec.length() == 0)
        {
            throw new IllegalArgumentException("error parsing graph spec: expected second part to be non-empty string: " + objectIdGraph);
        }

        return Pair.of(objectId, graphSpec);
    }


    protected FlowQuerySettings.ShowGraphs showGraphs(RenderContext ctx)
    {
        if (_showGraphs == null)
        {
            FlowQuerySettings.ShowGraphs showGraphs = FlowQuerySettings.ShowGraphs.None;
            DataRegion rgn = ctx.getCurrentRegion();
            if (rgn != null)
            {
                QuerySettings settings = rgn.getSettings();
                if (settings instanceof FlowQuerySettings)
                    showGraphs = ((FlowQuerySettings)settings).getShowGraphs();
                else
                {
                    // Most likely rendering a flow dataset that has been copied to a study.
                    showGraphs = FlowQuerySettings.ShowGraphs.Thumbnail;
                }
            }
            _showGraphs = showGraphs;
        }
        return _showGraphs;
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

        Integer objectId = null;
        String graphSpec = null;
        String graphTitle;
        Object boundValue = getColumnInfo().getValue(ctx);
        if ((boundValue instanceof String))
        {
            try
            {
                Pair<Integer, String> pair = parseObjectIdGraph((String) boundValue);
                objectId = pair.first;
                graphSpec = pair.second;
            }
            catch (IllegalArgumentException ex)
            {
                _log.debug(ex.getMessage());
                out.write("&nbsp;");
                return;
            }

            graphTitle = PageFlowUtil.filter(graphSpec);
        }
        else
        {
            _log.debug("error parsing graph spec: expected pair of values, but got '" + String.valueOf(boundValue) + "'");
            out.write("&nbsp;");
            return;
        }


        String graphSize = FlowPreference.graphSize.getValue(ctx.getRequest()) + "px";

        if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Inline)
        {
            out.write("<span style=\"display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize + ";\">");
            if (objectId == null)
            {
                out.write("<span class=\"labkey-disabled labkey-flow-graph\">No graph for:<br>" + graphTitle + "</span>");
            }
            else
            {
                String urlGraph = urlGraph(objectId, graphSpec, ctx.getContainer());
                out.write("<img alt=\"Graph of: " + graphTitle + "\" title=\"" + graphTitle + "\"");
                out.write(" style=\"height: " + graphSize + "; width: " + graphSize + ";\" class=\"labkey-flow-graph\" src=\"");
                out.write(PageFlowUtil.filter(urlGraph));
                out.write("\" onerror=\"flowImgError(this);\">");
            }
            out.write("</span><wbr>");
        }
        else if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Thumbnail)
        {
            if (objectId == null)
            {
                out.write("&nbsp;");
            }
            else
            {
                String urlGraph = urlGraph(objectId, graphSpec, ctx.getContainer());

                StringBuilder iconHtml = new StringBuilder();
                iconHtml.append("<img width=32 height=32");
                iconHtml.append(" title=\"").append(graphTitle).append("\"");
                iconHtml.append(" src=\"").append(PageFlowUtil.filter(urlGraph)).append("\"");
                iconHtml.append(" />");

                StringBuilder imageHtml = new StringBuilder();
                imageHtml.append("<img src=\"").append(PageFlowUtil.filter(urlGraph)).append("\" />");

                out.write(PageFlowUtil.helpPopup(graphSpec, imageHtml.toString(), true, iconHtml.toString(), 310));
            }
        }
    }

    // NOTE: We generate the URL for the current container, but the ShowGraphAction
    // will redirect to the objectId's container if the user has read permission there.
    private String urlGraph(Integer objectId, String graphSpec, Container container)
    {
        return new ActionURL(WellController.ShowGraphAction.class, container)
                .addParameter(FlowParam.objectId, objectId)
                .addParameter(FlowParam.graph, graphSpec)
                .toString();
    }
}
