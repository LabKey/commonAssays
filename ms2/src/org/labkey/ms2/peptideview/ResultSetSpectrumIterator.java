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

import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.util.Pair;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.Spectrum;
import org.labkey.ms2.SpectrumException;
import org.labkey.ms2.SpectrumImporter;
import org.labkey.ms2.SpectrumIterator;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: adam
 * Date: May 10, 2006
 * Time: 10:41:26 AM
 */
public class ResultSetSpectrumIterator implements SpectrumIterator
{
    protected ResultSet _rs;

    /** Cache the last 100 spectra loaded, especially useful for Mascot where there many be multiple hits per spectra */
    private Map<Pair<Integer, Integer>, Pair<float[], float[]>> _lruCache = new LinkedHashMap<Pair<Integer, Integer>, Pair<float[], float[]>>()
    {
        /** This method is called just after a new entry has been added */
        public boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 100;
        }
    };

    protected ResultSetSpectrumIterator()
    {
    }

    public ResultSetSpectrumIterator(List<MS2Run> runs, SimpleFilter filter, Sort sort)
    {
        _rs = new SpectrumResultSet(runs, filter, sort);
    }

    public boolean hasNext()
    {
        try
        {
            if (_rs.next())
                return true;

            _rs.close();
            _rs = null;
            return false;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public Spectrum next()
    {
        return new ResultSetSpectrum();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public void close()
    {
        if (_rs != null)
        {
            try
            {
                _rs.close();
                _rs = null;
            }
            catch (SQLException ignored) {}
        }
    }

    protected class ResultSetSpectrum implements Spectrum
    {
        Pair<float[], float[]> _pair = null;

        public float[] getX()
        {
            if (null == _pair)
                loadSpectrum();

            return _pair.first;
        }

        public float[] getY()
        {
            if (null == _pair)
                loadSpectrum();

            return _pair.second;
        }

        private void loadSpectrum()
        {
            try
            {
                byte[] bytes = _rs.getBytes("Spectrum");

                if (null != bytes)
                {
                    _pair = SpectrumImporter.byteArrayToFloatArrays(bytes);
                }
                else
                {
                    int fraction = _rs.getInt("Fraction");
                    int scan = _rs.getInt("Scan");
                    Pair<Integer, Integer> key = new Pair<>(fraction, scan);
                    _pair = _lruCache.get(key);
                    if (_pair == null)
                    {
                        _pair = MS2Manager.getSpectrumFromFile(fraction, scan);
                        _lruCache.put(key, _pair);
                    }
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            catch (SpectrumException e)
            {
                _pair = new Pair<>(new float[0], new float[0]);  // Ignore spectrum exceptions -- just return empty spectrum
            }
        }

        public int getCharge()
        {
            try
            {
                return _rs.getInt("Charge");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getTrimmedSequence()
        {
            try
            {
                return _rs.getString("TrimmedPeptide");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public int getFraction()
        {
            try
            {
                return _rs.getInt("Fraction");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public int getRun()
        {
            try
            {
                return _rs.getInt("Run");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getSequence()
        {
            try
            {
                return _rs.getString("Peptide");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getNextAA()
        {
            try
            {
                return _rs.getString("NextAA");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getPrevAA()
        {
            try
            {
                return _rs.getString("PrevAA");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public double getMZ()
        {
            try
            {
                return _rs.getDouble("MZ");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        @Override
        public double getScore(int index)
        {
            try
            {
                return _rs.getDouble("Score" + index);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        @Override
        public Double getRetentionTime()
        {
            try
            {
                double retentionTime = _rs.getDouble("RetentionTime");
                return _rs.wasNull() ? null : retentionTime;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public double getPrecursorMass()
        {
            try
            {
                return _rs.getDouble("PrecursorMass");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
    }


    private static class SpectrumResultSet extends MS2ResultSet
    {
        public SpectrumResultSet(List<MS2Run> runs, SimpleFilter filter, Sort sort)
        {
            super(runs, filter, sort);
        }

        public ResultSet getNextResultSet() throws SQLException
        {
            ProteinManager.replaceRunCondition(_filter, null, _iter.next());

            SQLFragment sql = getBaseResultSetSql();
            sql.append(") X\n");
            sql.append(_filter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
            sql.append('\n');
            sql.append(_sort.getOrderByClause(MS2Manager.getSqlDialect()));

            return new SqlSelector(MS2Manager.getSchema(), sql.getSQL(), _filter.getWhereParams(MS2Manager.getTableInfoPeptides())).getResultSet(false);
        }
    }
}
