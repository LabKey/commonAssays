package cpas.ms2.compare;

import MS2.GroupNumberDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.ms2.MS2Run;
import org.labkey.api.protein.ProteinManager;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.view.ViewURLHelper;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Oct 6, 2006
 */
public class ProteinProphetCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare ProteinProphet Proteins";

    protected ProteinProphetCompareQuery(ViewURLHelper currentUrl, List<MS2Run> runs)
    {
        super(currentUrl, "SeqId", runs);

        boolean groupProbability = "1".equals(currentUrl.getParameter("groupProbability"));
        boolean light2HeavyRatioMean = "1".equals(currentUrl.getParameter("light2HeavyRatioMean"));
        boolean heavy2LightRatioMean = "1".equals(currentUrl.getParameter("heavy2LightRatioMean"));
        boolean totalPeptides = "1".equals(currentUrl.getParameter("totalPeptides"));
        boolean uniquePeptides = "1".equals(currentUrl.getParameter("uniquePeptides"));

        StringBuilder header = new StringBuilder(HEADER_PREFIX);

        List<String> descriptions = new ArrayList<String>();

        addGridColumn("GroupNumber", "GroupNumber", "MAX");
        addGridColumn("CollectionId", "IndistinguishableCollectionId", "MAX");
        descriptions.add("protein group number");

        if (groupProbability)
        {
            addGridColumn("GroupProbability", "GroupProbability", "MAX");
            descriptions.add("protein group probability");
        }
        if (heavy2LightRatioMean)
        {
            addGridColumn("Heavy2LightRatioMean", "Heavy2LightRatioMean", "MAX");
            descriptions.add("heavy to light ratio mean");
        }
        if (light2HeavyRatioMean)
        {
            addGridColumn("ratiomean", "ratiomean", "MAX");
            descriptions.add("light to heavy ratio mean");
        }
        if (totalPeptides)
        {
            addGridColumn("TotalNumberPeptides", "TotalNumberPeptides", "MAX");
            descriptions.add("total peptides");
        }
        if (uniquePeptides)
        {
            addGridColumn("UniquePeptidesCount", "UniquePeptidesCount", "MAX");
            descriptions.add("unique peptides");
        }

        for (int i = 0; i < descriptions.size(); i++)
        {
            if (i > 0)
            {
                header.append(", ");
            }
            if (i == descriptions.size() - 1)
            {
                header.append(" and ");
            }
            header.append(descriptions.get(i));
        }
        header.append(" mapping to each protein in each run.");

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
        linkURL.setAction("showRun");
        linkURL.replaceParameter("expanded", "1");
        linkURL.replaceParameter("grouping", "proteinprophet");
        String paramName = MS2Manager.getDataRegionNameProteinGroups() + ".GroupNumber";
        linkURL.deleteParameter(paramName);
        return paramName + "~eq=${" + runPrefix + "GroupNumber}";
    }

    protected DisplayColumn createColumn(ViewURLHelper linkURL, RunColumn column, String runPrefix, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        if (column.getLabel().equals("CollectionId"))
        {
            return null;
        }
        else if (column.getLabel().equals("GroupNumber"))
        {
            ColumnInfo ci = new ColumnInfo(columnName);
            ci.setParentTable(ti);
            ci.setSqlTypeName(md.getColumnTypeName(rgn.getResultSet().findColumn(columnName)));
            ci.setCaption(column.getLabel());
            return new GroupNumberDisplayColumn(ci, linkURL, runPrefix + "GroupNumber", runPrefix + "CollectionId");
        }
        else
        {
            DisplayColumn result = super.createColumn(linkURL, column, runPrefix, columnName, ti, md, rgn);
            result.setURL(null);
            return result;
        }


    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        ColumnInfo result = ti.getColumn("Protein");
        ViewURLHelper linkURL = _currentUrl.clone();
        linkURL.setAction("showProtein");
        linkURL.deleteParameter("seqId");
        result.setURL(linkURL.getLocalURIString() + "&seqId=${SeqId}");
        return result;
    }

    public List<Pair<String, String>> getSQLSummaries()
    {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
        result.add(new Pair<String, String>("Protein Group Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteinGroups()).getFilterText(MS2Manager.getSqlDialect())));
        return result;
    }

    protected String getFromClause()
    {
        SimpleFilter proteinGroupFilter = new SimpleFilter();
        proteinGroupFilter.addUrlFilters(_currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        addAll(proteinGroupFilter.getWhereParams(MS2Manager.getTableInfoProteinGroupsWithQuantitation()));

        return MS2Manager.getTableInfoProteinProphetFiles() + " ppf, " +
            " ( SELECT * FROM " + MS2Manager.getTableInfoProteinGroupsWithQuantitation() +
            " " + proteinGroupFilter.getWhereSQL(MS2Manager.getSqlDialect()) + " ) pg, " +
            MS2Manager.getTableInfoProteinGroupMemberships() + " pgm";
    }


    protected void addWhereClauses(SimpleFilter filter)
    {
        filter.addWhereClause("ppf.rowid = pg.proteinprophetfileid", new Object[0]);
        filter.addWhereClause("pg.rowId = pgm.proteingroupid", new Object[0]);
    }
}
