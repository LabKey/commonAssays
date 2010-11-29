/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;

import java.util.*;

public class LuminexSchema extends AssaySchema
{
    private static final String ANALYTE_TABLE_NAME = "Analyte";
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    private final ExpProtocol _protocol;

    public LuminexSchema(User user, Container container, ExpProtocol protocol)
    {
        super("Luminex", user, container, getSchema());
        assert protocol != null;
        _protocol = protocol;
    }

    public Set<String> getTableNames()
    {
        // return only additional tables not exposed in the assay schema.
        return Collections.singleton(prefixTableName(ANALYTE_TABLE_NAME));
    }

    private String prefixTableName(String table)
    {
        return _protocol.getName() + " " + table;
    }


    public TableInfo createTable(String name)
    {
        String lname = name.toLowerCase();
        String protocolPrefix = _protocol.getName().toLowerCase() + " ";
        if (lname.startsWith(protocolPrefix))
        {
            name = name.substring(protocolPrefix.length());
            if (ANALYTE_TABLE_NAME.equalsIgnoreCase(name))
            {
                return createAnalyteTable(true);
            }
            /*
            else if (DATA_ROW_TABLE_NAME.equalsIgnoreCase(name))
            {
                return createDataRowTable();
            }
            */
        }
        return null;
    }

    protected TableInfo createAnalyteTable(boolean filterTable)
    {
        FilteredTable result = new FilteredTable(getTableInfoAnalytes());
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Name")));
        result.addColumn(result.wrapColumn("Data", result.getRealTable().getColumn("DataId"))).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createDataTable();
            }
        });
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setHidden(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FitProb")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ResVar")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RegressionType")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("StdCurve")));
        ColumnInfo lsidColumn = result.addColumn(result.wrapColumn(result.getRealTable().getColumn("LSID")));
        lsidColumn.setHidden(true);

        //String sqlObjectId = "(SELECT objectid FROM " + OntologyManager.getTinfoObject() + " o WHERE o.objecturi = " +
        //        ExprColumn.STR_TABLE_ALIAS + ".lsid)";

        //ColumnInfo colProperty = new ExprColumn(result, "Properties", new SQLFragment(sqlObjectId), Types.INTEGER);
        ColumnInfo colProperty = result.wrapColumn("Properties", result.getRealTable().getColumn("LSID"));
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        Map<String, PropertyDescriptor> map = new TreeMap<String, PropertyDescriptor>();
        for(DomainProperty pd : analyteDomain.getProperties())
        {
            map.put(pd.getName(), pd.getPropertyDescriptor());
        }
        colProperty.setFk(new PropertyForeignKey(map, this));
        colProperty.setIsUnselectable(true);
        result.addColumn(colProperty);

        if (filterTable)
        {
            addDataFilter(result);
        }
        result.setTitleColumn("Name");
        return result;
    }

    public ExpDataTable createDataTable()
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Datas.toString(), this);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);

        ColumnInfo runCol = ret.addColumn(ExpDataTable.Column.Run);
        if (_protocol != null)
        {
            runCol.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return AssayService.get().createRunTable(_protocol, AssayService.get().getProvider(_protocol), _user, _container);
                }
            });
        }

        return ret;
    }

    public FilteredTable createDataRowTable()
    {
        return new LuminexDataTable(this);
    }

    protected void addDataFilter(FilteredTable result)
    {
        SQLFragment filter = new SQLFragment("DataId IN (SELECT d.RowId FROM " + ExperimentService.get().getTinfoData() + " d, " + ExperimentService.get().getTinfoExperimentRun() + " r WHERE d.RunId = r.RowId AND d.Container = ?");
        filter.add(getContainer().getId());
        if (_protocol != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(_protocol.getLSID());
        }
        filter.append(")");
        result.addCondition(filter);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("luminex");
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoAnalytes()
    {
        return getSchema().getTable(ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoDataRow()
    {
        return getSchema().getTable(DATA_ROW_TABLE_NAME);
    }

    public ExpProtocol getProtocol()
    {
        return _protocol;
    }
}
