/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.labkey.api.exp.OntologyManager;
import org.labkey.luminex.model.Analyte;

import java.util.Map;
import java.util.Collection;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Jul 19, 2007
 */
public class AnalyteImportHelper implements OntologyManager.ImportHelper
{
    private final Collection<Analyte> _analytes;
    private final String _namePropertyURI;

    public AnalyteImportHelper(Collection<Analyte> analytes, String namePropertyURI)
    {
        _analytes = analytes;
        _namePropertyURI = namePropertyURI;
    }

    public String beforeImportObject(Map<String, Object> map) throws SQLException
    {
        String name = (String)map.get(_namePropertyURI);
        for (Analyte analyte : _analytes)
        {
            if (analyte.getName().equals(name))
            {
//                return analyte.getLSID();
            }
        }
        throw new IllegalStateException("Could not find LSID for Analyte with name " + name);
    }

    public void afterBatchInsert(int currentRow) throws SQLException
    {

    }

    public void updateStatistics(int currentRow) throws SQLException
    {
    }

}
