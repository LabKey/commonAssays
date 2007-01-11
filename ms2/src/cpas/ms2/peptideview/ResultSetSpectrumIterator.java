package cpas.ms2.peptideview;

import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.Table;
import org.labkey.api.ms2.*;
import org.labkey.api.protein.ProteinManager;
import org.fhcrc.cpas.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: adam
 * Date: May 10, 2006
 * Time: 10:41:26 AM
 */
public class ResultSetSpectrumIterator implements SpectrumIterator
{
    protected ResultSet _rs;

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


    protected class ResultSetSpectrum implements Spectrum
    {
        Pair<float[], float[]> _pair = null;

        public float[] getX()
        {
            if (null == _pair)
                getSpectrum();

            return _pair.first;
        }

        public float[] getY()
        {
            if (null == _pair)
                getSpectrum();

            return _pair.second;
        }

        private void getSpectrum()
        {
            byte[] bytes;

            try
            {
                bytes = _rs.getBytes("Spectrum");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }

            _pair = SpectrumLoader.byteArrayToFloatArrays(bytes);
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
            ProteinManager.replaceRunCondition(_filter, _iter.next(), null);

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT Charge, PrecursorMass, MZ, Spectrum FROM (SELECT pep.*, Spectrum FROM ");  // Use sub-SELECT to disambiguate filters/sorts on Scan & Fraction
            sql.append(MS2Manager.getTableInfoSpectraData());
            sql.append(" sd INNER JOIN ");
            sql.append(MS2Manager.getTableInfoPeptides());
            sql.append(" pep ON sd.Fraction = pep.Fraction AND sd.Scan = pep.Scan) X\n");
            sql.append(_filter.getWhereSQL(MS2Manager.getSqlDialect()));
            sql.append('\n');
            sql.append(_sort.getOrderByClause(MS2Manager.getSqlDialect()));

            return Table.executeQuery(MS2Manager.getSchema(), sql.toString(), _filter.getWhereParams(MS2Manager.getTableInfoPeptides()).toArray(), 0, false);
        }
    }
}
