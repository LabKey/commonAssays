/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.elispot.query;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.AssayDomainKind;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotManager;
import org.labkey.elispot.ElispotProtocolSchema;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by davebradlee on 3/24/15.
 */
public class ElispotAntigenDomainKind extends AssayDomainKind
{
    public static final String KINDNAME = "ElispotAntigenDomainKind";
    public static final String ANTIGEN_LSID_COLUMN_NAME = "AntigenLsid";
    public static final String SPECIMEN_LSID_COLUMN_NAME = "SpecimenLsid";
    public static final String RUNID_COLUMN_NAME = "RunId";
    public static final String MEAN_COLUMN_NAME = "Mean";
    public static final String MEDIAN_COLUMN_NAME = "Median";
    private static final Set<PropertyStorageSpec> _baseFields;

    static
    {
        Set<PropertyStorageSpec> baseFields = new LinkedHashSet<>();
        baseFields.add(new PropertyStorageSpec(ANTIGEN_LSID_COLUMN_NAME, JdbcType.VARCHAR).setPrimaryKey(true));
        baseFields.add(new PropertyStorageSpec(SPECIMEN_LSID_COLUMN_NAME, JdbcType.VARCHAR));
        baseFields.add(new PropertyStorageSpec(ElispotDataHandler.WELLGROUP_PROPERTY_NAME, JdbcType.VARCHAR));
        baseFields.add(new PropertyStorageSpec(RUNID_COLUMN_NAME, JdbcType.INTEGER));
        baseFields.add(new PropertyStorageSpec(MEAN_COLUMN_NAME, JdbcType.DOUBLE));
        baseFields.add(new PropertyStorageSpec(MEDIAN_COLUMN_NAME, JdbcType.DOUBLE));
        baseFields.add(new PropertyStorageSpec(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME, JdbcType.VARCHAR));
        baseFields.add(new PropertyStorageSpec(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME, JdbcType.VARCHAR));
        baseFields.add(new PropertyStorageSpec(ElispotDataHandler.ANALYTE_PROPERTY_NAME, JdbcType.VARCHAR));
        baseFields.add(new PropertyStorageSpec(ElispotDataHandler.CYTOKINE_PROPERTY_NAME, JdbcType.VARCHAR));

        _baseFields = Collections.unmodifiableSet(baseFields);
    }

    public ElispotAntigenDomainKind()
    {
        super(ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
    }

    @Override
    public String getKindName()
    {
        return KINDNAME;
    }

    @Override
    public Set<PropertyStorageSpec> getBaseProperties(Domain domain)
    {
        return _baseFields;
    }

    @Override
    public DbScope getScope()
    {
        return ElispotManager.getSchema().getScope();
    }

    @Override
    public String getStorageSchemaName()
    {
        return ElispotProtocolSchema.ELISPOT_ANTIGEN_SCHEMA_NAME;
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
        return getAssayReservedPropertyNames();
    }
}
