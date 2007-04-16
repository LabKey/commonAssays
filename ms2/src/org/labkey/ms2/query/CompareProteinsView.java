package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.DataView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.compare.CompareDataRegion;

import java.util.*;
import java.net.URISyntaxException;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinsView extends QueryView
{
    private final List<MS2Run> _runs;
    private SimpleFilter _runFilter = new SimpleFilter();

    public CompareProteinsView(ViewContext context, UserSchema schema, QuerySettings settings, List<MS2Run> runs)
    {
        super(new ViewContext(context), schema, settings);
        _runs = runs;

        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
/*
        ViewURLHelper url = getViewContext().cloneViewURLHelper();
        SimpleFilter filter = new SimpleFilter(url, getSettings().getDataRegionName());

        for (SimpleFilter.FilterClause clause : new ArrayList<SimpleFilter.FilterClause>(filter.getClauses()))
        {
            for (String colName : clause.getColumnNames())
            {
                if (colName.startsWith("Run/"))
                {
                    SimpleFilter filterToRemove = new SimpleFilter();
                    filterToRemove.addClause(clause);
                    String urlParam = filterToRemove.toQueryString(getSettings().getDataRegionName());
                    if (urlParam != null && urlParam.indexOf('=') != -1)
                    {
                        url.deleteParameter(PageFlowUtil.decode(urlParam.substring(0, urlParam.indexOf('='))));

                        SimpleFilter.OrClause orClause = new SimpleFilter.OrClause();
                        for (MS2Run run : _runs)
                        {
                            String newParam = urlParam.replace("Run%2F", "Run" + run.getRun() + "%2F");
                            ViewURLHelper newURL = url.clone();
                            newURL.deleteParameters();
                            try
                            {
                                newURL = new ViewURLHelper(newURL + newParam);
                            }
                            catch (URISyntaxException e)
                            {
                                throw new RuntimeException(e);
                            }
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

        getViewContext().setViewURLHelper(url);*/
    }

    protected DataView createDataView()
    {
        DataView result = super.createDataView();
        SimpleFilter filter = new SimpleFilter(result.getRenderContext().getBaseFilter());

        for (SimpleFilter.FilterClause clause : new ArrayList<SimpleFilter.FilterClause>(filter.getClauses()))
        {
            for (String colName : clause.getColumnNames())
            {
                if (colName.startsWith("Run/"))
                {
                    SimpleFilter filterToRemove = new SimpleFilter();
                    filterToRemove.addClause(clause);
                    String urlParam = filterToRemove.toQueryString(getSettings().getDataRegionName());
                    if (urlParam != null && urlParam.indexOf('=') != -1)
                    {
                        filter.deleteConditions(colName);

                        SimpleFilter.OrClause orClause = new SimpleFilter.OrClause();
                        for (MS2Run run : _runs)
                        {
                            String newParam = urlParam.replace("Run%2F", "Run" + run.getRun() + "%2F");
                            ViewURLHelper newURL = result.getRenderContext().getViewContext().cloneViewURLHelper();
                            newURL.deleteParameters();
                            try
                            {
                                newURL = new ViewURLHelper(newURL + newParam);
                            }
                            catch (URISyntaxException e)
                            {
                                throw new RuntimeException(e);
                            }
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


    protected DataRegion createDataRegion()
    {
        List<DisplayColumn> displayColumns = getDisplayColumns();
        CompareDataRegion rgn = new CompareDataRegion(null);
        int offset = 0;
        for (DisplayColumn col : displayColumns)
        {
            if (col.getColumnInfo() == null || (!col.getColumnInfo().getName().toLowerCase().startsWith("run") || col.getColumnInfo().getName().indexOf('/') == -1))
            {
                offset++;
            }
        }
        rgn.setOffset(offset);
        List<String> headings = new ArrayList<String>();
        for (MS2Run run : _runs)
        {
            headings.add(run.getDescription());
        }
        rgn.setMultiColumnCaptions(headings);
        rgn.setColSpan((displayColumns.size() - offset) / _runs.size());
        rgn.setMaxRows(getMaxRows());
        rgn.setShowRecordSelectors(showRecordSelectors());
        rgn.setName(getDataRegionName());
        rgn.setDisplayColumnList(displayColumns);
        return rgn;
    }

    protected TableInfo createTable()
    {
        return new CompareProteinProphetTableInfo(null, (MS2Schema)getSchema(), _runs);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList();
        TableInfo table = getTable();
        if (table == null)
            return Collections.EMPTY_LIST;

        Collection<ColumnInfo> columns;
        CustomView view = getCustomView();
        List<FieldKey> cols;

        if (view != null)
        {
            cols = view.getColumns();
        }
        else
        {
            cols = table.getDefaultVisibleColumns();
        }

        List<FieldKey> nonRunCols = new ArrayList<FieldKey>();
        List<FieldKey> runCols = new ArrayList<FieldKey>();
        int runColCount = 0;
        for (FieldKey col : cols)
        {
            if ("Run".equalsIgnoreCase(col.getParts().get(0)))
            {
                List<String> parts = new ArrayList<String>(col.getParts());
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

        List<FieldKey> newCols = new ArrayList<FieldKey>(nonRunCols);
        newCols.addAll(runCols);
        if (view != null)
        {
            view.setColumns(newCols);
            columns = getQueryDef().getColumns(view, table);
        }
        else
        {
            columns = QueryService.get().getColumns(table, newCols).values();
        }
        
        for (ColumnInfo col : columns)
        {
            DisplayColumn renderer = col.getRenderer();
            ret.add(renderer);
        }
        return ret;
    }
}
