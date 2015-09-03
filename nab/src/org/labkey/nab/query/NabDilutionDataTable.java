/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.nab.query;

import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.api.ExpProtocol;

/**
 * Created by davebradlee on 7/10/15.
 *
 */
public class NabDilutionDataTable extends NabBaseTable
{
    public NabDilutionDataTable(final NabProtocolSchema schema, ExpProtocol protocol)
    {
        super(schema, DilutionManager.getTableInfoDilutionData(), protocol);
        addRunColumn();

        for (ColumnInfo col : getRealTable().getColumns())
        {
            if ("RunId".equalsIgnoreCase(col.getName()))
                continue;

            if ("MaxDilution".equalsIgnoreCase(col.getName()))
                continue;           // Do with MinDilution to ensure ordering (hack because SqlServer DB order is reversed)

            String name = col.getName();
            if ("RunDataId".equalsIgnoreCase(name))
                name = "RunData";
            ColumnInfo newCol = addWrapColumn(name, col);
            if (col.isHidden())
            {
                newCol.setHidden(col.isHidden());
            }
            if ("MinDilution".equalsIgnoreCase(col.getName()))
            {
                ColumnInfo maxCol = getRealTable().getColumn("MaxDilution");
                if (null != maxCol)
                    addWrapColumn(maxCol.getName(), maxCol);
            }
        }

        addCondition(getRealTable().getColumn("ProtocolId"), protocol.getRowId());
    }
}
