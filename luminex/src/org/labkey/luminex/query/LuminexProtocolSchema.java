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

package org.labkey.luminex.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.query.ResultsQueryView;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpQCFlagTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.Dataset;
import org.labkey.api.study.publish.StudyPublishService;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link.LinkBuilder;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.luminex.LuminexAssayProvider;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.LuminexResultsDataRegion;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps to a single assay design for schema tables/queries (batch, run, data, analyte, titration, curve fit, etc.)
 */
public class LuminexProtocolSchema extends AssayProtocolSchema
{
    public static final String ANALYTE_TABLE_NAME = "Analyte";
    public static final String CURVE_FIT_TABLE_NAME = "CurveFit";
    public static final String GUIDE_SET_TABLE_NAME = "GuideSet";
    public static final String GUIDE_SET_CURVE_FIT_TABLE_NAME = "GuideSetCurveFit";
    public static final String TITRATION_TABLE_NAME = "Titration";
    public static final String SINGLE_POINT_CONTROL_TABLE_NAME = "SinglePointControl";
    public static final String ANALYTE_TITRATION_TABLE_NAME = "AnalyteTitration";
    public static final String ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME = "AnalyteSinglePointControl";
    public static final String DATA_ROW_TABLE_NAME = "DataRow";
    public static final String DATA_FILE_TABLE_NAME = "DataFile";
    public static final String WELL_EXCLUSION_TABLE_NAME = "WellExclusion";
    public static final String TITRATION_EXCLUSION_TABLE_NAME = "TitrationExclusion";
    public static final String SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME = "SinglepointUnknownExclusion";
    public static final String WELL_EXCLUSION_ANALYTE_TABLE_NAME = "WellExclusionAnalyte";
    public static final String RUN_EXCLUSION_TABLE_NAME = "RunExclusion";
    public static final String RUN_EXCLUSION_ANALYTE_TABLE_NAME = "RunExclusionAnalyte";
    public static final String ANALYTE_TITRATION_QC_FLAG_TABLE_NAME = "AnalyteTitrationQCFlags";
    public static final String ANALYTE_SINGLE_POONT_CONTROL_QC_FLAG_TABLE_NAME = "AnalyteSinglePointControlQCFlags";
    public static final String CV_QC_FLAG_TABLE_NAME = "CVQCFlags";

    public static final String DB_SCHEMA_NAME = "luminex";

    private List<String> _curveTypes;

