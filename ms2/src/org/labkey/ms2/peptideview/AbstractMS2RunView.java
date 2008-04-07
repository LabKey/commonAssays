package org.labkey.ms2.peptideview;

import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.security.User;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.common.tools.MS2Modification;
import org.labkey.common.util.Pair;
import org.labkey.ms2.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractMS2RunView<WebPartType extends WebPartView>
{
    private static Logger _log = Logger.getLogger(AbstractMS2RunView.class);

    protected static Map<String, Class<? extends DisplayColumn>> _calculatedPeptideColumns = new CaseInsensitiveHashMap<Class<? extends DisplayColumn>>();

    protected static final String AMT_PEPTIDE_COLUMN_NAMES = "Run,Fraction,Mass,Scan,RetentionTime,H,PeptideProphet,Peptide";

    static
    {
        _calculatedPeptideColumns.put("H", HydrophobicityColumn.class);
        _calculatedPeptideColumns.put("DeltaScan", DeltaScanColumn.class);

        // Different renderer to ensure that SeqId is always selected when Protein column is displayed
        MS2Manager.getTableInfoPeptides().getColumn("Protein").setDisplayColumnFactory(new ProteinDisplayColumnFactory());
    }

    private final Container _container;
    private final User _user;
    protected final ActionURL _url;
    protected final ViewContext _viewContext;
    protected final MS2Run[] _runs;
    protected int _maxPeptideRows = 1000; // Limit peptides returned to 1,000 rows
    protected int _maxGroupingRows = 250; // Limit proteins returned to 250 rows
    protected long _offset = 0;

    private String _columnPropertyName;

    public AbstractMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        _container = viewContext.getContainer();
        _user = viewContext.getUser();
        _columnPropertyName = columnPropertyName;
        _url = viewContext.getActionURL();
        _viewContext = viewContext;
        _runs = runs;
    }

    public WebPartView createGridView(MS2Controller.RunForm form) throws ServletException, SQLException
    {
        String peptideColumnNames = getPeptideColumnNames(form.getColumns());
        return createGridView(form.getExpanded(), peptideColumnNames, form.getProteinColumns(), true);
    }

    public abstract WebPartType createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException;

    public abstract GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException;

    public abstract void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries);

    public abstract MS2RunViewType getViewType();

    public abstract SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form);

    public abstract Map<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run);

    public Container getContainer()
    {
        return _container;
    }

    protected ButtonBar createButtonBar(String exportAllAction, String exportSelectedAction, String whatWeAreSelecting, DataRegion dataRegion)
    {
        ButtonBar result = new ButtonBar();

        List<String> exportFormats = getExportFormats();
        
        ActionURL exportUrl = _url.clone();
        exportUrl.setAction(exportAllAction);
        MenuButton exportAll = new MenuButton("Export All");
        for (String exportFormat : exportFormats)
        {
            exportUrl.replaceParameter("exportFormat", exportFormat);
            exportAll.addMenuItem(exportFormat, "javascript: " + dataRegion.getJavascriptFormReference() + ".action=\"" + exportUrl.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
        }
        result.add(exportAll);

        MenuButton exportSelected = new MenuButton("Export Selected");
        exportUrl.setAction(exportSelectedAction);
        for (String exportFormat : exportFormats)
        {
            exportUrl.replaceParameter("exportFormat", exportFormat);
            exportSelected.addMenuItem(exportFormat, "javascript: if (verifySelected(" + dataRegion.getJavascriptFormReference() + ", \"" + exportUrl.getEncodedLocalURIString() + "\", \"post\", \"" + whatWeAreSelecting + "\")) { " + dataRegion.getJavascriptFormReference() + ".submit(); }");
        }
        result.add(exportSelected);

        // TODO: Temp hack -- need to support GO charts in protein views
        if (getViewType().supportsGOPiechart() && GoLoader.isGoLoaded())
        {
            MenuButton goButton = new MenuButton("Gene Ontology Charts");
            List<ProteinDictionaryHelpers.GoTypes> types = new ArrayList<ProteinDictionaryHelpers.GoTypes>();
            types.add(ProteinDictionaryHelpers.GoTypes.CELL_LOCATION);
            types.add(ProteinDictionaryHelpers.GoTypes.FUNCTION);
            types.add(ProteinDictionaryHelpers.GoTypes.PROCESS);
            for (ProteinDictionaryHelpers.GoTypes goType : types)
            {
                ActionURL url = MS2Controller.getPeptideChartURL(getContainer(), goType);
                goButton.addMenuItem(goType.toString(), "javascript: " + dataRegion.getJavascriptFormReference() + ".action=\"" + url.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
            }
            result.add(goButton);
        }

        return result;
    }

    protected abstract List<String> getExportFormats();

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
        result.append(_runs[0].getRunType().getScoreColumnNames());
        result.append(", IonPercent, Mass, DeltaMass, PeptideProphet, Peptide, ProteinHits, Protein");
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
        ActionURL showUrl = _url.clone();
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

    public DataRegion getPeptideGrid(String peptideColumnNames, int maxRows, long offset) throws SQLException
    {
        DataRegion rgn = new DataRegion();

        rgn.setName(MS2Manager.getDataRegionNamePeptides());
        rgn.setDisplayColumns(getPeptideDisplayColumns(peptideColumnNames));
        rgn.setShowPagination(false);
        rgn.setMaxRows(maxRows);
        rgn.setOffset(offset);

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


    public long[] getPeptideIndex(ActionURL url) throws SQLException
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
    protected String getIndexLookup(ActionURL url)
    {
        return "Filter:" + getPeptideFilter(url).toSQLString(MS2Manager.getSqlDialect()) + "|Sort:" + getPeptideSort().getSortText();
    }


    private Sort getPeptideSort()
    {
        return new Sort(_url, MS2Manager.getDataRegionNamePeptides());
    }


    private SimpleFilter getPeptideFilter(ActionURL url)
    {
        return ProteinManager.getPeptideFilter(url, ProteinManager.ALL_FILTERS, _runs[0]);
    }


    protected Long[] generatePeptideIndex(ActionURL url) throws SQLException
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.insertSort(getPeptideSort());
        sort = ProteinManager.reduceToValidColumns(sort, MS2Manager.getTableInfoPeptides());

        SimpleFilter filter = getPeptideFilter(url);
        filter = ProteinManager.reduceToValidColumns(filter, MS2Manager.getTableInfoPeptides());

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

    public abstract ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception;

    public abstract ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception;

    public abstract ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception;

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
        
        public ProteinColumnNameList(Collection<String> columnNames)
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

    protected void addPeptideFilterText(List<String> headers, MS2Run run, ActionURL currentUrl)
    {
        headers.add("");
        headers.add("Peptide Filter: " + ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run).getFilterText());
        headers.add("Peptide Sort: " + new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText());
    }

    private String naForNull(String s)
    {
        return s == null ? "n/a" : s;
    }

    protected List<String> getRunSummaryHeaders(MS2Run run)
    {
        List<String> headers = new ArrayList<String>();
        headers.add("Run: " + naForNull(run.getDescription()));
        headers.add("");
        headers.add("Search Enzyme: " + naForNull(run.getSearchEnzyme()) + "\tFile Name: " + naForNull(run.getFileName()));
        headers.add("Search Engine: " + naForNull(run.getSearchEngine()) + "\tPath: " + naForNull(run.getPath()));
        headers.add("Mass Spec Type: " + naForNull(run.getMassSpecType()) + "\tFasta File: " + naForNull(run.getFastaFileName()));
        headers.add("");
        return headers;
    }

    protected List<String> getAMTFileHeader()
    {
        List<String> fileHeader = new ArrayList<String>(_runs.length);

        fileHeader.add("#HydrophobicityAlgorithm=" + HydrophobicityColumn.getAlgorithmVersion());

        for (MS2Run run : _runs)
        {
            StringBuilder header = new StringBuilder("#Run");
            header.append(run.getRun()).append("=");
            header.append(run.getDescription()).append("|");
            header.append(run.getFileName()).append("|");
            header.append(run.getLoaded()).append("|");

            MS2Modification[] mods = run.getModifications();

            for (MS2Modification mod : mods)
            {
                if (mod != mods[0])
                    header.append(';');
                header.append(mod.getAminoAcid());
                if (mod.getVariable())
                    header.append(mod.getSymbol());
                header.append('=');
                header.append(mod.getMassDiff());
            }

            fileHeader.add(header.toString());
        }

        return fileHeader;
    }
}
