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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.assay.AssayProtocolSchema;

/**
 * User: gktaylor
 * Date: Aug 9, 2013
 */
public class SinglePointControlTable extends AbstractLuminexTable
{
    public SinglePointControlTable(LuminexProtocolSchema schema, ContainerFilter cf, boolean filterTable)
    {
        // expose the actual columns in the table
        super(LuminexProtocolSchema.getTableInfoSinglePointControl(), schema, cf, filterTable);
        setName(LuminexProtocolSchema.SINGLE_POINT_CONTROL_TABLE_NAME);
        addWrapColumn(getRealTable().getColumn("RowId"));
        addWrapColumn(getRealTable().getColumn("Name"));

        // Alias the RunId column to be consistent with other Schema columns
        var runColumn = addColumn(wrapColumn("Run", getRealTable().getColumn("RunId")));
        runColumn.setFk( QueryForeignKey.from(schema, cf).to(AssayProtocolSchema.RUNS_TABLE_NAME, "RowId", "Name") );
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter)
    {
        return getUserSchema().createRunIdContainerFilterSQL(filter);
    }


}
