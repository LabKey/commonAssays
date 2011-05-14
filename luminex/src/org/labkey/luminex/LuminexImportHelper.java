/*
 * Copyright (c) 2011 LabKey Corporation
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
