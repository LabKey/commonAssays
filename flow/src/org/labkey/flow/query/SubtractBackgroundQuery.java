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

import org.apache.commons.collections15.IteratorUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.model.ScriptSettings;
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
public class SubtractBackgroundQuery
{
    protected ICSMetadata _metadata;

    protected FlowQueryView _view;
    protected QueryDefinition _query;
    protected TableInfo _table;
    protected List<ColumnInfo> _columns;
    protected List<DisplayColumn> _displayColumns;
    private List<ColumnInfo> _displayColumnInfos;

    protected List<ColumnInfo> _ordinaryCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _matchCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _statCols = new ArrayList<ColumnInfo>();
    protected List<ColumnInfo> _backgroundCols = new ArrayList<ColumnInfo>();
    protected SimpleFilter _backgroundFilter = new SimpleFilter();

    public SubtractBackgroundQuery(FlowQueryView view, ICSMetadata metadata, List<String> errors)
    {
        _metadata = metadata;
        _view = view;
//      _run = view.getSchema().getRun();
        _query = view.getQueryDef();
        _table = view.getTable();
        _displayColumns = new LinkedList<DisplayColumn>(view.getDisplayColumns());
        _columns = new LinkedList<ColumnInfo>();
        for (DisplayColumn displayCol : _displayColumns)
        {
            ColumnInfo column = displayCol.getColumnInfo();
            if (column != null)
                _columns.add(column);
        }
        _displayColumnInfos = getDisplayList(_columns);

        scanColumns(errors);
    }

    public ResultSet createResultSet(FlowQuerySettings settings, RenderContext context) throws SQLException
    {
        SimpleFilter filter = (SimpleFilter)context.getBaseFilter();
        Sort sort = context.getBaseSort();

        ActionURL sortFilterUrl = settings.getSortFilterURL();
        filter.addUrlFilters(sortFilterUrl, "query");
        sort.applyURLSort(sortFilterUrl, "query");

        SQLFragment sql = genSql(filter, sort, settings.getMaxRows(), settings.getOffset());
        return Table.executeQuery(FlowManager.get().getSchema(), sql.getSQL(), sql.getParams().toArray(), settings.getMaxRows(), true);
    }

    // XXX: only keep %P stats?
    protected boolean isStatColumn(ColumnInfo column)
    {
        return column.getName().startsWith("Statistic/") &&
               column.getName().endsWith(":Freq_Of_Parent");
    }

    protected List<ColumnInfo> getDisplayList(List<ColumnInfo> columns)
    {
        Map<String, ColumnInfo> colMap = new LinkedHashMap<String, ColumnInfo>();
        for (ColumnInfo column : columns)
        {
            colMap.put(column.getAlias(), column);
            ColumnInfo displayColumn = column.getDisplayField();
            if (displayColumn != null)
            {
                colMap.put(displayColumn.getAlias(), displayColumn);
            }
        }
        return Collections.unmodifiableList(new ArrayList<ColumnInfo>(colMap.values()));
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        return _displayColumns;
    }

    protected void scanColumns(List<String> errors)
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

        if (_matchCols.size() != _metadata.getMatchColumns().size())
            errors.add("expected to find background match columns '" + StringUtils.join(_metadata.getMatchColumns(), ", ") + "' but only found '" + columnsToString(_matchCols) + "'");
        if (_backgroundCols.size() != _metadata.getBackgroundFilter().size())
            errors.add("expected to find background columns '" + filtersToString(_metadata.getBackgroundFilter()) + "' but only found '" + StringUtils.join(_backgroundCols, ", ") + "'");
        if (_statCols.size() == 0)
            errors.add("at least one statistic column is required");

