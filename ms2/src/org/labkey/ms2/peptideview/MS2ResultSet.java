/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.labkey.api.data.ResultSetWrapper;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    // Subclasses should call at the end of their constructors -- this pre-fetches the first result set which ensures
    // that calls to getMetaData(), etc. will work before next() is called.
    protected void init()
    {
        try
        {
            prepareNextResultSet();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    @Override
    public void close() throws SQLException
    {
        // It's possible that we're between ResultSets and an exception has been thrown, and we're being closed
        // in a finally block. If so, we don't want to blow up here with a NPE
        if (null != resultset)
            super.close();
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

    /**
     * Wraps the join of Peptides, PeptidesData, & SpectraData in a subselect to disambiguate scan & fraction column references in filters/sorts
     *
     */
    protected SQLFragment getBaseResultSetSql()
    {
        SQLFragment sql = new SQLFragment();

        // Pull out peptides and spectra (if in the database) for the current run
        sql.append("SELECT TrimmedPeptide, Peptide, NextAA, PrevAA, Fraction, Scan, Charge, PrecursorMass, MZ, Run, Spectrum, RetentionTime, Score1, Score2, Score3, Score4, Score5, Score6, HitRank, QueryNumber, Decoy FROM (SELECT pep.*, pd.Score1, pd.Score2, pd.Score3, pd.Score4, pd.Score5, pd.Score6, Spectrum FROM ");  // Use sub-SELECT to disambiguate filters/sorts on Scan & Fraction
        sql.append(MS2Manager.getTableInfoPeptides(), "pep");
        sql.append(" INNER JOIN ");
        sql.append(MS2Manager.getTableInfoPeptidesData(), "pd");
        sql.append(" ON pep.RowId = pd.RowId ");
        sql.append(" LEFT OUTER JOIN ");         // LEFT OUTER so we also get peptides without spectra in the database
        sql.append(MS2Manager.getTableInfoSpectraData(), "sd");
        sql.append(" ON sd.Fraction = pep.Fraction AND sd.Scan = pep.Scan");

        return sql;
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
