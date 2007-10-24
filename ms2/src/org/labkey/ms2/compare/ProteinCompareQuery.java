package org.labkey.ms2.compare;

import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.util.Pair;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.ms2.protein.ProteinManager;

import java.util.List;
import java.util.ArrayList;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:56:03 AM
 */
public class ProteinCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare Search Engine Proteins";

    public ProteinCompareQuery(ViewURLHelper currentUrl, List<MS2Run> runs)
    {
        super(currentUrl, "SeqId", runs);

        boolean total = "1".equals(currentUrl.getParameter("total"));
        boolean unique = "1".equals(currentUrl.getParameter("unique"));

        StringBuilder header = new StringBuilder(HEADER_PREFIX);
        if (total)
        {
            addGridColumn("Total", "Peptide");
            header.append("total peptides ");
        }
        if (unique)
        {
            addGridColumn("Unique", "DISTINCT Peptide");
            if (total)
                header.append("and ");
            header.append("unique peptides ");
        }
        header.append("mapping to each protein in each run.");

        setHeader(header.toString());
    }

    public String getComparisonDescription()
    {
        return COMPARISON_DESCRIPTION;
    }

    protected String getLabelColumn()
    {
        return "BestName";
    }

    protected void selectColumns(List<String> errors)
    {
        // Use subselect to make it easier to join seqid to prot.sequences for bestname
        append("SELECT ");
        append(getLabelColumn());
        append(" AS Protein, grouped.* FROM");
        appendNewLine();
        append("(");
        indent();
        appendNewLine();

        super.selectColumns(errors);
    }

    protected String getFromClause()
    {
        return MS2Manager.getTableInfoPeptides() + " p LEFT OUTER JOIN " +
            "(SELECT Mass AS SequenceMass, BestName, BestGeneName, SeqId AS SeqSeqId FROM " + ProteinManager.getTableInfoSequences() + ") s ON " +
            "p.SeqId = s.SeqSeqId ";
    }

    protected void selectRows(List<String> errors)
    {
        super.selectRows(errors);

        SimpleFilter proteinFilter = new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins());
        // Add to GROUP BY
        for (String columnName : proteinFilter.getAllColumnNames())
        {
            if (!columnName.equalsIgnoreCase("Peptides") && !columnName.equalsIgnoreCase("UniquePeptides"))
            {
                append(", ");
                append(columnName);
            }
        }

        appendNewLine();


        // TODO: Make Nick happy by using a sub-SELECT instead of HAVING
        String proteinHaving = proteinFilter.getWhereSQL(MS2Manager.getSqlDialect()).replaceFirst("WHERE", "HAVING");
        // Can't use SELECT aliases in HAVING clause, so replace names with aggregate functions
        proteinHaving = proteinHaving.replaceAll("UniquePeptides", "COUNT(DISTINCT Peptide)");
        proteinHaving = proteinHaving.replaceAll("Peptides", "COUNT(Peptide)");
        addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteins()));
        append(proteinHaving);
    }

    protected void addWhereClauses(SimpleFilter filter)
    {
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);
        filter.addAllClauses(peptideFilter);
    }

    protected void groupByCompareColumn(List<String> errors)
    {
        super.groupByCompareColumn(errors);

        outdent();
        appendNewLine();
        append(") grouped INNER JOIN ");
        append(ProteinManager.getTableInfoSequences());
        append(" seq ON grouped.SeqId = seq.SeqId");
    }

    protected String setupComparisonColumnLink(ViewURLHelper linkURL, String columnName, String runPrefix)
    {
        linkURL.setAction("showProtein");   // Could target the "prot" window instead of using the main window
        return "protein=${Protein}&seqId=${SeqId}";
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        return ti.getColumn("Protein");
    }

    public List<Pair<String, String>> getSQLSummaries()
    {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
        String filterString = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER).getFilterText();
        result.add(new Pair<String, String>("Peptide Filter", filterString));
        result.add(new Pair<String, String>("Protein Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText()));
        return result;
    }
}