    public LuminexProtocolSchema(User user, Container container, @NotNull LuminexAssayProvider provider, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, provider, protocol, targetStudy);
    }

    @NotNull
    @Override
    public LuminexAssayProvider getProvider()
    {
        return (LuminexAssayProvider)super.getProvider();
    }

    @Override
    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(ANALYTE_TABLE_NAME);
        result.add(TITRATION_TABLE_NAME);
        result.add(DATA_FILE_TABLE_NAME);
        result.add(WELL_EXCLUSION_TABLE_NAME);
        result.add(TITRATION_EXCLUSION_TABLE_NAME);
        result.add(SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME);
        result.add(RUN_EXCLUSION_TABLE_NAME);
        result.add(CURVE_FIT_TABLE_NAME);
        result.add(GUIDE_SET_TABLE_NAME);
        result.add(GUIDE_SET_CURVE_FIT_TABLE_NAME);
        result.add(ANALYTE_TITRATION_TABLE_NAME);
        result.add(ANALYTE_TITRATION_QC_FLAG_TABLE_NAME);
        result.add(CV_QC_FLAG_TABLE_NAME);
        result.add(ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);
        result.add(ANALYTE_SINGLE_POONT_CONTROL_QC_FLAG_TABLE_NAME);
        result.add(SINGLE_POINT_CONTROL_TABLE_NAME);
        return result;
    }

    public synchronized List<String> getCurveTypes()
    {
        if (_curveTypes == null)
        {
            QueryDefinition queryDef = QueryService.get().createQueryDef(getUser(), _container, this, "query");
            queryDef.setSql("SELECT DISTINCT(CurveType) FROM \"" + CURVE_FIT_TABLE_NAME+ "\"");
            queryDef.setContainerFilter(ContainerFilter.EVERYTHING);

            ArrayList<QueryException> errors = new ArrayList<>();
            TableInfo table = queryDef.getTable(this, errors, false);
            String[] curveTypes = new TableSelector(table.getColumn("CurveType"), null, new Sort("CurveType")).getArray(String.class);
            _curveTypes = Arrays.asList(curveTypes);
        }
        return _curveTypes;
    }

    @Override
    protected TableInfo createProviderTable(String tableType, ContainerFilter cf)
    {
        if (tableType != null)
        {
            if (ANALYTE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteTable(cf, true);
            }

            if (TITRATION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createTitrationTable(cf, true);
            }

            if (GUIDE_SET_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createGuideSetTable(cf, true);
            }

            if (ANALYTE_TITRATION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteTitrationTable(cf, true);
            }

            if (DATA_FILE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                ExpDataTable result = createDataFileTable(cf);
                // Filter to just the files associated with this this protocol (container filter is already handled by ExpDataTable)
                result.addCondition(createDataIdContainerFilterSQL("RowId", null), FieldKey.fromParts("RowId"));
                return result;
            }

            if (WELL_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                WellExclusionTable result = createWellExclusionTable(cf, true);
                result.addCondition(new SimpleFilter(FieldKey.fromParts("Type"), null, CompareType.NONBLANK));
                result.removeColumn(result.getColumn("Dilution"));
                return result;
            }

            if (TITRATION_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                WellExclusionTable result = createWellExclusionTable(cf, true);
                result.setName(TITRATION_EXCLUSION_TABLE_NAME);
                SimpleFilter exclusionFilter = new SimpleFilter(FieldKey.fromParts("Type"), null, CompareType.ISBLANK);
                exclusionFilter.addCondition(FieldKey.fromParts("Dilution"), null, CompareType.ISBLANK);
                result.addCondition(exclusionFilter);
                result.removeColumn(result.getColumn("Dilution"));
                result.removeColumn(result.getColumn("Type"));
                result.removeColumn(result.getColumn("Well"));
                result.removeColumn(result.getColumn("Wells"));
                result.removeColumn(result.getColumn("Well Role"));
                return result;
            }

            if (SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                WellExclusionTable result = createWellExclusionTable(cf, true);
                result.setName(SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME);
                SimpleFilter exclusionFilter = new SimpleFilter(FieldKey.fromParts("Type"), null, CompareType.ISBLANK);
                exclusionFilter.addCondition(FieldKey.fromParts("Dilution"), null, CompareType.NONBLANK);
                result.addCondition(exclusionFilter);
                result.removeColumn(result.getColumn("Type"));
                result.removeColumn(result.getColumn("Well"));
                result.removeColumn(result.getColumn("Wells"));
                result.removeColumn(result.getColumn("Well Role"));
                return result;
            }

            if (CURVE_FIT_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createCurveFitTable(cf, true);
            }

            if (GUIDE_SET_CURVE_FIT_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createGuideSetCurveFitTable(cf);
            }

            if (RUN_EXCLUSION_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createRunExclusionTable(cf, true);
            }

            if (ANALYTE_TITRATION_QC_FLAG_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteTitrationQCFlagTable(cf);
            }

            if (ANALYTE_SINGLE_POONT_CONTROL_QC_FLAG_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteSinglePointControlQCFlagTable(cf);
            }

            if (CV_QC_FLAG_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createCVQCFlagTable(cf);
            }

            if (SINGLE_POINT_CONTROL_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createSinglePointControlTable(cf, true);
            }

            if (ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createAnalyteSinglePointControlTable(cf, true);
            }
        }

        return super.createProviderTable(tableType, cf);
    }

    public AnalyteTitrationTable createAnalyteTitrationTable(ContainerFilter cf, boolean filter)
    {
        return new AnalyteTitrationTable(this, cf, filter);
    }

    public GuideSetTable createGuideSetTable(ContainerFilter cf, boolean filterTable)
    {
        return new GuideSetTable(this, cf, filterTable);
    }

    public CurveFitTable createCurveFitTable(ContainerFilter cf, boolean filterTable)
    {
        return new CurveFitTable(this, cf, filterTable);
    }

    public GuideSetCurveFitTable createGuideSetCurveFitTable(ContainerFilter cf)
    {
        return new GuideSetCurveFitTable(this, cf, null);
    }

    /** @param curveType the type of curve to filter the results to */
    public GuideSetCurveFitTable createGuideSetCurveFitTable(ContainerFilter cf, String curveType)
    {
        return new GuideSetCurveFitTable(this, cf, curveType);
    }

    private WellExclusionTable createWellExclusionTable(ContainerFilter cf, boolean filterTable)
    {
        return new WellExclusionTable(this, cf, filterTable);
    }

    private SinglePointControlTable createSinglePointControlTable(ContainerFilter cf, boolean filterTable)
    {
        return new SinglePointControlTable(this, cf, filterTable);
    }

    public AnalyteSinglePointControlTable createAnalyteSinglePointControlTable(ContainerFilter cf, boolean filterTable)
    {
        return new AnalyteSinglePointControlTable(this, cf, filterTable);
    }

    private RunExclusionTable createRunExclusionTable(ContainerFilter cf, boolean filterTable)
    {
        return new RunExclusionTable(this, cf, filterTable);
    }

    public AnalyteTable createAnalyteTable(ContainerFilter cf, boolean filterTable)
    {
        return new AnalyteTable(this, cf, filterTable);
    }

    public TitrationTable createTitrationTable(ContainerFilter cf, boolean filter)
    {
        return new TitrationTable(this, cf, filter);
    }

    public ExpDataTable createDataFileTable(ContainerFilter cf)
    {
        final ExpDataTable ret = ExperimentService.get().createDataTable(DATA_FILE_TABLE_NAME, this, cf);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.addColumn(ExpDataTable.Column.LSID).setHidden(true);
        ret.addColumn(ExpDataTable.Column.DataFileUrl).setHidden(true);
        ret.addColumn(ExpDataTable.Column.SourceProtocolApplication).setHidden(true);
        ret.setTitleColumn("Name");
        var protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);

        var runCol = ret.addColumn(ExpDataTable.Column.Run);
        runCol.setFk(new LookupForeignKey(ret.getContainerFilter(),"RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable result = AssayService.get().createRunTable(getProtocol(), AssayService.get().getProvider(getProtocol()), _user, _container, null);
                result.setContainerFilter(getLookupContainerFilter());
                return result;
            }
        });

        Domain domain = LuminexAssayProvider.getExcelRunDomain(getProtocol());
        ret.addColumns(domain, null);

        List<FieldKey> visibleColumns = new ArrayList<>(ret.getDefaultVisibleColumns());
        for (DomainProperty domainProperty : domain.getProperties())
        {
            visibleColumns.add(FieldKey.fromParts(domainProperty.getName()));
        }
        ret.setDefaultVisibleColumns(visibleColumns);

        return ret;
    }

    @Override
    public LuminexDataTable createDataTable(ContainerFilter cf, boolean includeLinkedToStudyColumns)
    {
        LuminexDataTable table = new LuminexDataTable(this, cf);
        if (includeLinkedToStudyColumns)
        {
            ExpProtocol protocol = getProtocol();
            String rowIdName = getProvider().getTableMetadata(protocol).getResultRowIdFieldKey().getName();
            StudyPublishService.get().addLinkedToStudyColumns(table, Dataset.PublishSource.Assay, true, protocol.getRowId(), rowIdName, getUser());
        }
        return table;
    }

    public ExpQCFlagTable createAnalyteTitrationQCFlagTable(ContainerFilter cf)
    {
        ExpQCFlagTable result = ExperimentService.get().createQCFlagsTable(ANALYTE_TITRATION_QC_FLAG_TABLE_NAME, this, cf);
        result.populate();
        result.setAssayProtocol(getProvider(), getProtocol());

        var analyteColumn = result.addColumn("Analyte", ExpQCFlagTable.Column.IntKey1);
        analyteColumn.setFk(new AnalyteForeignKey(this, cf));
        var titrationColumn = result.addColumn("Titration", ExpQCFlagTable.Column.IntKey2);
        titrationColumn.setFk(new TitrationForeignKey(this, cf));

        result.setDescription("Contains Run QC Flags that are associated with an analyte/titration combination");
        SQLFragment nonCVFlagFilter = new SQLFragment(" Key1 IS NULL AND Key2 IS NULL AND FlagType != ?");
        nonCVFlagFilter.add(LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE);
        result.addCondition(nonCVFlagFilter);

        // disable insert/update for this table
        result.setImportURL(AbstractTableInfo.LINK_DISABLER);
        result.setInsertURL(AbstractTableInfo.LINK_DISABLER);
        result.setUpdateURL(AbstractTableInfo.LINK_DISABLER);

        return result;
    }

    public ExpQCFlagTable createAnalyteSinglePointControlQCFlagTable(ContainerFilter cf)
    {
        ExpQCFlagTable result = ExperimentService.get().createQCFlagsTable(ANALYTE_SINGLE_POONT_CONTROL_QC_FLAG_TABLE_NAME, this, cf);
        result.populate();
        result.setAssayProtocol(getProvider(), getProtocol());

        var analyteColumn = result.addColumn("Analyte", ExpQCFlagTable.Column.IntKey1);
        analyteColumn.setFk(new AnalyteForeignKey(this, cf));
        var titrationColumn = result.addColumn("SinglePointControl", ExpQCFlagTable.Column.IntKey2);
        titrationColumn.setFk(new SinglePointControlForeignKey(this, cf));

        result.setDescription("Contains Run QC Flags that are associated with an analyte/single point control combination");
        SQLFragment nonCVFlagFilter = new SQLFragment(" Key1 IS NULL AND Key2 IS NULL AND FlagType = ?");
        nonCVFlagFilter.add(LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE);
        result.addCondition(nonCVFlagFilter);

        // disable insert/update for this table
        result.setImportURL(AbstractTableInfo.LINK_DISABLER);
        result.setInsertURL(AbstractTableInfo.LINK_DISABLER);
        result.setUpdateURL(AbstractTableInfo.LINK_DISABLER);

        return result;
    }

    public ExpQCFlagTable createCVQCFlagTable(ContainerFilter cf)
    {
        ExpQCFlagTable result = ExperimentService.get().createQCFlagsTable(ExpSchema.TableType.QCFlags.toString(), this, cf);
        result.populate();
        result.setAssayProtocol(getProvider(), getProtocol());

        var analyteColumn = result.addColumn("Analyte", ExpQCFlagTable.Column.IntKey1);
        analyteColumn.setFk(new AnalyteForeignKey(this, cf));
        result.addColumn("DataId", ExpQCFlagTable.Column.IntKey2);
        var wellTypeColumn = result.addColumn("WellType", ExpQCFlagTable.Column.Key1);
        wellTypeColumn.setLabel("Well Type");
        var wellDescriptionColumn = result.addColumn("WellDescription", ExpQCFlagTable.Column.Key2);
        wellDescriptionColumn.setLabel("Well Description");

        result.setDescription("Contains %CV QC Flags that are associated with well replicates");
        SQLFragment cvFlagFilter = new SQLFragment(" Key1 IS NOT NULL AND Key2 IS NOT NULL ");
        result.addCondition(cvFlagFilter);

        // disable insert/update for this table
        result.setImportURL(AbstractTableInfo.LINK_DISABLER);
        result.setInsertURL(AbstractTableInfo.LINK_DISABLER);
        result.setUpdateURL(AbstractTableInfo.LINK_DISABLER);

        return result;
    }

    public SQLFragment createDataIdContainerFilterSQL(String dataIdColumnName, @Nullable ContainerFilter cf)
    {
        SQLFragment filter = new SQLFragment(dataIdColumnName);
        filter.append(" IN (SELECT d.RowId FROM ");
        filter.append(ExperimentService.get().getTinfoData(), "d");
        filter.append(", ");
        filter.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        filter.append(" WHERE d.RunId = r.RowId AND r.ProtocolLSID = ?");
        filter.add(getProtocol().getLSID());
        if (cf != null)
        {
            filter.append(" AND ");
            filter.append(cf.getSQLFragment(getSchema(), new SQLFragment("r.Container")));
        }
        filter.append(") ");
        return filter;
    }

    public SQLFragment createRunIdContainerFilterSQL(ContainerFilter filter)
    {
        SQLFragment sql = new SQLFragment("RunId IN (SELECT r.RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE r.ProtocolLSID = ? AND ");
        sql.add(getProtocol().getLSID());
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container")));
        sql.append(")");
        return sql;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(DB_SCHEMA_NAME, DbSchemaType.Module);
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

    public static TableInfo getTableInfoSinglePointControl()
    {
        return getSchema().getTable(SINGLE_POINT_CONTROL_TABLE_NAME);
    }

    public static TableInfo getTableInfoAnalyteSinglePointControl()
    {
        return getSchema().getTable(ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);
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

    public static TableInfo getTableInfoTitrationExclusion()
    {
        return getSchema().getTable(TITRATION_EXCLUSION_TABLE_NAME);
    }

    public static TableInfo getTableInfoSinglepointUnknownExclusion()
    {
        return getSchema().getTable(SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME);
    }

    public static TableInfo getTableInfoWellExclusionAnalyte()
    {
        return getSchema().getTable(WELL_EXCLUSION_ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoRunExclusionAnalyte()
    {
        return getSchema().getTable(RUN_EXCLUSION_ANALYTE_TABLE_NAME);
    }

    public TableInfo createWellExclusionAnalyteTable(ContainerFilter cf)
    {
        FilteredTable<?> result = new FilteredTable<>(getTableInfoWellExclusionAnalyte(), this, cf);
        result.wrapAllColumns(true);
        result.getMutableColumn("AnalyteId").setFk(new AnalyteForeignKey(this, cf));
        return result;
    }

    public TableInfo createRunExclusionAnalyteTable(ContainerFilter cf)
    {
        FilteredTable<?> result = new FilteredTable<>(getTableInfoRunExclusionAnalyte(), this, cf);
        result.wrapAllColumns(true);
        result.getMutableColumn("AnalyteId").setFk(new AnalyteForeignKey(this, cf));
        return result;
    }

    @Override
    public ExpRunTable createRunsTable(ContainerFilter cf)
    {
        final ExpRunTable result = super.createRunsTable(cf);

        // Render any PDF outputs we found as direct download links since they should be plots of standard curves
        var curvesColumn = result.addColumn("Curves", ExpRunTable.Column.Name);
        curvesColumn.setWidth("30");
        curvesColumn.setReadOnly(true);
        curvesColumn.setShownInInsertView(false);
        curvesColumn.setShownInUpdateView(false);
        curvesColumn.setDescription("Link to titration curves in PDF format. Available if assay design is configured to generate them.");
        List<FieldKey> visibleColumns = new ArrayList<>(result.getDefaultVisibleColumns());
        visibleColumns.add(Math.min(visibleColumns.size(), 3), curvesColumn.getFieldKey());
        result.setDefaultVisibleColumns(visibleColumns);

        DisplayColumnFactory factory = colInfo -> new DataColumn(colInfo)
        {
            /** RowId -> Name */
            private final Map<FieldKey, FieldKey> _pdfColumns = new HashMap<>();

            {
                TableInfo outputTable = result.getColumn(ExpRunTable.Column.Output).getFk().getLookupTableInfo();
                // Check for data outputs that are PDFs
                for (ColumnInfo columnInfo : outputTable.getColumns())
                {
                    if (columnInfo.getName().toLowerCase().endsWith("pdf"))
                    {
                        _pdfColumns.put(
                            FieldKey.fromParts(ExpRunTable.Column.Output.toString(), columnInfo.getName(), ExpDataTable.Column.RowId.toString()),
                            FieldKey.fromParts(ExpRunTable.Column.Output.toString(), columnInfo.getName(), ExpDataTable.Column.Name.toString()));
                    }
                }
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                keys.addAll(_pdfColumns.keySet());
                keys.addAll(_pdfColumns.values());
            }

            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Map<Integer, String> pdfs = new HashMap<>();
                for (Map.Entry<FieldKey, FieldKey> entry : _pdfColumns.entrySet())
                {
                    Number rowId = (Number)ctx.get(entry.getKey());
                    if (rowId != null)
                    {
                        pdfs.put(rowId.intValue(), (String)ctx.get(entry.getValue()));
                    }
                }

                if (pdfs.size() == 1)
                {
                    for (Map.Entry<Integer, String> entry : pdfs.entrySet())
                    {
                        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(getContainer());
                        url.addParameter("rowId", entry.getKey().toString());
                        out.write("<a href=\"" + url + "\">");
                        out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/_images/sigmoidal_curve.png\" />");
                        out.write("</a>");
                    }
                }
                else if (pdfs.size() > 1)
                {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<Integer, String> entry : pdfs.entrySet())
                    {
                        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(getContainer());
                        url.addParameter("rowId", entry.getKey().toString());
                        sb.append("<a href=\"");
                        sb.append(url);
                        sb.append("\">");
                        sb.append(PageFlowUtil.filter(entry.getValue()));
                        sb.append("</a><br/>");
                    }

                    HtmlString image = HtmlString.unsafe("<img src=\"" + AppProps.getInstance().getContextPath() + "/_images/sigmoidal_curve.png\" />");
                    new LinkBuilder(image)
                        .onMouseOver("return showHelpDiv(this, 'Titration Curves', " + PageFlowUtil.jsString(PageFlowUtil.filter(sb.toString())) + ");")
                        .clearClasses()
                        .appendTo(out);
                }
            }
        };
        curvesColumn.setDisplayColumnFactory(factory);

        return result;
    }

    public static class AnalyteForeignKey extends LookupForeignKey
    {
        private final LuminexProtocolSchema _schema;

        public AnalyteForeignKey(LuminexProtocolSchema schema, ContainerFilter cf)
        {
            super(cf,"RowId", null);
            _schema = schema;
        }

        @Override
        public TableInfo getLookupTableInfo()
        {
            return _schema.createAnalyteTable(getLookupContainerFilter(),false);
        }
    }

    public static class TitrationForeignKey extends LookupForeignKey
    {
        private final LuminexProtocolSchema _schema;

        public TitrationForeignKey(LuminexProtocolSchema schema, ContainerFilter cf)
        {
            super(cf,"RowId", null);
            _schema = schema;
        }

        @Override
        public TableInfo getLookupTableInfo()
        {
            return _schema.createTitrationTable(getLookupContainerFilter(), false);
        }
    }

    public static class SinglePointControlForeignKey extends LookupForeignKey
    {
        private final LuminexProtocolSchema _schema;

        public SinglePointControlForeignKey(LuminexProtocolSchema schema, ContainerFilter cf)
        {
            super(cf,"RowId", null);
            _schema = schema;
        }

        @Override
        public TableInfo getLookupTableInfo()
        {
            return _schema.createSinglePointControlTable(getLookupContainerFilter(), false);
        }
    }


    @Nullable
    @Override
    protected ResultsQueryView createDataQueryView(final ViewContext context, QuerySettings settings, BindException errors)
    {
        return new ResultsQueryView(getProtocol(), context, settings)
        {
            @Override
            protected DataRegion createDataRegion()
            {
                ResultsDataRegion rgn = new LuminexResultsDataRegion(_provider, _protocol);
                initializeDataRegion(rgn);
                return rgn;
            }

            @Override
            public DataView createDataView()
            {
                DataView result = super.createDataView();
                String runId = context.getRequest().getParameter(result.getDataRegion().getName() + ".Data/Run/RowId~eq");

                // if showing controls and user is viewing data results for a single run, add the Exclusions menu button to button bar
                if (showControls() && NumberUtils.isDigits(runId))
                {
                    MenuButton exclusionsMenu = new MenuButton("Exclusions");
                    exclusionsMenu.setDisplayPermission(UpdatePermission.class);

                    NavTree excludeAnalytes = new NavTree("Exclude Analytes");
                    excludeAnalytes.setScript("openExclusionsAnalyteWindow(" + getProtocol().getRowId() + ", " + runId + ");");
                    exclusionsMenu.addMenuItem(excludeAnalytes);

                    // query to see if we have any non-Standard titrations in this run
                    SimpleFilter f = new SimpleFilter();
                    f.addCondition(FieldKey.fromParts("Standard"), false, CompareType.EQUAL);
                    f.addCondition(FieldKey.fromParts("Run"), Integer.parseInt(runId), CompareType.EQUAL);
                    TableSelector tbs = new TableSelector(getSchema().getTable("Titration", getContainerFilter()), f, null);
                    long rows = tbs.getRowCount();
                    if (rows > 0)
                    {
                        NavTree excludeTitration = new NavTree("Exclude Titrations");
                        excludeTitration.setScript("openExclusionsTitrationWindow(" + getProtocol().getRowId() + ", " + runId + ");");
                        exclusionsMenu.addMenuItem(excludeTitration);
                    }

                    NavTree excludeSinglepointUnknowns = new NavTree("Exclude Singlepoint Unknowns");
                    excludeSinglepointUnknowns.setScript("openExclusionsSinglepointUnknownWindow(" + getProtocol().getRowId() + ", " + runId + ");");
                    exclusionsMenu.addMenuItem(excludeSinglepointUnknowns);

                    ButtonBar buttonBar = new ButtonBar(result.getDataRegion().getButtonBar(DataRegion.MODE_GRID));
                    buttonBar.add(exclusionsMenu);
                    result.getDataRegion().setButtonBar(buttonBar, DataRegion.MODE_GRID);
                }
                return result;
            }

            @NotNull
            @Override
            public LinkedHashSet<ClientDependency> getClientDependencies()
            {
                LinkedHashSet<ClientDependency> dependencies = super.getClientDependencies();
                dependencies.add(ClientDependency.fromPath("luminex/exclusion.lib.xml"));
                return dependencies;
            }
        };

    }
}
