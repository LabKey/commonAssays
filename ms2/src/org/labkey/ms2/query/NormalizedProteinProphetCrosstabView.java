/*
 * Copyright (c) 2010-2019 LabKey Corporation
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

import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 1, 2008
 */
public class NormalizedProteinProphetCrosstabView extends AbstractQueryCrosstabView
{
    public NormalizedProteinProphetCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ViewContext viewContext)
    {
        super(schema, form, viewContext, MS2Schema.HiddenTableType.ProteinProphetNormalizedCrosstab);
    }

    @Override
    protected TableInfo createTable()
    {
        return _schema.createNormalizedProteinProphetComparisonTable(_form, getViewContext());
    }

    @Override
    protected Sort getBaseSort()
    {
        Sort sort = new Sort("NormalizedId");
        sort.insertSort(new Sort(CrosstabTable.getDefaultSortString()));
        return sort;
    }
}
