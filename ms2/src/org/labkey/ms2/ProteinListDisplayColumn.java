package org.labkey.ms2;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.PageFlowUtil;

import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.sql.SQLException;
import java.text.DecimalFormat;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class ProteinListDisplayColumn extends SimpleDisplayColumn
{
    private final List<String> _sequenceColumns;
    private final ProteinGroupProteins _proteins;

    private static final DecimalFormat MASS_FORMAT = new DecimalFormat("0.0000");

    public ProteinListDisplayColumn(List<String> sequenceColumns, ProteinGroupProteins proteins)
    {
        _sequenceColumns = sequenceColumns;
        _proteins = proteins;
        setCaption("Indistinguishable Proteins");
        setWidth("450");
        setNoWrap(true);
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        Integer id = (Integer)row.get("RowId");
        if (id == null)
        {
            id = (Integer)row.get("ProteinGroupId");
        }
        try
        {
            List<ProteinSummary> summaryList = _proteins.getSummaries(id.intValue());
            StringBuilder sb = new StringBuilder();
            sb.append(summaryList.get(0).getName());
            for (int i = 1; i < summaryList.size(); i++)
            {
                sb.append(", ");
                sb.append(summaryList.get(i).getName());
            }
            return sb.toString();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Map row = ctx.getRow();
        try
        {
            List<ProteinSummary> summaryList = _proteins.getSummaries(((Integer)row.get("ProteinGroupId")).intValue());

            ViewURLHelper url = ctx.getViewContext().cloneViewURLHelper();
            url.setAction("showProtein.view");

            if (summaryList != null)
            {
                for (ProteinSummary summary : summaryList)
                {
                    out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
                    writeInfo(summary, out, url);
                    out.write("</table>");
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void writeInfo(ProteinSummary summary, Writer out, ViewURLHelper url) throws IOException
    {
        out.write("<tr>");
        for (String column : _sequenceColumns)
        {
            if (column.equalsIgnoreCase("Protein"))
            {
                out.write("<td nowrap title=\"Protein\">");
                url.replaceParameter("seqId", Integer.toString(summary.getSeqId()));
                out.write("<a href=\"");
                out.write(url.toString());
                out.write("\" target=\"prot\">");
                out.write(PageFlowUtil.filter(summary.getName()));
                out.write("</a>");
            }
            else if (column.equalsIgnoreCase("Description"))
            {
                out.write("<td nowrap title=\"Description\">");
                out.write(PageFlowUtil.filter(summary.getDescription()));
            }
            else if (column.equalsIgnoreCase("BestName"))
            {
                out.write("<td nowrap title=\"Best Name\">");
                out.write(PageFlowUtil.filter(summary.getBestName()));
            }
            else if (column.equalsIgnoreCase("BestGeneName"))
            {
                out.write("<td nowrap title=\"Best Gene Name\">");
                String geneName = summary.getBestGeneName();
                if (geneName != null)
                {
                    out.write(PageFlowUtil.filter(geneName));
                }
                else
                {
                    out.write(PageFlowUtil.filter("[No gene name available]"));
                }
            }
            else if (column.equalsIgnoreCase("SequenceMass"))
            {
                out.write("<td nowrap title=\"Sequence Mass\">");
                out.write(PageFlowUtil.filter(MASS_FORMAT.format(summary.getSequenceMass())));
            }
            out.write("</td>");
        }
        out.write("</tr>");
    }

}
