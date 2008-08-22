/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.query;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.controllers.run.RunForm;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.view.FlowQueryView;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * SELECT
 *   A.Name, A.OtherNonStatColumns, ...
 *   A.MatchColumn1, A.MatchColumn2,
 *   A.BackgroundColumn,
 *   A.Stat_1 - BG.Stat_1 AS "Corrected Stat_1",
 *   A.Stat_2 - BG.Stat_2 AS "Corrected Stat_2"
 * FROM
 *   (SELECT
 *      FCSAnalyses.Name, FCSAnalyses.OtherNonStatColumns, ...
 *      FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2,
 *      FCSAnalyses.BackgroundColumn,
 *      FCSAnalyses.Stat_1, FCSAnlyses.Stat_2
 *    FROM FCSAnalyses
 *    WHERE
 *      -- is not a background well
 *    ) AS A
 * INNER JOIN
 *   (SELECT
 *      FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2,
 *      AVG(FCSAnalyses.Stat_1) AS Stat_1,
 *      AVG(FCSAnalyses.Stat_2) AS Stat_2
 *    FROM FCSAnalyses
 *    WHERE
 *      -- is a background well
 *      FCSAnalyses.BackgroundColumn = BackgroundValue,
 *    GROUP BY FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2
 *    ) AS BG
 *  ON A.MatchColumn1 = BG.MatchColumn1 AND A.MatchColumn2 = BG.MatchColumn2
 */
public class SubtractBackgroundQuery extends SQLFragment
{
//    protected RunForm _form;
//    protected FlowRun _run;
    protected ActionURL _currentUrl;
    protected ICSMetadata _metadata;

    protected QueryDefinition _query;
//    protected CustomView _customView;
    protected TableInfo _table;
    protected List<ColumnInfo> _columns;
    protected List<ColumnInfo> _ordinaryCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _matchCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _statCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _backgroundCols = new ArrayList<ColumnInfo>();
    protected SimpleFilter _backgroundFilter = new SimpleFilter();
    private int _indent = 0;

    public SubtractBackgroundQuery(ActionURL currentUrl, FlowQueryView view, ICSMetadata metadata)
    {
        _currentUrl = currentUrl;
        _metadata = metadata;
//            _run = view.getSchema().getRun();
        _query = view.getQueryDef();
        _table = view.getTable();
        _columns = new LinkedList<ColumnInfo>();
        for (DisplayColumn displayCol : view.getDisplayColumns())
        {
            ColumnInfo column = displayCol.getColumnInfo();
            if (column != null)
                _columns.add(column);
        }

        scanColumns();
    }

    public SubtractBackgroundQuery(ActionURL currentUrl, RunForm form, ICSMetadata metadata)
    {
        _currentUrl = currentUrl;
//        _form = form;
//        _run = form.getRun();
        _metadata = metadata;

        _query = form.getQueryDef();
        CustomView customView = form.getCustomView();
        _table = _query.getMainTable();
        _columns = _query.getColumns(customView, _table);

        scanColumns();
    }

    public ResultSet createResultSet(int maxRows) throws SQLException
    {
        genSql();
        return Table.executeQuery(FlowManager.get().getSchema(), getSQL(), getParams().toArray(), maxRows, true);
    }

    // XXX: only keep %P stats?
    protected boolean isStatColumn(ColumnInfo column)
    {
        return column.getName().startsWith("Statistic/") &&
               column.getName().endsWith(":Freq_Of_Parent");
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> cols = new LinkedList<DisplayColumn>();
        for (ColumnInfo column : _columns)
        {
//                if (isStatColumn(column))
//                {
//                    ColumnInfo corrected = new ColumnInfo(column);
//                    corrected.setAlias("Corrected " + column.getName());
//                    DisplayColumn renderer = corrected.getRenderer();
//                    cols.add(renderer);
//                }
//                else
            {
                cols.add(column.getRenderer());
            }
        }
        return cols;
    }

    protected void scanColumns()
    {
        for (ColumnInfo column : _columns)
        {
            FieldKey key = FieldKey.fromString(column.getName());
            ScriptSettings.FilterInfo background = null;
            if (isStatColumn(column))
            {
                _statCols.add(column);
            }
            else if (_metadata.getMatchColumns().contains(key))
            {
                _matchCols.add(column);
            }
            else if (null != (background = _metadata.getBackgroundFilter(key)))
            {
                _backgroundCols.add(column);
                _backgroundFilter.addCondition(column.getValueSql().toString(), background.getValue(), background.getOp());
            }
            else
            {
                _ordinaryCols.add(column);
            }
        }

        if (_statCols.size() == 0)
            throw new IllegalArgumentException("at least one statistic column is required");
        if (_matchCols.size() != _metadata.getMatchColumns().size())
            throw new IllegalArgumentException("column matching background to stimulated wells is required");
        if (_backgroundCols.size() != _metadata.getBackgroundFilter().size())
            throw new IllegalArgumentException("at least one background column is required");
    }

