package cpas.ms2.peptideview;

import org.fhcrc.cpas.data.SQLFragment;
import org.fhcrc.cpas.data.Table;
import org.fhcrc.cpas.ms2.MS2Run;
import org.fhcrc.cpas.ms2.MS2Manager;
import org.fhcrc.cpas.protein.ProteinManager;
import org.fhcrc.cpas.view.ViewURLHelper;

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
    private ViewURLHelper _currentUrl;
    private AbstractPeptideView _peptideView;
    private String _extraWhere;

    public ProteinResultSetSpectrumIterator(List<MS2Run> runs, ViewURLHelper currentUrl, AbstractPeptideView peptideView, String extraWhere)
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

            if (_peptideView instanceof StandardProteinPeptideView)
                sql = ProteinManager.getPeptideSql(_currentUrl, _iter.next(), _extraWhere, 0, "Charge, PrecursorMass, MZ, Spectrum");
            else
                sql = ProteinManager.getProteinProphetPeptideSql(_currentUrl, _iter.next(), _extraWhere, 0, "Charge, PrecursorMass, MZ, Spectrum");

            String joinSql = sql.toString().replaceFirst("INNER JOIN", "INNER JOIN (SELECT Run AS fRun, Scan AS fScan, Spectrum FROM " + MS2Manager.getTableInfoSpectra() + ") spec ON Run=fRun AND Scan = fScan\nINNER JOIN");

            return Table.executeQuery(ProteinManager.getSchema(), joinSql, sql.getParams().toArray(), 0, false);
        }
    }
}
