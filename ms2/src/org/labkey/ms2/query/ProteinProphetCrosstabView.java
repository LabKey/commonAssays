/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 1, 2008
 */
public class ProteinProphetCrosstabView extends AbstractQueryCrosstabView
{
    public ProteinProphetCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ViewContext viewContext)
    {
        super(schema, form, viewContext, MS2Schema.HiddenTableType.ProteinProphetCrosstab);
    }

    protected TableInfo createTable()
    {
        return _schema.createProteinProphetCrosstabTable(_form, getViewContext());
    }

    @Override
    protected Sort getBaseSort()
    {
        return new Sort(CrosstabTable.getDefaultSortString() + ",SeqId/BestName");
    }

    protected FieldKey getComparisonColumn()
    {
        return FieldKey.fromParts(AggregateColumnInfo.NAME_PREFIX + "MIN_ProteinGroupId", "Group");
    }
}
