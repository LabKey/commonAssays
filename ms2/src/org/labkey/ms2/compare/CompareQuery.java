package org.labkey.ms2.compare;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.util.Pair;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:07:32 AM
 */
public abstract class CompareQuery extends SQLFragment
{
    protected ViewURLHelper _currentUrl;
    protected String _compareColumn;
    protected List<RunColumn> _gridColumns = new ArrayList<RunColumn>();
    protected int _columnsPerRun;
    protected List<MS2Run> _runs;
    protected int _runCount;
    private int _indent = 0;
    private String _header;
    protected static final String HEADER_PREFIX = "Numbers below represent ";

    public static CompareQuery getCompareQuery(String compareColumn /* TODO: Get this from url? */, ViewURLHelper currentUrl, List<MS2Run> runs)
    {
        if ("Peptide".equals(compareColumn))
            return new PeptideCompareQuery(currentUrl, runs);
        else if ("Protein".equals(compareColumn))
            return new ProteinCompareQuery(currentUrl, runs);
        else if ("ProteinProphet".equals(compareColumn))
            return new ProteinProphetCompareQuery(currentUrl, runs);
        else
            return null;
    }

    protected CompareQuery(ViewURLHelper currentUrl, String compareColumn, List<MS2Run> runs)
    {
        _currentUrl = currentUrl;
        _compareColumn = compareColumn;
        _runs = runs;
        _runCount = _runs.size();
    }

    public abstract String getComparisonDescription();

    protected void addGridColumn(String label, String name)
    {
        addGridColumn(label, name, "COUNT");
    }

    protected void addGridColumn(String label, String name, String aggregate)
    {
        _gridColumns.add(new RunColumn(label, name, aggregate));
    }


    public ResultSet createResultSet() throws SQLException
    {
        return Table.executeQuery(MS2Manager.getSchema(), getSQL(), getParams().toArray());
    }

    protected String getLabelColumn()
    {
        return _compareColumn;
    }

    protected void generateSql(List<String> errors)
    {
        selectColumns(errors);
        selectRows(errors);
        groupByCompareColumn(errors);
        sort(errors);
    }

    protected void selectColumns(List<String> errors)
    {
        // SELECT SeqId, Max(Run0Total) AS Run0Total, MAX(Run0Unique) AS Run0Unique..., COUNT(Run) As RunCount,
        append("SELECT ");
        append(_compareColumn);
        append(",");
        indent();
        appendNewLine();

        for (int i = 0; i < _runCount; i++)
        {
            for (RunColumn column : _gridColumns)
            {
                append("MAX(Run");
                append(i);
                append(column.getLabel());
                append(") AS Run");
                append(i);
                append(column.getLabel());
                append(", ");
            }
            appendNewLine();
        }
        append("COUNT(Run) AS RunCount,");
        appendNewLine();

        String firstColumnName = _gridColumns.get(0).getLabel();

        // Limit "pattern" to first 63 bits otherwise we'll overflow a BIGINT
        int patternRunCount = Math.min(_runCount, 63);

        // (CASE WHEN MAX(Run0Total) IS NULL THEN 0 ELSE 8) + (CASE WHEN MAX(Run1Total) IS NULL THEN 4 ELSE 0) + ... AS Pattern
        for (int i = 0; i < patternRunCount; i++)
        {
            if (i > 0)
                append("+");
            append("(CASE WHEN MAX(Run");
            append(i);
            append(firstColumnName);
            append(") IS NULL THEN 0 ELSE ");
            append(Math.round(Math.pow(2, patternRunCount - i - 1)));
            append(" END)");
        }
        append(" AS Pattern");
    }

