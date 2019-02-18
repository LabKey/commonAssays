/*
 * Copyright (c) 2006-2018 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.HydrophobicityColumn;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Modification;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MassType;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.SpectrumRenderer;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractMS2RunView<WebPartType extends WebPartView>
{
    private static Logger _log = Logger.getLogger(AbstractMS2RunView.class);

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

    public WebPartType createGridView(MS2Controller.RunForm form)
    {
        return createGridView(form.getExpanded(), true);
    }

    public abstract WebPartType createGridView(boolean expanded, boolean allowNesting);

    public abstract GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException;

    public abstract void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries);

    public abstract SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form);

    public abstract Map<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run);

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
