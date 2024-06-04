/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import com.google.common.primitives.ImmutableLongArray;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.NestedRenderContext;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.ProteinDictionaryHelpers;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.GridView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.HydrophobicityColumn;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Modification;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MassType;
import org.labkey.ms2.PeptideManager;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.SpectrumIterator;
import org.labkey.ms2.SpectrumRenderer;
import org.labkey.api.protein.go.GoLoader;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class AbstractMS2RunView
{
    private static final Logger _log = LogManager.getLogger(AbstractMS2RunView.class);

    private final Container _container;
    private final User _user;
    protected final ActionURL _url;
    protected final ViewContext _viewContext;
    protected final MS2Run[] _runs;

    public AbstractMS2RunView(ViewContext viewContext, MS2Run... runs)
    {
        _container = viewContext.getContainer();
        _user = viewContext.getUser();
        _url = viewContext.getActionURL();
        _viewContext = viewContext;
        _runs = runs;
    }

    public AbstractMS2QueryView createGridView(MS2Controller.RunForm form)
    {
        return createGridView(form.getExpanded(), false);
    }

    public abstract AbstractMS2QueryView createGridView(boolean expanded, boolean forExport);

    public abstract GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException;

    public abstract void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries);

    public abstract SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form);

    public Container getContainer()
    {
        return _container;
    }

    protected ActionURL getAJAXNestedGridURL()
    {
        ActionURL groupURL = _url.clone();
        groupURL.setAction(MS2Controller.GetProteinGroupingPeptidesAction.class);
        groupURL.deleteParameter("proteinGroupingId");

        return groupURL.addParameter("proteinGroupingId", null);
    }
    
    protected ButtonBar createButtonBar(String whatWeAreSelecting, DataRegion dataRegion)
    {
        ButtonBar result = new ButtonBar();

        List<MS2ExportType> exportFormats = getExportTypes();

        ActionURL exportUrl = _url.clone();
        exportUrl.setAction(MS2Controller.ExportAllPeptidesAction.class);
        MenuButton exportAll = new MenuButton("Export All");
        for (MS2ExportType exportFormat : exportFormats)
        {
            exportUrl.replaceParameter("exportFormat", exportFormat.name());
            NavTree menuItem = exportAll.addMenuItem(exportFormat.toString(), dataRegion.getJavascriptFormReference() + ".action=\"" + exportUrl.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
            if (exportFormat.getDescription() != null)
            {
                menuItem.setDescription(exportFormat.getDescription());
            }
        }
        result.add(exportAll);

        MenuButton exportSelected = new MenuButton("Export Selected");
        exportUrl.setAction(MS2Controller.ExportSelectedPeptidesAction.class);
        exportSelected.setRequiresSelection(true);
        for (MS2ExportType exportFormat : exportFormats)
        {
            if (exportFormat.supportsSelectedOnly())
            {
                exportUrl.replaceParameter("exportFormat", exportFormat.name());
                NavTree menuItem = exportSelected.addMenuItem(exportFormat.toString(), "if (verifySelected(" + dataRegion.getJavascriptFormReference() + ", \"" + exportUrl.getLocalURIString() + "\", \"post\", \"" + whatWeAreSelecting + "\")) { " + dataRegion.getJavascriptFormReference() + ".submit(); }");
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
                goButton.addMenuItem(goType.toString(), dataRegion.getJavascriptFormReference() + ".action=\"" + url.getLocalURIString() + "\"; " + dataRegion.getJavascriptFormReference() + ".submit();");
            }
            result.add(goButton);
        }

        return result;
    }

    protected abstract List<MS2ExportType> getExportTypes();

    protected User getUser()
    {
        return _user;
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

    public ImmutableLongArray getPeptideIndex(ActionURL url)
    {
        return MS2Manager.getPeptideIndex(getIndexCacheKey(url), (key, argument) -> generatePeptideIndex(url));
    }

    // Generate signature used to cache & retrieve the peptide index 
    protected String getIndexCacheKey(ActionURL url)
    {
        return "Filter:" + getPeptideFilter(url).toSQLString(MS2Manager.getSqlDialect()) + "|Sort:" + getPeptideSort().getSortText();
    }

    private Sort getPeptideSort()
    {
        return new Sort(_url, MS2Manager.getDataRegionNamePeptides());
    }

    private SimpleFilter getPeptideFilter(ActionURL url)
    {
        return PeptideManager.getPeptideFilter(url, PeptideManager.ALL_FILTERS, getUser(), _runs[0]);
    }

    protected ImmutableLongArray generatePeptideIndex(ActionURL url)
    {
        Sort sort = PeptideManager.getPeptideBaseSort();
        sort.insertSort(getPeptideSort());
        sort = PeptideManager.reduceToValidColumns(sort, MS2Manager.getTableInfoPeptides());

        SimpleFilter filter = getPeptideFilter(url);
        filter = PeptideManager.reduceToValidColumns(filter, MS2Manager.getTableInfoPeptides());

        return ImmutableLongArray.copyOf(new TableSelector(MS2Manager.getTableInfoPeptides().getColumn("RowId"), filter, sort).getCollection(Long.class));
    }

    protected MS2Run getSingleRun()
    {
        if (_runs.length > 1)
        {
            throw new UnsupportedOperationException("Not supported for multiple runs");
        }
        return _runs[0];
    }

    public Map<String, SimpleFilter> getFilter(ActionURL queryUrl)
    {
        NestableQueryView queryView = createGridView(false, false);
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        return Collections.singletonMap("Filter", context.buildFilter(tinfo, Collections.emptyList(), queryUrl, queryView.getDataRegionName(), Table.ALL_ROWS, Table.NO_OFFSET, sort));
    }

    public void exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        createGridView(form.getExpanded(), true).exportToExcel(response, selectedRows);
    }


    public void exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException
    {
        AbstractMS2QueryView gridView = createGridView(form.getExpanded(), true);
        gridView.createRowIdFragment(selectedRows);
        gridView.exportToTSV(response, headers);
    }

    public void exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        AbstractMS2QueryView ms2QueryView = createGridView(form.getExpanded(), true);
        ms2QueryView.createRowIdFragment(selectedRows);

        List<FieldKey> keys = new ArrayList<>();
        keys.add(FieldKey.fromParts("Fraction", "Run", "Run"));
        keys.add(FieldKey.fromParts("Fraction", "Fraction"));
        keys.add(FieldKey.fromParts("Mass"));
        keys.add(FieldKey.fromParts("Scan"));
        keys.add(FieldKey.fromParts("RetentionTime"));
        keys.add(FieldKey.fromParts("H"));
        keys.add(FieldKey.fromParts("PeptideProphet"));
        keys.add(FieldKey.fromParts("Peptide"));
        ms2QueryView.setOverrideColumns(keys);

        ms2QueryView.exportToTSV(response, getAMTFileHeader());
    }

    public void exportSpectra(MS2Controller.ExportForm form, ActionURL currentURL, SpectrumRenderer spectrumRenderer, List<String> exportRows) throws IOException, RunListException
    {
        List<MS2Run> runs = form.validateRuns();

        // Choose a different iterator based on whether this is a nested view that may include protein group criteria
        NestableQueryView queryView = createGridView(form);
        SQLFragment sql = generateSubSelect(queryView, currentURL, exportRows, FieldKey.fromParts("RowId")).second;
        try (SpectrumIterator iter = new QueryResultSetSpectrumIterator(runs, sql))
        {
            spectrumRenderer.render(iter);
            spectrumRenderer.close();
        }
    }

    /** Generate the SELECT SQL to get a particular FieldKey, respecting the filters and other config on the URL */
    protected Pair<ColumnInfo, SQLFragment> generateSubSelect(NestableQueryView queryView, ActionURL currentURL, @Nullable List<String> selectedIds, FieldKey desiredFK)
    {
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        SimpleFilter filter;
        if (context instanceof NestedRenderContext)
        {
            filter = ((NestedRenderContext)context).buildFilter(tinfo, Collections.emptyList(), currentURL, queryView.getDataRegionName(), Table.ALL_ROWS, Table.NO_OFFSET, sort, true);
        }
        else
        {
            filter = context.buildFilter(tinfo, Collections.emptyList(), currentURL, queryView.getDataRegionName(), Table.ALL_ROWS, Table.NO_OFFSET, sort);
        }
        addSelectionFilter(selectedIds, queryView, filter);

        ColumnInfo desiredCol = QueryService.get().getColumns(tinfo, Collections.singletonList(desiredFK)).get(desiredFK);
        if (desiredCol == null)
        {
            throw new IllegalArgumentException("Couldn't find column " + desiredFK + " in table " + tinfo);
        }

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(desiredCol);

        QueryService.get().ensureRequiredColumns(tinfo, columns, filter, sort, new HashSet<>());

        SQLFragment sql = QueryService.get().getSelectSQL(tinfo, columns, filter, sort, Table.ALL_ROWS, Table.NO_OFFSET, false);
        return new Pair<>(desiredCol, sql);
    }

    /** Add a filter for any selection the user might have made. The type of selection depends on the type of view (peptides/protein groups/search engine protein) */
    private void addSelectionFilter(@Nullable List<String> exportRows, NestableQueryView queryView, SimpleFilter filter)
    {
        if (exportRows != null)
        {
            List<Integer> rowIds = parseIds(exportRows);
            FieldKey selectionFK;
            QueryNestingOption nestingOption = queryView.getSelectedNestingOption();
            if (nestingOption != null)
            {
                // We're nested, so the selection key is going to be at the protein or protein group level
                selectionFK = nestingOption.getAggregateRowIdFieldKey();
            }
            else
            {
                // No nesting, so the selection key will just be the peptide's RowId
                selectionFK = FieldKey.fromParts("RowId");
            }
            filter.addClause(new SimpleFilter.InClause(selectionFK, rowIds));
        }
    }

    /**
     * Convert from Strings to Integers
     * @throws NotFoundException if there's an unparseable value
     */
    private List<Integer> parseIds(List<String> exportRows)
    {
        List<Integer> rowIds = new ArrayList<>(exportRows.size());
        for (String exportRow : exportRows)
        {
            try
            {
                rowIds.add(Integer.parseInt(exportRow));
            }
            catch (NumberFormatException e)
            {
                throw new NotFoundException("Invalid selection: " + exportRow);
            }
        }
        return rowIds;
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

    public abstract class AbstractMS2QueryView extends NestableQueryView
    {
        protected SimpleFilter.FilterClause _selectedRowsClause;

        public AbstractMS2QueryView(UserSchema schema, QuerySettings settings, boolean expanded, boolean forExport, QueryNestingOption... queryNestingOptions)
        {
            super(schema, settings, expanded, forExport, queryNestingOptions);

            setViewItemFilter((type, label) -> SingleMS2RunRReport.TYPE.equals(type));
        }

        @Override
        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);
            ButtonBar bb = createButtonBar("peptides", view.getDataRegion());
            for (DisplayElement element : bb.getList())
            {
                bar.add(element);
            }
        }

        public void exportToTSV(HttpServletResponse response, List<String> headers) throws IOException
        {
            getSettings().setMaxRows(Table.ALL_ROWS);

            try (TSVGridWriter tsvWriter = getTsvWriter())
            {
                tsvWriter.setColumnHeaderType(ColumnHeaderType.Caption);
                tsvWriter.setFileHeader(headers);
                tsvWriter.write(response);
            }
        }

        public void exportToExcel(HttpServletResponse response, List<String> selectedRows) throws IOException
        {
            createRowIdFragment(selectedRows);
            getSettings().setMaxRows(ExcelWriter.ExcelDocumentType.xlsx.getMaxRows());
            exportToExcel(response);
        }

        protected void createRowIdFragment(List<String> selectedRows)
        {
            if (selectedRows != null)
            {
                List<Integer> parsedSelection = new ArrayList<>();
                for (String selectedRow : selectedRows)
                {
                    Integer row = Integer.valueOf(selectedRow);
                    parsedSelection.add(row);
                }

                // Don't used _selectedNestingOption one because we want to export as if we're a simple flat view
                QueryNestingOption nesting = determineNestingOption();
                FieldKey column = nesting == null ? FieldKey.fromParts("RowId") : nesting.getRowIdFieldKey();
                _selectedRowsClause = new SimpleFilter.InClause(column, parsedSelection);
            }
        }

        @Override
        public DataView createDataView()
        {
            DataView result = super.createDataView();
            SimpleFilter filter = new SimpleFilter(result.getRenderContext().getBaseFilter());

            if (_selectedRowsClause != null)
            {
                filter.addClause(_selectedRowsClause);
            }

            filter.addAllClauses(PeptideManager.getPeptideFilter(_url, PeptideManager.EXTRA_FILTER | PeptideManager.PROTEIN_FILTER, getUser(), _runs));
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }
    }
}
