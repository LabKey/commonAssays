/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

import org.labkey.api.exp.property.AssayDomainKind;
import org.labkey.api.exp.property.Domain;
import org.labkey.luminex.query.LuminexDataTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Jan 27, 2012
 */
public class LuminexDataDomainKind extends AssayDomainKind
{
    public LuminexDataDomainKind()
    {
        super(LuminexAssayProvider.ASSAY_DOMAIN_CUSTOM_DATA);
    }

    @Override
    public String getKindName()
    {
        return "Luminex Results";
    }

    @Override
    public Set<String> getReservedPropertyNames(Domain domain)
    {
        // Standard reserved names
        Set<String> result = getAssayReservedPropertyNames();

        // All from the basic Luminex data table
        result.addAll(LuminexProtocolSchema.getTableInfoDataRow().getColumnNameSet());

        // Also reserve the aliased names of the columns
        for (Map.Entry<String,String> entry : LuminexDataTable.REMAPPED_SCHEMA_COLUMNS.entrySet())
        {
            result.add(entry.getKey());
            result.add(entry.getValue());
        }
        return result;
    }
}
