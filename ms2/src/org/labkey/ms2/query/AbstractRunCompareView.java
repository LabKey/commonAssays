package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.ACL;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.compare.CompareDataRegion;

import javax.servlet.ServletException;
import java.util.*;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jun 21, 2007
 */
public abstract class AbstractRunCompareView  extends QueryView
{
    protected final List<MS2Run> _runs;
    private int _runListIndex;
    protected boolean _forExport;
    private SimpleFilter _runFilter = new SimpleFilter();
    private List<FieldKey> _columns;

    private List<String> _errors = new ArrayList<String>();

    public AbstractRunCompareView(ViewContext context, MS2Controller controller, int runListIndex, boolean forExport, String tableName) throws ServletException
    {
        super(new ViewContext(context), new MS2Schema(context.getUser(), context.getContainer()), createSettings(context, tableName));

        _viewContext.setViewURLHelper(context.getViewURLHelper());

        _runs = controller.getCachedRuns(runListIndex, _errors, false);

        if (_runs != null)
        {
            for (MS2Run run : _runs)
            {
                Container c = ContainerManager.getForId(run.getContainer());
                if (c == null || !c.hasPermission(getUser(), ACL.PERM_READ))
                {
                    HttpView.throwUnauthorized();
                }
            }

            getSchema().setRuns(_runs.toArray(new MS2Run[_runs.size()]));

            _runListIndex = runListIndex;
            _forExport = forExport;

            setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
            setShowExportButtons(false);
        }
    }

    public List<MS2Run> getRuns()
    {
        return _runs;
    }
    
    public List<String> getErrors()
    {
        return _errors;
    }

    private static QuerySettings createSettings(ViewContext context, String tableName)
    {
        QuerySettings settings = new QuerySettings(context.cloneViewURLHelper(), context.getRequest(), "Compare");
        settings.setQueryName(tableName);
        settings.setAllowChooseQuery(false);
        return settings;
    }

    protected MS2Schema getSchema()
    {
        return (MS2Schema)super.getSchema();
    }

    protected abstract String getGroupingColumnName();

    public CompareResult createCompareResult()
            throws SQLException, IOException, ServletException
    {
        List<FieldKey> cols = new ArrayList<FieldKey>();
        cols.add(FieldKey.fromParts(getGroupingColumnName()));
        cols.add(FieldKey.fromParts("Run", "RowId"));
        setColumns(cols);

        StringBuilder sb = new StringBuilder();

        TSVGridWriter tsvWriter = getTsvWriter();
        try
        {
            tsvWriter.setCaptionRowVisible(false);
            tsvWriter.write(sb);

            StringTokenizer lines = new StringTokenizer(sb.toString(), "\n");
            int proteinCount = lines.countTokens();
            String[] proteins = new String[proteinCount];
            String[] runNames = new String[_runs.size()];
            String[] runURLs = new String[_runs.size()];
            boolean[][] hits = new boolean[proteinCount][];

            int runIndex = 0;
            for (MS2Run run : _runs)
            {
                ViewURLHelper runURL = new ViewURLHelper("MS2", "showRun.view", ContainerManager.getForId(run.getContainer()));
                runURL.addParameter("run", run.getRun());
                runURLs[runIndex] = runURL.getLocalURIString();
                runNames[runIndex++] = run.getDescription();
            }

            int index = 0;
            while (lines.hasMoreTokens())
            {
                String line = lines.nextToken();
                String[] values = line.split("\\t");
                proteins[index] = values[0];
                hits[index] = new boolean[_runs.size()];
                for (int i = 0; i < _runs.size(); i++)
                {
                    hits[index][i] = !"".equals(values[i + 1].trim());
                }
                index++;
            }
            return new CompareResult(proteins, runNames, runURLs, hits);
        }
        finally
        {
            tsvWriter.close();
        }
    }
    
    protected DataView createDataView()
    {
        DataView result = super.createDataView();
        Sort sort = result.getRenderContext().getBaseSort();
        if (sort == null)
        {
            sort = new Sort();
        }
        sort.insertSortColumn("-Pattern", false);
        sort.insertSortColumn("-RunCount", false);
        result.getRenderContext().setBaseSort(sort);
        result.getRenderContext().setViewContext(getViewContext());
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

    protected abstract String getGroupHeader();

    public void setColumns(List<FieldKey> columns)
    {
        _columns = columns;
    }

    protected DataRegion createDataRegion()
    {
        List<DisplayColumn> displayColumns = getDisplayColumns();
        CompareDataRegion rgn = new CompareDataRegion(null, getGroupHeader());
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
            ViewURLHelper url = new ViewURLHelper("MS2", "showRun.view", ContainerManager.getForId(run.getContainer()));
            url.addParameter("run", run.getRun());
            headings.add("<a href=\"" + url.getLocalURIString() + "\">" + PageFlowUtil.filter(run.getDescription()) + "</a>");
        }
        rgn.setMultiColumnCaptions(headings);
        rgn.setColSpan((displayColumns.size() - offset) / _runs.size());
        rgn.setMaxRows(getMaxRows());
        rgn.setShowRecordSelectors(showRecordSelectors());
        rgn.setName(getDataRegionName());
        rgn.setDisplayColumnList(displayColumns);
        return rgn;
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

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        ViewURLHelper excelURL = getViewContext().cloneViewURLHelper();
        excelURL.setAction(getExcelExportActionName());
        excelURL.addParameter("runList", Integer.toString(_runListIndex));
        ActionButton excelButton = new ActionButton("Export to Excel", excelURL);
        bar.add(excelButton);

        ViewURLHelper tsvURL = getViewContext().cloneViewURLHelper();
        tsvURL.setAction(getTSVExportActionName());
        excelURL.addParameter("runList", Integer.toString(_runListIndex));
        ActionButton tsvButton = new ActionButton("Export to TSV", tsvURL);
        bar.add(tsvButton);
    }

    protected abstract String getTSVExportActionName();

    protected abstract String getExcelExportActionName();

    public abstract String getComparisonName();
}
