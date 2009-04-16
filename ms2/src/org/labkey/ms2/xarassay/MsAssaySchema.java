/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.ms2.xarassay;

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MsAssaySchema extends UserSchema
{
    private static final String DATA_ROW_TABLE_NAME = "XarAssayDataRow";

    public MsAssaySchema(User user, Container container)
    {
        super("XarAssay", user, container, ExperimentService.get().getSchema());
    }

    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(DATA_ROW_TABLE_NAME));
    }

    public TableInfo createTable(String name)
    {
        for (ExpProtocol protocol : AssayService.get().getAssayProtocols(getContainer()))
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof XarAssayProvider)
            {
                if (DATA_ROW_TABLE_NAME.equalsIgnoreCase(name))
                {
                    return getDataRowTable(this, protocol);
                }
            }
        }
        return super.getTable(name);
    }

    public static TableInfo getDataRowTable(QuerySchema schema, ExpProtocol protocol)
    {
        return new MsFractionRunDataTable(schema, protocol);
    }
}