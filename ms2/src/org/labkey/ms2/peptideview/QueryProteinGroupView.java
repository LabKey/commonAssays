package org.labkey.ms2.peptideview;

import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.security.User;
import org.labkey.api.query.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class QueryProteinGroupView extends AbstractPeptideView
{
    private ViewContext _viewContext;
    private static final String DATA_REGION_NAME = "ProteinGroups";

    public QueryProteinGroupView(Container container, User user, ViewURLHelper url, MS2Run[] runs, ViewContext viewContext)
    {
        super(container, user, "Peptides", url, runs);
        _viewContext = viewContext;
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

    public WebPartView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(DATA_REGION_NAME, schema, _maxPeptideRows);

        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(_viewContext, schema, settings, expanded);

        peptideView.setTitle("Protein Groups");
        return peptideView;
    }

    private class ProteinGroupQueryView extends QueryView
    {
        private QueryNestingOption _selectedNestingOption;

        private final boolean _expanded;

        public ProteinGroupQueryView(ViewContext context, UserSchema schema, QuerySettings settings, boolean expanded)
        {
            super(context, schema, settings);
            _expanded = expanded;
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTTOM;
        }

        protected DataRegion createDataRegion()
        {
            List<DisplayColumn> originalColumns = getDisplayColumns();
            ProteinGroupQueryNestingOption proteinGroupNesting = new ProteinGroupQueryNestingOption();

            if (proteinGroupNesting.isNested(originalColumns))
            {
                _selectedNestingOption = proteinGroupNesting;
            }

            DataRegion rgn;
            if (_selectedNestingOption != null)
            {
                rgn = _selectedNestingOption.createDataRegion(originalColumns, _runs, _url, getDataRegionName());
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

        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);
            ButtonBar bb = createButtonBar("exportAllPeptides", "exportSelectedPeptides", "peptides");
            for (DisplayElement element : bb.getList())
            {
                bar.add(element);
            }
        }

        protected ProteinGroupTableInfo createTable()
        {
            return ((MS2Schema)getSchema()).createProteinGroupsForRunTable(null);
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

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        throw new UnsupportedOperationException();
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

}
