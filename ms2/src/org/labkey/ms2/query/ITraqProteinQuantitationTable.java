/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.labkey.api.query.FilteredTable;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Apr 17, 2007
 */
public class ITraqProteinQuantitationTable extends FilteredTable<MS2Schema>
{
    public ITraqProteinQuantitationTable(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoITraqProteinQuantitation(), schema);
        wrapAllColumns(true);
        getColumn("ProteinGroupId").setHidden(true);
    }
}
