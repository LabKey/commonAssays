package org.labkey.ms2.peptideview;

import org.labkey.api.data.ResultSetWrapper;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.ms2.MS2Run;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

/**
 * User: adam
 * Date: Aug 7, 2006
 * Time: 4:32:33 PM
 */
public abstract class MS2ResultSet extends ResultSetWrapper
{
    protected Iterator<MS2Run> _iter;
    protected SimpleFilter _filter;
    protected Sort _sort;

    protected MS2ResultSet()
    {
        super(null);
    }

    MS2ResultSet(List<MS2Run> runs, SimpleFilter filter, Sort sort)
    {
        super(null);
        _iter = runs.iterator();
        _filter = filter;
        _sort = sort;
    }

    private boolean prepareNextResultSet() throws SQLException
    {
        if (!_iter.hasNext())
            return false;

        if (null != resultset)
            super.close();

        resultset = getNextResultSet();

        return true;
    }

    abstract ResultSet getNextResultSet() throws SQLException;

    public boolean next() throws SQLException
    {
        // Will be null first time through... initialize now (doing so in constructor would be too early)
        if (null == resultset)
        {
            if (!prepareNextResultSet())
                return false;
        }

        // Loop until we find a result set with a row of data
        while(true)
        {
            // Still more rows in current result set
            if (super.next())
                return true;

            // Move to next result set
            if (!prepareNextResultSet())
                return false;
        }
    }
}
