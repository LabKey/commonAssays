/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 8, 2007
 */
public class ProteinProphetFileTableInfo extends FilteredTable
{
    private final MS2Schema _schema;

    public ProteinProphetFileTableInfo(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoProteinProphetFiles());
        _schema = schema;
        wrapAllColumns(true);

        ActionURL url = MS2Controller.getShowRunURL(_schema.getUser(), getContainer());
        getColumn("Run").setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                return new RunTableInfo(_schema);
            }
        });
    }
}
