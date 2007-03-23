package org.labkey.ms2;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.ms2.peptideview.QueryPeptideDataRegion;

import java.io.Writer;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;
import java.text.DecimalFormat;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class ProteinListDisplayColumn extends SimpleDisplayColumn
{
    private List<String> _sequenceColumns;
    private ProteinGroupProteins _proteins;

    private static final DecimalFormat MASS_FORMAT = new DecimalFormat("0.0000");
    private ColumnInfo _columnInfo;
    private String _columnName = "ProteinGroupId";
    private static final String NO_GENE_NAME_AVAILABLE = "[No gene name available]";

    public static final List<String> ALL_SEQUENCE_COLUMNS = Collections.unmodifiableList(Arrays.asList("Protein", "BestName", "BestGeneName", "SequenceMass", "Description"));
    private static final Map<String, String> ALL_SEQUENCE_COLUMNS_MAP;

    static
    {
        Map<String, String> values = new CaseInsensitiveHashMap<String>();
        for (String s : ALL_SEQUENCE_COLUMNS)
        {
            values.put(s, s);
        }
        ALL_SEQUENCE_COLUMNS_MAP = Collections.unmodifiableMap(values);
    }

    public ProteinListDisplayColumn(ColumnInfo col)
    {
        _columnInfo = col;
        _columnName = col.getAlias();
        _sequenceColumns = ALL_SEQUENCE_COLUMNS;
    }

    public ProteinListDisplayColumn(List<String> sequenceColumns, ProteinGroupProteins proteins)
    {
        _sequenceColumns = sequenceColumns;
        _proteins = proteins;
        setWidth("450");
        setNoWrap(true);
        setCaption("Indistinguishable Proteins");
    }

    public ProteinListDisplayColumn(List<String> sequenceColumns, ProteinGroupProteins proteins, String columnName)
    {
        this(sequenceColumns, proteins);
        setCaption(ALL_SEQUENCE_COLUMNS_MAP.get(columnName));
    }


    public ColumnInfo getColumnInfo()
    {
        return _columnInfo;
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        Integer id = (Integer)row.get("RowId");
        if (id == null)
        {
            id = (Integer)row.get(_columnName);
        }
        try
        {
            List<ProteinSummary> summaryList = getProteins(ctx).getSummaries(id.intValue());
            StringBuilder sb = new StringBuilder();
            String proteinSeparator = "";
            for (ProteinSummary summary : summaryList)
            {
                sb.append(proteinSeparator);
                proteinSeparator = ", ";

                String valueSeparator = "";
                for (String column : _sequenceColumns)
                {
                    sb.append(valueSeparator);
                    valueSeparator = " ";
                    if (column.equalsIgnoreCase("Protein"))
                    {
                        sb.append(summary.getName());
                    }
                    else if (column.equalsIgnoreCase("Description"))
                    {
                        sb.append(summary.getDescription());
                    }
                    else if (column.equalsIgnoreCase("BestName"))
                    {
                        sb.append(summary.getBestName());
                    }
                    else if (column.equalsIgnoreCase("BestGeneName"))
                    {
                        String geneName = summary.getBestGeneName();
                        if (geneName != null)
                        {
                            sb.append(geneName);
                        }
                        else
                        {
                            sb.append(NO_GENE_NAME_AVAILABLE);
                        }
                    }
                    else if (column.equalsIgnoreCase("SequenceMass"))
                    {
                        sb.append(summary.getSequenceMass());
                    }
                }
            }
            return sb.toString();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private ProteinGroupProteins getProteins(RenderContext ctx)
    {
        if (_proteins == null)
        {
            _proteins = ((QueryPeptideDataRegion)ctx.getCurrentRegion()).lookupProteinGroupProteins();
        }
        return _proteins;
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Map row = ctx.getRow();
        try
        {
            List<ProteinSummary> summaryList = getProteins(ctx).getSummaries(((Integer)row.get(_columnName)).intValue());

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


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        set.add(_columnInfo);
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
                    out.write(PageFlowUtil.filter(NO_GENE_NAME_AVAILABLE));
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
