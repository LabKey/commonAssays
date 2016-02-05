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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.ResultSet;
import java.util.List;

/**
 * Pulls peptides and any associated spectra from the database for query-based MS2 views
 *
 * User: jeckels
 * Date: Nov 8, 2013
 */
public class QueryResultSetSpectrumIterator extends ResultSetSpectrumIterator
{
    private SQLFragment _sql;

    public QueryResultSetSpectrumIterator(List<MS2Run> runs, SQLFragment sql)
    {
        _sql = sql;
        _rs = new QuerySpectrumResultSet(runs);
    }

    public class QuerySpectrumResultSet extends MS2ResultSet
    {
        QuerySpectrumResultSet(List<MS2Run> runs)
        {
            _iter = runs.iterator();
        }

        ResultSet getNextResultSet()
        {
            MS2Run ms2Run = _iter.next();
            SQLFragment sql = getBaseResultSetSql();
            sql.append(" WHERE pep.RowId IN (");
            sql.append(_sql);
            sql.append(")) X WHERE Run = ?");
            sql.add(ms2Run.getRun());

            return new SqlSelector(ProteinManager.getSchema(), sql).getResultSet(false);
        }
    }
}
