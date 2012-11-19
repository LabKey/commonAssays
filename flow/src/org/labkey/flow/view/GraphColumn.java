/*
 * Copyright (c) 2006-2012 LabKey Corporation
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
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.query.FlowQuerySettings;

import java.io.IOException;
import java.io.Writer;

public class GraphColumn extends DataColumn
{
    private static final String INCLUDE_UTIL_SCRIPT = "~~~Flow/util.js~~~";
    private Logger _log = Logger.getLogger(GraphColumn.class);
    private FlowQuerySettings.ShowGraphs _showGraphs;

    public GraphColumn(ColumnInfo colinfo)
    {
        super(colinfo);
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

        Object boundValue = getColumnInfo().getValue(ctx);
        String graphSize = FlowPreference.graphSize.getValue(ctx.getRequest()) + "px";
        Object displayValue = getColumnInfo().getDisplayField().getValue(ctx);
        String graphTitle = PageFlowUtil.filter(displayValue);

        if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Inline)
        {
            out.write("<span style=\"display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize + ";\">");
            if (boundValue == null)
            {
                out.write("<span class=\"labkey-disabled labkey-flow-graph\">No graph for:<br>" + graphTitle + "</span>");
            }
            else
            {
                String urlGraph = renderURL(ctx);
                out.write("<img alt=\"Graph of: " + graphTitle + "\" title=\"" + graphTitle + "\"");
                out.write(" style=\"height: " + graphSize + "; width: " + graphSize + ";\" class=\"labkey-flow-graph\" src=\"");
                out.write(PageFlowUtil.filter(urlGraph));
                out.write("\" onerror=\"flowImgError(this);\">");
            }
            out.write("</span><wbr>");
        }
        else if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Thumbnail)
        {
            if (boundValue == null)
            {
                out.write("&nbsp;");
            }
            else
            {
                String urlGraph = renderURL(ctx);

                StringBuilder iconHtml = new StringBuilder();
                iconHtml.append("<img width=32 height=32");
                iconHtml.append(" title=\"").append(graphTitle).append("\"");
                iconHtml.append(" src=\"").append(PageFlowUtil.filter(urlGraph)).append("\"");
                iconHtml.append(" />");

                StringBuilder imageHtml = new StringBuilder();
                imageHtml.append("<img src=\"").append(PageFlowUtil.filter(urlGraph)).append("\" />");

                out.write(PageFlowUtil.helpPopup(graphTitle, imageHtml.toString(), true, iconHtml.toString(), 310));
            }
        }
    }
}
