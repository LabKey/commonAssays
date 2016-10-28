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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.*;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.util.Formats;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Modification;
import org.labkey.api.util.Pair;
import org.labkey.api.query.DetailsURL;
import org.labkey.ms2.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.fasta.FastaFile;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractMS2RunView<WebPartType extends WebPartView>
{
    private static Logger _log = Logger.getLogger(AbstractMS2RunView.class);

    protected static final String AMT_PEPTIDE_COLUMN_NAMES = "Run,Fraction,Mass,Scan,RetentionTime,H,PeptideProphet,Peptide";

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

    public WebPartType createGridView(MS2Controller.RunForm form)
    {
        String peptideColumnNames = getPeptideColumnNames(form.getColumns());
        return createGridView(form.getExpanded(), peptideColumnNames, form.getProteinColumns(), true);
    }

    public abstract WebPartType createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting);

    public abstract GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException;

    public abstract void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries);

    public abstract SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form) throws ServletException;

    public abstract Map<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run) throws ServletException;

    public Container getContainer()
    {
        return _container;
    }

    protected String getAJAXNestedGridURL()
    {
        ActionURL groupURL = _url.clone();
        groupURL.setAction(MS2Controller.GetProteinGroupingPeptidesAction.class);
        groupURL.deleteParameter("proteinGroupingId");

        return groupURL.toString() + "&proteinGroupingId=";
    }
    
    protected ButtonBar createButtonBar(Class<? extends Controller> exportAllAction, Class<? extends Controller> exportSelectedAction, String whatWeAreSelecting, DataRegion dataRegion)
    {
        ButtonBar result = new ButtonBar();

        List<MS2ExportType> exportFormats = getExportTypes();

        ActionURL exportUrl = _url.clone();
        exportUrl.setAction(exportAllAction);
        MenuButton exportAll = new MenuButton("Export All");
        for (MS2ExportType exportFormat : exportFormats)
        {
            exportUrl.replaceParameter("exportFormat", exportFormat.name());
            NavTree menuItem = exportAll.addMenuItem(exportFormat.toString(), null, dataRegion.getJavascriptFormReference() + ".action=\"" + exportUrl.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
            if (exportFormat.getDescription() != null)
            {
                menuItem.setDescription(exportFormat.getDescription());
            }
        }
        result.add(exportAll);

        MenuButton exportSelected = new MenuButton("Export Selected");
        exportUrl.setAction(exportSelectedAction);
        exportSelected.setRequiresSelection(true);
        for (MS2ExportType exportFormat : exportFormats)
        {
            if (exportFormat.supportsSelectedOnly())
            {
                exportUrl.replaceParameter("exportFormat", exportFormat.name());
                NavTree menuItem = exportSelected.addMenuItem(exportFormat.toString(), null, "if (verifySelected(" + dataRegion.getJavascriptFormReference() + ", \"" + exportUrl.getLocalURIString() + "\", \"post\", \"" + whatWeAreSelecting + "\")) { " + dataRegion.getJavascriptFormReference() + ".submit(); }");
                if (exportFormat.getDescription() != null)
                {
                    menuItem.setDescription(exportFormat.getDescription());
                }
            }
        }
        result.add(exportSelected);

        if (GoLoader.isGoLoaded())
        {
            MenuButton goButton = new MenuButton("Gene Ontology Charts");
            List<ProteinDictionaryHelpers.GoTypes> types = new ArrayList<>();
            types.add(ProteinDictionaryHelpers.GoTypes.CELL_LOCATION);
            types.add(ProteinDictionaryHelpers.GoTypes.FUNCTION);
            types.add(ProteinDictionaryHelpers.GoTypes.PROCESS);
            for (ProteinDictionaryHelpers.GoTypes goType : types)
            {
                ActionURL url = MS2Controller.getPeptideChartURL(getContainer(), goType);
                goButton.addMenuItem(goType.toString(), null, dataRegion.getJavascriptFormReference() + ".action=\"" + url.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
            }
            result.add(goButton);
        }

        return result;
    }

    protected abstract List<MS2ExportType> getExportTypes();

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

    public void savePeptideColumnNames(String type, String columnNames)
    {
        PropertyManager.PropertyMap defaultColumnLists = PropertyManager.getWritableProperties(_user, ContainerManager.getRoot(), "ColumnNames", true);
        defaultColumnLists.put(type + _columnPropertyName, columnNames);
        defaultColumnLists.save();
    }

    public void saveProteinColumnNames(String type, String columnNames)
    {
        PropertyManager.PropertyMap defaultColumnLists = PropertyManager.getWritableProperties(_user, ContainerManager.getRoot(), "ProteinColumnNames", true);
        defaultColumnLists.put(type + "Protein", columnNames);
        defaultColumnLists.save();
    }

    private @Nullable String getSavedPeptideColumnNames()
    {
        Map<String, String> defaultColumnLists = PropertyManager.getProperties(getUser(), ContainerManager.getRoot(), "ColumnNames");
        return defaultColumnLists.get(_runs[0].getType() + "Peptides");
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
        ActionURL baseURL = null != extraPeptideUrlParams ? new ActionURL(_url.toString() + "&" + extraPeptideUrlParams) : _url.clone();
        baseURL.setAction(MS2Controller.ShowPeptideAction.class);
        // We might be displaying a peptide grid within a peptide detail (e.g. all matches in a Mascot run); don't duplicate the peptideId & rowIndex params
        // TODO: Should DetailsURL replaceParameter instead of addParameter?
        baseURL.deleteParameter("peptideId");
        baseURL.deleteParameter("rowIndex");
        Map<String, Object> peptideParams = new HashMap<>();
        peptideParams.put("peptideId", "RowId");
        peptideParams.put("rowIndex", "_row");
        DetailsURL peptideDetailsURL = new DetailsURL(baseURL, peptideParams);

        setColumnURL(rgn, "scan", peptideDetailsURL, "pep");
        setColumnURL(rgn, "peptide", peptideDetailsURL, "pep");

        baseURL.setFragment("quantitation");
        DetailsURL quantitationDetailsURL = new DetailsURL(baseURL, peptideParams);

        setColumnURL(rgn, "lightfirstscan", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "lightlastscan", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "heavyfirstscan", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "heavylastscan", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "lightmass", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "heavymass", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "ratio", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "heavy2lightratio", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "lightarea", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "heavyarea", quantitationDetailsURL, "pep");
        setColumnURL(rgn, "decimalratio", quantitationDetailsURL, "pep");

        baseURL.setAction(MS2Controller.ShowAllProteinsAction.class);
        DetailsURL proteinHitsDetailsURL = new DetailsURL(baseURL, Collections.singletonMap("peptideId", "RowId"));
        setColumnURL(rgn, "proteinHits", proteinHitsDetailsURL, "prot");

        baseURL.setFragment(null);
        DisplayColumn dc = rgn.getDisplayColumn("protein");
        if (null != dc)
        {
            baseURL.setAction(MS2Controller.ShowProteinAction.class);
            baseURL.deleteParameter("seqId");
            dc.setURLExpression(new ProteinStringExpression(baseURL.getLocalURIString()));
            dc.setLinkTarget("prot");
        }

        dc = rgn.getDisplayColumn("GeneName");
        if (null != dc)
            dc.setURL(_geneNameUrl);
    }

    private void setColumnURL(DataRegion rgn, String columnName, DetailsURL url, String linkTarget)
    {
        DisplayColumn dc = rgn.getDisplayColumn(columnName);
        if (null != dc)
        {
            dc.setURLExpression(url);
            dc.setLinkTarget(linkTarget);
        }
    }

    public DataRegion getPeptideGrid(String peptideColumnNames, int maxRows, long offset)
    {
        DataRegion rgn = new DataRegion();

        rgn.setName(MS2Manager.getDataRegionNamePeptides());
        rgn.setDisplayColumns(getPeptideDisplayColumns(peptideColumnNames));
        rgn.setShowPagination(false);
        rgn.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        rgn.setMaxRows(maxRows);
        rgn.setOffset(offset);

        return rgn;
    }

    public List<DisplayColumn> getPeptideDisplayColumns(String peptideColumnNames)
    {
        FilteredTable table = createLegacyWrappedPeptidesTable();
        return getColumns(new PeptideColumnNameList(peptideColumnNames), table);
    }

    /** Creates a version of the Peptides table for use in non-query based views. Includes calculated columns like DeltaScan and Hydrophobicity */
    protected FilteredTable createLegacyWrappedPeptidesTable()
    {
        FilteredTable table = new FilteredTable<>(MS2Manager.getTableInfoPeptides(), new MS2Schema(getUser(), getContainer()));
        table.wrapAllColumns(true);
        table.getColumn("RowId").setKeyField(true);
        // Different renderer to ensure that SeqId is always selected when Protein column is displayed
        table.getColumn("Protein").setDisplayColumnFactory(new ProteinDisplayColumnFactory(getContainer()));

        PeptidesTableInfo.addCalculatedColumns(table);
        return table;
    }

    protected void addColumn(String columnName, List<DisplayColumn> columns, TableInfo... tinfos)
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

        if (dc != null)
        {
            columns.add(dc);
        }
    }

    protected List<DisplayColumn> getColumns(List<String> columnNames, TableInfo... tinfos)
    {
        List<DisplayColumn> result = new ArrayList<>();

        for (String columnName : columnNames)
        {
            addColumn(columnName, result, tinfos);
        }

        if (result.isEmpty())
        {
            for (String columnName : new PeptideColumnNameList(getStandardPeptideColumnNames()))
            {
                addColumn(columnName, result, tinfos);
            }
        }

        return result;
    }


    public long[] getPeptideIndex(ActionURL url)
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
        return ProteinManager.getPeptideFilter(url, ProteinManager.ALL_FILTERS, getUser(), _runs[0]);
    }


    protected Long[] generatePeptideIndex(ActionURL url)
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.insertSort(getPeptideSort());
        sort = ProteinManager.reduceToValidColumns(sort, MS2Manager.getTableInfoPeptides());

        SimpleFilter filter = getPeptideFilter(url);
        filter = ProteinManager.reduceToValidColumns(filter, MS2Manager.getTableInfoPeptides());

        return new TableSelector(MS2Manager.getTableInfoPeptides().getColumn("RowId"), filter, sort).getArray(Long.class);
    }

    protected MS2Run getSingleRun()
    {
        if (_runs.length > 1)
        {
            throw new UnsupportedOperationException("Not supported for multiple runs");
        }
        return _runs[0];
    }

    public abstract ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException;

    public abstract ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException;

    public abstract ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException;

    public abstract void exportSpectra(MS2Controller.ExportForm form, ActionURL currentURL, SpectrumRenderer spectrumRenderer, List<String> exportRows) throws IOException, RunListException;

    protected class PeptideColumnNameList extends MS2Run.ColumnNameList
    {
        PeptideColumnNameList(String columnNames)
        {
            super(getPeptideColumnNames(columnNames));
        }
    }

    protected  class ProteinColumnNameList extends MS2Run.ColumnNameList
    {
        ProteinColumnNameList(String columnNames)
        {
            super(getProteinColumnNames(columnNames));
        }
        
        public ProteinColumnNameList(Collection<String> columnNames)
        {
            super(columnNames);
        }
    }

    public String getPeptideColumnNames(String columnNames)
    {
        if (null == columnNames)
        {
            columnNames = getSavedPeptideColumnNames();

            if (null == columnNames)
                columnNames = getStandardPeptideColumnNames();
        }

        return columnNames;
    }

    public String getProteinColumnNames(String proteinColumnNames)
    {
        if (null == proteinColumnNames)
        {
            proteinColumnNames = getSavedProteinColumnNames();

            if (null == proteinColumnNames)
                proteinColumnNames = getStandardProteinColumnNames();
        }

        return proteinColumnNames;
    }

    private @Nullable String getSavedProteinColumnNames()
    {
        Map<String, String> defaultColumnLists = PropertyManager.getProperties(getUser(), ContainerManager.getRoot(), "ProteinColumnNames");
        return defaultColumnLists.get(_runs[0].getType() + "Protein");
    }

    protected void addPeptideFilterText(List<String> headers, MS2Run run, ActionURL currentUrl)
    {
        headers.add("");
        headers.add("Peptide Filter: " + ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getUser(), run).getFilterText());
        headers.add("Peptide Sort: " + new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText());
    }

    private String naForNull(String s)
    {
        return s == null ? "n/a" : s;
    }

    protected List<String> getRunSummaryHeaders(MS2Run run)
    {
        Map<String, String> fixedMods = new TreeMap<>();
        Map<String, String> variableMods = new TreeMap<>();

        for (MS2Modification mod : run.getModifications(MassType.Average))
        {
            if (mod.getVariable())
                variableMods.put(mod.getAminoAcid() + mod.getSymbol(), Formats.f3.format(mod.getMassDiff()));
            else
                fixedMods.put(mod.getAminoAcid(), Formats.f3.format(mod.getMassDiff()));
        }

        List<String> modHeaders = new ArrayList<>(10);

        formatModifications("Fixed", fixedMods, modHeaders);
        formatModifications("Variable", variableMods, modHeaders);

        List<String> runHeaders = new ArrayList<>(3);
        runHeaders.add("Search Enzyme: " + naForNull(run.getSearchEnzyme()) + "\tFile Name: " + naForNull(run.getFileName()));
        runHeaders.add("Search Engine: " + naForNull(run.getSearchEngine()) + "\tPath: " + naForNull(run.getPath()));
        List<String> fastas = new ArrayList<>();
        for (int fastaId : run.getFastaIds())
        {
            FastaFile fastaFile = ProteinManager.getFastaFile(fastaId);
            if (fastaFile == null)
            {
                throw new IllegalStateException("Could not find FastaId " + fastaId + " referenced by run " + run.getFileName() + " - " + run.getRun());
            }
            fastas.add(fastaFile.getFilename());
        }
        runHeaders.add("Mass Spec Type: " + naForNull(run.getMassSpecType()) + "\tFasta File: " + naForNull(StringUtils.join(fastas, ", ")));

        List<String> headers = new ArrayList<>();
        headers.add("Run: " + naForNull(run.getDescription()));
        headers.add("");

        if (modHeaders.isEmpty())
        {
            headers.addAll(runHeaders);
        }
        else
        {
            // Merge modifications list and standard run headers into a single list
            for (int i = 0; i < Math.max(modHeaders.size(), runHeaders.size()); i++)
                headers.add((i < modHeaders.size() ? modHeaders.get(i) : "") + "\t" + (i < runHeaders.size() ? runHeaders.get(i) : ""));
        }

        headers.add("");

        return headers;
    }

    private void formatModifications(String label, Map<String, String> mods, List<String> modHeaders)
    {
        if (!mods.isEmpty())
        {
            modHeaders.add(label + " Modifications");

            for (Map.Entry<String, String> mod : mods.entrySet())
            {
                modHeaders.add(mod.getKey() + " @ " + mod.getValue());
            }
        }
    }

    protected List<String> getAMTFileHeader()
    {
        List<String> fileHeader = new ArrayList<>(_runs.length);

        fileHeader.add("#HydrophobicityAlgorithm=" + HydrophobicityColumn.getAlgorithmVersion());

        for (MS2Run run : _runs)
        {
            StringBuilder header = new StringBuilder("#Run");
            header.append(run.getRun()).append("=");
            header.append(run.getDescription()).append("|");
            header.append(run.getFileName()).append("|");

            List<MS2Modification> mods = run.getModifications(MassType.Average);

            for (MS2Modification mod : mods)
            {
                if (mod != mods.get(0))
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
