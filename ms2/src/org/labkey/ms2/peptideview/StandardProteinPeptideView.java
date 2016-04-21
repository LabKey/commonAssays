/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import com.google.common.collect.Iterables;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.ResultsImpl;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.Table;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.AACoverageColumn;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.SpectrumIterator;
import org.labkey.ms2.SpectrumRenderer;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class StandardProteinPeptideView extends AbstractLegacyProteinMS2RunView
{
    public StandardProteinPeptideView(ViewContext viewContext, MS2Run... runs)
    {
        super(viewContext, "NestedPeptides", runs);
    }

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean forExport)
    {
        DataRegion proteinRgn = createProteinDataRegion(expanded, requestedPeptideColumnNames, requestedProteinColumnNames);
        proteinRgn.setTable(MS2Manager.getTableInfoProteins());
        GridView proteinView = new GridView(proteinRgn, (BindException)null);
        proteinRgn.setShowPagination(false);
        proteinView.setResultSet(ProteinManager.getProteinRS(_url, getSingleRun(), null, proteinRgn.getMaxRows(), getUser()));
        proteinView.setContainer(getContainer());
        proteinView.setTitle("Proteins");
        return proteinView;
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport)
    {
        List<DisplayColumn> result = new ArrayList<>();

        FilteredTable table = new FilteredTable<>(MS2Manager.getTableInfoProteins(), new MS2Schema(getUser(), getContainer()));
        table.wrapAllColumns(true);

        ColumnInfo aaCoverageColumn = table.wrapColumn("AACoverage", table.getRealTable().getColumn("SeqId"));
        aaCoverageColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AACoverageColumn();
            }
        });
        table.addColumn(aaCoverageColumn);

        for (String columnName : new ProteinColumnNameList(requestedProteinColumnNames))
        {
            addColumn(columnName, result, table);
        }
        return result;
    }


    private StandardProteinDataRegion createProteinDataRegion(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames)
    {
        StandardProteinDataRegion proteinRgn = new StandardProteinDataRegion(getAJAXNestedGridURL());
        proteinRgn.setName(MS2Manager.getDataRegionNameProteins());
        proteinRgn.addDisplayColumns(getProteinDisplayColumns(requestedProteinColumnNames, false));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setMaxRows(_maxGroupingRows);
        proteinRgn.setOffset(_offset);
        proteinRgn.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);

        MS2Run run = getSingleRun();

        String columnNames = getPeptideColumnNames(requestedPeptideColumnNames);

        DataRegion peptideGrid = getNestedPeptideGrid(run, columnNames, true);
        proteinRgn.setNestedRegion(peptideGrid);
        GroupedResultSet peptideResultSet = createPeptideResultSet(columnNames, run, _maxGroupingRows, null);
        proteinRgn.setGroupedResultSet(peptideResultSet);

        ActionURL proteinUrl = _url.clone();
        proteinUrl.setAction(MS2Controller.ShowProteinAction.class);
        DisplayColumn proteinColumn = proteinRgn.getDisplayColumn("Protein");
        if (proteinColumn != null)
        {
            proteinColumn.setURLExpression(new DetailsURL(proteinUrl, Collections.singletonMap("seqId", "SeqId")));
            proteinColumn.setLinkTarget("prot");
        }

        ButtonBar bb = createButtonBar(MS2Controller.ExportAllProteinsAction.class, MS2Controller.ExportSelectedProteinsAction.class, "proteins", proteinRgn);
        proteinRgn.addHiddenFormField("queryString", _url.getRawQuery());
        //proteinRgn.addHiddenFormField("run", _url.getParameter("run"));
        //proteinRgn.addHiddenFormField("grouping", _url.getParameter("grouping"));

        proteinRgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return proteinRgn;
    }

    public GroupedResultSet createPeptideResultSet(String columnNames, MS2Run run, int maxRows, String extraWhere)
    {
        String sqlColumnNames = getPeptideSQLColumnNames(columnNames, run);
        return ProteinManager.getPeptideRS(_url, run, extraWhere, maxRows, sqlColumnNames, getUser());
    }

    public StandardProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames)
    {
        StandardProteinExcelWriter ew = new StandardProteinExcelWriter();
        ew.setDisplayColumns(getProteinDisplayColumns(requestedProteinColumnNames, true));
        return ew;
    }


    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        return new StandardProteinTSVGridWriter(proteinDisplayColumns, peptideDisplayColumns);
    }


    private String getPeptideSQLColumnNames(String peptideColumnNames, MS2Run run)
    {
        return run.getSQLPeptideColumnNames(peptideColumnNames + ", Protein, Peptide, RowId", true, MS2Manager.getTableInfoPeptides());
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where)
    {
        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumnNames);
        String sqlPeptideColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        ResultSet proteinRS = ProteinManager.getProteinRS(_url, run, where, ExcelWriter.MAX_ROWS_EXCEL_97, getUser());
        GroupedResultSet peptideRS = ProteinManager.getPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS_EXCEL_97, sqlPeptideColumnNames, getUser());
        DataRegion peptideRgn = getPeptideGrid(peptideColumnNames, Table.ALL_ROWS, 0);

        ewProtein.setResultSet(proteinRS);
        ewProtein.setGroupedResultSet(peptideRS);
        ExcelWriter ewPeptide = new ExcelWriter(new ResultsImpl(peptideRS), peptideRgn.getDisplayColumns(), ewProtein);
        if (expanded)
        {
            ExcelColumn ec = ewPeptide.getExcelColumn("Protein");
            if (null != ec)
                ec.setVisible(false);
        }
        ewProtein.setExcelWriter(ewPeptide);
        ewProtein.setExpanded(expanded);
        ewProtein.setAutoSize(false);
    }


    public void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumns, MS2Run run, String where)
    {
        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumns);
        String peptideSqlColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        ResultSet proteinRS = null;
        GroupedResultSet peptideRS = null;

        try
        {
            proteinRS = ProteinManager.getProteinRS(_url, run, where, Table.ALL_ROWS, getUser());
            peptideRS = ProteinManager.getPeptideRS(_url, run, where, Table.ALL_ROWS, peptideSqlColumnNames, getUser());

            TSVGridWriter twPeptide = new TSVGridWriter(new ResultsImpl(peptideRS), getPeptideDisplayColumns(peptideColumnNames))
            {
                @Override
                protected Iterable<String> getValues(RenderContext ctx, Iterable<DisplayColumn> displayColumns)
                {
                    Iterable<String> proteinRow = (Iterable<String>)ctx.get("ProteinRow");
                    return Iterables.concat(proteinRow, super.getValues(ctx, displayColumns));
                }
            };

            // TODO: Get rid of duplicate columns (e.g., Protein)?

            twPeptide.setPrintWriter(tw.getPrintWriter());

            // TODO: Consider getting rid of tw.setResultSet(), pass back resultset to controller
            tw.setGroupedResultSet(peptideRS);
            tw.setTSVGridWriter(twPeptide);
            tw.writeResultSet(new ResultsImpl(proteinRS));
        }
        finally
        {
            if (proteinRS != null) try { proteinRS.close(); } catch (SQLException ignored) {}
            if (peptideRS != null) try { peptideRS.close(); } catch (SQLException ignored) {}
        }
    }


    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<>("Peptide Filter", peptideFilter.getFilterText()));
        sqlSummaries.add(new Pair<>("Peptide Sort", new Sort(_url, MS2Manager.getDataRegionNamePeptides()).getSortText()));

        sqlSummaries.add(new Pair<>("Protein Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteins()).getFilterText()));
        sqlSummaries.add(new Pair<>("Protein Sort", new Sort(_url, MS2Manager.getDataRegionNameProteins()).getSortText()));
    }

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT sSeqId AS SeqId FROM ( ");
        ProteinManager.addProteinQuery(fragment, run, queryUrl, null, Table.ALL_ROWS, false, getUser());
        fragment.append(" ) seqids");
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<>();
        map.put("Peptide filter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), run));
        map.put("Protein filter", ProteinManager.getProteinFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, null, getUser(), run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        String peptideColumns = getPeptideColumnNames(columns);
        DataRegion peptideRegion = getNestedPeptideGrid(_runs[0], peptideColumns, true);
        String extraWhere = MS2Manager.getTableInfoPeptides() + ".Protein= '" + proteinGroupingId + "'";
        final GroupedResultSet groupedResultSet = createPeptideResultSet(peptideColumns, _runs[0], _maxGroupingRows, extraWhere);
        GridView view = new GridView(peptideRegion, (BindException)null)
        {
            @Override
            public void renderView(RenderContext model, PrintWriter out) throws IOException
            {
                super.renderView(model, out);
                try
                {
                    // Close the outer result set after we're done with it
                    groupedResultSet.close();
                }
                catch (SQLException ignored) {}
            }
        };
        view.setResultSet(groupedResultSet.getNextResultSet());
        return view;
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException
    {
        String where = createExtraWhere(selectedRows);

        String columnNames = getPeptideColumnNames(form.getColumns());
        List<DisplayColumn> displayColumns = getPeptideDisplayColumns(columnNames);
        changePeptideCaptionsForTsv(displayColumns);

        try (ProteinTSVGridWriter tw = getTSVProteinGridWriter(form.getProteinColumns(), form.getColumns(), form.getExpanded()))
        {
            tw.prepare(response);
            tw.setFileHeader(headers);
            tw.setFilenamePrefix("MS2Runs");
            tw.writeFileHeader();
            tw.writeColumnHeaders();

            for (MS2Run run : _runs)
                exportTSVProteinGrid(tw, form.getColumns(), run, where);
        }
        return null;
    }

    @Override
    public void exportSpectra(MS2Controller.ExportForm form, ActionURL currentURL, SpectrumRenderer spectrumRenderer, List<String> exportRows) throws IOException, RunListException
    {
        List<MS2Run> runs = form.validateRuns();
        String where = createExtraWhere(exportRows);

        try (SpectrumIterator iter = new ProteinResultSetSpectrumIterator(runs, currentURL, this, where, form.getViewContext().getUser()))
        {
            spectrumRenderer.render(iter);
            spectrumRenderer.close();
        }
    }

    protected String createExtraWhere(List<String> selectedRows)
    {
        String where = null;
        if (selectedRows != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Protein IN (");
            String separator = "";
            for (String row : selectedRows)
            {
                sb.append(separator);
                separator = ", ";
                sb.append("'");
                sb.append(row.replace("'", "''"));
                sb.append("'");
            }
            sb.append(")");
            where = sb.toString();
        }
        return where;
    }

    protected void addGroupingFilterText(List<String> headers, ActionURL currentUrl, boolean handSelected)
    {
        headers.add((_runs.length > 1 ? "Multiple runs" : "One run") + " showing " + (handSelected ? "hand selected" : "all") + " proteins matching the following query:");
        headers.add("Protein Filter: " + new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText());
        headers.add("Protein Sort: " + new Sort(currentUrl, MS2Manager.getDataRegionNameProteins()).getSortText());
    }
}