    protected void genSql()
    {
        SqlDialect dialect = _table.getSqlDialect();
        Map<String, ColumnInfo> columnMap = Table.createColumnMap(_table, _columns);

        append("SELECT");
        indent();

        String strComma = "";
        for (ColumnInfo column : _ordinaryCols)
        {
            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
//                column.declareJoins(joins);
            append(strComma);
            appendNewLine();
            append("A.").append(column.getSelectName());
//                column.getSelectName();
//                column.getLegalName();
//                column.getColumnName();
            strComma = ",";
        }
        for (ColumnInfo column : _matchCols)
        {
            append(",");
            appendNewLine();
            append("A.").append(column.getSelectName());
        }
        for (ColumnInfo column: _backgroundCols)
        {
            append(",");
            appendNewLine();
            append("A.").append(column.getSelectName());
        }
        for (ColumnInfo column : _statCols)
        {
            append(",");
            appendNewLine();
            append("A.").append(column.getSelectName()).append(" - ").append("BG.").append(column.getSelectName());
//                append(" AS ").append("\"Corrected ").append(column.getSelectName()).append("\"");
            append(" AS ").append(column.getSelectName());
        }

        appendNewLine();
        append("FROM");
        indent();
        appendNewLine();

//            ActionURL sortFilterUrl = _form.getQuerySettings().getSortFilterURL();
        ActionURL sortFilterUrl = _currentUrl;
        SimpleFilter filter = new SimpleFilter(sortFilterUrl, "query");

        // WHERE not background
        SimpleFilter.FilterClause andClause = new SimpleFilter.AndClause(_backgroundFilter.getClauses().toArray(new SimpleFilter.FilterClause[0]));
        SimpleFilter.FilterClause notBackground = new SimpleFilter.NotClause(andClause);
        filter.addClause(notBackground);

        SQLFragment originalFrag = Table.getSelectSQL(_table, _columns, filter, null, 0, 0);
        append("(").append(originalFrag).append(") AS A");
        outdent();

        Map<String, SQLFragment> joins = new LinkedHashMap<String, SQLFragment>();

        appendNewLine();
        append("INNER JOIN");
        indent();
        appendNewLine();
        append("(SELECT");
        indent();

        strComma = "";
        for (ColumnInfo column : _matchCols)
        {
            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
            column.declareJoins(joins);
            append(strComma);
            appendNewLine();
            append(column.getSelectSql());
            strComma = ",";
        }
        for (ColumnInfo column : _statCols)
        {
            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
            column.declareJoins(joins);
            append(",");
            appendNewLine();
            append("AVG(").append(column.getValueSql()).append(") AS ").append(column.getSelectName());
        }
        outdent();

        appendNewLine();
        append("FROM");
        indent();
        appendNewLine();
        append(_table.getFromSQL());
        for (Map.Entry<String, SQLFragment> entry : joins.entrySet())
        {
            appendNewLine();
            append(entry.getValue());
        }
        outdent();

        appendNewLine();

        // WHERE background ...
        filter = new SimpleFilter(sortFilterUrl, "query");
        filter.addAllClauses(_backgroundFilter);
        SQLFragment filterFrag = filter.getSQLFragment(dialect, columnMap);

        append(filterFrag);

        appendNewLine();
        append("GROUP BY");
        indent();

        strComma = "";
        for (ColumnInfo column : _matchCols)
        {
            append(strComma);
            appendNewLine();
            append(column.getValueSql());
            strComma = ", ";
        }
        outdent();

        appendNewLine();
        append(") AS BG");
        outdent();

        strComma = "";
        appendNewLine();
        append("ON");
        indent();

        for (ColumnInfo column : _matchCols)
        {
            append(strComma);
            appendNewLine();
            append("A.").append(column.getSelectName());
            append(" = BG.").append(column.getSelectName());
            strComma = " AND ";
        }

        Sort sort = new Sort(sortFilterUrl, "query");
        appendNewLine();
        append(sort.getOrderByClause(dialect, columnMap));
    }

    protected void appendNewLine()
    {
        append(getNewLine());
    }

    protected String getNewLine()
    {
        assert _indent >= 0;
        return "\n" + StringUtils.repeat("  ", _indent);
    }

    protected void indent()
    {
        _indent++;
    }

    protected void outdent()
    {
        _indent--;
    }
}
