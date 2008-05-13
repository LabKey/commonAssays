/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.DataRegion;
import org.labkey.api.view.ActionURL;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

/**
 * User: jeckels
 */
public class QueryPeptideDataRegion extends AbstractProteinDataRegion
{
    private final List<DisplayColumn> _allColumns;
    private final int _resultSetRowLimit;
    private final int _outerGroupLimit;

    public QueryPeptideDataRegion(List<DisplayColumn> allColumns, String groupingColumnName, ActionURL url, int resultSetRowLimit, int outerGroupLimit)
    {
        super(groupingColumnName, url);
        _allColumns = allColumns;
        _resultSetRowLimit = resultSetRowLimit;
        _outerGroupLimit = outerGroupLimit;
        setShadeAlternatingRows(true);
    }


    public ResultSet getResultSet(RenderContext ctx, boolean async) throws SQLException, IOException
    {
        List<DisplayColumn> realColumns = getDisplayColumns();
        setDisplayColumns(_allColumns);
        ResultSet rs = super.getResultSet(ctx, async);
        setDisplayColumns(realColumns);

        _groupedRS = new GroupedResultSet(rs, _uniqueColumnName, _resultSetRowLimit, _outerGroupLimit);
        return _groupedRS;
    }

    @Override
    protected void renderTableRow(RenderContext ctx, Writer out, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        super.renderTableRow(ctx, out, renderers, rowIndex);

        _groupedRS.previous();
        ResultSet nestedRS = _groupedRS.getNextResultSet();

        // Validate that the inner and outer result sets are sorted the same
        while (nestedRS.next())
        {
            if (!ctx.getRow().get(_uniqueColumnName).equals(nestedRS.getInt(_uniqueColumnName)))
            {
                throw new IllegalArgumentException("Ids do not match for the outer and inner result sets");
            }
        }
        nestedRS.beforeFirst();

        renderNestedGrid(out, ctx, nestedRS, rowIndex);
        nestedRS.close();
    }

    public DataRegion getNestedRegion()
    {
        return _nestedRegion;
    }
}
