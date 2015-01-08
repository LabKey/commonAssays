/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

package org.labkey.ms2.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Feb 22, 2007
 */
public class RunTableInfo extends FilteredTable<MS2Schema>
{
    public RunTableInfo(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoRuns(), schema);

        wrapAllColumns(true);

        DetailsURL url = new DetailsURL(new ActionURL(MS2Controller.ShowListAction.class, getContainer()));
        url.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("Container")));
        ColumnInfo containerColumn = getColumn("Container");
        ContainerForeignKey.initColumn(containerColumn, _userSchema, null);
        containerColumn.setURL(url);
        containerColumn.setHidden(true);

        ColumnInfo folderColumn = wrapColumn("Folder", getRealTable().getColumn("Container"));
        folderColumn.setHidden(false);
        ContainerForeignKey.initColumn(folderColumn, _userSchema, null);
        folderColumn.setURL(url);
        addColumn(folderColumn);

        ColumnInfo erLSIDColumn = getColumn("ExperimentRunLSID");
        erLSIDColumn.setLabel("Experiment Run");
        erLSIDColumn.setFk(new LookupForeignKey("LSID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpSchema schema = new ExpSchema(_userSchema.getUser(), _userSchema.getContainer());
                return schema.getRunsTable();
            }
        });
    }
}
