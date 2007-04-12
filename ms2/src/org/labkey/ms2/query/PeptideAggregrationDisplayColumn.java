package org.labkey.ms2.query;

import org.labkey.api.data.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public abstract class PeptideAggregrationDisplayColumn extends SimpleDisplayColumn
{
    private ColumnInfo _groupingColumn;
    private ColumnInfo _peptideColumn;

    public PeptideAggregrationDisplayColumn(ColumnInfo groupingColumn, ColumnInfo peptideColumn)
    {
        _groupingColumn = groupingColumn;
        _peptideColumn = peptideColumn;
    }

    public ColumnInfo getColumnInfo()
    {
        return _groupingColumn;
    }

    public Object getValue(RenderContext ctx)
    {
        ResultSet rs = ctx.getResultSet();
        try
        {
            List<String> peptides = (List<String>)ctx.getRow().get("PeptideList");
            if (peptides == null)
            {
                peptides = new ArrayList<String>();
                Object groupingValue = rs.getObject(_groupingColumn.getAlias());

                peptides.add(rs.getString(_peptideColumn.getAlias()));
                int originalRow = rs.getRow();

                while (rs.next())
                {
                    if (rs.getObject(_groupingColumn.getAlias()).equals(groupingValue))
                    {
                        peptides.add(rs.getString(_peptideColumn.getAlias()));
                    }
                    else
                    {
                        break;
                    }
                }
                rs.absolute(originalRow);

                ctx.getRow().put("PeptidesList", peptides);
            }

            return calculateValue(ctx, peptides);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    protected abstract Object calculateValue(RenderContext ctx, List<String> peptides)
        throws SQLException;


    public boolean isFilterable()
    {
        return false;
    }

    public boolean isSortable()
    {
        return false;
    }

    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        set.add(_groupingColumn);
        set.add(_peptideColumn);
    }

    public abstract Class getValueClass();
}
