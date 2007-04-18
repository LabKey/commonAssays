package org.labkey.ms2.peptideview;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.DataRegion;
import org.labkey.api.view.ViewURLHelper;

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

    public QueryPeptideDataRegion(List<DisplayColumn> allColumns, String groupingColumnName, ViewURLHelper url, int resultSetRowLimit, int outerGroupLimit)
    {
        super(groupingColumnName, url);
        _allColumns = allColumns;
        _resultSetRowLimit = resultSetRowLimit;
        _outerGroupLimit = outerGroupLimit;
        setShadeAlternatingRows(true);
    }


    public ResultSet getResultSet(RenderContext ctx) throws SQLException, IOException
    {
        List<DisplayColumn> realColumns = getDisplayColumnList();
        setDisplayColumnList(_allColumns);
        ResultSet rs = super.getResultSet(ctx);
        setDisplayColumnList(realColumns);

        _groupedRS = new GroupedResultSet(rs, _uniqueColumnName, _resultSetRowLimit, _outerGroupLimit);
        return _groupedRS;
    }

    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
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
