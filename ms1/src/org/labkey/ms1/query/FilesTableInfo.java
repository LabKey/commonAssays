/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.ms1.MS1Manager;

import java.util.Collection;

/**
 * User schema table info for the ms1.Files table
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 23, 2007
 * Time: 2:01:34 PM
 */
public class FilesTableInfo extends FilteredTable
{
    public FilesTableInfo(ExpSchema expSchema, ContainerFilter filter)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FILES));

        _expSchema = expSchema;
        wrapAllColumns(true);

        getColumn("FileId").setIsHidden(true);
        ColumnInfo edfid = getColumn("ExpDataFileId");
        edfid.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _expSchema.createDatasTable();
            }
        });

        //add a condition that excludes deleted and not full-imported files
        //also limit to the passed container if not null
        SQLFragment sf = new SQLFragment("Imported=? AND Deleted=?");
        sf.add(true);
        sf.add(false);
        Collection<String> containerIds = filter.getIds(_expSchema.getContainer());
        if (containerIds != null)
        {
            sf.append(" AND ExpDataFileId IN (SELECT RowId FROM Exp.Data WHERE Container IN (");
            String separator = "";
            for (String containerId : containerIds)
            {
                sf.append(separator);
                separator = ", ";
                sf.append("?");
                sf.add(containerId);
            }
            sf.append("))");
        }
        addCondition(sf, "Imported", "Deleted", "ExpDataFileId");
    }

    private ExpSchema _expSchema;
}
