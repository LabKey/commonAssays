/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.property.AssayDomainKind;
import org.labkey.api.exp.property.Domain;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by klum on 7/18/2014.
 */
public class NabVirusDomainKind extends AssayDomainKind
{
    public static final String VIRUS_LSID_COLUMN_NAME = "virusLsid";
    public static final String DATLSID_COLUMN_NAME = "dataLsid";
    private static final Set<PropertyStorageSpec> _baseFields;

    static
    {
        Set<PropertyStorageSpec> baseFields = new LinkedHashSet<>();
        baseFields.add(new PropertyStorageSpec(VIRUS_LSID_COLUMN_NAME, JdbcType.VARCHAR).setPrimaryKey(true));
        baseFields.add(new PropertyStorageSpec("Container", JdbcType.VARCHAR).setEntityId(true));
        baseFields.add(new PropertyStorageSpec(DATLSID_COLUMN_NAME, JdbcType.VARCHAR));

        _baseFields = Collections.unmodifiableSet(baseFields);
    }

    public NabVirusDomainKind()
    {
        super(NabAssayProvider.ASSAY_DOMAIN_VIRUS_WELLGROUP);
    }

    @Override
    public String getKindName()
    {
        return "NAb Virus Data";
    }

    @Override
    public Set<PropertyStorageSpec> getBaseProperties(Domain domain)
    {
        return _baseFields;
    }

    @Override
    public DbScope getScope()
    {
        return NabManager.getSchema().getScope();
    }

    @Override
    public String getStorageSchemaName()
    {
        return NabProtocolSchema.NAB_VIRUS_SCHEMA_NAME;
    }

    private DbSchema getSchema()
    {
        return DbSchema.get(getStorageSchemaName(), getSchemaType());
    }

    @Override
    public DbSchemaType getSchemaType()
    {
        return DbSchemaType.Provisioned;
    }

    @Override
    public Set<String> getReservedPropertyNames(Domain domain)
    {
        Set<String> names = getAssayReservedPropertyNames();

        for (PropertyStorageSpec spec : getBaseProperties(domain))
            names.add(spec.getName());

        return names;
    }
}
