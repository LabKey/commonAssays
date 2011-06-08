/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;

import org.labkey.ms2.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetPeptideView extends AbstractLegacyProteinMS2RunView
{
    private GroupedResultSet _rs;

    public ProteinProphetPeptideView(ViewContext viewContext, MS2Run... runs)
    {
        super(viewContext, "NestedPeptides", runs);
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException
    {
        ProteinColumnNameList originalNames = new ProteinColumnNameList(requestedProteinColumnNames);
        List<DisplayColumn> displayColumns = new ArrayList<DisplayColumn>();
        ProteinGroupProteins proteins = new ProteinGroupProteins(Arrays.asList(_runs));
        Set<String> sequenceColumnNames = new CaseInsensitiveHashSet(ProteinListDisplayColumn.SEQUENCE_COLUMN_NAMES);

        for (String name : originalNames)
        {
            if (name.equalsIgnoreCase("GroupNumber"))
            {
                displayColumns.add(new GroupNumberDisplayColumn(MS2Manager.getTableInfoProteinGroupsWithQuantitation().getColumn("GroupNumber"), _url, "GroupNumber", "IndistinguishableCollectionId"));
            }
            else if (name.equalsIgnoreCase("AACoverage"))
            {
                addColumn(_calculatedProteinColumns, "PercentCoverage", displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
            else if (name.equalsIgnoreCase(TotalFilteredPeptidesColumn.NAME))
            {
                addColumn(_calculatedProteinColumns, TotalFilteredPeptidesColumn.NAME, displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
            else if (name.equalsIgnoreCase(UniqueFilteredPeptidesColumn.NAME))
            {
                addColumn(_calculatedProteinColumns, UniqueFilteredPeptidesColumn.NAME, displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
            else if (name.equalsIgnoreCase("Peptides"))
            {
                addColumn(_calculatedProteinColumns, "TotalNumberPeptides", displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
            else if (name.equalsIgnoreCase("UniquePeptides"))
            {
                addColumn(_calculatedProteinColumns, "UniquePeptidesCount", displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
            else if (name.equalsIgnoreCase("FirstProtein"))
            {
                displayColumns.add(new FirstProteinDisplayColumn("First Protein", FirstProteinDisplayColumn.FirstProteinType.NAME, proteins));
            }
            else if (name.equalsIgnoreCase("FirstDescription"))
            {
                displayColumns.add(new FirstProteinDisplayColumn("First Description", FirstProteinDisplayColumn.FirstProteinType.DESCRIPTION, proteins));
            }
            else if (name.equalsIgnoreCase("FirstBestName"))
            {
                displayColumns.add(new FirstProteinDisplayColumn("First Best Name", FirstProteinDisplayColumn.FirstProteinType.BEST_NAME, proteins));
            }
            else if (name.equalsIgnoreCase("FirstBestGeneName"))
            {
                displayColumns.add(new FirstProteinDisplayColumn("First Best Gene Name", FirstProteinDisplayColumn.FirstProteinType.BEST_GENE_NAME, proteins));
            }
            else if (sequenceColumnNames.contains(name))
            {
                displayColumns.add(new ProteinListDisplayColumn(name, proteins));
            }
            else
            {
                addColumn(_calculatedProteinColumns, name, displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
        }

        return displayColumns;
    }

    public List<DisplayColumn> getPeptideDisplayColumns(String peptideColumnNames) throws SQLException
    {
        return getColumns(_calculatedPeptideColumns, new PeptideColumnNameList(peptideColumnNames), MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoPeptideMemberships());
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
    {
        String where = createExtraWhere(selectedRows);

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
            sb.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            sb.append(".RowId IN (");
            String separator = "";
            for (String protein : selectedRows)
            {
                sb.append(separator);
                separator = ", ";
                sb.append(new Long(protein));
            }
            sb.append(")");
            where = sb.toString();
        }
        return where;
    }

    private ProteinProphetDataRegion createProteinDataRegion(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws SQLException
    {
        if (expanded)
        {
            _maxPeptideRows = 15000;
            _maxGroupingRows = 250;
        }
        else
        {
            _maxPeptideRows = 75000;
            _maxGroupingRows = 1000;
        }
        ProteinProphetDataRegion proteinRgn = new ProteinProphetDataRegion(_url);
        proteinRgn.setTable(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        proteinRgn.setName(MS2Manager.getDataRegionNameProteinGroups());
        proteinRgn.addDisplayColumns(getProteinDisplayColumns(requestedProteinColumnNames, false));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTH);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setShowPagination(false);
        proteinRgn.setRecordSelectorValueColumns("ProteinGroupId");
        proteinRgn.setMaxRows(_maxPeptideRows);
        proteinRgn.setOffset(_offset);

        ResultSet rs = createPeptideResultSet(requestedPeptideColumnNames, proteinRgn.getMaxRows(), null);
        _rs = new GroupedResultSet(rs, "ProteinGroupId", _maxPeptideRows, _maxGroupingRows);

        proteinRgn.setGroupedResultSet(_rs);
        DataRegion peptideGrid = createPeptideDataRegion(requestedPeptideColumnNames);
        proteinRgn.setNestedRegion(peptideGrid);

        ButtonBar bb = createButtonBar(MS2Controller.ExportProteinGroupsAction.class, MS2Controller.ExportSelectedProteinGroupsAction.class, "protein groups", proteinRgn);

        proteinRgn.addHiddenFormField("queryString", _url.getRawQuery());
        //proteinRgn.addHiddenFormField("run", _url.getParameter("run"));
        //proteinRgn.addHiddenFormField("grouping", _url.getParameter("grouping"));

        proteinRgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return proteinRgn;
    }

    private DataRegion createPeptideDataRegion(String requestedPeptideColumnNames)
            throws SQLException
    {
        DataRegion peptideGrid = getNestedPeptideGrid(getSingleRun(), requestedPeptideColumnNames, false);
        peptideGrid.removeColumns("Protein", "Description", "GeneName", "SeqId");
        return peptideGrid;
    }

    public ResultSet createPeptideResultSet(String requestedPeptideColumnNames, int maxRows, String extraWhere) throws SQLException
    {
        MS2Run run = getSingleRun();
        String sqlColumnNames = getPeptideSQLColumnNames(requestedPeptideColumnNames, run);
        return ProteinManager.getProteinProphetPeptideRS(_url, run, extraWhere, maxRows, sqlColumnNames, getUser());
    }

    private String getPeptideSQLColumnNames(String peptideColumnNames, MS2Run run)
    {
        return run.getSQLPeptideColumnNames(peptideColumnNames + ", Peptide", false, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoPeptideMemberships());
    }

    // Need to add proteinGroupId to signature
    protected String getIndexLookup(ActionURL url)
    {
        return super.getIndexLookup(url) + "|GroupId=" + url.getParameter("proteinGroupId");
    }

    // Default behavior doesn't work for ProteinProphet groupings -- need to add in GroupId by joining to PeptideMemberships table
    protected Long[] generatePeptideIndex(ActionURL url) throws SQLException
    {
        String extraWhere = null;
        String groupIdString = url.getParameter("proteinGroupId");
        if (groupIdString != null)
        {
            try
            {
                extraWhere = MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId = " + Integer.parseInt(groupIdString);
            }
            catch (NumberFormatException e)
            {
                throw new NotFoundException("Invalid protein group id specified - " + groupIdString);
            }
        }
        else
        {
            String run = url.getParameter("run");
            String groupNumber = url.getParameter("groupNumber");
            String indistinguishableCollectionId = url.getParameter("indistinguishableCollectionId");
            if (run != null && groupNumber != null && indistinguishableCollectionId != null)
            {
                try
                {
                    extraWhere = MS2Manager.getTableInfoSimplePeptides() + ".Run = " + Integer.parseInt(run) + " AND " +
                            MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber = " + Integer.parseInt(groupNumber) + " AND " +
                            MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId = " + Integer.parseInt(indistinguishableCollectionId);
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Invalid run/groupNumber/indistinguishableCollectionId specified - " + run + "/" + groupNumber + "/" + indistinguishableCollectionId);
                }
            }
        }
        if (extraWhere == null)
        {
            throw new NotFoundException("No protein group specified");
        }
        ResultSet rs = null;
        try
        {
            rs = createPeptideResultSet("RowId", _maxPeptideRows, extraWhere);
            int columnIndex = rs.findColumn("RowId");
            ArrayList<Long> rowIdsLong = new ArrayList<Long>(100);
            while(rs.next())
                rowIdsLong.add(rs.getLong(columnIndex));

            return rowIdsLong.toArray(new Long[rowIdsLong.size()]);
        }
        finally
        {
            if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
        }
    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        GridView peptidesGridView;
        DataRegion rgn = getNestedPeptideGrid(getSingleRun(), form.getColumns(), false);

        peptidesGridView = new GridView(rgn, (BindException)null);

        String columnNames = getPeptideColumnNames(form.getColumns());
        String sqlColumnNames = getPeptideSQLColumnNames(columnNames, getSingleRun());
        String extraWhere;
        if (form.getGroupNumber() != 0)
        {
            extraWhere = MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber = " + form.getGroupNumber() + " AND " +
                MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId = " + form.getIndistinguishableCollectionId();
            peptidesGridView.setResultSet(ProteinManager.getProteinProphetPeptideRS(_url, getSingleRun(), extraWhere, 250, sqlColumnNames, getUser()));
        }
        return peptidesGridView;
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        SimpleFilter coverageFilter = null;

        if (form.getSeqIdInt() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), getSingleRun());
            // Can't use addCondition below because it's too dumb to handle alias.columnName
            coverageFilter.addWhereClause("pgm.SeqId = ?", new Object[]{form.getSeqIdInt()}, "SeqId");
        }
        else if (form.getGroupNumber() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), getSingleRun());
            // Can't use addCondition below because it's too dumb to handle alias.columnName
            coverageFilter.addWhereClause("pg.GroupNumber = ?", new Object[]{form.getGroupNumber()}, "GroupNumber");
            coverageFilter.addWhereClause("pg.IndistinguishableCollectionId = ?", new Object[]{form.getIndistinguishableCollectionId()}, "IndistinguishableCollectionId");
        }

        if (coverageFilter != null)
        {
            coverageFilter = ProteinManager.reduceToValidColumns(coverageFilter, MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoProteinGroups(), MS2Manager.getTableInfoPeptideMemberships(), MS2Manager.getTableInfoProteinProphetFiles());
            String sql = "SELECT Peptide FROM " + MS2Manager.getTableInfoPeptides() + " p, " +
                    MS2Manager.getTableInfoPeptideMemberships() + " pm, " +
                    MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " +
                    MS2Manager.getTableInfoProteinGroups() + " pg, " +
                    "(SELECT Run as PPRun, RowId FROM " + MS2Manager.getTableInfoProteinProphetFiles() + ") ppf " +
                    coverageFilter.getWhereSQL(ProteinManager.getSqlDialect()) +
                    " AND pg.RowId = pgm.ProteinGroupId" +
                    " AND pm.PeptideId = p.RowId" +
                    " AND pm.ProteinGroupId = pgm.ProteinGroupId" +
                    " AND ppf.PPRun = p.Run" +
                    " AND ppf.RowId = pg.ProteinProphetFileId";
            return Table.executeArray(ProteinManager.getSchema(), sql, coverageFilter.getWhereParams(MS2Manager.getTableInfoPeptides()).toArray(), String.class);
        }

        return null;
    }

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean forExport) throws ServletException, SQLException
    {
        DataRegion proteinRgn = createProteinDataRegion(expanded, requestedPeptideColumnNames, requestedProteinColumnNames);
        GridView proteinView = new GridView(proteinRgn, (BindException)null);
        ResultSet rs;
        if (expanded)
        {
            rs = _rs;
        }
        else
        {
            rs = new ResultSetCollapser(_rs, "ProteinGroupId", _maxPeptideRows);
        }
        proteinView.setResultSet(rs);

        proteinView.setContainer(getContainer());
        proteinView.setTitle("Protein Prophet Groups");
        return proteinView;
    }

    public ProteinProphetExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
    {
        ProteinProphetExcelWriter ew = new ProteinProphetExcelWriter();
        ew.setDisplayColumns(getProteinDisplayColumns(requestedProteinColumnNames, true));
        return ew;
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        return new ProteinProphetTSVGridWriter(proteinDisplayColumns, peptideDisplayColumns);
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        ResultSet proteinRS = ProteinManager.getProteinProphetRS(_url, run, where, 0, getUser());

        ewProtein.setResultSet(proteinRS);
        ewProtein.setExpanded(expanded);
        ewProtein.setAutoSize(false);

        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumnNames);
        String sqlPeptideColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS, sqlPeptideColumnNames, getUser()), "ProteinGroupId");
        ewProtein.setGroupedResultSet(peptideRS);

        if (expanded)
        {
            DataRegion peptideRgn = getPeptideGrid(peptideColumnNames, 0, 0);
            ExcelWriter ewPeptide = new ExcelWriter(new ResultsImpl(peptideRS), peptideRgn.getDisplayColumns());
            ExcelColumn ec = ewPeptide.getExcelColumn("Protein");
            if (null != ec)
                ec.setVisible(false);
            ewProtein.setExcelWriter(ewPeptide);
        }
    }

    // TODO: Put in base class?
    public void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumns, MS2Run run, String where) throws SQLException
    {
        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumns);
        String peptideSqlColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);
        GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, 0, peptideSqlColumnNames, getUser()), "ProteinGroupId");
        tw.setGroupedResultSet(peptideRS);
        if (tw.getExpanded())
        {
            TSVGridWriter twPeptide = new TSVGridWriter(new ResultsImpl(peptideRS), getPeptideDisplayColumns(peptideColumnNames))
            {
                protected StringBuilder getRow(RenderContext ctx, List<DisplayColumn> displayColumns)
                {
                    StringBuilder row = super.getRow(ctx, displayColumns);
                    row.insert(0, (StringBuilder)ctx.get("ProteinRow"));
                    return row;
                }
            };

            twPeptide.setPrintWriter(tw.getPrintWriter());
            tw.setTSVGridWriter(twPeptide);
        }

        ResultSet proteinRS = null;
        try
        {
            proteinRS = ProteinManager.getProteinProphetRS(_url, run, where, 0, getUser());
            tw.writeResultSet(new ResultsImpl(proteinRS));
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

        sqlSummaries.add(new Pair<String, String>("Protein Group Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteinGroups()).getFilterText()));
        sqlSummaries.add(new Pair<String, String>("Protein Group Sort", new Sort(_url, MS2Manager.getDataRegionNameProteinGroups()).getSortText()));
    }

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT PGM.SeqId FROM ");
        fragment.append(MS2Manager.getTableInfoProteinGroupMemberships() + " PGM, ( ");
        fragment.append(ProteinManager.getProteinProphetPeptideSql(queryUrl, run, null, -1, "ms2.ProteinGroupsWithQuantitation.ProteinGroupId AS GroupId", false, getUser()));
        fragment.append(" ) x WHERE x.ProteinGroupId = PGM.ProteinGroupId");
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<String, SimpleFilter>();
        map.put("Peptide filter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), run));
        map.put("Protein group filter", ProteinManager.getProteinGroupFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, null, getUser(), run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String requestedPeptideColumns)
            throws SQLException
    {
        String peptideColumns = getPeptideColumnNames(requestedPeptideColumns);
        DataRegion peptideRegion = createPeptideDataRegion(requestedPeptideColumns);
        GridView view = new GridView(peptideRegion, (BindException)null);
        String extraWhere = MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId = " + proteinGroupingId;
        view.setResultSet(createPeptideResultSet(peptideColumns, _maxPeptideRows, extraWhere));
        return view;
    }

    protected void addGroupingFilterText(List<String> headers, ActionURL currentUrl, boolean handSelected)
    {
        headers.add((_runs.length > 1 ? "Multiple runs" : "One run") + " showing " + (handSelected ? "hand selected" : "all") + " protein groups matching the following query:");
        headers.add("Protein Group Filter: " + new SimpleFilter(currentUrl, MS2Manager.getTableInfoProteinGroupsWithQuantitation().getName()).getFilterText());
        headers.add("Protein Group Sort: " + new Sort(currentUrl, MS2Manager.getTableInfoProteinGroupsWithQuantitation().getName()).getSortText());
    }
}
