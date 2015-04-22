/*
 * Copyright (c) 2014-2015 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.EnumTableInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProviderSchema;

import java.util.Collections;
import java.util.Set;

/**
 * Created by klum on 12/23/2014.
 */
public class ElispotProviderSchema extends AssayProviderSchema
{
    public static final String SCHEMA_NAME = "ELISpot";
    public static final String ELISPOT_PLATE_READER_TABLE = "ElispotPlateReader";

    public ElispotProviderSchema(User user, Container container, ElispotAssayProvider provider, @Nullable Container targetStudy)
    {
        super(user, container, provider, targetStudy);
    }

    @NotNull
    @Override
    public ElispotAssayProvider getProvider()
    {
        return (ElispotAssayProvider)super.getProvider();
    }

    @Override
    public Set<String> getTableNames()
    {
        return Collections.singleton(ELISPOT_PLATE_READER_TABLE);
    }

    @Override
    public TableInfo createTable(String name)
    {
        if (name.equalsIgnoreCase(ELISPOT_PLATE_READER_TABLE))
        {
            EnumTableInfo<ElispotAssayProvider.PlateReaderType> result = new EnumTableInfo<>(ElispotAssayProvider.PlateReaderType.class, this, new EnumTableInfo.EnumValueGetter<ElispotAssayProvider.PlateReaderType>()
            {
                public String getValue(ElispotAssayProvider.PlateReaderType e)
                {
                    return e.getLabel();
                }
            }, false, "List of possible plate reader types for the ELISpot assay.");
            result.setPublicSchemaName(this.getSchemaName());
            result.setPublicName(ELISPOT_PLATE_READER_TABLE);

            return result;
        }
        return super.createTable(name);
    }
}
