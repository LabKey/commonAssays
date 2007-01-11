/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package MS2;

import cpas.ms2.compare.*;
import cpas.ms2.peptideview.*;
import cpas.ms2.pipeline.ProteinProphetPipelineJob;
import jxl.write.WritableWorkbook;
import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMapping;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.ms2.*;
import org.labkey.api.ms2.pipeline.MS2PipelineManager;
import org.labkey.api.ms2.pipeline.MascotClientImpl;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.pipeline.PipelineStatusManager;
import org.labkey.api.protein.*;
import org.labkey.api.protein.tools.NullOutputStream;
import org.labkey.api.protein.tools.PieJChartHelper;
import org.labkey.api.protein.tools.ProteinDictionaryHelpers;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.fhcrc.cpas.tools.MS2Modification;
import org.fhcrc.cpas.tools.PeptideProphetSummary;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.jfree.chart.imagemap.ImageMapUtilities;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.List;


@Jpf.Controller
public class MS2Controller extends ViewController
{
    private static final int MAX_INSERTIONS_DISPLAY_ROWS = 1000; // Limit annotation table insertions to 1000 rows

    private static Logger _log = Logger.getLogger("org.labkey.api." + MS2Controller.class);
    private static final String MS2_VIEWS_CATEGORY = "MS2Views";

    private static final String CAPTION_SCORING_BUTTON = "Compare Scoring";


    private ButtonBar getListButtonBar(Container c)
    {
        ButtonBar bb = new ButtonBar();

        addSelectAndClearButtons(bb);

        ActionButton compareProteins = new ActionButton("button", "Compare Proteins");
        compareProteins.setScript("return verifySelected(this.form, \"compareProteins.view\", \"post\", \"runs\")");
        compareProteins.setActionType(ActionButton.Action.GET);
        compareProteins.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareProteins);

