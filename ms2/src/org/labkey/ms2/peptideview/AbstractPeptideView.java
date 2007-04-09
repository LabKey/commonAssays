package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.GridView;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.*;
import org.labkey.api.security.User;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.common.util.Pair;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.*;

import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractPeptideView
{
    private static Logger _log = Logger.getLogger(AbstractPeptideView.class);

    protected static Map<String, Class<? extends DisplayColumn>> _calculatedPeptideColumns = new HashMap<String, Class<? extends DisplayColumn>>();
    protected static Map<String, Class<? extends DisplayColumn>> _calculatedProteinColumns = new HashMap<String, Class<? extends DisplayColumn>>();

    static
    {
        _calculatedPeptideColumns.put("H", HydrophobicityColumn.class);
        _calculatedPeptideColumns.put("DeltaScan", DeltaScanColumn.class);

        // Different renderer to ensure that SeqId is always selected when Protein column is displayed
        MS2Manager.getTableInfoPeptides().getColumn("Protein").setRenderClass(ProteinDisplayColumn.class);

        _calculatedProteinColumns.put("AACoverage", AACoverageColumn.class);
    }


    private final Container _container;
    private final User _user;
    protected final ViewURLHelper _url;
    protected final MS2Run[] _runs;
    protected int _maxPeptideRows = 1000; // Limit peptides returned to 1,000 rows
    protected int _maxGroupingRows = 250; // Limit proteins returned to 250 rows

    private String _columnPropertyName;

    public static AbstractPeptideView getPeptideView(String grouping, Container c, User user, ViewURLHelper url, ViewContext viewContext, MS2Run... runs)
    {
        if ("protein".equals(grouping))
        {
            return new StandardProteinPeptideView(c, user, url, runs);
        }
        else if ("proteinprophet".equals(grouping))
        {
            return new ProteinProphetPeptideView(c, user, url, runs);
        }
        else if ("query".equals(grouping))
        {
            return new QueryPeptideView(c, user, url, runs, viewContext);
        }
        else
        {
            return new FlatPeptideView(c, user, url, runs);
        }
    }

    public AbstractPeptideView(Container c, User u, String columnPropertyName, ViewURLHelper url, MS2Run... runs)
    {
        _container = c;
        _user = u;
        _columnPropertyName = columnPropertyName;
        _url = url;
        _runs = runs;
    }

    public WebPartView createGridView(MS2Controller.RunForm form) throws ServletException, SQLException
    {
        String peptideColumnNames = getPeptideColumnNames(form.getColumns());
        return createGridView(form.getExpanded(), peptideColumnNames, form.getProteinColumns());
    }

    public abstract WebPartView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException;

    public abstract AbstractProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException;

    protected abstract List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException;

    public abstract GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException;

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

    public abstract void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException;

    public abstract void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException;

    public abstract void addSQLSummaries(List<Pair<String, String>> sqlSummaries);

    public Container getContainer()
    {
        return _container;
    }

    protected ButtonBar createButtonBar(String exportAllAction, String exportSelectedAction, String whatWeAreSelecting)
    {
        ButtonBar result = new ButtonBar();

        result.add(ActionButton.BUTTON_SELECT_ALL);
        result.add(ActionButton.BUTTON_CLEAR_ALL);

        ViewURLHelper exportUrl = _url.clone();

        exportUrl.setAction(exportAllAction);
        ActionButton exportAll = new ActionButton(exportUrl.getEncodedLocalURIString(), "Export All");
        exportAll.setActionType(ActionButton.Action.POST);
        result.add(exportAll);

        ActionButton exportSelected = new ActionButton("", "Export Selected");
        exportUrl.setAction(exportSelectedAction);
        exportSelected.setScript("return verifySelected(this.form, \"" + exportUrl.getEncodedLocalURIString() + "\", \"post\", \"" + whatWeAreSelecting + "\")");
        exportSelected.setActionType(ActionButton.Action.POST);
        result.add(exportSelected);

        DropDownList exportFormat = new DropDownList("exportFormat");
        exportFormat.add("Excel");
        exportFormat.add("TSV");
        exportFormat.add("DTA");
        exportFormat.add("PKL");
        exportFormat.add("AMT");
        result.add(exportFormat);

        // TODO: Temp hack -- need to support GO charts in protein views
        if ("peptides".equals(whatWeAreSelecting) && ProteinManager.isGoLoaded())
        {
            result.add(new ActionButton("peptideCharts.post", "GO Piechart"));
            DropDownList chartType = new DropDownList("chartType");
            chartType.add(ProteinDictionaryHelpers.GoTypes.CELL_LOCATION.toString());
            chartType.add(ProteinDictionaryHelpers.GoTypes.FUNCTION.toString());
            chartType.add(ProteinDictionaryHelpers.GoTypes.PROCESS.toString());
            result.add(chartType);
        }

        return result;
    }


    public void changePeptideCaptionsForTsv(List<DisplayColumn> displayColumns)
    {
        // Get rid of % and + in these captions if the columns are being used; they cause problem for some statistical tools
        replaceCaption(displayColumns, "IonPercent", "IonPercent");
        replaceCaption(displayColumns, "Mass", "CalcMHPlus");
        replaceCaption(displayColumns, "PrecursorMass", "ObsMHPlus");
    }


    private void replaceCaption(List<DisplayColumn> displayColumns, String columnName, String caption)
    {
        for (DisplayColumn dc : displayColumns)
            if (dc.getName().equalsIgnoreCase(columnName))
                dc.setCaption(caption);
    }


    protected User getUser()
    {
        return _user;
    }

    public void writeExcel(HttpServletResponse response, MS2Run run, boolean expanded, String columns, String where, List<String> headers, String proteinColumns) throws SQLException
    {
        AbstractProteinExcelWriter ewProtein = getExcelProteinGridWriter(proteinColumns);
        ewProtein.setSheetName(run.getDescription());
        ewProtein.setFooter(run.getDescription() + " Proteins");
        ewProtein.setHeaders(headers);
        setUpExcelProteinGrid(ewProtein, expanded, columns, run, where);
        ewProtein.write(response);
    }

    public void savePeptideColumnNames(String type, String columnNames) throws SQLException
    {
        PropertyManager.PropertyMap defaultColumnLists = PropertyManager.getWritableProperties(_user.getUserId(), ContainerManager.getRoot().getId(), "ColumnNames", true);
        defaultColumnLists.put(type + _columnPropertyName, columnNames);
        PropertyManager.saveProperties(defaultColumnLists);
    }

    public void saveProteinColumnNames(String type, String columnNames) throws SQLException
    {
        PropertyManager.PropertyMap defaultColumnLists = PropertyManager.getWritableProperties(_user.getUserId(), ContainerManager.getRoot().getId(), "ProteinColumnNames", true);
        defaultColumnLists.put(type + "Protein", columnNames);
        PropertyManager.saveProperties(defaultColumnLists);
    }

    private String getSavedPeptideColumnNames() throws SQLException
    {
        Map defaultColumnLists = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), "ColumnNames", false);

        if (null != defaultColumnLists)
            return (String) defaultColumnLists.get(_runs[0].getType() + "Peptides");

        return null;
    }

    public String getStandardPeptideColumnNames()
    {
        StringBuilder result = new StringBuilder();
        result.append("Scan, Charge, ");
        result.append(_runs[0].getScoreColumnNames());
        result.append("IonPercent, Mass, DeltaMass, PeptideProphet, Peptide, ProteinHits, Protein");
        return result.toString();
    }

    public String getStandardProteinColumnNames()
    {
        StringBuilder result = new StringBuilder();
        result.append(MS2Run.getDefaultProteinProphetProteinColumnNames());
        result.append(",");
        result.append(MS2Run.getCommonProteinColumnNames());
        return result.toString();
    }

    public DataRegion getNestedPeptideGrid(MS2Run run, String requestedPeptideColumnNames, boolean selectSeqId) throws SQLException
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

        ViewURLHelper showUrl = _url.clone();
        String seqId = showUrl.getParameter("seqId");
        showUrl.deleteParameter("seqId");
        String extraParams = "";
        if (selectSeqId)
        {
            extraParams = null == seqId ? "seqId=${SeqId}" : "seqId=" + seqId;
        }
        if ("proteinprophet".equals(_url.getParameter("grouping")))
        {
            extraParams = "proteinGroupId=${proteinGroupId}";
        }
        setPeptideUrls(rgn, extraParams);

        ButtonBar bb = new ButtonBar();
        bb.setVisible(false);  // Don't show button bar on nested region; also ensures no nested <form>...</form>
        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }


    // Pull the URL associated with gene name from the database and cache it for use with the Gene Name column
    static String _geneNameUrl = null;

    static
    {
        try
        {
            _geneNameUrl = ProteinManager.makeIdentURLStringWithType("GeneNameGeneName", "GeneName").replaceAll("GeneNameGeneName", "\\${GeneName}");
        }
        catch(Exception e)
        {
            _log.debug("Problem getting gene name URL", e);
        }
    }


    public void setPeptideUrls(DataRegion rgn, String extraPeptideUrlParams)
    {
        ViewURLHelper showUrl = _url.clone();
        showUrl.setAction("showPeptide");
        String peptideUrlString = showUrl.getLocalURIString() + "&peptideId=${RowId}";

        if (null != extraPeptideUrlParams)
            peptideUrlString += "&" + extraPeptideUrlParams;

        peptideUrlString += "&rowIndex=${_row}";

        setColumnURL(rgn, "scan", peptideUrlString, "pep");
        setColumnURL(rgn, "peptide", peptideUrlString, "pep");
        String quantURLString = peptideUrlString + "#quantitation";

        setColumnURL(rgn, "lightfirstscan", quantURLString, "pep");
        setColumnURL(rgn, "lightlastscan", quantURLString, "pep");
        setColumnURL(rgn, "heavyfirstscan", quantURLString, "pep");
        setColumnURL(rgn, "heavylastscan", quantURLString, "pep");
        setColumnURL(rgn, "lightmass", quantURLString, "pep");
        setColumnURL(rgn, "heavymass", quantURLString, "pep");
        setColumnURL(rgn, "ratio", quantURLString, "pep");
        setColumnURL(rgn, "heavy2lightratio", quantURLString, "pep");
        setColumnURL(rgn, "lightarea", quantURLString, "pep");
        setColumnURL(rgn, "heavyarea", quantURLString, "pep");
        setColumnURL(rgn, "decimalratio", quantURLString, "pep");

        showUrl.setAction("showAllProteins");
        setColumnURL(rgn, "proteinHits", showUrl.getLocalURIString() + "&peptideId=${RowId}", "prot");

        DisplayColumn dc = rgn.getDisplayColumn("protein");
        if (null != dc)
        {
            showUrl.setAction("showProtein");
            ((DataColumn)dc).setURLExpression(new ProteinStringExpression(showUrl.getLocalURIString()));
            dc.setLinkTarget("prot");
        }

        dc = rgn.getDisplayColumn("GeneName");
        if (null != dc)
            dc.setURL(_geneNameUrl);
    }

    private void setColumnURL(DataRegion rgn, String columnName, String peptideUrlString, String linkTarget)
    {
        DisplayColumn dc = rgn.getDisplayColumn(columnName);
        if (null != dc)
        {
            dc.setURL(peptideUrlString);
            dc.setLinkTarget(linkTarget);
        }
    }

    public DataRegion getPeptideGrid(String peptideColumnNames, int maxRows) throws SQLException
    {
        DataRegion rgn = new DataRegion();

        rgn.setName(MS2Manager.getDataRegionNamePeptides());
        rgn.setDisplayColumnList(getPeptideDisplayColumns(peptideColumnNames));
        rgn.setMaxRows(maxRows);

        return rgn;
    }

    public List<DisplayColumn> getPeptideDisplayColumns(String peptideColumnNames) throws SQLException
    {
        return getColumns(_calculatedPeptideColumns, new PeptideColumnNameList(peptideColumnNames), MS2Manager.getTableInfoPeptides());
    }

    protected void addColumn(Map<String, Class<? extends DisplayColumn>> calculatedColumns, String columnName, List<DisplayColumn> columns, TableInfo... tinfos)
    {
        ColumnInfo ci = null;
        for (TableInfo tableInfo : tinfos)
        {
            ci = tableInfo.getColumn(columnName);
            if (ci != null)
            {
                break;
            }
        }
        DisplayColumn dc = null;

        if (null != ci)
            dc = ci.getRenderer();
        else
        {
            Class<? extends DisplayColumn> clss = calculatedColumns.get(columnName);

            if (null != clss)
            {
                try
                {
                    dc = clss.newInstance();
                }
                catch (Exception e)
                {
                    _log.error("Failed to create a column", e);
                }
            }
        }
        if (dc != null)
        {
            columns.add(dc);
        }
    }

    protected List<DisplayColumn> getColumns(Map<String, Class<? extends DisplayColumn>> calculatedColumns, List<String> columnNames, TableInfo... tinfos) throws SQLException
    {
        List<DisplayColumn> result = new ArrayList<DisplayColumn>();

        for (String columnName : columnNames)
        {
            addColumn(calculatedColumns, columnName, result, tinfos);
        }

        if (result.isEmpty())
        {
            for (String columnName : new PeptideColumnNameList(getStandardPeptideColumnNames()))
            {
                addColumn(calculatedColumns, columnName, result, tinfos);
            }
        }

        return result;
    }


    public long[] getPeptideIndex(ViewURLHelper url) throws SQLException
    {
        String lookup = getIndexLookup(url);
        long[] index = MS2Manager.getPeptideIndex(lookup);

        if (null == index)
        {
            Long[] rowIdsLong = generatePeptideIndex(url);

            index = new long[rowIdsLong.length];
            for (int i=0; i<rowIdsLong.length; i++)
                index[i] = rowIdsLong[i];

            MS2Manager.cachePeptideIndex(lookup, index);
        }

        return index;
    }


    // Generate signature used to cache & retrieve the peptide index 
    protected String getIndexLookup(ViewURLHelper url)
    {
        return "Filter:" + getPeptideFilter(url).toSQLString(MS2Manager.getSqlDialect()) + "|Sort:" + getPeptideSort().getSortText();
    }


    private Sort getPeptideSort()
    {
        return new Sort(_url, MS2Manager.getDataRegionNamePeptides());
    }


    private Filter getPeptideFilter(ViewURLHelper url)
    {
        return ProteinManager.getPeptideFilter(url, _runs[0], ProteinManager.ALL_FILTERS);
    }


    protected Long[] generatePeptideIndex(ViewURLHelper url) throws SQLException
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.insertSort(getPeptideSort());

        Filter filter = getPeptideFilter(url);

        return Table.executeArray(MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoPeptides().getColumn("RowId"), filter, sort, Long.class);
    }

    protected MS2Run getSingleRun()
    {
        if (_runs.length > 1)
        {
            throw new UnsupportedOperationException("Not supported for multiple runs");
        }
        return _runs[0];
    }

    /**
     * Creates a grid view to show the peptides that are part of the grouping assignment
     * that's specified by the form. For example, for a given SeqId or a ProteinProphet ProteinGroup. 
     */
    public abstract GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form) throws SQLException;

    public abstract String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException;

    protected class PeptideColumnNameList extends MS2Run.ColumnNameList
    {
        PeptideColumnNameList(String columnNames) throws SQLException
        {
            super(getPeptideColumnNames(columnNames));
        }
    }

    protected  class ProteinColumnNameList extends MS2Run.ColumnNameList
    {
        ProteinColumnNameList(String columnNames) throws SQLException
        {
            super(getProteinColumnNames(columnNames));
        }
        
        public ProteinColumnNameList(List<String> columnNames)
        {
            super(columnNames);
        }
    }

    public String getPeptideColumnNames(String columnNames) throws SQLException
    {
        if (null == columnNames)
        {
            columnNames = getSavedPeptideColumnNames();

            if (null == columnNames)
                columnNames = getStandardPeptideColumnNames();
        }

        return columnNames;
    }

    public String getProteinColumnNames(String proteinColumnNames) throws SQLException
    {
        if (null == proteinColumnNames)
        {
            proteinColumnNames = getSavedProteinColumnNames();

            if (null == proteinColumnNames)
                proteinColumnNames = getStandardProteinColumnNames();
        }

        return proteinColumnNames;
    }

    private String getSavedProteinColumnNames() throws SQLException
    {
        Map defaultColumnLists = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), "ProteinColumnNames", false);

        if (null != defaultColumnLists)
            return (String) defaultColumnLists.get(_runs[0].getType() + "Protein");

        return null;
    }
}
