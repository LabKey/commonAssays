/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.labkey.ms2.MS2Controller;
import org.springframework.web.servlet.ModelAndView;

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

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean forExport) throws ServletException, SQLException
    {
        DataRegion proteinRgn = createProteinDataRegion(expanded, requestedPeptideColumnNames, requestedProteinColumnNames);
        proteinRgn.setTable(MS2Manager.getTableInfoProteins());
        GridView proteinView = new GridView(proteinRgn);
        proteinRgn.setShowPagination(false);
        proteinView.setResultSet(ProteinManager.getProteinRS(_url, getSingleRun(), null, proteinRgn.getMaxRows(), proteinRgn.getOffset()));
        proteinView.setContainer(getContainer());
        proteinView.setTitle("Proteins");
        return proteinView;
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException
    {
        List<DisplayColumn> result = new ArrayList<DisplayColumn>();

        for (String columnName : new ProteinColumnNameList(requestedProteinColumnNames))
        {
            addColumn(_calculatedProteinColumns, columnName, result, MS2Manager.getTableInfoProteins());
        }
        return result;
    }


    private StandardProteinDataRegion createProteinDataRegion(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws SQLException
    {
        StandardProteinDataRegion proteinRgn = new StandardProteinDataRegion(_url);
        proteinRgn.setName(MS2Manager.getDataRegionNameProteins());
        proteinRgn.addDisplayColumns(getProteinDisplayColumns(requestedProteinColumnNames, false));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setMaxRows(_maxGroupingRows);
        proteinRgn.setOffset(_offset);
        proteinRgn.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTH);

        MS2Run run = getSingleRun();

        String columnNames = getPeptideColumnNames(requestedPeptideColumnNames);

        DataRegion peptideGrid = getNestedPeptideGrid(run, columnNames, true);
        proteinRgn.setNestedRegion(peptideGrid);
        GroupedResultSet peptideResultSet = createPeptideResultSet(columnNames, run, _maxGroupingRows, _offset, null);
        proteinRgn.setGroupedResultSet(peptideResultSet);

        ActionURL proteinUrl = _url.clone();
        proteinUrl.setAction("showProtein");
        DisplayColumn proteinColumn = proteinRgn.getDisplayColumn("Protein");
        if (proteinColumn != null)
        {
            proteinColumn.setURL(proteinUrl.getLocalURIString() + "&seqId=${SeqId}");
            proteinColumn.setLinkTarget("prot");
        }

        ButtonBar bb = createButtonBar("exportAllProteins", "exportSelectedProteins", "proteins", proteinRgn);
        proteinRgn.addHiddenFormField("queryString", _url.getRawQuery());
        //proteinRgn.addHiddenFormField("run", _url.getParameter("run"));
        //proteinRgn.addHiddenFormField("grouping", _url.getParameter("grouping"));

        proteinRgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return proteinRgn;
    }

    public GroupedResultSet createPeptideResultSet(String columnNames, MS2Run run, int maxRows, long offset, String extraWhere) throws SQLException
    {
        String sqlColumnNames = getPeptideSQLColumnNames(columnNames, run);
        return ProteinManager.getPeptideRS(_url, run, extraWhere, maxRows, offset, sqlColumnNames);
    }

    public StandardProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
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

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumnNames);
        String sqlPeptideColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        ResultSet proteinRS = ProteinManager.getProteinRS(_url, run, where, ExcelWriter.MAX_ROWS, 0);
        GroupedResultSet peptideRS = ProteinManager.getPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS, 0, sqlPeptideColumnNames);
        DataRegion peptideRgn = getPeptideGrid(peptideColumnNames, 0, 0);

        ewProtein.setResultSet(proteinRS);
        ewProtein.setGroupedResultSet(peptideRS);
        ExcelWriter ewPeptide = new ExcelWriter(peptideRS, peptideRgn.getDisplayColumns());
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


    public void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumns, MS2Run run, String where) throws SQLException
    {
        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumns);
        String peptideSqlColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        ResultSet proteinRS = null;
        GroupedResultSet peptideRS = null;

        try
        {
            proteinRS = ProteinManager.getProteinRS(_url, run, where, 0, 0);
            peptideRS = ProteinManager.getPeptideRS(_url, run, where, 0, 0, peptideSqlColumnNames);

            TSVGridWriter twPeptide = new TSVGridWriter(peptideRS, getPeptideDisplayColumns(peptideColumnNames))
            {
                protected StringBuilder getRow(RenderContext ctx, List<DisplayColumn> displayColumns)
                {
                    StringBuilder row = super.getRow(ctx, displayColumns);
                    row.insert(0, (StringBuilder)ctx.get("ProteinRow"));
                    return row;
                }
            };

            // TODO: Get rid of duplicate columns (e.g., Protein)?

            twPeptide.setPrintWriter(tw.getPrintWriter());

            // TODO: Consider getting rid of tw.setResultSet(), pass back resultset to controller
            tw.setGroupedResultSet(peptideRS);
            tw.setTSVGridWriter(twPeptide);
            tw.writeResultSet(proteinRS);
        }
        finally
        {
            if (proteinRS != null) try { proteinRS.close(); } catch (SQLException e) {}
            if (peptideRS != null) try { peptideRS.close(); } catch (SQLException e) {}
        }
    }


    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilter.getFilterText()));
        sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(_url, MS2Manager.getDataRegionNamePeptides()).getSortText()));

        sqlSummaries.add(new Pair<String, String>("Protein Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteins()).getFilterText()));
        sqlSummaries.add(new Pair<String, String>("Protein Sort", new Sort(_url, MS2Manager.getDataRegionNameProteins()).getSortText()));
    }

    public MS2RunViewType getViewType()
    {
        return MS2RunViewType.PROTEIN;
    }

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT sSeqId AS SeqId FROM ( ");
        ProteinManager.addProteinQuery(fragment, run, queryUrl, null, 0, 0, false);
        fragment.append(" ) seqids");
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<String, SimpleFilter>();
        map.put("peptideFilter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run));
        map.put("proteinFilter", ProteinManager.getProteinFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, null, run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
    {
        String peptideColumns = getPeptideColumnNames(columns);
        DataRegion peptideRegion = getNestedPeptideGrid(_runs[0], peptideColumns, true);
        GridView view = new GridView(peptideRegion);
        String extraWhere = MS2Manager.getTableInfoPeptides() + ".Protein= '" + proteinGroupingId + "'";
        GroupedResultSet groupedResultSet = createPeptideResultSet(peptideColumns, _runs[0], _maxGroupingRows, _offset, extraWhere);
        // Shouldn't really close it here, but this prevents us from getting errors logged about not closing the result set
        // The problem is that nobody cares about the outer GroupedResultSet - we only care about the inner ResultSet,
        // and there's nobody to close the outer when we're done. Since the ResultSet continues to work after it's closed,
        // this is good enough for now.
        groupedResultSet.close();
        view.setResultSet(groupedResultSet.getNextResultSet());
        return view;
    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        DataRegion rgn = getNestedPeptideGrid(getSingleRun(), form.getColumns(), true);
        GridView gridView = new GridView(rgn);
        SimpleFilter gridFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER + ProteinManager.PROTEIN_FILTER, getSingleRun());
        gridView.setFilter(gridFilter);
        gridView.setSort(ProteinManager.getPeptideBaseSort());
        return gridView;
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        SimpleFilter coverageFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.ALL_FILTERS, getSingleRun());
        SimpleFilter validFilter = ProteinManager.reduceToValidColumns(coverageFilter, MS2Manager.getTableInfoPeptides());
        return Table.executeArray(ProteinManager.getSchema(), "SELECT Peptide FROM " + MS2Manager.getTableInfoPeptides() + " " + validFilter.getWhereSQL(ProteinManager.getSqlDialect()), validFilter.getWhereParams(MS2Manager.getTableInfoPeptides()).toArray(), String.class);
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
    {
        String where = createExtraWhere(selectedRows);

        String columnNames = getPeptideColumnNames(form.getColumns());
        List<DisplayColumn> displayColumns = getPeptideDisplayColumns(columnNames);
        changePeptideCaptionsForTsv(displayColumns);

        ProteinTSVGridWriter tw = getTSVProteinGridWriter(form.getProteinColumns(), form.getColumns(), form.getExpanded());
        if (form.isExportAsWebPage())
                tw.setExportAsWebPage(true);
        tw.prepare(response);
        tw.setFileHeader(headers);
        tw.setFilenamePrefix("MS2Runs");
        tw.writeFileHeader();
        tw.writeColumnHeaders();

        for (MS2Run run : _runs)
            exportTSVProteinGrid(tw, form.getColumns(), run, where);

        tw.close();
        return null;
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
