package org.labkey.ms2.peptideview;

import org.labkey.api.ms2.MS2Run;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.security.User;
import org.fhcrc.cpas.util.Pair;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

import org.labkey.ms2.*;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetPeptideView extends AbstractPeptideView
{
    private ResultSet _rs;

    public ProteinProphetPeptideView(Container c, User u, ViewURLHelper url, MS2Run... runs)
    {
        super(c, u, "NestedPeptides", url, runs);
    }

    private List<String> getSequenceColumns(String requestedProteinColumnNames) throws SQLException
    {
        ProteinColumnNameList originalNames = new ProteinColumnNameList(requestedProteinColumnNames);
        List<String> allSequenceColumns = new ProteinColumnNameList("Description,Protein,BestName,BestGeneName,SequenceMass");
        List<String> result = new ArrayList<String>();
        for (String originalName : originalNames)
        {
            if (allSequenceColumns.contains(originalName))
            {
                result.add(originalName);
            }
        }
        return result;
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames) throws SQLException
    {
        ProteinColumnNameList originalNames = new ProteinColumnNameList(requestedProteinColumnNames);
        List<DisplayColumn> displayColumns = new ArrayList<DisplayColumn>();
        ProteinGroupProteins proteins = new ProteinGroupProteins(_runs);
        for (String name : originalNames)
        {
            if (name.equalsIgnoreCase("GroupNumber"))
            {
                displayColumns.add(new GroupNumberDisplayColumn(MS2Manager.getTableInfoProteinGroupsWithQuantitation().getColumn("GroupNumber"), _url));
            }
            else if (name.equalsIgnoreCase("AACoverage"))
            {
                addColumn(_calculatedProteinColumns, "PercentCoverage", displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
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
            else
            {
                addColumn(_calculatedProteinColumns, name, displayColumns, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            }
        }
        List<String> sequenceColumns = getSequenceColumns(requestedProteinColumnNames);
        if (!sequenceColumns.isEmpty())
        {
            displayColumns.add(new ProteinListDisplayColumn(sequenceColumns, proteins));
        }
        return displayColumns;
    }

    public List<DisplayColumn> getPeptideDisplayColumns(String peptideColumnNames) throws SQLException
    {
        return getColumns(_calculatedPeptideColumns, new PeptideColumnNameList(peptideColumnNames), MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoPeptideMemberships());
    }

    private ProteinProphetDataRegion createProteinDataRegion(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws SQLException
    {
        ProteinProphetDataRegion proteinRgn = new ProteinProphetDataRegion(_url);
        proteinRgn.setTable(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        proteinRgn.setName(MS2Manager.getDataRegionNameProteinGroups());
        proteinRgn.addColumns(getProteinDisplayColumns(requestedProteinColumnNames));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setRecordSelectorValueColumns("ProteinGroupId");
        proteinRgn.setMaxRows(MAX_PEPTIDE_DISPLAY_ROWS);

        ResultSet rs = createPeptideResultSet(requestedPeptideColumnNames, null);
        _rs = new GroupedResultSet(rs, "ProteinGroupId", proteinRgn.getMaxRows());

        if (expanded)
        {
            proteinRgn.setGroupedResultSet((GroupedResultSet)_rs);
            DataRegion peptideGrid = createPeptideDataRegion(requestedPeptideColumnNames);
            proteinRgn.setNestedRegion(peptideGrid);
        }

        ButtonBar bb = createButtonBar("exportProteinGroups", "exportSelectedProteinGroups", "protein groups");

        proteinRgn.addHiddenFormField("queryString", _url.getRawQuery());

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

    public ResultSet createPeptideResultSet(String requestedPeptideColumnNames, String extraWhere) throws SQLException
    {
        return createPeptideResultSet(requestedPeptideColumnNames, getSingleRun(), MAX_PEPTIDE_DISPLAY_ROWS, extraWhere);
    }

    public ResultSet createPeptideResultSet(String requestedPeptideColumnNames, MS2Run run, int maxRows, String extraWhere) throws SQLException
    {
        String sqlColumnNames = run.getSQLPeptideColumnNames(requestedPeptideColumnNames, false, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoPeptideMemberships());
        return ProteinManager.getProteinProphetPeptideRS(_url, run, extraWhere, maxRows, sqlColumnNames);
    }

    // Need to add proteinGroupId to signature
    protected String getIndexLookup(ViewURLHelper url)
    {
        return super.getIndexLookup(url) + "|GroupId=" + url.getParameter("proteinGroupId");
    }

    // Default behavior doesn't work for ProteinProphet groupings -- need to add in GroupId by joining to PeptideMemberships table
    protected Long[] generatePeptideIndex(ViewURLHelper url) throws SQLException
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
            rs = createPeptideResultSet("RowId", extraWhere);
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

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        GridView peptidesGridView;
        DataRegion rgn = getNestedPeptideGrid(getSingleRun(), form.getColumns(), false);

        peptidesGridView = new GridView(rgn);

        String columnNames = getPeptideColumnNames(form.getColumns());
        String sqlColumnNames = getSingleRun().getSQLPeptideColumnNames(columnNames, false, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoPeptideMemberships());
        String extraWhere;
        if (form.getSeqId() != 0)
        {
            extraWhere = MS2Manager.getTableInfoProteinGroupMemberships() + ".SeqId = " + form.getSeqId();
            peptidesGridView.setResultSet(ProteinManager.getProteinProphetPeptideRS(_url, getSingleRun(), extraWhere, 250, sqlColumnNames));
        }
        else if (form.getGroupNumber() != 0)
        {
            extraWhere = MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber = " + form.getGroupNumber() + " AND " +
                MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId = " + form.getIndistinguishableCollectionId();
            peptidesGridView.setResultSet(ProteinManager.getProteinProphetPeptideRS(_url, getSingleRun(), extraWhere, 250, sqlColumnNames));
        }
        return peptidesGridView;
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        SimpleFilter coverageFilter = null;
        if (form.getSeqId() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);
            coverageFilter.addCondition("pgm.SeqId", form.getSeqId());
        }
        else if (form.getGroupNumber() != 0)
        {
            coverageFilter = ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);
            coverageFilter.addCondition("pg.GroupNumber", form.getGroupNumber());
            coverageFilter.addCondition("pg.IndistinguishableCollectionId", form.getIndistinguishableCollectionId());
        }

        if (coverageFilter != null)
        {
            String sql = "SELECT Peptide FROM " + MS2Manager.getTableInfoPeptides() + " p, " +
                    MS2Manager.getTableInfoPeptideMemberships() + " pm," +
                    MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " +
                    MS2Manager.getTableInfoProteinGroups() + " pg " +
                    coverageFilter.getWhereSQL(ProteinManager.getSqlDialect()) +
                    " AND pg.RowId = pgm.ProteinGroupId" +
                    " AND pm.PeptideId = p.RowId" +
                    " AND pm.ProteinGroupId = pgm.ProteinGroupId";
            return Table.executeArray(ProteinManager.getSchema(), sql, coverageFilter.getWhereParams(MS2Manager.getTableInfoPeptides()).toArray(), String.class);
        }
        return null;
    }

    public GridView getGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
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
            rs = new ResultSetCollapser(_rs, "ProteinGroupId", MAX_PEPTIDE_DISPLAY_ROWS);
        }
        proteinView.setResultSet(rs);

        proteinView.setContainer(getContainer());
        proteinView.setTitle("Protein Prophet Groups");
        return proteinView;
    }

    public ProteinProphetExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
    {
        ProteinProphetExcelWriter ew = new ProteinProphetExcelWriter();
        ew.setColumns(getProteinDisplayColumns(requestedProteinColumnNames));
        return ew;
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        return new ProteinTSVGridWriter(proteinDisplayColumns, peptideDisplayColumns);
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        ResultSet proteinRS = ProteinManager.getProteinProphetRS(_url, run, where, ExcelWriter.MAX_ROWS);

        ewProtein.setResultSet(proteinRS);
        ewProtein.setExpanded(expanded);
        ewProtein.setAutoSize(false);

        if (expanded)
        {
            String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumnNames);
            String sqlPeptideColumnNames = run.getSQLPeptideColumnNames(peptideColumnNames, false, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoPeptideMemberships());
            GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS, sqlPeptideColumnNames), "ProteinGroupId");
            ewProtein.setGroupedResultSet(peptideRS);

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
        if (tw.getExpanded())
        {
            String peptideColumnNames = getPeptideColumnNames(requestedPeptideColumns);
            String peptideSqlColumnNames = run.getSQLPeptideColumnNames(peptideColumnNames, false, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoPeptideMemberships());

            GroupedResultSet peptideRS = new GroupedResultSet(ProteinManager.getProteinProphetPeptideRS(_url, run, where, 0, peptideSqlColumnNames), "ProteinGroupId");

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
            tw.setGroupedResultSet(peptideRS);
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
        }
    }

    public void addSQLSummaries(List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<String, String>("Protein Group Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteinGroups()).getFilterText(MS2Manager.getSqlDialect())));
        sqlSummaries.add(new Pair<String, String>("Protein Group Sort", new Sort(_url, MS2Manager.getDataRegionNameProteinGroups()).getSortText(MS2Manager.getSqlDialect())));
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String requestedPeptideColumns)
            throws SQLException
    {
        String peptideColumns = getPeptideColumnNames(requestedPeptideColumns);
        DataRegion peptideRegion = createPeptideDataRegion(requestedPeptideColumns);
        GridView view = new GridView(peptideRegion);
        String extraWhere = MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId = " + proteinGroupingId;
        view.setResultSet(createPeptideResultSet(peptideColumns, extraWhere));
        return view;
    }
}
