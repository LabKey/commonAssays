package org.labkey.ms2.peptideview;

import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.OldMS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.util.List;
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

    protected QuerySettings createQuerySettings(UserSchema schema, int maxRows) throws RedirectException
    {
        QuerySettings settings = new QuerySettings(_url, _viewContext.getRequest(), DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.PROTEIN_GROUPS_FOR_RUN_TABLE_NAME);
        settings.setMaxRows(maxRows);

        return settings;
    }

    public ProteinGroupQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(schema, _maxPeptideRows);

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
            ProteinGroupQueryNestingOption proteinGroupNesting = new ProteinGroupQueryNestingOption(_allowNesting);

            if (proteinGroupNesting.isNested(originalColumns))
            {
                _selectedNestingOption = proteinGroupNesting;
            }

            DataRegion rgn;
            if (_selectedNestingOption != null && (_allowNesting || !_expanded))
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
            if (customViewSort != null)
            {
                sort.insertSort(customViewSort);
            }
            result.getRenderContext().setBaseSort(sort);
            Filter customViewFilter = result.getRenderContext().getBaseFilter();
            SimpleFilter filter = new SimpleFilter(customViewFilter);
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, _runs));
            if (_selectedRows != null)
            {
                String columnName = _selectedNestingOption == null ? "RowId" : _selectedNestingOption.getRowIdColumnName();
                filter.addClause(new SimpleFilter.InClause(columnName, _selectedRows));
            }
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }

        protected ProteinGroupTableInfo createTable()
        {
            ProteinGroupTableInfo result = ((MS2Schema)getSchema()).createProteinGroupsForRunTable(null, false);
            result.setRunFilter(_runs);
            return result;
        }
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {

    }

    public MS2RunViewType getViewType()
    {
        return MS2RunViewType.QUERY_PROTEIN_GROUPS;
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
            settings = createQuerySettings(schema, _maxPeptideRows);
        }
        catch (RedirectException e)
        {
            throw new RuntimeException(e);
        }
        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(_viewContext, schema, settings, true, true);
        QueryPeptideDataRegion rgn = (QueryPeptideDataRegion)peptideView.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getSingleRun()));
        filter.addCondition(peptideView._selectedNestingOption.getRowIdColumnName(), Integer.parseInt(proteinGroupingId));
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    protected void addExportFormats(DropDownList exportFormat)
    {
        exportFormat.add("Excel");
        exportFormat.add("TSV");
    }
}
