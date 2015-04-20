/*
 * Copyright (c) 2011-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.elispot.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.StorageProvisioner;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotDataHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * User: klum
 * Date: Jan 27, 2011
 * Time: 1:45:11 PM
 */
public class ElispotRunAntigenTable extends PlateBasedAssayRunDataTable
{
    public ElispotRunAntigenTable(final AssaySchema schema, final Domain domain, ExpProtocol protocol)
    {
        super(schema, StorageProvisioner.createTableInfo(domain), protocol);
        setDescription("Contains one row per well for the \"" + protocol.getName() + "\" ELISpot assay design.");
        setTitle("Antigen");
        this.setPublic(false);

        // Add column for AntigenStats heading
        SQLFragment sql = new SQLFragment("CASE WHEN AntigenName IS NULL OR AntigenName = AntigenWellgroupName THEN AntigenWellgroupName ELSE ");
        if (getSqlDialect().isPostgreSQL())
            sql.append("AntigenName || ' (' || SUBSTRING(AntigenWellgroupName, 9) || ')' END");
        else
            sql.append("CONCAT(AntigenName, ' (', REPLACE(AntigenWellgroupName, 'Antigen ', ''), ')') END");

        ColumnInfo antigenHeading = new ExprColumn(this, "AntigenHeading", sql, JdbcType.VARCHAR, getColumn("AntigenWellgroupName"), getColumn("AntigenName"));
        antigenHeading.setHidden(true);
        addColumn(antigenHeading);
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> fieldKeys = new ArrayList<>();
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.WELLGROUP_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromString("Mean"));
        fieldKeys.add(FieldKey.fromString("Median"));
        fieldKeys.add(FieldKey.fromParts("RunId"));
        fieldKeys.add(FieldKey.fromParts("SpecimenLsid", "Property", "ParticipantId"));
        return fieldKeys;
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo result = super.resolveColumn(name);

        if ("Properties".equalsIgnoreCase(name))
        {
            // Hook up a column that joins back to this table so that the columns formerly under the Properties
            // node can still be queried there.
            result = wrapColumn("Properties", getRealTable().getColumn("ObjectId"));
            result.setIsUnselectable(true);
            LookupForeignKey fk = new LookupForeignKey("ObjectId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    Domain domain = AbstractAssayProvider.getDomainByPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);
                    return new ElispotRunAntigenTable(_userSchema, domain, _protocol);
                }
            };
            fk.setPrefixColumnCaption(false);
            result.setFk(fk);
        }

        return result;
    }

    @Override
    protected void addPropertyColumns(final AssaySchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns)
    {
        for (ColumnInfo column : _rootTable.getColumns())
        {
            if ("RunId".equalsIgnoreCase(column.getName()))
            {
                continue;
            }
            ColumnInfo wrapColumn = addWrapColumn(column);
            if ("AntigenLsid".equalsIgnoreCase(column.getName()) || "SpecimenLsid".equalsIgnoreCase(column.getName()))
                wrapColumn.setHidden(true);
            else if ("Mean".equalsIgnoreCase(column.getName()) || "Median".equalsIgnoreCase(column.getName()))
                wrapColumn.setFormat("###0.0");
        }
    }

    @Override
    protected boolean hasMaterialSpecimenPropertyColumnDecorator()
    {
        return false;
    }
}
