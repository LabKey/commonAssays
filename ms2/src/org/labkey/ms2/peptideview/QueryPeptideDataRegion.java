package org.labkey.ms2.peptideview;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.DataColumn;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.ProteinGroupProteins;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class QueryPeptideDataRegion extends AbstractProteinDataRegion
{
    private final List<DisplayColumn> _allColumns;
    private final DataColumn _groupIdColumn;
    private final MS2Run[] _runs;

    public QueryPeptideDataRegion(List<DisplayColumn> allColumns, DataColumn groupIdColumn, MS2Run[] runs)
    {
        super(groupIdColumn.getColumnInfo().getAlias());
        _allColumns = allColumns;
        _groupIdColumn = groupIdColumn;
        _runs = runs;
        setShadeAlternatingRows(true);
    }


    public ResultSet getResultSet(RenderContext ctx) throws SQLException, IOException
    {
        List<DisplayColumn> realColumns = getDisplayColumnList();
        setDisplayColumnList(_allColumns);
        ResultSet rs = super.getResultSet(ctx);
        setDisplayColumnList(realColumns);

        String columnAlias = _groupIdColumn.getColumnInfo().getAlias();

        _groupedRS = new GroupedResultSet(rs, columnAlias);
        return rs;
    }

    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
    {
        super.renderTableRow(ctx, out, renderers, rowIndex);

        _groupedRS.previous();
        ResultSet nestedRS = _groupedRS.getNextResultSet();

        // Validate that the inner and outer result sets are sorted the same
        while (nestedRS.next())
        {
            if (!ctx.getRow().get(_groupIdColumn.getColumnInfo().getAlias()).equals(nestedRS.getInt(_groupIdColumn.getColumnInfo().getAlias())))
            {
                throw new IllegalArgumentException("ProteinGroup ids do not match for the outer and inner queries");
            }
        }
        nestedRS.beforeFirst();

        renderNestedGrid(out, ctx, nestedRS, rowIndex);
        nestedRS.close();
    }

    public ProteinGroupProteins lookupProteinGroupProteins()
    {
        return new ProteinGroupProteins(_runs);
    }

}
