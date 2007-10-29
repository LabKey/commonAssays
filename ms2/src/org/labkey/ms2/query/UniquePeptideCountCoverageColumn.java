package org.labkey.ms2.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;

import java.util.List;
import java.util.HashSet;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class UniquePeptideCountCoverageColumn extends PeptideAggregrationDisplayColumn
{
    public UniquePeptideCountCoverageColumn(ColumnInfo colInfo, ColumnInfo peptideColumn, String caption)
    {
        super(colInfo, peptideColumn, caption);
    }

    protected Object calculateValue(RenderContext ctx, List<String> peptides) throws SQLException
    {
        return new HashSet<String>(peptides).size();
    }

    public Class getValueClass()
    {
        return Integer.class;
    }
}
