/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.MultiValuedRenderContext;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunDatabaseContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.luminex.LuminexManager;
import org.labkey.luminex.LuminexRunCreator;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public class WellExclusionTable extends AbstractExclusionTable
{
    public WellExclusionTable(LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoWellExclusion(), schema, filter);

        getColumn("DataId").setLabel("Data File");
        getColumn("DataId").setFk(new ExpSchema(schema.getUser(), schema.getContainer()).getDataIdForeignKey());
        
        getColumn("Analytes").setFk(new MultiValuedForeignKey(new LookupForeignKey("WellExclusionId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createWellExclusionAnalyteTable();
            }
        }, "AnalyteId"));
        getColumn("Analytes").setUserEditable(false);

        SQLFragment joinSQL = new SQLFragment(" FROM ");
        joinSQL.append(LuminexProtocolSchema.getTableInfoDataRow(), "dr");
        joinSQL.append(" WHERE (dr.Description = ");
        joinSQL.append(ExprColumn.STR_TABLE_ALIAS);
        joinSQL.append(".Description OR (dr.Description IS NULL AND ");
        joinSQL.append(ExprColumn.STR_TABLE_ALIAS);
        joinSQL.append(".Description IS NULL)) AND dr.DataId = ");
        joinSQL.append(ExprColumn.STR_TABLE_ALIAS);
        joinSQL.append(".DataId AND dr.Type = ");
        joinSQL.append(ExprColumn.STR_TABLE_ALIAS);
        joinSQL.append(".Type");

        SQLFragment wellRoleSQL = new SQLFragment("SELECT WellRole FROM (SELECT DISTINCT dr.WellRole");
        wellRoleSQL.append(joinSQL);
        wellRoleSQL.append(") x");
        addColumn(new ExprColumn(this, "Well Role", schema.getDbSchema().getSqlDialect().getSelectConcat(wellRoleSQL, ","), JdbcType.VARCHAR));

        SQLFragment wellSQL = new SQLFragment("SELECT Well FROM (SELECT DISTINCT dr.Well").append(joinSQL).append(") x");
        //only pull in wells list for replicate group exclusions
        wellSQL.append(" WHERE ").append(ExprColumn.STR_TABLE_ALIAS).append(".Well IS NULL");
        ExprColumn wellsCol = new ExprColumn(this, "Wells", schema.getDbSchema().getSqlDialect().getSelectConcat(wellSQL, ","), JdbcType.VARCHAR);
        wellsCol.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Object o = getDisplayValue(ctx);
                out.write(null == o ? "&nbsp;" : PageFlowUtil.filter(o.toString()));
            }

            @Override
            public Object getDisplayValue(RenderContext ctx)
            {
                Object result = getValue(ctx);
                if (null != result)
                {
                    // get the list of unique wells (by splitting the concatenated string)
                    TreeSet<String> uniqueWells = new TreeSet<>();
                    uniqueWells.addAll(Arrays.asList(result.toString().split(MultiValuedRenderContext.VALUE_DELIMITER_REGEX)));

                    // put the unique wells back into a comma separated string
                    StringBuilder sb = new StringBuilder();
                    String comma = "";
                    for (String well : uniqueWells)
                    {
                        sb.append(comma);
                        sb.append(well);
                        comma = ",";
                    }
                    result = sb.toString();
                }
                return result;
            }
        });
        addColumn(wellsCol);

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.remove(FieldKey.fromParts("ModifiedBy"));
        defaultCols.remove(FieldKey.fromParts("Modified"));
        defaultCols.add(0, FieldKey.fromParts("DataId", "Run"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("DataId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), container));
        sql.append(")");
        return sql;
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExclusionUpdateService(this, getRealTable(), LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "WellExclusionId")
        {
            private Set<ExpRun> _runsToRefresh = new HashSet<>();

            private Integer getDataId(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer dataId = convertToInteger(rowMap.get("DataId"));
                if (dataId == null)
                {
                    throw new QueryUpdateServiceException("No DataId specified");
                }
                return dataId;
            }

            @Override
            protected void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission> permission) throws QueryUpdateServiceException
            {
                ExpData data = getData(rowMap);
                if (!data.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }

            private ExpData getData(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer dataId = getDataId(rowMap);
                ExpData data = ExperimentService.get().getExpData(dataId);
                if (data == null)
                {
                    throw new QueryUpdateServiceException("No such data file: " + dataId);
                }
                return data;
            }

            @Override
            protected @NotNull ExpRun resolveRun(Map<String, Object> rowMap) throws QueryUpdateServiceException, SQLException
            {
                ExpData data = getData(rowMap);
                ExpProtocolApplication protApp = data.getSourceApplication();
                if (protApp == null)
                {
                    throw new QueryUpdateServiceException("Unable to resolve run for data " + data.getRowId() + ", no source protocol application");
                }
                ExpRun run = protApp.getRun();
                if (run == null)
                {
                    throw new QueryUpdateServiceException("Unable to resolve run for data " + data.getRowId());
                }
                if (!_runsToRefresh.contains(run))
                {
                    String description = rowMap.get("Description") == null ? null : rowMap.get("Description").toString();
                    String type = rowMap.get("Type") == null ? null : rowMap.get("Type").toString();
                    String bTRUE = getSchema().getSqlDialect().getBooleanTRUE();

                    SQLFragment dataRowSQL = new SQLFragment("SELECT * FROM ");
                    dataRowSQL.append(LuminexProtocolSchema.getTableInfoDataRow(), "dr");
                    //dataRowSQL.append(" LEFT JOIN ").append(LuminexProtocolSchema.getTableInfoTitration(), "t").append(" ON dr.TitrationId = t.RowId ");
                    //dataRowSQL.append(" WHERE dr.TitrationId IS NOT NULL ");
                    dataRowSQL.append(" WHERE dr.DataId = ? AND dr.Description ");
                    dataRowSQL.add(data.getRowId());

                    if (description == null)
                    {
                        dataRowSQL.append("IS NULL");
                    }
                    else
                    {
                        dataRowSQL.append("= ?");
                        dataRowSQL.add(description);
                    }

                    dataRowSQL.append(" AND dr.Type ");

                    if (type == null)
                    {
                        dataRowSQL.append("IS NULL");
                    }
                    else
                    {
                        dataRowSQL.append("= ?");
                        dataRowSQL.add(type);
                    }

                    if (new SqlSelector(LuminexProtocolSchema.getSchema(), dataRowSQL).exists())
                    {
                        _runsToRefresh.add(run);
                    }
                }

                return run;
            }

            @Override
            public List<Map<String, Object>> insertRows(User user, Container container, List<Map<String, Object>> rows, BatchValidationException errors, @Nullable Map<Enum, Object> configParameters, Map<String, Object> extraScriptContext) throws DuplicateKeyException, QueryUpdateServiceException, SQLException
            {
                // Only allow one thread to be running a Luminex transform script and importing its results at a time
                // See issue 17424
                synchronized (LuminexRunCreator.LOCK_OBJECT)
                {
                    try (DbScope.Transaction transaction = LuminexProtocolSchema.getSchema().getScope().ensureTransaction())
                    {
                        List<Map<String, Object>> result = super.insertRows(user, container, rows, errors, configParameters, extraScriptContext);

                        if (extraScriptContext != null && (Boolean)extraScriptContext.getOrDefault(LuminexManager.RERUN_TRANSFORM, false))
                            rerunTransformScripts(errors);

                        if (errors.hasErrors())
                            throw errors;

                        transaction.commit();
                        return result;
                    }
                    catch(BatchValidationException e)
                    {
                        throw new QueryUpdateServiceException(e.getMessage(), e);
                    }
                }
            }

            @Override
            public List<Map<String, Object>> deleteRows(User user, Container container, List<Map<String, Object>> keys, @Nullable Map<Enum, Object> configParameters, @Nullable Map<String, Object> extraScriptContext) throws InvalidKeyException, BatchValidationException, QueryUpdateServiceException, SQLException
            {
                // Only allow one thread to be running a Luminex transform script and importing its results at a time
                // See issue 17424
                synchronized (LuminexRunCreator.LOCK_OBJECT)
                {
                    try (DbScope.Transaction transaction = LuminexProtocolSchema.getSchema().getScope().ensureTransaction())
                    {
                        List<Map<String, Object>> result = super.deleteRows(user, container, keys, configParameters, extraScriptContext);

                        BatchValidationException errors = new BatchValidationException();
                        if (extraScriptContext != null && (Boolean)extraScriptContext.getOrDefault(LuminexManager.RERUN_TRANSFORM, false))
                            rerunTransformScripts(errors);

                        if (errors.hasErrors())
                            throw errors;

                        transaction.commit();
                        return result;
                    }
                }
            }

            @Override
            public List<Map<String, Object>> updateRows(User user, Container container, List<Map<String, Object>> rows, List<Map<String, Object>> oldKeys, @Nullable Map<Enum, Object> configParameters, Map<String, Object> extraScriptContext) throws InvalidKeyException, BatchValidationException, QueryUpdateServiceException, SQLException
            {
                // Only allow one thread to be running a Luminex transform script and importing its results at a time
                // See issue 17424
                synchronized (LuminexRunCreator.LOCK_OBJECT)
                {
                    try (DbScope.Transaction transaction = LuminexProtocolSchema.getSchema().getScope().ensureTransaction())
                    {
                        List<Map<String, Object>> result = super.updateRows(user, container, rows, oldKeys, configParameters, extraScriptContext);

                        BatchValidationException errors = new BatchValidationException();
                        if (extraScriptContext != null && (Boolean)extraScriptContext.getOrDefault(LuminexManager.RERUN_TRANSFORM, false))
                            rerunTransformScripts(errors);

                        if (errors.hasErrors())
                            throw errors;

                        transaction.commit();
                        return result;
                    }
                }
            }

            private void rerunTransformScripts(BatchValidationException errors) throws QueryUpdateServiceException
            {
                try
                {
                    for (ExpRun run : _runsToRefresh)
                    {
                        AssayProvider provider = AssayService.get().getProvider(run);
                        AssayRunDatabaseContext context = provider.createRunDatabaseContext(run, _userSchema.getUser(), null);
                        provider.getRunCreator().saveExperimentRun(context, AssayService.get().findBatch(run), run, false);
                    }
                }
                catch (ExperimentException e)
                {
                    throw new QueryUpdateServiceException(e);
                }
                catch (ValidationException e)
                {
                    errors.addRowError(e);
                }
            }
        };
    }
}
