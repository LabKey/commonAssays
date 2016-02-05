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
import org.labkey.api.data.Table;
import org.labkey.api.security.User;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.view.ActionURL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: adam
 * Date: Sep 11, 2006
 * Time: 3:36:03 PM
 */
public class ProteinResultSetSpectrumIterator extends ResultSetSpectrumIterator
{
    private ActionURL _currentUrl;
    private AbstractMS2RunView _peptideView;
    private String _extraWhere;

    public ProteinResultSetSpectrumIterator(List<MS2Run> runs, ActionURL currentUrl, AbstractMS2RunView peptideView, String extraWhere, User user)
    {
        _rs = new ProteinSpectrumResultSet(runs, user);
        _currentUrl = currentUrl;
        _peptideView = peptideView;
        _extraWhere = extraWhere;
    }

    public class ProteinSpectrumResultSet extends MS2ResultSet
    {
        private final User _user;

        ProteinSpectrumResultSet(List<MS2Run> runs, User user)
        {
            _user = user;
            _iter = runs.iterator();
        }

        ResultSet getNextResultSet() throws SQLException
        {
            SQLFragment sql;
            String joinSql;

            // Peptide columns are coming from ms2.SimplePeptides, so translate the search-engine specific scoring
            // column aliases back to Score1, etc. Along with addition of RetentionTime, fixes issue 24836
            String columnNames = "Peptide, TrimmedPeptide, RetentionTime, RawScore AS Score1, DeltaCN AS Score2, ZScore AS Score3, SPRank AS Score4, Expect AS Score5, NextAA, PrevAA, Scan, Charge, Fraction, PrecursorMass, MZ, Spectrum";
            if (_peptideView instanceof StandardProteinPeptideView)
            {
                sql = ProteinManager.getPeptideSql(_currentUrl, _iter.next(), _extraWhere, Table.ALL_ROWS, columnNames + ", Run", _user);
                joinSql = sql.getSQL().replaceFirst("RIGHT OUTER JOIN", "LEFT OUTER JOIN (SELECT Run AS fRun, Scan AS fScan, Spectrum FROM " + MS2Manager.getTableInfoSpectra() + ") spec ON Run=fRun AND Scan = fScan\nRIGHT OUTER JOIN");
            }
            else
            {
                sql = ProteinManager.getProteinProphetPeptideSql(_currentUrl, _iter.next(), _extraWhere, Table.ALL_ROWS, columnNames + ", ms2.SimplePeptides.Run", _user);
                joinSql = sql.getSQL().replaceFirst("WHERE", "LEFT OUTER JOIN (SELECT s.Run AS fRun, s.Scan AS fScan, Spectrum FROM " + MS2Manager.getTableInfoSpectra() + " s) spec ON " + MS2Manager.getTableInfoSimplePeptides() + ".Run=fRun AND Scan = fScan WHERE ");
            }

            return new SqlSelector(ProteinManager.getSchema(), joinSql, sql.getParams()).getResultSet(false);
        }
    }
}
