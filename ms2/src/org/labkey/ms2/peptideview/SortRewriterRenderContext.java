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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;

/**
 * User: jeckels
* Date: Apr 11, 2007
*/
public class SortRewriterRenderContext extends RenderContext
{
    private QueryNestingOption _nestingOption;

    public SortRewriterRenderContext(QueryNestingOption nestingOption, ViewContext context)
    {
        super(context);
        _nestingOption = nestingOption;
    }


    protected Sort buildSort(TableInfo tinfo, ActionURL url, String name)
    {
        Sort standardSort = super.buildSort(tinfo, url, name);
        if (_nestingOption != null)
        {
            boolean foundGroupId = false;
            standardSort.getSortList();
            Sort sort = new Sort();

            int totalIndex = 0;
            int outerIndex = 0;
            for (Sort.SortField field : standardSort.getSortList())
            {
                boolean innerColumn = _nestingOption.isOuter(field.getColumnName());
                foundGroupId = foundGroupId || field.getColumnName().equalsIgnoreCase(_nestingOption.getRowIdColumnName());
                sort.insertSortColumn(field.toUrlString(), field.isUrlClause(), innerColumn ? outerIndex++ : totalIndex);
                totalIndex++;
            }

            if (!foundGroupId)
            {
                sort.insertSortColumn(_nestingOption.getRowIdColumnName(), false, outerIndex++);
            }

            return sort;
        }
        else
        {
            return standardSort;
        }
    }


    protected SimpleFilter buildFilter(TableInfo tinfo, ActionURL url, String name)
    {
        SimpleFilter result = super.buildFilter(tinfo, url, name);
        if (_nestingOption != null)
        {
            result.addCondition(_nestingOption.getRowIdColumnName(), null, CompareType.NONBLANK);
        }
        return result;
    }
}
