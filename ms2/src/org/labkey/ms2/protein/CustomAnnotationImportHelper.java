package org.labkey.ms2.protein;

import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

/**
 * User: jeckels
* Date: May 2, 2007
*/
public class CustomAnnotationImportHelper implements OntologyManager.ImportHelper
{
    private final String _setLsid;
    private final String _lookupStringColumnName;
    private final PreparedStatement _stmt;
    private final Connection _conn;
    private int _count;

    CustomAnnotationImportHelper(PreparedStatement stmt, Connection conn, String lsid, String lookupStringColumnName)
    {
        _stmt = stmt;
        _conn = conn;
        _setLsid = lsid;
        _lookupStringColumnName = lookupStringColumnName;
    }

    public static String convertLookup(Object lookup)
    {
        if (lookup == null)
        {
            return null;
        }
        return lookup.toString().trim();
    }

    public String beforeImportObject(Map map) throws SQLException
    {
        String lookupString = convertLookup(map.get(_lookupStringColumnName));

        String lsid = new Lsid(_setLsid + "-" + lookupString).toString();

        _stmt.setString(2, lookupString);
        _stmt.setString(3, lsid);
        _stmt.addBatch();
        _count++;
        if (_count % 5000 == 0)
        {
            _stmt.executeBatch();
            _conn.commit();
        }

        return lsid;
    }

    public void afterImportObject(String lsid, ObjectProperty[] props) throws SQLException
    {
    }
}
