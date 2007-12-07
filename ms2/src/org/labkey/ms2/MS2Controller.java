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

package org.labkey.ms2;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.*;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.DialogTemplate;
import org.labkey.api.view.template.FastTemplate;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.api.view.template.PrintTemplate;
import org.labkey.common.tools.MS2Modification;
import org.labkey.common.tools.PeptideProphetSummary;
import org.labkey.common.util.Pair;
import org.labkey.ms2.compare.CompareDataRegion;
import org.labkey.ms2.compare.CompareExcelWriter;
import org.labkey.ms2.compare.CompareQuery;
import org.labkey.ms2.compare.RunColumn;
import org.labkey.ms2.peptideview.*;
import org.labkey.ms2.pipeline.*;
import org.labkey.ms2.protein.*;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.NullOutputStream;
import org.labkey.ms2.protein.tools.PieJChartHelper;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.protocol.MascotSearchProtocolFactory;
import org.labkey.ms2.protocol.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.protocol.MS2SearchPipelineProtocol;
import org.labkey.ms2.query.*;
import org.labkey.ms2.search.ProteinSearchWebPart;

import javax.servlet.ServletException;
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


@Jpf.Controller(longLived = true)
public class MS2Controller extends ViewController
{
    private static final int MAX_INSERTIONS_DISPLAY_ROWS = 1000; // Limit annotation table insertions to 1000 rows

    private static Logger _log = Logger.getLogger(MS2Controller.class);
    private static final String MS2_VIEWS_CATEGORY = "MS2Views";

    public static final String CAPTION_SCORING_BUTTON = "Compare Scoring";


    private ButtonBar getListButtonBar(Container c)
    {
        ButtonBar bb = new ButtonBar();

        ActionButton compareRuns = new ActionButton("button", "Compare");
        compareRuns.setScript("return verifySelected(this.form, \"compare.view\", \"post\", \"runs\")");
        compareRuns.setActionType(ActionButton.Action.GET);
        compareRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareRuns);

        ActionButton compareScoring = new ActionButton("", CAPTION_SCORING_BUTTON);
        compareScoring.setScript("return verifySelected(this.form, \"" + ViewURLHelper.toPathString("MS2-Scoring", "compare", c.getPath())+ "\", \"get\", \"runs\")");
        compareScoring.setActionType(ActionButton.Action.GET);
        compareScoring.setDisplayPermission(ACL.PERM_READ);
        compareScoring.setVisible(false);   // Hidden unless turned on during grid rendering.
        bb.add(compareScoring);

        ActionButton exportRuns = new ActionButton("button", "MS2 Export");
        exportRuns.setScript("return verifySelected(this.form, \"pickExportRunsView.view\", \"post\", \"runs\")");
        exportRuns.setActionType(ActionButton.Action.GET);
        exportRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(exportRuns);

        ActionButton showHierarchy = new ActionButton("showHierarchy.view", "Show Hierarchy");
        showHierarchy.setActionType(ActionButton.Action.LINK);
        showHierarchy.setDisplayPermission(ACL.PERM_READ);
        bb.add(showHierarchy);

        ActionButton moveRuns = new ActionButton("", "Move");
        moveRuns.setScript("return verifySelected(this.form, \"selectMoveLocation.view\", \"get\", \"runs\")");
        moveRuns.setActionType(ActionButton.Action.GET);
        moveRuns.setDisplayPermission(ACL.PERM_DELETE);
        bb.add(moveRuns);

        ActionButton deleteRuns = new ActionButton("", "Delete");
        deleteRuns.setScript("return verifySelected(this.form, \"deleteRuns.view\", \"get\", \"runs\")");
        deleteRuns.setActionType(ActionButton.Action.GET);
        deleteRuns.setDisplayPermission(ACL.PERM_DELETE);
        bb.add(deleteRuns);

