package org.labkey.ms2.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;

import java.util.List;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class PeptideCountCoverageColumn extends PeptideAggregrationDisplayColumn
{
    public PeptideCountCoverageColumn(ColumnInfo colInfo, ColumnInfo peptideColumn, String caption)
    {
        super(colInfo, peptideColumn, caption);
    }

    public Class getValueClass()
    {
        return Integer.class;
    }

    protected Object calculateValue(RenderContext ctx, List<String> peptides) throws SQLException
    {
        return peptides.size();
    }
}
