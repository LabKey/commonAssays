package org.labkey.luminex;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;

import java.util.Set;
import java.io.Writer;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jul 30, 2007
 */
public class OutOfRangeDisplayColumn extends DataColumn
{
    private final ColumnInfo _oorIndicatorColumn;

    public OutOfRangeDisplayColumn(ColumnInfo numberColumn, ColumnInfo oorIndicatorColumn)
    {
        super(numberColumn);
        _oorIndicatorColumn = oorIndicatorColumn;
    }


    public Class getDisplayValueClass()
    {
        return String.class;
    }

    public String getFormattedValue(RenderContext ctx)
    {
        StringBuilder result = new StringBuilder();
        Object oorValue = _oorIndicatorColumn.getValue(ctx);
        if (oorValue != null)
        {
            result.append(oorValue);
        }
        result.append(super.getFormattedValue(ctx));
        return result.toString();
    }
    
    public Object getDisplayValue(RenderContext ctx)
    {
        return getFormattedValue(ctx);
    }

    public String getTsvFormattedValue(RenderContext ctx)
    {
        return getFormattedValue(ctx);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        String value = getFormattedValue(ctx);

        if ("".equals(value.trim()))
        {
            out.write("&nbsp;");
        }
        else
        {
            out.write(h(value));
        }
    }

    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);
        columns.add(_oorIndicatorColumn);
    }
}
