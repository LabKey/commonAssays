package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * User: jeckels
 * Date: 6/29/11
 */
public class ExclusionUIDisplayColumn extends DataColumn
{
    private final FieldKey _dilutionFieldKey;
    private final FieldKey _descriptionFieldKey;
    private final FieldKey _dataFieldKey;
    private final FieldKey _runFieldKey;
    private final FieldKey _analyteFieldKey;

    public ExclusionUIDisplayColumn(ColumnInfo colInfo)
    {
        super(colInfo);
        FieldKey parentFK = colInfo.getFieldKey().getParent();

        _dilutionFieldKey = new FieldKey(parentFK, "Dilution");
        _descriptionFieldKey = new FieldKey(parentFK, "Description");
        _dataFieldKey = new FieldKey(new FieldKey(parentFK, "Data"), "RowId");
        _runFieldKey = new FieldKey(new FieldKey(new FieldKey(parentFK, "Data"), "Run"), "RowId");
        _analyteFieldKey = new FieldKey(new FieldKey(parentFK, "Analyte"), "RowId");
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_dilutionFieldKey);
        keys.add(_descriptionFieldKey);
        keys.add(_dataFieldKey);
        keys.add(_runFieldKey);
        keys.add(_analyteFieldKey);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Number dilution = (Number)ctx.get(_dilutionFieldKey);
        String description = (String)ctx.get(_descriptionFieldKey);
        Integer dataId = (Integer)ctx.get(_dataFieldKey);
        Integer runId = (Integer)ctx.get(_runFieldKey);
        Integer analyteId = (Integer)ctx.get(_analyteFieldKey);

        out.write("<a onclick=\"alert('dilution: " + dilution + ", description: " + description + ", dataId: " + dataId + ", runId: " + runId + ", analyteId: " + analyteId + "');\">");

        Boolean excluded = (Boolean)ctx.get(getColumnInfo().getFieldKey());
        if (excluded.booleanValue())
        {
            out.write("EXCLUDED!");
        }
        else
        {
            out.write("NOT EXCLUDED");
        }
        out.write("</a>");
    }
}
