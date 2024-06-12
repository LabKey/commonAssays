/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

package org.labkey.api.protein.annotation;

import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class CustomAnnotationImportHelper implements OntologyManager.ImportHelper
{
    private final String _setLsid;
    private final String _lookupStringColumnName;
    private final PreparedStatement _stmt;
    private final Connection _conn;
    private int _count;

    public CustomAnnotationImportHelper(PreparedStatement stmt, Connection conn, String lsid, String lookupStringColumnName)
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

    @Override
    public String beforeImportObject(Map<String, Object> map) throws SQLException
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

    @Override
    public void afterBatchInsert(int currentRow)
    {
    }

    @Override
    public void updateStatistics(int currentRow)
    {
    }
}
