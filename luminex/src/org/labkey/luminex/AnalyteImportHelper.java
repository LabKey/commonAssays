package org.labkey.luminex;

import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.ObjectProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Jul 19, 2007
 */
public class AnalyteImportHelper implements OntologyManager.ImportHelper
{
    private final Collection<LuminexExcelDataHandler.Analyte> _analytes;
    private final String _namePropertyURI;

    public AnalyteImportHelper(Collection<LuminexExcelDataHandler.Analyte> analytes, String namePropertyURI)
    {
        _analytes = analytes;
        _namePropertyURI = namePropertyURI;
    }

    public String beforeImportObject(Map map) throws SQLException
    {
        String name = (String)map.get(_namePropertyURI);
        for (LuminexExcelDataHandler.Analyte analyte : _analytes)
        {
            if (analyte.getName().equals(name))
            {
                return analyte.getLSID();
            }
        }
        throw new IllegalStateException("Could not find LSID for Analyte with name " + name);
    }

    public void afterImportObject(String lsid, ObjectProperty[] props) throws SQLException
    {

    }

}
