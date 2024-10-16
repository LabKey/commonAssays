/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

package org.labkey.ms2.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListCache;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.compare.CompareDataRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 21, 2007
 */
public abstract class AbstractRunCompareView extends QueryView
{
    protected List<MS2Run> _runs;
    protected boolean _forExport;
    private final SimpleFilter _runFilter = new SimpleFilter();
    private List<FieldKey> _columns;

    public AbstractRunCompareView(ViewContext context, int runListIndex, boolean forExport, String tableName)
    {
        super(new MS2Schema(context.getUser(), context.getContainer()));
        setSettings(createSettings(context, tableName));

        _viewContext.setActionURL(context.getActionURL());

        try
        {
            _runs = RunListCache.getCachedRuns(runListIndex, false, context);
        }
        catch (RunListException e)
        {
            _runs = null;
        }

        if (_runs != null)
        {
            for (MS2Run run : _runs)
            {
                Container c = run.getContainer();
                if (c == null || !c.hasPermission(getUser(), ReadPermission.class))
                {
                    throw new UnauthorizedException();
                }
            }

            getSchema().setRuns(_runs);

            _forExport = forExport;

            setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        }
        // ExcelWebQueries won't be part of the same HTTP session so we won't have access to the run list anymore
        setAllowExportExternalQuery(false);
    }

    public List<MS2Run> getRuns()
    {
        return _runs;
    }
    
    private QuerySettings createSettings(ViewContext context, String tableName)
    {
        return getSchema().getSettings(context, "Compare", tableName);
    }

    @Override
    public MS2Schema getSchema()
    {
        return (MS2Schema)super.getSchema();
    }

    @Override
    public DataView createDataView()
    {
        DataView result = super.createDataView();
        Sort sort = result.getRenderContext().getBaseSort();
        if (sort == null)
        {
            sort = new Sort();
        }
        sort.insertSortColumn("-RunCount", false, sort.getSortList().size());
        sort.insertSortColumn("-Pattern", false, sort.getSortList().size());
        result.getRenderContext().setBaseSort(sort);
        result.getRenderContext().setViewContext(getViewContext());
        SimpleFilter filter = new SimpleFilter(result.getRenderContext().getBaseFilter());

        for (SimpleFilter.FilterClause clause : new ArrayList<>(filter.getClauses()))
        {
            for (FieldKey fieldKey : clause.getFieldKeys())
            {
                if (fieldKey.toString().startsWith("Run/"))
                {
                    SimpleFilter filterToRemove = new SimpleFilter();
                    filterToRemove.addClause(clause);
                    String urlParam = filterToRemove.toQueryString(getSettings().getDataRegionName());
                    if (urlParam != null && urlParam.indexOf('=') != -1)
                    {
                        filter.deleteConditions(fieldKey);

                        SimpleFilter.OrClause orClause = new SimpleFilter.OrClause();
                        for (MS2Run run : _runs)
                        {
                            String newParam = urlParam.replace("Run%2F", "Run" + run.getRun() + "%2F");
                            ActionURL newURL = result.getRenderContext().getViewContext().cloneActionURL();
                            newURL.deleteParameters();
                            newURL = new ActionURL(newURL + newParam);
                            SimpleFilter newFilter = new SimpleFilter(newURL, getSettings().getDataRegionName());
                            for (SimpleFilter.FilterClause newClause : newFilter.getClauses())
                            {
                                orClause.addClause(newClause);
                            }
                        }
                        _runFilter.addClause(orClause);
                    }
                }
            }
        }

        filter.addAllClauses(_runFilter);

        result.getRenderContext().setBaseFilter(filter);
        return result;
    }

    protected abstract String getGroupHeader();

    public void setColumns(List<FieldKey> columns)
    {
        _columns = columns;
    }

    @Override
    protected DataRegion createDataRegion()
    {
        CompareDataRegion rgn = new CompareDataRegion(null, getGroupHeader());
        configureDataRegion(rgn);
        List<DisplayColumn> displayColumns = getDisplayColumns();
        int offset = 0;
        for (DisplayColumn col : displayColumns)
        {
            if (col.getColumnInfo() == null || (!col.getColumnInfo().getName().toLowerCase().startsWith("run") || col.getColumnInfo().getName().indexOf('/') == -1))
            {
                offset++;
            }
        }
        rgn.setOffset(offset);
        List<String> headings = new ArrayList<>();
        for (MS2Run run : _runs)
        {
            ActionURL url = MS2Controller.getShowRunURL(getUser(), run.getContainer(), run.getRun());
            headings.add("<a href=\"" + url.getLocalURIString() + "\">" + PageFlowUtil.filter(run.getDescription()) + "</a>");
        }
        rgn.setMultiColumnCaptions(headings);
        rgn.setColSpan((displayColumns.size() - offset) / _runs.size());
        return rgn;
    }

    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList<>();
        TableInfo table = getTable();
        if (table == null)
            return Collections.emptyList();

        CustomView view = getCustomView();
        List<FieldKey> cols;

        if (_columns != null)
        {
            cols = _columns;
        }
        else if (view != null)
        {
            cols = view.getColumns();
        }
        else
        {
            cols = table.getDefaultVisibleColumns();
        }

        List<FieldKey> nonRunCols = new ArrayList<>();
        List<FieldKey> runCols = new ArrayList<>();
        int runColCount = 0;
        for (FieldKey col : cols)
        {
            if ("Run".equalsIgnoreCase(col.getParts().get(0)))
            {
                List<String> parts = new ArrayList<>(col.getParts());
                int runOffset = 0;
                for (MS2Run run : _runs)
                {
                    parts.set(0, "Run" + run.getRun());
                    runCols.add((runColCount + 1) * runOffset + runColCount, FieldKey.fromParts(parts));
                    runOffset++;
                }
                runColCount++;
            }
            else
            {
                nonRunCols.add(col);
            }
        }

        List<FieldKey> newCols = new ArrayList<>(nonRunCols);
        newCols.addAll(runCols);
        if (view != null)
        {
            view.setColumns(newCols);
            ret.addAll(getQueryDef().getDisplayColumns(view, table));
        }
        else
        {
            for (ColumnInfo col : QueryService.get().getColumns(table, newCols).values())
            {
                DisplayColumn renderer = col.getRenderer();
                ret.add(renderer);
            }
        }

        return ret;
    }
}
