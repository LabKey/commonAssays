package org.labkey.ms2.peptideview;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.GridView;
import org.labkey.api.data.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.security.User;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class StandardProteinPeptideView extends AbstractPeptideView
{
    public StandardProteinPeptideView(Container c, User u, ViewURLHelper url, MS2Run... runs)
    {
        super(c, u, "NestedPeptides", url, runs);
    }

    public GridView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
    {
        DataRegion proteinRgn = createProteinDataRegion(expanded, requestedPeptideColumnNames, requestedProteinColumnNames);
        proteinRgn.setTable(MS2Manager.getTableInfoProteins());
        proteinRgn.setName(MS2Manager.getDataRegionNameProteins());
        GridView proteinView = new GridView(proteinRgn);
        proteinView.setResultSet(ProteinManager.getProteinRS(_url, getSingleRun(), null, proteinRgn.getMaxRows()));
        proteinView.setContainer(getContainer());
        proteinView.setTitle("Proteins");
        return proteinView;
    }

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames) throws SQLException
    {
        return getColumns(_calculatedProteinColumns, new ProteinColumnNameList(requestedProteinColumnNames), MS2Manager.getTableInfoProteins());
    }


    private StandardProteinDataRegion createProteinDataRegion(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws SQLException
    {
        StandardProteinDataRegion proteinRgn = new StandardProteinDataRegion();
        proteinRgn.addColumns(getProteinDisplayColumns(requestedProteinColumnNames));
        proteinRgn.setShowRecordSelectors(true);
        proteinRgn.setExpanded(expanded);
        proteinRgn.setMaxRows(MAX_PROTEIN_DISPLAY_ROWS);

        MS2Run run = getSingleRun();

        String columnNames = getPeptideColumnNames(requestedPeptideColumnNames);

        DataRegion peptideGrid = getNestedPeptideGrid(run, columnNames, true);
        proteinRgn.setNestedRegion(peptideGrid);
        GroupedResultSet peptideResultSet = createPeptideResultSet(columnNames, run, MAX_PROTEIN_DISPLAY_ROWS, null);
        proteinRgn.setGroupedResultSet(peptideResultSet);

        ViewURLHelper proteinUrl = _url.clone();
        proteinUrl.setAction("showProtein");
        DisplayColumn proteinColumn = proteinRgn.getDisplayColumn("Protein");
        if (proteinColumn != null)
        {
            proteinColumn.setURL(proteinUrl.getLocalURIString() + "&seqId=${SeqId}");
            proteinColumn.setLinkTarget("prot");
        }

        ButtonBar bb = createButtonBar("exportAllProteins", "exportSelectedProteins", "proteins");
        proteinRgn.addHiddenFormField("queryString", _url.getRawQuery());

        proteinRgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return proteinRgn;
    }

    public GroupedResultSet createPeptideResultSet(String columnNames, MS2Run run, int maxRows, String extraWhere) throws SQLException
    {
        String sqlColumnNames = getPeptideSQLColumnNames(columnNames, run);
        return ProteinManager.getPeptideRS(_url, run, extraWhere, maxRows, sqlColumnNames);
    }

    public StandardProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
    {
        StandardProteinExcelWriter ew = new StandardProteinExcelWriter();
        ew.setColumns(getProteinDisplayColumns(requestedProteinColumnNames));
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

        ResultSet proteinRS = ProteinManager.getProteinRS(_url, run, where, ExcelWriter.MAX_ROWS);
        GroupedResultSet peptideRS = ProteinManager.getPeptideRS(_url, run, where, ExcelWriter.MAX_ROWS, sqlPeptideColumnNames);
        DataRegion peptideRgn = getPeptideGrid(peptideColumnNames, 0);

        ewProtein.setResultSet(proteinRS);
        ewProtein.setGroupedResultSet(peptideRS);
        ExcelWriter ewPeptide = new ExcelWriter(peptideRS, peptideRgn.getDisplayColumnList());
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
            proteinRS = ProteinManager.getProteinRS(_url, run, where, 0);
            peptideRS = ProteinManager.getPeptideRS(_url, run, where, 0, peptideSqlColumnNames);

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


    public void addSQLSummaries(List<Pair<String, String>> sqlSummaries)
    {
        sqlSummaries.add(new Pair<String, String>("Protein Filter", new SimpleFilter(_url, MS2Manager.getDataRegionNameProteins()).getFilterText(MS2Manager.getSqlDialect())));
        sqlSummaries.add(new Pair<String, String>("Protein Sort", new Sort(_url, MS2Manager.getDataRegionNameProteins()).getSortText(MS2Manager.getSqlDialect())));
    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        DataRegion rgn = getNestedPeptideGrid(getSingleRun(), form.getColumns(), true);
        GridView gridView = new GridView(rgn);
        SimpleFilter gridFilter = ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.RUN_FILTER + ProteinManager.EXTRA_FILTER + ProteinManager.PROTEIN_FILTER);
        gridView.setFilter(gridFilter);
        gridView.setSort(ProteinManager.getPeptideBaseSort());
        return gridView;
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        SimpleFilter coverageFilter = ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.ALL_FILTERS);
        return Table.executeArray(ProteinManager.getSchema(), "SELECT Peptide FROM " + MS2Manager.getTableInfoPeptides() + " " + coverageFilter.getWhereSQL(ProteinManager.getSqlDialect()), coverageFilter.getWhereParams(MS2Manager.getTableInfoPeptides()).toArray(), String.class);
    }
}