        ActionButton compareProteinProphet = new ActionButton("button", "Compare ProteinProphet");
        compareProteinProphet.setScript("return verifySelected(this.form, \"compareProteinProphetProteins.view\", \"post\", \"runs\")");
        compareProteinProphet.setActionType(ActionButton.Action.GET);
        compareProteinProphet.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareProteinProphet);

        ActionButton comparePeptides = new ActionButton("button", "Compare Peptides");
        comparePeptides.setScript("return verifySelected(this.form, \"comparePeptides.view\", \"post\", \"runs\")");
        comparePeptides.setActionType(ActionButton.Action.GET);
        comparePeptides.setDisplayPermission(ACL.PERM_READ);
        bb.add(comparePeptides);

        ActionButton compareScoring = new ActionButton("", CAPTION_SCORING_BUTTON);
        compareScoring.setScript("return verifySelected(this.form, \"" + ViewURLHelper.toPathString("MS2-Scoring", "compare", c.getPath())+ "\", \"get\", \"runs\")");
        compareScoring.setActionType(ActionButton.Action.GET);
        compareScoring.setDisplayPermission(ACL.PERM_READ);
        compareScoring.setVisible(false);   // Hidden unless turned on during grid rendering.
        bb.add(compareScoring);

        ActionButton exportRuns = new ActionButton("button", "Export Runs");
        exportRuns.setScript("return verifySelected(this.form, \"pickExportRunsView.view\", \"post\", \"runs\")");
        exportRuns.setActionType(ActionButton.Action.GET);
        exportRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(exportRuns);

        ActionButton showHierarchy = new ActionButton("showHierarchy.view", "Show Hierarchy");
        showHierarchy.setActionType(ActionButton.Action.LINK);
        showHierarchy.setDisplayPermission(ACL.PERM_READ);
        bb.add(showHierarchy);

        ActionButton showManage = new ActionButton("showManage.view", "Manage Runs");
        showManage.setActionType(ActionButton.Action.GET);
        showManage.setDisplayPermission(ACL.PERM_INSERT | ACL.PERM_DELETE);
        bb.add(showManage);

        return bb;
    }


    private ButtonBar getManageButtonBar()
    {
        ButtonBar bb = new ButtonBar();

        addSelectAndClearButtons(bb);

        ActionButton moveRuns = new ActionButton("", "Move Runs");
        moveRuns.setScript("return verifySelected(this.form, \"selectMoveLocation.view\", \"get\", \"runs\")");
        moveRuns.setActionType(ActionButton.Action.GET);
        moveRuns.setDisplayPermission(ACL.PERM_DELETE);
        bb.add(moveRuns);

        ActionButton deleteRuns = new ActionButton("", "Delete Runs");
        deleteRuns.setScript("return verifySelected(this.form, \"deleteRuns.view\", \"get\", \"runs\")");
        deleteRuns.setActionType(ActionButton.Action.GET);
        deleteRuns.setDisplayPermission(ACL.PERM_DELETE);
        bb.add(deleteRuns);

        ActionButton addRun = new ActionButton("showAddRun.view", "Add Run");
        addRun.setActionType(ActionButton.Action.LINK);
        addRun.setDisplayPermission(ACL.PERM_ADMIN);
        bb.add(addRun);

        ActionButton showManage = new ActionButton("showList.view", "Show Runs");
        showManage.setActionType(ActionButton.Action.GET);
        showManage.setDisplayPermission(ACL.PERM_READ);
        bb.add(showManage);

        return bb;
    }


    private void addSelectAndClearButtons(ButtonBar bb)
    {
        bb.add(ActionButton.BUTTON_SELECT_ALL);
        bb.add(ActionButton.BUTTON_CLEAR_ALL);
    }


    private enum Template
    {
        home, fast, print
    }


    private Forward _renderInTemplate(HttpView view, boolean fastTemplate, String title, String helpTopic, Pair<String, ViewURLHelper>... navTrailChildren) throws Exception
    {
        return _renderInTemplate(view, fastTemplate ? Template.fast : Template.home, title, helpTopic, false, navTrailChildren);
    }

    private Forward _renderInTemplate(HttpView view, Template templateType, String title, String helpTopic, boolean exploratoryFeatures, Pair<String, ViewURLHelper>... navTrailChildren) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "ms2";

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(helpTopic);
        trailConfig.setExploratoryFeatures(exploratoryFeatures);
        trailConfig.setExtraChildren(navTrailChildren);

        HttpView template = null;

        switch(templateType)
        {
            case home:
                template = new HomeTemplate(getViewContext(), view, trailConfig);
                break;

            case fast:
                template = new FastTemplate(getViewContext(), view, trailConfig);
                break;

            case print:
                template = new PrintTemplate(getViewContext(), view, title);
                break;
        }

        return includeView(template);
    }

    private boolean isAuthorized(int runId) throws ServletException
    {
        String message = null;
        boolean success = false;

        Container c = getContainer();
        MS2Run run = MS2Manager.getRun(runId);

        if (null == run)
            message = "Run not found";
        else if (run.isDeleted())
            message = "Run has been deleted.";
        else if (run.getStatusId() == CometImporter.STATUS_RUNNING)
            message = "Run is still loading.  Current status: " + run.getStatus();
        else if (run.getStatusId() == CometImporter.STATUS_FAILED)
            message = "Run failed loading.  Status: " + run.getStatus();
        else
        {
            String cId = run.getContainer();

            if (null != cId && cId.equals(c.getId()))
                success = true;
            else
                message = "No permission";
        }

        if (null != message)
        {
            try
            {
                _renderError(message);
            }
            catch (Exception x)
            {
                _log.error(null, x);
            }
        }

        return success;
    }


    private Forward _renderErrors(List<String> messages) throws Exception
    {
        StringBuilder sb = new StringBuilder("<table class=\"DataRegion\">");

        for (String message : messages)
        {
            sb.append("<tr><td>");
            sb.append(message);
            sb.append("</td></tr>");
        }
        sb.append("</table>");
        HtmlView view = new HtmlView(sb.toString());
        return _renderInTemplate(view, false, "Error", null,
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    private Forward _renderError(String message) throws Exception
    {
        List<String> messages = new ArrayList<String>(1);
        messages.add(message);
        return _renderErrors(messages);
    }


    @Jpf.Action
    protected Forward begin() throws Exception
    {
        ViewURLHelper url = cloneViewURLHelper().setAction("showList");
        return new ViewForward(url);
    }

    public static class ImportProteinProphetForm extends ViewForm
    {
        protected String _path;

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }
    }


    @Jpf.Action
    protected Forward importProteinProphet(ImportProteinProphetForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_INSERT);
        PipelineService service = PipelineService.get();

        File f = null;
        String path = form.getPath();
        if (path != null)
        {
            PipelineService.PipeRoot pr = service.findPipelineRoot(c);
            if (pr != null)
            {
                URI uriData = URIUtil.resolve(pr.getUri(c), path);
                f = new File(uriData);
            }
        }


        if (null != f && f.exists() && f.isFile())
        {
            ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);
            ProteinProphetPipelineJob job = new ProteinProphetPipelineJob(info, f);
            service.queueJob(job);

            // Be sure to use the job's container for forward.
            c = info.getContainer();
        }
        else
        {
            throw new FileNotFoundException("Unable to open the file '" + form.getPath() + "' to load as a ProteinProphet file");
        }

        ViewURLHelper url = new ViewURLHelper(getRequest(), "Project", "begin", c.getPath());
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward showList() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        return showRuns(getListButtonBar(getContainer()), "MS2 Runs", "ms2RunsList");
    }


    @Jpf.Action
    protected Forward showManage() throws Exception
    {
        requiresPermission(ACL.PERM_INSERT | ACL.PERM_DELETE);

        return showRuns(getManageButtonBar(), "Manage Runs", "ms2RunsList");
    }

    private static class HideShowScoringColumn extends SimpleDisplayColumn
    {
        private ActionButton btnScoring;

        public HideShowScoringColumn(ButtonBar bb)
        {
            List<DisplayElement> buttons = bb.getList();
            for (DisplayElement button : buttons)
            {
                if (CAPTION_SCORING_BUTTON.equals(button.getCaption()))
                    btnScoring = (ActionButton) button;
            }
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            if (btnScoring == null)
                return;

            Map cols = ctx.getRow();
            Integer peptideCount = (Integer) cols.get("PeptideCount");
            Integer revPeptideCount = (Integer) cols.get("NegativeHitCount");

            // Show the scoring button, if one of the rows contains over 50% reversed peptides.
            if (revPeptideCount.intValue() > peptideCount / 3)
                btnScoring.setVisible(true);
        }
    }

    private Forward showRuns(ButtonBar bb, String pageName, String helpTopic) throws Exception
    {
        ViewURLHelper currentUrl = cloneViewURLHelper();
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
        rgn.setColumns(MS2Manager.getTableInfoExperimentRuns().getColumns("Description, Path, Created, Status, ExperimentRunLSID, ExperimentRunRowId, ProtocolName, PeptideCount, NegativeHitCount"));
        for (int i = 4; i < rgn.getDisplayColumnList().size(); i++)
            rgn.getDisplayColumn(i).setVisible(false);

        ExperimentInfoColumns expColumns = new ExperimentInfoColumns(getContainer());
        rgn.addColumn(expColumns.getSampleColumn());
        rgn.addColumn(expColumns.getProtocolColumn());

        rgn.addColumn(new HideShowScoringColumn(bb));

        currentUrl.setAction("showRun");
        currentUrl.deleteParameters();
        rgn.getDisplayColumn(0).setURL(currentUrl.getLocalURIString() + "&run=${Run}");
        // TODO: What does this do?
        rgn.setFixedWidthColumns(false);
        rgn.setShowRecordSelectors(true);
        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        GridView gridView = new GridView(rgn);
        gridView.setTitle(pageName);
        gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
        gridView.setSort(MS2Manager.getRunsBaseSort());

        return _renderInTemplate(gridView, true, pageName, helpTopic);
    }


    @Jpf.Action
    protected Forward getProteinGroupingPeptides(RunForm form) throws Exception
    {
        MS2Run run = MS2Manager.getRun(form.getRun());

        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        GridView gridView = peptideView.getPeptideViewForProteinGrouping(form.getProteinGroupingId(), form.getColumns());
        return includeView(gridView);
    }


    @Jpf.Action
    protected Forward applyRunView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // Forward with redirect: this lets struts fill in the form and ensures that the JavaScript sees the showRun action
        return new ViewForward(getApplyViewForwardUrl(form, "showRun"), true);
    }


    @Jpf.Action
    protected Forward showRun(RunForm form) throws Exception
    {
        long time = System.currentTimeMillis();
        requiresPermission(ACL.PERM_READ);

        if (form.getErrors().size() > 0)
            return _renderErrors(form.getErrors());

        if (!isAuthorized(form.run))
            return null;

        ViewURLHelper currentUrl = getViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);

        VBox vBox = new VBox();
        vBox.addView(new GroovyView("/MS2/nestedGridScript.gm"));
        VelocityView runSummary = new VelocityView("runSummary.vm");
        runSummary.addObject("run", run);
        runSummary.addObject("modHref", modificationHref(run));
        runSummary.addObject("writePermissions", getViewContext().hasPermission(ACL.PERM_UPDATE));
        runSummary.addObject("quantAlgorithm", MS2Manager.getQuantAnalysisAlgorithm(form.run));
        vBox.addView(runSummary);

        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);
        vBox.addView(createHeader(currentUrl, form, run, peptideView));

        GridView grid = peptideView.getGridView(form);

        boolean exploratoryFeatures =
        (
            null != grid.getDataRegion().getDisplayColumn("H") ||
            null != grid.getDataRegion().getDisplayColumn("DeltaScan") ||
            run instanceof XTandemRun && run.getHasPeptideProphet() && null != grid.getDataRegion().getDisplayColumn("PeptideProphet")
        );

        vBox.addView(grid);
        _log.info("Lookup took " + (System.currentTimeMillis() - time) + " milliseconds");
        time = System.currentTimeMillis();
        _renderInTemplate(vBox, Template.fast, run.getDescription(), "viewRun", exploratoryFeatures,
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
        _log.info("Render took " + (System.currentTimeMillis() - time) + " milliseconds");
        return null;
    }


    public static class RenameForm extends FormData
    {
        private int run;
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public int getRun()
        {
            return run;
        }

        public void setRun(int run)
        {
            this.run = run;
        }
    }

    @Jpf.Action
    protected Forward renameRun(RenameForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        MS2Run run = MS2Manager.getRun(form.getRun());
        String description = form.getDescription();
        if (description == null || description.length() == 0)
            description = run.getDescription();

        VelocityView view = new VelocityView("renameRun.vm");
        view.addObject("run", run);
        view.addObject("description", description);

        return _renderInTemplate(view, true, "Rename Run", null);
    }

    @Jpf.Action
    protected Forward rename(RenameForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        MS2Manager.renameRun(form.getRun(), form.getDescription());

        ViewURLHelper url = new ViewURLHelper(getRequest(), "MS2", "showRun", getViewURLHelper().getExtraPath());
        url.addParameter("run", Integer.toString(form.getRun()));
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward discriminateScore(RenameForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper url = new ViewURLHelper(getRequest(), "MS2-Scoring", "discriminate", getViewURLHelper().getExtraPath());
        url.addParameter("runId", Integer.toString(form.getRun()));
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward showFullMaterialList(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        final MS2Run run = MS2Manager.getRun(form.getRun());
        if (run == null)
        {
            return HttpView.throwNotFound();
        }
        ExperimentInfoColumns cols = new ExperimentInfoColumns(getContainer());
        final ExperimentInfoColumns.SampleColumn sampleColumn = cols.getSampleColumn();

        HttpView view = new HttpView()
        {
            @Override
            protected void renderInternal(Object model, PrintWriter out) throws Exception
            {
                sampleColumn.calculateAndRenderLinks(run.getExperimentRunLSID(), cloneViewURLHelper(), run.getRun(), out, false);
            }
        };
        return renderInTemplate(view, getContainer(), "Samples for " + run.getDescription());
    }

    private HttpView createHeader(ViewURLHelper currentUrl, RunForm form, MS2Run run, AbstractPeptideView peptideView) throws ServletException, SQLException
    {
        String chargeFilterParamName = run.getChargeFilterParamName();

        VelocityView headerView = new VelocityView("filterHeader.vm");
        headerView.addObject("run", run);

        Map<String, String> urlMap = new HashMap<String, String>();

        ViewURLHelper applyViewUrl = clearFilter(currentUrl).setAction("applyRunView");
//        applyViewUrl.addParameter("action", "showRun");
        urlMap.put("applyView", applyViewUrl.getEncodedLocalURIString());
        headerView.addObject("applyView", renderViewSelect(0, true, ACL.PERM_READ, true));

        ViewURLHelper cloneUrl = currentUrl.clone().setAction("pickName");
        urlMap.put("saveView", cloneUrl.getEncodedLocalURIString());

        urlMap.put("manageViews", cloneUrl.setAction("manageViews").getEncodedLocalURIString());
        urlMap.put("pickPeptideColumns", cloneUrl.setAction("pickPeptideColumns").getEncodedLocalURIString());
        urlMap.put("pickProteinColumns", cloneUrl.setAction("pickProteinColumns").getEncodedLocalURIString());

        cloneUrl.deleteParameter("expanded");
        cloneUrl.deleteParameter("grouping");
        cloneUrl.setAction("showRun");
        String grouping = form.getGrouping();
        headerView.addObject("tabPeptide", renderTab(cloneUrl, "&nbsp;None&nbsp;", !"protein".equals(grouping) && !"proteinprophet".equals(grouping)));

        cloneUrl.replaceParameter("grouping", "protein");
        headerView.addObject("tabCollapsedProtein", renderTab(cloneUrl, "&nbsp;Protein&nbsp;Collapsed&nbsp;", "protein".equals(grouping) && !form.getExpanded()));

        cloneUrl.replaceParameter("expanded", "1");
        headerView.addObject("tabExpandedProtein", renderTab(cloneUrl, "&nbsp;Protein&nbsp;Expanded&nbsp;", "protein".equals(grouping) && form.getExpanded()));

        if (run.hasProteinProphet())
        {
            cloneUrl.deleteParameter("expanded");
            cloneUrl.replaceParameter("grouping", "proteinprophet");
            headerView.addObject("tabCollapsedProteinProphet", renderTab(cloneUrl, "&nbsp;Protein&nbsp;Prophet&nbsp;Collapsed&nbsp;", "proteinprophet".equals(grouping) && !form.getExpanded()));
            cloneUrl.replaceParameter("expanded", "1");
            headerView.addObject("tabExpandedProteinProphet", renderTab(cloneUrl, "&nbsp;Protein&nbsp;Prophet&nbsp;Expanded&nbsp;", "proteinprophet".equals(grouping) && form.getExpanded()));
        }

        ViewURLHelper extraFilterUrl = currentUrl.clone().setAction("addExtraFilter.post");
        extraFilterUrl.deleteParameter(chargeFilterParamName + "1");
        extraFilterUrl.deleteParameter(chargeFilterParamName + "2");
        extraFilterUrl.deleteParameter(chargeFilterParamName + "3");
        extraFilterUrl.deleteParameter("tryptic");
        urlMap.put("extraFilter", extraFilterUrl.getEncodedLocalURIString());

        headerView.addObject("charge1", defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "1"), "0"));
        headerView.addObject("charge2", defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "2"), "0"));
        headerView.addObject("charge3", defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "3"), "0"));
        headerView.addObject("tryptic", form.tryptic);

        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, run, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

        List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
        sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilter.getFilterText(MS2Manager.getSqlDialect())));
        sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText(MS2Manager.getSqlDialect())));

        peptideView.addSQLSummaries(sqlSummaries);

        headerView.addObject("sqlSummaries", sqlSummaries);
        headerView.addObject("urls", urlMap);
        headerView.setTitle("View");
        return headerView;
    }

    private String defaultIfNull(String s, String def)
    {
        return (null != s ? s : def);
    }


    private StringBuffer renderTab(ViewURLHelper currentUrl, String text, boolean selected)
    {
        StringBuffer tab = new StringBuffer();

        tab.append("<a href=\"");
        tab.append(currentUrl.getEncodedLocalURIString());
        tab.append("\">");

        if (selected)
        {
            tab.append("<font style=\"background-color: #e1ecfc\">");  // #003399??
            tab.append(text);
            tab.append("</font>");
        }
        else
            tab.append(text);

        tab.append("</a>\n");

        return tab;
    }

    // Render current user's MS2Views in a list box with a submit button beside.  Size == 0 is a dropdown, Size > 0 is a
    // multi-select normal list box.  postValue determines whether to post the value or the name of selected view(s)
    //
    // Caller is responsible for wrapping this in a <form> and (if desired) a <table>

    private StringBuilder renderViewSelect(int height, boolean postValue, int sharedPerm, boolean selectCurrent) throws SQLException, ServletException
    {
        Map<String, Object> m = getViewMap(true, getContainer().hasPermission(getUser(), sharedPerm), postValue);

        StringBuilder viewSelect = new StringBuilder("<select id=\"views\" name=\"viewParams\" style=\"width:");

        if (height > 0)
        {
            viewSelect.append("400\" size=\"");
            viewSelect.append(height);
            viewSelect.append("\" multiple>\n");
        }
        else
        {
            viewSelect.append("200\">");
            viewSelect.append("\n<option value=\"\">Choose A View</option>\n");
        }

        String currentViewParams = cloneViewURLHelper().deleteParameter("run").getRawQuery();

        // Use TreeSet to sort by name
        TreeSet<String> names = new TreeSet<String>(m.keySet());

        for (String name : names)
        {
            String viewParams = (String) m.get(name);

            viewSelect.append("<option value=\"");
            viewSelect.append(PageFlowUtil.filter(viewParams));
            viewSelect.append('"');
            if (selectCurrent && viewParams.equals(currentViewParams))
                viewSelect.append(" selected");
            viewSelect.append('>');
            viewSelect.append(PageFlowUtil.filter(name));
            viewSelect.append("</option>\n");
        }

        viewSelect.append("</select>");

        return viewSelect;
    }


    private Map<String, Object> getViewMap(boolean includeUser, boolean includeShared, boolean mapValue) throws SQLException, ServletException
    {
        Map<String, Object> m = new HashMap<String, Object>();

        if (includeUser)
        {
            Map<String, Object> properties = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

            for (Map.Entry<String, Object> entry : properties.entrySet())
            {
                if (mapValue)
                    m.put(entry.getKey(), entry.getValue());
                else
                    m.put(entry.getKey(), entry.getKey());
            }
        }

        //In addition to the user views, get shared views attached to this folder
        if (includeShared)
        {
            Map<String, Object> mShared = PropertyManager.getProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY, true);
            for (Map.Entry<String, Object> entry : mShared.entrySet())
            {
                String name = entry.getKey();
                if (includeUser)
                    name += SHARED_VIEW_SUFFIX;

                if (mapValue)
                    m.put(name, entry.getValue());
                else
                    m.put(name, entry.getKey());
            }
        }

        return m;
    }


    private ViewURLHelper clearFilter(ViewURLHelper currentUrl)
    {
        ViewURLHelper newUrl = currentUrl.clone();
        String run = newUrl.getParameter("run");
        newUrl.deleteParameters();
        if (null != run)
            newUrl.addParameter("run", run);
        return newUrl;
    }


    @Jpf.Action
    protected Forward pickName(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        VelocityView pickName = new VelocityView("pickName.vm");

        ViewURLHelper returnUrl = cloneViewURLHelper().setAction("showRun");
        pickName.addObject("returnUrl", returnUrl);

        if (getContainer().hasPermission(getUser(), ACL.PERM_INSERT))
            pickName.addObject("canShare", Boolean.TRUE);

        ViewURLHelper newUrl = returnUrl.clone();
        newUrl.deleteParameter("run");
        pickName.addObject("viewParams", PageFlowUtil.filter(newUrl.getRawQuery()));

        return _renderInTemplate(pickName, true, "Save View", "viewRun",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(MS2Manager.getRun(form.run).getDescription(), returnUrl));
    }


    final static String SHARED_VIEW_SUFFIX = " (Shared)";

    @Jpf.Action
    protected Forward saveView(MS2ViewForm form) throws SQLException, ServletException, URISyntaxException
    {
        requiresPermission(ACL.PERM_READ);
        String viewParams = (null == form.getViewParams() ? "" : form.getViewParams());

        String name = form.name;
        PropertyManager.PropertyMap m;
        if (form.isShared() && getContainer().hasPermission(getUser(), ACL.PERM_INSERT))
            m = PropertyManager.getWritableProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY, true);
        else
            m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

        m.put(name, viewParams);
        PropertyManager.saveProperties(m);

        return new ViewForward(form.returnUrl);
    }


    private ViewURLHelper getApplyViewForwardUrl(MS2ViewForm form, String action)
    {
        // Add the "view params" (which were posted as a single param) to the URL params.
        ViewURLHelper forwardUrl = cloneViewURLHelper();
        forwardUrl.setRawQuery(forwardUrl.getRawQuery() + (null == form.viewParams ? "" : "&" + form.viewParams));
        return forwardUrl.setAction(action);
    }


    @Jpf.Action
    protected Forward manageViews() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        ViewURLHelper runUrl = cloneViewURLHelper().setAction("showRun");

        MS2Run run = MS2Manager.getRun(runUrl.getParameter("run"));
        if (run == null)
        {
            return HttpView.throwNotFound("Could not find run " + runUrl.getParameter("run"));
        }

        ViewURLHelper postUrl = cloneViewURLHelper();
        postUrl.setAction("deleteViews");
        postUrl.deleteParameter("x");
        postUrl.deleteParameter("y");

        VelocityView manageViews = new VelocityView("manageViews.vm");
        manageViews.addObject("postUrl", postUrl.getEncodedLocalURIString());
        manageViews.addObject("select", renderViewSelect(10, false, ACL.PERM_DELETE, false));

        return _renderInTemplate(manageViews, false, "Manage Views", "viewRun",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(run.getDescription(), runUrl));
    }


    @Jpf.Action
    protected Forward deleteViews() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        List<String> viewNames = getViewContext().getList("viewParams");

        if (null != viewNames)
        {
            PropertyManager.PropertyMap m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

            for (String viewName : viewNames)
                m.remove(viewName);

            PropertyManager.saveProperties(m);

            //NOTE: If names collide between
            //shared and user-specific view names (unlikely since we append "(Shared)" to project views
            //only the shared names will be seen and deleted. Local names ending in "(Shared)" are shadowed
            if (getContainer().hasPermission(getUser(), ACL.PERM_DELETE))
            {
                m = PropertyManager.getWritableProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY, true);

                for (String name : viewNames)
                {
                    if (name.endsWith(SHARED_VIEW_SUFFIX))
                        name = name.substring(0, name.length() - SHARED_VIEW_SUFFIX.length());

                    m.remove(name);
                }

                PropertyManager.saveProperties(m);
            }
        }

        return new ViewForward(cloneViewURLHelper().setAction("manageViews"));
    }


    /**
     * Ensure there are at least some shared views in a folder.
     * Do this by copying from root folder
     * If there are none in the root folder, load from properties file
     */
    public static void ensureViews(Container c) throws SQLException, IOException
    {
        PropertyManager.PropertyMap m = PropertyManager.getWritableProperties(0, c.getId(), MS2_VIEWS_CATEGORY, true);
        if (0 == m.size())
        {
            PropertyManager.PropertyMap mGlobal = PropertyManager.getWritableProperties(0, ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);
            if (mGlobal.size() == 0)
            {
                Properties p = new Properties();
                InputStream is = MS2Controller.class.getResourceAsStream("/cpasProperties/defaultViews.properties");
                p.load(is);
                is.close();
                for (Object prop : p.keySet())
                    mGlobal.put((String) prop, p.get(prop));

                PropertyManager.saveProperties(mGlobal);
            }

            for (Map.Entry<String, Object> entry : mGlobal.entrySet())
                m.put(entry.getKey(), entry.getValue());

            PropertyManager.saveProperties(m);
        }
    }


    @Jpf.Action
    protected Forward pickPeptideColumns(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper url = cloneViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);

        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        VelocityView pickColumns = new VelocityView("pickPeptideColumns.vm");
        pickColumns.addObject("commonColumns", run.getCommonPeptideColumnNames());
        pickColumns.addObject("proteinProphetColumns", run.getProteinProphetPeptideColumnNames());
        pickColumns.addObject("quantitationColumns", run.getQuantitationPeptideColumnNames());

        // Put a space between each name
        pickColumns.addObject("defaultColumns", peptideView.getPeptideColumnNames(null).replaceAll(" ", "").replaceAll(",", ", "));
        pickColumns.addObject("currentColumns", peptideView.getPeptideColumnNames(form.getColumns()).replaceAll(" ", "").replaceAll(",", ", "));

        url.deleteParameter("columns");

        pickColumns.addObject("queryString", PageFlowUtil.filter(url.getRawQuery()));
        url.deleteParameters().setAction("savePeptideColumns");
        pickColumns.addObject("saveUrl", url.getEncodedLocalURIString());
        pickColumns.addObject("saveDefaultUrl", url.addParameter("saveDefault", "1").getEncodedLocalURIString());
        return _renderInTemplate(pickColumns, false, "Pick Peptide Columns", "pickPeptideColumns",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(run.getDescription(), cloneViewURLHelper().setAction("showRun")));
    }

    @Jpf.Action
    protected Forward pickProteinColumns(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper url = cloneViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);
        if (run == null)
        {
            return HttpView.throwNotFound("Could not find run " + form.run);
        }

        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        VelocityView pickColumns = new VelocityView("pickProteinColumns.vm");
        pickColumns.addObject("commonColumns", MS2Run.getCommonProteinColumnNames());
        pickColumns.addObject("proteinProphetColumns", MS2Run.getProteinProphetProteinColumnNames());
        pickColumns.addObject("quantitationColumns", run.getQuantitationProteinColumnNames());

        // Put a space between each name
        pickColumns.addObject("defaultColumns", peptideView.getProteinColumnNames(null).replaceAll(" ", "").replaceAll(",", ", "));
        pickColumns.addObject("currentColumns", peptideView.getProteinColumnNames(form.getProteinColumns()).replaceAll(" ", "").replaceAll(",", ", "));

        url.deleteParameter("proteinColumns");

        pickColumns.addObject("queryString", PageFlowUtil.filter(url.getRawQuery()));
        url.deleteParameters().setAction("saveProteinColumns");
        pickColumns.addObject("saveUrl", url.getEncodedLocalURIString());
        pickColumns.addObject("saveDefaultUrl", url.addParameter("saveDefault", "1").getEncodedLocalURIString());
        return _renderInTemplate(pickColumns, false, "Pick Protein Columns", "pickProteinColumns",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(run.getDescription(), cloneViewURLHelper().setAction("showRun")));
    }


    @Jpf.Action
    protected Forward savePeptideColumns(ColumnForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper returnUrl = cloneViewURLHelper();
        returnUrl.setAction("showRun");
        returnUrl.setRawQuery(form.getQueryString());
        String columnNames = form.getColumns();
        if (columnNames == null)
        {
            columnNames = "";
        }
        columnNames = columnNames.replaceAll(" ", "");

        if (form.getSaveDefault())
        {
            MS2Run run = MS2Manager.getRun(returnUrl.getParameter("run"));
            AbstractPeptideView view = getPeptideView(returnUrl.getParameter("grouping"), run);
            view.savePeptideColumnNames(run.getType(), columnNames);
        }
        else
            returnUrl.replaceParameter("columns", columnNames);

        return new ViewForward(returnUrl);
    }


    @Jpf.Action
    protected Forward saveProteinColumns(ColumnForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper returnUrl = cloneViewURLHelper();
        returnUrl.setAction("showRun");
        returnUrl.setRawQuery(form.getQueryString());
        String columnNames = form.getColumns();
        if (columnNames == null)
        {
            columnNames = "";
        }
        columnNames = columnNames.replaceAll(" ", "");

        if (form.getSaveDefault())
        {
            MS2Run run = MS2Manager.getRun(returnUrl.getParameter("run"));
            AbstractPeptideView view = getPeptideView(returnUrl.getParameter("grouping"), run);
            view.saveProteinColumnNames(run.getType(), columnNames);
        }
        else
            returnUrl.replaceParameter("proteinColumns", columnNames);

        return new ViewForward(returnUrl);
    }


    private AbstractPeptideView getPeptideView(String grouping, MS2Run... runs) throws ServletException
    {
        return AbstractPeptideView.getPeptideView(grouping, getContainer(), getUser(), getViewURLHelper(), runs);
    }


    public long[] getPeptideIndex(ViewURLHelper currentUrl, MS2Run run) throws SQLException, ServletException
    {
        AbstractPeptideView view = getPeptideView(currentUrl.getParameter("grouping"), run);
        return view.getPeptideIndex(currentUrl);
    }


    @Jpf.Action
    protected Forward showAddRun(AddRunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_ADMIN);

        HttpView v = new GroovyView("/MS2/addRun.gm");
        v.addObject("form", form);
        return includeView(new LoginTemplate(v));
    }


    @Jpf.Action
    protected Forward processAnnots(LoadAnnotForm form) throws Exception
    {
        requiresGlobalAdmin();

        if (null != form)
        {
            String fname = form.getFileName();
            String comment = form.getComment();

            //TODO: this style of dealing with different file types must be repaired.
            if (form.getFileType().equalsIgnoreCase("uniprot"))
            {
                try
                {
                    XMLProteinLoader xpl = new XMLProteinLoader(fname);
                    xpl.setComment(comment);
                    xpl.parseInBackground();
                }
                catch (Exception e)
                {
                    _log.error("Problem loading XML protein annotations for file " + fname + ": " + e);
                    throw e;
                }
            }
            if (form.getFileType().equalsIgnoreCase("fasta"))
            {
                try
                {
                    FastaDbLoader fdbl = new FastaDbLoader(new File(fname));
                    fdbl.setComment(comment);
                    fdbl.setDefaultOrganism(form.getDefaultOrganism());
                    fdbl.setOrganismIsToGuessed(form.getShouldGuess() != null);
                    fdbl.parseInBackground();
                }
                catch (Exception e)
                {
                    _log.error("Problem loading FASTA file " + fname + ": " + e);
                }
            }
        }
        ViewURLHelper url = cloneViewURLHelper();
        url.setAction("showProteinAdmin");
        url.deleteParameters();
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward insertAnnots() throws Exception
    {
        requiresGlobalAdmin();
        HttpView v = new GroovyView("/MS2/insertAnnots.gm");
        return _renderInTemplate(v, true, "Protein Annotations Loaded", null);
    }


    @Jpf.Action
    protected Forward peptideCharts2(ChartForm form) throws Exception
    {
        List<Long> timings = new ArrayList<Long>(10);
        for (int i=0; i<10; i++)
        {
            long start = System.currentTimeMillis();
            peptideCharts(form);
            long elapsed = System.currentTimeMillis() - start;
            _log.debug(elapsed);
            timings.add(elapsed);
        }

        for (Long timing : timings)
        {
            _log.debug(form.getChartType() + " " + timing);
        }
        return null;
    }

    @Jpf.Action
    protected Forward peptideCharts(ChartForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        if (!isAuthorized(form.run))
            return null;

        ProteinDictionaryHelpers.GoTypes goChartType = ProteinDictionaryHelpers.GTypeStringToEnum(form.getChartType());
        String chartTitle = "GO " + goChartType + " Classifications";

        MS2Run run = MS2Manager.getRun(form.run);

        ViewURLHelper queryUrl = cloneViewURLHelper();
        ViewContext ctx = getViewContext();
        String queryString = (String) ctx.get("queryString");
        queryUrl.setRawQuery(queryString);
        SimpleFilter filter = ProteinManager.getPeptideFilter(queryUrl, run, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

        SQLFragment fragment = new SQLFragment();
        fragment.append("SELECT DISTINCT SeqId FROM ");
        fragment.append(MS2Manager.getTableInfoPeptides());
        fragment.append(" ");
        fragment.append(filter.getWhereSQL(ProteinManager.getSqlDialect()));

        fragment.addAll(filter.getWhereParams(MS2Manager.getTableInfoPeptides()));

        PieJChartHelper pjch = PieJChartHelper.prepareGOPie(chartTitle, fragment, goChartType);

        HttpView v = new VelocityView("/MS2/peptideChart.vm");

        String runInfo = "Run: " + run;
        v.addObject("runInfo", runInfo);
        String filterInfo = "Filter: " + filter;
        v.addObject("filterInfo", filterInfo);
        pjch.renderAsPNG(new NullOutputStream());

        v.addObject("imageMap", ImageMapUtilities.getImageMap("pie1", pjch.getChartRenderingInfo()));
        v.addObject("queryString", queryString);
        v.addObject("run", run.getRun());

        String pieHelperObjName = "piechart-" + (new Random().nextInt(1000000000));
        String listOfSlices = "?ctype=" + goChartType.toString().replace(' ', '+') + "&helpername=" + pieHelperObjName;
        v.addObject("PieSliceString", listOfSlices);
        v.addObject("PiechartHelperObj", pieHelperObjName);
        v.addObject("ChartTitle", chartTitle);
        v.addObject("GOC", ProteinDictionaryHelpers.GoTypes.CELL_LOCATION);
        v.addObject("GOF", ProteinDictionaryHelpers.GoTypes.FUNCTION);
        v.addObject("GOP", ProteinDictionaryHelpers.GoTypes.PROCESS);
        Cache.getShared().put(pieHelperObjName, pjch, Cache.HOUR * 2);

        return _renderInTemplate(v, true, "GO " + goChartType + " Chart", null);
    }


    @Jpf.Action
    protected Forward doOnePeptideChart() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        HttpServletRequest req = getRequest();
        HttpServletResponse response = getResponse();
        response.setContentType("image/png");
        OutputStream out = response.getOutputStream();

        String helperName = req.getParameter("helpername");
        PieJChartHelper pjch = (PieJChartHelper) Cache.getShared().get(helperName);

        try
        {
            pjch.renderAsPNG(out);
        }
        catch (Exception e)
        {
            _log.error("Chart rendering failed: " + e);
        }
        finally
        {
            Cache.getShared().remove(helperName);
        }

        return null;
    }


    @Jpf.Action
    public Forward pieSliceSection() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        VBox lotsOfAnnots = new VBox();
        HttpServletRequest req = getRequest();

        VelocityView vv = new VelocityView("PiesliceDetailListHeader.vm", "Definition");
        String accn = req.getParameter("sliceTitle").split(" ")[0];
        vv.addObject("sliceDefinition", ProteinDictionaryHelpers.getGODefinitionFromAcc(accn));
        lotsOfAnnots.addView(vv);
        String sqids = req.getParameter("sqids");
        String sqidArr[] = sqids.split(",");
        for (String curSqid : sqidArr)
        {
            int curSeqId = Integer.parseInt(curSqid);
            String curTitle = ProteinManager.getSeqParamFromId("BestName", curSeqId);
            lotsOfAnnots.addView(annotView(curTitle, curSeqId));
        }
        return _renderInTemplate(lotsOfAnnots, false, "Pieslice Details for: " + req.getParameter("sliceTitle"), null);
    }


    public static class DisplayThreadStatusColumn extends SimpleDisplayColumn
    {
        public DisplayThreadStatusColumn()
        {
            super("");
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            Map rowMap = ctx.getRow();
            int curId = (Integer) rowMap.get("insertId");
            if (curId <= 0) return;
            AnnotationLoader.Status curStatus;
            try
            {
                curStatus = AnnotationUploadManager.getInstance().annotThreadStatus(curId);
            }
            catch (SQLException e)
            {
                throw (IOException)new IOException().initCause(e);
            }
            String curStatusString = curStatus.toString();
            String button1 = "";
            String button2 = "";
            if (!ctx.getViewContext().getUser().isAdministrator())
            {
                out.write(curStatusString);
                return;
            }
            switch (curStatus)
            {
                case RUNNING:
                    button1 =
                            "<a href=\"annotThreadControl.view?button=kill&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Kill") + "'></a>";
                    button2 =
                            "<a href=\"annotThreadControl.view?button=pause&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Pause") + "'></a>";
                    break;
                case PAUSED:
                    button1 =
                            "<a href=\"annotThreadControl.view?button=kill&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Kill") + "'></a>";
                    button2 =
                            "<a href=\"annotThreadControl.view?button=continue&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Continue") + "'></a>";
                    break;
                case INCOMPLETE:
                    button1 =
                            "<a href=\"annotThreadControl.view?button=recover&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Recover") + "'></a>";
                    button2 = "";
                    break;
                case UNKNOWN:
                case COMPLETE:
                    button1 = "";
                    button2 = "";
                    break;
            }
            out.write(curStatusString + "&nbsp;" + button1 + "&nbsp;" + button2);
        }
    }


    private static ActionButton NewAnnot = new ActionButton("insertAnnots.post", "Load New Annot File");
    private static ActionButton ReloadSprotOrgMap = new ActionButton("reloadSPOM.post", "Reload SWP Org Map");
    private static ActionButton ReloadGO = new ActionButton("reloadGO.post", "Load/Reload GO");
    private static ButtonBar annotLoadButtons = new ButtonBar();


    static
    {
        annotLoadButtons.add(NewAnnot);
        annotLoadButtons.add(ReloadSprotOrgMap);
        annotLoadButtons.add(ReloadGO);
        assert MemTracker.remove(annotLoadButtons);
        assert MemTracker.remove(NewAnnot);
        assert MemTracker.remove(ReloadSprotOrgMap);
        assert MemTracker.remove(ReloadGO);
    }


    @Jpf.Action
    protected Forward annotThreadControl() throws Exception
    {
        requiresGlobalAdmin();
        ViewContext ctx = getViewContext();
        HttpServletRequest req = ctx.getRequest();
        String commandType = req.getParameter("button");
        int threadId = Integer.parseInt(req.getParameter("id"));

        if (commandType.equalsIgnoreCase("kill"))
        {
            AnnotationLoader annotLoader = AnnotationUploadManager.getInstance().getActiveLoader(threadId);
            annotLoader.requestThreadState(AnnotationLoader.Status.KILLED);
        }

        if (commandType.equalsIgnoreCase("pause"))
        {
            AnnotationLoader annotLoader = AnnotationUploadManager.getInstance().getActiveLoader(threadId);
            annotLoader.requestThreadState(AnnotationLoader.Status.PAUSED);
        }

        if (commandType.equalsIgnoreCase("continue"))
        {
            AnnotationLoader annotLoader = AnnotationUploadManager.getInstance().getActiveLoader(threadId);
            annotLoader.requestThreadState(AnnotationLoader.Status.RUNNING);
        }

        if (commandType.equalsIgnoreCase("recover"))
        {
            String fnameToRecover = Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileName FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + threadId, null, String.class);
            String ftypeToRecover = Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileType FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + threadId, null, String.class);
            //TODO:  Major kludge.  This will have to be done correctly.  Possibly with generics, which is what they're for
            if (ftypeToRecover.equalsIgnoreCase("uniprot"))
            {
                XMLProteinLoader xpl = new XMLProteinLoader(fnameToRecover);
                xpl.parseInBackground(threadId);
                Thread.sleep(2001);
            }
            if (ftypeToRecover.equalsIgnoreCase("fasta"))
            {
                FastaDbLoader fdbl = new FastaDbLoader(new File(fnameToRecover));
                fdbl.parseInBackground(threadId);
                Thread.sleep(2002);
            }
        }

        ViewURLHelper url = cloneViewURLHelper();
        url.setAction("showProteinAdmin");
        url.deleteParameters();
        return new ViewForward(url);
    }


    private DataRegion getAnnotInsertsGrid()
    {
        String columnNames = "InsertId, FileName, FileType, Comment, InsertDate, CompletionDate, RecordsProcessed";
        DataRegion rgn = new DataRegion();

        DisplayColumn threadControl1 = new DisplayThreadStatusColumn();
        threadControl1.setName("threadControl");
        threadControl1.setCaption("State");
        rgn.addColumns(ProteinManager.getTableInfoAnnotInsertions(), columnNames);
        rgn.getDisplayColumn("fileType").setWidth("20");
        rgn.getDisplayColumn("insertId").setCaption("ID");
        rgn.getDisplayColumn("insertId").setWidth("5");
        ViewURLHelper showUrl = cloneViewURLHelper();
        showUrl.setAction("showAnnotInsertDetails");
        showUrl.deleteParameters();
        String detailUrl = showUrl.getLocalURIString() + "insertId=${InsertId}";
        rgn.getDisplayColumn("insertId").setURL(detailUrl);
        rgn.addColumn(threadControl1);
        rgn.setMaxRows(MAX_INSERTIONS_DISPLAY_ROWS);
        rgn.setShowRecordSelectors(true);

        ButtonBar bb = new ButtonBar();
        bb.add(NewAnnot);
        bb.add(ReloadSprotOrgMap);
        ReloadGO.setScript("alert(\"Note: Gene Ontologies are large.  This takes some time,\\nit will loaded in the background.\");this.form.action=\"reloadGO.post\";this.form.method=\"post\";return true");
        ReloadGO.setActionType(ActionButton.Action.GET);
        bb.add(ReloadGO);
        addSelectAndClearButtons(bb);

        ActionButton delete = new ActionButton("", "Delete Selected");
        delete.setScript("alert(\"Note: this will not delete actual annotations,\\njust the entries on this list.\"); return verifySelected(this.form, \"deleteAnnotInsertEntries.post\", \"post\", \"annotations\")");
        delete.setActionType(ActionButton.Action.GET);
        bb.add(delete);

        rgn.setButtonBar(bb);
        return rgn;
    }


    @Jpf.Action
    protected Forward reloadSPOM() throws Exception
    {
        requiresGlobalAdmin();

        ProteinDictionaryHelpers.loadProtSprotOrgMap();
        ViewURLHelper currentUrl = cloneViewURLHelper();
        currentUrl.deleteParameters();
        currentUrl.setAction("showProteinAdmin");
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward reloadGO() throws Exception
    {
        _log.debug("Entering reloadGO");
        requiresGlobalAdmin();
        JobRunner.getDefault().execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    ProteinManager.clearGoLoaded();
                    ProteinDictionaryHelpers.loadGo();
                }
                catch (Exception e)
                {
                    _log.error("Couldn't load Gene Ontology", e);
                }
            }
        }
        );
        ViewURLHelper currentUrl = cloneViewURLHelper();
        currentUrl.deleteParameters();
        currentUrl.setAction("showProteinAdmin");
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward deleteAnnotInsertEntries() throws Exception
    {
        requiresGlobalAdmin();

        ViewURLHelper currentUrl = cloneViewURLHelper();
        HttpServletRequest request = getRequest();
        String[] deleteAIEs = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        String idList = StringUtils.join(deleteAIEs, ',');
        Table.execute(ProteinManager.getSchema(), "DELETE FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId in (" + idList + ")", null);
        currentUrl.deleteParameters();
        currentUrl.setAction("showProteinAdmin");
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward showAnnotInserts() throws Exception
    {
        Container c = ContainerManager.getForPath("home");
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            HttpView.throwUnauthorized();

        ViewURLHelper urlhelp = cloneViewURLHelper();
        urlhelp.deleteParameters();
        urlhelp.setPageFlow("MS2").setAction("showProteinAdmin");
        DataRegion dr = getAnnotInsertsGrid();
        GridView grid = new GridView(dr);

        ViewURLHelper showUrl = cloneViewURLHelper();
        showUrl.setAction("showAnnotInsertDetails");
        String detailUrl = showUrl.getLocalURIString();

        DisplayColumn dc = dr.getDisplayColumn("InsertId");
        if (null != dc)
            dc.setURL(detailUrl);

        grid.setTitle("Protein Annotations Loaded");
        grid.getViewContext().setPermissions(ACL.PERM_READ);

        return _renderInTemplate(grid, true, "Protein Annotations Loaded", null);
    }


    VelocityView fillInInsertDetails(int insertId) throws Exception
    {
        VelocityView retVal = null;
        try
        {
            retVal = new VelocityView("annotLoadDetails.vm");
            ResultSet rs =
                    Table.executeQuery(ProteinManager.getSchema(), "SELECT * FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE insertId=" + insertId, null);
            rs.next();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            for (int i = 1; i <= colCount; i++)
            {
                String name = rsmd.getColumnName(i).toLowerCase();
                String val = rs.getString(name);
                retVal.addObject(name, val);
            }
            rs.close();
        }
        catch (Exception e)
        {
            _log.error("In fillInInsertDetails:" + e);
        }
        return retVal;
    }


    @Jpf.Action
    protected Forward showAnnotInsertDetails() throws Exception
    {
        Container c = ContainerManager.getForPath("home");
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            HttpView.throwUnauthorized();
        ViewURLHelper urlhelp = cloneViewURLHelper();
        String insertIdStr = urlhelp.getParameter("insertId");
        int insertId = Integer.parseInt(insertIdStr);
        VelocityView vv = fillInInsertDetails(insertId);
        return _renderInTemplate(vv, true, "Annotation Insertion Details", null);
    }


    public static class AddRunForm extends ViewForm
    {
        private String fileName;
        private String protocol;
        private String dataDir;
        private String description;
        private String error;
        private boolean auto;
        private boolean experiment;

        public String getFileName()
        {
            return fileName;
        }

        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public boolean isAuto()
        {
            return auto;
        }

        public void setAuto(boolean auto)
        {
            this.auto = auto;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }

        public String getProtocol()
        {
            return protocol;
        }

        public void setProtocol(String protocol)
        {
            this.protocol = protocol;
        }

        public boolean isExperiment()
        {
            return experiment;
        }

        public void setExperiment(boolean experiment)
        {
            this.experiment = experiment;
        }

        public String getDataDir()
        {
            return dataDir;
        }

        public void setDataDir(String dataDir)
        {
            this.dataDir = dataDir;
        }
    }


    @Jpf.Action
    protected Forward addRun(AddRunForm form) throws Exception
    {
        HttpServletRequest request = getRequest();
        Container c = null;
        ViewURLHelper url;
        File f = null;

        try
        {
            c = getContainer();
        }
        catch (ServletException e)
        {
            // null container handled below
        }

        if ("Show Runs".equals(getViewURLHelper().getParameter("list")))
        {
            if (c == null)
                return HttpView.throwNotFound();

            return new ViewForward(new ViewURLHelper(request, "MS2", "showList", c.getPath()));
        }

        if (null != form.getFileName())
        {
            f = new File(form.getFileName());

            if (!f.exists())
                NetworkDrive.ensureDrive(f.getPath());
        }

        if (null != f && f.exists())
        {
            if (!form.isAuto())
            {
                PipelineService service = PipelineService.get();
                ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);

//wch:mascotdev
                boolean mascotFile = "mascot".equalsIgnoreCase(MS2PipelineManager.getSearchEngine (new File(form.getFileName())));
                int run;
                if (mascotFile)
                {
                    run = MS2Manager.addMascotRunToQueue(info,
                            f, form.getDescription(), false).getRunId();
                }
                else
                {
                    run = MS2Manager.addRunToQueue(info,
                            f, form.getDescription(), false).getRunId();
                }
//END-wch:mascotdev
                if (run == -1)
                    return HttpView.throwNotFound();

                url = new ViewURLHelper(request, "MS2", "showList", c.getPath());
                url.addParameter(MS2Manager.getDataRegionNameExperimentRuns() + ".Run~eq", Integer.toString(run));
            }
            else if (!form.isExperiment())
            {
                PipelineService service = PipelineService.get();
                ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);

                int run = MS2Manager.addRunToQueue(info,
                        f, form.getDescription(), true).getRunId();
                if (run == -1)
                    HttpView.throwNotFound();

                url = new ViewURLHelper(request, "MS2", "addFileRunStatus", "");
                url.addParameter("run", Integer.toString(run));
            }
            else
            {
                // Make sure container exists.
                c = ContainerManager.ensureContainer(getViewURLHelper().getExtraPath());
                if (null == c)
                    HttpView.throwNotFound();

                PipelineService service = PipelineService.get();
                PipelineService.PipeRoot pr = service.findPipelineRoot(c);
                if (pr == null)
                    return HttpView.throwUnauthorized();

                ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);
//wch:mascotdev
                PipelineJob job = MS2PipelineManager.runUpload(info,
//END-wch:mascotdev
                        pr.getUri(c),
                        new File(form.getDataDir()).toURI(),
                        MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer()),
                        form.getProtocol(),
                        f);

                if (job == null)
                    return HttpView.throwNotFound();

                url = new ViewURLHelper(request, "MS2", "addFileRunStatus", "");
                url.addParameter("path", job.getLogFile().getAbsolutePath());
            }
        }
        else if (!form.isAuto())
        {
            url = cloneViewURLHelper();
            url.setAction("showAddRun");
            url.addParameter("error", "File not found.<br>");
        }
        else
        {
            url = new ViewURLHelper(request, "MS2", "addFileRunStatus", "");
        }

        return new ViewForward(url);
    }


    public static class RunStatus
    {
        int statusId;
        String status;
        String description;
        boolean deleted;

        public int getStatusId()
        {
            return statusId;
        }

        public void setStatusId(int statusId)
        {
            this.statusId = statusId;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public boolean getDeleted()
        {
            return deleted;
        }

        public void setDeleted(boolean deleted)
        {
            this.deleted = deleted;
        }
    }

    @Jpf.Action
    protected Forward addFileRunStatus() throws Exception
    {
        String status = null;
        HttpServletResponse response = getResponse();
        response.setContentType("text/plain");

        String path = getViewURLHelper().getParameter("path");
        if (path != null)
        {
            path = PipelineStatusManager.getStatusFilePath(path);
            PipelineStatusFile sf = PipelineStatusManager.getStatusFile(path);
            if (sf == null)
                status = "ERROR->path=" + path + ",message=Job not found in database";
/*            else if (run.getDeleted())
                status = "ERROR->run=" + runId + ",message=Run deleted"; */
            else
            {
                String[] parts = (sf.getInfo() == null ?
                        new String[0] : sf.getInfo().split(","));
                StringBuffer sb = new StringBuffer(sf.getStatus());
                sb.append("->path=").append(sf.getFilePath());
                for (String part : parts)
                {
                    if (part.startsWith("path="))
                        continue;
                    sb.append(",").append(part);
                }

                status = sb.toString();
            }
        }
        else
        {
            // Old MS2-only code.  Still supports Comet searches.
            int runId = 0;
            String runParam = getViewURLHelper().getParameter("run");
            if (runParam != null)
            {
                try
                {
                    runId = Integer.parseInt(runParam);
                }
                catch (NumberFormatException e)
                {
                    _log.error(e);
                }
            }

            if (runId > 0)
            {
                TableInfo info = MS2Manager.getTableInfoRuns();
                RunStatus run = Table.selectObject(info, runId, RunStatus.class);
                if (run == null)
                    status = "ERROR->run=" + runId + ",message=Run not found in database";
                else if (run.getDeleted())
                    status = "ERROR->run=" + runId + ",message=Run deleted";
                else if (run.getStatusId() == 1)
                    status = "SUCCESS->run=" + runId;
                else if (run.getStatusId() == 2)
                    status = "FAILED->run=" + runId;
                else if (run.getStatusId() == 0)
                {
                    status = "LOADING->run=" + runId + ",status=" + run.getStatus()
                            + ",description=" + run.getDescription();
                }
            }
        }

        if (status == null)
        {
            response.setStatus(400);    // Bad request.
            status = "ERROR->File not found";
        }

        response.getWriter().println(status);

        return null;
    }


    @Jpf.Action
    protected Forward deleteRuns() throws URISyntaxException, ServletException, IOException
    {
        requiresPermission(ACL.PERM_DELETE);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        String[] deleteRuns = currentUrl.getParameters(DataRegion.SELECT_CHECKBOX_NAME);

        if (null != deleteRuns)
        {
            MS2Manager.markAsDeleted(deleteRuns, c);
        }

        currentUrl.setAction("showList");
        currentUrl.deleteParameter("x");
        currentUrl.deleteParameter("y");
        currentUrl.deleteParameter(DataRegion.SELECT_CHECKBOX_NAME);

        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward selectMoveLocation() throws URISyntaxException, ServletException
    {
        requiresPermission(ACL.PERM_DELETE);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        String[] moveRuns = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);

        currentUrl.setAction("pickMoveLocation");
        currentUrl.deleteParameters();
        if ("true".equals(getRequest().getParameter("ExperimentRunIds")))
        {
            currentUrl.addParameter("ExperimentRunIds", "true");
        }
        currentUrl.addParameter("moveRuns", StringUtils.join(moveRuns, ','));
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward pickMoveLocation() throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        currentUrl.setAction("moveRuns");
        ContainerTree ct = new ContainerTree("/", getUser(), ACL.PERM_INSERT, currentUrl);

        StringBuilder html = new StringBuilder("<table class=\"dataRegion\"><tr><td>Choose a destination folder:</td></tr><tr><td>&nbsp;</td></tr>");
        ct.render(html);
        html.append("</table>");

        return _renderInTemplate(new HtmlView(html.toString()), false, "Move Runs", "ms2RunsList",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    @Jpf.Action
    protected Forward moveRuns() throws URISyntaxException, ServletException
    {
        requiresPermission(ACL.PERM_INSERT);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        String moveRuns = currentUrl.getParameter("moveRuns");
        String[] idStrings = moveRuns.split(",");
        List<Integer> ids = new ArrayList<Integer>();
        for (String idString : idStrings)
        {
            ids.add(new Integer(idString));
        }
        List<MS2Run> runs = getRuns(ids, new ArrayList<String>(), false, false);
        MS2Manager.moveRuns(getUser(), runs, getContainer());

        currentUrl.setAction("showList");
        currentUrl.deleteParameter("moveRuns");
        return new ViewForward(currentUrl);
    }


    private static class MS2RunHierarchyTree extends ContainerTree
    {
        public MS2RunHierarchyTree(String rootPath, User user, int perm)
        {
            super(rootPath, user, perm);
        }


        public MS2RunHierarchyTree(String rootPath, User user, int perm, ViewURLHelper url)
        {
            super(rootPath, user, perm, url);
        }


        @Override
        protected void renderNode(StringBuilder html, Container parent, ViewURLHelper url, boolean isAuthorized, int level)
        {
            html.append("<tr>");
            String firstTd = "<td style=\"padding-left:" + 20 * level + "\">";
            html.append(firstTd);

            if (isAuthorized)
            {
                html.append("<a href=\"");
                url.setExtraPath(parent.getPath());
                html.append(url.getEncodedLocalURIString());
                html.append("\">");
            }

            html.append(PageFlowUtil.filter(parent.getName()));

            if (isAuthorized)
                html.append("</a>");

            html.append("</td></tr>\n");

            if (isAuthorized)
            {
                try
                {
                    ResultSet rs = Table.executeQuery(MS2Manager.getSchema(), "SELECT Run, Description, FileName FROM " + MS2Manager.getTableInfoRuns() + " WHERE Container=? AND Deleted=?", new Object[]{parent.getId(), Boolean.FALSE});

                    boolean moreRuns = rs.next();

                    if (moreRuns)
                    {
                        ViewURLHelper runUrl = url.clone();
                        runUrl.setAction("showRun");

                        html.append("<tr>");
                        html.append(firstTd);
                        html.append("<table>\n");

                        while (moreRuns)
                        {
                            int run = rs.getInt(1);
                            runUrl.replaceParameter("run", String.valueOf(run));
                            html.append("<tr><td>");
                            html.append("<input type=checkbox name='");
                            html.append(DataRegion.SELECT_CHECKBOX_NAME);
                            html.append("' value='");
                            html.append(run);
                            html.append("'></td><td><a href=\"");
                            html.append(runUrl.getEncodedLocalURIString());
                            html.append("\">");
                            html.append(PageFlowUtil.filter(rs.getString(2)));
                            html.append("</a></td><td>");
                            html.append(PageFlowUtil.filter(rs.getString(3)));
                            html.append("</td></tr>\n");
                            moreRuns = rs.next();
                        }

                        html.append("</table></td></tr>\n");
                    }

                    rs.close();
                }
                catch (SQLException e)
                {
                    _log.error("renderHierarchyChildren", e);
                }
            }
        }
    }


    @Jpf.Action
    protected Forward showHierarchy() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        MS2RunHierarchyTree ht = new MS2RunHierarchyTree(currentUrl.getExtraPath(), getUser(), ACL.PERM_READ, currentUrl);

        StringBuilder html = new StringBuilder("<form method=post action=''><table class=\"dataRegion\">");
        ht.render(html);
        renderHierarchyButtonBar(html);
        html.append("</table></form>");

        return _renderInTemplate(new HtmlView(html.toString()), false, "Hierarchy", "ms2RunsList",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    private void renderHierarchyButtonBar(StringBuilder html) throws IOException
    {
        ButtonBar bb = new ButtonBar();

        ActionButton compareProteins = new ActionButton("button", "Compare Proteins");
        compareProteins.setScript("return verifySelected(this.form, \"compareProteins.view\", \"post\", \"runs\")");
        compareProteins.setActionType(ActionButton.Action.GET);
        compareProteins.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareProteins);

        ActionButton comparePeptides = new ActionButton("button", "Compare Peptides");
        comparePeptides.setScript("return verifySelected(this.form, \"comparePeptides.view\", \"post\", \"runs\")");
        comparePeptides.setActionType(ActionButton.Action.GET);
        comparePeptides.setDisplayPermission(ACL.PERM_READ);
        bb.add(comparePeptides);

        ActionButton compareProteinProphet = new ActionButton("button", "Compare ProteinProphet");
        compareProteinProphet.setScript("return verifySelected(this.form, \"compareProteinProphetProteins.view\", \"post\", \"runs\")");
        compareProteinProphet.setActionType(ActionButton.Action.GET);
        compareProteinProphet.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareProteinProphet);

        ActionButton exportRuns = new ActionButton("button", "Export Runs");
        exportRuns.setScript("return verifySelected(this.form, \"pickExportRunsView.view\", \"post\", \"runs\")");
        exportRuns.setActionType(ActionButton.Action.GET);
        exportRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(exportRuns);

        StringWriter s = new StringWriter();

        bb.render(new RenderContext(getViewContext()), s);
        html.append("<tr><td>");
        html.append(s);
        html.append("</td></tr>");
    }


    @Jpf.Action
    protected Forward editElutionGraph(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());
        Quantitation quant = peptide.getQuantitation();

        EditElutionGraphContext ctx = new EditElutionGraphContext(quant.getLightElutionProfile(peptide.getCharge()), quant.getHeavyElutionProfile(peptide.getCharge()), quant, getViewURLHelper(), peptide);
        JspView v = new JspView<EditElutionGraphContext>("/MS2/editElution.jsp", ctx);
        includeView(v);
        return null;
    }

    @Jpf.Action
    protected Forward saveElutionProfile(ElutionProfileForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());
        if (peptide == null)
        {
            throw new NotFoundException();
        }
        Quantitation quant = peptide.getQuantitation();
        if (quant == null)
        {
            throw new NotFoundException();
        }

        boolean validRanges = quant.resetRanges(form.getLightFirstScan(), form.getLightLastScan(), form.getHeavyFirstScan(), form.getHeavyLastScan(), peptide.getCharge());
        Table.update(getUser(), MS2Manager.getTableInfoQuantitation(), quant, quant.getPeptideId(), null);

        ViewURLHelper url = getViewURLHelper().clone();
        url.setAction("showPeptide.view");
        if (!validRanges)
        {
            url.addParameter("elutionProfileError", "Invalid elution profile range");
        }

        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward showGraph(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

        if (null != peptide)
        {
            HttpServletResponse response = getResponse();
            response.setDateHeader("Expires", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
            response.setHeader("Pragma", "");
            response.setContentType("image/png");
            peptide.renderGraph(response, form.getTolerance(), form.getxStart(), form.getxEnd(), form.getWidth(), form.getHeight());
        }

        return null;
    }

    protected Forward showElutionGraph(DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

        HttpServletResponse response = getResponse();
        if (null != peptide)
        {
            Quantitation quantitation = peptide.getQuantitation();
            response.setDateHeader("Expires", 0);
            response.setContentType("image/png");

            File f = quantitation.findScanFile();
            if (f != null)
            {
                ElutionGraph g = new ElutionGraph();
                int charge = form.getQuantitationCharge() == Integer.MIN_VALUE ? peptide.getCharge() : form.getQuantitationCharge();
                if (showLight)
                {
                    g.addInfo(quantitation.getLightElutionProfile(charge), quantitation.getLightFirstScan(), quantitation.getLightLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.RED);
                }
                if (showHeavy)
                {
                    g.addInfo(quantitation.getHeavyElutionProfile(charge), quantitation.getHeavyFirstScan(), quantitation.getHeavyLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.BLUE);
                }
                g.render(response.getOutputStream());
                return null;
            }
        }

        Graph g = new Graph(new float[0], new float[0], 300, ElutionGraph.HEIGHT)
        {
            protected void initializeDataPoints(Graphics2D g) {}
            protected void renderDataPoint(Graphics2D g, double x, double y) {}
        };
        g.render(response.getOutputStream());
        return null;
    }

    @Jpf.Action
    protected Forward showLightElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, true, false);
    }

    @Jpf.Action
    protected Forward showHeavyElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, false, true);
    }

    @Jpf.Action
    protected Forward showCombinedElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, true, true);
    }

    @Jpf.Action
    protected Forward showPeptide(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        ViewURLHelper currentUrl = getViewURLHelper();

        long peptideId = form.getPeptideId();
        MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

        if (null == peptide)
            return _renderError("Peptide was not found");

        int sqlRowIndex = form.getRowIndex();
        int rowIndex = sqlRowIndex - 1;  // Switch 1-based, JDBC row index to 0-based row index for array lookup

        MS2Run run = MS2Manager.getRun(form.run);
        long[] peptideIndex = getPeptideIndex(currentUrl, run);
        rowIndex = MS2Manager.verifyRowIndex(peptideIndex, rowIndex, peptideId);

        peptide.init(form.tolerance, form.xStart, form.xEnd);

        ViewURLHelper previousUrl = null;
        ViewURLHelper nextUrl = null;
        ViewURLHelper showGzUrl = null;

        // Display next and previous only if we have a cached index and a valid pointer
        if (null != peptideIndex && -1 != rowIndex)
        {
            if (0 == rowIndex)
                previousUrl = null;
            else
            {
                previousUrl = cloneViewURLHelper();
                previousUrl.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex - 1]));
                previousUrl.replaceParameter("rowIndex", String.valueOf(sqlRowIndex - 1));
            }

            if (rowIndex == (peptideIndex.length - 1))
                nextUrl = null;
            else
            {
                nextUrl = cloneViewURLHelper();
                nextUrl.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex + 1]));
                nextUrl.replaceParameter("rowIndex", String.valueOf(sqlRowIndex + 1));
            }

            showGzUrl = cloneViewURLHelper();
            showGzUrl.deleteParameter("seqId");
            showGzUrl.deleteParameter("rowIndex");
            showGzUrl.setAction("showGZFile");
        }

        ShowPeptideContext ctx = new ShowPeptideContext(form, run, peptide, currentUrl, previousUrl, nextUrl, showGzUrl, modificationHref(run), getContainer(), getUser());
        JspView v = new JspView<ShowPeptideContext>("/MS2/showPeptide.jsp", ctx);
        v.setFrame(WebPartView.FrameType.NONE);
        includeView(v);

        return null;
    }

    public static class EditElutionGraphContext
    {
        private final List<Quantitation.ScanInfo> _lightElutionProfile;
        private final List<Quantitation.ScanInfo> _heavyElutionProfile;
        private final ViewURLHelper _url;
        private final MS2Peptide _peptide;

        public float getMaxLightIntensity()
        {
            return _maxLightIntensity;
        }

        public float getMaxHeavyIntensity()
        {
            return _maxHeavyIntensity;
        }

        private float _maxLightIntensity;
        private float _maxHeavyIntensity;

        public Quantitation getQuantitation()
        {
            return _quantitation;
        }

        private Quantitation _quantitation;

        public EditElutionGraphContext(List<Quantitation.ScanInfo> lightElutionProfile, List<Quantitation.ScanInfo> heavyElutionProfile, Quantitation quant, ViewURLHelper url, MS2Peptide peptide)
        {
            _lightElutionProfile = lightElutionProfile;
            _heavyElutionProfile = heavyElutionProfile;
            _maxLightIntensity = findMaxIntensity(_lightElutionProfile);
            _maxHeavyIntensity = findMaxIntensity(_heavyElutionProfile);
            _quantitation = quant;
            _url = url;
            _peptide = peptide;
        }


        public ViewURLHelper getUrl()
        {
            return _url;
        }

        private float findMaxIntensity(List<Quantitation.ScanInfo> scanInfos)
        {
            float max = 0f;
            for (Quantitation.ScanInfo scanInfo : scanInfos)
            {
                max = Math.max(max, scanInfo.getIntensity());
            }
            return max;
        }

        private Float getProfileValue(int scan, List<Quantitation.ScanInfo> infos)
        {
            for (Quantitation.ScanInfo scanInfo : infos)
            {
                if (scanInfo.getScan() == scan)
                {
                    return new Float(scanInfo.getIntensity());
                }
            }
            return null;

        }

        public Float getLightValue(int scan)
        {
            return getProfileValue(scan, _lightElutionProfile);
        }

        public Float getHeavyValue(int scan)
        {
            return getProfileValue(scan, _heavyElutionProfile);
        }

        public List<Quantitation.ScanInfo> getLightElutionProfile()
        {
            return _lightElutionProfile;
        }

        public List<Quantitation.ScanInfo> getHeavyElutionProfile()
        {
            return _heavyElutionProfile;
        }

        public MS2Peptide getPeptide()
        {
            return _peptide;
        }
    }

    public static class ShowPeptideContext
    {
        public DetailsForm form;
        public MS2Run run;
        public final Container container;
        public final User user;
        public MS2Fraction fraction;
        public MS2Peptide peptide;
        public ViewURLHelper url;
        public ViewURLHelper previousUrl;
        public ViewURLHelper nextUrl;
        public ViewURLHelper showGzUrl;
        public String actualXStart;
        public String actualXEnd;
        public String modificationHref;

        ShowPeptideContext(DetailsForm form, MS2Run run, MS2Peptide peptide, ViewURLHelper url, ViewURLHelper previousUrl, ViewURLHelper nextUrl, ViewURLHelper showGzUrl, String modHref, Container container, User user)
        {
            this.form = form;
            this.run = run;
            this.container = container;
            this.user = user;
            this.fraction = MS2Manager.getFraction(peptide.getFraction());
            this.peptide = peptide;
            this.url = url;
            this.previousUrl = previousUrl;
            this.nextUrl = nextUrl;
            this.showGzUrl = showGzUrl;
            this.modificationHref = modHref;

            calcXRange();
        }


        private void calcXRange()
        {
            SpectrumGraph graph = new SpectrumGraph(peptide, form.width, form.height, form.tolerance, form.xStart,  form.xEnd);

            actualXStart = Formats.f0.format(graph.getXStart());
            actualXEnd = Formats.f0.format(graph.getXEnd());
        }
    }


    @Jpf.Action
    protected Forward updateShowPeptide() throws Exception
    {
        ViewContext ctx = getViewContext();

        ViewURLHelper fwdUrl = cloneViewURLHelper().setAction("showPeptide");
        String queryString = (String)ctx.get("queryString");
        fwdUrl.setRawQuery(queryString);

        String xStart = (String)ctx.get("xStart");
        String xEnd = (String)ctx.get("xEnd");

        if ("".equals(xStart))
            fwdUrl.deleteParameter("xStart");
        else
            fwdUrl.replaceParameter("xStart", xStart);

        if ("".equals(xEnd))
            fwdUrl.deleteParameter("xEnd");
        else
            fwdUrl.replaceParameter("xEnd", xEnd);

        return new ViewForward(fwdUrl);
    }


    @Jpf.Action
    protected Forward showPeptideProphetDetails(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

        JspView view = new JspView("/MS2/showPeptideProphetDetails.jsp");
        view.addObject("run", run);
        view.addObject("summary", summary);
        view.addObject("sensitivityPlotAction", "showPeptideProphetSensitivityPlot.view");

        return _renderInTemplate(view, false, "Peptide Prophet Details", null);
    }

    @Jpf.Action
    protected Forward showProteinProphetDetails(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        ProteinProphetFile summary = MS2Manager.getProteinProphetFile(form.run);

        JspView view = new JspView("/MS2/showSensitivityDetails.jsp");
        view.addObject("summary", summary);
        view.addObject("sensitivityPlotAction", "showProteinProphetSensitivityPlot.view");

        return _renderInTemplate(view, false, "Protein Prophet Details", null);
    }


    @Jpf.Action
    protected Forward showPeptideProphetSensitivityPlot(PeptideProphetForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

        PeptideProphetGraphs.renderSensitivityGraph(getResponse(), summary);

        return null;
    }

    @Jpf.Action
    protected Forward showProteinProphetSensitivityPlot(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        ProteinProphetFile summary = MS2Manager.getProteinProphetFile(form.run);

        PeptideProphetGraphs.renderSensitivityGraph(getResponse(), summary);

        return null;
    }


    @Jpf.Action
    protected Forward showPeptideProphetDistributionPlot(PeptideProphetForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

        PeptideProphetGraphs.renderDistribution(getResponse(), summary, form.charge, form.cumulative);

        return null;
    }


    @Jpf.Action
    protected Forward showPeptideProphetObservedVsModelPlot(PeptideProphetForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

        PeptideProphetGraphs.renderObservedVsModel(getResponse(), summary, form.charge, form.cumulative);

        return null;
    }

    @Jpf.Action
    protected Forward showPeptideProphetObservedVsPPScorePlot(PeptideProphetForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        PeptideProphetGraphs.renderObservedVsPPScore(getResponse(), getContainer(), form.run, form.charge, form.cumulative);

        return null;
    }


    private String modificationHref(MS2Run run)
    {
        // Need to make the pop-up window wider on SSL connections since Firefox insists on displaying the full server name
        // in the status bar and spreads out the content unnecessarily. 
        int width = ("https".equals(getViewURLHelper().getScheme()) ? 175 : 100);

        StringBuilder href = new StringBuilder();
        href.append("<a href=\"showModifications.view?run=");
        href.append(run.getRun());
        href.append("\" target=\"modifications\" onClick=\"window.open('showModifications.view?run=");
        href.append(run.getRun());
        href.append("','modifications','height=300,width=");
        href.append(width);
        href.append(",status=yes,toolbar=no,menubar=no,location=no,resizable=yes');return false;\"><img border=0 src=\"");
        href.append(PageFlowUtil.buttonSrc("Show Modifications"));
        href.append("\"></a>");

        return href.toString();
    }


    @Jpf.Action
    protected Forward showModifications(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        SortedMap<String, String> fixed = new TreeMap<String, String>();
        SortedMap<String, String> var = new TreeMap<String, String>();

        for (MS2Modification mod : run.getModifications())
        {
            if (mod.getVariable())
                var.put(mod.getAminoAcid() + mod.getSymbol(), Formats.f3.format(mod.getMassDiff()));
            else
                fixed.put(mod.getAminoAcid(), Formats.f3.format(mod.getMassDiff()));
        }

        GroovyView view = new GroovyView("/MS2/modifications.gm");
        view.setFrame(WebPartView.FrameType.NONE);
        view.addObject("pageTitle", "Modifications");
        view.addObject("fixed", fixed);
        view.addObject("var", var);

        return includeView(view);
    }


    private List<String> getRunSummaryHeaders(MS2Run run)
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


    private String naForNull(String s)
    {
        return s == null ? "n/a" : s;
    }


    @Jpf.Action
    protected Forward pickExportRunsView() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        String extraFormHtml =
            "<tr><td><br>Choose an export format:</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"Excel\" checked=\"checked\">Excel (limited to 65,535 rows)</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"ExcelBare\">Excel with minimal header text (limited to 65,535 rows)</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"TSV\">TSV</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"DTA\">Spectra as DTA</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"PKL\">Spectra as PKL</td></tr>\n" +
            "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"AMT\">AMT (Accurate Mass & Time) file</td></tr>\n";

        return pickView(cloneViewURLHelper().setAction("applyExportRunsView"), "Select a view to apply a filter to all the runs and to indicate what columns to export.", extraFormHtml, "Export Runs", "ms2RunsList", false);
    }


    @Jpf.Action
    protected Forward applyExportRunsView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // Forward without redirect: this lets struts fill in the form and preserves the post data
        return new ViewForward(getApplyViewForwardUrl(form, "exportRuns"), false);
    }


    @Jpf.Action
    protected Forward exportRuns(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        List<String> errors = new ArrayList<String>();
        List<MS2Run> runs = getCachedRuns(form.getRunList(), errors);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), runs.toArray(new MS2Run[]{}));
        ViewURLHelper currentUrl = cloneViewURLHelper();
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

        if (form.getExportFormat() != null && form.getExportFormat().startsWith("Excel"))
        {
            exportToExcel(runs, peptideFilter, form, peptideView, "All");
        }

        if ("TSV".equals(form.getExportFormat()))
        {
            if (peptideView instanceof FlatPeptideView)
                exportPeptidesToTSV(runs, peptideFilter, "MS2Runs", null, form.getColumns(), peptideView);
            else
                exportProteinsToTSV(runs, null, "MS2Runs", form.getColumns(), form.getProteinColumns(), form.getExpanded(), peptideView, null);
        }

        if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            if (peptideView instanceof FlatPeptideView)
                exportSpectra(runs, currentUrl, peptideFilter, form.getExportFormat().toLowerCase());
            else
                exportProteinsAsSpectra(runs, currentUrl, form.getExportFormat().toLowerCase(), peptideView, null);
        }

        if ("AMT".equals(form.getExportFormat()))
        {
            if (peptideView instanceof FlatPeptideView)
                exportToAMT(runs, peptideFilter, peptideView);
            else
                exportProteinsToAMT(runs, peptideView, null);
        }

        return null;
    }


    private void exportToExcel(List<MS2Run> runs, SimpleFilter filter, ExportForm form, AbstractPeptideView peptideView, String which) throws ServletException, SQLException, IOException
    {
        boolean includeHeaders = form.getExportFormat().equals("Excel");

        ServletOutputStream outputStream = ExcelWriter.getOutputStream(getResponse(), "MS2Runs");
        WritableWorkbook workbook = ExcelWriter.getWorkbook(outputStream);

        if (peptideView instanceof FlatPeptideView)
            exportPeptidesToExcel(workbook, runs, filter, form.getColumns(), includeHeaders, peptideView, which);
        else
            exportProteinsToExcel(workbook, runs, form.getColumns(), form.getProteinColumns(), includeHeaders, form.getGrouping(), form.getExpanded(), peptideView);

        ExcelWriter.closeWorkbook(workbook, outputStream);
    }


    private void exportPeptidesToExcel(WritableWorkbook workbook, List<MS2Run> runs, SimpleFilter filter, String requestedPeptideColumns, boolean includeHeaders, AbstractPeptideView peptideView, String whichPeptides) throws ServletException, SQLException, IOException
    {
        ViewURLHelper currentUrl = cloneViewURLHelper();

        ExcelWriter ew = new ExcelWriter();
        ew.setSheetName("MS2 Runs");

        List<String> headers;

        if (includeHeaders)
        {
            MS2Run run = runs.get(0);

            if (runs.size() == 1)
            {
                headers = getRunSummaryHeaders(run);
                headers.add(whichPeptides + " peptides matching the following query:");
                addPeptideFilterText(headers, run, currentUrl);
                ew.setSheetName(run.getDescription() + " Peptides");
            }
            else
            {
                headers = new ArrayList<String>();
                headers.add("Multiple runs showing all peptides matching the following query:");
                addPeptideFilterText(headers, run, currentUrl);  // TODO: Version that takes runs[]
            }
            headers.add("");
            ew.setHeaders(headers);
        }

        // Always include column captions at the top
        ew.setColumns(peptideView.getPeptideDisplayColumns(peptideView.getPeptideColumnNames(requestedPeptideColumns)));
        ew.renderNewSheet(workbook);
        ew.setCaptionRowVisible(false);

        // TODO: Footer?

        for (int i = 0; i < runs.size(); i++)
        {
            if (includeHeaders)
            {
                headers = new ArrayList<String>();

                if (runs.size() > 1)
                {
                    if (i > 0)
                        headers.add("");

                    headers.add(runs.get(i).getDescription());
                }

                ew.setHeaders(headers);
            }

            setupExcelPeptideGrid(ew, filter, requestedPeptideColumns, runs.get(i), peptideView);
            ew.renderCurrentSheet(workbook);
        }
    }


    private void setupExcelPeptideGrid(ExcelWriter ew, SimpleFilter filter, String requestedPeptideColumns, MS2Run run, AbstractPeptideView peptideView) throws ServletException, SQLException, IOException
    {
        String columnNames = peptideView.getPeptideColumnNames(requestedPeptideColumns);
        DataRegion rgn = peptideView.getPeptideGrid(columnNames, ExcelWriter.MAX_ROWS);
        Container c = getContainer();
        ProteinManager.replaceRunCondition(filter, run, null);

        RenderContext ctx = new RenderContext(getViewContext());
        ctx.setContainer(c);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        ew.setResultSet(rgn.getResultSet(ctx));
        ew.setColumns(rgn.getDisplayColumnList());
        ew.setAutoSize(true);
    }


    private void exportProteinsToTSV(List<MS2Run> runs, List<String> fileHeader, String fileNamePrefix, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean expanded, AbstractPeptideView peptideView, String where) throws ServletException, SQLException
    {
        ProteinTSVGridWriter tw = peptideView.getTSVProteinGridWriter(requestedProteinColumnNames, requestedPeptideColumnNames, expanded);
        tw.prepare(getResponse());
        tw.setFileHeader(fileHeader);
        tw.setFilenamePrefix(fileNamePrefix);
        tw.writeFileHeader();
        tw.writeColumnHeaders();

        for (MS2Run run : runs)
            peptideView.exportTSVProteinGrid(tw, requestedPeptideColumnNames, run, where);

        tw.close();
    }


    // TODO: Move this code to PeptideView
    private void exportProteinsToExcel(WritableWorkbook workbook, List<MS2Run> runs, String requestedPeptideColumns, String requestedProteinColumns, boolean includeHeaders, String grouping, boolean expanded, AbstractPeptideView peptideView) throws SQLException
    {
        ViewURLHelper currentUrl = cloneViewURLHelper();

        AbstractProteinExcelWriter ew = peptideView.getExcelProteinGridWriter(requestedProteinColumns);
        ew.setSheetName("MS2 Runs");
        List<String> headers;

        if (includeHeaders)
        {
            headers = new ArrayList<String>();
            headers.add("Multiple runs showing all protein" + ("proteinprophet".equals(grouping) ? " group" : "") + "s matching the following query:");
            addPeptideFilterText(headers, runs.get(0), currentUrl);

            if ("protein".equals(grouping))
                addProteinFilterText(headers, currentUrl);
            else
                addProteinGroupFilterText(headers, currentUrl);

            headers.add("");
            ew.setHeaders(headers);
        }

        ew.renderNewSheet(workbook);
        ew.setCaptionRowVisible(false);

        for (int i = 0; i < runs.size(); i++)
        {
            if (includeHeaders && runs.size() > 1)
            {
                headers = new ArrayList<String>();

                if (i > 0)
                    headers.add("");

                headers.add(runs.get(i).getDescription());
                ew.setHeaders(headers);
            }

            peptideView.setUpExcelProteinGrid(ew, expanded, requestedPeptideColumns, runs.get(i), null);
            ew.renderCurrentSheet(workbook);
        }
    }


    @Jpf.Action
    protected Forward exportAllProteins(ExportForm form) throws ServletException, IOException, SQLException
    {
        requiresPermission(ACL.PERM_READ);

        if (isAuthorized(form.run))
            exportProteins(form, "All", null);

        return null;
    }


    @Jpf.Action
    protected Forward exportSelectedProteins(ExportForm form) throws ServletException, SQLException, IOException
    {
        if (!isAuthorized(form.run))
            return null;

        requiresPermission(ACL.PERM_READ);

        ViewContext ctx = getViewContext();
        List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

        if (null != proteins)
        {
            StringBuffer where = new StringBuffer("Protein IN (");

            for (int i = 0; i < Math.min(proteins.size(), ExcelWriter.MAX_ROWS); i++)
            {
                // Escape all single quotes in the protein names
                // TODO: Use (?, ?, ...) and JDBC parameters instead
                String protein = proteins.get(i).replaceAll("'", "\\\\'");

                if (i > 0)
                    where.append(",");

                where.append('\'');
                where.append(protein);
                where.append('\'');
            }

            where.append(")");

            exportProteins(form, "Hand-selected from", where.toString());
        }

        return null;
    }


    private void exportProteins(ExportForm form, String what, String extraWhere) throws ServletException, SQLException, IOException
    {
        MS2Run run = MS2Manager.getRun(form.getRun());
        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        if ("Excel".equals(form.getExportFormat()))
        {
            // TODO: Use exportRunsAsProteins instead
            exportProteinsToExcel(getViewURLHelper(), form, what, extraWhere);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            exportProteinsToTSV(Arrays.asList(run), null, "MS2Runs", form.getColumns(), form.getProteinColumns(), form.getExpanded(), peptideView, extraWhere);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            exportProteinsToAMT(Arrays.asList(run), peptideView, extraWhere);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewURLHelper(), form.getExportFormat().toLowerCase(), peptideView, extraWhere);
        }
    }


    @Jpf.Action
    protected Forward exportSelectedProteinGroups(ExportForm form) throws ServletException, SQLException, IOException
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        ViewContext ctx = getViewContext();
        List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

        if (null != proteins)
        {
            StringBuffer where = new StringBuffer();
            where.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation().toString());
            where.append(".RowId IN (");

            for (int i = 0; i < Math.min(proteins.size(), ExcelWriter.MAX_ROWS); i++)
            {
                // Escape all single quotes in the protein names
                String protein = proteins.get(i).replaceAll("'", "\\\\'");

                if (i > 0)
                    where.append(",");

                where.append('\'');
                where.append(protein);
                where.append('\'');
            }

            where.append(")");

            exportProteinGroups(form, "Hand-selected from", where.toString());
        }

        return null;
    }

    @Jpf.Action
    protected Forward exportProteinGroups(ExportForm form) throws ServletException, IOException, SQLException
    {
        requiresPermission(ACL.PERM_READ);

        if (isAuthorized(form.run))
            exportProteinGroups(form, "All", null);

        return null;
    }


    private void exportProteinGroups(ExportForm form, String what, String where) throws ServletException, SQLException, IOException
    {
        MS2Run run = MS2Manager.getRun(form.getRun());
        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        if ("Excel".equals(form.getExportFormat()))
        {
            // TODO: Use exportRunsAsProteinGroups instead
            exportProteinGroupsToExcel(getViewURLHelper(), form, what, where);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            exportProteinsToTSV(Arrays.asList(run), null, "MS2Runs", form.getColumns(), form.getProteinColumns(), form.getExpanded(), peptideView, where);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            exportProteinsToAMT(Arrays.asList(run), peptideView, where);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewURLHelper(), form.getExportFormat().toLowerCase(), peptideView, where);
        }
    }


    private void exportProteinGroupsToExcel(ViewURLHelper currentUrl, RunForm form, String whatProteins, String where) throws SQLException, ServletException
    {
        MS2Run run = MS2Manager.getRun(form.run);

        List<String> headers = getRunSummaryHeaders(run);
        headers.add(whatProteins + " protein groups matching the following query:");
        addPeptideFilterText(headers, run, currentUrl);
        addProteinGroupFilterText(headers, currentUrl);
        headers.add("");

        AbstractPeptideView proteinView = new ProteinProphetPeptideView(getContainer(), getUser(), getViewURLHelper(), run);
        proteinView.writeExcel(getResponse(), run, form.getExpanded(), form.getColumns(), where, headers, form.getProteinColumns());
    }

    private void exportProteinsToExcel(ViewURLHelper currentUrl, RunForm form, String whatProteins, String where) throws SQLException, ServletException
    {
        MS2Run run = MS2Manager.getRun(form.run);

        List<String> headers = getRunSummaryHeaders(run);
        headers.add(whatProteins + " proteins matching the following query:");
        addPeptideFilterText(headers, run, currentUrl);
        addProteinFilterText(headers, currentUrl);
        headers.add("");

        AbstractPeptideView proteinView = new StandardProteinPeptideView(getContainer(), getUser(), getViewURLHelper(), run);
        proteinView.writeExcel(getResponse(), run, form.getExpanded(), form.getColumns(), where, headers, form.getProteinColumns());
    }


    private void addPeptideFilterText(List<String> headers, MS2Run run, ViewURLHelper currentUrl)
    {
        headers.add("");
        headers.add("Peptide Filter: " + ProteinManager.getPeptideFilter(currentUrl, run, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER).getFilterText(MS2Manager.getSqlDialect()));
        headers.add("Peptide Sort: " + new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText(MS2Manager.getSqlDialect()));
    }


    private void addProteinFilterText(List<String> headers, ViewURLHelper currentUrl)
    {
        headers.add("Protein Filter: " + new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText(MS2Manager.getSqlDialect()));
        headers.add("Protein Sort: " + new Sort(currentUrl, MS2Manager.getDataRegionNameProteins()).getSortText(MS2Manager.getSqlDialect()));
    }


    private void addProteinGroupFilterText(List<String> headers, ViewURLHelper currentUrl)
    {
        headers.add("Protein Group Filter: " + new SimpleFilter(currentUrl, MS2Manager.getTableInfoProteinGroupsWithQuantitation().getName()).getFilterText(MS2Manager.getSqlDialect()));
        headers.add("Protein Group Sort: " + new Sort(currentUrl, MS2Manager.getTableInfoProteinGroupsWithQuantitation().getName()).getSortText(MS2Manager.getSqlDialect()));
    }


    @Jpf.Action
    protected Forward exportAllPeptides(ExportForm form) throws ServletException, IOException, SQLException
    {
        requiresPermission(ACL.PERM_READ);

        return exportPeptides(form, false);
    }


    @Jpf.Action
    protected Forward exportSelectedPeptides(ExportForm form) throws ServletException, IOException, SQLException
    {
        requiresPermission(ACL.PERM_READ);

        return exportPeptides(form, true);
    }


    private Forward exportPeptides(ExportForm form, boolean selected) throws ServletException, IOException, SQLException
    {
        if (!isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        ViewURLHelper currentUrl = getViewURLHelper();
        AbstractPeptideView peptideView = getPeptideView(form.getGrouping(), run);

        // Need to create a filter for 1) extra filter and 2) selected peptides
        // URL filter is applied automatically (except for DTA/PKL)
        SimpleFilter baseFilter = ProteinManager.getPeptideFilter(currentUrl, run, ProteinManager.EXTRA_FILTER);

        if (selected)
            addSelectedPeptides(baseFilter, getViewContext(), ExcelWriter.MAX_ROWS);  // Technically, should only limit this in Excel export case... but there's no way to individually select 65K peptides

        if ("Excel".equals(form.exportFormat))
            exportToExcel(Arrays.asList(run), baseFilter, form, peptideView, (selected ? "Hand-selected from" : "All"));

        if ("TSV".equals(form.exportFormat))
            return exportPeptidesToTSV(Arrays.asList(run), baseFilter, run.getDescription(), null, form.columns, peptideView);

        if ("AMT".equals(form.exportFormat))
            return exportToAMT(Arrays.asList(run), baseFilter, peptideView);

        // Add URL filter manually
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentUrl, run, ProteinManager.URL_FILTER));

        if ("DTA".equals(form.exportFormat) || "PKL".equals(form.exportFormat))
            return exportSpectra(Arrays.asList(run), currentUrl, baseFilter, form.exportFormat.toLowerCase());

        return null;
    }


    private void addSelectedPeptides(SimpleFilter filter, ViewContext ctx, int maxRows)
    {
        List<String> exportRows = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

        if (null != exportRows)
        {
            List<Long> peptideIds = new ArrayList<Long>(exportRows.size());

            for (int i = 0; i < Math.min(exportRows.size(), maxRows); i++)
            {
                String[] row = exportRows.get(i).split(",");
                peptideIds.add(Long.parseLong(row[1]));
            }

            filter.addInClause("RowId", peptideIds);
        }
    }


    private List<String> getAMTFileHeader(List<MS2Run> runs)
    {
        List<String> fileHeader = new ArrayList<String>(runs.size());

        fileHeader.add("#HydrophobicityAlgorithm=" + HydrophobicityColumn.getAlgorithmVersion());

        for (MS2Run run : runs)
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


    private static final String AMT_PEPTIDE_COLUMN_NAMES = "Run,Fraction,Mass,Scan,RetentionTime,H,PeptideProphet,Peptide";

    private Forward exportToAMT(List<MS2Run> runs, SimpleFilter filter, AbstractPeptideView peptideView) throws ServletException, SQLException, IOException
    {
        List<String> fileHeader = getAMTFileHeader(runs);

        return exportPeptidesToTSV(runs, filter, "AMT", fileHeader, AMT_PEPTIDE_COLUMN_NAMES, peptideView);
    }


    private void exportProteinsToAMT(List<MS2Run> runs, AbstractPeptideView peptideView, String where) throws ServletException, SQLException
    {
        List<String> fileHeader = getAMTFileHeader(runs);

        exportProteinsToTSV(runs, fileHeader, "AMT", AMT_PEPTIDE_COLUMN_NAMES, "" /* No protein columns */, true, peptideView, where);
    }


    private Forward exportPeptidesToTSV(List<MS2Run> runs, SimpleFilter filter, String fileNamePrefix, List<String> fileHeader, String requestedColumnNames, AbstractPeptideView peptideView) throws ServletException, SQLException, IOException
    {
        RenderContext ctx = new MultiRunRenderContext(getViewContext(), runs);
        ctx.setBaseFilter(filter);
        ctx.setBaseSort(ProteinManager.getPeptideBaseSort());
        ctx.setCache(false);

        String columnNames = peptideView.getPeptideColumnNames(requestedColumnNames);
        List<DisplayColumn> displayColumns = peptideView.getPeptideDisplayColumns(columnNames);
        peptideView.changePeptideCaptionsForTsv(displayColumns);

        TSVGridWriter tw = new TSVGridWriter(ctx, MS2Manager.getTableInfoPeptides(), displayColumns, MS2Manager.getDataRegionNamePeptides());
        tw.setFilenamePrefix(fileNamePrefix);
        tw.setFileHeader(fileHeader);   // Used for AMT file export
        tw.write(getResponse());

        return null;
    }


    private Forward exportSpectra(List<MS2Run> runs, ViewURLHelper currentUrl, SimpleFilter filter, String extension) throws IOException
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.applyURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        SpectrumIterator iter = new ResultSetSpectrumIterator(runs, filter, sort);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();

        return null;
    }


    private Forward exportProteinsAsSpectra(List<MS2Run> runs, ViewURLHelper currentUrl, String extension, AbstractPeptideView peptideView, String where) throws IOException
    {
        SpectrumIterator iter = new ProteinResultSetSpectrumIterator(runs, currentUrl, peptideView, where);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();

        return null;
    }


    @Jpf.Action
    protected Forward showParamsFile(DetailsForm form) throws IOException, ServletException
    {
        requiresPermission(ACL.PERM_READ);

        MS2Run run = MS2Manager.getRun(form.run);

        try
        {
            // TODO: Ensure drive?
            PageFlowUtil.streamFile(getResponse(), run.getPath() + "/" + run.getParamsFileName(), false);
        }
        catch (Exception e)
        {
            getResponse().getWriter().print("Error retrieving file: " + e.getMessage());
        }

        return null;
    }


    @Jpf.Action
    protected Forward showGZFile(DetailsForm form) throws Exception
    {
        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

        if (null == peptide)
        {
            // This should only happen if an old, cached link is being used... a saved favorite or google bot with fraction=x&scan=y&charge=z instead of peptideId
            // Log an error just to make sure.
            _log.error("Couldn't find peptide " + form.getPeptideId() + ". " + getViewURLHelper().toString());
            return _renderError("Peptide not found");
        }

        getResponse().setContentType("text/plain");
        PrintWriter out = getResponse().getWriter();

        MS2GZFileRenderer renderer = new MS2GZFileRenderer(peptide, form.getExtension());

        if (!renderer.render(out))
        {
            MS2GZFileRenderer.renderFileHeader(out, MS2GZFileRenderer.getFileNameInGZFile(MS2Manager.getFraction(peptide.getFraction()), peptide.getScan(), peptide.getCharge(), form.extension));
            out.println(renderer.getLastErrorMessage());
        }

        return null;
    }


    @Jpf.Action
    protected Forward addExtraFilter() throws URISyntaxException, ServletException
    {
        requiresPermission(ACL.PERM_READ);

        ViewContext ctx = getViewContext();
        ViewURLHelper url = cloneViewURLHelper();
        url.setAction("showRun.view");

        MS2Run run = MS2Manager.getRun((String) ctx.get("run"));
        String paramName = run.getChargeFilterParamName();

        // Stick posted values onto showRun URL and forward.  URL shouldn't have any rawScores or tryptic (they are
        // deleted from the button URL and get posted instead).  Don't bother adding "0" since it's the default.

        // Verify that charge filter scroes are valid floats and, if so, add as URL params
        parseChargeScore(ctx, url, "1", paramName);
        parseChargeScore(ctx, url, "2", paramName);
        parseChargeScore(ctx, url, "3", paramName);

        String tryptic = (String) ctx.get("tryptic");

        if (!"0".equals(tryptic))
            url.addParameter("tryptic", tryptic);

        return new ViewForward(url);
    }


    // Parse parameter to float, returning 0 for any parsing exceptions
    private void parseChargeScore(ViewContext ctx, ViewURLHelper url, String digit, String paramName)
    {
        float value = 0;
        String score = (String)ctx.get("charge" + digit);

        try
        {
            value = Float.parseFloat(score);
        }
        catch(NumberFormatException e)
        {
            // Can't parse... just use default
        }

        if (0.0 != value)
            url.addParameter(paramName + digit, Formats.chargeFilter.format(value));
    }


    // extraFormHtml gets inserted between the view dropdown and the button.
    private Forward pickView(ViewURLHelper nextUrl, String viewInstructions, String extraFormHtml, String navTreeName, String helpTopic, boolean requireProteinProphet) throws Exception
    {
        List<String> errors = new ArrayList<String>();
        int runListIndex = cacheSelectedRuns(errors, requireProteinProphet);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        VelocityView pickView = new VelocityView("pickView.vm");

        nextUrl.deleteParameters("button");
        nextUrl.deleteParameters("button.x");
        nextUrl.deleteParameters("button.y");

        pickView.addObject("nextUrl", nextUrl.getEncodedLocalURIString());
        pickView.addObject("select", renderViewSelect(0, true, ACL.PERM_READ, false));
        pickView.addObject("extraHtml", extraFormHtml);
        pickView.addObject("viewInstructions", viewInstructions);
        pickView.addObject("runList", runListIndex);

        return _renderInTemplate(pickView, false, navTreeName, helpTopic,
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    @Jpf.Action
    protected Forward compareProteins(ExportForm form) throws Exception
    {
        // Don't require any particular permission in current container, since we could be in hierarchy mode

        String extraFormHtml =
            "<tr><td><br>Choose what columns should appear in the grid:</td></tr>\n" +
            "<tr><td><input type=\"hidden\" name=\"column\" value=\"Protein\"></td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"unique\" value=\"1\" checked=\"checked\">Unique Peptides</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"total\" value=\"1\">Total Peptides</td></tr>\n";

        return pickCompareView(ProteinCompareQuery.COMPARISON_DESCRIPTION, extraFormHtml, false);
    }

    @Jpf.Action
    protected Forward compareProteinProphetProteins(ExportForm form) throws Exception
    {
        // Don't require any particular permission in current container, since we could be in hierarchy mode

        String extraFormHtml =
            "<tr><td><br>Choose what columns should appear in the grid:</td></tr>\n" +
            "<tr><td><input type=\"hidden\" name=\"column\" value=\"ProteinProphet\"></td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"proteinGroup\" value=\"1\" checked=\"checked\" disabled>Protein Group</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"groupProbability\" value=\"1\" checked=\"checked\">Group Probability</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"light2HeavyRatioMean\" value=\"1\">Light to Heavy Quantitation</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"heavy2LightRatioMean\" value=\"1\">Heavy to Light Quantitation</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"totalPeptides\" value=\"1\">Total Peptides</td></tr>\n" +
            "<tr><td><input type=\"checkbox\" name=\"uniquePeptides\" value=\"1\">Unique Peptides</td></tr>";

        return pickCompareView(ProteinProphetCompareQuery.COMPARISON_DESCRIPTION, extraFormHtml, true);
    }

    @Jpf.Action
    protected Forward comparePeptides() throws Exception
    {
        // Don't require any particular permission in current container, since this could be in hierarchy mode

        String extraFormHtml = "<tr><td><input type=\"hidden\" name=\"column\" value=\"Peptide\"></td></tr>\n";

        return pickCompareView(PeptideCompareQuery.COMPARISON_DESCRIPTION, extraFormHtml, false);
    }


    private Forward pickCompareView(String comparisonDescription, String extraHtml, boolean requireProteinProphet) throws Exception
    {
        ViewURLHelper nextUrl = cloneViewURLHelper().setAction("applyCompareView");
        return pickView(nextUrl, "Select a view to apply a filter to all the runs.", extraHtml, comparisonDescription, "ms2RunsList", requireProteinProphet);
    }


    @Jpf.Action
    protected Forward applyCompareView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper forwardUrl = getApplyViewForwardUrl(form, "showCompare");

        forwardUrl.deleteParameter("submit.x");
        forwardUrl.deleteParameter("submit.y");
        forwardUrl.deleteParameter("viewParams");

        // Forward with redirect: this lets struts fill in the form and ensures that the JavaScript sees the showCompare action
        return new ViewForward(forwardUrl, true);
    }


    @Jpf.Action
    protected Forward showCompare(ExportForm form) throws Exception
    {
        return compareRuns(form.getRunList(), false);
    }


    @Jpf.Action
    protected Forward exportCompareToExcel(ExportForm form) throws Exception
    {
        return compareRuns(form.getRunList(), true);
    }


    private Forward compareRuns(int runListIndex, boolean exportToExcel) throws Exception
    {
        List<String> errors = new ArrayList<String>();
        List<MS2Run> runs = getCachedRuns(runListIndex, errors);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        ViewURLHelper currentUrl = getViewURLHelper();
        String column = currentUrl.getParameter("column");

        CompareQuery query = CompareQuery.getCompareQuery(column, currentUrl, runs);
        if (query == null)
            return _renderError("You must specify a column name");

        if (query.getGridColumns().size() == 0)
            errors.add("You must choose at least one column to display in the grid.");

        if (runs.size() > 63)
            errors.add("You can't compare more than 63 runs at a time.");

        if (errors.size() > 0)
            return _renderErrors(errors);

        List<RunColumn> gridColumns = query.getGridColumns();
        CompareDataRegion rgn = query.getCompareGrid();

        List<String> runCaptions = new ArrayList<String>(runs.size());
        for (MS2Run run : runs)
            runCaptions.add(run.getDescription());

        int offset = 1;

        if (exportToExcel)
        {
            ResultSet rs = rgn.getResultSet();
            CompareExcelWriter ew = new CompareExcelWriter(rs, rgn.getDisplayColumnList());
            ew.setAutoSize(true);
            ew.setSheetName(query.getComparisonDescription());
            ew.setFooter(query.getComparisonDescription());

            // Set up the row display the run descriptions (which can span more than one data column)
            ew.setOffset(offset);
            ew.setColSpan(gridColumns.size());
            ew.setMultiColumnCaptions(runCaptions);

            List<String> headers = new ArrayList<String>();
            headers.add(query.getHeader());
            headers.add("");
            for (Pair<String, String> sqlSummary : query.getSQLSummaries())
            {
                headers.add(sqlSummary.getKey() + ": " + sqlSummary.getValue());
            }
            headers.add("");
            ew.setHeaders(headers);
            ew.write(getResponse());
        }
        else
        {
            rgn.setOffset(offset);
            rgn.setColSpan(gridColumns.size());
            rgn.setMultiColumnCaptions(runCaptions);

            VelocityView filterView = new VelocityView("renderFilter.vm");

            filterView.addObject("headers", new String[]{query.getHeader()});
            filterView.addObject("sqlSummaries", query.getSQLSummaries());

            GridView compareView = new GridView(rgn);
            compareView.setResultSet(rgn.getResultSet());
            List<DisplayColumn> columns = rgn.getDisplayColumnList();
            boolean shaded = false;
            int i = 0;
            while (i < offset)
            {
                if (shaded)
                {
                    columns.get(i).setBackgroundColor("#EEEEEE");
                }
                i++;
                shaded = !shaded;
            }
            for (String runCaption : runCaptions)
            {
                for (RunColumn gridColumn : gridColumns)
                {
                    if (shaded)
                    {
                        columns.get(i).setBackgroundColor("#EEEEEE");
                    }
                    i++;
                }
                shaded = !shaded;
            }
            while (i < columns.size())
            {
                if (shaded)
                {
                    columns.get(i).setBackgroundColor("#EEEEEE");
                }
                i++;
                shaded = !shaded;
            }

            _renderInTemplate(new VBox(filterView, compareView), false, query.getComparisonDescription(), "ms2RunsList",
                    new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
        }

        return null;
    }

    private List<MS2Run> getSelectedRuns(List<String> errors, boolean requireProteinProphet) throws ServletException
    {
        ViewContext ctx = getViewContext();
        List<String> stringIds = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

        if (null == stringIds)
        {
            errors.add(NO_RUNS_MESSAGE);
            return null;
        }

        List<Integer> runIds = new ArrayList<Integer>(stringIds.size());

        for (String stringId : stringIds)
        {
            try
            {
                runIds.add(Integer.parseInt(stringId));
            }
            catch (NumberFormatException e)
            {
                _log.error("getSelectedRuns", e);
                errors.add("Run " + stringId + ": Number format error");
            }
        }

        return getRuns(runIds, errors, requireProteinProphet, true);
    }


    private List<MS2Run> getRuns(List<Integer> runIds, List<String> errors, boolean requireProteinProphet, boolean requireSameType) throws ServletException
    {
        List<MS2Run> runs = new ArrayList<MS2Run>(runIds.size());
        boolean experimentRunIds = "true".equals(getRequest().getParameter("ExperimentRunIds"));
        String type = null;

        for (Integer runId : runIds)
        {
            MS2Run run = null;
            if (experimentRunIds)
            {
                ExpRun expRun = ExperimentService.get().getExperimentRun(runId.intValue());
                if (expRun != null)
                {
                    run = MS2Manager.getRunByExperimentRunLSID(expRun.getLSID());
                }
            }
            else
            {
                run = MS2Manager.getRun(runId);
            }

            if (null == run)
            {
                errors.add("Run " + runId + ": Not found");
                continue;
            }

            // Authorize this run
            Container c = ContainerManager.getForId(run.getContainer());

            if (!c.hasPermission(getUser(), ACL.PERM_READ))
            {
                if (getUser().isGuest())
                    HttpView.throwUnauthorized();

                errors.add("Run " + runId + ": Not authorized");
                continue;
            }

            if (run.getStatusId() == MS2Importer.STATUS_RUNNING)
            {
                errors.add(run.getDescription() + " is still loading");
                continue;
            }

            if (run.getStatusId() == MS2Importer.STATUS_FAILED)
            {
                errors.add(run.getDescription() + " did not load successfully");
                continue;
            }

            if (requireProteinProphet)
            {
                try
                {
                    if (run.getProteinProphetFile() == null)
                    {
                        errors.add(run.getDescription() + " does not have ProteinProphet data associated with it.");
                    }
                }
                catch (SQLException e)
                {
                    throw new ServletException(e);
                }
            }

            if (requireSameType)
            {
                if (null == type)
                    type = run.getType();
                else if (!type.equals(run.getType()))
                {
                    errors.add("Can't mix " + type + " and " + run.getType() + " runs.");
                    continue;
                }
            }

            runs.add(run);
        }

        return runs;
    }


    // Stash lists of run ids in session state.  Use object Id as index into map, and pass the Id on the URL.  We can't stash these
    // lists on the URL because it could be too large (we support exporting/comparing hundreds of runs).  We can't post the data
    // and forward through the applyView process because we must redirect to end up on the right action (otherwise the filter box
    // JavaScript will call the wrong action).  Plus, DataRegion sorting uses GET, so we'd lose the list of runs after sorting.
    private Map<Integer, List<Integer>> _runListCache = new HashMap <Integer, List<Integer>>(10);

    // We cache just the list of run IDs, not the runs themselves.  This keeps things small and eases mem tracking.  Even though we're
    // just caching the list, we do all error & security checks upfront to alert the user early.
    private int cacheSelectedRuns(List<String> errors, boolean requireProteinProphet) throws ServletException
    {
        List<MS2Run> runs = getSelectedRuns(errors, requireProteinProphet);

        if (errors.size() > 0)
            return 0;

        List<Integer> runIds = new ArrayList<Integer>(runs.size());

        for (MS2Run run : runs)
            runIds.add(run.getRun());

        int index = runIds.hashCode();
        _runListCache.put(index, runIds);
        return index;
    }


    private static final String NO_RUNS_MESSAGE = "Run list is empty; session may have timed out.  Please reselect the runs.";

    private List<MS2Run> getCachedRuns(int index, List<String> errors) throws ServletException
    {
        List<Integer> runIds = _runListCache.get(index);

        if (null == runIds)
        {
            errors.add(NO_RUNS_MESSAGE);
            return null;
        }

        return getRuns(runIds, errors, false, true);
    }

    @Jpf.Action
    protected Forward showProteinGroup(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (form.run != 0 && !isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        ProteinProphetFile proteinProphet = run.getProteinProphetFile();
        if (proteinProphet == null)
        {
            return HttpView.throwNotFound();
        }
        ProteinGroupWithQuantitation group = proteinProphet.lookupGroup(form.getGroupNumber(), form.getIndistinguishableCollectionId());
        Protein[] proteins = group.lookupProteins();

        AbstractPeptideView peptideView = new ProteinProphetPeptideView(getContainer(), getUser(), getViewURLHelper(), run);
        VBox view = getShowProteinsView(getViewURLHelper(), run, form, proteins, null, peptideView);

        JspView summaryView = new JspView("/MS2/showProteinGroup.jsp");
        summaryView.addObject("proteinGroup", group);
        VBox fullPage = new VBox(summaryView, view);

        return _renderInTemplate(fullPage, false, "Protein Group Details", "showProteinGroup",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(MS2Manager.getRun(form.run).getDescription(), getViewURLHelper().clone().setAction("showRun")));
    }

    @Jpf.Action
    protected Forward showProtein(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (form.run != 0 && !isAuthorized(form.run))
            return null;

        ViewURLHelper currentUrl = getViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);
        int seqId = form.getSeqId();

        if (0 == seqId)
            return _renderInTemplate(new HtmlView("No details are available for protein " + form.getProtein() + "; its sequence is not available"), false, "Protein sequence not found", null);

        Protein protein = ProteinManager.getProtein(seqId);

        // Set the protein name used in this run's FASTA file; we want to include this in the view.
        protein.setLookupString(form.getProtein());

        AbstractPeptideView peptideView = new StandardProteinPeptideView(getContainer(), getUser(), getViewURLHelper(), run);
        HttpView proteinView = getShowProteinsView(currentUrl, run, form, new Protein[] {protein}, null, peptideView);

        ViewURLHelper runUrl = currentUrl.clone();
        runUrl.deleteParameter("seqId");

        List<Pair<String, ViewURLHelper>> navTrail = new ArrayList<Pair<String, ViewURLHelper>>();
        navTrail.add(new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
        if (run != null)
        {
            navTrail.add(new Pair<String, ViewURLHelper>(run.getDescription(), runUrl.setAction("showRun")));
        }

        return _renderInTemplate(proteinView, Template.print, getProteinTitle(protein, true), "showProtein", false,
                navTrail.toArray(new Pair[navTrail.size()]));
    }


    @Jpf.Action
    protected Forward showAllProteins(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        long peptideId = form.getPeptideId();

        if (peptideId == 0)
            return _renderError("No peptide specified");

        MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

        if (null == peptide)
            return _renderError("Could not locate peptide with this ID: " + peptideId);

        MS2Run run = MS2Manager.getRun(form.run);

        Protein[] proteins = ProteinManager.getProteinsContainingPeptide(run.getFastaId(), peptide);
        ViewURLHelper currentUrl = getViewURLHelper().clone();
        AbstractPeptideView peptideView = new StandardProteinPeptideView(getContainer(), getUser(), getViewURLHelper(), run);
        HttpView proteinView = getShowProteinsView(currentUrl, run, form, proteins, new String[]{peptide.getTrimmedPeptide()}, peptideView);

        currentUrl.deleteParameter("peptideId");

        return _renderInTemplate(proteinView, false, "Proteins Containing " + peptide, "showProtein",
                new Pair<String, ViewURLHelper>("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new Pair<String, ViewURLHelper>(MS2Manager.getRun(form.run).getDescription(), currentUrl.setAction("showRun")));
    }


    // TODO: Pass in Protein object
    protected VelocityView annotView(String title, int seqId) throws Exception
    {
        VelocityView retVal = new VelocityView("ProtAnnots.vm");
        if (title != null)
        {
            retVal.setTitle("Annotations for " + title);
        }
        /* collect header info */
        String SeqName = ProteinManager.getSeqParamFromId("BestName", seqId);
        String SeqDesc = ProteinManager.getSeqParamFromId("Description", seqId);
        String GeneNames[] = ProteinManager.getIdentifiersFromId("GeneName", seqId);
        /* collect first table info */
        String GenBankIds[] = ProteinManager.getIdentifiersFromId("GenBank", seqId);
        String SwissProtNames[] = ProteinManager.getIdentifiersFromId("SwissProt", seqId);
        String EnsemblIDs[] = ProteinManager.getIdentifiersFromId("Ensembl", seqId);
        String GIs[] = ProteinManager.getIdentifiersFromId("GI", seqId);
        String SwissProtAccns[] = ProteinManager.getIdentifiersFromId("SwissProtAccn", seqId);
        String GOCategories[] = ProteinManager.getGOCategoriesFromId(seqId);
        String IPIds[] = ProteinManager.getIdentifiersFromId("IPI", seqId);
        String RefSeqIds[] = ProteinManager.getIdentifiersFromId("REFSEQ", seqId);
        HashSet<String> allGbIds = new HashSet<String>();
        HashSet<String> allGbURLs = new HashSet<String>();

        for (String GenBankId : GenBankIds) allGbIds.add(GenBankId);
        for (String RefSeqId : RefSeqIds) allGbIds.add(RefSeqId);
        for (Object allGbId : allGbIds)
        {
            String ident = (String) allGbId;
            String url = ProteinManager.makeFullAnchorString(
                    ProteinManager.makeAnyKnownIdentURLString(ident, 1),
                    "protWindow",
                    ident);
            allGbURLs.add(url);
        }

        /* info from db into view */
        retVal.addObject("SeqName", SeqName);
        retVal.addObject("SeqDesc", SeqDesc);
        if (GeneNames != null && GeneNames.length > 0)
            retVal.addObject("GeneName", StringUtils.join(ProteinManager.makeFullAnchorStringArray(GeneNames, "protWindow", "GeneName"), ", "));
        retVal.addObject("SeqOrgs", ProteinManager.getOrganismsFromId(seqId));

        retVal.addObject("GenBankIds", allGbURLs);

        retVal.addObject("SwissProtNames", ProteinManager.makeFullAnchorStringArray(SwissProtNames, "protWindow", "SwissProt"));
        retVal.addObject("SwissProtAccns", ProteinManager.makeFullAnchorStringArray(SwissProtAccns, "protWindow", "SwissProtAccn"));
        retVal.addObject("GIs", ProteinManager.makeFullAnchorStringArray(GIs, "protWindow", "GI"));
        retVal.addObject("EnsemblIDs", ProteinManager.makeFullAnchorStringArray(EnsemblIDs, "protWindow", "Ensembl"));
        retVal.addObject("GOCategories", ProteinManager.makeFullGOAnchorStringArray(GOCategories, "protWindow"));

        // It is convenient to strip the version numbers from the IPI identifiers
        // and this may cause some duplications.  Use a hash-set to compress
        // duplicates
        HashSet<String> IPIset = new HashSet<String>();

        for (String idWithoutVersion : IPIds)
        {
            int dotIndex = idWithoutVersion.indexOf(".");
            if (dotIndex != -1) idWithoutVersion = idWithoutVersion.substring(0, dotIndex);
            IPIset.add(idWithoutVersion);
        }

        IPIds = new String[IPIset.size()];
        IPIset.toArray(IPIds);

        retVal.addObject("IPIDs", ProteinManager.makeFullAnchorStringArray(IPIds, "protWindow", "IPI"));

        return retVal;
    }


    private String getProteinTitle(Protein p, boolean includeBothNames)
    {
        if (null == p.getLookupString())
            return p.getBestName();

        if (!includeBothNames || p.getLookupString().equalsIgnoreCase(p.getBestName()))
            return p.getLookupString();

        return p.getLookupString() + " (" + p.getBestName() + ")";
    }


    private VBox getShowProteinsView(ViewURLHelper currentUrl, MS2Run run, DetailsForm form, Protein[] proteins, String[] peptides, AbstractPeptideView peptideView) throws Exception
    {
        VBox vbox = new VBox();

        // Limit to 100 proteins
        int proteinCount = Math.min(100, proteins.length);
        boolean stringSearch = (null != peptides);
        boolean showPeptides = !stringSearch && run != null;

        if (showPeptides)
        {
            peptides = peptideView.getPeptideStringsForGrouping(form);
        }

        for (int i = 0; i < proteinCount; i++)
        {
            VelocityView proteinSummary = new VelocityView("protein.vm");

            proteins[i].setPeptides(peptides);
            proteins[i].setShowEntireFragmentInCoverage(stringSearch);
            proteinSummary.addObject("protein", proteins[i]);
            proteinSummary.setTitle(getProteinTitle(proteins[i], true));
            proteinSummary.addObject("integer", Formats.commaf0);

            if (showPeptides)
                proteinSummary.addObject("percent", Formats.percent);
            vbox.addView(new HtmlView("<a name=\"Protein" + i + "\"/>"));
            vbox.addView(proteinSummary);
            // Add annotations
            vbox.addView(annotView(null, proteins[i].getSeqId()));
        }

        if (showPeptides)
        {
            VelocityView peptideFilter = new VelocityView("renderFilter.vm");
            vbox.addView(peptideFilter);
            vbox.addView(new HtmlView("<a name=\"Peptides\"/>"));
            List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();

            GridView peptidesGridView = peptideView.createPeptideViewForGrouping(form);

            peptidesGridView.getDataRegion().removeColumnsFromDisplayColumnList("Description,Protein,GeneName,SeqId");
            String peptideFilterString = ProteinManager.getPeptideFilter(currentUrl, run, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER).getFilterText(MS2Manager.getSqlDialect());
            sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilterString));
            sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText(MS2Manager.getSqlDialect())));
            peptideFilter.addObject("sqlSummaries", sqlSummaries);
            peptideFilter.setTitle("Peptides");

            vbox.addView(peptidesGridView);
        }

        return vbox;
    }

    @Jpf.Action
    protected Forward showMS2Admin() throws Exception
    {
        requiresGlobalAdmin();

        int days = getDays();

        Map stats = MS2Manager.getStats(days);
        VelocityView vv = new VelocityView("ms2Admin.vm");
        vv.addObject("stats", stats);
        vv.addObject("days", days);
        vv.addObject("purgeStatus", MS2Manager.getPurgeStatus());

        Map<String, String> urls = new HashMap<String, String>(10);
        urls.put("successful", showRunsUrl(false, 1));
        urls.put("inprocess", showRunsUrl(false, 0));
        urls.put("failed", showRunsUrl(false, 2));
        urls.put("deleted", showRunsUrl(true, null));

        vv.addObject("urls", urls);

        return _renderInTemplate(vv, false, "MS2 Admin", null);
    }


    private int getDays()
    {
        int days = 14;

        String daysParam = (String)getViewContext().get("days");

        if (null != daysParam)
        {
            try
            {
                days = Integer.parseInt(daysParam);
            }
            catch(NumberFormatException e)
            {
                // Just use the default if we can't parse the parameter
            }
        }

        return days;
    }


    private String showRunsUrl(Boolean deleted, Integer statusId)
    {
        ViewURLHelper url = new ViewURLHelper("MS2", "showAllRuns", (String)null);

        if (null != deleted)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".Deleted~eq", deleted.booleanValue() ? "1" : "0");

        if (null != statusId)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".StatusId~eq", String.valueOf(statusId));

        return url.getEncodedLocalURIString();
    }


    @Jpf.Action
    protected Forward showAllRuns() throws Exception
    {
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameRuns());
        ContainerDisplayColumn cdc = new ContainerDisplayColumn(MS2Manager.getTableInfoRuns().getColumn("Container"));
        cdc.setCaption("Folder");

        ViewURLHelper containerUrl = cloneViewURLHelper().setAction("showList");

        // We don't want ViewURLHelper to encode ${ContainerPath}, so set a dummy value and use string substitution
        String urlString = containerUrl.setExtraPath("ContainerPath").getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/");
        cdc.setURL(urlString);
        rgn.addColumn(cdc);
        rgn.addColumns(MS2Manager.getTableInfoRuns().getColumns("Description, Path, Created, Deleted, StatusId, Status, PeptideCount, SpectrumCount, FastaId"));

        ViewURLHelper showRunUrl = new ViewURLHelper("MS2", "showRun", "ContainerPath");
        String showUrlString = showRunUrl.getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/") + "run=${Run}";
        rgn.getDisplayColumn("Description").setURL(showUrlString);

        GridView gridView = new GridView(rgn);
        gridView.getViewContext().setContainer(ContainerManager.getNullContainer());
        gridView.getViewContext().setPermissions(ACL.PERM_READ);
        SimpleFilter runFilter = new SimpleFilter();

        runFilter.addInClause("Container", ContainerManager.getIds(getUser(), ACL.PERM_READ));

        gridView.setFilter(runFilter);
        gridView.setTitle("Show All Runs");

        // TODO: Help topic
        return _renderInTemplate(gridView, true, "Show All Runs", null);
    }


    @Jpf.Action
    protected Forward showProteinAdmin() throws Exception
    {
        requiresGlobalAdmin();

        GridView grid = new GridView(getFastaAdminGrid());
        grid.setTitle("FASTA Files");

        grid.getViewContext().setPermissions(ACL.PERM_READ);

        GridView annots = new GridView(getAnnotInsertsGrid());
        annots.setTitle("Protein Annotations Loaded");

        annots.getViewContext().setPermissions(ACL.PERM_READ);

        return _renderInTemplate(new VBox(grid, annots), false, null, null);
    }


    @Jpf.Action
    protected Forward purgeRuns() throws IOException, ServletException, URISyntaxException, SQLException
    {
        requiresGlobalAdmin();

        int days = getDays();

        MS2Manager.purgeDeleted(days);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        currentUrl.setAction("showMS2Admin");
        currentUrl.deleteParameters();
        currentUrl.addParameter("days", String.valueOf(days));

        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward deleteDataBases() throws IOException, ServletException, URISyntaxException, SQLException
    {
        requiresGlobalAdmin();

        HttpServletRequest request = getRequest();
        String[] fastaIds = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        String idList = StringUtils.join(fastaIds, ',');
        Integer[] validIds = Table.executeArray(ProteinManager.getSchema(), "SELECT FastaId FROM " + ProteinManager.getTableInfoFastaAdmin() + " WHERE (FastaId <> 0) AND (Runs IS NULL) AND (FastaId IN (" + idList + "))", new Object[]{}, Integer.class);

        for (int id : validIds)
            ProteinManager.deleteFastaFile(id);

        return new ViewForward("MS2", "showProteinAdmin", "");
    }


    @Jpf.Action
    protected Forward updateSeqIds() throws Exception
    {
        requiresGlobalAdmin();

        HttpServletRequest request = getRequest();
        String[] fastaIds = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);

        List<Integer> ids = new ArrayList<Integer>(fastaIds.length);

        for (String fastaId : fastaIds)
        {
            int id = Integer.parseInt(fastaId);
            if (0 != id)
                ids.add(id);
        }

        FastaDbLoader.updateSeqIds(ids);

        return new ViewForward("MS2", "showProteinAdmin", "");
    }


    private DataRegion getFastaAdminGrid()
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(ProteinManager.getTableInfoFastaAdmin().getColumns("FileName, Loaded, FastaId, Runs"));
        String runsUrl = ViewURLHelper.toPathString("MS2", "showAllRuns", (String)null) + "?" + MS2Manager.getDataRegionNameRuns() + ".FastaId~eq=${FastaId}";
        rgn.getDisplayColumn("Runs").setURL(runsUrl);
        rgn.setFixedWidthColumns(false);
        rgn.setShowRecordSelectors(true);

        ButtonBar bb = new ButtonBar();

        ActionButton delete = new ActionButton("", "Delete");
        delete.setScript("return verifySelected(this.form, \"deleteDataBases.post\", \"post\", \"databases\")");
        delete.setActionType(ActionButton.Action.GET);
        bb.add(delete);

        ActionButton update = new ActionButton("button", "Update SeqIds");
        update.setScript("return verifySelected(this.form, \"updateSeqIds.post\", \"post\", \"databases\")");
        update.setActionType(ActionButton.Action.GET);
        bb.add(update);

        rgn.setButtonBar(bb, DataRegion.MODE_GRID);
        return rgn;
    }

    public static class MS2WebPart extends WebPartView
    {
        public MS2WebPart()
        {
        }


        @Override
        public void renderView(Object model, PrintWriter out) throws Exception
        {
            Container c = hasAccess(getViewContext(), "MS2 Runs");
            if (c == null)
            {
                return;
            }

            DataRegion rgn = getGridRegionWebPart(c);
            rgn.getDisplayColumn(0).setURL(ViewURLHelper.toPathString("MS2", "showRun", c.getPath()) + "?run=${Run}");

            GridView gridView = new GridView(rgn);
            gridView.setCustomizeLinks(getCustomizeLinks());
            gridView.setTitle("MS2 Runs");
            gridView.setTitleHref(ViewURLHelper.toPathString("MS2", "showList", c.getPath()));
            gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
            gridView.setSort(MS2Manager.getRunsBaseSort());

            include(gridView);
        }


        private DataRegion getGridRegionWebPart(Container c)
        {
            DataRegion rgn = new DataRegion();
            rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
            TableInfo ti = MS2Manager.getTableInfoExperimentRuns();
            ColumnInfo[] cols = ti.getColumns("Description", "Path", "Created", "Run", "ExperimentRunLSID", "ProtocolName", "ExperimentRunRowId");
            rgn.setColumns(cols);
            rgn.getDisplayColumn(3).setVisible(false);
            rgn.getDisplayColumn(4).setVisible(false);
            rgn.getDisplayColumn(5).setVisible(false);
            rgn.getDisplayColumn(6).setVisible(false);

            ExperimentInfoColumns experimentCols = new ExperimentInfoColumns(c);
            DisplayColumn dcSample = experimentCols.getSampleColumn();
            rgn.addColumn(dcSample);

            DisplayColumn dcProtocol = experimentCols.getProtocolColumn();
            rgn.addColumn(dcProtocol);

            rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
            return rgn;
        }
    }

    /**
     * Class holds a bunch of info about all the experiments in this folder.
     */
    public static class ExperimentInfoColumns
    {
        private Map<String, List<Data>> runInputData;
        private Map<String, List<Material>> runInputMaterial;
        private Map<String, String> dataCreatingRuns;

        public ExperimentInfoColumns(Container c)
        {
            runInputData = ExperimentManager.get().getRunInputData(c);
            runInputMaterial = ExperimentManager.get().getRunInputMaterial(c);
            dataCreatingRuns = ExperimentManager.get().getDataCreatingRuns(c);
        }

        private String getDataCreatingRun(String lsid)
        {
            if (dataCreatingRuns.containsKey(lsid))
            {
                return dataCreatingRuns.get(lsid);
            }
            String result = ExperimentManager.get().getDataCreatingRun(lsid);
            dataCreatingRuns.put(lsid, result);
            return result;
        }

        private List<Data> getRunInputData(String lsid)
        {
            List<Data> result = runInputData.get(lsid);
            if (result == null)
            {
                try
                {
                    result = ExperimentManager.get().getRunInputData(lsid);
                    runInputData.put(lsid, result);
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }
            return result;
        }

        private List<Material> getRunInputMaterial(String lsid)
        {
            List<Material> result = runInputMaterial.get(lsid);
            if (result == null)
            {
                try
                {
                    result = ExperimentManager.get().getRunInputMaterial(lsid);
                    runInputMaterial.put(lsid, result);
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }
            return result;
        }

        public SampleColumn getSampleColumn()
        {
            return new SampleColumn();
        }

        public ProtocolColumn getProtocolColumn()
        {
            return new ProtocolColumn();
        }

        public class SampleColumn extends SimpleDisplayColumn
        {
            public SampleColumn()
            {
                setWidth(null);
                setCaption("Sample");
            }

            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                assert ctx.containsKey("ExperimentRunLSID") : "Data region should select ExperimentRunLSID column";
                String runLSID = (String) ctx.get("ExperimentRunLSID");
                int run = ((Integer)ctx.get("Run")).intValue();
                calculateAndRenderLinks(runLSID, ctx.getViewContext().getViewURLHelper(), run, out, true);
            }

            private void calculateAndRenderLinks(String runLSID, ViewURLHelper urlHelper, int run, Writer out, boolean collapse)
                    throws IOException
            {
                //TODO: Should be able to annotate run from here.
                if (null == runLSID)
                    return;

                //Render links for all the direct and indirect inputs.
                List<Material> inputMaterials = new ArrayList<Material>(getRunInputMaterial(runLSID));
                List<Data> inputData = getRunInputData(runLSID);
                for (Data d : inputData)
                {
                    String creatingRunLsid = getDataCreatingRun(d.getLSID());
                    if (null != creatingRunLsid)
                    {
                        List<Material> tmpM = getRunInputMaterial(creatingRunLsid);
                        for (Material m : tmpM)
                        {
                            boolean foundMatch = false;
                            for (Material m2 : inputMaterials)
                            {
                                if (m2.getLSID().equals(m.getLSID()))
                                {
                                    foundMatch = true;
                                    break;
                                }
                            }
                            if (!foundMatch)
                            {
                                inputMaterials.add(m);
                            }
                        }
                    }
                }
                renderMaterialLinks(inputMaterials, urlHelper, run, out, collapse);
            }

            private void renderMaterialLinks(List<Material> inputMaterials, ViewURLHelper currentURL, int run, Writer out, boolean collapse) throws IOException
            {
                ViewURLHelper resolveHelper = currentURL.clone();
                String resolveURL = resolveHelper.relativeUrl("resolveLSID.view", "lsid=", "Experiment");
                ViewURLHelper fullMaterialListHelper = currentURL.clone();
                String fullMaterialListURL = fullMaterialListHelper.relativeUrl("showFullMaterialList.view", "run=" + run, "MS2");

                if (collapse && inputMaterials.size() > 2)
                {
                    renderMaterialLink(out, resolveURL, inputMaterials.get(0));
                    out.write(",<br/><a href=\"");
                    out.write(fullMaterialListURL);
                    StringBuilder sb = new StringBuilder();
                    String sep = "";
                    for (int i = 1; i < inputMaterials.size(); i++)
                    {
                        sb.append(sep);
                        sb.append(PageFlowUtil.filter(inputMaterials.get(i).getName()));
                        sep = ", ";
                    }
                    out.write("\" title=\"");
                    out.write(sb.toString());
                    out.write("\">");
                    out.write(Integer.toString(inputMaterials.size() - 1));
                    out.write(" more samples...</a>");
                }
                else
                {
                    String sep = "";
                    for (Material mat : inputMaterials)
                    {
                        out.write(sep);
                        renderMaterialLink(out, resolveURL, mat);
                        sep = ",<br/>";
                    }
                }
            }

            private void renderMaterialLink(Writer out, String resolveURL, Material mat)
                    throws IOException
            {
                out.write("<a href='");
                out.write(resolveURL);
                out.write(PageFlowUtil.filter(mat.getLSID()));
                out.write("'>");
                out.write(PageFlowUtil.filter(mat.getName()));
                out.write("</a>");
            }

        }

        public class ProtocolColumn extends SimpleDisplayColumn
        {
            public ProtocolColumn()
            {
                setWidth(null);
                setCaption("Protocol");
            }

            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                assert ctx.containsKey("ExperimentRunLSID") : "Data region should select ExperimentRunLSID column";
                assert ctx.containsKey("ExperimentRunRowId") : "Data region should select ExperimentRunRowId column";
                assert ctx.containsKey("ProtocolName") : "Data region should select ProtocolName column";
                String runLSID = (String) ctx.get("ExperimentRunLSID");
                if (null == runLSID)
                    return;
                Integer runRowId = (Integer) ctx.get("ExperimentRunRowId");
                String protocolName = (String) ctx.get("ProtocolName");

                String runLink = getRunLink(ctx, runRowId, protocolName);
                List<Data> inputData = getRunInputData(runLSID);
                if (null == inputData)
                {
                    out.write(runLink);
                    return;
                }

                Map<String, String> predecessorLinks = new HashMap<String, String>();

                for (Data data : inputData)
                {
                    //TODO: Make this faster. All of this should be queried at the same time..
                    //ExperimentRun and Protocol are cached however, so in usual cases this isn't tragic
                    String creatingRunLsid = getDataCreatingRun(data.getLSID());
                    if (null == creatingRunLsid)
                        continue;
                    if (predecessorLinks.containsKey(creatingRunLsid))
                        continue;

                    ExperimentRun run = ExperimentManager.get().getExperimentRun(creatingRunLsid);
                    Protocol p = ExperimentManager.get().getProtocol(run.getProtocolLSID());
                    predecessorLinks.put(creatingRunLsid, getRunLink(ctx, run.getRowId(), p.getName()));
                }

                if (predecessorLinks.values().size() == 0)
                    out.write(runLink);
                else
                {
                    String sep = "";
                    for (String s : predecessorLinks.values())
                    {
                        out.write(sep);
                        out.write(s);
                        sep = ", ";
                    }
                    out.write(" -> ");
                    out.write(runLink);
                }
            }

            public String getRunLink(RenderContext ctx, Integer experimentRunRowId, String protocolName)
            {
                ViewURLHelper helper = ctx.getViewContext().getViewURLHelper();
                String resolveURL = helper.relativeUrl("showRunGraph.view", "rowId=", "Experiment");

                return "<a href='" + resolveURL + experimentRunRowId + "'>" + PageFlowUtil.filter(protocolName) + "</a>";
            }
        }
    }

    @Jpf.Action
    protected Forward testMascot(TestMascotForm form) throws Exception
    {
        requiresGlobalAdmin();

        String originalMascotServer = form.getMascotServer();
        MascotClientImpl mascotClient = new MascotClientImpl(form.getMascotServer(), null,
            form.getUserAccount(), form.getPassword());
        mascotClient.setProxyURL(form.getHTTPProxyServer());
        mascotClient.findWorkableSettings(true);
        form.setStatus(mascotClient.getErrorCode());

        String message;
        if (0 == mascotClient.getErrorCode())
        {
            message = "Test passed.";
            form.setParameters(mascotClient.getParameters());
        }
        else
        {
            message = "Test failed.";
            message = message + "<br>" + mascotClient.getErrorString();
        }
        form.setMessage(message);

        form.setMascotServer(originalMascotServer);

        HttpView view = new GroovyView("admin/testMascot.gm");
        form.setPassword(("".equals(form.getPassword())) ? "" : "***");  // do not show password in clear
        view.addObject("form", form);
        return includeView(new LoginTemplate(view));
    }

    @Jpf.Action
    protected Forward showMascotTest(TestMascotForm form) throws Exception
    {
        return testMascot (form);
    }

    @Jpf.Action
    protected Forward showUpgradeMascotTest(TestMascotForm form) throws Exception
    {
        return testMascot (form);
    }

    public static class TestMascotForm extends FormData
    {
        private String mascotserver;
        private String useraccount;
        private String password;
        private String httpproxyserver;
        private int status;
        private String parameters;
        private String message;

        public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            setMascotServer(httpServletRequest.getParameter("mascotServer").trim());
            setUserAccount(httpServletRequest.getParameter("mascotUserAccount").trim());
            setPassword(httpServletRequest.getParameter("mascotUserPassword").trim());
            setHTTPProxyServer(httpServletRequest.getParameter("mascotHTTPProxy").trim());
            super.reset(actionMapping, httpServletRequest);
        }

        public String getUserAccount()
        {
            return (null == useraccount ? "" : useraccount);
        }

        public void setUserAccount(String useraccount)
        {
            this.useraccount = useraccount;
        }

        public String getPassword()
        {
            return (null == password ? "" : password);
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getMascotServer()
        {
            return (null == mascotserver ? "" : mascotserver);
        }

        public void setMascotServer(String mascotserver)
        {
            this.mascotserver = mascotserver;
        }

        public String getHTTPProxyServer()
        {
            return (null == httpproxyserver ? "" : httpproxyserver);
        }

        public void setHTTPProxyServer(String httpproxyserver)
        {
            this.httpproxyserver = httpproxyserver;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public String getParameters()
        {
            return (null == parameters ? "" : parameters);
        }

        public void setParameters(String parameters)
        {
            this.parameters = parameters;
        }
    }

    @Jpf.Action
    protected Forward exportHistory() throws ServletException
    {
        requiresLogin();

        try
        {
            TableInfo tinfo = MS2Manager.getTableInfoHistory();
            ExcelWriter ew = new ExcelWriter(MS2Manager.getSchema(), "SELECT * FROM " + MS2Manager.getTableInfoHistory() + " ORDER BY Date");
            ew.setColumns(tinfo.getColumns());
            ew.setSheetName("MS2 History");
            ew.write(getResponse());
        }
        catch (SQLException e)
        {
            _log.error(e);
        }

        return null;
    }


    public static class MS2StatsWebPart extends VelocityView
    {
        public MS2StatsWebPart()
        {
            super("/MS2/stats.vm");
            setTitle("MS2 Statistics");
            Map stats;
            try
            {
                stats = MS2Manager.getBasicStats();
            } catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            addObject("stats", stats);
        }


        @Override
        protected void prepareWebPart(Object model)
        {
            if (!getViewContext().getUser().isGuest())
                setTitleHref(ViewURLHelper.toPathString("MS2", "exportHistory", ""));
        }
    }


    public static class LoadAnnotForm extends FormData
    {
        private String fileType;

        public void setFileType(String ft)
        {
            this.fileType = ft;
        }

        public String getFileType()
        {
            return this.fileType;
        }

        private String fileName;

        public void setFileName(String file)
        {
            this.fileName = file;
        }

        public String getFileName()
        {
            return this.fileName;
        }

        private String comment;

        public void setComment(String s)
        {
            this.comment = s;
        }

        public String getComment()
        {
            return this.comment;
        }

        private String defaultOrganism;

        public String getDefaultOrganism()
        {
            return this.defaultOrganism;
        }

        public void setDefaultOrganism(String o)
        {
            this.defaultOrganism = o;
        }

        private String shouldGuess;

        public String getShouldGuess()
        {
            return shouldGuess;
        }

        public void setShouldGuess(String shouldGuess)
        {
            this.shouldGuess = shouldGuess;
        }
    }


    public static class RunForm extends FormData
    {
        int run;
        int tryptic;
        boolean expanded;
        String grouping;
        String columns;
        String proteinColumns;
        String proteinGroupingId;

        ArrayList<String> errors;

        // Set form default values; will be overwritten by any params included on the url
        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
            run = 0;
            expanded = false;
            errors = new ArrayList<String>();
        }

        private int toInt(String s, String field)
        {
            try
            {
                return Integer.parseInt(s);
            }
            catch (NumberFormatException e)
            {
                errors.add("Error: " + s + " is not a valid value for " + field + ".");
                return 0;
            }
        }

        public List<String> getErrors()
        {
            return errors;
        }

        public void setExpanded(boolean expanded)
        {
            this.expanded = expanded;
        }

        public boolean getExpanded()
        {
            return this.expanded;
        }

        public void setRun(String run)
        {
            this.run = toInt(run, "Run");
        }

        public String getRun()
        {
            return String.valueOf(this.run);
        }

        public void setTryptic(String tryptic)
        {
            this.tryptic = toInt(tryptic, "Tryptic");
        }

        public String getTryptic()
        {
            return null;
        }

        public void setGrouping(String grouping)
        {
            this.grouping = grouping;
        }

        public String getGrouping()
        {
            return grouping;
        }

        public String getColumns()
        {
            return columns;
        }

        public void setColumns(String columns)
        {
            this.columns = columns;
        }

        public String getProteinColumns()
        {
            return proteinColumns;
        }

        public void setProteinColumns(String proteinColumns)
        {
            this.proteinColumns = proteinColumns;
        }

        public String getProteinGroupingId()
        {
            return proteinGroupingId;
        }

        public void setProteinGroupingId(String proteinGroupingId)
        {
            this.proteinGroupingId = proteinGroupingId;
        }
    }

    public static class ElutionProfileForm extends DetailsForm
    {
        private int _lightFirstScan;
        private int _lightLastScan;
        private int _heavyFirstScan;
        private int _heavyLastScan;

        public int getLightFirstScan()
        {
            return _lightFirstScan;
        }

        public void setLightFirstScan(int lightFirstScan)
        {
            _lightFirstScan = lightFirstScan;
        }

        public int getLightLastScan()
        {
            return _lightLastScan;
        }

        public void setLightLastScan(int lightLastScan)
        {
            _lightLastScan = lightLastScan;
        }

        public int getHeavyFirstScan()
        {
            return _heavyFirstScan;
        }

        public void setHeavyFirstScan(int heavyFirstScan)
        {
            _heavyFirstScan = heavyFirstScan;
        }

        public int getHeavyLastScan()
        {
            return _heavyLastScan;
        }

        public void setHeavyLastScan(int heavyLastScan)
        {
            _heavyLastScan = heavyLastScan;
        }
    }

    public static class DetailsForm extends RunForm
    {
        private long peptideId;
        private int rowIndex;
        private int height;
        private int width;
        private double tolerance;
        private double xEnd;
        private double xStart;
        private int seqId;
        private String extension;
        private String protein;
        private int quantitationCharge;
        private int groupNumber;
        private int indistinguishableCollectionId;

        public int getGroupNumber()
        {
            return groupNumber;
        }

        public void setGroupNumber(int groupNumber)
        {
            this.groupNumber = groupNumber;
        }

        public int getIndistinguishableCollectionId()
        {
            return indistinguishableCollectionId;
        }

        public void setIndistinguishableCollectionId(int indistinguishableCollectionId)
        {
            this.indistinguishableCollectionId = indistinguishableCollectionId;
        }

        public void setPeptideId(long peptideId)
        {
            this.peptideId = peptideId;
        }

        public long getPeptideId()
        {
            return this.peptideId;
        }

        public void setxStart(double xStart)
        {
            this.xStart = xStart;
        }

        public double getxStart()
        {
            return this.xStart;
        }

        public String getStringXStart()
        {
            return Double.MIN_VALUE == xStart ? "" : Formats.fv2.format(xStart);
        }

        public void setxEnd(double xEnd)
        {
            this.xEnd = xEnd;
        }

        public double getxEnd()
        {
            return this.xEnd;
        }

        public String getStringXEnd()
        {
            return Double.MAX_VALUE == xEnd ? "" : Formats.fv2.format(xEnd);
        }

        public void setTolerance(double tolerance)
        {
            this.tolerance = tolerance;
        }

        public double getTolerance()
        {
            return this.tolerance;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }

        public int getWidth()
        {
            return this.width;
        }

        public void setHeight(int height)
        {
            this.height = height;
        }

        public int getHeight()
        {
            return this.height;
        }

        // Set form default values for graphs and peptides; these will be overwritten by
        // any params included on the url
        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
            rowIndex = -1;
            xStart = Double.MIN_VALUE;
            xEnd = Double.MAX_VALUE;
            tolerance = 1.0;
            height = 400;
            width = 600;
            quantitationCharge = Integer.MIN_VALUE;
        }

        public void setRowIndex(int rowIndex)
        {
            this.rowIndex = rowIndex;
        }

        public int getRowIndex()
        {
            return this.rowIndex;
        }

        public void setSeqId(int seqId)
        {
            this.seqId = seqId;
        }

        public int getSeqId()
        {
            return this.seqId;
        }

        public String getExtension()
        {
            return extension;
        }

        public void setExtension(String extension)
        {
            this.extension = extension;
        }

        public String getProtein()
        {
            return protein;
        }

        public void setProtein(String protein)
        {
            this.protein = protein;
        }

        public int getQuantitationCharge()
        {
            return quantitationCharge;
        }

        public void setQuantitationCharge(int quantitationCharge)
        {
            this.quantitationCharge = quantitationCharge;
        }

    }

    public static class ExportForm extends RunForm
    {
        private String exportFormat;
        private int runList;

        // Set form default values; will be overwritten by any params included on the url
        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
        }

        public String getExportFormat()
        {
            return exportFormat;
        }

        public void setExportFormat(String exportFormat)
        {
            this.exportFormat = exportFormat;
        }

        public int getRunList() {
            return runList;
        }

        public void setRunList(int runList) {
            this.runList = runList;
        }
    }

    public static class ChartForm extends RunForm
    {
        private String chartType;

        public String getChartType()
        {
            return chartType;
        }

        public void setChartType(String chartType)
        {
            this.chartType = chartType;
        }
    }


    public static class PeptideProphetForm extends RunForm
    {
        private int charge;
        private boolean cumulative;

        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
            cumulative = false;
        }

        public int getCharge()
        {
            return charge;
        }

        public void setCharge(int charge)
        {
            this.charge = charge;
        }

        public boolean isCumulative()
        {
            return cumulative;
        }

        public void setCumulative(boolean cumulative)
        {
            this.cumulative = cumulative;
        }
    }


    public static class MS2ViewForm extends FormData
    {
        private String viewParams;
        private String returnUrl;
        private String name;
        private int run;
        private boolean shared;

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public void setReturnUrl(String returnUrl)
        {
            this.returnUrl = returnUrl;
        }

        public String getReturnUrl()
        {
            return this.returnUrl;
        }

        public void setViewParams(String viewParams)
        {
            this.viewParams = viewParams;
        }

        public String getViewParams()
        {
            return this.viewParams;
        }

        public void setRun(int run)
        {
            this.run = run;
        }

        public int getRun()
        {
            return this.run;
        }

        public boolean isShared()
        {
            return shared;
        }

        public void setShared(boolean shared)
        {
            this.shared = shared;
        }
    }


    /**
     * FormData get and set methods may be overwritten by the Form Bean editor.
     */
    public static class ColumnForm extends FormData
    {
        private boolean saveDefault;
        private String queryString;
        private String columns;

        public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            super.reset(actionMapping, httpServletRequest);
            saveDefault = false;
        }

        public boolean getSaveDefault()
        {
            return saveDefault;
        }

        public void setSaveDefault(boolean saveDefault)
        {
            this.saveDefault = saveDefault;
        }

        public String getQueryString()
        {
            return queryString;
        }

        public void setQueryString(String queryString)
        {
            this.queryString = queryString;
        }

        public String getColumns()
        {
            return columns;
        }

        public void setColumns(String columns)
        {
            this.columns = columns;
        }
    }
}
