package org.labkey.ms2.peptideview;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.common.util.Pair;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;

import org.labkey.ms2.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetPeptideView extends AbstractLegacyProteinMS2RunView
{
    private Table.TableResultSet _rs;

    public ProteinProphetPeptideView(ViewContext viewContext, MS2Run... runs)
    {
        super(viewContext, "NestedPeptides", runs);
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException
    {
        ProteinColumnNameList originalNames = new ProteinColumnNameList(requestedProteinColumnNames);
        List<DisplayColumn> displayColumns = new ArrayList<DisplayColumn>();
        ProteinGroupProteins proteins = new ProteinGroupProteins(_runs);
        Set<String> sequenceColumnNames = new CaseInsensitiveHashSet(ProteinListDisplayColumn.ALL_SEQUENCE_COLUMNS);

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

    public void exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
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
        proteinRgn.addColumns(getProteinDisplayColumns(requestedProteinColumnNames, false));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setRecordSelectorValueColumns("ProteinGroupId");
        proteinRgn.setMaxRows(_maxPeptideRows);

        ResultSet rs = createPeptideResultSet(requestedPeptideColumnNames, proteinRgn.getMaxRows(), null);
        _rs = new GroupedResultSet(rs, "ProteinGroupId", _maxPeptideRows, _maxGroupingRows);

        proteinRgn.setGroupedResultSet((GroupedResultSet)_rs);
        DataRegion peptideGrid = createPeptideDataRegion(requestedPeptideColumnNames);
        proteinRgn.setNestedRegion(peptideGrid);

        ButtonBar bb = createButtonBar("exportProteinGroups", "exportSelectedProteinGroups", "protein groups", proteinRgn);

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
        peptideGrid.removeColumnsFromDisplayColumnList("Protein", "Description", "GeneName", "SeqId");
        return peptideGrid;
    }

    public ResultSet createPeptideResultSet(String requestedPeptideColumnNames, int maxRows, String extraWhere) throws SQLException
    {
        MS2Run run = getSingleRun();
        String sqlColumnNames = getPeptideSQLColumnNames(requestedPeptideColumnNames, run);
        return ProteinManager.getProteinProphetPeptideRS(_url, run, extraWhere, maxRows, sqlColumnNames);
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
        String groupIdString = url.getParameter("proteinGroupId");
        if (groupIdString == null)
        {
            HttpView.throwNotFound("No protein group id specified");
        }
        int groupId = 0;
        try
        {
            groupId = Integer.parseInt(groupIdString);
        }
        catch (NumberFormatException e)
        {
            HttpView.throwNotFound("Invalid protein group id specified - " + groupIdString);
        }
        String extraWhere = MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId = " + groupId;
        ResultSet rs = null;
        try
        {
            rs = createPeptideResultSet("RowId", _maxPeptideRows, extraWhere);
            int columnIndex = rs.findColumn("RowId");
            ArrayList<Long> rowIdsLong = new ArrayList<Long>(100);
            while(rs.next())
                rowIdsLong.add(rs.getLong(columnIndex));

            return rowIdsLong.toArray(new Long[]{});
        }
        finally
        {
            if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
        }
    }

    public GridView createPeptideViewForGrouping(OldMS2Controller.DetailsForm form) throws SQLException
    {
        GridView peptidesGridView;
        DataRegion rgn = getNestedPeptideGrid(getSingleRun(), form.getColumns(), false);

        peptidesGridView = new GridView(rgn);

        String columnNames = getPeptideColumnNames(form.getColumns());
        String sqlColumnNames = getPeptideSQLColumnNames(columnNames, getSingleRun());
        String extraWhere;
        if (form.getGroupNumber() != 0)
        {
            extraWhere = MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber = " + form.getGroupNumber() + " AND " +
                MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId = " + form.getIndistinguishableCollectionId();
            peptidesGridView.setResultSet(ProteinManager.getProteinProphetPeptideRS(_url, getSingleRun(), extraWhere, 250, sqlColumnNames));
        }
        return peptidesGridView;
    }

    public String[] getPeptideStringsForGrouping(OldMS2Controller.DetailsForm form) throws SQLException
    {
        SimpleFilter coverageFilter = null;
        if (form.getSeqIdInt() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getSingleRun());
            coverageFilter.addCondition("pgm.SeqId", form.getSeqIdInt());
        }
        else if (form.getGroupNumber() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getSingleRun());
            coverageFilter.addCondition("pg.GroupNumber", form.getGroupNumber());
            coverageFilter.addCondition("pg.IndistinguishableCollectionId", form.getIndistinguishableCollectionId());
        }

        if (coverageFilter != null)
        {
            String sql = "SELECT Peptide FROM " + MS2Manager.getTableInfoPeptides() + " p, " +
                    MS2Manager.getTableInfoPeptideMemberships() + " pm," +
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
        GridView proteinView = new GridView(proteinRgn);
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
        ew.setColumns(getProteinDisplayColumns(requestedProteinColumnNames, true));
        return ew;
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        return new ProteinProphetTSVGridWriter(proteinDisplayColumns, peptideDisplayColumns);
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        ResultSet proteinRS = ProteinManager.getProteinProphetRS(_url, run, where, 0);

        ewProtein.setResultSet(proteinRS);
        ewProtein.setExpanded(expanded);
        ewProtein.setAutoSize(false);

        String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumnNames);
        String sqlPeptideColumnNames = getPeptideSQLColumnNames(peptideColumnNames, run);

        GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS, sqlPeptideColumnNames), "ProteinGroupId");
        ewProtein.setGroupedResultSet(peptideRS);

        if (expanded)
        {
            DataRegion peptideRgn = getPeptideGrid(peptideColumnNames, 0);
            ExcelWriter ewPeptide = new ExcelWriter(peptideRS, peptideRgn.getDisplayColumnList());
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
        GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, 0, peptideSqlColumnNames), "ProteinGroupId", false);
        tw.setGroupedResultSet(peptideRS);
        if (tw.getExpanded())
        {



            TSVGridWriter twPeptide = new TSVGridWriter(peptideRS, getPeptideDisplayColumns(peptideColumnNames))
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
            proteinRS = ProteinManager.getProteinProphetRS(_url, run, where, 0);
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

        sqlSummaries.add(new Pair<String, String>("Protein Group Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteinGroups()).getFilterText()));
        sqlSummaries.add(new Pair<String, String>("Protein Group Sort", new Sort(_url, MS2Manager.getDataRegionNameProteinGroups()).getSortText()));
    }

    public MS2RunViewType getViewType()
    {
        return MS2RunViewType.PROTEIN_PROPHET;
    }

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT PGM.SeqId FROM ");
        fragment.append(MS2Manager.getTableInfoProteinGroupMemberships() + " PGM, ( ");
        fragment.append(ProteinManager.getProteinProphetPeptideSql(queryUrl, run, null, -1, "ms2.ProteinGroupsWithQuantitation.ProteinGroupId AS GroupId", false));
        fragment.append(" ) x WHERE x.ProteinGroupId = PGM.ProteinGroupId");
        return fragment;
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        HashMap<String, SimpleFilter> map = new HashMap<String, SimpleFilter>();
        map.put("peptideFilter", ProteinManager.getPeptideFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run));
        map.put("proteinGroupFilter", ProteinManager.getProteinGroupFilter(queryUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, null, run));
        return map;
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String requestedPeptideColumns)
            throws SQLException
    {
        String peptideColumns = getPeptideColumnNames(requestedPeptideColumns);
        DataRegion peptideRegion = createPeptideDataRegion(requestedPeptideColumns);
        GridView view = new GridView(peptideRegion);
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
