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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JavaScriptDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Pair;
import org.labkey.luminex.AbstractLuminexControlUpdateService;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.AnalyteSinglePointControl;
import org.labkey.luminex.model.GuideSet;
import org.labkey.luminex.model.SinglePointControl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyteSinglePointControlTable extends AbstractLuminexTable
{
    public AnalyteSinglePointControlTable(final LuminexProtocolSchema schema, ContainerFilter cf, boolean filterTable)
    {
        // expose the actual columns in the table
        super(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), schema, cf, filterTable);
        setName(LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);

        var singlePointControlCol = addColumn(wrapColumn("SinglePointControl", getRealTable().getColumn("SinglePointControlId")));
        singlePointControlCol.setFk(QueryForeignKey.from(schema, cf).to("SinglePointControl", "RowId", "Name"));

        var runColumn = addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        runColumn.setFk(QueryForeignKey.from(schema, cf).to("Analyte", "RowId", "Name"));

        var guideSetCol = addColumn(wrapColumn("GuideSet", getRealTable().getColumn("GuideSetId")));
        guideSetCol.setFk(QueryForeignKey.from(schema, cf).to("GuideSet", "RowId", "AnalyteName"));

        addColumn(wrapColumn(getRealTable().getColumn("IncludeInGuideSetCalculation")));

        // Get the average of the non-excluded FI-Background values for this control
        SQLFragment avgFiSQL = new SQLFragment("(SELECT AVG(dr.FIBackground) FROM (");
        // TODO ContainerFilter -- Do we really want a non-permission checking container filter here?
        LuminexDataTable dataTable = schema.createDataTable(ContainerFilter.EVERYTHING, false);
        List<ColumnInfo> dataColumns = Arrays.asList(dataTable.getColumn("FlaggedAsExcluded"), dataTable.getColumn("FIBackground"), dataTable.getColumn("Description"), dataTable.getColumn("Data"), dataTable.getColumn("Analyte"));
        avgFiSQL.append(QueryService.get().getSelectSQL(dataTable, dataColumns, null, null, Table.ALL_ROWS, 0, false));
        avgFiSQL.append(") dr, ");
        avgFiSQL.append(ExperimentService.get().getTinfoData(), "d");
        avgFiSQL.append(", ");
        avgFiSQL.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "spc");
        avgFiSQL.append(" WHERE dr.Description = spc.Name AND dr.Data = d.RowId AND d.RunId = spc.RunId ");
        avgFiSQL.append(" AND dr.Analyte = ");
        avgFiSQL.append(ExprColumn.STR_TABLE_ALIAS);
        avgFiSQL.append(".AnalyteId AND spc.RowId = ");
        avgFiSQL.append(ExprColumn.STR_TABLE_ALIAS);
        avgFiSQL.append(".SinglePointControlId AND dr.FlaggedAsExcluded = ?)");
        avgFiSQL.add(false);

        ExprColumn avgFiColumn = new ExprColumn(this, "AverageFiBkgd", avgFiSQL, JdbcType.DOUBLE);
        avgFiColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "AverageFiBkgdQCFlagsEnabled");
            }
        });
        addColumn(avgFiColumn);

        // add a column for 'AverageFiBkgdQCFlagsEnabled'
        SQLFragment averageFiBkgdFlagEnabledSQL = createQCFlagEnabledSQLFragment(this.getSqlDialect(), LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE, null, LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_ID);
        ExprColumn averageFiBkgdFlagEnabledColumn = new ExprColumn(this, "AverageFiBkgdQCFlagsEnabled", averageFiBkgdFlagEnabledSQL, JdbcType.VARCHAR);
        averageFiBkgdFlagEnabledColumn.setLabel("Average FI Background QC Flags Enabled State");
        averageFiBkgdFlagEnabledColumn.setHidden(true);
        addColumn(averageFiBkgdFlagEnabledColumn);

        // add LJ plot column
        var ljPlots = addWrapColumn("L-J Plots", getRealTable().getColumn(FieldKey.fromParts("SinglePointControlId")));
        ljPlots.setDisplayColumnFactory(colInfo -> {
            // using JavaScriptDisplayColumn for dependencies
            return new JavaScriptDisplayColumn(colInfo, List.of("clientapi/ext3", "vis/vis", "luminex/LeveyJenningsPlotHelpers.js", "luminex/LeveyJenningsReport.css"))
            {
                @Override
                public void renderGridCellContents(RenderContext ctx, Writer out)
                {
                    int protocolId = schema.getProtocol().getRowId();
                    int analyte = (int)ctx.get("analyte");
                    int singlePointControl = (int)ctx.get("singlePointControl");
                    String onClickPattern = "LABKEY.LeveyJenningsPlotHelper.getLeveyJenningsPlotWindow(%d,%d,%d,'%s','SinglePoint');";
                    String onClick = String.format(onClickPattern, protocolId, analyte, singlePointControl, "MFI");

                    HtmlString html = HtmlString.unsafe(String.format("<img src='%s' width='27' height='20'>", AppProps.getInstance().getContextPath() + "/luminex/ljPlotIcon.png"));

                    renderLink(out, html, onClick, null);
                }

                @Override
                public boolean isSortable()
                {
                    return false;
                }

                @Override
                public boolean isFilterable()
                {
                    return false;
                }
            };
        });

        // set the default columns for this table to be those used for the QC Report
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Name"));
        defaultCols.add(FieldKey.fromParts("L-J Plots"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Name"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Batch", "Network"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Batch", "CustomProtocol"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Folder"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "NotebookNo"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "AssayType"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "ExpPerformer"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Data", "AcquisitionDate"));
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Isotype"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Conjugate"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Properties", "LotNumber"));
        defaultCols.add(FieldKey.fromParts("GuideSet", "Created"));
        defaultCols.add(FieldKey.fromParts("AverageFiBkgd"));
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "QCFlags"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter)
    {
        SQLFragment sql = new SQLFragment("SinglePointControlId IN (SELECT RowId FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "spc");
        sql.append(" WHERE ");
        sql.append(getUserSchema().createRunIdContainerFilterSQL(filter));
        sql.append(")");
        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        checkedPermissions.add(perm);
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _userSchema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        // Pair<Integer, Integer> is analyteid/singlePointControlId combo
        return new AbstractLuminexControlUpdateService<AnalyteSinglePointControl>(this, AnalyteSinglePointControl.class)
        {
            @Override
            protected AnalyteSinglePointControl createNewBean()
            {
                return new AnalyteSinglePointControl();
            }

            @Override
            protected Pair<Integer, Integer> keyFromMap(Map<String, Object> map) throws InvalidKeyException
            {
                Integer analyteId = getInteger(map, map.containsKey("analyte") ? "analyte" : "analyteid");
                Integer singlePointControlId = getInteger(map, map.containsKey("singlepointcontrol") ? "singlepointcontrol" : "singlepointcontrolid");
                return new Pair<>(analyteId, singlePointControlId);
            }

            @Override
            protected AnalyteSinglePointControl get(User user, Container container, Pair<Integer, Integer> key)
            {
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), key.getKey());
                filter.addCondition(FieldKey.fromParts("SinglePointControlId"), key.getValue());
                return new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), filter, null).getObject(AnalyteSinglePointControl.class);
            }

            @Override
            protected void validate(AnalyteSinglePointControl bean, GuideSet guideSet, Analyte analyte) throws ValidationException
            {
                SinglePointControl control = bean.getSinglePointControlFromId();
                if (!Objects.equals(analyte.getName(), guideSet.getAnalyteName()))
                {
                    throw new ValidationException("GuideSet is for analyte " + guideSet.getAnalyteName(), " but this row is mapped to analyte " + analyte.getName());
                }
                if (!Objects.equals(control.getName(), guideSet.getControlName()))
                {
                    throw new ValidationException("GuideSet is for single point control" + guideSet.getControlName(), " but this row is mapped to " + control.getName());
                }
            }
        };
    }
}
