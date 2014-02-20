/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

package org.labkey.ms1.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.ms1.MS1Manager;

/**
 * User schema table info for the ms1.Files table
 *
 * User: Dave
 * Date: Oct 23, 2007
 * Time: 2:01:34 PM
 */
public class FilesTableInfo extends FilteredTable<ExpSchema>
{
    public FilesTableInfo(ExpSchema expSchema, ContainerFilter filter)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FILES), expSchema);

        wrapAllColumns(true);

        getColumn("FileId").setHidden(true);
        ColumnInfo edfid = getColumn("ExpDataFileId");
        edfid.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getDatasTable();
            }
        });

        //add a condition that excludes deleted and not full-imported files
        //also limit to the passed container if not null
        SQLFragment sf = new SQLFragment("Imported=? AND Deleted=? AND ExpDataFileId IN (SELECT RowId FROM Exp.Data WHERE ");
        sf.add(true);
        sf.add(false);
        sf.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), _userSchema.getContainer()));
        sf.append(")");
        addCondition(sf, FieldKey.fromParts("Imported"), FieldKey.fromParts("Deleted"), FieldKey.fromParts("ExpDataFileId"));
    }
}
