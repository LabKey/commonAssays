package cpas.ms2.compare;

import org.fhcrc.cpas.view.ViewURLHelper;
import org.fhcrc.cpas.util.Pair;
import org.fhcrc.cpas.ms2.MS2Run;
import org.fhcrc.cpas.ms2.MS2Manager;
import org.fhcrc.cpas.data.SimpleFilter;
import org.fhcrc.cpas.data.ColumnInfo;
import org.fhcrc.cpas.data.TableInfo;
import org.fhcrc.cpas.protein.ProteinManager;

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

    protected void selectColumns()
    {
        // Use subselect to make it easier to join seqid to prot.sequences for bestname
        append("SELECT ");
        append(getLabelColumn());
        append(" AS Protein, grouped.* FROM");
        appendNewLine();
        append("(");
        indent();
        appendNewLine();

        super.selectColumns();
    }

    protected void selectRows()
    {
        super.selectRows();
        appendNewLine();

        SimpleFilter proteinFilter = new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins());

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

    protected void groupByCompareColumn()
    {
        super.groupByCompareColumn();

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
        String filterString = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER).getFilterText(ProteinManager.getSqlDialect());
        result.add(new Pair<String, String>("Peptide Filter", filterString));
        result.add(new Pair<String, String>("Protein Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText(MS2Manager.getSqlDialect())));
        return result;
    }
}
