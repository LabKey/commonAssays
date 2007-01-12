package org.labkey.ms2.peptideview;

import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.ms2.MS2Run;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.security.User;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewURLHelper;
import org.fhcrc.cpas.util.Pair;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.List;

import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class FlatPeptideView extends AbstractPeptideView
{
    public FlatPeptideView(Container container, User user, ViewURLHelper url, MS2Run[] runs)
    {
        super(container, user, "Peptides", url, runs);
    }

    public GridView getGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
    {
        DataRegion rgn = getPeptideGridForDisplay(requestedPeptideColumnNames);
        GridView peptideView = new GridView(rgn);
        peptideView.setFilter(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER));
        peptideView.setSort(ProteinManager.getPeptideBaseSort());
        peptideView.setTitle("Peptides");
        return peptideView;
    }

    private DataRegion getPeptideGridForDisplay(String columnNames) throws SQLException
    {
        DataRegion rgn = getPeptideGrid(columnNames, MAX_PEPTIDE_DISPLAY_ROWS);

        rgn.setShowRecordSelectors(true);
        rgn.setFixedWidthColumns(true);

        setPeptideUrls(rgn, null);

        ButtonBar bb = createButtonBar("exportAllPeptides", "exportSelectedPeptides", "peptides");

        rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
        rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it
        rgn.addHiddenFormField("columns", _url.getParameter("columns"));
        rgn.addHiddenFormField("run", _url.getParameter("run"));

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);
        return rgn;
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
