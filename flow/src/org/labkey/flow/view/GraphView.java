/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import org.labkey.api.view.DataView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.PageFlowUtil;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.labkey.flow.controllers.well.WellController;

/**
 */
public class GraphView extends GridView
{
    public GraphView(DataView otherDataView)
    {
        super(otherDataView.getDataRegion(), otherDataView.getRenderContext());
    }

    protected void renderGraph(RenderContext ctx, Writer out, String description) throws IOException
    {
        try
        {
            int wellId = (Integer) ctx.getRow().get("RowId");
            ActionURL src = PageFlowUtil.urlFor(WellController.Action.showGraph, ctx.getContainer());
            src.addParameter("wellId", Integer.toString(wellId));
            src.addParameter("graph", description);
            out.write("<img src=\"" + src + "\">");
        }
        catch (Exception e)
        {
            out.write(e.toString());
        }

    }

    protected void renderDataColumn(RenderContext ctx, Writer out, DisplayColumn column) throws IOException, SQLException
    {
        column.renderGridHeaderCell(ctx, out);
        column.renderGridDataCell(ctx, out);
    }

    protected void _renderDataRegion(RenderContext ctx, Writer out) throws IOException, SQLException
    {
        DataRegion oldRegion = ctx.getCurrentRegion();
        try
        {
            DataRegion region = getDataRegion();
            ctx.setCurrentRegion(region);
            ctx.setResultSet(region.getResultSet(ctx));
            region.writeFilterHtml(ctx, out);
            List<DisplayColumn> dataColumns = new ArrayList();
            List<GraphColumn> graphColumns = new ArrayList();

            for (DisplayColumn dc : region.getDisplayColumns())
            {
                if (dc instanceof GraphColumn)
                {
                    graphColumns.add((GraphColumn) dc);
                }
                else
                {
                    dataColumns.add(dc);
                }
            }

            out.write("<table class=\"dataregion_header\">\n");
            region.getButtonBar(DataRegion.MODE_GRID).render(ctx, out);
            out.write("</table>\n");

            out.write("<table class=\"dataRegion\">\n");
            ResultSet rs = ctx.getResultSet();
            Map rowMap = null;
            while (rs.next())
            {
                out.write("<tr>");
                for (DisplayColumn col : dataColumns)
                {
                    col.renderGridHeaderCell(ctx, out);
                }
                out.write("<th width=\"100%\">&nbsp;</th>");
                out.write("</tr>");
                rowMap = ResultSetUtil.mapRow(rs, rowMap);
                ctx.setRow(rowMap);
                out.write("<tr>");
                for (DisplayColumn col : dataColumns)
                {
                    col.renderGridDataCell(ctx, out);
                }
                out.write("</tr><tr><td colspan=\"" + (dataColumns.size() + 1) + "\">");
                for (GraphColumn graphColumn : graphColumns)
                {
                    graphColumn.renderGraph(ctx, out);
                }
                out.write("</td></tr>");
            }
            out.write("</table>\n");

            out.write("<table class=\"dataregion_footer\">\n");
            region.getButtonBar(DataRegion.MODE_DETAILS).render(ctx, out);
            out.write("</table>\n");
        }
        finally
        {
            ctx.setCurrentRegion(oldRegion);
        }
    }
}