        // add a new "Corrected_Stat" columns
        for (ColumnInfo column : _statCols)
        {
            ColumnInfo correctedColumn = new ColumnInfo(column, column.getParentTable());
            correctedColumn.setName("Corrected_" + column.getName());
            correctedColumn.setAlias("Corrected_" + column.getAlias());

            DisplayColumn corrected = correctedColumn.getDisplayColumnFactory().createRenderer(correctedColumn);
            corrected.setCaption("Corrected " + corrected.getCaption());

            // XXX: ugh, linear
            int i = 0;
            for (DisplayColumn d : _displayColumns)
            {
                if (column.equals(d.getColumnInfo()))
                    break;
                i++;
            }
            _displayColumns.add(i+1, corrected);
        }
    }

    private String columnsToString(List<ColumnInfo> columns)
    {
        return StringUtils.join(IteratorUtils.transformedIterator(columns.iterator(), new Transformer<ColumnInfo, String>() {
            public String transform(ColumnInfo column)
            {
                return column.getName();
            }
        }), ", ");
    }

    private String filtersToString(List<ScriptSettings.FilterInfo> filters)
    {
        return StringUtils.join(IteratorUtils.transformedIterator(filters.iterator(), new Transformer<ScriptSettings.FilterInfo, FieldKey>() {
            public FieldKey transform(ScriptSettings.FilterInfo filter)
            {
                return filter.getField();
            }
        }), ", ");
    }

    protected SQLFragment genSql(Filter baseFilter, Sort baseSort, int maxRows, long offset)
    {
        SqlDialect dialect = _table.getSqlDialect();
        Map<String, ColumnInfo> columnMap = Table.createColumnMap(_table, _displayColumnInfos);
        Table.ensureRequiredColumns(_table, columnMap, baseFilter, baseSort);

        SQLFragment selectFrag = new SQLFragment();
        selectFrag.append("SELECT");

        String strComma = "";
//        for (ColumnInfo column : _ordinaryCols)
//        {
//            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
//            selectFrag.append(strComma);
//            selectFrag.appendNewLine();
//            selectFrag.append("A.").append(column.getSelectName());
//            strComma = ",";
//        }
//        for (ColumnInfo column : _matchCols)
//        {
//            selectFrag.append(",");
//            selectFrag.appendNewLine();
//            selectFrag.append("A.").append(column.getSelectName());
//        }
//        for (ColumnInfo column: _backgroundCols)
//        {
//            selectFrag.append(",");
//            selectFrag.appendNewLine();
//            selectFrag.append("A.").append(column.getSelectName());
//        }
        selectFrag.append(" A.*");
        for (ColumnInfo column : _statCols)
        {
            selectFrag.append(",\n");
            selectFrag.append("A.").append(column.getSelectName()).append(" - ").append("BG.").append(column.getSelectName());
            selectFrag.append(" AS ").append("Corrected_").append(column.getSelectName());
        }

        selectFrag.append("\n");
        SQLFragment fromFrag = new SQLFragment();
        fromFrag.append("FROM");
        selectFrag.append("\n");

        // WHERE not background
        // BUG: baseFilter doesn't work -- need to have selectSql for some reason
        SimpleFilter filter = new SimpleFilter(); // new SimpleFilter(baseFilter);
        SimpleFilter.FilterClause andClause = new SimpleFilter.AndClause(_backgroundFilter.getClauses().toArray(new SimpleFilter.FilterClause[0]));
        SimpleFilter.FilterClause notBackground = new SimpleFilter.NotClause(andClause);
        filter.addClause(notBackground);

        SQLFragment originalFrag = Table.getSelectSQL(_table, _displayColumnInfos, filter, null, 0, 0);
        fromFrag.append("(").append(originalFrag).append(") AS A");

        Map<String, SQLFragment> joins = new LinkedHashMap<String, SQLFragment>();

        fromFrag.append("\n");
        fromFrag.append("INNER JOIN");
        fromFrag.append("\n");
        fromFrag.append("(SELECT");

        strComma = "\n";
        for (ColumnInfo column : _matchCols)
        {
            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
            column.declareJoins(joins);
            fromFrag.append(strComma);
            fromFrag.append(column.getSelectSql());
            strComma = ",\n";
        }
        for (ColumnInfo column : _statCols)
        {
            assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
            column.declareJoins(joins);
            fromFrag.append(",\n");
            fromFrag.append("AVG(").append(column.getValueSql()).append(") AS ").append(column.getSelectName());
        }

        fromFrag.append("\n");
        fromFrag.append("FROM");
        fromFrag.append("\n");
        fromFrag.append(_table.getFromSQL());
        for (Map.Entry<String, SQLFragment> entry : joins.entrySet())
        {
            fromFrag.append("\n");
            fromFrag.append(entry.getValue());
        }

        fromFrag.append("\n");

        // WHERE background ...
        // XXX: baseFilter doesn't work
        filter = new SimpleFilter(); //new SimpleFilter(baseFilter);
        filter.addAllClauses(_backgroundFilter);
        SQLFragment filterFrag = filter.getSQLFragment(dialect, columnMap);

        fromFrag.append(filterFrag);

        fromFrag.append("\n");
        fromFrag.append("GROUP BY");

        strComma = "\n";
        for (ColumnInfo column : _matchCols)
        {
            fromFrag.append(strComma);
            fromFrag.append(column.getValueSql());
            strComma = ",\n";
        }

        fromFrag.append("\n");
        fromFrag.append(") AS BG");

        fromFrag.append("\n");
        fromFrag.append("ON");

        strComma = "";
        for (ColumnInfo column : _matchCols)
        {
            fromFrag.append(strComma);
            fromFrag.append("\n");
            fromFrag.append("A.").append(column.getSelectName());
            fromFrag.append(" = BG.").append(column.getSelectName());
            strComma = " AND ";
        }

        String orderBy = null;
        if ((baseSort == null || baseSort.getSortList().size() == 0) && (maxRows > 0 || offset > 0))
        {
            baseSort = Table.createDefaultSort(_columns);
        }
        if (baseSort != null && baseSort.getSortList().size() > 0)
        {
            orderBy = baseSort.getOrderByClause(dialect, columnMap);
        }

        return dialect.limitRows(selectFrag, fromFrag, null, orderBy, maxRows, offset);
    }
}