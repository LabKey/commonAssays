package cpas.ms2.peptideview;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ViewURLHelper;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractProteinDataRegion extends DataRegion
{
    protected boolean _expanded = false;
    protected DataRegion _nestedRegion = null;
    private final String _uniqueColumnName;
    private final String _groupURL;
    protected GroupedResultSet _groupedRS = null;

    protected AbstractProteinDataRegion(String uniqueColumnName, ViewURLHelper urlHelper)
    {
        _uniqueColumnName = uniqueColumnName;
        ViewURLHelper groupURL = urlHelper.clone();
        groupURL.setAction("getProteinGroupingPeptides.view");
        groupURL.deleteParameter("proteinGroupingId");

        _groupURL = groupURL.toString() + "&proteinGroupingId=";
    }

    public void _renderTable(RenderContext ctx, Writer out) throws SQLException, IOException
    {
        super._renderTable(ctx, out);
        if (_groupedRS != null)
        {
            _groupedRS.close();
        }
    }

    protected void renderExtraRecordSelectorContent(RenderContext ctx, Writer out) throws IOException
    {
        String value = getUniqueColumnValue(ctx);
        out.write("<a href=\"javascript:togglePeptides('");
        out.write(_groupURL);
        out.write(value);
        out.write("', '");
        out.write(value);
        out.write("');\"><img valign=\"middle\" id=\"proteinHandle");
        out.write(value);
        out.write("\" src=\"");
        out.write(ctx.getViewContext().getContextPath());
        out.write("/_images/");
        out.write(_expanded ? "minus" : "plus");
        out.write(".gif\"/></a>");
    }

    private String getUniqueColumnValue(RenderContext ctx)
    {
        Object value = ctx.getRow().get(_uniqueColumnName);
        return value == null ? "null" : value.toString();
    }

    public void setGroupedResultSet(GroupedResultSet groupedRS) throws SQLException
    {
        _groupedRS = groupedRS;
    }

    public GroupedResultSet getNestedResultSet()
    {
        return _groupedRS;
    }


    public void setExpanded(boolean expanded)
    {
        _expanded = expanded;
    }


    public boolean getExpanded()
    {
        return _expanded;
    }

    public void setNestedRegion(DataRegion nestedRegion)
    {
        _nestedRegion = nestedRegion;
    }

    protected void renderNestedGrid(Writer out, RenderContext ctx, ResultSet nestedRS, int rowIndex)
            throws IOException
    {
        renderRowStart(rowIndex, out, true, ctx);

        RenderContext nestedCtx = new RenderContext(ctx.getViewContext());
        nestedCtx.setResultSet(nestedRS);
        nestedCtx.setMode(DataRegion.MODE_GRID);
        _nestedRegion.render(nestedCtx, out);
        renderRowEnd(out);
    }

    private void renderRowEnd(Writer out)
            throws IOException
    {
        out.write("</td></tr>\n");
    }

    private void renderRowStart(int rowIndex, Writer out, boolean visible, RenderContext ctx)
            throws IOException
    {
        out.write("<tr");
        if (isShadeAlternatingRows() && rowIndex % 2 == 0)
        {
            out.write(" bgcolor=\"#EEEEEE\"");
        }
        if (!visible)
        {
            out.write(" style=\"display:none\"");
        }
        out.write(" id=\"proteinRow");
        String value = getUniqueColumnValue(ctx);
        out.write(value);
        out.write("\"><td></td><td colspan=\"20\" align=\"left\" id=\"proteinContent");
        out.write(value);
        out.write("\">");
    }

    protected void renderPlaceholderGrid(Writer out, RenderContext ctx, int rowIndex) throws IOException
    {
        renderRowStart(rowIndex, out, false, ctx);
        renderRowEnd(out);
    }
}