        return bb;
    }


    private enum Template
    {
        home
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new HomeTemplate(viewContext, view, navTrail);
            }
        },
        fast
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new FastTemplate(viewContext, view, navTrail);
            }
        },
        print
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new PrintTemplate(view, navTrail.getTitle());
            }
        },
        dialog
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new DialogTemplate(view);
            }
        };

        public abstract HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail);
    }


    private Forward _renderInTemplate(HttpView view, boolean fastTemplate, String title, String helpTopic, NavTree... navTrailChildren) throws Exception
    {
        return _renderInTemplate(view, fastTemplate ? Template.fast : Template.home, title, helpTopic, false, navTrailChildren);
    }

    private Forward _renderInTemplate(HttpView view, Template templateType, String title, String helpTopic, boolean exploratoryFeatures, NavTree... navTrailChildren) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "ms2";

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.CPAS));
        trailConfig.setExploratoryFeatures(exploratoryFeatures);
        trailConfig.setExtraChildren(navTrailChildren);

        HttpView template = templateType.createTemplate(getViewContext(), view, trailConfig);

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
        else if (run.getStatusId() == MS2Importer.STATUS_RUNNING)
            message = "Run is still loading.  Current status: " + run.getStatus();
        else if (run.getStatusId() == MS2Importer.STATUS_FAILED)
            message = "Run failed loading.  Status: " + run.getStatus();
        else
        {
            String cId = run.getContainer();

            if (null != cId && cId.equals(c.getId()))
                success = true;
            else
            {
                ViewURLHelper url = getViewURLHelper().clone();
                url.setExtraPath(ContainerManager.getForId(run.getContainer()).getPath());
                HttpView.throwRedirect(url);
            }
        }

        if (null != message)
        {
            HttpView.throwNotFound(message);
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
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
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
            PipeRoot pr = service.findPipelineRoot(c);
            if (pr != null)
            {
                URI uriData = URIUtil.resolve(pr.getUri(c), path);
                if (uriData != null)
                {
                    f = new File(uriData);
                }
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
            HttpView.throwNotFound("Unable to open the file '" + form.getPath() + "' to load as a ProteinProphet file");
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


    private Forward showRuns(ButtonBar bb, String pageName, String helpTopic) throws Exception
    {
        ViewURLHelper currentUrl = cloneViewURLHelper();
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
        rgn.setColumns(MS2Manager.getTableInfoExperimentRuns().getColumns("Description, Path, Created, Status, ExperimentRunLSID, ExperimentRunRowId, ProtocolName, PeptideCount, NegativeHitCount"));
        for (int i = 4; i < rgn.getDisplayColumnList().size(); i++)
            rgn.getDisplayColumn(i).setVisible(false);

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

        ProteinSearchWebPart searchView = new ProteinSearchWebPart(true);

        ViewURLHelper url = cloneViewURLHelper();
        url.deleteParameters();
        url.setPageFlow("protein");
        url.setAction("begin.view");

        VBox vbox = new VBox(searchView, gridView);

        return _renderInTemplate(vbox, false, pageName, helpTopic);
    }

    @Jpf.Action
    protected Forward applyRunView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // Forward with redirect: this lets struts fill in the form and ensures that the JavaScript sees the showRun action
        return new ViewForward(getApplyViewForwardUrl(form, "showRun"), true);
    }


    @Jpf.Action
    protected Forward getProteinGroupingPeptides(RunForm form) throws Exception
    {
        MS2Run run = MS2Manager.getRun(form.getRun());

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        GridView gridView = peptideView.getPeptideViewForProteinGrouping(form.getProteinGroupingId(), form.getColumns());
        return includeView(gridView);
    }

    @Jpf.Action
    protected Forward showRun(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        long time = System.currentTimeMillis();

        if (form.getErrors().size() > 0)
            return _renderErrors(form.getErrors());

        if (!isAuthorized(form.run))
            return null;

        ViewURLHelper currentUrl = getViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        WebPartView grid = peptideView.createGridView(form);
        List<DisplayColumn> displayColumns;
        String dataRegionName;
        if (grid instanceof QueryView)
        {
            displayColumns = ((QueryView)grid).getDisplayColumns();
            dataRegionName = ((QueryView)grid).getDataRegionName();
        }
        else
        {
            displayColumns = ((GridView)grid).getDataRegion().getDisplayColumnList();
            dataRegionName = ((GridView)grid).getDataRegion().getName();
            if (form.isExportAsWebPage())
                ((GridView)grid).getDataRegion().addHiddenFormField("exportAsWebPage", "true");
        }

        VBox vBox = new VBox();
        GroovyView scriptView = new GroovyView("/org/labkey/ms2/nestedGridScript.gm");
        scriptView.addObject("DataRegionName", dataRegionName);
        vBox.addView(scriptView);

        JspView<RunSummaryBean> runSummary = new JspView<RunSummaryBean>("/org/labkey/ms2/runSummary.jsp", new RunSummaryBean());
        RunSummaryBean bean = runSummary.getModelBean();
        bean.run = run;
        bean.modHref = modificationHref(run);
        bean.writePermissions = getViewContext().hasPermission(ACL.PERM_UPDATE);
        bean.quantAlgorithm = MS2Manager.getQuantAnalysisAlgorithm(form.run);
        vBox.addView(runSummary);

        vBox.addView(new FilterHeaderView(currentUrl, form, run));

        List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run);
        peptideView.addSQLSummaries(peptideFilter, sqlSummaries);

        vBox.addView(new CurrentFilterView(null, sqlSummaries));

        boolean exploratoryFeatures = false;
        for (DisplayColumn displayColumn : displayColumns)
        {
            if (displayColumn.getName().equalsIgnoreCase("H") ||
                displayColumn.getName().equalsIgnoreCase("DeltaScan") ||
                run instanceof XTandemRun && run.getHasPeptideProphet() && displayColumn.getName().equalsIgnoreCase("PeptideProphet"))
            {
                exploratoryFeatures = true;
                break;
            }
        }

        vBox.addView(grid);
        _log.debug("Lookup took " + (System.currentTimeMillis() - time) + " milliseconds");
        time = System.currentTimeMillis();
        _renderInTemplate(vBox, Template.fast, run.getDescription(), "viewRun", exploratoryFeatures,
                new NavTree("MS2 Runs", new ViewURLHelper("MS2", "showList", getViewURLHelper().getExtraPath())));
        _log.debug("Render took " + (System.currentTimeMillis() - time) + " milliseconds");
        return null;
    }


    public static class RunSummaryBean
    {
        public MS2Run run;
        public String modHref;
        public boolean writePermissions;
        public String quantAlgorithm;
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

        RenameBean bean = new RenameBean();

        MS2Run run = MS2Manager.getRun(form.getRun());
        String description = form.getDescription();
        if (description == null || description.length() == 0)
            description = run.getDescription();

        bean.run = run;
        bean.description = description;

        HttpView view = new JspView<RenameBean>("/org/labkey/ms2/renameRun.jsp", bean);

        return _renderInTemplate(view, true, "Rename Run", null);
    }

    public class RenameBean
    {
        public MS2Run run;
        public String description;
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

    private class FilterHeaderView extends JspView<FilterHeaderBean>
    {
        private FilterHeaderView(ViewURLHelper currentUrl, RunForm form, MS2Run run) throws ServletException, SQLException
        {
            super("/org/labkey/ms2/filterHeader.jsp", new FilterHeaderBean());

            FilterHeaderBean bean = getModelBean();

            bean.run = run;
            bean.applyViewUrl = clearFilter(currentUrl).setAction("applyRunView");
            bean.applyView = renderViewSelect(0, true, ACL.PERM_READ, true);
            bean.saveViewUrl = currentUrl.clone().setAction("pickName");
            bean.manageViewsUrl = currentUrl.clone().setAction("manageViews");
            bean.pickPeptideColumnsUrl = currentUrl.clone().setAction("pickPeptideColumns");
            bean.pickProteinColumnsUrl = currentUrl.clone().setAction("pickProteinColumns");
            bean.viewTypes = MS2RunViewType.getTypesForRun(run);
            bean.currentViewType = MS2RunViewType.getViewType(form.getGrouping());
            bean.expanded = form.getExpanded();

            String chargeFilterParamName = run.getChargeFilterParamName();
            ViewURLHelper extraFilterUrl = currentUrl.clone().setAction("addExtraFilter.post");
            extraFilterUrl.deleteParameter(chargeFilterParamName + "1");
            extraFilterUrl.deleteParameter(chargeFilterParamName + "2");
            extraFilterUrl.deleteParameter(chargeFilterParamName + "3");
            extraFilterUrl.deleteParameter("tryptic");
            extraFilterUrl.deleteParameter("grouping");
            extraFilterUrl.deleteParameter("expanded");
            bean.extraFilterUrl = extraFilterUrl;

            bean.charge1 = defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "1"), "0");
            bean.charge2 = defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "2"), "0");
            bean.charge3 = defaultIfNull(currentUrl.getParameter(chargeFilterParamName + "3"), "0");
            bean.tryptic = form.tryptic;

            setTitle("View");
        }
    }


    public static class FilterHeaderBean
    {
        public MS2Run run;
        public ViewURLHelper applyViewUrl;
        public StringBuilder applyView;        
        public ViewURLHelper saveViewUrl;
        public ViewURLHelper manageViewsUrl;
        public ViewURLHelper pickPeptideColumnsUrl;
        public ViewURLHelper pickProteinColumnsUrl;
        public ViewURLHelper extraFilterUrl;
        public List<MS2RunViewType> viewTypes;
        public MS2RunViewType currentViewType;
        public boolean expanded;
        public String charge1;
        public String charge2;
        public String charge3;
        public int tryptic;
    }


    public static String defaultIfNull(String s, String def)
    {
        return (null != s ? s : def);
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
            Map<String, String> properties = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

            for (Map.Entry<String, String> entry : properties.entrySet())
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
            Map<String, String> mShared = PropertyManager.getProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY, true);
            for (Map.Entry<String, String> entry : mShared.entrySet())
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


    private static ViewURLHelper clearFilter(ViewURLHelper currentUrl)
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

        if (!isAuthorized(form.run))
        {
            return null;
        }

        JspView<PickNameBean> pickName = new JspView<PickNameBean>("/org/labkey/ms2/pickName.jsp", new PickNameBean());
        PickNameBean bean = pickName.getModelBean();
        bean.returnUrl = cloneViewURLHelper().setAction("showRun");
        bean.canShare = getContainer().hasPermission(getUser(), ACL.PERM_INSERT);

        ViewURLHelper newUrl = bean.returnUrl.clone().deleteParameter("run");
        bean.viewParams = newUrl.getRawQuery();

        MS2Run run = MS2Manager.getRun(form.run);
        return _renderInTemplate(pickName, false, "Save View", "viewRun",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(run.getDescription(), bean.returnUrl));
    }


    public static class PickNameBean
    {
        public ViewURLHelper returnUrl;
        public boolean canShare;
        public String viewParams;
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

        ManageViewsBean bean = new ManageViewsBean();
        bean.postUrl = postUrl;
        bean.select = renderViewSelect(10, false, ACL.PERM_DELETE, false);

        HttpView manageViews = new JspView<ManageViewsBean>("/org/labkey/ms2/manageViews.jsp", bean);

        return _renderInTemplate(manageViews, false, "Manage Views", "viewRun",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(run.getDescription(), runUrl));
    }


    public static class ManageViewsBean
    {
        public ViewURLHelper postUrl;
        public StringBuilder select;
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


    @Jpf.Action
    protected Forward pickPeptideColumns(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper url = cloneViewURLHelper();
        MS2Run run = MS2Manager.getRun(form.run);
        if (run == null)
        {
            return HttpView.throwNotFound("Could not find run");
        }

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickPeptideColumns.jsp", new PickColumnsBean());
        PickColumnsBean bean = pickColumns.getModelBean();
        bean.commonColumns = run.getCommonPeptideColumnNames();
        bean.proteinProphetColumns = run.getProteinProphetPeptideColumnNames();
        bean.quantitationColumns = run.getQuantitationPeptideColumnNames();

        // Put a space between each name
        bean.defaultColumns = peptideView.getPeptideColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
        bean.currentColumns = peptideView.getPeptideColumnNames(form.getColumns()).replaceAll(" ", "").replaceAll(",", ", ");

        url.deleteParameter("columns");

        bean.queryString = url.getRawQuery();
        url.deleteParameters().setAction("savePeptideColumns");
        bean.saveUrl = url;
        bean.saveDefaultUrl = url.clone().addParameter("saveDefault", "1");
        return _renderInTemplate(pickColumns, false, "Pick Peptide Columns", "pickPeptideColumns",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(run.getDescription(), cloneViewURLHelper().setAction("showRun")));
    }


    public static class PickColumnsBean
    {
        public String commonColumns;
        public String proteinProphetColumns;
        public String quantitationColumns;
        public String defaultColumns;
        public String currentColumns;
        public String queryString;
        public ViewURLHelper saveUrl;
        public ViewURLHelper saveDefaultUrl;
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

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickProteinColumns.jsp", new PickColumnsBean());
        PickColumnsBean bean = pickColumns.getModelBean();

        bean.commonColumns = MS2Run.getCommonProteinColumnNames();
        bean.proteinProphetColumns = MS2Run.getProteinProphetProteinColumnNames();
        bean.quantitationColumns = run.getQuantitationProteinColumnNames();

        // Put a space between each name
        bean.defaultColumns = peptideView.getProteinColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
        bean.currentColumns = peptideView.getProteinColumnNames(form.getProteinColumns()).replaceAll(" ", "").replaceAll(",", ", ");

        url.deleteParameter("proteinColumns");

        bean.queryString = url.getRawQuery();
        url.deleteParameters().setAction("saveProteinColumns");
        bean.saveUrl = url;
        bean.saveDefaultUrl = url.clone().addParameter("saveDefault", "1");
        return _renderInTemplate(pickColumns, false, "Pick Protein Columns", "pickProteinColumns",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(run.getDescription(), cloneViewURLHelper().setAction("showRun")));
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
            if (run == null)
            {
                return HttpView.throwNotFound("Could not find run with id " + returnUrl.getParameter("run"));
            }
            AbstractMS2RunView view = getPeptideView(returnUrl.getParameter("grouping"), run);
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
            AbstractMS2RunView view = getPeptideView(returnUrl.getParameter("grouping"), run);
            view.saveProteinColumnNames(run.getType(), columnNames);
        }
        else
            returnUrl.replaceParameter("proteinColumns", columnNames);

        return new ViewForward(returnUrl);
    }


    private AbstractMS2RunView getPeptideView(String grouping, MS2Run... runs) throws ServletException
    {
        return MS2RunViewType.getViewType(grouping).createView(getViewContext(), runs);
    }


    public long[] getPeptideIndex(ViewURLHelper currentUrl, MS2Run run) throws SQLException, ServletException
    {
        AbstractMS2RunView view = getPeptideView(currentUrl.getParameter("grouping"), run);
        return view.getPeptideIndex(currentUrl);
    }


    @Jpf.Action
    protected Forward processAnnots(LoadAnnotForm form) throws Exception
    {
        requiresGlobalAdmin();

        if (null != form)
        {
            String fname = form.getFileName();
            String comment = form.getComment();

            DefaultAnnotationLoader loader;

            //TODO: this style of dealing with different file types must be repaired.
            if ("uniprot".equalsIgnoreCase(form.getFileType()))
            {
                loader = new XMLProteinLoader(fname);
            }
            else if ("fasta".equalsIgnoreCase(form.getFileType()))
            {
                FastaDbLoader fdbl = new FastaDbLoader(new File(fname));
                fdbl.setDefaultOrganism(form.getDefaultOrganism());
                fdbl.setOrganismIsToGuessed(form.getShouldGuess() != null);
                loader = fdbl;
            }
            else
            {
                throw new IllegalArgumentException("Unknown annotation file type: " + form.getFileType());
            }

            loader.setComment(comment);

            try
            {
                loader.validate();
            }
            catch (IOException e)
            {
                PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionMessage(e.getMessage()));
                return insertAnnots(form);
            }

            loader.parseInBackground();
        }

        return new ViewForward(getShowProteinAdminUrl());
    }

    @Jpf.Action
    protected Forward insertAnnots(LoadAnnotForm form) throws Exception
    {
        requiresGlobalAdmin();
        HttpView v = new JspView<LoadAnnotForm>("/org/labkey/ms2/insertAnnots.jsp", form);
        return _renderInTemplate(v, true, "Load Protein Annotations", null);
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

        MS2Run run = MS2Manager.getRun(form.run);

        ViewURLHelper queryUrl = cloneViewURLHelper();
        ViewContext ctx = getViewContext();
        String queryString = (String) ctx.get("queryString");
        queryUrl.setRawQuery(queryString);

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        Map<String, SimpleFilter> filters = peptideView.getFilter(queryUrl, run);
        String peptideFilterInfo = "";
        String proteinFilterInfo = "";
        String proteinGroupFilterInfo = "";
        if (filters != null)
        {
            if (filters.containsKey("peptideFilter"))
                peptideFilterInfo = "Peptide Filter: " + filters.get("peptideFilter").getFilterText();
            if (filters.containsKey("proteinFilter"))
                proteinFilterInfo = "Protein Filter: " + filters.get("proteinFilter").getFilterText();
            if (filters.containsKey("proteinGroupFilter"))
                proteinFilterInfo = "Protein Group Filter: " + filters.get("proteinGroupFilter").getFilterText();
        }

        String chartTitle = "GO " + goChartType + " Classifications";
        SQLFragment fragment = peptideView.getProteins(queryUrl, run, form);
        PieJChartHelper pjch = PieJChartHelper.prepareGOPie(chartTitle, fragment, goChartType);
        pjch.renderAsPNG(new NullOutputStream());

        GoChartBean bean = new GoChartBean();
        bean.run = run;
        bean.chartTitle = chartTitle;
        bean.goChartType = goChartType;
        bean.peptideFilterInfo = peptideFilterInfo;
        bean.proteinFilterInfo = proteinFilterInfo;
        bean.proteinGroupFilterInfo = proteinGroupFilterInfo;
        bean.imageMap = ImageMapUtilities.getImageMap("pie1", pjch.getChartRenderingInfo());
        bean.queryString = queryString;
        bean.grouping = form.getGrouping();
        bean.pieHelperObjName = "piechart-" + (new Random().nextInt(1000000000));
        bean.chartUrl = new ViewURLHelper("ms2", "doOnePeptideChart", getContainer()).addParameter("ctype", goChartType.toString()).addParameter("helpername", bean.pieHelperObjName);

        Cache.getShared().put(bean.pieHelperObjName, pjch, Cache.HOUR * 2);

        HttpView v = new JspView<GoChartBean>("/org/labkey/ms2/peptideChart.jsp", bean);

        return _renderInTemplate(v, false, "GO " + goChartType + " Chart", null);
    }


    public static class GoChartBean
    {
        public MS2Run run;
        public ProteinDictionaryHelpers.GoTypes goChartType;
        public String chartTitle;
        public String peptideFilterInfo = "";
        public String proteinFilterInfo = "";
        public String proteinGroupFilterInfo = "";
        public String pieHelperObjName;
        public ViewURLHelper chartUrl;
        public String imageMap;
        public String queryString;
        public String grouping;
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
        VBox vbox = new VBox();
        HttpServletRequest req = getRequest();

        String accn = req.getParameter("sliceTitle").split(" ")[0];
        String sliceDefinition = ProteinDictionaryHelpers.getGODefinitionFromAcc(accn);
        if (StringUtils.isBlank(sliceDefinition))
            sliceDefinition = "Miscellaneous or Defunct Category";
        String html = "<font size=\"+1\">" + PageFlowUtil.filter(sliceDefinition) + "</font>";
        HttpView definitionView = new HtmlView("Definition", html);
        vbox.addView(definitionView);

        String sqids = req.getParameter("sqids");
        String sqidArr[] = sqids.split(",");
        for (String curSqid : sqidArr)
        {
            int curSeqId = Integer.parseInt(curSqid);
            String curTitle = ProteinManager.getSeqParamFromId("BestName", curSeqId);
            vbox.addView(annotView(curTitle, curSeqId));
        }
        return _renderInTemplate(vbox, false, "Pieslice Details for: " + req.getParameter("sliceTitle"), null);
    }


    private static ActionButton NewAnnot = new ActionButton("insertAnnots.post", "Load New Annot File");
    private static ActionButton ReloadSprotOrgMap = new ActionButton("reloadSPOM.post", "Reload SWP Org Map");
    private static ActionButton ReloadGO = new ActionButton("showLoadGo.view", "Load or Reload GO");
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

        AnnotationLoader annotLoader = AnnotationUploadManager.getInstance().getActiveLoader(threadId);

        if (annotLoader != null)
        {
            if (commandType.equalsIgnoreCase("kill"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.KILLED);
            }

            if (commandType.equalsIgnoreCase("pause"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.PAUSED);
            }

            if (commandType.equalsIgnoreCase("continue"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.RUNNING);
            }
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

        return new ViewForward(getShowProteinAdminUrl());
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

        ActionButton delete = new ActionButton("", "Delete Selected");
        delete.setScript("alert(\"Note: this will not delete actual annotations,\\njust the entries on this list.\"); return verifySelected(this.form, \"deleteAnnotInsertEntries.post\", \"post\", \"annotations\")");
        delete.setActionType(ActionButton.Action.GET);
        bb.add(delete);

        bb.add(NewAnnot);
        bb.add(ReloadSprotOrgMap);
        ReloadGO.setActionType(ActionButton.Action.LINK);
        bb.add(ReloadGO);

        rgn.setButtonBar(bb);
        return rgn;
    }


    @Jpf.Action
    protected Forward reloadSPOM() throws Exception
    {
        requiresGlobalAdmin();

        ProteinDictionaryHelpers.loadProtSprotOrgMap();

        return new ViewForward(getShowProteinAdminUrl());
    }


    @Jpf.Action
    protected Forward showLoadGo() throws Exception
    {
        requiresGlobalAdmin();

        JspView view = new JspView("/org/labkey/ms2/loadGo.jsp");

        return _renderInTemplate(view, Template.home, "Load GO Annotations", null, false);
    }


    public static ViewURLHelper getLoadGoUrl()
    {
        return new ViewURLHelper("MS2", "loadGo", "");
    }


    @Jpf.Action
    protected Forward loadGo() throws Exception
    {
        requiresGlobalAdmin();

        GoLoader loader = GoLoader.getGoLoader();
        ViewURLHelper forwardUrl = new ViewURLHelper("MS2", "showGoStatus.view", "");

        if (null != loader)
        {
            loader.load();
            Thread.sleep(2000);
        }
        else
        {
            forwardUrl.addParameter("message", "<b>Can't load GO annotations, a GO annotation load is already in progress.  See below for details.</b><br>");
        }

        return new ViewForward(forwardUrl);
    }


    @Jpf.Action
    protected Forward showGoStatus() throws Exception
    {
        requiresGlobalAdmin();

        HttpView view = GoLoader.getCurrentStatus(getViewURLHelper().getParameter("message"));
        return _renderInTemplate(view, Template.home, "GO Load Status", null, false);
    }


    @Jpf.Action
    protected Forward deleteAnnotInsertEntries() throws Exception
    {
        requiresGlobalAdmin();

        String[] deleteAIEs = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        String idList = StringUtils.join(deleteAIEs, ',');
        Table.execute(ProteinManager.getSchema(), "DELETE FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId in (" + idList + ")", null);

        return new ViewForward(getShowProteinAdminUrl());
    }


    @Jpf.Action
    protected Forward showAnnotInserts() throws Exception
    {
        Container c = ContainerManager.getForPath("home");
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            HttpView.throwUnauthorized();

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


    HttpView fillInInsertDetails(int insertId) throws Exception
    {
        HttpView view = null;
        ResultSet rs = null;
        Map<String, String> map = new HashMap<String, String>();

        try
        {
            rs = Table.executeQuery(ProteinManager.getSchema(), "SELECT * FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE insertId=" + insertId, null);
            rs.next();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            for (int i = 1; i <= colCount; i++)
            {
                String name = rsmd.getColumnName(i).toLowerCase();
                String val = rs.getString(name);
                map.put(name, val);
            }
            view = new JspView<Map<String, String>>("/org/labkey/ms2/annotLoadDetails.jsp", map);            
            rs.close();
        }
        finally
        {
            ResultSetUtil.close(rs);
        }
        return view;
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
        HttpView vv = fillInInsertDetails(insertId);
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

            return new ViewForward(new ViewURLHelper("MS2", "showList", c.getPath()));
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

                // TODO: Clean this up.
                boolean mascotFile = MascotSearchProtocolFactory.get().equals(AbstractMS2SearchProtocolFactory.fromFile(f));
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

                if (run == -1)
                    return HttpView.throwNotFound();

                url = new ViewURLHelper("MS2", "showList", c.getPath());
                url.addParameter(MS2Manager.getDataRegionNameExperimentRuns() + ".Run~eq", Integer.toString(run));
            }
            else if (!AppProps.getInstance().hasPipelineCluster())
            {
                url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                url.addParameter("error", "Automated upload disabled.");
            }
            else
            {
                if (!form.isExperiment())
                {
                    PipelineService service = PipelineService.get();
                    ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);

                    int run = MS2Manager.addRunToQueue(info,
                            f, form.getDescription(), true).getRunId();
                    if (run == -1)
                        HttpView.throwNotFound();

                    url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                    url.addParameter("run", Integer.toString(run));
                }
                else
                {
                    // Make sure container exists.
                    c = ContainerManager.ensureContainer(getViewURLHelper().getExtraPath());
                    if (null == c)
                        return HttpView.throwNotFound();

                    PipelineService service = PipelineService.get();
                    PipeRoot pr = service.findPipelineRoot(c);
                    if (pr == null)
                        return HttpView.throwUnauthorized();

                    ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);
                    String protocolName = form.getProtocol();
                    File dirData = new File(form.getDataDir());
                    if (!NetworkDrive.exists(dirData))
                        return HttpView.throwNotFound();

                    AbstractMS2SearchProtocolFactory protocolFactory = AbstractMS2SearchProtocolFactory.fromFile(f);

                    File dirSeqRoot = new File(MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer()));
                    File dirAnalysis = protocolFactory.getAnalysisDir(dirData, protocolName);
                    File fileParameters = protocolFactory.getParametersFile(dirData, protocolName);
                    String baseName = FileUtil.getBaseName(f, 2);

                    File[] filesMzXML;
                    if (!"all".equals(baseName))
                    {
                        filesMzXML = new File[] { MS2PipelineManager.getMzXMLFile(dirData, baseName) };
                    }
                    else
                    {
                        // Anything that is running or complete.
                        Map<File, FileStatus> mzXMLFileStatus = MS2PipelineManager.getAnalysisFileStatus(dirData, dirAnalysis, info.getContainer());
                        List<File> fileList = new ArrayList<File>();
                        for (File fileMzXML : mzXMLFileStatus.keySet())
                        {
                            FileStatus status = mzXMLFileStatus.get(fileMzXML);
                            if (status.equals(FileStatus.COMPLETE) || status.equals(FileStatus.RUNNING))
                                fileList.add(fileMzXML);
                        }
                        filesMzXML = fileList.toArray(new File[fileList.size()]);

                        if (filesMzXML.length == 0)
                            return HttpView.throwNotFound();
                    }

                    MS2SearchPipelineProtocol protocol = protocolFactory.loadInstance(fileParameters);

                    PipelineJob job = protocol.createPipelineJob(info,
                            dirSeqRoot,
                            filesMzXML,
                            fileParameters,
                            true);

                    PipelineService.get().queueJob(job);

                    url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                    url.addParameter("path", job.getLogFile().getAbsolutePath());
                }
            }
        }
        else
        {
            url = new ViewURLHelper("MS2", "addFileRunStatus", "");
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
        else if (getViewURLHelper().getParameter("error") != null)
        {
            status = "ERROR->message=" + getViewURLHelper().getParameter("error");            
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
            MS2Manager.markAsDeleted(deleteRuns, c, getUser());
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
        final Container originalContainer = getContainer();
        ContainerTree ct = new ContainerTree("/", getUser(), ACL.PERM_INSERT, currentUrl)
        {
            protected void renderCellContents(StringBuilder html, Container c, ViewURLHelper url)
            {
                boolean hasRoot = false;
                try
                {
                    hasRoot = PipelineService.get().findPipelineRoot(c) != null;
                }
                catch (SQLException e)
                {
                    _log.error("Unable to determine pipeline root", e);
                }

                if (hasRoot && !c.equals(originalContainer))
                {
                    super.renderCellContents(html, c, url);
                }
                else
                {
                    html.append(PageFlowUtil.filter(c.getName()));
                }
            }
        };

        StringBuilder html = new StringBuilder("<table class=\"dataRegion\"><tr><td>Please select the destination folder. Folders that are not configured with a pipeline root are not valid destinations. They are shown in the list, but are not linked.</td></tr><tr><td>&nbsp;</td></tr>");
        ct.render(html);
        html.append("</table>");

        return _renderInTemplate(new HtmlView(html.toString()), false, "Move Runs", "ms2RunsList",
                new NavTree("MS2 Runs", new ViewURLHelper("MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    @Jpf.Action
    protected Forward moveRuns() throws URISyntaxException, ServletException, SQLException, IOException
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
        List<MS2Run> runs = getRuns(ids, new ArrayList<String>(), false);
        List<ExpRun> expRuns = new ArrayList<ExpRun>();
        Container sourceContainer = null;
        for (Iterator<MS2Run> iter = runs.iterator(); iter.hasNext(); )
        {
            MS2Run run = iter.next();
            if (run.getExperimentRunLSID() != null)
            {
                ExpRun expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
                if (expRun != null && expRun.getContainer().getId().equals(run.getContainer()))
                {
                    sourceContainer = expRun.getContainer();
                    expRuns.add(expRun);
                    iter.remove();
                }
            }
        }
        if (runs.size() > 0)
        {
            MS2Manager.moveRuns(getUser(), runs, getContainer());
        }
        if (expRuns.size() > 0)
        {
            ViewBackgroundInfo info = getViewBackgroundInfo();
            info.setContainer(getContainer());
            try
            {
                ExperimentService.get().moveRuns(info, sourceContainer, expRuns);
            }
            catch (FileNotFoundException e)
            {
                HttpView.throwNotFound(e.getMessage());
            }
        }

        currentUrl.setAction("showList");
        currentUrl.deleteParameter("moveRuns");
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward showHierarchy() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        MS2RunHierarchyTree ht = new MS2RunHierarchyTree(currentUrl.getExtraPath(), getUser(), ACL.PERM_READ, currentUrl);

        StringBuilder html = new StringBuilder();
        html.append("<script type=\"text/javascript\">\n");
        html.append("LABKEY.requiresScript('filter.js');\n");
        html.append("</script>");
        html.append("<form method=post action=''>");

        html.append("<table class=\"dataRegion\" cellspacing=\"0\" cellpadding=\"1\">");
        ht.render(html);
        html.append("</table>");

        renderHierarchyButtonBar(html);
        html.append("</form>");

        return _renderInTemplate(new HtmlView(html.toString()), false, "Hierarchy", "ms2RunsList",
                new NavTree("MS2 Runs", new ViewURLHelper("MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    private void renderHierarchyButtonBar(StringBuilder html) throws IOException
    {
        ButtonBar bb = new ButtonBar();

        bb.add(ActionButton.BUTTON_SELECT_ALL);
        bb.add(ActionButton.BUTTON_CLEAR_ALL);

        ActionButton compareRuns = new ActionButton("button", "Compare");
        compareRuns.setScript("return verifySelected(this.form, \"compare.view\", \"post\", \"runs\")");
        compareRuns.setActionType(ActionButton.Action.GET);
        compareRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareRuns);

        ActionButton exportRuns = new ActionButton("button", "MS2 Export");
        exportRuns.setScript("return verifySelected(this.form, \"pickExportRunsView.view\", \"post\", \"runs\")");
        exportRuns.setActionType(ActionButton.Action.GET);
        exportRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(exportRuns);

        StringWriter s = new StringWriter();

        bb.render(new RenderContext(getViewContext()), s);
        html.append(s);
    }


    @Jpf.Action
    protected Forward editElutionGraph(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
        Quantitation quant = peptide.getQuantitation();

        EditElutionGraphContext ctx = new EditElutionGraphContext(quant.getLightElutionProfile(peptide.getCharge()), quant.getHeavyElutionProfile(peptide.getCharge()), quant, getViewURLHelper(), peptide);
        JspView v = new JspView<EditElutionGraphContext>("/org/labkey/ms2/editElution.jsp", ctx);
        includeView(v);
        return null;
    }

    @Jpf.Action
    protected Forward saveElutionProfile(ElutionProfileForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
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

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

        if (null != peptide)
        {
            if (!isAuthorized(peptide.getRun()))
                return null;

            HttpServletResponse response = getResponse();
            response.setDateHeader("Expires", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
            response.setHeader("Pragma", "");
            response.setContentType("image/png");
            peptide.renderGraph(response, form.getTolerance(), form.getxStartDouble(), form.getxEnd(), form.getWidth(), form.getHeight());
        }

        return null;
    }

    protected Forward showElutionGraph(DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

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
                if (charge < 1 || charge > 3)
                {
                    return renderErrorImage("Invalid charge state: " + charge, response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
                }
                if (showLight)
                {
                    g.addInfo(quantitation.getLightElutionProfile(charge), quantitation.getLightFirstScan(), quantitation.getLightLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.RED);
                }
                if (showHeavy)
                {
                    g.addInfo(quantitation.getHeavyElutionProfile(charge), quantitation.getHeavyFirstScan(), quantitation.getHeavyLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.BLUE);
                }
                if (quantitation.isNoScansFound())
                {
                    return renderErrorImage("No relevant MS1 scans found in spectra file", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
                }
                else
                {
                    g.render(response.getOutputStream());
                    return null;
                }
            }
            else
            {
                return renderErrorImage("Could not open spectra file to get MS1 scans", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
            }
        }
        else
        {
            return HttpView.throwNotFound("Could not find peptide with id " + form.getPeptideIdLong());
        }
    }

    private Forward renderErrorImage(String errorMessage, HttpServletResponse response, int width, int height)
            throws IOException
    {
        Graph g = new Graph(new float[0], new float[0], width, height)
        {
            protected void initializeDataPoints(Graphics2D g) {}
            protected void renderDataPoint(Graphics2D g, double x, double y) {}
        };
        g.setNoDataErrorMessage(errorMessage);
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

        long peptideId = form.getPeptideIdLong();
        MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

        if (peptide == null)
        {
            return HttpView.throwNotFound("Could not find peptide with RowId " + peptideId);
        }

        int runId = peptide.getRun();

        if (!isAuthorized(runId))
            return HttpView.throwUnauthorized();

        ViewURLHelper currentUrl = getViewURLHelper();

        int sqlRowIndex = form.getRowIndex();
        int rowIndex = sqlRowIndex - 1;  // Switch 1-based, JDBC row index to 0-based row index for array lookup

        MS2Run run = MS2Manager.getRun(runId);
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
        JspView v = new JspView<ShowPeptideContext>("/org/labkey/ms2/showPeptide.jsp", ctx);
        v.setFrame(WebPartView.FrameType.NONE);
        includeView(v);

        return null;
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

        JspView view = new JspView("/org/labkey/ms2/showPeptideProphetDetails.jsp");
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

        ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);

        JspView view = new JspView("/org/labkey/ms2/showSensitivityDetails.jsp");
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

        ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);

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

        GroovyView view = new GroovyView("/org/labkey/ms2/modifications.gm");
        view.setFrame(WebPartView.FrameType.NONE);
        view.addObject("pageTitle", "Modifications");
        view.addObject("fixed", fixed);
        view.addObject("var", var);

        return includeView(view);
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

        return pickView(cloneViewURLHelper().setAction("applyExportRunsView"), "Select a view to apply a filter to all the runs and to indicate what columns to export.", extraFormHtml, "Export Runs", "ms2RunsList", true);
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
        List<MS2Run> runs = getCachedRuns(form.getRunList(), errors, true);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), runs.toArray(new MS2Run[runs.size()]));
        ViewURLHelper currentUrl = cloneViewURLHelper();
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

        if (form.getExportFormat() != null && form.getExportFormat().startsWith("Excel"))
        {
            peptideView.exportToExcel(form, getResponse(), null);
        }

        if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, getResponse(), null, null);
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
            peptideView.exportToAMT(form, getResponse(), null);
        }

        return null;
    }

    @Jpf.Action
    protected Forward exportAllProteins(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (isAuthorized(form.run))
            exportProteins(form, null, null);

        return null;
    }


    @Jpf.Action
    protected Forward exportSelectedProteins(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

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

            exportProteins(form, where.toString(), proteins);
        }

        return null;
    }


    private void exportProteins(ExportForm form, String extraWhere, List<String> proteins) throws Exception
    {
        MS2Run run = MS2Manager.getRun(form.getRun());
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        if ("Excel".equals(form.getExportFormat()))
        {
            peptideView.exportToExcel(form, getResponse(), proteins);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, getResponse(), proteins, null);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, getResponse(), proteins);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewURLHelper(), form.getExportFormat().toLowerCase(), peptideView, extraWhere);
        }
    }


    @Jpf.Action
    protected Forward exportSelectedProteinGroups(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        ViewContext ctx = getViewContext();
        List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

        exportProteinGroups(form, proteins);

        return null;
    }

    @Jpf.Action
    protected Forward exportProteinGroups(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (isAuthorized(form.run))
            exportProteinGroups(form, null);

        return null;
    }


    private void exportProteinGroups(ExportForm form, List<String> proteins) throws Exception
    {
        MS2Run run = MS2Manager.getRun(form.getRun());
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        String where = null;
        if (proteins != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
            sb.append(".RowId IN (");
            String separator = "";
            for (String protein : proteins)
            {
                sb.append(separator);
                separator = ", ";
                sb.append(new Long(protein));
            }
            sb.append(")");
            where = sb.toString();
        }

        if ("Excel".equals(form.getExportFormat()))
        {
            peptideView.exportToExcel(form, getResponse(), proteins);
//            exportProteinGroupsToExcel(getViewURLHelper(), form, what, where);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, getResponse(), proteins, null);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, getResponse(), proteins);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewURLHelper(), form.getExportFormat().toLowerCase(), peptideView, where);
        }
    }

    @Jpf.Action
    protected Forward exportProteinSearchToExcel(final ProteinSearchForm form) throws Exception
    {
        QueryView view = createProteinSearchView(form);
        ExcelWriter excelWriter = view.getExcelWriter();
        excelWriter.setFilenamePrefix("ProteinSearchResults");
        excelWriter.write(getResponse());
        return null;
    }

    @Jpf.Action
    protected Forward exportProteinSearchToTSV(final ProteinSearchForm form) throws Exception
    {
        QueryView view = createProteinSearchView(form);
        TSVGridWriter tsvWriter = view.getTsvWriter();
        tsvWriter.setFilenamePrefix("ProteinSearchResults");
        tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
        tsvWriter.write(getResponse());
        return null;
    }

    @Jpf.Action
    protected Forward exportQueryProteinProphetCompareToExcel(final ExportForm form) throws Exception
    {
        return exportQueryCompareToExcel(new CompareProteinsView(getViewContext(), this, form.getRunList(), true));
    }

    @Jpf.Action
    protected Forward exportQueryProteinProphetCompareToTSV(final ExportForm form) throws Exception
    {
        return exportQueryCompareToTSV(new CompareProteinsView(getViewContext(), this, form.getRunList(), true), form.isExportAsWebPage());
    }

    @Jpf.Action
    protected Forward exportQueryPeptideCompareToExcel(final ExportForm form) throws Exception
    {
        return exportQueryCompareToExcel(new ComparePeptidesView(getViewContext(), this, form.getRunList(), true));
    }

    @Jpf.Action
    protected Forward exportQueryPeptideCompareToTSV(final ExportForm form) throws Exception
    {
        return exportQueryCompareToTSV(new ComparePeptidesView(getViewContext(), this, form.getRunList(), true), form.isExportAsWebPage());
    }

    private Forward exportQueryCompareToExcel(AbstractRunCompareView view) throws Exception
    {
        if (!view.getErrors().isEmpty())
            return _renderErrors(view.getErrors());

        ExcelWriter excelWriter = view.getExcelWriter();
        excelWriter.setFilenamePrefix("CompareRuns");
        excelWriter.write(getResponse());
        return null;
    }

    private Forward exportQueryCompareToTSV(AbstractRunCompareView view, boolean exportAsWebPage) throws Exception
    {
        if (!view.getErrors().isEmpty())
            return _renderErrors(view.getErrors());

        TSVGridWriter tsvWriter = view.getTsvWriter();
        tsvWriter.setExportAsWebPage(exportAsWebPage);
        tsvWriter.setFilenamePrefix("CompareRuns");
        tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
        tsvWriter.write(getResponse());
        return null;
    }

    @Jpf.Action
    protected Forward exportProteinGroupSearchToExcel(final ProteinSearchForm form) throws Exception
    {
        QueryView view = createProteinGroupSearchView(form);
        ExcelWriter excelWriter = view.getExcelWriter();
        excelWriter.setFilenamePrefix("ProteinGroupSearchResults");
        excelWriter.write(getResponse());
        return null;
    }

    @Jpf.Action
    protected Forward exportProteinGroupSearchToTSV(final ProteinSearchForm form) throws Exception
    {
        QueryView view = createProteinGroupSearchView(form);
        TSVGridWriter tsvWriter = view.getTsvWriter();
        tsvWriter.setFilenamePrefix("ProteinGroupSearchResults");
        tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
        tsvWriter.write(getResponse());
        return null;
    }

    @Jpf.Action
    protected Forward exportAllPeptides(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        return exportPeptides(form, false);
    }


    @Jpf.Action
    protected Forward exportSelectedPeptides(ExportForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        return exportPeptides(form, true);
    }


    private Forward exportPeptides(ExportForm form, boolean selected) throws Exception
    {
        if (!isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        ViewURLHelper currentUrl = getViewURLHelper();
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        // Need to create a filter for 1) extra filter and 2) selected peptides
        // URL filter is applied automatically (except for DTA/PKL)
        SimpleFilter baseFilter = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.EXTRA_FILTER, run);

        List<String> exportRows = null;
        if (selected)
        {
            exportRows = getViewContext().getList(DataRegion.SELECT_CHECKBOX_NAME);
            if (exportRows == null)
            {
                exportRows = new ArrayList<String>();
            }

            List<Long> peptideIds = new ArrayList<Long>(exportRows.size());

            // Technically, should only limit this in Excel export case... but there's no way to individually select 65K peptides
            for (int i = 0; i < Math.min(exportRows.size(), ExcelWriter.MAX_ROWS); i++)
            {
                String[] row = exportRows.get(i).split(",");
                peptideIds.add(Long.parseLong(row[row.length == 1 ? 0 : 1]));
            }

            baseFilter.addInClause("RowId", peptideIds);
        }

        if ("Excel".equals(form.exportFormat))
        {
            peptideView.exportToExcel(form, getResponse(), exportRows);
            return null;
        }

        if ("TSV".equals(form.exportFormat))
        {
            peptideView.exportToTSV(form, getResponse(), exportRows, null);
            return null;
        }

        if ("AMT".equals(form.exportFormat))
        {
            peptideView.exportToAMT(form, getResponse(), exportRows);
            return null;
        }

        // Add URL filter manually
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER, run));

        if ("DTA".equals(form.exportFormat) || "PKL".equals(form.exportFormat))
            return exportSpectra(Arrays.asList(run), currentUrl, baseFilter, form.exportFormat.toLowerCase());

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


    private Forward exportProteinsAsSpectra(List<MS2Run> runs, ViewURLHelper currentUrl, String extension, AbstractMS2RunView peptideView, String where) throws IOException
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
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

        if (null == peptide)
        {
            // This should only happen if an old, cached link is being used... a saved favorite or google bot with fraction=x&scan=y&charge=z instead of peptideId
            // Log an error just to make sure.
            _log.error("Couldn't find peptide " + form.getPeptideIdLong() + ". " + getViewURLHelper().toString());
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

        MS2Run run = MS2Manager.getRun(getRequest().getParameter("run"));
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

        if (getRequest().getParameter("grouping") != null)
        {
            url.addParameter("grouping", getRequest().getParameter("grouping"));
        }

        if (getRequest().getParameter("expanded") != null)
        {
            url.addParameter("expanded", "1");
        }

        return new ViewForward(url);
    }


    // Parse parameter to float, returning 0 for any parsing exceptions
    private void parseChargeScore(ViewContext ctx, ViewURLHelper url, String digit, String paramName)
    {
        float value = 0;
        String score = (String)ctx.get("charge" + digit);

        try
        {
            if (score != null)
            {
                value = Float.parseFloat(score);
            }
        }
        catch(NumberFormatException e)
        {
            // Can't parse... just use default
        }

        if (0.0 != value)
            url.addParameter(paramName + digit, Formats.chargeFilter.format(value));
    }


    // extraFormHtml gets inserted between the view dropdown and the button.
    private Forward pickView(ViewURLHelper nextUrl, String viewInstructions, String extraFormHtml, String navTreeName, String helpTopic, boolean requireSameType) throws Exception
    {
        List<String> errors = new ArrayList<String>();
        int runListIndex = cacheSelectedRuns(errors, requireSameType);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        JspView<PickViewBean> pickView = new JspView<PickViewBean>("/org/labkey/ms2/pickView.jsp", new PickViewBean());

        PickViewBean bean = pickView.getModelBean();

        nextUrl.deleteFilterParameters("button");
        nextUrl.deleteFilterParameters("button.x");
        nextUrl.deleteFilterParameters("button.y");

        bean.nextUrl = nextUrl;
        bean.select = renderViewSelect(0, true, ACL.PERM_READ, false);
        bean.extraHtml = extraFormHtml;
        bean.viewInstructions = viewInstructions;
        bean.runList = runListIndex;

        return _renderInTemplate(pickView, false, navTreeName, helpTopic,
                new NavTree("MS2 Runs", new ViewURLHelper("MS2", "showList", getViewURLHelper().getExtraPath())));
    }


    public static class PickViewBean
    {
        public ViewURLHelper nextUrl;
        public StringBuilder select;
        public String extraHtml;
        public String viewInstructions;
        public int runList;
    }


    @Jpf.Action
    protected Forward compare() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        StringBuilder sb = new StringBuilder();

        sb.append("<tr><td>");
        sb.append("<p>Choose a way to compare the runs:</p>");
        sb.append("<input type=\"radio\" name=\"column\" value=\"ProteinProphet\" checked /><b>Protein Prophet</b><br/>");
        sb.append("<div style=\"padding-left: 20px;\">Choose what columns should appear in the grid:<br/>\n");
        sb.append("<div style=\"padding-left: 20px;\"><input type=\"checkbox\" name=\"proteinGroup\" value=\"1\" checked=\"checked\" disabled>Protein Group<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"groupProbability\" value=\"1\" checked=\"checked\">Group Probability<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"light2HeavyRatioMean\" value=\"1\">Light to Heavy Quantitation<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"heavy2LightRatioMean\" value=\"1\">Heavy to Light Quantitation<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"totalPeptides\" value=\"1\">Total Peptides<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"uniquePeptides\" value=\"1\">Unique Peptides<br/>\n");
        sb.append("</div></div><br/>");

        sb.append("<input type=\"radio\" name=\"column\" value=\"Protein\" /><b>Search Engine Protein Assignment</b><br/>");
        sb.append("<div style=\"padding-left: 20px;\">Choose what columns should appear in the grid:<br/>\n");
        sb.append("<div style=\"padding-left: 20px;\"><input type=\"checkbox\" name=\"unique\" value=\"1\" checked=\"checked\">Unique Peptides<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"total\" value=\"1\">Total Peptides<br/>\n");
