package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.api.security.User;
import org.labkey.api.view.*;
import org.labkey.api.query.*;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.apache.commons.lang.StringUtils;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class QueryFlatPeptideView extends AbstractPeptideView
{
    private ViewContext _viewContext;
    private static final String DATA_REGION_NAME = "MS2Peptides";

    public QueryFlatPeptideView(Container container, User user, ViewURLHelper url, MS2Run[] runs, ViewContext viewContext)
    {
        super(container, user, "Peptides", url, runs);
        _viewContext = viewContext;
    }

    protected QuerySettings createQuerySettings(String tableName, UserSchema schema, int maxRows) throws RedirectException
    {
        QuerySettings settings = new QuerySettings(_url, DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName("FlatPeptides");
        settings.setDataRegionName(MS2Manager.getDataRegionNamePeptides());
        settings.setMaxRows(maxRows);
        String columnNames = _url.getParameter("columns");
        if (columnNames != null)
        {
            QueryDefinition def = settings.getQueryDef(schema);
            CustomView view = def.getCustomView(getUser(), "columns");
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
            view.save(getUser());
            settings.setViewName("columns");
            ViewURLHelper url = _url.clone();
            url.deleteParameter("columns");
            url.addParameter(DATA_REGION_NAME + ".viewName", "columns");
            throw new RedirectException(url.toString());
        }

        return settings;
    }

    public WebPartView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(DATA_REGION_NAME, schema, MAX_PEPTIDE_DISPLAY_ROWS);

        FlatPeptideQueryView peptideView = new FlatPeptideQueryView(_viewContext, schema, settings);
        FilteredTable table = peptideView.getFilteredTable();
        List<Object> params = new ArrayList<Object>(_runs.length);
        for (MS2Run run : _runs)
        {
            params.add(run.getRun());
        }

        table.addCondition(new SQLFragment(MS2Manager.getTableInfoPeptidesData() + ".Fraction IN " +
            "(SELECT Fraction FROM " + MS2Manager.getTableInfoFractions() + " WHERE Run IN (?" + StringUtils.repeat(", ?", params.size() - 1) + "))", params));

        peptideView.setTitle("Peptides");
        return peptideView;
    }

    private class FlatPeptideQueryView extends QueryView
    {
        public FlatPeptideQueryView(ViewContext context, UserSchema schema, QuerySettings settings)
        {
            super(context, schema, settings);
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTTOM; 
        }

        protected DataRegion createDataRegion()
        {
            DataRegion rgn = super.createDataRegion();
            rgn.setShowRecordSelectors(true);
            rgn.setFixedWidthColumns(true);

            setPeptideUrls(rgn, null);

            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
            rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it
            rgn.addHiddenFormField("columns", _url.getParameter("columns"));
            rgn.addHiddenFormField("run", _url.getParameter("run"));

            return rgn;
        }


        protected DataView createDataView()
        {
            DataView result = super.createDataView();
            result.getRenderContext().setBaseSort(ProteinManager.getPeptideBaseSort());
            result.getRenderContext().setBaseFilter(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.EXTRA_FILTER));
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

        public FilteredTable getFilteredTable()
        {
            return (FilteredTable)getTable();
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

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        throw new UnsupportedOperationException();
    }
}
