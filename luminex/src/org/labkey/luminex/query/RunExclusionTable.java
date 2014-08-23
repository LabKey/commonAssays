/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.view.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public class RunExclusionTable extends AbstractExclusionTable
{
    public RunExclusionTable(LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoRunExclusion(), schema, filter);

        getColumn("RunId").setLabel("Assay ID");
        getColumn("RunId").setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createRunsTable();
            }
        });

        getColumn("Analytes").setFk(new MultiValuedForeignKey(new LookupForeignKey("RunId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createRunExclusionAnalyteTable();
            }
        }, "AnalyteId"));

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.remove(FieldKey.fromParts("Modified"));
        defaultCols.remove(FieldKey.fromParts("ModifiedBy"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("RunId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), container));
        sql.append(")");
        return sql;
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExclusionUpdateService(this, getRealTable(), LuminexProtocolSchema.getTableInfoRunExclusionAnalyte(), "RunId")
        {
            @NotNull
            @Override
            protected ExpRun resolveRun(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer runId = convertToInteger(rowMap.get("RunId"));
                if (runId == null)
                {
                    throw new QueryUpdateServiceException("No RunId specified");
                }
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (run == null)
                {
                    throw new QueryUpdateServiceException("No such run: " + runId);
                }
                return run;
            }

            @Override
            protected void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission > permission) throws QueryUpdateServiceException
            {
                ExpRun run = resolveRun(rowMap);
                if (!run.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }
        };
    }
}
