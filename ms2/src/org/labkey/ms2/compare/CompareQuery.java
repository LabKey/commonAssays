/*
 * Copyright (c) 2007-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2.compare;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.protein.ProteinManager;
import org.springframework.validation.BindException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:07:32 AM
 */
public class CompareQuery extends SQLFragment
{
    protected ActionURL _currentUrl;
    protected final String _compareColumn = "SeqId";
    protected List<RunColumn> _gridColumns = new ArrayList<>();
    protected int _columnsPerRun;
    protected List<MS2Run> _runs;
    private int _indent = 0;
    private String _header;
    protected final User _user;

    private final Set<FieldKey> _columns = new HashSet<>();
    protected static final String HEADER_PREFIX = "Numbers below represent ";

    public CompareQuery(ActionURL currentUrl, List<MS2Run> runs, User user)
    {
        _currentUrl = currentUrl;
        _runs = runs;
        _user = user;

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
        if ("1".equals(currentUrl.getParameter("sumLightArea-Protein")))
        {
            addGridColumn("SumLightArea", "lightarea", "SUM");
        }
        if ("1".equals(currentUrl.getParameter("sumHeavyArea-Protein")))
        {
            addGridColumn("SumHeavyArea", "heavyarea", "SUM");
        }
        if ("1".equals(currentUrl.getParameter("avgDecimalRatio-Protein")))
        {
            addGridColumn("AvgDecimalRatio", "DecimalRatio", "AVG");
        }
        if ("1".equals(currentUrl.getParameter("maxDecimalRatio-Protein")))
        {
            addGridColumn("MaxDecimalRatio", "DecimalRatio", "MAX");
        }
        if ("1".equals(currentUrl.getParameter("minDecimalRatio-Protein")))
        {
            addGridColumn("MinDecimalRatio", "DecimalRatio", "MIN");
        }
        header.append("mapping to each protein in each run.");

        setHeader(header.toString());
    }

    public String getComparisonDescription()
    {
        return "Compare Search Engine Proteins";
    }

    protected void addGridColumn(String label, String name)
    {
        _gridColumns.add(new RunColumn(label, name, "COUNT"));
    }

    protected void addGridColumn(String label, String name, String aggregate)
    {
        _gridColumns.add(new RunColumn(label, name, aggregate, "0.##"));
    }

    public ResultSet createResultSet(boolean export, int maxRows)
    {
        return new SqlSelector(MS2Manager.getSchema(), this).setMaxRows(maxRows).getResultSet(!export);
    }

    protected String getLabelColumn()
    {
        return "BestName";
    }

    protected void generateSql(BindException errors)
    {
        selectColumns();
        selectRows(errors);
        groupByCompareColumn();
        sort();
    }

    protected void addColumn(String name)
    {
        _columns.add(FieldKey.fromParts(name));
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

        // SELECT SeqId, Max(Run0Total) AS Run0Total, MAX(Run0Unique) AS Run0Unique..., COUNT(Run) As RunCount,
        append("SELECT ");
        addColumn(_compareColumn);
        append(_compareColumn);
        append(",");
        indent();
        appendNewLine();

        for (int i = 0; i < _runs.size(); i++)
        {
            for (RunColumn column : _gridColumns)
            {
                append("MAX(Run");
                append(i);
                append(column.getLabel());
                append(") AS ");
                String colName = "Run" + i + column.getLabel();
                append(colName);
                addColumn(colName);
                append(", ");
            }
            appendNewLine();
        }
        append("COUNT(Run) AS RunCount,");
        addColumn("RunCount");
        appendNewLine();

        String firstColumnName = _gridColumns.get(0).getLabel();

        // Limit "pattern" to first 63 bits otherwise we'll overflow a BIGINT
        int patternRunCount = Math.min(_runs.size(), 63);

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
        addColumn("Pattern");
    }

    protected void selectRows(BindException errors)
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

        for (int i = 0; i < _runs.size(); i++)
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
        filter.addInClause(FieldKey.fromParts("Run"), MS2Manager.getRunIds(_runs));

        addWhereClauses(filter);

