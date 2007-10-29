package org.labkey.ms2.query;

import org.labkey.api.data.*;
import org.labkey.ms2.Protein;

import java.util.Set;
import java.util.List;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class QueryAACoverageColumn extends PeptideAggregrationDisplayColumn
{
    private final ColumnInfo _sequenceColumn;

    public QueryAACoverageColumn(ColumnInfo sequenceColumn, ColumnInfo seqIdColumn, ColumnInfo peptideColumn)
    {
        super(seqIdColumn, peptideColumn, "AA Coverage");

        _sequenceColumn = sequenceColumn;

        setFormatString("0.0%");
        setTsvFormatString("0.00");
        setWidth("90");
        setTextAlign("right");
    }


    public ColumnInfo getColumnInfo()
    {
        return _sequenceColumn;
    }

    public Class getValueClass()
    {
        return Double.class;
    }

    protected Object calculateValue(RenderContext ctx, List<String> peptides)
        throws SQLException
    {
        Protein protein = new Protein();
        protein.setSequence(ctx.getResultSet().getString(_sequenceColumn.getAlias()));
        protein.setPeptides(peptides.toArray(new String[peptides.size()]));
        return protein.getAAPercent();
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        set.add(_sequenceColumn);
    }
}
