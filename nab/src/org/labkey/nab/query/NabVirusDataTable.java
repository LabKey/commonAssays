/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.ParameterMapStatement;
import org.labkey.api.data.StatementUtils;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpdateableTableInfo;
import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.dataiterator.DataIteratorContext;
import org.labkey.api.dataiterator.TableInsertDataIteratorBuilder;
import org.labkey.api.exp.PropertyColumn;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.RawValueColumn;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.StorageProvisioner;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayResultUpdateService;
import org.labkey.nab.NabAssayProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by klum on 7/17/2014.
 */
public class NabVirusDataTable extends FilteredTable<AssayProtocolSchema> implements UpdateableTableInfo
{
    protected final ExpProtocol _protocol;
    protected final AssayProvider _provider;
    private Domain _virusDomain = null;

    public NabVirusDataTable(AssayProtocolSchema schema, ContainerFilter cf, Domain virusDomain)
    {
        super(StorageProvisioner.createTableInfo(virusDomain), schema, cf);

        _protocol = _userSchema.getProtocol();
        _provider = _userSchema.getProvider();
        _virusDomain = virusDomain;

        setDescription("Contains plate virus information for the " + _protocol.getName() + " assay definition");
        setName(DilutionManager.VIRUS_TABLE_NAME);
        setPublicSchemaName(_userSchema.getSchemaName());

        List<FieldKey> visibleColumns = new ArrayList<>();
        for (ColumnInfo baseColumn : getRealTable().getColumns())
        {
            var col = wrapColumn(baseColumn);

            if (NabVirusDomainKind.VIRUS_LSID_COLUMN_NAME.equalsIgnoreCase(col.getName()))
            {
                col.setHidden(true);
                col.setKeyField(true);
            }
            else if (NabVirusDomainKind.DATLSID_COLUMN_NAME.equalsIgnoreCase(col.getName()))
            {
                col.setHidden(true);
            }

            DomainProperty domainProperty = _virusDomain.getPropertyByName(baseColumn.getName());
            if (domainProperty != null)
            {
                col.setFieldKey(new FieldKey(null,domainProperty.getName()));
                PropertyDescriptor pd = domainProperty.getPropertyDescriptor();
                FieldKey pkFieldKey = new FieldKey(null, NabAssayProvider.VIRUS_LSID_COLUMN_NAME);
                PropertyColumn.copyAttributes(_userSchema.getUser(), col, pd, schema.getContainer(), _userSchema.getSchemaPath(), getPublicName(), pkFieldKey, null, null);
            }
            addColumn(col);

            if (col.getMvColumnName() != null)
            {
                var rawValueCol = createRawValueColumn(baseColumn, col, RawValueColumn.RAW_VALUE_SUFFIX, "Raw Value", "This column contains the raw value itself, regardless of any missing value indicators that may have been set.");
                addColumn(rawValueCol);
            }

            if (col != null && !col.isHidden() && !col.isUnselectable() && !col.isMvIndicatorColumn())
                visibleColumns.add(col.getFieldKey());
        }

        getMutableColumn(NabAssayProvider.VIRUS_LSID_COLUMN_NAME).setShownInUpdateView(false);
        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    public Domain getDomain()
    {
        return _virusDomain;
    }

    private BaseColumnInfo createRawValueColumn(ColumnInfo baseColumn, ColumnInfo col, String nameSuffix, String labelSuffix, String descriptionSuffix)
    {
        var rawValueCol = new AliasedColumn(baseColumn.getName() + nameSuffix, col);
        rawValueCol.setDisplayColumnFactory(BaseColumnInfo.DEFAULT_FACTORY);
        rawValueCol.setLabel(baseColumn.getLabel() + " " + labelSuffix);
        String description = baseColumn.getDescription();
        if (description == null)
        {
            description = "";
        }
        else
        {
            description += " ";
        }
        description += descriptionSuffix;
        rawValueCol.setDescription(description);
        rawValueCol.setUserEditable(false);
        rawValueCol.setHidden(true);
        rawValueCol.setRawValueColumn(true);
        rawValueCol.setMvColumnName(null); // This column itself does not allow QC
        rawValueCol.setNullable(true); // Otherwise we get complaints on import for required fields
        return rawValueCol;
    }

    @Override
    public boolean insertSupported()
    {
        return true;
    }

    @Override
    public boolean updateSupported()
    {
        return false;
    }

    @Override
    public boolean deleteSupported()
    {
        return false;
    }

    @Override
    public TableInfo getSchemaTableInfo()
    {
        return getRealTable();
    }

    @Override
    public ObjectUriType getObjectUriType()
    {
        return ObjectUriType.schemaColumn;
    }

    @Override
    public String getObjectURIColumnName()
    {
        return null;
    }

    @Override
    public String getObjectIdColumnName()
    {
        return null;
    }

    @Override
    public CaseInsensitiveHashMap<String> remapSchemaColumns()
    {
        return null;
    }

    @Override
    public CaseInsensitiveHashSet skipProperties()
    {
        return null;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        if (ReadPermission.class.isAssignableFrom(perm))
            return _userSchema.getContainer().hasPermission(user, perm, _userSchema.getContextualRoles());
        if (DeletePermission.class.isAssignableFrom(perm) || UpdatePermission.class.isAssignableFrom(perm))
            return _provider.isEditableResults(_protocol) && _userSchema.getContainer().hasPermission(user, perm);
        return false;
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new AssayResultUpdateService(_userSchema, this);
    }

    @Override
    public DataIteratorBuilder persistRows(DataIteratorBuilder data, DataIteratorContext context)
    {
        return new TableInsertDataIteratorBuilder(data, this);
    }

    @Override
    public ParameterMapStatement insertStatement(Connection conn, User user) throws SQLException
    {
        return StatementUtils.insertStatement(conn, this, getContainer(), user, false, true);
    }

    @Override
    public ParameterMapStatement updateStatement(Connection conn, User user, Set<String> columns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterMapStatement deleteStatement(Connection conn)
    {
        throw new UnsupportedOperationException();
    }
}
