package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.ms2.MS2Run;
import org.labkey.api.view.ViewContext;
import org.labkey.api.protein.ProteinManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: adam
 * Date: Aug 8, 2006
 * Time: 4:27:44 PM
 */
public class MultiRunRenderContext extends RenderContext
{
    List<MS2Run> _runs;

    public MultiRunRenderContext(ViewContext ctx, List<MS2Run> runs)
    {
        super(ctx);
        _runs = runs;
    }

    protected ResultSet selectForDisplay(TableInfo table, ColumnInfo[] columns, SimpleFilter filter, Sort sort, int maxRows) throws SQLException
    {
        return new MultiRunResultSet(_runs, table, columns, filter, sort, maxRows, getCache());
    }


    public static class MultiRunResultSet extends MS2ResultSet
    {
        private TableInfo _table;
        private ColumnInfo[] _columns;
        private int _maxRows;
        private boolean _cache;

        MultiRunResultSet(List<MS2Run> runs, TableInfo table, ColumnInfo[] columns, SimpleFilter filter, Sort sort, int maxRows, boolean cache)
        {
            super(runs, filter, sort);
            _table = table;
            _columns = columns;
            _maxRows = maxRows;
            _cache = cache;
        }


        ResultSet getNextResultSet() throws SQLException
        {
            ProteinManager.replaceRunCondition(_filter, _iter.next(), null);
            return Table.selectForDisplay(_table, _columns, _filter, _sort, _maxRows, _cache);
        }
    }
}
