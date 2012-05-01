package org.labkey.ms2;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;

/**
 * User: jeckels
 * Date: Apr 30, 2012
 */
public class AbstractPeptideDisplayColumn extends SimpleDisplayColumn
{
    /** Look for a value based first on a ColumnInfo, and then falling back on alternative aliases it might have in the ResultSet */
    protected Object getColumnValue(RenderContext ctx, ColumnInfo colInfo, String... alternates)
    {
        Object result = null;
        if (colInfo != null)
        {
            result = ctx.get(colInfo.getAlias());
        }
        if (result == null)
        {
            for (String alternate : alternates)
            {
                result = ctx.get(alternate);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return result;
    }

}
