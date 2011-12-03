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

import java.sql.SQLException;
import java.util.*;

public class LuminexSchema extends AssaySchema
{
    public static final String ANALYTE_TABLE_NAME = "Analyte";
    public static final String CURVE_FIT_TABLE_NAME = "CurveFit";
    public static final String GUIDE_SET_TABLE_NAME = "GuideSet";
    public static final String GUIDE_SET_CURVE_FIT_TABLE_NAME = "GuideSetCurveFit";
    public static final String TITRATION_TABLE_NAME = "Titration";
    public static final String ANALYTE_TITRATION_TABLE_NAME = "AnalyteTitration";
    public static final String DATA_ROW_TABLE_NAME = "DataRow";
    public static final String DATA_FILE_TABLE_NAME = "DataFile";
    public static final String WELL_EXCLUSION_TABLE_NAME = "WellExclusion";
    public static final String WELL_EXCLUSION_ANALYTE_TABLE_NAME = "WellExclusionAnalyte";
    public static final String RUN_EXCLUSION_TABLE_NAME = "RunExclusion";
    public static final String RUN_EXCLUSION_ANALYTE_TABLE_NAME = "RunExclusionAnalyte";

    private List<String> _curveTypes;

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
                prefixTableName(RUN_EXCLUSION_TABLE_NAME),
                prefixTableName(CURVE_FIT_TABLE_NAME),
                prefixTableName(GUIDE_SET_TABLE_NAME),
                prefixTableName(GUIDE_SET_CURVE_FIT_TABLE_NAME),
                prefixTableName(ANALYTE_TITRATION_TABLE_NAME)
        );
    }

    private String prefixTableName(String table)
    {
        return getProtocol().getName() + " " + table;
    }

    public static String getWellExclusionTableName(ExpProtocol protocol)
    {
        return getProviderTableName(protocol, WELL_EXCLUSION_TABLE_NAME);
    }

    public static String getRunExclusionTableName(ExpProtocol protocol)
    {
        return getProviderTableName(protocol, RUN_EXCLUSION_TABLE_NAME);
    }

    public static String getAnalyteTitrationTableName(ExpProtocol protocol)
    {
        return getProviderTableName(protocol, ANALYTE_TITRATION_TABLE_NAME);
    }

    public synchronized List<String> getCurveTypes()
    {
        if (_curveTypes == null)
        {
            QueryDefinition queryDef = QueryService.get().createQueryDef(getUser(), _container, this, "query");
            queryDef.setSql("SELECT DISTINCT(CurveType) FROM \"" + getProviderTableName(getProtocol(), CURVE_FIT_TABLE_NAME).replace("\"", "\"\"") + "\"");
            queryDef.setContainerFilter(ContainerFilter.EVERYTHING);

            try
            {
                ArrayList<QueryException> errors = new ArrayList<QueryException>();
                TableInfo table = queryDef.getTable(errors, false);
                String[] curveTypes = Table.executeArray(table, "CurveType", null, new Sort("CurveType"), String.class);
                _curveTypes = Arrays.asList(curveTypes);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return _curveTypes;
    }

    public TableInfo createTable(String name)
    {
        String tableType = AssaySchema.getProviderTableType(getProtocol(), name);
        if (tableType != null)
        {
            if (ANALYTE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteTable(true);
            }
            if (TITRATION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createTitrationTable(true);
            }
            if (GUIDE_SET_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createGuideSetTable(true);
            }
            if (ANALYTE_TITRATION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                AnalyteTitrationTable result = createAnalyteTitrationTable(true);
                SQLFragment filter = new SQLFragment("AnalyteId IN (SELECT a.RowId FROM ");
                filter.append(getTableInfoAnalytes(), "a");
                filter.append(" WHERE a.DataId ");
                filter.append(createDataFilterInClause());
                filter.append(")");
                result.addCondition(filter, "RunId");
                return result;
            }
            if (DATA_FILE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                ExpDataTable result = createDataTable();
                SQLFragment filter = new SQLFragment("RowId");
                filter.append(createDataFilterInClause());
                result.addCondition(filter, "RowId");
                return result;
            }
            if (WELL_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                FilteredTable result = createWellExclusionTable(true);
                SQLFragment filter = new SQLFragment("DataId");
                filter.append(createDataFilterInClause());
                result.addCondition(filter, "DataId");
                return result;
            }
            if (CURVE_FIT_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                CurveFitTable result = createCurveFitTable(true);
                SQLFragment filter = new SQLFragment("AnalyteId IN (SELECT a.RowId FROM ");
                filter.append(getTableInfoAnalytes(), "a");
                filter.append(" WHERE a.DataId ");
                filter.append(createDataFilterInClause());
                filter.append(")");
                result.addCondition(filter, "RunId");
                return result;
            }
            if (GUIDE_SET_CURVE_FIT_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createGuideSetCurveFitTable();
            }
            if (RUN_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                FilteredTable result = createRunExclusionTable(true);
                SQLFragment filter = new SQLFragment("RunId IN (SELECT pa.RunId FROM ");
                filter.append(ExperimentService.get().getTinfoProtocolApplication(), "pa");
                filter.append(", ");
                filter.append(ExperimentService.get().getTinfoData(), "d");
                filter.append(" WHERE pa.RowId = d.SourceApplicationId AND d.RowId ");
                filter.append(createDataFilterInClause());
                filter.append(")");
                result.addCondition(filter, "RunId");
                return result;
            }
        }
        return null;
    }

    public AnalyteTitrationTable createAnalyteTitrationTable(boolean filter)
    {
        return new AnalyteTitrationTable(this, filter);
    }

    protected GuideSetTable createGuideSetTable(boolean filterTable)
    {
        return new GuideSetTable(this, filterTable);
    }

    protected CurveFitTable createCurveFitTable(boolean filterTable)
    {
        return new CurveFitTable(this, filterTable);
    }

    protected GuideSetCurveFitTable createGuideSetCurveFitTable()
    {
        return new GuideSetCurveFitTable(this, null);
    }

    /** @param curveType the type of curve to filter the results to */
    protected GuideSetCurveFitTable createGuideSetCurveFitTable(String curveType)
    {
        return new GuideSetCurveFitTable(this, curveType);
    }

    private WellExclusionTable createWellExclusionTable(boolean filterTable)
    {
        return new WellExclusionTable(this, filterTable);
    }

    private RunExclusionTable createRunExclusionTable(boolean filterTable)
    {
        return new RunExclusionTable(this, filterTable);
    }

    protected AnalyteTable createAnalyteTable(boolean filterTable)
    {
        AnalyteTable result = new AnalyteTable(this, filterTable);

        if (filterTable)
        {
            SQLFragment sql = new SQLFragment("DataId");
            sql.append(createDataFilterInClause());
            result.addCondition(sql);
        }
        result.setTitleColumn("Name");
        return result;
    }

    public TitrationTable createTitrationTable(boolean filter)
    {
        TitrationTable result = new TitrationTable(this, filter);
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
        return result;
    }

    public ExpDataTable createDataTable()
    {
        final ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Data.toString(), this);
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
        filter.append(" WHERE d.RunId = r.RowId");
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

    public static TableInfo getTableInfoGuideSet()
    {
        return getSchema().getTable(GUIDE_SET_TABLE_NAME);
    }

    public static TableInfo getTableInfoCurveFit()
    {
        return getSchema().getTable(CURVE_FIT_TABLE_NAME);
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

    public TableInfo createWellExclusionAnalyteTable()
    {
        FilteredTable result = new FilteredTable(getTableInfoWellExclusionAnalyte());
        result.wrapAllColumns(true);
        result.getColumn("AnalyteId").setFk(new AnalyteForeignKey(this));
        return result;
    }

    public TableInfo createRunExclusionAnalyteTable()
    {
        FilteredTable result = new FilteredTable(getTableInfoRunExclusionAnalyte());
        result.wrapAllColumns(true);
        result.getColumn("AnalyteId").setFk(new AnalyteForeignKey(this));
        return result;
    }

    public static class AnalyteForeignKey extends LookupForeignKey
    {
        private final LuminexSchema _schema;

        public AnalyteForeignKey(LuminexSchema schema)
        {
            super("RowId");
            _schema = schema;
        }

        @Override
        public TableInfo getLookupTableInfo()
        {
            return _schema.createAnalyteTable(false);
        }
    }
}
