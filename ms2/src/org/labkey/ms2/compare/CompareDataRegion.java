package org.labkey.ms2.compare;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.ms2.MS2Manager;

import java.util.List;
import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;

public class CompareDataRegion extends DataRegion
{
    List<String> _multiColumnCaptions;
    int _offset = 0;
    int _colSpan;
    private final ResultSet _rs;

    public CompareDataRegion(ResultSet rs)
    {
        _rs = rs;
        setName(MS2Manager.getDataRegionNameCompare());
        setShadeAlternatingRows(true);
        setShowColumnSeparators(true);
    }
    
    public ResultSet getResultSet()
    {
        return _rs;
    }

    public void setMultiColumnCaptions(List<String> multiColumnCaptions)
    {
        _multiColumnCaptions = multiColumnCaptions;
    }

    public void setColSpan(int colSpan)
    {
        _colSpan = colSpan;
    }

    public void setOffset(int offset)
    {
        _offset = offset;
    }

    protected void renderGridHeaders(RenderContext ctx, Writer out, DisplayColumn[] renderers) throws SQLException, IOException
    {
        // Add an extra row and render the multi-column captions
        out.write("<tr>");

        if (_showRecordSelectors)
            out.write("<td></td>");

        boolean shade = false;
        int columnIndex = 0;
        for (int i = 0; i < _offset; i++)
        {
            out.write("<td style=\"border-bottom: 1px solid rgb(170, 170, 170);");
            if (i == _offset - 1)
            {
                out.write("border-right: 1px solid rgb(170, 170, 170);");
            }
            out.write("\">&nbsp;</td>");
            if (shade)
            {
                renderers[columnIndex].setBackgroundColor("#EEEEEE");
            }
            shade = !shade;
            columnIndex++;
        }

        for (String caption : _multiColumnCaptions)
        {
            out.write("<td align=\"center\" colspan=\"" + _colSpan + "\" style=\"" + COLUMN_SEPARATOR_STYLE_ARRIBS + "; border-bottom: 1px solid rgb(170, 170, 170);\"");
            if (shade)
            {
                out.write(" bgcolor=\"#EEEEEE\"");
                for (int i = 0; i < _colSpan; i++)
                {
                    renderers[columnIndex++].setBackgroundColor("#EEEEEE");
                }
            }
            else
            {
                columnIndex += _colSpan;
            }

            out.write(">" + caption + "</td>");
            shade = !shade;
        }

        out.write("</tr>\n");

        super.renderGridHeaders(ctx, out, renderers);
    }
}
