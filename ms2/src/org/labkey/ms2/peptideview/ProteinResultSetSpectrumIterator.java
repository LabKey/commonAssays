/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
import org.labkey.api.data.Table;
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

    public ProteinResultSetSpectrumIterator(List<MS2Run> runs, ActionURL currentUrl, AbstractMS2RunView peptideView, String extraWhere)
    {
        _rs = new ProteinSpectrumResultSet(runs);
        _currentUrl = currentUrl;
        _peptideView = peptideView;
        _extraWhere = extraWhere;
    }

    public class ProteinSpectrumResultSet extends MS2ResultSet
    {
        ProteinSpectrumResultSet(List<MS2Run> runs)
        {
            _iter = runs.iterator();
        }

        ResultSet getNextResultSet() throws SQLException
        {
            SQLFragment sql;
            String joinSql;

            if (_peptideView instanceof StandardProteinPeptideView)
            {
                sql = ProteinManager.getPeptideSql(_currentUrl, _iter.next(), _extraWhere, 0, 0, "Scan, Charge, Fraction, PrecursorMass, MZ, Spectrum");
                joinSql = sql.toString().replaceFirst("RIGHT OUTER JOIN", "LEFT OUTER JOIN (SELECT Run AS fRun, Scan AS fScan, Spectrum FROM " + MS2Manager.getTableInfoSpectra() + ") spec ON Run=fRun AND Scan = fScan\nRIGHT OUTER JOIN");
            }
            else
            {
                sql = ProteinManager.getProteinProphetPeptideSql(_currentUrl, _iter.next(), _extraWhere, 0, 0, "Scan, Charge, Fraction, PrecursorMass, MZ, Spectrum");
                joinSql = sql.toString().replaceFirst("WHERE", "LEFT OUTER JOIN (SELECT s.Run AS fRun, s.Scan AS fScan, Spectrum FROM " + MS2Manager.getTableInfoSpectra() + " s) spec ON " + MS2Manager.getTableInfoSimplePeptides() + ".Run=fRun AND Scan = fScan WHERE ");
            }

            return Table.executeQuery(ProteinManager.getSchema(), joinSql, sql.getParams().toArray(), 0, false);
        }
    }
}
