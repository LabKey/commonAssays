package org.labkey.ms2.peptideview;

import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class QueryProteinGroupMS2RunView extends AbstractQueryMS2RunView
{
    private static final String DATA_REGION_NAME = "ProteinGroups";

    public QueryProteinGroupMS2RunView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, "Peptides", runs);
    }

    protected QuerySettings createQuerySettings(String tableName, UserSchema schema, int maxRows) throws RedirectException
    {
        QuerySettings settings = new QuerySettings(_url, _viewContext.getRequest(), DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.PROTEIN_GROUPS_FOR_RUN_TABLE_NAME);
        settings.setDataRegionName(MS2Manager.getDataRegionNamePeptides());
        settings.setMaxRows(maxRows);

        return settings;
    }

    public ProteinGroupQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(DATA_REGION_NAME, schema, _maxPeptideRows);

        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(_viewContext, schema, settings, expanded, allowNesting);

        peptideView.setTitle("Protein Groups");
        return peptideView;
    }

    public class ProteinGroupQueryView extends AbstractMS2QueryView
    {
        public ProteinGroupQueryView(ViewContext context, UserSchema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(context, schema, settings, expanded, allowNesting);
        }

        protected DataRegion createDataRegion()
        {
            List<DisplayColumn> originalColumns = getDisplayColumns();
            ProteinGroupQueryNestingOption proteinGroupNesting = new ProteinGroupQueryNestingOption();

            if (_allowNesting && proteinGroupNesting.isNested(originalColumns))
            {
                _selectedNestingOption = proteinGroupNesting;
            }

            DataRegion rgn;
            if (_selectedNestingOption != null)
            {
                rgn = _selectedNestingOption.createDataRegion(originalColumns, _runs, _url, getDataRegionName(), _expanded);
            }
            else
            {
                rgn = new DataRegion();
                rgn.setDisplayColumnList(originalColumns);
            }
            rgn.setMaxRows(getMaxRows());
            rgn.setShowRecordSelectors(showRecordSelectors());
            rgn.setName(getDataRegionName());

            rgn.setShowRecordSelectors(true);
            rgn.setFixedWidthColumns(true);

            return rgn;
        }

        protected DataView createDataView()
        {
            DataRegion rgn = createDataRegion();
            GridView result = new GridView(rgn, new SortRewriterRenderContext(_selectedNestingOption, getViewContext()));
            setupDataView(result);

            Sort customViewSort = result.getRenderContext().getBaseSort();
            Sort sort = new Sort("RowId");     // Always sort peptide lists by RowId
            sort.setMaxClauses(4);             // Need room for base sort plus three clauses from URL
            if (customViewSort != null)
            {
                sort.insertSort(customViewSort);
            }
            result.getRenderContext().setBaseSort(sort);
            Filter customViewFilter = result.getRenderContext().getBaseFilter();
            SimpleFilter filter = new SimpleFilter(customViewFilter);
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.EXTRA_FILTER));
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }

        protected ProteinGroupTableInfo createTable()
        {
            ProteinGroupTableInfo result = ((MS2Schema)getSchema()).createProteinGroupsForRunTable(null);
            if (_extraFragment != null)
            {
                result.addCondition(_extraFragment);
            }
            result.setRunFilter(_runs);
            return result;
        }

        public void exportToExcel(HttpServletResponse response, SimpleFilter filter) throws Exception
        {
            SQLFragment sql = new SQLFragment();
            String separator = "";
            for (SimpleFilter.FilterClause clause : filter.getClauses())
            {
                sql.append(separator);
                separator = " AND ";
                sql.append(clause.toSQLFragment(Collections.<String, ColumnInfo>emptyMap(), MS2Manager.getSqlDialect()));
            }
            _extraFragment = sql;
            exportToExcel(response);
        }
    }

    public AbstractProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumns, MS2Run run, String where) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(String requestedProteinColumnNames, String requestedPeptideColumnNames, boolean expanded) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void addSQLSummaries(List<Pair<String, String>> sqlSummaries)
    {

    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        throw new UnsupportedOperationException();
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());

        QuerySettings settings;
        try
        {
            settings = createQuerySettings(DATA_REGION_NAME, schema, _maxPeptideRows);
        }
        catch (RedirectException e)
        {
            throw new RuntimeException(e);
        }
        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(_viewContext, schema, settings, true, false);
        QueryPeptideDataRegion rgn = (QueryPeptideDataRegion)peptideView.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.EXTRA_FILTER));
        filter.addCondition(peptideView._selectedNestingOption.getRowIdColumnName(), Integer.parseInt(proteinGroupingId));
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

}
