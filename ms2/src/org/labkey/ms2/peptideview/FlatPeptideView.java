package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class FlatPeptideView extends AbstractMS2RunView
{
    public FlatPeptideView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, "Peptides", runs);
    }

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean forExport) throws ServletException, SQLException
    {
        DataRegion rgn = getPeptideGridForDisplay(requestedPeptideColumnNames);
        GridView peptideView = new GridView(rgn);
        peptideView.setFilter(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER));
        peptideView.setSort(ProteinManager.getPeptideBaseSort());
        peptideView.setTitle("Peptides");
        return peptideView;
    }


    public void exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        exportToTSV(form, response, selectedRows);
    }

    private DataRegion getPeptideGridForDisplay(String columnNames) throws SQLException
    {
        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows);

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

    public void exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        List<MS2Run> runs = Arrays.asList(_runs);
        SimpleFilter filter = ProteinManager.getPeptideFilter(_url, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

        if (selectedRows != null)
        {
            List<Long> peptideIds = new ArrayList<Long>(selectedRows.size());

            // Technically, should only limit this in Excel export case... but there's no way to individually select 65K peptides
            for (int i = 0; i < Math.min(selectedRows.size(), ExcelWriter.MAX_ROWS); i++)
            {
                String[] row = selectedRows.get(i).split(",");
                peptideIds.add(Long.parseLong(row[row.length == 1 ? 0 : 1]));
            }

            filter.addInClause("RowId", peptideIds);
        }

        RenderContext ctx = new MultiRunRenderContext(_viewContext, runs);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        ctx.setCache(false);

        String columnNames = getPeptideColumnNames(form.getColumns());
        List<DisplayColumn> displayColumns = getPeptideDisplayColumns(columnNames);
        changePeptideCaptionsForTsv(displayColumns);

        TSVGridWriter tw = new TSVGridWriter(ctx, MS2Manager.getTableInfoPeptides(), displayColumns, MS2Manager.getDataRegionNamePeptides());
        tw.setFilenamePrefix("MS2Runs");
        tw.setFileHeader(null);   // Used for AMT file export
        tw.write(response);
    }
}
