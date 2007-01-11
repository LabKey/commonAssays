package cpas.ms2.compare;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.ms2.MS2Manager;

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
        for (int i = 0; i < _offset; i++)
        {
            out.write("<td></td>");
            shade = !shade;
        }

        for (String caption : _multiColumnCaptions)
        {
            out.write("<td align=\"center\" colspan=" + _colSpan + " style='white-space:nowrap'");
            if (shade)
            {
                out.write(" bgcolor=\"#EEEEEE\"");
            }
            out.write("><b>" + caption + "</b></td>");
            shade = !shade;
        }

        out.write("</tr>\n");

        super.renderGridHeaders(ctx, out, renderers);
    }
}
