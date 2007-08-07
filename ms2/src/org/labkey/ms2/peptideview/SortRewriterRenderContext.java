package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;

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


    protected Sort buildSort(TableInfo tinfo, ViewURLHelper url, String name)
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


    protected SimpleFilter buildFilter(TableInfo tinfo, ViewURLHelper url, String name)
    {
        SimpleFilter result = super.buildFilter(tinfo, url, name);
        if (_nestingOption != null)
        {
            result.addCondition(_nestingOption.getRowIdColumnName(), null, CompareType.NONBLANK);
        }
        return result;
    }
}
