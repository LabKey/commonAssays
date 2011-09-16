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
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.elispot.query.ElispotRunAntigenTable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

public class ElispotSchema extends AssaySchema
{
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    public static final String ANTIGEN_STATS_TABLE_NAME = "AntigenStats";

    public ElispotSchema(User user, Container container, ExpProtocol protocol)
    {
        super(AssaySchema.NAME, user, container, ExperimentService.get().getSchema(), protocol);
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

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol, String propertyPrefix) throws SQLException
    {
        String propPrefix = new Lsid(propertyPrefix, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);

        PropertyDescriptor[] result = Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);

        // Merge measure/dimension properties from well group domain into ElispotProperties domain
        // This needs to be removed and instead base the properties on a single PropertyDescriptor/DomainProperty
        Domain domain = PropertyService.get().getDomain(protocol.getContainer(), new Lsid(ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP, "Folder-" + protocol.getContainer().getRowId(), protocol.getName()).toString());
        if (domain != null)
        {
            for (PropertyDescriptor dataProperty : result)
            {
                DomainProperty domainProp = domain.getPropertyByName(dataProperty.getName());
                if (domainProp != null)
                {
                    dataProperty.setMeasure(domainProp.isMeasure());
                    dataProperty.setDimension(domainProp.isDimension());
                }
            }
        }
        return result;
    }

}
