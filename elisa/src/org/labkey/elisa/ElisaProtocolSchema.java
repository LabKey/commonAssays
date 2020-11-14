/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.elisa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.elisa.query.CurveFitTable;
import org.labkey.elisa.query.ElisaResultsTable;

import java.util.Set;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class ElisaProtocolSchema extends AssayProtocolSchema
{
    public static final String CURVE_FIT_TABLE_NAME = "CurveFit";
    public static final String ELISA_DB_SCHEMA_NAME = "elisa";

    public ElisaProtocolSchema(User user, Container container, @NotNull ElisaAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @Override
    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(CURVE_FIT_TABLE_NAME);

        return result;
    }

    @Override
    protected TableInfo createProviderTable(String name, ContainerFilter cf)
    {
        if (name != null)
        {
            if (CURVE_FIT_TABLE_NAME.equalsIgnoreCase(name))
            {
                return new CurveFitTable(this, cf);
            }
        }
        return super.createProviderTable(name, cf);
    }

    @Override
    public FilteredTable createDataTable(ContainerFilter cf, boolean includeCopiedToStudyColumns)
    {
        return new ElisaResultsTable(this, cf, includeCopiedToStudyColumns);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(ELISA_DB_SCHEMA_NAME);
    }

    public static TableInfo getTableInfoCurveFit()
    {
        return getSchema().getTable(CURVE_FIT_TABLE_NAME);
    }
}
