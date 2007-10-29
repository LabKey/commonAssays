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

    public PeptideAggregrationDisplayColumn(ColumnInfo groupingColumn, ColumnInfo peptideColumn, String caption)
    {
        _groupingColumn = groupingColumn;
        _peptideColumn = peptideColumn;
        setCaption(caption);
    }

    public ColumnInfo getColumnInfo()
    {
        return _groupingColumn;
    }

    public Object getValue(RenderContext ctx)
    {
        ResultSet originalRS = ctx.getResultSet();
        ResultSet rs = originalRS;

        boolean closeRS = false;
        try
        {
            if (originalRS instanceof GroupedResultSet)
            {
                rs = ((GroupedResultSet)originalRS).getNextResultSet();
                closeRS = true;
            }
            Object groupingValue = originalRS.getObject(_groupingColumn.getAlias());
            List<String> peptides = (List<String>)ctx.get("PeptideList");
            Object cachedGroupingValue = ctx.get("PeptideListGroupingValue");
            if (peptides == null || cachedGroupingValue == null || !cachedGroupingValue.equals(groupingValue))
            {
                peptides = new ArrayList<String>();

                peptides.add(originalRS.getString(_peptideColumn.getAlias()));
                int originalRow = originalRS.getRow();

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
                originalRS.absolute(originalRow);

                ctx.put("PeptideList", peptides);
                ctx.put("PeptideListGroupingValue", groupingValue);
            }

            return calculateValue(ctx, peptides);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (closeRS) { try { rs.close(); } catch (SQLException e) {} }
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
