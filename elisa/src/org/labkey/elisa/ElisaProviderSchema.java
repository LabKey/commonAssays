/*
 * Copyright (c) 2020-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayProviderSchema;
import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;

import java.util.Collections;
import java.util.Set;

public class ElisaProviderSchema extends AssayProviderSchema
{
    public static final String CURVE_FIT_METHOD_TABLE_NAME = "CurveFitMethod";

    public ElisaProviderSchema(User user, Container container, AssayProvider provider, @Nullable Container targetStudy)
    {
        super(user, container, provider, targetStudy);
    }

    @Override
    public Set<String> getTableNames()
    {
        return Collections.singleton(CURVE_FIT_METHOD_TABLE_NAME);
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (CURVE_FIT_METHOD_TABLE_NAME.equalsIgnoreCase(name))
        {
            PlateBasedAssayProvider provider = (PlateBasedAssayProvider)getProvider();
            AbstractPlateBasedAssayProvider.CurveFitTableInfo table = new AbstractPlateBasedAssayProvider.CurveFitTableInfo(this, provider,
                    "List of possible curve fitting methods for the " + getProvider().getResourceName() + " assay.");
            table.setPublicSchemaName(getSchemaName());
            table.setPublicName(CURVE_FIT_METHOD_TABLE_NAME);

            return table;
        }
        return super.createTable(name, cf);
    }
}