        String firstType = _runs.get(0).getType();
        boolean sameType = true;
        for (MS2Run run : _runs)
        {
            sameType = sameType && firstType.equals(run.getType());
        }
        if (!sameType)
        {
            Set<FieldKey> engineScores = new HashSet<>();
            for (MS2RunType type : MS2RunType.values())
            {
                engineScores.addAll(type.getScoreColumnList());
            }
            Set<FieldKey> illegalColumns = new HashSet<>();
            for (SimpleFilter.FilterClause clause : filter.getClauses())
            {
                for (FieldKey colName : clause.getFieldKeys())
                {
                    if (engineScores.contains(colName))
                    {
                        illegalColumns.add(colName);
                    }
                }
            }
            if (!illegalColumns.isEmpty())
            {
                errors.addError(new LabKeyError("If you are comparing runs of different types, you cannot filter on search-engine specific scores: " + illegalColumns));
            }
        }

        append(filter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
        addAll(filter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        
        appendNewLine();
        append("GROUP BY Run, ");
        append(_compareColumn);

        SimpleFilter proteinFilter = new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins());
        // Add to GROUP BY
        for (FieldKey fieldKey : proteinFilter.getAllFieldKeys())
        {
            if (!fieldKey.equals(FieldKey.fromParts("Peptides")) && !fieldKey.equals(FieldKey.fromParts("UniquePeptides")))
            {
                append(", ");
                append(fieldKey.toString());
            }
        }

        appendNewLine();

        String proteinHaving = proteinFilter.getWhereSQL(MS2Manager.getTableInfoProteins()).replaceFirst("WHERE", "HAVING");
        // Can't use SELECT aliases in HAVING clause, so replace names with aggregate functions
        proteinHaving = proteinHaving.replaceAll("UniquePeptides", "COUNT(DISTINCT Peptide)");
        proteinHaving = proteinHaving.replaceAll("Peptides", "COUNT(Peptide)");
        addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteins()));
        append(proteinHaving);

    }

    protected String getFromClause()
    {
        return MS2Manager.getTableInfoPeptides() + " p LEFT OUTER JOIN " +
                "(SELECT Mass AS SequenceMass, BestName, BestGeneName, SeqId AS SeqSeqId FROM " + ProteinManager.getTableInfoSequences() + ") s ON " +
                "p.SeqId = s.SeqSeqId ";
    }

    protected void addWhereClauses(SimpleFilter filter)
    {
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, _user);
        peptideFilter = ProteinManager.reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        filter.addAllClauses(peptideFilter);
    }

    protected void groupByCompareColumn()
    {
        outdent();
        appendNewLine();
        append(") X");
        appendNewLine();

        // GROUP BY Peptide/SeqId
        append("GROUP BY ");
        append(_compareColumn);

        outdent();
        appendNewLine();
        append(") grouped INNER JOIN ");
        append(ProteinManager.getTableInfoSequences(), "seq");
        append(" ON grouped.SeqId = seq.SeqId");
    }

    protected void sort()
    {
        appendNewLine();
        // ORDER BY RunCount DESC, Pattern DESC, Protein ASC (plus apply any URL sort)
        Sort sort = new Sort("-RunCount,-Pattern," + getLabelColumn());
        sort.addURLSort(_currentUrl, MS2Manager.getDataRegionNameCompare());

        // Filter out bogus URL sort columns
        int index = 0;
        while (sort.getSortList().size() > index)
        {
            if (!_columns.contains(sort.getSortList().get(index).getFieldKey()))
            {
                sort.deleteSortColumn(index);
            }
            else
            {
                index++;
            }
        }

        // TODO: If there are more than three columns in the sort list, then it may be that "BestName" and "Protein"
        // are in the list, in which case SQL server will fail to execute the query.  Therefore, we restrict the number
        // of columns you can sort on to 3.
        while (sort.getSortList().size() > 3)
        {
            sort.deleteSortColumn(sort.getSortList().size() - 1);
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
    protected String setupComparisonColumnLink(ActionURL linkURL)
    {
        linkURL.setAction(MS2Controller.ShowProteinAction.class);   // Could target the "prot" window instead of using the main window
        return "protein=${Protein}&seqId=${SeqId}";
    }

    public int getColumnsPerRun()
    {
        return _columnsPerRun;
    }

    // CONSIDER: Split into getCompareGrid (for Excel export) and getCompareGridForDisplay?
    public CompareDataRegion getCompareGrid(boolean export)
    {
        // Limit Excel export and leave a some spare rows for the headers
        int maxRows = export ? ExcelWriter.ExcelDocumentType.xlsx.getMaxRows() - 10 : 1000;
        CompareDataRegion rgn = new CompareDataRegion(createResultSet(export, maxRows));
        rgn.setMaxRows(maxRows);
        TableInfo ti = MS2Manager.getTableInfoCompare();
        ColumnInfo comparisonColumn = getComparisonCommonColumn(ti);
        rgn.addColumn(comparisonColumn);
        rgn.getDisplayColumn(comparisonColumn.getName()).setLinkTarget("prot");

        ActionURL originalLinkURL = _currentUrl.clone();
        originalLinkURL.deleteFilterParameters(DataRegion.SELECT_CHECKBOX_NAME);
        originalLinkURL.deleteParameter("column");
        originalLinkURL.deleteParameter("total");
        originalLinkURL.deleteParameter("unique");

        for (Pair<String, String> param : originalLinkURL.getParameters())
            if (param.getKey().startsWith(MS2Manager.getDataRegionNameCompare()))
                originalLinkURL.deleteParameter(param.getKey());

        try
        {
            ResultSetMetaData md = rgn.getResultSet().getMetaData();
            for (int i = 0; i < _runs.size(); i++)
            {
                ActionURL linkURL = originalLinkURL.clone();
                linkURL.setContainer(_runs.get(i).getContainer());
                linkURL.replaceParameter("run", _runs.get(i).getRun());

                _columnsPerRun = 0;
                for (RunColumn column : _gridColumns)
                {
                    String runPrefix = "Run" + i;
                    String columnName = runPrefix + column.getLabel();

                    DisplayColumn displayColumn = createColumn(linkURL, column, columnName, ti, md, rgn);
                    if (column.getFormatString() != null)
                    {
                        displayColumn.setFormatString(column.getFormatString());
                    }
                    if (displayColumn != null)
                    {
                        _columnsPerRun++;
                        rgn.addDisplayColumn(displayColumn);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        rgn.addColumns(ti.getColumns("RunCount, Pattern"));
        rgn.setShowFilters(false);

        ButtonBar bb = new ButtonBar();

        ActionURL excelUrl = _currentUrl.clone();
        excelUrl.setAction(MS2Controller.ExportCompareToExcel.class);
        ActionButton exportAll = new ActionButton(excelUrl, "Export to Excel");
        exportAll.setActionType(ActionButton.Action.LINK);
        bb.add(exportAll);

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }

    protected DisplayColumn createColumn(ActionURL linkURL, RunColumn column, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        String columnFilter = setupComparisonColumnLink(linkURL);
        var ci = new BaseColumnInfo(columnName);
        ci.setParentTable(ti);
        ci.setSqlTypeName(md.getColumnTypeName(rgn.getResultSet().findColumn(columnName)));
        ci.setLabel(column.getLabel());
        DataColumn dc = new DataColumn(ci);
        dc.setURL(linkURL.getLocalURIString() + "&" + columnFilter);
        return dc;
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        return ti.getColumn("Protein");
    }

    public List<Pair<String, String>> getSQLSummaries()
    {
        List<Pair<String, String>> result = new ArrayList<>();
        SimpleFilter peptideFilter = new SimpleFilter();
        addWhereClauses(peptideFilter);
        result.add(new Pair<>("Peptide Filter", peptideFilter.getFilterText()));
        result.add(new Pair<>("Protein Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText()));
        return result;
    }

    public void checkForErrors(BindException errors)
    {
        if (_runs.isEmpty())
        {
            errors.addError(new LabKeyError("You must select at least one run."));
            return;
        }
        if (getGridColumns().size() == 0)
        {
            errors.addError(new LabKeyError("You must choose at least one comparison column to display in the grid."));
            return;
        }
        generateSql(errors);
    }
}
