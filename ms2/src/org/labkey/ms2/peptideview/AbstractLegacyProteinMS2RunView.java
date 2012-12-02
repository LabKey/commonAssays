/*
 * Copyright (c) 2007-2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2.peptideview;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.WebPartView;
import org.labkey.api.data.*;
import org.labkey.ms2.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractLegacyProteinMS2RunView extends AbstractMS2RunView<WebPartView>
{
    public AbstractLegacyProteinMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        return exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    protected abstract List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport);

    public ProteinTSVGridWriter getTSVProteinGridWriter(String requestedProteinColumnNames, String requestedPeptideColumnNames, boolean expanded)
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

        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows, _offset);
        rgn.setShowFilterDescription(false);

        if (selectSeqId && null == rgn.getDisplayColumn("SeqId"))
        {
            DisplayColumn seqId = MS2Manager.getTableInfoPeptides().getColumn("SeqId").getRenderer();
            seqId.setVisible(false);
            rgn.addDisplayColumn(seqId);
        }
        rgn.setTable(MS2Manager.getTableInfoPeptides());
        rgn.setName(MS2Manager.getDataRegionNamePeptides());
        rgn.setShowPagination(false);

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

    public ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        String where = createExtraWhere(selectedRows);

        boolean includeHeaders = form.getExportFormat().equals("Excel");

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

        ew.renderNewSheet();
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
            ew.renderCurrentSheet();
        }

        OutputStream outputStream = ExcelWriter.getOutputStream(response, "MS2Runs", ew.getDocumentType());

        ew.getWorkbook().write(outputStream);

        return null;
    }

    protected abstract String createExtraWhere(List<String> selectedRows);

    protected abstract void addGroupingFilterText(List<String> headers, ActionURL currentUrl, boolean handSelected);

    protected abstract void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException;

    public abstract AbstractProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException;
    
    protected List<MS2Controller.MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2Controller.MS2ExportType.values());
    }
}
