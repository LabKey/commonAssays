/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.MS1Manager;

/**
 * User schema table info over ms1.Scans
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 23, 2007
 * Time: 2:52:32 PM
 */
public class ScansTableInfo extends FilteredTable
{
    public ScansTableInfo(MS1Schema schema, Container container)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_SCANS));

        _schema = schema;
        wrapAllColumns(true);

        ColumnInfo fid = getColumn("FileId");
        fid.setFk(new LookupForeignKey("FileId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.getFilesTableInfo();
            }
        });

        //add a condition that limits the files returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Files table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND f.Deleted=?)",
                                            container.getId(), true, false);
        addCondition(sf, "FileId");

    }

    private MS1Schema _schema;
}
