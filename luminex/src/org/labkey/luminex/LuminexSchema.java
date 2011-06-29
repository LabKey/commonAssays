/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;

import java.util.*;

public class LuminexSchema extends AssaySchema
{
    private static final String ANALYTE_TABLE_NAME = "Analyte";
    private static final String TITRATION_TABLE_NAME = "Titration";
    private static final String ANALYTE_TITRATION_TABLE_NAME = "AnalyteTitration";
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    private static final String DATA_FILE_TABLE_NAME = "DataFile";
    private static final String WELL_EXCLUSION_TABLE_NAME = "WellExclusion";
    private static final String WELL_EXCLUSION_ANALYTE_TABLE_NAME = "WellExclusionAnalyte";
    private static final String RUN_EXCLUSION_TABLE_NAME = "RunExclusion";
    private static final String RUN_EXCLUSION_ANALYTE_TABLE_NAME = "RunExclusionAnalyte";

    public LuminexSchema(User user, Container container, ExpProtocol protocol)
    {
        super("Luminex", user, container, getSchema(), protocol);
        assert protocol != null;
    }

    public Set<String> getTableNames()
    {
        // return only additional tables not exposed in the assay schema.
        return PageFlowUtil.set(
                prefixTableName(ANALYTE_TABLE_NAME),
                prefixTableName(TITRATION_TABLE_NAME),
                prefixTableName(DATA_FILE_TABLE_NAME),
                prefixTableName(WELL_EXCLUSION_TABLE_NAME),
                prefixTableName(RUN_EXCLUSION_TABLE_NAME)
        );
    }

    private String prefixTableName(String table)
    {
        return getProtocol().getName() + " " + table;
    }


    public TableInfo createTable(String name)
    {
        String lname = name.toLowerCase();
        String protocolPrefix = getProtocol().getName().toLowerCase() + " ";
        if (lname.startsWith(protocolPrefix))
        {
            name = name.substring(protocolPrefix.length());
            if (ANALYTE_TABLE_NAME.equalsIgnoreCase(name))
            {
                return createAnalyteTable(true);
            }
            if (TITRATION_TABLE_NAME.equalsIgnoreCase(name))
            {
                return createTitrationTable(true);
            }
            if (DATA_FILE_TABLE_NAME.equalsIgnoreCase(name))
            {
                ExpDataTable result = createDataTable();
                SQLFragment filter = new SQLFragment("RowId");
                filter.append(createDataFilterInClause());
                result.addCondition(filter, "RowId");
                return result;
            }
            if (WELL_EXCLUSION_TABLE_NAME.equalsIgnoreCase(name))
            {
                FilteredTable result = createWellExclusionTable();
                SQLFragment filter = new SQLFragment("DataId");
                filter.append(createDataFilterInClause());
                result.addCondition(filter, "DataId");
                return result;
            }
            if (RUN_EXCLUSION_TABLE_NAME.equalsIgnoreCase(name))
            {
                FilteredTable result = createRunExclusionTable();
                SQLFragment filter = new SQLFragment("RunId");
                filter.append(createDataFilterInClause());
                result.addCondition(filter, "RunId");
                return result;
            }
        }
        return null;
    }

    private FilteredTable createWellExclusionTable()
    {
        FilteredTable result = new FilteredTable(getTableInfoAnalyteTitration());
        result.wrapAllColumns(true);
        
        return result;
    }

