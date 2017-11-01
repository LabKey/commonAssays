/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.SpectrumIterator;
import org.labkey.ms2.SpectrumRenderer;
import org.labkey.ms2.protein.ProteinManager;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class FlatPeptideView extends AbstractMS2RunView<WebPartView>
{
    public FlatPeptideView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, "Peptides", runs);
    }

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean forExport)
    {
        DataRegion rgn = getPeptideGridForDisplay(requestedPeptideColumnNames);
        GridView peptideView = new GridView(rgn, (BindException)null);
        peptideView.setFilter(ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER, getUser(), getSingleRun()));
        peptideView.setSort(ProteinManager.getPeptideBaseSort());
        peptideView.setTitle("Peptides");
        return peptideView;
    }


    public ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        return exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    public ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        List<MS2Run> runs = Arrays.asList(_runs);
        SimpleFilter filter = createFilter(selectedRows);

        boolean includeHeaders = form.getExportFormat().equals("Excel");

        ActionURL currentUrl = _url.clone();

        ExcelWriter ew = new ExcelWriter(ExcelWriter.ExcelDocumentType.xls);
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
                headers = new ArrayList<>();
                headers.add("Multiple runs showing " + (selectedRows == null ? "all" : "hand selected") + " peptides matching the following query:");
                addPeptideFilterText(headers, run, currentUrl);  // TODO: Version that takes runs[]
            }
            headers.add("");
            ew.setHeaders(headers);
        }

        // Always include column captions at the top
        ew.setDisplayColumns(getPeptideDisplayColumns(getPeptideColumnNames(form.getColumns())));
        ew.renderNewSheet();
        ew.setCaptionRowVisible(false);

        // TODO: Footer?

        for (int i = 0; i < runs.size(); i++)
        {
            if (includeHeaders)
            {
                headers = new ArrayList<>();

                if (runs.size() > 1)
                {
                    if (i > 0)
                        headers.add("");

                    headers.add(runs.get(i).getDescription());
                }

                ew.setHeaders(headers);
            }

            setupExcelPeptideGrid(ew, filter, form.getColumns(), runs.get(i));
            ew.renderCurrentSheet();
        }

        ew.write(response, "MS2Runs");
        return null;
    }

    private SimpleFilter createFilter(List<String> selectedRows)
    {
        SimpleFilter filter = ProteinManager.getPeptideFilter(_url, Arrays.asList(_runs), ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser());

        if (selectedRows != null)
        {
            List<Long> peptideIds = new ArrayList<>(selectedRows.size());

            // Technically, should only limit this in Excel export case... but there's no way to individually select 65K peptides
            for (int i = 0; i < Math.min(selectedRows.size(), ExcelWriter.MAX_ROWS_EXCEL_97); i++)
            {
                String[] row = selectedRows.get(i).split(",");
                peptideIds.add(Long.parseLong(row[row.length == 1 ? 0 : 1]));
            }

            filter.addInClause(FieldKey.fromParts("RowId"), peptideIds);
        }
        return filter;
    }

    private void setupExcelPeptideGrid(ExcelWriter ew, SimpleFilter filter, String requestedPeptideColumns, MS2Run run) throws IOException
    {
        String columnNames = getPeptideColumnNames(requestedPeptideColumns);
        DataRegion rgn = getPeptideGrid(columnNames, ExcelWriter.MAX_ROWS_EXCEL_97, 0);
        Container c = getContainer();
        ProteinManager.replaceRunCondition(filter, null, run);

        RenderContext ctx = new RenderContext(_viewContext);
        ctx.setContainer(c);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        try
        {
            ew.setResultSet(rgn.getResultSet(ctx));
            ew.setDisplayColumns(rgn.getDisplayColumns());
            ew.setAutoSize(true);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private DataRegion getPeptideGridForDisplay(String columnNames)
    {
        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows, _offset);
        rgn.setShowRecordSelectors(true);

        setPeptideUrls(rgn, null);

        ButtonBar bb = createButtonBar(MS2Controller.ExportAllPeptidesAction.class, MS2Controller.ExportSelectedPeptidesAction.class, "peptides", rgn);

        rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
        rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);
        return rgn;
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<>("Peptide Filter", peptideFilter.getFilterText()));
        sqlSummaries.add(new Pair<>("Peptide Sort", new Sort(_url, MS2Manager.getDataRegionNamePeptides()).getSortText()));
    }

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT SeqId FROM ");
        fragment.append(MS2Manager.getTableInfoPeptides());
        fragment.append(" ");
        SimpleFilter filter = ProteinManager.getPeptideFilter(queryUrl, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), run);
        fragment.append(filter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
        fragment.addAll(filter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<>();
        map.put("Peptide filter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException
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

        TableInfo tableInfo = null;
        for (DisplayColumn displayColumn : displayColumns)
        {
            ColumnInfo columnInfo = displayColumn.getColumnInfo();
            if (columnInfo != null && columnInfo.getParentTable() != null)
            {
                tableInfo = columnInfo.getParentTable();
                break;
            }
        }

        TSVGridWriter tw = new TSVGridWriter(ctx, tableInfo, displayColumns, MS2Manager.getDataRegionNamePeptides());
        tw.setFilenamePrefix("MS2Runs");
        tw.setFileHeader(headers);   // Used for AMT file export
        tw.write(response);
        return null;
    }

    protected List<MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2ExportType.values());
    }

    @Override
    public void exportSpectra(MS2Controller.ExportForm form, ActionURL currentURL, SpectrumRenderer spectrumRenderer, List<String> exportRows) throws IOException, RunListException
    {
        List<MS2Run> runs = form.validateRuns();
        SimpleFilter baseFilter = new SimpleFilter();
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentURL, runs, ProteinManager.URL_FILTER, form.getViewContext().getUser()));
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.addURLSort(currentURL, MS2Manager.getDataRegionNamePeptides());
        try (SpectrumIterator iter = new ResultSetSpectrumIterator(runs, baseFilter, sort))
        {
            spectrumRenderer.render(iter);
            spectrumRenderer.close();
        }
    }
}
