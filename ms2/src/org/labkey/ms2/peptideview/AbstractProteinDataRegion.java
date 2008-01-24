package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.view.ActionURL;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.Iterator;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractProteinDataRegion extends DataRegion
{
    protected boolean _expanded = false;
    protected DataRegion _nestedRegion = null;
    protected final String _uniqueColumnName;
    private final String _groupURL;
    protected GroupedResultSet _groupedRS = null;

    protected AbstractProteinDataRegion(String uniqueColumnName, ActionURL url)
    {
        _uniqueColumnName = uniqueColumnName;
        ActionURL groupURL = url.clone();
        groupURL.setAction("getProteinGroupingPeptides.view");
        groupURL.deleteParameter("proteinGroupingId");

        _groupURL = groupURL.toString() + "&proteinGroupingId=";
    }

    public void _renderTable(RenderContext ctx, Writer out) throws SQLException, IOException
    {
        if (_expanded)
        {
            List<DisplayColumn> displayColumnList = getDisplayColumnList();
            for (Iterator<DisplayColumn> i = displayColumnList.iterator(); i.hasNext(); )
            {
                DisplayColumn col = i.next();
                if (col instanceof EmptyDisplayColumn)
                {
                    i.remove();
                }
            }
            displayColumnList.add(new EmptyDisplayColumn());
        }

        super._renderTable(ctx, out);
        if (_groupedRS != null)
        {
            _groupedRS.close();
        }
    }

    protected void renderExtraRecordSelectorContent(RenderContext ctx, Writer out) throws IOException
    {
        String value = getUniqueColumnValue(ctx);
        out.write("<a href=\"javascript:toggleNestedGrid('");
        out.write(_groupURL);
        out.write(value);
        out.write("', '");
        out.write(value);
        out.write("');\"><img valign=\"middle\" id=\"");
        out.write(getName());
        out.write("-Handle");
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

    public void setNestedRegion(DataRegion nestedRegion)
    {
        _nestedRegion = nestedRegion;
        _nestedRegion.setShowPagination(false);
    }

    protected void renderNestedGrid(Writer out, RenderContext ctx, ResultSet nestedRS, int rowIndex)
            throws IOException
    {
        renderRowStart(rowIndex, out, ctx);

        RenderContext nestedCtx = new RenderContext(ctx.getViewContext());
        nestedCtx.setResultSet(nestedRS);
        nestedCtx.setMode(DataRegion.MODE_GRID);
        if (_expanded)
        {
            _nestedRegion.render(nestedCtx, out);
        }
        else
        {
            try
            {
                while(nestedRS.next());
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        renderRowEnd(out);
    }

    private void renderRowEnd(Writer out)
            throws IOException
    {
        out.write("</td></tr>\n");
    }

    private void renderRowStart(int rowIndex, Writer out, RenderContext ctx)
            throws IOException
    {
        out.write("<tr");
        if (isShadeAlternatingRows() && rowIndex % 2 == 0)
        {
            out.write(" bgcolor=\"#EEEEEE\"");
        }
        if (!_expanded)
        {
            out.write(" style=\"display:none\"");
        }
        out.write(" id=\"");
        out.write(getName());
        out.write("-Row");
        String value = getUniqueColumnValue(ctx);
        out.write(value);
        out.write("\"><td></td><td colspan=\"");
        int colspan = 0;
        for (DisplayColumn dc : getDisplayColumnList())
        {
            if (dc.getVisible(ctx))
            {
                colspan++;
            }
        }
        out.write(Integer.toString(colspan));
        out.write("\" align=\"left\" id=\"");
        out.write(getName());
        out.write("-Content");
        out.write(value);
        out.write("\">");
    }

    private static class EmptyDisplayColumn extends SimpleDisplayColumn
    {
        public EmptyDisplayColumn()
        {
            super("");
            setWidth(null);
        }
    }
}
