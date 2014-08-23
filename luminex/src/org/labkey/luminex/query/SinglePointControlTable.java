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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.study.assay.AssayProtocolSchema;

/**
 * User: gktaylor
 * Date: Aug 9, 2013
 */
public class SinglePointControlTable extends AbstractLuminexTable
{
    public SinglePointControlTable(LuminexProtocolSchema schema, boolean filterTable)
    {
        // expose the actual columns in the table
        super(LuminexProtocolSchema.getTableInfoSinglePointControl(), schema, filterTable);
        setName(LuminexProtocolSchema.SINGLE_POINT_CONTROL_TABLE_NAME);
        addWrapColumn(getRealTable().getColumn("RowId"));
        addWrapColumn(getRealTable().getColumn("Name"));

        // Alias the RunId column to be consistent with other Schema columns
        ColumnInfo runColumn = addColumn(wrapColumn("Run", getRealTable().getColumn("RunId")));
        runColumn.setFk(new QueryForeignKey(schema, null, AssayProtocolSchema.RUNS_TABLE_NAME, "RowId", "Name"));
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


}
