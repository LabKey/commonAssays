package org.labkey.ms2.compare;

import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.util.Pair;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;

import java.util.List;
import java.util.ArrayList;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:56:03 AM
 */
public class PeptideCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare Peptides";

    public PeptideCompareQuery(ViewURLHelper currentUrl, List<MS2Run> runs)
    {
        super(currentUrl, "Peptide", runs);
        
        StringBuilder header = new StringBuilder(HEADER_PREFIX);
        header.append("number of times each peptide appears in each run.");
        setHeader(header.toString());
        addGridColumn("Total", "Peptide");
    }

    public String getComparisonDescription()
    {
        return COMPARISON_DESCRIPTION;
    }

    protected void addWhereClauses(SimpleFilter filter)
    {
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);
        filter.addAllClauses(peptideFilter);
    }

    protected String setupComparisonColumnLink(ViewURLHelper linkURL, String columnName, String runPrefix)
    {
        linkURL.setAction("showRun");
        linkURL.deleteParameter("view");  // Always link to Peptide view (the default)
        return MS2Manager.getDataRegionNamePeptides() + ".Peptide~eq=${Peptide}";
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        return ti.getColumn("Peptide");
    }

    public List<Pair<String, String>> getSQLSummaries()
        
    {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
        String filterString = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER).getFilterText();
        result.add(new Pair<String, String>("Peptide Filter", filterString));
        return result;
    }
}
