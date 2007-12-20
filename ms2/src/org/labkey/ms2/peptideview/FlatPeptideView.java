package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.*;
import java.io.IOException;

import org.labkey.ms2.OldMS2Controller;
import jxl.write.WritableWorkbook;

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
        peptideView.setFilter(ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER, getSingleRun()));
        peptideView.setSort(ProteinManager.getPeptideBaseSort());
        peptideView.setTitle("Peptides");
        return peptideView;
    }


    public void exportToAMT(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    public void exportToExcel(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        List<MS2Run> runs = Arrays.asList(_runs);
        SimpleFilter filter = createFilter(selectedRows);

        boolean includeHeaders = form.getExportFormat().equals("Excel");

        ServletOutputStream outputStream = ExcelWriter.getOutputStream(response, "MS2Runs");
        WritableWorkbook workbook = ExcelWriter.getWorkbook(outputStream);

        ViewURLHelper currentUrl = _url.clone();

        ExcelWriter ew = new ExcelWriter();
        ew.setSheetName("MS2 Runs");

        List<String> headers;

        if (includeHeaders)
        {
            MS2Run run = runs.get(0);

            if (runs.size() == 1)
            {
                headers = getRunSummaryHeaders(run);
                String whichPeptides;
                if (selectedRows == null)
                {
                    whichPeptides = "All";
                }
                else
                {
                    whichPeptides = "Hand selected";
                }
                headers.add(whichPeptides + " peptides matching the following query:");
                addPeptideFilterText(headers, run, currentUrl);
                ew.setSheetName(run.getDescription() + " Peptides");
            }
            else
            {
                headers = new ArrayList<String>();
                headers.add("Multiple runs showing " + (selectedRows == null ? "all" : "hand selected") + " peptides matching the following query:");
                addPeptideFilterText(headers, run, currentUrl);  // TODO: Version that takes runs[]
            }
            headers.add("");
            ew.setHeaders(headers);
        }

        // Always include column captions at the top
        ew.setColumns(getPeptideDisplayColumns(getPeptideColumnNames(form.getColumns())));
        ew.renderNewSheet(workbook);
        ew.setCaptionRowVisible(false);

        // TODO: Footer?

        for (int i = 0; i < runs.size(); i++)
        {
            if (includeHeaders)
            {
                headers = new ArrayList<String>();

                if (runs.size() > 1)
                {
                    if (i > 0)
                        headers.add("");

                    headers.add(runs.get(i).getDescription());
                }

                ew.setHeaders(headers);
            }

            setupExcelPeptideGrid(ew, filter, form.getColumns(), runs.get(i));
            ew.renderCurrentSheet(workbook);
        }
        

        ExcelWriter.closeWorkbook(workbook, outputStream);
    }

    private SimpleFilter createFilter(List<String> selectedRows)
    {
        SimpleFilter filter = ProteinManager.getPeptideFilter(_url, Arrays.asList(_runs), ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

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
        return filter;
    }

    private void setupExcelPeptideGrid(ExcelWriter ew, SimpleFilter filter, String requestedPeptideColumns, MS2Run run) throws ServletException, SQLException, IOException
    {
        String columnNames = getPeptideColumnNames(requestedPeptideColumns);
        DataRegion rgn = getPeptideGrid(columnNames, ExcelWriter.MAX_ROWS);
        Container c = getContainer();
        ProteinManager.replaceRunCondition(filter, null, run);

        RenderContext ctx = new RenderContext(_viewContext);
        ctx.setContainer(c);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        ew.setResultSet(rgn.getResultSet(ctx));
        ew.setColumns(rgn.getDisplayColumnList());
        ew.setAutoSize(true);
    }

    private DataRegion getPeptideGridForDisplay(String columnNames) throws SQLException
    {
        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows);

        rgn.setShowRecordSelectors(true);
        rgn.setFixedWidthColumns(true);

        setPeptideUrls(rgn, null);

        ButtonBar bb = createButtonBar("exportAllPeptides", "exportSelectedPeptides", "peptides", rgn);

        rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
        rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it
        rgn.addHiddenFormField("columns", _url.getParameter("columns"));
        rgn.addHiddenFormField("run", _url.getParameter("run"));

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);
        return rgn;
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilter.getFilterText()));
        sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(_url, MS2Manager.getDataRegionNamePeptides()).getSortText()));
    }

    public MS2RunViewType getViewType()
    {
        return MS2RunViewType.NONE;
    }

    public SQLFragment getProteins(ViewURLHelper queryUrl, MS2Run run, OldMS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT SeqId FROM ");
        fragment.append(MS2Manager.getTableInfoPeptides());
        fragment.append(" ");
        SimpleFilter filter = ProteinManager.getPeptideFilter(queryUrl, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run);
        fragment.append(filter.getWhereSQL(ProteinManager.getSqlDialect()));
        fragment.addAll(filter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ViewURLHelper queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<String, SimpleFilter>();
        map.put("peptideFilter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public GridView createPeptideViewForGrouping(OldMS2Controller.DetailsForm form)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPeptideStringsForGrouping(OldMS2Controller.DetailsForm form) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void exportToTSV(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
    {
        List<MS2Run> runs = Arrays.asList(_runs);
        SimpleFilter filter = createFilter(selectedRows);

        RenderContext ctx = new MultiRunRenderContext(_viewContext, runs);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        ctx.setCache(false);

        String columnNames = getPeptideColumnNames(form.getColumns());
        List<DisplayColumn> displayColumns = getPeptideDisplayColumns(columnNames);
        changePeptideCaptionsForTsv(displayColumns);

        TSVGridWriter tw = new TSVGridWriter(ctx, MS2Manager.getTableInfoPeptides(), displayColumns, MS2Manager.getDataRegionNamePeptides());
        if (form.isExportAsWebPage())
                tw.setExportAsWebPage(true);
        tw.setFilenamePrefix("MS2Runs");
        tw.setFileHeader(headers);   // Used for AMT file export
        tw.write(response);
    }

    protected List<String> getExportFormats()
    {
        List<String> result = new ArrayList<String>();
        result.add("Excel");
        result.add("TSV");
        result.add("DTA");
        result.add("PKL");
        result.add("AMT");
        return result;
    }
}
