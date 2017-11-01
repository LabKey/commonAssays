/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.JavaScriptDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.model.AnalyteSinglePointControl;
import org.labkey.luminex.model.AnalyteTitration;
import org.labkey.luminex.model.GuideSet;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSetTable extends AbstractCurveFitPivotTable
{
    public GuideSetTable(final LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoGuideSet(), schema, filter, "RowId");
        setName(LuminexProtocolSchema.GUIDE_SET_TABLE_NAME);

        for (ColumnInfo col : getRealTable().getColumns())
        {
            // value-based average and std dev columns will be aliased and only editable via the Manage Guide Set UI
            if (col.getName().endsWith("Average") || col.getName().endsWith("StdDev"))
            {
                ColumnInfo valueBasedCol = addWrapColumn(col);
                valueBasedCol.setJdbcType(JdbcType.DOUBLE);
                valueBasedCol.setHidden(true);
                valueBasedCol.setUserEditable(false);
            }
            else
            {
                ColumnInfo wrapCol = addWrapColumn(col);
                wrapCol.setHidden(col.isHidden());

                if (wrapCol.getName().equals("ValueBased"))
                    wrapCol.setUserEditable(false);
            }
        }

        ColumnInfo protocolCol = getColumn("ProtocolId");
        protocolCol.setLabel("Assay Design");
        protocolCol.setHidden(true);
        protocolCol.setShownInDetailsView(false);
        protocolCol.setShownInUpdateView(false);
        protocolCol.setShownInInsertView(false);
        protocolCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return AssayService.get().createSchema(schema.getUser(), schema.getContainer(), null).getTable(AssaySchema.ASSAY_LIST_TABLE_NAME);
            }
        });

        AliasedColumn detailsCol = new AliasedColumn("Details", wrapColumn(getRealTable().getColumn(FieldKey.fromParts("RowId"))));
        detailsCol.setDisplayColumnFactory(colInfo ->
        {
            Collection<String> dependencies = Arrays.asList("Ext4", "luminex/GuideSetWindow.js");
            String javaScriptEvent = "onclick=\"createGuideSetWindow(${ProtocolId:jsString}, ${RowId:jsString}, true);\"";
            return new JavaScriptDisplayColumn(colInfo, dependencies, javaScriptEvent, "labkey-text-link")
            {
                @NotNull
                @Override
                public String getFormattedValue(RenderContext ctx)
                {
                    return "details";
                }

                @Override
                public void renderTitle(RenderContext ctx, Writer out) throws IOException
                {
                    // no title
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
        addColumn(detailsCol);

        AliasedColumn valueBasedCol = new AliasedColumn("Type", wrapColumn(getRealTable().getColumn(FieldKey.fromParts("ValueBased"))));
        valueBasedCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo){
                    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                    {
                        if ( (Boolean)ctx.get(this.getColumnInfo().getFieldKey()) )
                            out.write("Value-based");
                        else
                            out.write("Run-based");
                    }
                };
            }
        });
        addColumn(valueBasedCol);

        addFIColumns(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "MaxFI", "TitrationMax", "Titration Max", "GuideSetId");
        AnalyteSinglePointControlTable analyteSinglePointControlTable = schema.createAnalyteSinglePointControlTable(false);
        analyteSinglePointControlTable.setContainerFilter(ContainerFilter.EVERYTHING);
        addFIColumns(analyteSinglePointControlTable, "AverageFiBkgd", "SinglePointControl", "Single Point Control", "GuideSet");

        SQLFragment controlTypeSql = new SQLFragment("(SELECT CASE WHEN IsTitration=" + this.getSqlDialect().getBooleanTRUE() +" THEN 'Titration' ELSE 'SinglePoint' END)");
        ExprColumn controlTypeCol = new ExprColumn(this, "ControlType", controlTypeSql, JdbcType.VARCHAR);
        addColumn(controlTypeCol);

        addRunCounts();

        ForeignKey userIdForeignKey = new UserIdQueryForeignKey(schema.getUser(), schema.getContainer(), true);
        getColumn("ModifiedBy").setFk(userIdForeignKey);
        getColumn("CreatedBy").setFk(userIdForeignKey);

        getColumn("Created").setLabel("Guide Set Start Date");

        addCurveTypeColumns();

        // set the default columns for this table to be those used for the Manage Guide Set page
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(detailsCol.getFieldKey());
        defaultCols.add(FieldKey.fromParts("ControlName"));
        defaultCols.add(FieldKey.fromParts("AnalyteName"));
        defaultCols.add(FieldKey.fromParts("Isotype"));
        defaultCols.add(FieldKey.fromParts("Conjugate"));
        defaultCols.add(valueBasedCol.getFieldKey());
        defaultCols.add(FieldKey.fromParts("Created"));
        defaultCols.add(FieldKey.fromParts("CurrentGuideSet"));
        defaultCols.add(FieldKey.fromParts("Comment"));
        setDefaultVisibleColumns(defaultCols);
    }

    /** Add a flavor of FI columns (as of this writing, used by SinglePointControls and Titrations) to the current table
     * @param joinTable the table to join to for the actual FI data
     * @param srcFIColumnName name of the FI column in the join table
     * @param targetColumnNamePrefix prefix to be used for the column name to be added to the current table
     * @param targetColumnLabelPrefix prefix to be used for the column label to be added to the current table
     */
    private void addFIColumns(TableInfo joinTable, String srcFIColumnName, String targetColumnNamePrefix, String targetColumnLabelPrefix, String guideSetColumnName)
    {
        List<ColumnInfo> columns = Arrays.asList(
                joinTable.getColumn(guideSetColumnName),
                joinTable.getColumn("IncludeInGuideSetCalculation"),
                joinTable.getColumn(srcFIColumnName));
        SQLFragment baseSQL = new SQLFragment(" FROM (");
        baseSQL.append(QueryService.get().getSelectSQL(joinTable, columns, null, null, Table.ALL_ROWS, 0, false));
        baseSQL.append(") x WHERE x.");
        baseSQL.append(guideSetColumnName);
        baseSQL.append(" = ");
        baseSQL.append(ExprColumn.STR_TABLE_ALIAS);
        baseSQL.append(".RowId AND x.IncludeInGuideSetCalculation = ?");
        baseSQL.add(Boolean.TRUE);
        baseSQL.append(")");

        SQLFragment averageSQL = new SQLFragment("(SELECT AVG(x.");
        averageSQL.append(srcFIColumnName);
        averageSQL.append(")");
        averageSQL.append(baseSQL);
        ExprColumn maxFIAverageCol = new ExprColumn(this, targetColumnNamePrefix + "FIAverage", averageSQL, JdbcType.DOUBLE);
        maxFIAverageCol.setLabel(targetColumnLabelPrefix + "FI Average");
        maxFIAverageCol.setFormat("0.00");
        addColumn(maxFIAverageCol);

        SQLFragment stdDevSQL = new SQLFragment("(SELECT ");
        stdDevSQL.append(LuminexProtocolSchema.getSchema().getSqlDialect().getStdDevFunction());
        stdDevSQL.append("(x.");
        stdDevSQL.append(srcFIColumnName);
        stdDevSQL.append(")");
        stdDevSQL.append(baseSQL);
        ExprColumn maxFIStdDevCol = new ExprColumn(this, targetColumnNamePrefix + "FIStdDev", stdDevSQL, JdbcType.DOUBLE);
        maxFIStdDevCol.setLabel(targetColumnLabelPrefix + " FI StdDev");
        maxFIStdDevCol.setFormat("0.00");
        addColumn(maxFIStdDevCol);
    }
    
    private void addRunCounts() {
        SQLFragment runCountsBaseSQL = new SQLFragment("(SELECT COUNT(*) ");
        runCountsBaseSQL.append("FROM ");
        runCountsBaseSQL.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");

        runCountsBaseSQL.append(" JOIN ");
        runCountsBaseSQL.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "at");
        runCountsBaseSQL.append(" ON gs.RowId = at.GuideSetId ");

        // do MaxFI run count before joining in CurveFit table
        SQLFragment maxFIRunCountsSQL = new SQLFragment("(SELECT CASE IsTitration ");
        maxFIRunCountsSQL.append("WHEN ? THEN (SELECT COUNT(*) FROM ");
        maxFIRunCountsSQL.add(Boolean.TRUE);

        maxFIRunCountsSQL.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");
        maxFIRunCountsSQL.append(" JOIN ");
        maxFIRunCountsSQL.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "at");
        maxFIRunCountsSQL.append(" ON gs.RowId = at.GuideSetId ");
        maxFIRunCountsSQL.append("WHERE gs.RowId = ");
        maxFIRunCountsSQL.append(ExprColumn.STR_TABLE_ALIAS);
        maxFIRunCountsSQL.append(".RowId AND at.IncludeInGuideSetCalculation = ? AND at.MaxFI IS NOT NULL) ");
        maxFIRunCountsSQL.add(Boolean.TRUE);

        maxFIRunCountsSQL.append("ELSE (SELECT COUNT(*) FROM ");

        maxFIRunCountsSQL.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");
        maxFIRunCountsSQL.append(" JOIN ");
        maxFIRunCountsSQL.append(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), "aspc");
        maxFIRunCountsSQL.append(" ON gs.RowId = aspc.GuideSetId ");
        maxFIRunCountsSQL.append("WHERE gs.RowId = ");
        maxFIRunCountsSQL.append(ExprColumn.STR_TABLE_ALIAS);
        maxFIRunCountsSQL.append(".RowId AND aspc.IncludeInGuideSetCalculation = ?) ");
        maxFIRunCountsSQL.add(Boolean.TRUE);

        maxFIRunCountsSQL.append("END FROM ");
        maxFIRunCountsSQL.append("(SELECT gs.* FROM ");
        maxFIRunCountsSQL.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");

        maxFIRunCountsSQL.append(" WHERE gs.RowId = ");
        maxFIRunCountsSQL.append(ExprColumn.STR_TABLE_ALIAS);
        maxFIRunCountsSQL.append(".RowId) AS t)"); // needs some name

        ExprColumn maxFIRunCounts = new ExprColumn(this, "MaxFIRunCount", maxFIRunCountsSQL, JdbcType.INTEGER);
        maxFIRunCounts.setLabel("MaxFI Run Count");
        addColumn(maxFIRunCounts);

        // finish runCountsBaseSQL by joining in CurveFit table
        runCountsBaseSQL.append(" JOIN ");
        runCountsBaseSQL.append(LuminexProtocolSchema.getTableInfoCurveFit(), "cf");
        runCountsBaseSQL.append(" ON cf.AnalyteId = at.AnalyteId AND cf.TitrationId = at.TitrationId ");

        runCountsBaseSQL.append("WHERE gs.RowId = ");
        runCountsBaseSQL.append(ExprColumn.STR_TABLE_ALIAS);
        runCountsBaseSQL.append(".RowId AND at.IncludeInGuideSetCalculation = ? ");
        runCountsBaseSQL.add(Boolean.TRUE);

        SQLFragment ec504plRunCountsSQL = new SQLFragment(runCountsBaseSQL);
        ec504plRunCountsSQL.append("AND cf.CurveType='Four Parameter' AND cf.EC50 IS NOT NULL AND cf.FailureFlag IS NULL)");
        ExprColumn ec504plRunCounts = new ExprColumn(this, "EC504PLRunCount", ec504plRunCountsSQL, JdbcType.INTEGER);
        ec504plRunCounts.setLabel("EC50 4PL Run Count");
        addColumn(ec504plRunCounts);

        SQLFragment ec505plRunCountsSQL = new SQLFragment(runCountsBaseSQL);
        ec505plRunCountsSQL.append("AND cf.CurveType='Five Parameter' AND cf.EC50 IS NOT NULL AND cf.FailureFlag IS NULL)");
        ExprColumn ec505plRunCounts = new ExprColumn(this, "EC505PLRunCount", ec505plRunCountsSQL, JdbcType.INTEGER);
        ec505plRunCounts.setLabel("EC50 5PL Run Count");
        addColumn(ec505plRunCounts);

        SQLFragment aucRunCountsSQL = new SQLFragment(runCountsBaseSQL);
        aucRunCountsSQL.append("AND cf.CurveType='Trapezoidal' AND cf.AUC IS NOT NULL AND cf.FailureFlag IS NULL)");
        ExprColumn aucRunCounts = new ExprColumn(this, "AUCRunCount", aucRunCountsSQL, JdbcType.INTEGER);
        aucRunCounts.setLabel("AUC Run Count");
        addColumn(aucRunCounts);
    }

    protected LookupForeignKey createCurveFitFK(final String curveType)
    {
        return new LookupForeignKey("GuideSetId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createGuideSetCurveFitTable(curveType);
            }
        };
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        // Guide sets are scoped to the protocol, not to folders, so filter on ProtocolId instead of Container
        SQLFragment sql = new SQLFragment("ProtocolId = ?");
        sql.add(_userSchema.getProtocol().getRowId());
        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // First check if the user has the permission in the folder where the assay is defined
        if (_userSchema.getProtocol().getContainer().hasPermission(user, perm))
        {
            return true;
        }

        // Then look if they have the permission in any of the folders where there are runs for the assay design
        for (Container container : _userSchema.getProtocol().getExpRunContainers())
        {
            if (container.hasPermission(user, perm))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new GuideSetTableUpdateService(this);
    }

    public static class GuideSetTableUpdateService extends RowIdQueryUpdateService<GuideSet>
    {
        private ExpProtocol _protocol;
        private LuminexProtocolSchema _userSchema;

        public GuideSetTableUpdateService(GuideSetTable guideSetTable)
        {
            super(guideSetTable);
            _userSchema = guideSetTable._userSchema;
            _protocol = guideSetTable._userSchema.getProtocol();
        }

        public static GuideSet getMatchingCurrentGuideSet(@NotNull ExpProtocol protocol, String analyteName, String controlName, String conjugate, String isotype, Boolean isTitration)
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM ");
            sql.append(LuminexProtocolSchema.getTableInfoGuideSet(), "gs");
            sql.append(" WHERE ProtocolId = ?");
            sql.add(protocol.getRowId());
            sql.append(" AND AnalyteName");
            appendNullableString(sql, analyteName);
            sql.append(" AND ControlName");
            appendNullableString(sql, controlName);
            sql.append(" AND Conjugate");
            appendNullableString(sql, conjugate);
            sql.append(" AND Isotype");
            appendNullableString(sql, isotype);
            sql.append(" AND CurrentGuideSet = ?");
            sql.add(true);
            sql.append(" AND IsTitration = ?");
            sql.add(isTitration);

            GuideSet[] matches = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getArray(GuideSet.class);
            if (matches.length == 1)
            {
                return matches[0];
            }
            if (matches.length == 0)
            {
                return null;
            }

            throw new IllegalStateException("More than one guide set is current for assay design '" + protocol.getName() + "', analyte '" + analyteName + "', conjugate '" + conjugate + "', isotype '" + isotype + "'");
        }

        private static void appendNullableString(SQLFragment sql, String value)
        {
            if (value == null)
            {
                sql.append(" IS NULL ");
            }
            else
            {
                sql.append(" = ?");
                sql.add(value);
            }
        }

        @Override
        public GuideSet get(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
        {
            return new TableSelector(LuminexProtocolSchema.getTableInfoGuideSet()).getObject(key, GuideSet.class);
        }

        @Override
        public void delete(User user, Container container, int key) throws QueryUpdateServiceException, SQLException
        {
            DbScope scope = LuminexProtocolSchema.getSchema().getScope();
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("GuideSetId"), key);

            try (DbScope.Transaction tx = scope.ensureTransaction())
            {
                // NOTE: room to be smart here and only clean up ASPC or AT because a single GS should not be split across these tables.

                // Issue 22980: when deleting a guide set, we can bulk delete QC Flags instead of using the updateQCFlags method
                for (String flagType : LuminexDataHandler.getAllGuideSetFlagTypes())
                {
                    removeGuideSetQCFlagsByFlagType(key, flagType);
                }

                // update rows in AnalayteSinglePointControl table
                List<AnalyteSinglePointControl> aspcResults = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), filter, null).getArrayList(AnalyteSinglePointControl.class);
                for (AnalyteSinglePointControl aspc : aspcResults)
                {
                    Map<String, Object> keys = new CaseInsensitiveHashMap<>();
                    keys.put("AnalyteId", aspc.getAnalyteId());
                    keys.put("SinglePointControlId", aspc.getSinglePointControlId());

                    aspc.setGuideSetId(null);
                    aspc.setIncludeInGuideSetCalculation(false);
                    Table.update(user, LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), aspc, keys);
                }

                // update rows in AnalayteTitration table
                List<AnalyteTitration> atResults = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteTitration(), filter, null).getArrayList(AnalyteTitration.class);
                for (AnalyteTitration at : atResults )
                {
                    Map<String, Object> keys = new CaseInsensitiveHashMap<>();
                    keys.put("AnalyteId", at.getAnalyteId() );
                    keys.put("TitrationId", at.getTitrationId() );

                    at.setGuideSetId(null);
                    at.setIncludeInGuideSetCalculation(false);
                    Table.update(user, LuminexProtocolSchema.getTableInfoAnalyteTitration(), at, keys);
                }

                // delete the guide set row
                Table.delete(LuminexProtocolSchema.getTableInfoGuideSet(), key);
                tx.commit();
            }
        }

        @Override
        protected GuideSet createNewBean()
        {
            return new GuideSet();
        }

        @Override
        protected GuideSet insert(User user, Container container, GuideSet bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
        {
            validateProtocol(bean);
            validateGuideSetValues(bean);
            boolean current = bean.isCurrentGuideSet();
            if (current && getMatchingCurrentGuideSet(_protocol, bean.getAnalyteName(), bean.getControlName(), bean.getConjugate(), bean.getIsotype(), bean.getIsTitration()) != null)
            {
                throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/Conjugate/Isotype combination");
            }
            return Table.insert(user, LuminexProtocolSchema.getTableInfoGuideSet(), bean);
        }

        @Override
        protected GuideSet update(User user, Container container, GuideSet bean, Integer oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
        {
            if (oldKey == null)
            {
                throw new ValidationException("RowId is required for updates");
            }
            validateGuideSetValues(bean);
            if (bean.isCurrentGuideSet())
            {
                GuideSet currentGuideSet = getMatchingCurrentGuideSet(_protocol, bean.getAnalyteName(), bean.getControlName(), bean.getConjugate(), bean.getIsotype(), bean.getIsTitration());
                if (currentGuideSet != null && currentGuideSet.getRowId() != oldKey.intValue())
                {
                    throw new ValidationException("There is already a current guide set for that ProtocolId/AnalyteName/ControlName/Conjugate/Isotype combination");
                }
            }

            GuideSet oldBean = new TableSelector(LuminexProtocolSchema.getTableInfoGuideSet(), new SimpleFilter(FieldKey.fromParts("RowId"), oldKey), null).getObject(GuideSet.class);
            if (bean.hasUneditablePropertyChanged(oldBean))
            {
                throw new ValidationException("The following properties should not be updated for an existing guide set: " + bean.getUneditablePropertyNames());
            }

            GuideSet updatedGuideSet = Table.update(user, LuminexProtocolSchema.getTableInfoGuideSet(), bean, oldKey);

            // we will need to update QC Flags if one of the value-based or run-based QC flag properties has changed
            List<String> qcFlagTypesNowEnabled = bean.getQCFlagTypesForChanged(oldBean, true);
            List<String> qcFlagTypesNowDisabled = bean.getQCFlagTypesForChanged(oldBean, false);
            boolean hasValueBasedQCRelatedPropertyChanged = bean.hasValueBasedQCRelatedPropertyChanged(oldBean);
            if (hasValueBasedQCRelatedPropertyChanged || !qcFlagTypesNowEnabled.isEmpty() || !qcFlagTypesNowDisabled.isEmpty())
            {
                // Issue 22980: bulk delete of QC flags by guide set and type for perf improvement
                if (!qcFlagTypesNowDisabled.isEmpty())
                {
                    for (String flagType : qcFlagTypesNowDisabled)
                    {
                        removeGuideSetQCFlagsByFlagType(updatedGuideSet.getRowId(), flagType);
                    }
                }

                // Only need to call the updateQCFlags method if we have QC flags to insert/update
                if (hasValueBasedQCRelatedPropertyChanged || !qcFlagTypesNowEnabled.isEmpty())
                {
                    SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("GuideSetId"), updatedGuideSet.getRowId());
                    TableSelector selector = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), filter, null);
                    List<AnalyteSinglePointControl> singlePointControls = selector.getArrayList(AnalyteSinglePointControl.class);
                    for (AnalyteSinglePointControl singlePointControl : singlePointControls)
                    {
                        singlePointControl.updateQCFlags(_userSchema);
                    }

                    selector = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteTitration(), filter, null);
                    List<AnalyteTitration> titrations = selector.getArrayList(AnalyteTitration.class);
                    for (AnalyteTitration titration : titrations)
                    {
                        titration.updateQCFlags(_userSchema);
                    }
                }
            }

            return updatedGuideSet;
        }

        private void validateProtocol(GuideSet bean) throws ValidationException
        {
            int protocolId = bean.getProtocolId();
            if (protocolId == 0)
            {
                bean.setProtocolId(_protocol.getRowId());
            }
            else
            {
                if (protocolId != _protocol.getRowId())
                {
                    throw new ValidationException("ProtocolId must be set to " + _protocol.getRowId());
                }
            }
        }

        private void validateGuideSetValues(GuideSet bean) throws ValidationException
        {
            int maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("AnalyteName").getScale();
            if (bean.getAnalyteName() != null && bean.getAnalyteName().length() > maxLength)
            {
                throw new ValidationException("AnalyteName value '" + bean.getAnalyteName() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("Conjugate").getScale();
            if (bean.getConjugate() != null && bean.getConjugate().length() > maxLength)
            {
                throw new ValidationException("Conjugate value '" + bean.getConjugate() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("Isotype").getScale();
            if (bean.getIsotype() != null && bean.getIsotype().length() > maxLength)
            {
                throw new ValidationException("Isotype value '" + bean.getIsotype() + "' is too long, maximum length is " + maxLength + " characters");
            }
            maxLength = LuminexProtocolSchema.getTableInfoGuideSet().getColumn("ControlName").getScale();
            if (bean.getControlName() != null && bean.getControlName().length() > maxLength)
            {
                throw new ValidationException("ControlName value '" + bean.getControlName() + "' is too long, maximum length is " + maxLength + " characters");
            }

            if (!bean.isValueBased() && bean.hasMetricValues())
            {
                throw new ValidationException("Metric values should only be provided for value-based guide sets.");
            }
        }

        /*
         * Remove all QC Flags associated with the guide set and metric being disabled.
         * Return the number of QC Flag records that were deleted.
         */
        private void removeGuideSetQCFlagsByFlagType(int guideSetRowId, String flagType)
        {
            TableInfo ti = LuminexProtocolSchema.getTableInfoAnalyteTitration();
            String colName = LuminexDataHandler.QC_FLAG_TITRATION_ID;
            if (LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE.equals(flagType))
            {
                ti = LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl();
                colName = LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_ID;
            }

            // SQL IN clause to get the RowIDs of the QCFlag records associated with this guide set
            SQLFragment inClauseSql = new SQLFragment("RowId IN (SELECT qc.RowId FROM ");
            inClauseSql.append(ti, "a");
            inClauseSql.append(" LEFT JOIN ");
            inClauseSql.append(ExperimentService.get().getTinfoAssayQCFlag(), "qc");
            inClauseSql.append(" ON a.AnalyteId = qc.IntKey1 AND a.").append(colName).append(" = qc.IntKey2 ");
            inClauseSql.append(" WHERE a.GuideSetId = ? AND qc.FlagType = ?)");
            Object[] params = new Object[]{guideSetRowId, flagType};

            SimpleFilter filter = new SimpleFilter();
            filter.addWhereClause(inClauseSql.getSQL(), params, FieldKey.fromParts("RowId"));
            Table.delete(ExperimentService.get().getTinfoAssayQCFlag(), filter);
        }
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        if (name.equalsIgnoreCase("TitrationName"))
        {
            return getColumn("ControlName");
        }
        return super.resolveColumn(name);
    }

}