    protected void selectRows(List<String> errors)
    {
        // FROM (SELECT Run, SeqId, CASE WHEN Run=1 THEN COUNT(DISTINCT Peptide) ELSE NULL END AS Run0, ... FROM MS2Peptides WHERE (peptideFilter)
        //       AND Run IN (?, ?, ...) GROUP BY Run, SeqId
        outdent();
        appendNewLine();
        append("FROM");
        appendNewLine();
        append("(");
        indent();
        appendNewLine();
        append("SELECT Run, ");
        append(_compareColumn);
        indent();

        for (int i = 0; i < _runCount; i++)
        {
            String separator = "," + getNewLine();

            for (RunColumn column : _gridColumns)
            {
                append(separator);
                append("CASE WHEN Run=?");
                add(_runs.get(i).getRun());
                append(" THEN ");
                append(column.getAggregate());
                append("(");
                append(column.getName());
                append(") ELSE NULL END AS Run");
                append(i);
                append(column.getLabel());
                separator = ", ";
            }
        }
        outdent();
        appendNewLine();
        append("FROM ");
        append(getFromClause());
        appendNewLine();
        SimpleFilter filter = new SimpleFilter();
        filter.addInClause("Run", MS2Manager.getRunIds(_runs));

        addWhereClauses(filter);

        String firstType = _runs.get(0).getType();
        boolean sameType = true;
        for (MS2Run run : _runs)
        {
            sameType = sameType && firstType.equals(run.getType());
        }
        if (!sameType)
        {
            Set<String> engineScores = new CaseInsensitiveHashSet();
            for (MS2RunType type : MS2RunType.values())
            {
                engineScores.addAll(type.getScoreColumnList());
            }
            Set<String> illegalColumns = new CaseInsensitiveHashSet();
            for (SimpleFilter.FilterClause clause : filter.getClauses())
            {
                for (String colName : clause.getColumnNames())
                {
                    if (engineScores.contains(colName))
                    {
                        illegalColumns.add(colName);
                    }
                }
            }
            if (!illegalColumns.isEmpty())
            {
                errors.add("If you are comparing runs of different types, you cannot filter on search-engine specific scores: " + illegalColumns);
            }
        }

        append(filter.getWhereSQL(MS2Manager.getSqlDialect()));
        addAll(filter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        
        appendNewLine();
        append("GROUP BY Run, ");
        append(_compareColumn);
    }

    protected String getFromClause()
    {
        return MS2Manager.getTableInfoPeptides().toString() + " p";        
    }

    protected abstract void addWhereClauses(SimpleFilter filter);

    protected void groupByCompareColumn(List<String> errors)
    {
        outdent();
        appendNewLine();
        append(") X");
        appendNewLine();

        // GROUP BY Peptide/SeqId
        append("GROUP BY ");
        append(_compareColumn);
    }

    protected void sort(List<String> errors)
    {
        appendNewLine();
        // ORDER BY RunCount DESC, Pattern DESC, Protein ASC (plus apply any URL sort)
        Sort sort = new Sort("-RunCount,-Pattern," + getLabelColumn());
        sort.applyURLSort(_currentUrl, MS2Manager.getDataRegionNameCompare());
        for (Sort.SortField sortField : sort.getSortList())
        {
            sortField.getColumnName();
        }
        // TODO: If there are more than three columns in the sort list, then it may be that "BestName" and "Protein"
        // are in the list, in which case SQL server will fail to execute the query.  Therefore, we restrict the number
        // of columns you can sort on to 3.
        while (sort.getSortList().size() > 3)
        {
            sort.getSortList().remove(sort.getSortList().size() - 1);
        }
        append(sort.getOrderByClause(MS2Manager.getSqlDialect()));
    }

    protected void appendNewLine()
    {
        append(getNewLine());
    }

    protected String getNewLine()
    {
        assert _indent >= 0;
        return "\n" + StringUtils.repeat("\t", _indent);
    }

    protected void indent()
    {
        _indent++;
    }

    protected void outdent()
    {
        _indent--;
    }

    protected void setHeader(String header)
    {
        _header = header;
    }

    public String getHeader()
    {
        return _header;
    }

    public List<RunColumn> getGridColumns()
    {
        return _gridColumns;
    }

    /** @return link filter */
    protected abstract String setupComparisonColumnLink(ViewURLHelper linkURL, String columnName, String runPrefix);

    public int getColumnsPerRun()
    {
        return _columnsPerRun;
    }

    // CONSIDER: Split into getCompareGrid (for Excel export) and getCompareGridForDisplay?
    public CompareDataRegion getCompareGrid() throws SQLException
    {
        CompareDataRegion rgn = new CompareDataRegion(createResultSet());
        TableInfo ti = MS2Manager.getTableInfoCompare();
        rgn.addColumn(getComparisonCommonColumn(ti));

        ViewURLHelper originalLinkURL = this._currentUrl.clone();
        originalLinkURL.deleteFilterParameters(".select");
        originalLinkURL.deleteParameter("column");
        originalLinkURL.deleteParameter("total");
        originalLinkURL.deleteParameter("unique");

        Pair<String, String>[] params = originalLinkURL.getParameters();

        for (Pair<String, String> param : params)
            if (param.getKey().startsWith(MS2Manager.getDataRegionNameCompare()))
                originalLinkURL.deleteParameter(param.getKey());

        ResultSetMetaData md = rgn.getResultSet().getMetaData();

        for (int i = 0; i < _runs.size(); i++)
        {
            ViewURLHelper linkURL = originalLinkURL.clone();
            linkURL.setExtraPath(ContainerManager.getForId(_runs.get(i).getContainer()).getPath());
            linkURL.replaceParameter("run", String.valueOf(_runs.get(i).getRun()));

            _columnsPerRun = 0;
            for (RunColumn column : _gridColumns)
            {
                String runPrefix = "Run" + i;
                String columnName = runPrefix + column.getLabel();

                DisplayColumn displayColumn = createColumn(linkURL, column, runPrefix, columnName, ti, md, rgn);
                if (displayColumn != null)
                {
                    _columnsPerRun++;
                    rgn.addColumn(displayColumn);
                }
            }
        }

        rgn.addColumns(ti.getColumns("RunCount, Pattern"));
        rgn.setFixedWidthColumns(false);
        rgn.setShowFilters(false);

        ButtonBar bb = new ButtonBar();

        ViewURLHelper excelUrl = _currentUrl.clone();
        ActionButton exportAll = new ActionButton("ExportAll", "Export to Excel");
        excelUrl.setAction("exportCompareToExcel");
        exportAll.setURL(excelUrl.getEncodedLocalURIString());
        exportAll.setActionType(ActionButton.Action.LINK);
        bb.add(exportAll);

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }

    protected DisplayColumn createColumn(ViewURLHelper linkURL, RunColumn column, String runPrefix, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        String columnFilter = setupComparisonColumnLink(linkURL, column.getLabel(), runPrefix);
        ColumnInfo ci = new ColumnInfo(columnName);
        ci.setParentTable(ti);
        ci.setSqlTypeName(md.getColumnTypeName(rgn.getResultSet().findColumn(columnName)));
        ci.setCaption(column.getLabel());
        DataColumn dc = new DataColumn(ci);
        dc.setURL(linkURL.getLocalURIString() + "&" + columnFilter);
        return dc;
    }

    protected abstract ColumnInfo getComparisonCommonColumn(TableInfo ti);

    public abstract List<Pair<String, String>> getSQLSummaries();

    public void checkForErrors(List<String> errors) throws SQLException
    {
        generateSql(errors);
        if (getGridColumns().size() == 0)
            errors.add("You must choose at least one column to display in the grid.");
    }
}
