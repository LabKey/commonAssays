/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.labkey.api.admin.CoreUrls;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.ContainerTable;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Feb 22, 2007
 */
public class RunTableInfo extends FilteredTable
{
    private final MS2Schema _schema;

    public RunTableInfo(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoRuns());
        _schema = schema;

        wrapAllColumns(true);

        ActionURL url = PageFlowUtil.urlProvider(CoreUrls.class).getContainerRedirectURL(ContainerManager.getRoot(), "ms2", "showList.view");
        LookupForeignKey containerFK = new LookupForeignKey(url, "containerId", "EntityId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return new ContainerTable(_schema);
            }
        };
        getColumn("Container").setFk(containerFK);
        getColumn("Container").setHidden(true);

        ColumnInfo folderColumn = wrapColumn("Folder", getRealTable().getColumn("Container"));
        folderColumn.setHidden(false);
        folderColumn.setFk(containerFK);
        addColumn(folderColumn);

        ColumnInfo erLSIDColumn = getColumn("ExperimentRunLSID");
        erLSIDColumn.setLabel("Experiment Run");
        erLSIDColumn.setFk(new LookupForeignKey("LSID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpSchema schema = new ExpSchema(_schema.getUser(), _schema.getContainer());
                return schema.getRunsTable();
            }
        });
    }
}
