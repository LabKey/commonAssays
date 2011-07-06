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
    private final String _protocolName;
    private boolean _exclusionJSIncluded = false;

    public ExclusionUIDisplayColumn(ColumnInfo colInfo, String protocolName)
    {
        super(colInfo);
        FieldKey parentFK = colInfo.getFieldKey().getParent();

        _dilutionFieldKey = new FieldKey(parentFK, "Dilution");
        _descriptionFieldKey = new FieldKey(parentFK, "Description");
        _dataFieldKey = new FieldKey(new FieldKey(parentFK, "Data"), "RowId");
        _runFieldKey = new FieldKey(new FieldKey(new FieldKey(parentFK, "Data"), "Run"), "RowId");
        _analyteFieldKey = new FieldKey(new FieldKey(parentFK, "Analyte"), "RowId");
        _protocolName = protocolName;
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

        if (!_exclusionJSIncluded)
        {
            // add script block to include the necessary JS and CSS files for the exclusion popups
            out.write("<script type='text/javascript'>"
                    + "   LABKEY.requiresScript('AnalyteExclusionPanel.js');"
                    + "   LABKEY.requiresScript('WellExclusionPanel.js');"
                    + "   LABKEY.requiresCss('Exclusion.css');"
                    + "</script>");

            _exclusionJSIncluded = true;
        }

        // add onclick handler to call the well exclusion window creation function
        out.write("<a onclick=\"wellExclusionWindow('" + _protocolName + "', " + runId + ", " + dataId + ", '" + description + "', " + dilution + ");\">");

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