//        sb.append("<input type=\"checkbox\" name=\"sumLightArea-Protein\" value=\"1\">Total light area (quantitation)<br/>\n");
//        sb.append("<input type=\"checkbox\" name=\"sumHeavyArea-Protein\" value=\"1\">Total heavy area (quantitation)<br/>\n");
//        sb.append("<input type=\"checkbox\" name=\"avgDecimalRatio-Protein\" value=\"1\">Average decimal ratio (quantitation)<br/>\n");
//        sb.append("<input type=\"checkbox\" name=\"maxDecimalRatio-Protein\" value=\"1\">Maximum decimal ratio (quantitation)<br/>\n");
//        sb.append("<input type=\"checkbox\" name=\"minDecimalRatio-Protein\" value=\"1\">Minimum decimal ratio (quantitation)<br/>\n");
        sb.append("</div></div><br/>");

        sb.append("<input type=\"radio\" name=\"column\" value=\"Peptide\" /><b>Peptide</b><br/>");
        sb.append("<div style=\"padding-left: 20px;\">Choose what columns should appear in the grid:<br/>\n");
        sb.append("<div style=\"padding-left: 20px;\"><input type=\"checkbox\" name=\"peptideCount\" value=\"1\" checked=\"checked\" disabled>Count<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"maxPeptideProphet\" value=\"1\" checked=\"checked\">Maximum Peptide Prophet Probability<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"avgPeptideProphet\" value=\"1\" checked=\"checked\">Average Peptide Prophet Probability<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"minPeptideProphetErrorRate\" value=\"1\">Minimum Peptide Prophet Error Rate<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"avgPeptideProphetErrorRate\" value=\"1\">Average Peptide Prophet Error Rate<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"sumLightArea-Peptide\" value=\"1\">Total light area (quantitation)<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"sumHeavyArea-Peptide\" value=\"1\">Total heavy area (quantitation)<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"avgDecimalRatio-Peptide\" value=\"1\">Average decimal ratio (quantitation)<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"maxDecimalRatio-Peptide\" value=\"1\">Maximum decimal ratio (quantitation)<br/>\n");
        sb.append("<input type=\"checkbox\" name=\"minDecimalRatio-Peptide\" value=\"1\">Minimum decimal ratio (quantitation)<br/>\n");
        sb.append("</div></div><br/>");
        sb.append("<hr>");
        sb.append("<input type=\"radio\" name=\"column\" value=\"Query\" /><b>Query (beta)</b><br/>");
        sb.append("<div style=\"padding-left: 20px;\">The query-based comparison does not use the view selected above. Instead, please follow the instructions at the top of the comparison page to customize the results. It is based on ProteinProphet protein groups, so the runs must be associated with ProteinProphet data.</div>");
        sb.append("<hr>");

