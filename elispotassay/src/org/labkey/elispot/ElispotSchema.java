/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

package org.labkey.elispot;

import org.labkey.api.data.*;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.elispot.query.ElispotRunAntigenTable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElispotSchema extends AssaySchema
{
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    public static final String ANTIGEN_STATS_TABLE_NAME = "AntigenStats";

    public ElispotSchema(User user, Container container, ExpProtocol protocol)
    {
        super("Assay", user, container, ExperimentService.get().getSchema(), protocol);
    }

    public static String getAssayTableName(ExpProtocol protocol, String table)
    {
        return protocol.getName() + " " + table;
    }

    public Set<String> getTableNames()
    {
        return Collections.singleton(getAssayTableName(getProtocol(), ANTIGEN_STATS_TABLE_NAME));
    }

    public TableInfo createTable(String name)
    {
        String lname = name.toLowerCase();
        String protocolPrefix = getProtocol().getName().toLowerCase() + " ";
        if (lname.startsWith(protocolPrefix))
        {
            name = name.substring(protocolPrefix.length());
            if (ANTIGEN_STATS_TABLE_NAME.equalsIgnoreCase(name))
            {
                return new ElispotRunAntigenTable(AssayService.get().createSchema(getUser(), getContainer()), getProtocol());
            }
        }
        return null;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("elispot");
    }

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol, String propertyPrefix) throws SQLException
    {
        String propPrefix = new Lsid(propertyPrefix, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);

        return Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);
    }

}
