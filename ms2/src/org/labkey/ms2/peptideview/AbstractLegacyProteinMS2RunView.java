package org.labkey.ms2.peptideview;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.*;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.ms2.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.sql.SQLException;
import java.util.*;

import jxl.write.WritableWorkbook;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractLegacyProteinMS2RunView extends AbstractMS2RunView
{
    protected static Map<String, Class<? extends DisplayColumn>> _calculatedProteinColumns = new CaseInsensitiveHashMap<Class<? extends DisplayColumn>>();

    static
    {
        _calculatedProteinColumns.put("AACoverage", AACoverageColumn.class);
        _calculatedProteinColumns.put(TotalFilteredPeptidesColumn.NAME, TotalFilteredPeptidesColumn.class);
        _calculatedProteinColumns.put(UniqueFilteredPeptidesColumn.NAME, UniqueFilteredPeptidesColumn.class);
    }

    public AbstractLegacyProteinMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public void exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    protected abstract List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException;

    public ProteinTSVGridWriter getTSVProteinGridWriter(String requestedProteinColumnNames, String requestedPeptideColumnNames, boolean expanded) throws SQLException
    {
        List<DisplayColumn> proteinDisplayColumns = getProteinDisplayColumns(requestedProteinColumnNames, true);
        List<DisplayColumn> peptideDisplayColumns = null;

        if (expanded)
        {
            peptideDisplayColumns = getPeptideDisplayColumns(getPeptideColumnNames(requestedPeptideColumnNames));
            changePeptideCaptionsForTsv(peptideDisplayColumns);
        }

        ProteinTSVGridWriter tw = getTSVProteinGridWriter(proteinDisplayColumns, peptideDisplayColumns);
        tw.setExpanded(expanded);
        return tw;
    }

    public abstract ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns);

    protected DataRegion getNestedPeptideGrid(MS2Run run, String requestedPeptideColumnNames, boolean selectSeqId) throws SQLException
    {
        String columnNames = getPeptideColumnNames(requestedPeptideColumnNames);

        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows);

        if (selectSeqId && null == rgn.getDisplayColumn("SeqId"))
        {
            DisplayColumn seqId = MS2Manager.getTableInfoPeptides().getColumn("SeqId").getRenderer();
            seqId.setVisible(false);
            rgn.addColumn(seqId);
        }
        rgn.setTable(MS2Manager.getTableInfoPeptides());
        rgn.setName(MS2Manager.getDataRegionNamePeptides());

        rgn.setShowRecordSelectors(false);
        rgn.setFixedWidthColumns(true);

        ActionURL showUrl = _url.clone();
        String seqId = showUrl.getParameter("seqId");
        showUrl.deleteParameter("seqId");
        String extraParams = "";
        if (selectSeqId)
        {
            extraParams = null == seqId ? "seqId=${SeqId}" : "seqId=" + seqId;
        }
        if ("proteinprophet".equals(_url.getParameter("grouping")) && _url.getParameter("proteinGroupId") == null)
        {
            extraParams = "proteinGroupId=${proteinGroupId}";
        }
        setPeptideUrls(rgn, extraParams);

        ButtonBar bb = new ButtonBar();
        bb.setVisible(false);  // Don't show button bar on nested region; also ensures no nested <form>...</form>
        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }

    public void exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        String where = createExtraWhere(selectedRows);

        boolean includeHeaders = form.getExportFormat().equals("Excel");

        ServletOutputStream outputStream = ExcelWriter.getOutputStream(response, "MS2Runs");
        WritableWorkbook workbook = ExcelWriter.getWorkbook(outputStream);

        ActionURL currentUrl = _url.clone();

        AbstractProteinExcelWriter ew = getExcelProteinGridWriter(form.getProteinColumns());
        ew.setSheetName("MS2 Runs");
        List<MS2Run> runs = Arrays.asList(_runs);
        List<String> headers;

        if (includeHeaders)
        {
            headers = new ArrayList<String>();
            if (_runs.length == 1)
            {
                headers.addAll(getRunSummaryHeaders(_runs[0]));
            }
            addGroupingFilterText(headers, currentUrl, (selectedRows != null));
            addPeptideFilterText(headers, runs.get(0), currentUrl);

            headers.add("");
            ew.setHeaders(headers);
        }

        ew.renderNewSheet(workbook);
        ew.setHeaders(Collections.<String>emptyList());
        ew.setCaptionRowVisible(false);

        for (int i = 0; i < runs.size(); i++)
        {
            if (includeHeaders && runs.size() > 1)
            {
                headers = new ArrayList<String>();

                if (i > 0)
                    headers.add("");

                headers.add(runs.get(i).getDescription());
                ew.setHeaders(headers);
            }

            setUpExcelProteinGrid(ew, form.getExpanded(), form.getColumns(), runs.get(i), where);
            ew.renderCurrentSheet(workbook);
        }

        ExcelWriter.closeWorkbook(workbook, outputStream);
    }

    protected abstract String createExtraWhere(List<String> selectedRows);

    protected abstract void addGroupingFilterText(List<String> headers, ActionURL currentUrl, boolean handSelected);

    protected abstract void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException;

    public abstract AbstractProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException;
    
    public void writeExcel(HttpServletResponse response, MS2Run run, boolean expanded, String columns, String where, List<String> headers, String proteinColumns) throws SQLException
    {
        AbstractProteinExcelWriter ewProtein = getExcelProteinGridWriter(proteinColumns);
        ewProtein.setSheetName(run.getDescription());
        ewProtein.setFooter(run.getDescription() + " Proteins");
        ewProtein.setHeaders(headers);
        setUpExcelProteinGrid(ewProtein, expanded, columns, run, where);
        ewProtein.write(response);
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
