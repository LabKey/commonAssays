/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Render row of values and row of graphs for every row in the grid.
 * Replaces GraphView.
 *
 * User: kevink
 * Date: 7/20/11
 */
public class GraphDataRegion extends DataRegion
{
    private List<GraphColumn> _graphColumns;
    private List<DisplayColumn> _dataColumns;

    // Remove the graph columns from the set of display columns
    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        if (_dataColumns != null)
            return _dataColumns;

        List<DisplayColumn> renderers = super.getDisplayColumns();
        _dataColumns = new ArrayList<>(renderers.size());
        _graphColumns = new ArrayList<>(10);

        for (DisplayColumn dc : renderers)
        {
            if (dc instanceof GraphColumn)
                _graphColumns.add((GraphColumn) dc);
            else
                _dataColumns.add(dc);
        }

        return _dataColumns;
    }

    // Add the graph columns back into the set of selected columns
    @Override
    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);

        columns.addAll(RenderContext.getSelectColumns((List)_graphColumns, getTable()));
    }

    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        super.renderTableRow(ctx, out, showRecordSelectors, renderers, rowIndex);

        out.write("<tr");
        String rowClass = getRowClass(ctx, rowIndex);
        if (rowClass != null)
            out.write(" class=\"" + rowClass + "\"");
        out.write(">");
        // skip one cell for the [details] column
        out.write("<td>&nbsp;</td>");
        out.write("<td colspan=\"" + (renderers.size()) + "\">");

        for (GraphColumn graphColumn : _graphColumns)
        {
            if (graphColumn.isVisible(ctx))
                graphColumn.renderGraph(ctx, out);
        }
        out.write("</td></tr>\n");
    }
}