//        sb.append("<input type=\"radio\" name=\"column\" value=\"QueryPeptides\" /><b>Query Peptides (beta)</b><br/>");
//        sb.append("<hr>");
        sb.append("</td></tr>\n");

        ViewURLHelper nextUrl = cloneViewURLHelper().setAction("applyCompareView");
        return pickView(nextUrl, "Select a view to apply a filter to all the runs.", sb.toString(), "Compare Runs", "ms2RunsList", false);
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

    @Jpf.Action
    protected Forward compareService() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        CompareServiceImpl service = new CompareServiceImpl(getViewContext(), this);
        service.doPost(getRequest(), getResponse());
        return null;
    }

    private Forward compareRuns(int runListIndex, boolean exportToExcel) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper currentUrl = getViewURLHelper();
        String column = currentUrl.getParameter("column");
        boolean isQueryProteinProphet = "query".equalsIgnoreCase(column);
        boolean isQueryPeptides = "querypeptides".equalsIgnoreCase(column);

        if (isQueryProteinProphet || isQueryPeptides)
        {
            AbstractRunCompareView view = isQueryPeptides ? new ComparePeptidesView(getViewContext(), this, runListIndex, false) : new CompareProteinsView(getViewContext(), this, runListIndex, false);

            if (!view.getErrors().isEmpty())
                return _renderErrors(view.getErrors());

            HtmlView helpView = new HtmlView("Comparison Details", "<div style=\"width: 800px;\"><p>To change the columns shown and set filters, use the Customize View link below. Add protein-specific columns, or expand <em>Run</em> to see the values associated with individual runs, like probability. To set a filter, select the Filter tab, add column, and filter it based on the desired threshold.</p></div>");

            Map<String, String> props = new HashMap<String, String>();
            props.put("originalURL", getViewURLHelper().toString());
            props.put("comparisonName", view.getComparisonName());
            GWTView gwtView = new GWTView("org.labkey.ms2.RunComparator", props);
            VBox vbox = new VBox(gwtView, helpView, view);

            return _renderInTemplate(vbox, false, "Compare Runs", null);
        }

        List<String> errors = new ArrayList<String>();
        List<MS2Run> runs = getCachedRuns(runListIndex, errors, false);

        if (!errors.isEmpty())
            return _renderErrors(errors);

        for (MS2Run run : runs)
        {
            Container c = ContainerManager.getForId(run.getContainer());
            if (c == null || !c.hasPermission(getUser(), ACL.PERM_READ))
            {
                return HttpView.throwUnauthorized();
            }
        }

        CompareQuery query = CompareQuery.getCompareQuery(column, currentUrl, runs);
        if (query == null)
            return _renderError("You must specify a comparison type");

        query.checkForErrors(errors);

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
            rgn.setColSpan(query.getColumnsPerRun());
            rgn.setMultiColumnCaptions(runCaptions);

            HttpView filterView = new CurrentFilterView(query);

            GridView compareView = new GridView(rgn);
            compareView.setResultSet(rgn.getResultSet());

            _renderInTemplate(new VBox(filterView, compareView), false, query.getComparisonDescription(), "ms2RunsList",
                    new NavTree("MS2 Runs", new ViewURLHelper("MS2", "showList", getViewURLHelper().getExtraPath())));
        }

        return null;
    }


    public static class CurrentFilterView extends JspView<CurrentFilterView.CurrentFilterBean>
    {
        private CurrentFilterView(String[] headers, List<Pair<String, String>> sqlSummaries)
        {
            super("/org/labkey/ms2/currentFilter.jsp", new CurrentFilterBean(headers, sqlSummaries));
        }

        private CurrentFilterView(CompareQuery query)
        {
            this(new String[]{query.getHeader()}, query.getSQLSummaries());
        }

        public static class CurrentFilterBean
        {
            public String[] headers;
            public List<Pair<String, String>> sqlSummaries;

            private CurrentFilterBean(String[] headers, List<Pair<String, String>> sqlSummaries)
            {
                this.headers = headers;
                this.sqlSummaries = sqlSummaries;
            }
        }
    }


    private List<MS2Run> getSelectedRuns(List<String> errors, boolean requireSameType) throws ServletException
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

        return getRuns(runIds, errors, requireSameType);
    }


    private List<MS2Run> getRuns(List<Integer> runIds, List<String> errors, boolean requireSameType) throws ServletException
    {
        List<MS2Run> runs = new ArrayList<MS2Run>(runIds.size());
        boolean experimentRunIds = "true".equals(getRequest().getParameter("ExperimentRunIds"));
        String type = null;

        for (Integer runId : runIds)
        {
            MS2Run run = null;
            if (experimentRunIds)
            {
                ExpRun expRun = ExperimentService.get().getExpRun(runId.intValue());
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
    private int cacheSelectedRuns(List<String> errors, boolean requireSameType) throws ServletException
    {
        List<MS2Run> runs = getSelectedRuns(errors, requireSameType);

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

    public List<MS2Run> getCachedRuns(int index, List<String> errors, boolean requireSameType) throws ServletException
    {
        List<Integer> runIds = _runListCache.get(index);

        if (null == runIds)
        {
            errors.add(NO_RUNS_MESSAGE);
            return null;
        }

        return getRuns(runIds, errors, requireSameType);
    }

    @Jpf.Action
    protected Forward showProteinGroup(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // May have a runId, a group number, and an indistinguishableGroupId, or might just have a
        // proteinGroupId
        if (form.getProteinGroupId() != null)
        {
            ProteinGroupWithQuantitation group = MS2Manager.getProteinGroup(form.getProteinGroupId());
            if (group != null)
            {
                ProteinProphetFile file = MS2Manager.getProteinProphetFile(group.getProteinProphetFileId());
                if (file != null)
                {
                    form.run = file.getRun();

                    if (!isAuthorized(form.run))
                        return null;

                    MS2Run run = MS2Manager.getRun(form.run);
                    Container c = ContainerManager.getForId(run.getContainer());
                    ViewURLHelper url = cloneViewURLHelper();
                    url.deleteParameter("proteinGroupId");
                    url.replaceParameter("run", Integer.toString(form.run));
                    url.replaceParameter("groupNumber", Integer.toString(group.getGroupNumber()));
                    url.replaceParameter("indistinguishableCollectionId", Integer.toString(group.getIndistinguishableCollectionId()));
                    url.setExtraPath(c.getPath());
                    return new ViewForward(url);
                }
            }
        }

        if (!isAuthorized(form.run))
            return null;

        MS2Run run = MS2Manager.getRun(form.run);

        ProteinProphetFile proteinProphet = run.getProteinProphetFile();
        if (proteinProphet == null)
        {
            return HttpView.throwNotFound();
        }
        ProteinGroupWithQuantitation group = proteinProphet.lookupGroup(form.getGroupNumber(), form.getIndistinguishableCollectionId());
        Protein[] proteins = group.lookupProteins();

        AbstractMS2RunView peptideView = new ProteinProphetPeptideView(getViewContext(), run);
        VBox view = getShowProteinsView(getViewURLHelper(), run, form, proteins, null, peptideView);

        JspView summaryView = new JspView("/org/labkey/ms2/showProteinGroup.jsp");
        summaryView.addObject("proteinGroup", group);
        VBox fullPage = new VBox(summaryView, view);

        return _renderInTemplate(fullPage, false, "Protein Group Details", "showProteinGroup",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(MS2Manager.getRun(form.run).getDescription(), getViewURLHelper().clone().setAction("showRun")));
    }

    @Jpf.Action
    protected Forward showProtein(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        int runId;
        int seqId;
        if (form.run != 0)
        {
            runId = form.run;
            seqId = form.seqId;
        }
        else if (form.peptideId != 0)
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.peptideId);
            if (peptide != null)
            {
                runId = peptide.getRun();
                seqId = peptide.getSeqId() == null ? 0 : peptide.getSeqId().intValue();
            }
            else
            {
                return HttpView.throwNotFound();
            }
        }
        else
        {
            seqId = form.seqId;
            runId = 0;
        }


        ViewURLHelper currentUrl = getViewURLHelper();

        if (0 == seqId)
            return _renderInTemplate(new HtmlView("No details are available for protein " + form.getProtein() + "; its sequence is not available"), false, "Protein sequence not found", null);

        Protein protein = ProteinManager.getProtein(seqId);

        HttpView proteinView;
        List<NavTree> navTrail = new ArrayList<NavTree>();
        Template template;

        if (runId != 0)
        {
            MS2Run run = MS2Manager.getRun(runId);
            if (!isAuthorized(runId))
            {
                return HttpView.throwUnauthorized();
            }

            AbstractMS2RunView peptideView = new StandardProteinPeptideView(getViewContext(), run);
            proteinView = getShowProteinsView(currentUrl, run, form, new Protein[] {protein}, null, peptideView);

            // Set the protein name used in this run's FASTA file; we want to include this in the view.
            protein.setLookupString(form.getProtein());

            ViewURLHelper runUrl = currentUrl.clone();
            runUrl.deleteParameter("seqId");

            navTrail.add(new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())));
            if (run != null)
            {
                navTrail.add(new NavTree(run.getDescription(), runUrl.setAction("showRun")));
            }
            template = Template.print;
        }
        else
        {
            proteinView = getShowProteinsView(currentUrl, null, form, new Protein[] {protein}, null, null);
            template = Template.home;
        }

        return _renderInTemplate(proteinView, template, getProteinTitle(protein, true), "showProtein", false,
                navTrail.toArray(new NavTree[navTrail.size()]));
    }


    @Jpf.Action
    protected Forward showAllProteins(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        long peptideId = form.getPeptideIdLong();

        if (peptideId == 0)
            return _renderError("No peptide specified");

        MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

        if (null == peptide)
            return _renderError("Could not locate peptide with this ID: " + peptideId);

        MS2Run run = MS2Manager.getRun(form.run);

        Protein[] proteins = ProteinManager.getProteinsContainingPeptide(run.getFastaId(), peptide);
        ViewURLHelper currentUrl = getViewURLHelper().clone();
        AbstractMS2RunView peptideView = new StandardProteinPeptideView(getViewContext(), run);
        HttpView proteinView = getShowProteinsView(currentUrl, run, form, proteins, new String[]{peptide.getTrimmedPeptide()}, peptideView);

        currentUrl.deleteParameter("peptideId");

        return _renderInTemplate(proteinView, false, "Proteins Containing " + peptide, "showProtein",
                new NavTree("MS2 Runs", new ViewURLHelper(getRequest(), "MS2", "showList", getViewURLHelper().getExtraPath())),
                new NavTree(MS2Manager.getRun(form.run).getDescription(), currentUrl.setAction("showRun")));
    }


    // TODO: Pass in Protein object
    protected HttpView annotView(String title, int seqId) throws Exception
    {
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
        allGbIds.addAll(Arrays.asList(GenBankIds));
        allGbIds.addAll(Arrays.asList(RefSeqIds));

        Set<String> allGbURLs = new HashSet<String>();

        for (Object allGbId : allGbIds)
        {
            String ident = (String) allGbId;
            String url = ProteinManager.makeFullAnchorString(
                    ProteinManager.makeAnyKnownIdentURLString(ident, 1),
                    "protWindow",
                    ident);
            allGbURLs.add(url);
        }

        // It is convenient to strip the version numbers from the IPI identifiers
        // and this may cause some duplications.  Use a hash-set to compress
        // duplicates
        Set<String> IPIset = new HashSet<String>();

        for (String idWithoutVersion : IPIds)
        {
            int dotIndex = idWithoutVersion.indexOf(".");
            if (dotIndex != -1) idWithoutVersion = idWithoutVersion.substring(0, dotIndex);
            IPIset.add(idWithoutVersion);
        }

        IPIds = new String[IPIset.size()];
        IPIset.toArray(IPIds);

        AnnotViewBean bean = new AnnotViewBean();

        /* info from db into view */
        bean.seqName = SeqName;
        bean.seqDesc = SeqDesc;
        if (GeneNames != null && GeneNames.length > 0)
            bean.geneName = StringUtils.join(ProteinManager.makeFullAnchorStringArray(GeneNames, "protWindow", "GeneName"), ", ");
        bean.seqOrgs = ProteinManager.getOrganismsFromId(seqId);
        bean.genBankUrls = allGbURLs;
        bean.swissProtNames = ProteinManager.makeFullAnchorStringArray(SwissProtNames, "protWindow", "SwissProt");
        bean.swissProtAccns = ProteinManager.makeFullAnchorStringArray(SwissProtAccns, "protWindow", "SwissProtAccn");
        bean.GIs = ProteinManager.makeFullAnchorStringArray(GIs, "protWindow", "GI");
        bean.ensemblIds = ProteinManager.makeFullAnchorStringArray(EnsemblIDs, "protWindow", "Ensembl");
        bean.goCategories = ProteinManager.makeFullGOAnchorStringArray(GOCategories, "protWindow");
        bean.IPI = ProteinManager.makeFullAnchorStringArray(IPIds, "protWindow", "IPI");

        JspView view = new JspView<AnnotViewBean>("/org/labkey/ms2/protAnnots.jsp", bean);
        if (title != null)
        {
            view.setTitle("Annotations for " + title);
        }

        return view;
    }


    public static class AnnotViewBean
    {
        public String seqName;
        public String seqDesc;
        public String geneName = null;
        public Set<String> seqOrgs;
        public Set<String> genBankUrls;
        public String[] swissProtNames;
        public String[] swissProtAccns;
        public String[] GIs;
        public String[] ensemblIds;
        public String[] goCategories;
        public String[] IPI;
    }


    private String getProteinTitle(Protein p, boolean includeBothNames)
    {
        if (null == p.getLookupString())
            return p.getBestName();

        if (!includeBothNames || p.getLookupString().equalsIgnoreCase(p.getBestName()))
            return p.getLookupString();

        return p.getLookupString() + " (" + p.getBestName() + ")";
    }


    private VBox getShowProteinsView(ViewURLHelper currentUrl, MS2Run run, DetailsForm form, Protein[] proteins, String[] peptides, AbstractMS2RunView peptideView) throws Exception
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
            vbox.addView(new HtmlView("<a name=\"Protein" + i + "\"/>"));

            ProteinViewBean bean = new ProteinViewBean();
            proteins[i].setPeptides(peptides);
            proteins[i].setShowEntireFragmentInCoverage(stringSearch);
            bean.protein = proteins[i];
            bean.showPeptides = showPeptides;
            JspView proteinSummary = new JspView<ProteinViewBean>("/org/labkey/ms2/protein.jsp", bean);
            proteinSummary.setTitle(getProteinTitle(proteins[i], true));
            vbox.addView(proteinSummary);

            // Add annotations
            vbox.addView(annotView(null, proteins[i].getSeqId()));
        }

        if (showPeptides)
        {
            List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
            String peptideFilterString = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run).getFilterText();
            sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilterString));
            sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText()));
            CurrentFilterView peptideFilter = new CurrentFilterView(null, sqlSummaries);
            peptideFilter.setTitle("Peptides");
            vbox.addView(peptideFilter);

            GridView peptidesGridView = peptideView.createPeptideViewForGrouping(form);
            peptidesGridView.getDataRegion().removeColumnsFromDisplayColumnList("Description,Protein,GeneName,SeqId");
            vbox.addView(peptidesGridView);
        }

        return vbox;
    }


    public static class ProteinViewBean
    {
        public Protein protein;
        public boolean showPeptides;
    }


    @Jpf.Action
    protected Forward showMS2Admin() throws Exception
    {
        requiresGlobalAdmin();

        MS2AdminBean bean = new MS2AdminBean();

        bean.days = getDays();
        bean.stats = MS2Manager.getStats(bean.days);
        bean.purgeStatus = MS2Manager.getPurgeStatus();
        bean.successfulUrl = showRunsUrl(false, 1);
        bean.inProcessUrl = showRunsUrl(false, 0);
        bean.failedUrl = showRunsUrl(false, 2);
        bean.deletedUrl = showRunsUrl(true, null);

        HttpView view = new JspView<MS2AdminBean>("/org/labkey/ms2/ms2Admin.jsp", bean);

        return _renderInTemplate(view, false, "MS2 Admin", null);
    }


    public static class MS2AdminBean
    {
        public ViewURLHelper successfulUrl;
        public ViewURLHelper inProcessUrl;
        public ViewURLHelper failedUrl;
        public ViewURLHelper deletedUrl;
        public Map<String, String> stats;
        public int days;
        public String purgeStatus;
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


    private ViewURLHelper showRunsUrl(Boolean deleted, Integer statusId)
    {
        ViewURLHelper url = new ViewURLHelper("MS2", "showAllRuns", (String)null);

        if (null != deleted)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".Deleted~eq", deleted.booleanValue() ? "1" : "0");

        if (null != statusId)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".StatusId~eq", String.valueOf(statusId));

        return url;
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
        gridView.getRenderContext().setUseContainerFilter(false);
        gridView.getViewContext().setPermissions(ACL.PERM_READ);
        SimpleFilter runFilter = new SimpleFilter();

        if (!getUser().isAdministrator())
        {
            runFilter.addInClause("Container", ContainerManager.getIds(getUser(), ACL.PERM_READ));
        }

        gridView.setFilter(runFilter);
        gridView.setTitle("Show All Runs");

        // TODO: Help topic
        return _renderInTemplate(gridView, true, "Show All Runs", null);
    }


    public static ViewURLHelper getShowProteinAdminUrl()
    {
        return new ViewURLHelper("MS2", "showProteinAdmin.view", "");
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

        return _renderInTemplate(new VBox(grid, annots), false, "Protein Database Admin", null);
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

        return new ViewForward(getShowProteinAdminUrl());
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

        return new ViewForward(getShowProteinAdminUrl());
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

    @Jpf.Action
    public Forward doProteinSearch(final ProteinSearchForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        SimpleFilter filter = new SimpleFilter();
        boolean addedFilter = false;
        if (form.getMaximumErrorRate() != null)
        {
            filter.addCondition("ErrorRate", form.getMaximumErrorRate(), CompareType.LTE);
            addedFilter = true;
        }
        if (form.getMinimumProbability() != null)
        {
            filter.addCondition("GroupProbability", form.getMinimumProbability(), CompareType.GTE);
            addedFilter = true;
        }
        
        if (addedFilter)
        {
            ViewURLHelper url = getViewURLHelper().clone();
            url.deleteParameter("minimumProbability");
            url.deleteParameter("maximumErrorRate");
            HttpView.throwRedirect(url + "&" + filter.toQueryString("ProteinSearchResults"));
        }

        QueryView proteinsView = createProteinSearchView(form);
        QueryView groupsView = createProteinGroupSearchView(form);

        ProteinSearchWebPart searchView = new ProteinSearchWebPart(true);
        searchView.getModelBean().setIdentifier(form.getIdentifier());
        searchView.getModelBean().setIncludeSubfolders(form.isIncludeSubfolders());
        searchView.getModelBean().setExactMatch(form.isExactMatch());
        if (getRequest().getParameter("ProteinSearchResults.GroupProbability~gte") != null)
        {
            try
            {
                searchView.getModelBean().setMinProbability(Float.parseFloat(getRequest().getParameter("ProteinSearchResults.GroupProbability~gte")));
            }
            catch (NumberFormatException e) {}
        }
        if (getRequest().getParameter("ProteinSearchResults.ErrorRate~lte") != null)
        {
            try
            {
                searchView.getModelBean().setMaxErrorRate(Float.parseFloat(getRequest().getParameter("ProteinSearchResults.ErrorRate~lte")));
            }
            catch (NumberFormatException e) {}
        }

        VBox vbox = new VBox(searchView, proteinsView, groupsView);

        return renderInTemplate(vbox, getContainer(), "Protein Search Results");
    }

    private QueryView createProteinGroupSearchView(final ProteinSearchForm form) throws ServletException
    {
        QuerySettings groupsSettings = new QuerySettings(getViewURLHelper(), getRequest(), "ProteinSearchResults");
        groupsSettings.setQueryName(MS2Schema.PROTEIN_GROUPS_FOR_SEARCH_TABLE_NAME);
        groupsSettings.setAllowChooseQuery(false);
        QueryView groupsView = new QueryView(getViewContext(), QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME), groupsSettings)
        {
            protected TableInfo createTable()
            {
                ProteinGroupTableInfo table = ((MS2Schema)getSchema()).createProteinGroupsForSearchTable(null);
                table.addProteinNameFilter(form.getIdentifier(), form.isExactMatch());
                table.addContainerCondition(getContainer(), getUser(), form.isIncludeSubfolders());

                return table;
            }
            
            protected void populateButtonBar(DataView view, ButtonBar bar)
            {
                super.populateButtonBar(view, bar);

                ViewURLHelper excelURL = cloneViewURLHelper();
                excelURL.setAction("exportProteinGroupSearchToExcel.view");
                ActionButton excelButton = new ActionButton("Export to Excel", excelURL);
                bar.add(excelButton);

                ViewURLHelper tsvURL = cloneViewURLHelper();
                tsvURL.setAction("exportProteinGroupSearchToTSV.view");
                ActionButton tsvButton = new ActionButton("Export to TSV", tsvURL);
                bar.add(tsvButton);
            }
        };
        groupsView.setShowExportButtons(false);
        groupsView.setShowCustomizeViewLinkInButtonBar(true);
        groupsView.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);

        groupsView.setTitle("Protein Group Results");
        return groupsView;
    }

    private QueryView createProteinSearchView(ProteinSearchForm form)
        throws ServletException
    {
        QuerySettings proteinsSettings = new QuerySettings(getViewURLHelper(), getRequest(), "PotentialProteins");
        proteinsSettings.setQueryName(MS2Schema.SEQUENCES_TABLE_NAME);
        proteinsSettings.setAllowChooseQuery(false);
        QueryView proteinsView = new QueryView(getViewContext(), QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME), proteinsSettings)
        {
            protected void populateButtonBar(DataView view, ButtonBar bar)
            {
                super.populateButtonBar(view, bar);

                ViewURLHelper excelURL = cloneViewURLHelper();
                excelURL.setAction("exportProteinSearchToExcel.view");
                ActionButton excelButton = new ActionButton("Export to Excel", excelURL);
                bar.add(excelButton);

                ViewURLHelper tsvURL = cloneViewURLHelper();
                tsvURL.setAction("exportProteinSearchToTSV.view");
                ActionButton tsvButton = new ActionButton("Export to TSV", tsvURL);
                bar.add(tsvButton);
            }
        };

        proteinsView.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
        proteinsView.setShowExportButtons(false);
        proteinsView.setShowCustomizeViewLinkInButtonBar(true);
        SequencesTableInfo sequencesTableInfo = (SequencesTableInfo)proteinsView.getTable();
        sequencesTableInfo.addProteinNameFilter(form.getIdentifier(), form.isExactMatch());
        sequencesTableInfo.addContainerCondition(getContainer(), getUser(), true);
        proteinsView.setTitle("Matching Proteins");
        return proteinsView;
    }

    public static class ProteinSearchForm extends FormData
    {
        private String _identifier;
        private Float _minimumProbability;
        private Float _maximumErrorRate;
        private boolean _includeSubfolders;
        private boolean _exactMatch;

        public boolean isExactMatch()
        {
            return _exactMatch;
        }

        public void setExactMatch(boolean exactMatch)
        {
            _exactMatch = exactMatch;
        }

        public String getIdentifier()
        {
            return _identifier;
        }

        public void setIdentifier(String identifier)
        {
            _identifier = identifier;
        }

        public Float getMaximumErrorRate()
        {
            return _maximumErrorRate;
        }

        public void setMaximumErrorRate(Float maximumErrorRate)
        {
            _maximumErrorRate = maximumErrorRate;
        }

        public Float getMinimumProbability()
        {
            return _minimumProbability;
        }

        public void setMinimumProbability(Float minimumProbability)
        {
            _minimumProbability = minimumProbability;
        }

        public boolean isIncludeSubfolders()
        {
            return _includeSubfolders;
        }

        public void setIncludeSubfolders(boolean includeSubfolders)
        {
            _includeSubfolders = includeSubfolders;
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

        HttpView view = new GroovyView("/org/labkey/core/admin/testMascot.gm");
        form.setPassword(("".equals(form.getPassword())) ? "" : "***");  // do not show password in clear
        view.addObject("form", form);
        return includeView(new DialogTemplate(view));
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

            private String trimSafe(String s)
            {
                return (s == null ? "" : s.trim());
            }

            public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
            {
                setMascotServer(trimSafe(httpServletRequest.getParameter("mascotServer")));
                setUserAccount(trimSafe(httpServletRequest.getParameter("mascotUserAccount")));
                setPassword(trimSafe(httpServletRequest.getParameter("mascotUserPassword")));
                setHTTPProxyServer(trimSafe(httpServletRequest.getParameter("mascotHTTPProxy")));
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
    protected Forward testSequest(TestSequestForm form) throws Exception
    {
        requiresGlobalAdmin();

        String originalSequestServer = form.getSequestServer();
        SequestClientImpl sequestClient = new SequestClientImpl(form.getSequestServer());
        String html = sequestClient.testConnectivity();
        if (sequestClient.getErrorCode() == 0)
                html = sequestClient.getEnvironmentConf();

        String message;
        if(sequestClient.getErrorCode() != 0)
        {
            form.setStatus(sequestClient.getErrorCode());
            message = "Test failed.";
            message = message + "<br>" + html;
        }
        else
        {
            message = "Connection test passed.";
            form.setParameters(html);
        }

        form.setMessage(message);

        form.setSequestServer(originalSequestServer);

        HttpView view = new GroovyView("org/labkey/core/admin/testSequest.gm");
        view.addObject("form", form);
        return includeView(new DialogTemplate(view));
    }

    @Jpf.Action
    protected Forward showSequestTest(TestSequestForm form) throws Exception
    {
        return testSequest(form);
    }

    @Jpf.Action
    protected Forward showUpgradeSequestTest(TestSequestForm form) throws Exception
    {
        return testSequest (form);
    }

    public static class TestSequestForm extends FormData
    {
        private String sequestserver;
        private int status;
        private String parameters;
        private String message;

        public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            setSequestServer(httpServletRequest.getParameter("sequestServer").trim());
            super.reset(actionMapping, httpServletRequest);
        }

        public String getSequestServer()
        {
            return (null == sequestserver ? "" : sequestserver);
        }

        public void setSequestServer(String sequestserver)
        {
            this.sequestserver = sequestserver;
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

        private String defaultOrganism = "Unknown unknown";

        public String getDefaultOrganism()
        {
            return this.defaultOrganism;
        }

        public void setDefaultOrganism(String o)
        {
            this.defaultOrganism = o;
        }

        private String shouldGuess = "1";

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
        boolean exportAsWebPage;
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

        public void setExportAsWebPage(boolean exportAsWebPage)
        {
            this.exportAsWebPage = exportAsWebPage;
        }

        public boolean isExportAsWebPage()
        {
            return exportAsWebPage;
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
        private Integer proteinGroupId;

        public Integer getProteinGroupId()
        {
            return proteinGroupId;
        }

        public void setProteinGroupId(Integer proteinGroupId)
        {
            this.proteinGroupId = proteinGroupId;
        }

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

        public void setPeptideId(String peptideId)
        {
            try
            {
                this.peptideId = Long.parseLong(peptideId);
            }
            catch (NumberFormatException e) {}
        }

        public String getPeptideId()
        {
            return Long.toString(peptideId);
        }

        public long getPeptideIdLong()
        {
            return this.peptideId;
        }

        public void setxStart(String xStart)
        {
            try
            {
                this.xStart = Double.parseDouble(xStart);
            }
            catch (NumberFormatException e) {}
        }

        public String getxStart()
        {
            return Double.toString(xStart);
        }

        public double getxStartDouble()
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

        public void setSeqId(String seqId)
        {
            try
            {
                this.seqId = Integer.parseInt(seqId);
            }
            catch (NumberFormatException e) {}
        }

        public String getSeqId()
        {
            return Integer.toString(this.seqId);
        }

        public int getSeqIdInt()
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

        public void setRun(String run)
        {
            try
            {
                this.run = Integer.parseInt(run);
            }
            catch (NumberFormatException e) {}
        }

        public String getRun()
        {
            return Integer.toString(run);
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
