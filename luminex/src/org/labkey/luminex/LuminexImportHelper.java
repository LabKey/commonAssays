package org.labkey.luminex;

import org.labkey.api.data.Parameter;
import org.labkey.api.exp.OntologyManager;

import java.sql.SQLException;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 4, 2011
 */
public class LuminexImportHelper implements OntologyManager.UpdateableTableImportHelper
{
    @Override
    public void afterImportObject(Map<String, Object> map) throws SQLException
    {

    }

    @Override
    public void bindAdditionalParameters(Map<String, Object> map, Parameter.ParameterMap target)
    {

    }

    @Override
    public String beforeImportObject(Map<String, Object> map) throws SQLException
    {
        return (String)map.get("LSID");
    }

    @Override
    public void afterBatchInsert(int currentRow) throws SQLException
    {

    }

    @Override
    public void updateStatistics(int currentRow) throws SQLException
    {
        
    }
}
