/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.elisa.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.elisa.ElisaProtocolSchema;

public class CurveFitTable extends FilteredTable<AssayProtocolSchema>
{
    public CurveFitTable(@NotNull AssayProtocolSchema userSchema, @Nullable ContainerFilter cf)
    {
        super(ElisaProtocolSchema.getTableInfoCurveFit(), userSchema, cf);

        ExpProtocol protocol = userSchema.getProtocol();

        setDescription("Contains standard curve fit information for the " + protocol.getName() + " assay definition");
        setName(ElisaProtocolSchema.CURVE_FIT_TABLE_NAME);

        for (ColumnInfo col : getRealTable().getColumns())
        {
            var newCol = addWrapColumn(col);

            if (newCol.getName().equalsIgnoreCase("RowId") || newCol.getName().equalsIgnoreCase("ProtocolId"))
            {
                newCol.setHidden(true);
            }
        }
        addCondition(getRealTable().getColumn("ProtocolId"), protocol.getRowId());
    }
}
