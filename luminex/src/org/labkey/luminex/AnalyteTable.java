/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.assay.AbstractAssayProvider;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class AnalyteTable extends AbstractLuminexTable
{
    public AnalyteTable(LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoAnalytes(), schema, filter);
        setName(LuminexSchema.getProviderTableName(schema.getProtocol(), LuminexSchema.ANALYTE_TABLE_NAME));
        
        addColumn(wrapColumn(getRealTable().getColumn("Name")));
        addColumn(wrapColumn("Data", getRealTable().getColumn("DataId"))).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createDataTable();
            }
        });
        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setHidden(true);
        addColumn(wrapColumn(getRealTable().getColumn("FitProb")));
        addColumn(wrapColumn(getRealTable().getColumn("ResVar")));
        addColumn(wrapColumn(getRealTable().getColumn("RegressionType")));
        addColumn(wrapColumn(getRealTable().getColumn("StdCurve")));

        ColumnInfo titrationColumn = addColumn(wrapColumn("Standard", getRealTable().getColumn("RowId")));
        titrationColumn.setFk(new MultiValuedForeignKey(new LookupForeignKey("Analyte")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(LuminexSchema.getTableInfoAnalyteTitration());
                ColumnInfo titrationColumn = result.addColumn(result.wrapColumn("Titration", result.getRealTable().getColumn("TitrationId")));
                titrationColumn.setFk(new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        TitrationTable titrationTable = _schema.createTitrationTable(false);
                        titrationTable.addCondition(new SimpleFilter("Standard", Boolean.TRUE));
                        return titrationTable;
                    }
                });
                ColumnInfo analyteColumn = result.addColumn(result.wrapColumn("Analyte", result.getRealTable().getColumn("AnalyteId")));
                analyteColumn.setFk(new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return _schema.createAnalyteTable(false);
                    }
                });
                return result;
            }
        }, "Titration"));
        titrationColumn.setHidden(false);

        ColumnInfo lsidColumn = addColumn(wrapColumn(getRealTable().getColumn("LSID")));
        lsidColumn.setHidden(true);
        lsidColumn.setShownInInsertView(false);
        lsidColumn.setShownInUpdateView(false);

        ColumnInfo colProperty = wrapColumn("Properties", getRealTable().getColumn("LSID"));
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_schema.getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        Map<String, PropertyDescriptor> map = new TreeMap<String, PropertyDescriptor>();
        for(DomainProperty pd : analyteDomain.getProperties())
        {
            map.put(pd.getName(), pd.getPropertyDescriptor());
        }
        colProperty.setFk(new PropertyForeignKey(map, _schema));
        colProperty.setIsUnselectable(true);
        colProperty.setReadOnly(true);
        colProperty.setShownInInsertView(false);
        colProperty.setShownInUpdateView(false);
        addColumn(colProperty);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("DataId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), "Container", container));
        sql.append(")");
        return sql;
    }

    @Override
    public boolean hasPermission(UserPrincipal user, Class<? extends Permission> perm)
    {
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _schema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new RowIdQueryUpdateService<Analyte>(this)
        {
            @Override
            public Analyte get(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
            {
                return Table.selectObject(LuminexSchema.getTableInfoAnalytes(), key, Analyte.class);
            }

            @Override
            public void delete(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Analyte createNewBean()
            {
                return new Analyte();
            }

            @Override
            protected Analyte insert(User user, Container container, Analyte bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Analyte update(User user, Container container, Analyte newAnalyte, Integer oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
            {
                return Table.update(user, LuminexSchema.getTableInfoAnalytes(), newAnalyte, oldKey);
            }
        };
    }
}