    private FilteredTable createRunExclusionTable()
    {
        FilteredTable result = new FilteredTable(getTableInfoRunExclusion());
        result.wrapAllColumns(true);

        return result;
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
        ColumnInfo titrationColumn = result.addColumn(result.wrapColumn("Standard", result.getRealTable().getColumn("RowId")));
        titrationColumn.setFk(new MultiValuedForeignKey(new LookupForeignKey("Analyte")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(getTableInfoAnalyteTitration());
                ColumnInfo titrationColumn = result.addColumn(result.wrapColumn("Titration", result.getRealTable().getColumn("TitrationId")));
                titrationColumn.setFk(new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return createTitrationTable(false);
                    }
                });
                ColumnInfo analyteColumn = result.addColumn(result.wrapColumn("Analyte", result.getRealTable().getColumn("AnalyteId")));
                analyteColumn.setFk(new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return createAnalyteTable(false);
                    }
                });
                return result;
            }
        }, "Titration"));
        titrationColumn.setHidden(false);

        ColumnInfo lsidColumn = result.addColumn(result.wrapColumn(result.getRealTable().getColumn("LSID")));
        lsidColumn.setHidden(true);

        ColumnInfo colProperty = result.wrapColumn("Properties", result.getRealTable().getColumn("LSID"));
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
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
            SQLFragment sql = new SQLFragment("DataId");
            sql.append(createDataFilterInClause());
            result.addCondition(sql);
        }
        result.setTitleColumn("Name");
        return result;
    }

    public TableInfo createTitrationTable(boolean filter)
    {
        FilteredTable result = new FilteredTable(getTableInfoTitration());
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setHidden(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Name")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Standard")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("QCControl")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Unknown")));
        ColumnInfo runColumn = result.addColumn(result.wrapColumn("Run", result.getRealTable().getColumn("RunId")));
        runColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return QueryService.get().getUserSchema(getUser(), getContainer(), NAME).getTable(getRunsTableName(getProtocol()));
            }
        });
        if (filter)
        {
            SQLFragment sql = new SQLFragment("RunId IN (SELECT pa.RunId FROM ");
            sql.append(ExperimentService.get().getTinfoProtocolApplication(), "pa");
            sql.append(", ");
            sql.append(ExperimentService.get().getTinfoData(), "d");
            sql.append(" WHERE pa.RowId = d.SourceApplicationId AND d.RowId ");
            sql.append(createDataFilterInClause());
            sql.append(")");
            result.addCondition(sql);
        }
        result.setTitleColumn("Name");
        return result;
    }

    public ExpDataTable createDataTable()
    {
        final ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Datas.toString(), this);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.addColumn(ExpDataTable.Column.LSID).setHidden(true);
        ret.addColumn(ExpDataTable.Column.SourceProtocolApplication).setHidden(true);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);

        ColumnInfo runCol = ret.addColumn(ExpDataTable.Column.Run);
        if (getProtocol() != null)
        {
            runCol.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    ExpRunTable result = AssayService.get().createRunTable(getProtocol(), AssayService.get().getProvider(getProtocol()), _user, _container);
                    result.setContainerFilter(ret.getContainerFilter());
                    return result;
                }
            });
        }

        Domain domain = AbstractAssayProvider.getDomainByPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
        ret.addColumns(domain, null);

        List<FieldKey> visibleColumns = new ArrayList<FieldKey>(ret.getDefaultVisibleColumns());
        for (DomainProperty domainProperty : domain.getProperties())
        {
            visibleColumns.add(FieldKey.fromParts(domainProperty.getName()));
        }
        ret.setDefaultVisibleColumns(visibleColumns);

        return ret;
    }

    public LuminexDataTable createDataRowTable()
    {
        return new LuminexDataTable(this);
    }

    protected SQLFragment createDataFilterInClause()
    {
        SQLFragment filter = new SQLFragment(" IN (SELECT d.RowId FROM ");
        filter.append(ExperimentService.get().getTinfoData(), "d");
        filter.append(", ");
        filter.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        // TODO - make this respect container filters
        filter.append(" WHERE d.RunId = r.RowId AND d.Container = ?");
        filter.add(getContainer().getId());
        if (getProtocol() != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(getProtocol().getLSID());
        }
        filter.append(") ");
        return filter;
    }

    protected SQLFragment createRunFilterInClause()
    {
        SQLFragment filter = new SQLFragment(" IN (SELECT r.RowId FROM ");
        filter.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        // TODO - make this respect container filters
        filter.append(" WHERE r.Container = ?");
        filter.add(getContainer().getId());
        if (getProtocol() != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(getProtocol().getLSID());
        }
        filter.append(") ");
        return filter;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("luminex");
    }

    public static TableInfo getTableInfoAnalytes()
    {
        return getSchema().getTable(ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoDataRow()
    {
        return getSchema().getTable(DATA_ROW_TABLE_NAME);
    }

    public static TableInfo getTableInfoTitration()
    {
        return getSchema().getTable(TITRATION_TABLE_NAME);
    }

    public static TableInfo getTableInfoAnalyteTitration()
    {
        return getSchema().getTable(ANALYTE_TITRATION_TABLE_NAME);
    }

    public static TableInfo getTableInfoRunExclusion()
    {
        return getSchema().getTable(RUN_EXCLUSION_TABLE_NAME);
    }

    public static TableInfo getTableInfoWellExclusion()
    {
        return getSchema().getTable(WELL_EXCLUSION_TABLE_NAME);
    }

    public static TableInfo getTableInfoWellExclusionAnalyte()
    {
        return getSchema().getTable(WELL_EXCLUSION_ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoRunExclusionAnalyte()
    {
        return getSchema().getTable(RUN_EXCLUSION_ANALYTE_TABLE_NAME);
    }
}
