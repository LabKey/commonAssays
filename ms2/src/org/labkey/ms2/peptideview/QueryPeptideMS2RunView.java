package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.api.view.*;
import org.labkey.api.query.*;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

import org.labkey.ms2.OldMS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.PeptidesTableInfo;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class QueryPeptideMS2RunView extends AbstractQueryMS2RunView
{
    private static final String DATA_REGION_NAME = "MS2Peptides";

    public QueryPeptideMS2RunView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, "Peptides", runs);
    }

    protected QuerySettings createQuerySettings(String tableName, MS2Schema schema, int maxRows) throws RedirectException
    {
        QuerySettings settings = new QuerySettings(_url, _viewContext.getRequest(), DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.PEPTIDES_TABLE_NAME);
        settings.setDataRegionName(MS2Manager.getDataRegionNamePeptides());
        String columnNames = _url.getParameter("columns");
        if (columnNames != null)
        {
            QueryDefinition def = settings.getQueryDef(schema);
            CustomView view = def.getCustomView(getUser(), _viewContext.getRequest(), "columns");
            if (view == null)
            {
                view = def.createCustomView(getUser(), "columns");
            }
            StringTokenizer st = new StringTokenizer(columnNames, ", ");
            List<FieldKey> fieldKeys = new ArrayList<FieldKey>();
            while (st.hasMoreTokens())
            {
                fieldKeys.add(FieldKey.fromString(st.nextToken()));
            }
            view.setColumns(fieldKeys);
            view.save(getUser(), _viewContext.getRequest());
            settings.setViewName("columns");
            ActionURL url = _url.clone();
            url.deleteParameter("columns");
            url.addParameter(DATA_REGION_NAME + ".viewName", "columns");
            throw new RedirectException(url.toString());
        }

        return settings;
    }

    public PeptideQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings = createQuerySettings(DATA_REGION_NAME, schema, _maxPeptideRows);

        PeptideQueryView peptideView = new PeptideQueryView(_viewContext, schema, settings, expanded, allowNesting);

        peptideView.setTitle("Peptides");
        return peptideView;
    }

    private class PeptideQueryView extends AbstractMS2QueryView
    {
        public PeptideQueryView(ViewContext context, MS2Schema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(context, schema, settings, expanded, allowNesting);
        }

        public List<DisplayColumn> getDisplayColumns()
        {
            if (_overrideColumns != null)
            {
                List<DisplayColumn> result = new ArrayList<DisplayColumn>();
                for (ColumnInfo colInfo : QueryService.get().getColumns(getTable(), _overrideColumns).values())
                {
                    result.add(colInfo.getRenderer());
                }
                assert result.size() == _overrideColumns.size() : "Got the wrong number of columns back, " + result.size() + " instead of " + _overrideColumns.size();
                return result;
            }

            return super.getDisplayColumns();
        }

        protected DataRegion createDataRegion()
        { 
            List<DisplayColumn> originalColumns = getDisplayColumns();
            ProteinProphetQueryNestingOption proteinProphetNesting = new ProteinProphetQueryNestingOption(_allowNesting);
            StandardProteinQueryNestingOption standardProteinNesting = new StandardProteinQueryNestingOption(_allowNesting);

            if (proteinProphetNesting.isNested(originalColumns))
            {
                _selectedNestingOption = proteinProphetNesting;
            }
            else if (standardProteinNesting.isNested(originalColumns))
            {
                _selectedNestingOption = standardProteinNesting;
            }

            DataRegion rgn;
            if (_selectedNestingOption != null && (_allowNesting || !_expanded))
            {
                rgn = _selectedNestingOption.createDataRegion(originalColumns, _runs, _url, getDataRegionName(), _expanded);
                getSettings().setMaxRows(_selectedNestingOption.getResultSetRowLimit());
            }
            else
            {
                rgn = new DataRegion();
                rgn.setDisplayColumnList(originalColumns);
                getSettings().setMaxRows(1000);
            }
            rgn.setMaxRows(getMaxRows());
            rgn.setShowRecordSelectors(showRecordSelectors());
            rgn.setName(getDataRegionName());

            rgn.setShowRecordSelectors(true);
            rgn.setFixedWidthColumns(true);

            setPeptideUrls(rgn, null);

            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
            rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it
            rgn.addHiddenFormField("columns", _url.getParameter("columns"));
            //rgn.addHiddenFormField("run", _url.getParameter("run"));
            //rgn.addHiddenFormField("grouping", _url.getParameter("grouping"));

            return rgn;
        }

        protected DataView createDataView()
        {
            DataRegion rgn = createDataRegion();
            GridView result = new GridView(rgn, new SortRewriterRenderContext(_selectedNestingOption, getViewContext()));
            setupDataView(result);

            Sort customViewSort = result.getRenderContext().getBaseSort();
            Sort sort = ProteinManager.getPeptideBaseSort();
            if (customViewSort != null)
            {
                sort.insertSort(customViewSort);
            }
            result.getRenderContext().setBaseSort(sort);
            Filter customViewFilter = result.getRenderContext().getBaseFilter();
            SimpleFilter filter = new SimpleFilter(customViewFilter);
            if (_selectedRows != null)
            {
                String columnName = _selectedNestingOption == null ? "RowId" : _selectedNestingOption.getRowIdColumnName();
                filter.addClause(new SimpleFilter.InClause(columnName, _selectedRows));
            }
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, _runs));
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }

        protected PeptidesTableInfo createTable()
        {
            return new PeptidesTableInfo((MS2Schema) getSchema(), _runs, _url.clone(), true);
        }
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
    }

    public MS2RunViewType getViewType()
    {
        return MS2RunViewType.QUERY_PEPTIDES;
    }

    public GridView createPeptideViewForGrouping(OldMS2Controller.DetailsForm form)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPeptideStringsForGrouping(OldMS2Controller.DetailsForm form) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings;
        try
        {
            settings = createQuerySettings(DATA_REGION_NAME, schema, _maxPeptideRows);
        }
        catch (RedirectException e)
        {
            throw new RuntimeException(e);
        }
        PeptideQueryView view = new PeptideQueryView(_viewContext, schema, settings, true, true);
        QueryPeptideDataRegion rgn = (QueryPeptideDataRegion)view.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getSingleRun()));
        filter.addCondition(view._selectedNestingOption.getRowIdColumnName(), Integer.parseInt(proteinGroupingId));
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    protected List<String> getExportFormats()
    {
        List<String> result = new ArrayList<String>();
        result.add("Excel");
        result.add("TSV");
        result.add("AMT");
        return result;
    }
}
