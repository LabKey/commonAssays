/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User: gktaylor
 * Date: Aug 9, 2013
 */
public class AnalyteSinglePointControlTable extends AbstractLuminexTable
{
    public AnalyteSinglePointControlTable(LuminexProtocolSchema schema, boolean filterTable)
    {
        // expose the actual columns in the table
        super(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), schema, filterTable);
        setName(LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);

        ColumnInfo singlePointControlCol = addColumn(wrapColumn("SinglePointControl", getRealTable().getColumn("SinglePointControlId")));
        singlePointControlCol.setFk(new QueryForeignKey(schema, null, "SinglePointControl", "RowId", "Name"));

        ColumnInfo runColumn = addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        runColumn.setFk(new QueryForeignKey(schema, null, "Analyte", "RowId", "Name"));

        ColumnInfo guideSetCol = addColumn(wrapColumn("GuideSet", getRealTable().getColumn("GuideSetId")));
        guideSetCol.setFk(new QueryForeignKey(schema, null, "GuideSet", "RowId", "AnalyteName"));

        addColumn(wrapColumn(getRealTable().getColumn("IncludeInGuideSetCalculation")));

        // Get the average of the non-excluded FI-Background values for this control
        SQLFragment avgFiSQL = new SQLFragment("(SELECT AVG(dr.FIBackground) FROM (");
        LuminexDataTable dataTable = schema.createDataTable(false);
        dataTable.setContainerFilter(ContainerFilter.EVERYTHING);
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

        // set the default columns for this table to be those used for the QC Report
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("SinglePointControl", "Run", "Name"));
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
        defaultCols.add(FieldKey.fromParts("AverageFiBkgdQCFlagsEnabled"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("SinglePointControlId IN (SELECT RowId FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "spc");
        sql.append(" WHERE RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), container));
        sql.append("))");
        return sql;
    }

    @Override
    public boolean hasPermission(UserPrincipal user, Class<? extends Permission> perm)
    {
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _userSchema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        // Pair<Integer, Integer> is analyteid/titrationid combo
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
            protected AnalyteSinglePointControl get(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException
            {
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), key.getKey());
                filter.addCondition(FieldKey.fromParts("SinglePointControlId"), key.getValue());
                return new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), filter, null).getObject(AnalyteSinglePointControl.class);
            }


            protected SinglePointControl getSinglePointControl(int rowId)
            {
                SinglePointControl result = new TableSelector(LuminexProtocolSchema.getTableInfoSinglePointControl()).getObject(rowId, SinglePointControl.class);
                if (result == null)
                {
                    throw new IllegalStateException("Unable to find referenced single point control: " + rowId);
                }
                return result;
            }

            protected void validate(AnalyteSinglePointControl bean, GuideSet guideSet, Analyte analyte) throws ValidationException
            {
                SinglePointControl control = getSinglePointControl(bean.getSinglePointControlId());
                if (!Objects.equals(analyte.getName(), guideSet.getAnalyteName()))
                {
                    throw new ValidationException("GuideSet is for analyte " + guideSet.getAnalyteName(), " but this row is mapped to analyte " + analyte.getName());
                }
                if (!Objects.equals(control.getName(), guideSet.getControlName()))
                {
                    throw new ValidationException("GuideSet is for single point control" + guideSet.getControlName(), " but this row is mapped to " + control.getName());
                }
            }

            protected void updateQCFlags(User user, AnalyteSinglePointControl bean) throws SQLException
            {
                // get the run, isotype, conjugate, and analtye/single point control information in order to update QC Flags
                Analyte analyte = getAnalyte(bean.getAnalyteId());
                SinglePointControl control = getSinglePointControl(bean.getSinglePointControlId());
                ExpRun run = getRun(control.getRunId());
                Map<String, String> runIsotypeConjugate = getIsotypeAndConjugate(run);

                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Analyte"), analyte.getRowId());
                filter.addCondition(FieldKey.fromParts("SinglePointControl"), control.getRowId());
                AnalyteSinglePointControlTable analyteSinglePointControlTable = getUserSchema().createAnalyteSinglePointControlTable(true);
                analyteSinglePointControlTable.setContainerFilter(ContainerFilter.EVERYTHING);
                Double average = new TableSelector(analyteSinglePointControlTable, Collections.singleton("AverageFiBkgd"), filter, null).getObject(Double.class);

                LuminexDataHandler.insertOrUpdateAnalyteSinglePointControlQCFlags(user, run, _userSchema.getProtocol(), bean, analyte, control, runIsotypeConjugate.get("Isotype"), runIsotypeConjugate.get("Conjugate"), average.doubleValue());
            }
        };
    }
}
