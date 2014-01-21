/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.nab.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.query.DilutionProviderSchema;
import org.labkey.api.data.*;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.nab.NabAssayProvider;
import org.labkey.api.assay.dilution.SampleInfo;

import java.util.*;

/**
 * User: brittp
 * Date: Oct 3, 2007
 * Time: 4:22:26 PM
 */
public class NabProviderSchema extends DilutionProviderSchema
{
    public static final String SCHEMA_NAME = "Nab";

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                // Nab top-level schema for backwards compatibility <12.3.  Moved to assay schema.
                return new NabProviderSchema(schema.getUser(), schema.getContainer(), AssayService.get().getProvider(NabAssayProvider.NAME), null, true);
            }
        });
    }

    public NabProviderSchema(User user, Container container, AssayProvider provider, @Nullable Container targetStudy, boolean hidden)
    {
        super(user, container, provider, SCHEMA_NAME, targetStudy, hidden);
    }

    @Override
    protected Set<String> getTableNames(boolean visible)
    {
        Set<String> names = super.getTableNames(visible);

        if (!visible)
        {
            // For backwards compatibility <12.3.  Data tables moved to NabProtocolSchema (assay.Nab.<protocol> schema)
            for (ExpProtocol protocol : getProtocols())
            {
                names.add(AssaySchema.getLegacyProtocolTableName(protocol, NabProtocolSchema.DATA_ROW_TABLE_NAME));
            }
        }
        return names;
    }

    @Override
    public TableInfo createTable(String name)
    {
        if (SAMPLE_PREPARATION_METHOD_TABLE_NAME.equalsIgnoreCase(name))
        {
            EnumTableInfo<SampleInfo.Method> result = new EnumTableInfo<>(SampleInfo.Method.class, getDbSchema(), "List of possible sample preparation methods for the NAb assay.", false);
            result.setPublicSchemaName(SCHEMA_NAME);
            result.setPublicName(SAMPLE_PREPARATION_METHOD_TABLE_NAME);
            return result;
        }
        if (CURVE_FIT_METHOD_TABLE_NAME.equalsIgnoreCase(name))
        {
            EnumTableInfo<StatsService.CurveFitType> result = new EnumTableInfo<>(StatsService.CurveFitType.class, getDbSchema(), new EnumTableInfo.EnumValueGetter<StatsService.CurveFitType>()
            {
                public String getValue(StatsService.CurveFitType e)
                {
                    return e.getLabel();
                }
            }, false, "List of possible curve fitting methods for the NAb assay.");
            result.setPublicSchemaName(SCHEMA_NAME);
            result.setPublicName(CURVE_FIT_METHOD_TABLE_NAME);
            return result;
        }

        // For backwards compatibility <12.3.  Data tables moved to NabProtocolSchema (assay.Nab.<protocol> schema)
        for (ExpProtocol protocol : getProtocols())
        {
            String tableName = AssaySchema.getLegacyProtocolTableName(protocol, NabProtocolSchema.DATA_ROW_TABLE_NAME);
            if (tableName.equalsIgnoreCase(name))
            {
                return createDataRowTable(protocol);
            }
        }
        return super.createTable(name);
    }

    public NabRunDataTable createDataRowTable(ExpProtocol protocol)
    {
        return new NabRunDataTable(new NabProtocolSchema(getUser(), getContainer(), protocol, getTargetStudy()), protocol);
    }
}
