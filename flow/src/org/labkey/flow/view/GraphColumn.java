/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.template.PageConfig;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.query.FlowQuerySettings;

import java.io.IOException;
import java.io.Writer;

import static org.labkey.api.util.DOM.Attribute.alt;
import static org.labkey.api.util.DOM.Attribute.height;
import static org.labkey.api.util.DOM.Attribute.src;
import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.Attribute.title;
import static org.labkey.api.util.DOM.Attribute.width;
import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.IMG;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.at;
import static org.labkey.api.util.DOM.cl;

public class GraphColumn extends DataColumn
{
    public static final String SEP = "~~~";

    private static final String INCLUDE_UTIL_SCRIPT = "~~~Flow/util.js~~~";
    private static final Logger LOG = LogManager.getLogger(GraphColumn.class);

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

        if (!parts[0].isEmpty())
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
        if (graphSpec.isEmpty())
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
                    // Most likely rendering a flow dataset that has been linked to a study.
                    showGraphs = FlowQuerySettings.ShowGraphs.Thumbnail;
                }
            }
            _showGraphs = showGraphs;
        }
        return _showGraphs;
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        renderGraph(ctx, out);
    }

    public void renderGraph(RenderContext ctx, Writer out) throws IOException
    {
        if (!ctx.containsKey(INCLUDE_UTIL_SCRIPT))
        {
            out.write(String.format("<script type='text/javascript' src='%1$s/Flow/util.js' nonce='%2$s' ></script>", AppProps.getInstance().getContextPath(), PageConfig.getScriptNonceHeader(ctx.getRequest())));
            ctx.put(INCLUDE_UTIL_SCRIPT, true);
        }

        Object boundValue = getColumnInfo().getValue(ctx);
        if ((!(boundValue instanceof String)))
        {
            LOG.debug("error parsing graph spec: expected pair of values, but got '" + boundValue + "'");
            out.write("&nbsp;");
            return;
        }

        Integer objectId;
        String graphSpec;

        try
        {
            Pair<Integer, String> pair = parseObjectIdGraph((String) boundValue);
            objectId = pair.first;
            graphSpec = pair.second;
        }
        catch (IllegalArgumentException ex)
        {
            LOG.debug(ex.getMessage());
            out.write("&nbsp;");
            return;
        }

        String graphSize = FlowPreference.graphSize.getValue(ctx.getRequest()) + "px";

        if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Inline)
        {
            PageConfig pageConfig = HttpView.currentPageConfig();
            String id = pageConfig.makeId("img_");

            SPAN(
                at(style, "display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize),
                objectId == null ?
                    SPAN(cl("labkey-disabled labkey-flow-graph"), "No graph for:", BR(), graphSpec) :
                    IMG(
                        at(alt, "Graph of: " + graphSpec).at(title, graphSpec).at(style, "height: " + graphSize + "; width: " + graphSize)
                            .at(src, urlGraph(objectId, graphSpec, ctx.getContainer()))
                            .id(id).cl("labkey-flow-graph")
                    )
            ).appendTo(out);
            pageConfig.addHandler(id, "error", "flowImgError(this);");
            out.write("<wbr>");
        }
        else if (showGraphs(ctx) == FlowQuerySettings.ShowGraphs.Thumbnail)
        {
            if (objectId == null)
            {
                out.write("&nbsp;");
            }
            else
            {
                ActionURL urlGraph = urlGraph(objectId, graphSpec, ctx.getContainer());
                HtmlString iconHtml = DOM.createHtmlFragment(IMG(at(width, 32).at(height, 32).at(title, graphSpec).at(src, urlGraph)));
                HtmlString imageHtml = DOM.createHtmlFragment(IMG(at(src, urlGraph)));
                PageFlowUtil.popupHelp(imageHtml, graphSpec).link(iconHtml).width(310).appendTo(out);
            }
        }
    }

    // NOTE: We generate the URL for the current container, but the ShowGraphAction
    // will redirect to the objectId's container if the user has read permission there.
    private ActionURL urlGraph(Integer objectId, String graphSpec, Container container)
    {
        return new ActionURL(WellController.ShowGraphAction.class, container)
            .addParameter(FlowParam.objectId, objectId)
            .addParameter(FlowParam.graph, graphSpec);
    }
}
