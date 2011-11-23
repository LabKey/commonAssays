/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.labkey.api.action.*;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.StringKeyCache;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.*;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.permissions.*;
import org.labkey.api.security.User;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.settings.AdminConsole.SettingsLinkType;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.WriteableAppProps;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.ms2.compare.*;
import org.labkey.ms2.peptideview.*;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.ImportScanCountsUpgradeJob;
import org.labkey.ms2.pipeline.MSPictureUpgradeJob;
import org.labkey.ms2.pipeline.ProteinProphetPipelineJob;
import org.labkey.ms2.pipeline.mascot.MascotClientImpl;
import org.labkey.ms2.pipeline.mascot.MascotSearchProtocolFactory;
import org.labkey.ms2.protein.*;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.NullOutputStream;
import org.labkey.ms2.protein.tools.PieJChartHelper;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.query.*;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.SensitivitySummary;
import org.labkey.ms2.scoring.ScoringController;
import org.labkey.ms2.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * User: adam
 * Date: Dec 10, 2007
 * Time: 3:57:13 PM
 */
public class MS2Controller extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(MS2Controller.class);
    private static final Logger _log = Logger.getLogger(MS2Controller.class);
    private static final String MS2_VIEWS_CATEGORY = "MS2Views";
    private static final String MS2_DEFAULT_VIEW_CATEGORY = "MS2DefaultView";
    private static final String DEFAULT_VIEW_NAME = "DefaultViewName";
    private static final String LAST_VIEW = "LastView";
    private static final int MAX_INSERTIONS_DISPLAY_ROWS = 1000; // Limit annotation table insertions to 1000 rows
    private static final String SHARED_VIEW_SUFFIX = " (Shared)";
    static final String CAPTION_SCORING_BUTTON = "Compare Scoring";
    private static final int MAX_MATCHING_PROTEINS = 100;
   
    public MS2Controller()
    {
        setActionResolver(_actionResolver);
    }


    public static void registerAdminConsoleLinks()
    {
        AdminConsole.addLink(SettingsLinkType.Management, "ms2", getShowMS2AdminURL(null));
        AdminConsole.addLink(SettingsLinkType.Management, "protein databases", MS2UrlsImpl.get().getShowProteinAdminUrl());
    }

    /**
     * @throws NotFoundException if the run can't be found, has been deleted, etc
     * @throws RedirectException if the run is from another container
     */
    @NotNull
    private MS2Run validateRun(RunForm form) throws ServletException
    {
        if (form.run == 0)
        {
            MS2Fraction fraction = MS2Manager.getFraction(form.getFraction());
            if (fraction != null)
            {
                form.run = fraction.getRun();
            }
        }
        return validateRun(form.run);
    }

    /**
     * @throws NotFoundException if the run can't be found, has been deleted, etc
     * @throws RedirectException if the run is from another container
     */
    @NotNull
    private MS2Run validateRun(int runId) throws ServletException
    {
        Container c = getContainer();
        MS2Run run = MS2Manager.getRun(runId);

        if (null == run)
            throw new NotFoundException("Run " + runId + " not found");
        if (run.isDeleted())
            throw new NotFoundException("Run has been deleted.");
        if (run.getStatusId() == MS2Importer.STATUS_RUNNING)
            throw new NotFoundException("Run is still loading.  Current status: " + run.getStatus());
        if (run.getStatusId() == MS2Importer.STATUS_FAILED)
            throw new NotFoundException("Run failed loading.  Status: " + run.getStatus());

        Container container = run.getContainer();

        if (null == container || !container.equals(c))
        {
            ActionURL url = getViewContext().getActionURL().clone();
            url.setContainer(run.getContainer());
            throw new RedirectException(url);
        }

        return run;
    }


    private NavTree appendRootNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "ms2" : helpTopic));
        root.addChild("MS2 Runs", getShowListURL(getContainer()));
        if (null != title)
            root.addChild(title);
        return root;
    }


    private NavTree appendRunNavTrail(NavTree root, MS2Run run, URLHelper runURL, String title, PageConfig page, String helpTopic)
    {
        appendRootNavTrail(root, null, page, helpTopic);

        if (null != runURL)
            root.addChild(run.getDescription(), runURL);
        else
            root.addChild(run.getDescription());

        if (null != title)
            root.addChild(title);
        return root;
    }


    private NavTree appendAdminNavTrail(NavTree root, String adminPageTitle, ActionURL adminPageURL, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "ms2" : helpTopic));
        root.addChild("Admin Console", PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL());
        root.addChild(adminPageTitle, adminPageURL);
        root.addChild(title);
        return root;
    }


    private NavTree appendProteinAdminNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        return appendAdminNavTrail(root, "Protein Database Admin", MS2UrlsImpl.get().getShowProteinAdminUrl(), title, page, helpTopic);
    }


    private AbstractMS2RunView<? extends WebPartView> getPeptideView(String grouping, MS2Run... runs) throws ServletException
    {
        return MS2RunViewType.getViewType(grouping).createView(getViewContext(), runs);
    }


    public static ActionURL getBeginURL(Container c)
    {
        return new ActionURL(BeginAction.class, c);
    }


    public static ActionURL getPeptideChartURL(Container c, ProteinDictionaryHelpers.GoTypes chartType)
    {
        ActionURL url = new ActionURL(PeptideChartsAction.class, c);
        url.addParameter("chartType", chartType.toString());
        return url;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o)
        {
            return getShowListURL(getContainer());
        }
    }


    public static ActionURL getShowListURL(Container c)
    {
        return new ActionURL(ShowListAction.class, c);
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ProteinSearchWebPart searchView = new ProteinSearchWebPart(true, ProteinSearchForm.createDefault());

            QueryView gridView = ExperimentService.get().createExperimentRunWebPart(getViewContext(), MS2Module.SEARCH_RUN_TYPE);
            gridView.setTitle(MS2Module.MS2_RUNS_NAME);
            gridView.setTitleHref(PageFlowUtil.urlProvider(MS2Urls.class).getShowListUrl(getContainer()));

            return new VBox(searchView, gridView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "MS2 Runs", getPageConfig(), "ms2RunsList");
        }
    }

    public static MenuButton createCompareMenu(Container container, DataView view, boolean experimentRunIds)
    {
        MenuButton compareMenu = new MenuButton("Compare");
        compareMenu.setDisplayPermission(ReadPermission.class);

        ActionURL proteinProphetQueryURL = new ActionURL(MS2Controller.CompareProteinProphetQuerySetupAction.class, container);
        if (experimentRunIds)
        {
            proteinProphetQueryURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("ProteinProphet", view.createVerifySelectedScript(proteinProphetQueryURL, "runs"));

        ActionURL peptideQueryURL = new ActionURL(MS2Controller.ComparePeptideQuerySetupAction.class, container);
        if (experimentRunIds)
        {
            peptideQueryURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("Peptide", view.createVerifySelectedScript(peptideQueryURL, "runs"));

        ActionURL searchEngineURL = new ActionURL(MS2Controller.CompareSearchEngineProteinSetupAction.class, container);
        if (experimentRunIds)
        {
            searchEngineURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("Search Engine Protein", view.createVerifySelectedScript(searchEngineURL, "runs"));

        ActionURL peptidesURL = new ActionURL(MS2Controller.ComparePeptidesSetupAction.class, container);
        if (experimentRunIds)
        {
            peptidesURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("Peptide (Legacy)", view.createVerifySelectedScript(peptidesURL, "runs"));

        ActionURL proteinProphetURL = new ActionURL(MS2Controller.CompareProteinProphetSetupAction.class, container);
        String selectionKey = view.getDataRegion().getSelectionKey();
        proteinProphetURL.addParameter("selectionKey", selectionKey);
        if (experimentRunIds)
        {
            proteinProphetURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("ProteinProphet (Legacy)", view.createVerifySelectedScript(proteinProphetURL, "runs"));

        ActionURL spectraURL = new ActionURL(MS2Controller.SpectraCountSetupAction.class, container);
        if (experimentRunIds)
        {
            spectraURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("Spectra Count", view.createVerifySelectedScript(spectraURL, "runs"));
        return compareMenu;
    }

    public static ActionURL getShowRunURL(Container c, int runId)
    {
        ActionURL url = new ActionURL(ShowRunAction.class, c);
        url.addParameter(RunForm.PARAMS.run, String.valueOf(runId));
        return url;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            MS2Run run = validateRun(form);

            ActionURL currentURL = getViewContext().getActionURL();
            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

            // If the user hasn't customized the view at all, show them their default view
            if (currentURL.getParameters().length == 1)
            {
                Map<String, String> props = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_DEFAULT_VIEW_CATEGORY);
                String params;
                // Check if they've explicitly requested a view by name as their default
                if (props.get(MS2Controller.DEFAULT_VIEW_NAME) != null)
                {
                    Map<String, String> savedViews = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY);
                    params = savedViews.get(props.get(MS2Controller.DEFAULT_VIEW_NAME));
                }
                else
                {
                    // Grab the last URL that they used
                    params = props.get(MS2Controller.LAST_VIEW);
                }

                if (params != null && params.trim().length() > 0)
                {
                    throw new RedirectException(currentURL + "&" + params);
                }
            }
            else if (!getUser().isGuest())
            {
                PropertyManager.PropertyMap props = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_DEFAULT_VIEW_CATEGORY, true);
                ActionURL newURL = currentURL.clone().deleteParameter("run");
                for (String paramName : newURL.getParameterMap().keySet())
                {
                    if (paramName.endsWith("." + QueryParam.offset))
                    {
                        newURL.deleteParameter(paramName);
                    }
                }
                props.put(MS2Controller.LAST_VIEW, newURL.getRawQuery());
                PropertyManager.saveProperties(props);
            }

            WebPartView grid = peptideView.createGridView(form);
            String dataRegionName;

            if (grid instanceof QueryView)
            {
                dataRegionName = ((QueryView)grid).getDataRegionName();
            }
            else
            {
                dataRegionName = ((GridView)grid).getDataRegion().getName();
            }

            VBox vBox = new VBox();
            JspView scriptView = new JspView<String>("/org/labkey/ms2/nestedGridScript.jsp", dataRegionName);
            vBox.addView(scriptView);

            JspView<RunSummaryBean> runSummary = new JspView<RunSummaryBean>("/org/labkey/ms2/runSummary.jsp", new RunSummaryBean());
            runSummary.setFrame(WebPartView.FrameType.PORTAL);
            runSummary.setTitle("Run Overview");
            RunSummaryBean bean = runSummary.getModelBean();
            bean.run = run;
            bean.modHref = modificationHref(run);
            bean.writePermissions = getViewContext().hasPermission(UpdatePermission.class);
            bean.quantAlgorithm = MS2Manager.getQuantAnalysisAlgorithm(form.run);
            vBox.addView(runSummary);

            List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
            SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentURL, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getViewContext().getUser(), run);
            peptideView.addSQLSummaries(peptideFilter, sqlSummaries);

            VBox filterBox = new VBox(new FilterHeaderView(currentURL, form, run), new CurrentFilterView(null, sqlSummaries));
            filterBox.setFrame(WebPartView.FrameType.PORTAL);
            filterBox.setTitle("View");

            vBox.addView(filterBox);

            vBox.addView(grid);
            _run = run;

            return vBox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
                appendRunNavTrail(root, _run, null, null, getPageConfig(), "viewRuns");

            return root;
        }
    }


    public static class RunSummaryBean
    {
        public MS2Run run;
        public String modHref;
        public boolean writePermissions;
        public String quantAlgorithm;
    }


    private class FilterHeaderView extends JspView<FilterHeaderBean>
    {
        private FilterHeaderView(ActionURL currentURL, RunForm form, MS2Run run) throws ServletException, SQLException
        {
            super("/org/labkey/ms2/filterHeader.jsp", new FilterHeaderBean());

            FilterHeaderBean bean = getModelBean();

            bean.run = run;
            bean.applyViewURL = clearFilter(currentURL).setAction(ApplyRunViewAction.class);
            bean.applyView = renderViewSelect(true);
            bean.saveViewURL = currentURL.clone().setAction(SaveViewAction.class);
            bean.manageViewsURL = getManageViewsURL(run, currentURL);
            bean.pickPeptideColumnsURL = getPickPeptideColumnsURL(run, form.getColumns(), currentURL);
            bean.pickProteinColumnsURL = getPickProteinColumnsURL(run, form.getProteinColumns(), currentURL);
            bean.viewTypes = MS2RunViewType.getTypesForRun(run);
            bean.currentViewType = MS2RunViewType.getViewType(form.getGrouping());
            bean.expanded = form.getExpanded();

            String chargeFilterParamName = run.getChargeFilterParamName();
            ActionURL extraFilterURL = currentURL.clone().setAction(AddExtraFilterAction.class);
            extraFilterURL.deleteParameter(chargeFilterParamName + "1");
            extraFilterURL.deleteParameter(chargeFilterParamName + "2");
            extraFilterURL.deleteParameter(chargeFilterParamName + "3");
            extraFilterURL.deleteParameter("tryptic");
            extraFilterURL.deleteParameter("grouping");
            extraFilterURL.deleteParameter("expanded");
            bean.extraFilterURL = extraFilterURL;

            bean.charge1 = defaultIfNull(currentURL.getParameter(chargeFilterParamName + "1"), "0");
            bean.charge2 = defaultIfNull(currentURL.getParameter(chargeFilterParamName + "2"), "0");
            bean.charge3 = defaultIfNull(currentURL.getParameter(chargeFilterParamName + "3"), "0");
            bean.tryptic = form.tryptic;
        }


        private ActionURL clearFilter(ActionURL currentURL)
        {
            ActionURL newURL = currentURL.clone();
            String run = newURL.getParameter("run");
            newURL.deleteParameters();
            if (null != run)
                newURL.addParameter("run", run);
            return newURL;
        }
    }


    public static class FilterHeaderBean
    {
        public MS2Run run;
        public ActionURL applyViewURL;
        public StringBuilder applyView;
        public ActionURL saveViewURL;
        public ActionURL manageViewsURL;
        public ActionURL pickPeptideColumnsURL;
        public ActionURL pickProteinColumnsURL;
        public ActionURL extraFilterURL;
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

    /**
     * @return map from view name to view URL parameters
     */
    private Map<String, String> getViewMap(boolean includeUser, boolean includeShared)
    {
        Map<String, String> m = new HashMap<String, String>();

        if (includeUser)
        {
            Map<String, String> properties = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY);

            for (Map.Entry<String, String> entry : properties.entrySet())
            {
                m.put(entry.getKey(), entry.getValue());
            }
        }

        //In addition to the user views, get shared views attached to this folder
        if (includeShared)
        {
            Map<String, String> mShared = PropertyManager.getProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY);
            for (Map.Entry<String, String> entry : mShared.entrySet())
            {
                String name = entry.getKey();
                if (includeUser)
                    name += SHARED_VIEW_SUFFIX;

                m.put(name, entry.getValue());
            }
        }

        return m;
    }


    public static class CurrentFilterView extends JspView<CurrentFilterView.CurrentFilterBean>
    {
        private CurrentFilterView(String[] headers, List<Pair<String, String>> sqlSummaries)
        {
            super("/org/labkey/ms2/currentFilter.jsp", new CurrentFilterBean(headers, sqlSummaries));
        }

        private CurrentFilterView(CompareQuery query, User user)
        {
            this(new String[]{query.getHeader()}, query.getSQLSummaries(user));
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


    /**
     * Render current user's MS2Views in a drop down box with a submit button beside.
     * Caller is responsible for wrapping this in a <form> and (if desired) a <table>
     */
    private StringBuilder renderViewSelect(boolean selectCurrent)
    {
        Map<String, String> m = getViewMap(true, getContainer().hasPermission(getUser(), ReadPermission.class));

        StringBuilder viewSelect = new StringBuilder("<select id=\"views\" name=\"viewParams\" style=\"width:200\">");
        // The defaultView parameter isn't used directly - it's just something on the URL so that it's clear
        // that the user has explicitly requested the standard view and therefore prevent us from
        // bouncing to the user's defined default
        viewSelect.append("\n<option value=\"doNotApplyDefaultView=yes\">&lt;Select a saved view&gt;</option>\n");
        viewSelect.append("\n<option value=\"doNotApplyDefaultView=yes\">&lt;Standard View&gt;</option>\n");

        String currentViewParams = getViewContext().cloneActionURL().deleteParameter("run").getRawQuery();

        // Use TreeSet to sort by name
        TreeSet<String> names = new TreeSet<String>(m.keySet());

        for (String name : names)
        {
            String viewParams = m.get(name);

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


    private String modificationHref(MS2Run run)
    {
        Map<String, String> fixed = new TreeMap<String, String>();
        Map<String, String> var = new TreeMap<String, String>();

        for (MS2Modification mod : run.getModifications())
        {
            if (mod.getVariable())
                var.put(mod.getAminoAcid() + mod.getSymbol(), Formats.f3.format(mod.getMassDiff()));
            else
                fixed.put(mod.getAminoAcid(), Formats.f3.format(mod.getMassDiff()));
        }

        StringBuilder onClick = new StringBuilder("showHelpDiv(this, 'Modifications', '");
        StringBuilder html = new StringBuilder();

        if (0 == (var.size() + fixed.size()))
            html.append("<tr><td colspan=2><b>None</b></td></tr>");

        if (0 != fixed.size())
        {
            html.append("<tr><td colspan=2><b>Fixed</b></td></tr>");

            for (Map.Entry<String, String> entry : fixed.entrySet())
            {
                html.append("<tr><td class=labkey-form-label>");
                html.append(PageFlowUtil.filter(entry.getKey()));
                html.append("</td><td align=right>");
                html.append(PageFlowUtil.filter(entry.getValue()));
                html.append("</td></tr>");
            }
        }

        if (0 != var.size())
        {
            if (0 != fixed.size())
                html.append("<tr><td colspan=2>&nbsp;</td></tr>");

            html.append("<tr><td colspan=2><b>Variable</b></td></tr>");

            for (Map.Entry<String, String> entry : var.entrySet())
            {
                html.append("<tr><td class=labkey-form-label>");
                html.append(PageFlowUtil.filter(entry.getKey()));
                html.append("</td><td align=right>");
                html.append(PageFlowUtil.filter(entry.getValue()));
                html.append("</td></tr>");
            }
        }

        onClick.append(PageFlowUtil.filter(html.toString()).replace("'", "\\'"));

        onClick.append("'); return false;");

        return PageFlowUtil.textLink("Show Modifications", (ActionURL)null, onClick.toString(), "modificationsLink");
    }


    public static class RenameForm extends RunForm
    {
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }


    public static ActionURL getRenameRunURL(Container c, MS2Run run, ActionURL returnURL)
    {
        ActionURL url = new ActionURL(RenameRunAction.class, c);
        url.addParameter("run", run.getRun());
        url.addReturnURL(returnURL);
        return url;
    }


    @RequiresPermissionClass(UpdatePermission.class)
    public class RenameRunAction extends FormViewAction<RenameForm>
    {
        private MS2Run _run;
        private URLHelper _returnURL;

        public void validateCommand(RenameForm target, Errors errors)
        {
        }

        public ModelAndView getView(RenameForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form);
            _returnURL = form.getReturnURLHelper(getShowRunURL(getContainer(), form.getRun()));

            String description = form.getDescription();
            if (description == null || description.length() == 0)
                description = _run.getDescription();

            RenameBean bean = new RenameBean();
            bean.run = _run;
            bean.description = description;
            bean.returnURL = _returnURL;

            getPageConfig().setFocusId("description");

            return new JspView<RenameBean>("/org/labkey/ms2/renameRun.jsp", bean);
        }

        public boolean handlePost(RenameForm form, BindException errors) throws Exception
        {
            _run = validateRun(form);
            MS2Manager.renameRun(form.getRun(), form.getDescription());
            return true;
        }

        public URLHelper getSuccessURL(RenameForm form)
        {
            return form.getReturnURLHelper();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, _returnURL, "Rename Run", getPageConfig(), null);
        }
    }


    public class RenameBean
    {
        public MS2Run run;
        public String description;
        public URLHelper returnURL;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            long peptideId = form.getPeptideId();
            MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + peptideId);

            int runId = peptide.getRun();

            MS2Run run = validateRun(runId);

            ActionURL currentURL = getViewContext().getActionURL();

            int sqlRowIndex = form.getRowIndex();
            int rowIndex = sqlRowIndex - 1;  // Switch 1-based, JDBC row index to 0-based row index for array lookup

            long[] peptideIndex = null;

            //if no row index was passed, don't try to look it up, as it always results
            //in an error being written to the log. There are now other instances where
            //peptide sequences are displayed with hyperlinks to this action, and they
            //often do not want the prev/next buttons to be enabled.
            if (rowIndex >= 0)
            {
                peptideIndex = getPeptideIndex(currentURL, run);
                rowIndex = MS2Manager.verifyRowIndex(peptideIndex, rowIndex, peptideId);
                sqlRowIndex = rowIndex + 1;  // Different rowIndex may be returned -- make sure sqlRowIndex matches
            }

            peptide.init(form.getTolerance(), form.getxStartDouble(), form.getxEnd());

            ActionURL previousURL = null;
            ActionURL nextURL = null;
            ActionURL showGzURL = null;

            // Display next and previous only if we have a cached index and a valid pointer
            if (null != peptideIndex && -1 != rowIndex)
            {
                if (0 == rowIndex)
                    previousURL = null;
                else
                {
                    previousURL = getViewContext().cloneActionURL();
                    previousURL.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex - 1]));
                    previousURL.replaceParameter("rowIndex", String.valueOf(sqlRowIndex - 1));
                }

                if (rowIndex == (peptideIndex.length - 1))
                    nextURL = null;
                else
                {
                    nextURL = getViewContext().cloneActionURL();
                    nextURL.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex + 1]));
                    nextURL.replaceParameter("rowIndex", String.valueOf(sqlRowIndex + 1));
                }

                showGzURL = getViewContext().cloneActionURL();
                showGzURL.deleteParameter("seqId");
                showGzURL.deleteParameter("rowIndex");
                showGzURL.setAction(ShowGZFileAction.class);
            }

            setTitle(peptide.toString());

            VBox result = new VBox();

            String nextPrevStr = "";
            if (null != previousURL) {
                 nextPrevStr += PageFlowUtil.textLink("Previous", previousURL);
            }
            if (null != nextURL) {
                 nextPrevStr += PageFlowUtil.textLink("Next", nextURL);
            }
            if (nextPrevStr.length() > 0) {
                result.addView(new HtmlView(nextPrevStr));
            }

            ShowPeptideContext ctx = new ShowPeptideContext(form, run, peptide, currentURL, previousURL, nextURL, showGzURL, modificationHref(run), getContainer(), getUser());
            JspView<ShowPeptideContext> peptideView = new JspView<ShowPeptideContext>("/org/labkey/ms2/showPeptide.jsp", ctx);
            peptideView.setTitle("Peptide Details");

            NavTree pepNavTree = new NavTree();
            if (null != ctx.pepSearchHref && ctx.pepSearchHref.length() > 0)
                pepNavTree.addChild("Find MS1 Features", ctx.pepSearchHref);
            pepNavTree.addChild("Blast", AppProps.getInstance().getBLASTServerBaseURL() + peptide.getTrimmedPeptide());
            peptideView.setNavMenu(pepNavTree);
            peptideView.setIsWebPart(false);

            peptideView.setFrame(WebPartView.FrameType.PORTAL);
            result.addView(peptideView);
            PeptideQuantitation quant = peptide.getQuantitation();
            if (quant != null)
            {
                JspView<ShowPeptideContext> quantView = new JspView<ShowPeptideContext>("/org/labkey/ms2/showPeptideQuantitation.jsp", ctx);
                quantView.setTitle("Quantitation (performed on " + peptide.getCharge() + "+)");
                getContainer().hasPermission(getUser(), UpdatePermission.class);
                {
                    ActionURL editUrl = getViewContext().getActionURL().clone();
                    editUrl.setAction(MS2Controller.EditElutionGraphAction.class);
                    ActionURL toggleUrl = getViewContext().getActionURL().clone();
                    toggleUrl.setAction(MS2Controller.ToggleValidQuantitationAction.class);

                    NavTree navTree = new NavTree();
                    if (quant.findScanFile() != null && !"q3".equals(run.getQuantAnalysisType()))
                    {
                        navTree.addChild("Edit Elution Profile", editUrl);
                    }
                    navTree.addChild((quant.includeInProteinCalc() ? "Invalidate" : "Revalidate") + " Quantitation Results", toggleUrl);
                    quantView.setNavMenu(navTree);
                    quantView.setIsWebPart(false);
                }

                quantView.setFrame(WebPartView.FrameType.PORTAL);
                result.addView(quantView);
            }
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    private long[] getPeptideIndex(ActionURL currentURL, MS2Run run) throws SQLException, ServletException
    {
        try
        {
            AbstractMS2RunView view = getPeptideView(currentURL.getParameter("grouping"), run);
            return view.getPeptideIndex(currentURL);
        }
        catch (RuntimeSQLException e)
        {
            if (e.getSQLException() instanceof SQLGenerationException)
            {
                throw new NotFoundException("Invalid filter " + e.getSQLException().toString());
            }
            throw e;
        }
    }


    public static ActionURL getLoadGoURL()
    {
        return new ActionURL(LoadGoAction.class, ContainerManager.getRoot());
    }


    @RequiresSiteAdmin
    public class LoadGoAction extends FormViewAction<Object>
    {
        private String _message = null;

        public void validateCommand(Object target, Errors errors)
        {
        }

        public ModelAndView getView(Object o, boolean reshow, BindException errors) throws Exception
        {
            return new GoView();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setTitle((GoLoader.isGoLoaded().booleanValue() ? "Reload" : "Load") + " GO Annotations");
            setHelpTopic(new HelpTopic("annotations"));
            return null;  // TODO: Admin navtrail
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            GoLoader loader;

            if ("1".equals(getViewContext().get("manual")))
            {
                Map<String, MultipartFile> fileMap = getFileMap();
                MultipartFile goFile = fileMap.get("gofile");                       // TODO: Check for NULL and display error
                loader = GoLoader.getStreamLoader(goFile.getInputStream());
            }
            else
            {
                loader = GoLoader.getFtpLoader();
            }

            if (null != loader)
            {
                loader.load();
                Thread.sleep(2000);
            }
            else
            {
                _message = "Can't load GO annotations, a GO annotation load is already in progress.  See below for details.";
            }

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return getGoStatusURL(_message);
        }

        private class GoView extends TabStripView
        {
            public List<NavTree> getTabList()
            {
                return Arrays.<NavTree>asList(new TabInfo("Automatic", "automatic", getLoadGoURL()), new TabInfo("Manual", "manual", getLoadGoURL()));
            }

            public HttpView getTabView(String tabId) throws Exception
            {
                if ("manual".equals(tabId))
                    return new JspView("/org/labkey/ms2/loadGoManual.jsp");
                else
                    return new JspView("/org/labkey/ms2/loadGoAutomatic.jsp");
            }
        }
    }


    private ActionURL getGoStatusURL(String message)
    {
        ActionURL url = new ActionURL(GoStatusAction.class, ContainerManager.getRoot());
        if (null != message)
            url.addParameter("message", message);
        return url;
    }


    @RequiresSiteAdmin
    public class GoStatusAction extends SimpleViewAction<GoForm>
    {
        public ModelAndView getView(GoForm form, BindException errors) throws Exception
        {
            return GoLoader.getCurrentStatus(form.getMessage());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setTitle("GO Load Status");
            setHelpTopic(new HelpTopic("annotations"));
            return null;
        }
    }


    private static class GoForm
    {
        String _message = null;

        public String getMessage()
        {
            return _message;
        }

        public void setMessage(String message)
        {
            _message = message;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PeptideChartsAction extends SimpleViewAction<ChartForm>
    {
        private ProteinDictionaryHelpers.GoTypes _goChartType;
        private MS2Run _run;

        public ModelAndView getView(ChartForm form, BindException errors) throws Exception
        {
            ViewContext ctx = getViewContext();
            ActionURL queryURL = ctx.cloneActionURL();
            String queryString = (String) ctx.get("queryString");
            queryURL.setRawQuery(queryString);

            // Shove the run id into the form bean. Since it's on directly on the URL it won't be bound directly
            if (queryURL.getParameter("run") != null)
            {
                try
                {
                    form.run = Integer.parseInt(queryURL.getParameter("run"));
                }
                catch (NumberFormatException ignored) {}
            }
            _run = validateRun(form);

            _goChartType = ProteinDictionaryHelpers.GTypeStringToEnum(form.getChartType());

            AbstractMS2RunView<? extends WebPartView> peptideView = getPeptideView(queryURL.getParameter("grouping"), _run);

            Map<String, SimpleFilter> filters = peptideView.getFilter(queryURL, _run);

            String chartTitle = "GO " + _goChartType + " Classifications";
            SQLFragment fragment = peptideView.getProteins(queryURL, _run, form);
            PieJChartHelper pjch = PieJChartHelper.prepareGOPie(chartTitle, fragment, _goChartType);
            pjch.renderAsPNG(new NullOutputStream());

            GoChartBean bean = new GoChartBean();
            bean.run = _run;
            bean.chartTitle = chartTitle;
            bean.goChartType = _goChartType;
            bean.filterInfos = filters;
            bean.imageMap = ImageMapUtilities.getImageMap("pie1", pjch.getChartRenderingInfo());
            bean.foundData = !pjch.getDataset().getExtraInfo().isEmpty();
            bean.queryString = queryString;
            bean.grouping = form.getGrouping();
            bean.pieHelperObjName = "piechart-" + (new Random().nextInt(1000000000));
            bean.chartURL = new ActionURL(DoOnePeptideChartAction.class, getContainer()).addParameter("ctype", _goChartType.toString()).addParameter("helpername", bean.pieHelperObjName);

            PIE_CHART_CACHE.put(bean.pieHelperObjName, pjch, CacheManager.HOUR * 2);

            return new JspView<GoChartBean>("/org/labkey/ms2/peptideChart.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL runURL = new ActionURL(ShowRunAction.class, getContainer());
            String queryString = getViewContext().getActionURL().getParameter("queryString");
            runURL.setRawQuery(queryString);

            return appendRunNavTrail(root, _run, runURL, "GO " + _goChartType + " Chart", getPageConfig(), "viewingGeneOntologyData");
        }
    }


    public static class GoChartBean
    {
        public MS2Run run;
        public ProteinDictionaryHelpers.GoTypes goChartType;
        public String chartTitle;
        public Map<String, SimpleFilter> filterInfos;
        public String pieHelperObjName;
        public ActionURL chartURL;
        public String imageMap;
        public boolean foundData;
        public String queryString;
        public String grouping;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class GetProteinGroupingPeptidesAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            MS2Run run = validateRun(form);

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);
            getPageConfig().setTemplate(PageConfig.Template.None);

            return peptideView.getPeptideViewForProteinGrouping(form.getProteinGroupingId(), form.getColumns());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    private ActionURL getManageViewsURL(MS2Run run, ActionURL runURL)
    {
        ActionURL url = new ActionURL(ManageViewsAction.class, getContainer());
        url.addParameter("run", run.getRun());
        url.addReturnURL(runURL);
        return url;
    }

    public static class ManageViewsForm extends RunForm
    {
        private String _defaultViewName;
        private String[] _viewsToDelete;
        private String _defaultViewType;

        public String getDefaultViewType()
        {
            return _defaultViewType;
        }

        public void setDefaultViewType(String defaultViewType)
        {
            _defaultViewType = defaultViewType;
        }

        public String getDefaultViewName()
        {
            return _defaultViewName;
        }

        public void setDefaultViewName(String defaultViewName)
        {
            _defaultViewName = defaultViewName;
        }

        public String[] getViewsToDelete()
        {
            return _viewsToDelete;
        }

        public void setViewsToDelete(String[] viewsToDelete)
        {
            _viewsToDelete = viewsToDelete;
        }
    }

    @RequiresLogin
    @RequiresPermissionClass(ReadPermission.class)
    public class ManageViewsAction extends FormViewAction<ManageViewsForm>
    {
        private MS2Run _run;
        private ActionURL _returnURL;

        public void validateCommand(ManageViewsForm form, Errors errors)
        {
        }

        public ModelAndView getView(ManageViewsForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form);

            _returnURL = form.getReturnActionURL();

            DefaultViewType defaultViewType;
            String viewName;
            Map<String, String> props = PropertyManager.getProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_DEFAULT_VIEW_CATEGORY);
            if (props.get(MS2Controller.DEFAULT_VIEW_NAME) == null)
            {
                defaultViewType = DefaultViewType.LastViewed;
                viewName = null;
            }
            else
            {
                defaultViewType = DefaultViewType.Manual;
                viewName = props.get(MS2Controller.DEFAULT_VIEW_NAME);
            }

            ManageViewsBean bean = new ManageViewsBean(_returnURL, defaultViewType, getViewMap(true, getContainer().hasPermission(getUser(), DeletePermission.class)), viewName);
            JspView<ManageViewsBean> view = new JspView<ManageViewsBean>("/org/labkey/ms2/manageViews.jsp", bean);
            view.setFrame(WebPartView.FrameType.PORTAL);
            view.setTitle("Manage Views");
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, _returnURL, "Customize Views", getPageConfig(), "viewRun");
        }

        public boolean handlePost(ManageViewsForm form, BindException errors) throws Exception
        {
            String[] viewNames = form.getViewsToDelete();

            if (null != viewNames)
            {
                PropertyManager.PropertyMap m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

                for (String viewName : viewNames)
                    m.remove(viewName);

                PropertyManager.saveProperties(m);

                // NOTE: If names collide between shared and user-specific view names (unlikely since we append "(Shared)" to
                // project views) only the shared names will be seen and deleted. Local names ending in "(Shared)" are shadowed
                if (getContainer().hasPermission(getUser(), DeletePermission.class))
                {
                    m = PropertyManager.getWritableProperties(getContainer().getId(), MS2_VIEWS_CATEGORY, true);

                    for (String name : viewNames)
                    {
                        if (name.endsWith(SHARED_VIEW_SUFFIX))
                            name = name.substring(0, name.length() - SHARED_VIEW_SUFFIX.length());

                        m.remove(name);
                    }

                    PropertyManager.saveProperties(m);
                }
            }

            DefaultViewType viewType = DefaultViewType.valueOf(form.getDefaultViewType());

            PropertyManager.PropertyMap m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_DEFAULT_VIEW_CATEGORY, true);
            m.put(DEFAULT_VIEW_NAME, viewType == DefaultViewType.Manual ? form.getDefaultViewName() : null);
            PropertyManager.saveProperties(m);

            return true;
        }

        public ActionURL getSuccessURL(ManageViewsForm runForm)
        {
            return runForm.getReturnActionURL();
        }
    }

    public enum DefaultViewType
    {
        LastViewed("Remember the last MS2 run view that I looked at and use it the next time I look at a different MS2 run"),
        Manual("Use the selected default below when I look at a MS2 run");

        private final String _description;

        private DefaultViewType(String description)
        {
            _description = description;
        }

        public String getDescription()
        {
            return _description;
        }
    }

    public static class ManageViewsBean
    {
        private ActionURL _returnURL;
        private DefaultViewType _defaultViewType;
        private Map<String, String> _views;
        private final String _viewName;

        public ManageViewsBean(ActionURL returnURL, DefaultViewType defaultViewType, Map<String, String> views, String viewName)
        {
            _returnURL = returnURL;
            _defaultViewType = defaultViewType;
            _views = views;
            _viewName = viewName;
        }

        public ActionURL getReturnURL()
        {
            return _returnURL;
        }

        public DefaultViewType getDefaultViewType()
        {
            return _defaultViewType;
        }

        public Map<String, String> getViews()
        {
            return _views;
        }

        public String getViewName()
        {
            return _viewName;
        }
    }

    public static class PickViewBean
    {
        public ActionURL nextURL;
        public StringBuilder select;
        public HttpView extraOptionsView;
        public String viewInstructions;
        public int runList;
        public String buttonText;
    }

    public abstract class LegacyCompareSetupAction extends AbstractRunListCreationAction<RunListForm>
    {
        private String _optionsJSP;
        private String _description;

        public LegacyCompareSetupAction(String optionsJSP, String description)
        {
            super(RunListForm.class, true);
            _optionsJSP = optionsJSP;
            _description = description;
        }

        protected ModelAndView getView(RunListForm form, BindException errors, int runListId)
        {
            JspView<CompareOptionsBean> extraCompareOptions = new JspView<CompareOptionsBean>(_optionsJSP);

            ActionURL nextURL = getViewContext().cloneActionURL().setAction(ApplyCompareViewAction.class);
            return pickView(nextURL, "Select a view to apply a filter to all the runs.", extraCompareOptions, runListId, "Compare");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild(_description);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ComparePeptidesSetupAction extends LegacyCompareSetupAction
    {
        public ComparePeptidesSetupAction()
        {
            super("/org/labkey/ms2/compare/comparePeptidesOptions.jsp", "Compare Peptides (Legacy) Setup");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class CompareSearchEngineProteinSetupAction extends LegacyCompareSetupAction
    {
        public CompareSearchEngineProteinSetupAction()
        {
            super("/org/labkey/ms2/compare/compareSearchEngineProteinOptions.jsp", "Compare Search Engine Protein Setup");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class CompareProteinProphetSetupAction extends LegacyCompareSetupAction
    {
        public CompareProteinProphetSetupAction()
        {
            super("/org/labkey/ms2/compare/compareProteinProphetOptions.jsp", "Compare ProteinProphet (Legacy) Setup");
        }
    }



    @RequiresPermissionClass(ReadPermission.class)
    public class CompareProteinProphetQuerySetupAction extends AbstractRunListCreationAction<PeptideFilteringComparisonForm>
    {
        public CompareProteinProphetQuerySetupAction()
        {
            super(PeptideFilteringComparisonForm.class, false);
        }

        protected PeptideFilteringComparisonForm getCommand(HttpServletRequest request) throws Exception
        {
            PeptideFilteringComparisonForm form = super.getCommand(request);
            Map<String, String> prefs = getPreferences(CompareProteinProphetQuerySetupAction.class);
            form.setPeptideFilterType(prefs.get(PeptideFilteringFormElements.peptideFilterType.name()) == null ? ProphetFilterType.none.toString() : prefs.get(PeptideFilteringFormElements.peptideFilterType.name()));
            form.setProteinGroupFilterType(prefs.get(PeptideFilteringFormElements.proteinGroupFilterType.name()) == null ? ProphetFilterType.none.toString() : prefs.get(PeptideFilteringFormElements.proteinGroupFilterType.name()));
            form.setOrCriteriaForEachRun(Boolean.parseBoolean(prefs.get(PeptideFilteringFormElements.orCriteriaForEachRun.name())));
            form.setDefaultPeptideCustomView(prefs.get(PEPTIDES_FILTER_VIEW_NAME));
            form.setDefaultProteinGroupCustomView(prefs.get(PROTEIN_GROUPS_FILTER_VIEW_NAME));
            form.setNormalizeProteinGroups(Boolean.parseBoolean(prefs.get(NORMALIZE_PROTEIN_GROUPS_NAME)));
            if (prefs.get(PIVOT_TYPE_NAME) != null)
            {
                form.setPivotType(prefs.get(PIVOT_TYPE_NAME));
            }
            if (prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name()) != null)
            {
                try
                {
                    form.setPeptideProphetProbability(new Float(prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name())));
                }
                catch (NumberFormatException ignored) {}
            }
            if (prefs.get(PeptideFilteringFormElements.proteinProphetProbability.name()) != null)
            {
                try
                {
                    form.setProteinProphetProbability(new Float(prefs.get(PeptideFilteringFormElements.proteinProphetProbability.name())));
                }
                catch (NumberFormatException ignored) {}
            }

            return form;
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors, int runListId)
        {
            CompareOptionsBean<PeptideFilteringComparisonForm> bean = new CompareOptionsBean<PeptideFilteringComparisonForm>(new ActionURL(CompareProteinProphetQueryAction.class, getContainer()), runListId, form);

            return new JspView<CompareOptionsBean>("/org/labkey/ms2/compare/compareProteinProphetQueryOptions.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setHelpTopic("compareProteinProphet");
            return root.addChild("Compare ProteinProphet Options");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ComparePeptideQuerySetupAction extends AbstractRunListCreationAction<PeptideFilteringComparisonForm>
    {
        public ComparePeptideQuerySetupAction()
        {
            super(PeptideFilteringComparisonForm.class, false);
        }

        protected PeptideFilteringComparisonForm getCommand(HttpServletRequest request) throws Exception
        {
            PeptideFilteringComparisonForm form = super.getCommand(request);
            Map<String, String> prefs = getPreferences(ComparePeptideQuerySetupAction.class);
            form.setPeptideFilterType(prefs.get(PeptideFilteringFormElements.peptideFilterType.name()) == null ? ProphetFilterType.none.toString() : prefs.get(PeptideFilteringFormElements.peptideFilterType.name()));
            form.setDefaultPeptideCustomView(prefs.get(PEPTIDES_FILTER_VIEW_NAME));
            if (prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name()) != null)
            {
                try
                {
                    form.setPeptideProphetProbability(new Float(prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name())));
                }
                catch (NumberFormatException ignored) {}
            }
            form.setTargetProtein(prefs.get(PeptideFilteringFormElements.targetProtein.name()));            
            return form;
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors, int runListId)
        {
            CompareOptionsBean<PeptideFilteringComparisonForm> bean = new CompareOptionsBean<PeptideFilteringComparisonForm>(new ActionURL(ComparePeptideQueryAction.class, getContainer()), runListId, form);

            return new JspView<CompareOptionsBean>("/org/labkey/ms2/compare/comparePeptideQueryOptions.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare Peptides Options");
        }
    }


    public enum PeptideFilteringFormElements
    {
        peptideFilterType,
        peptideProphetProbability,
        proteinGroupFilterType,
        proteinProphetProbability,
        orCriteriaForEachRun,
        runList,
        spectraConfig,
        pivotType,
        targetProtein,
        targetSeqId,
        targetProteinMsg,
        matchingProtNames,
        matchingSeqIds
      }

    public enum PivotType
    {
        run, fraction
    }

    public enum ProphetFilterType
    {
        none, probability, customView
    }

    public static class PeptideFilteringComparisonForm extends RunListForm implements PeptideFilter
    {
        private String _peptideFilterType = ProphetFilterType.none.toString();
        private String _proteinGroupFilterType = ProphetFilterType.none.toString();
        private Float _peptideProphetProbability;
        private Float _proteinProphetProbability;
        private boolean _orCriteriaForEachRun;
        private String _defaultPeptideCustomView;
        private String _defaultProteinGroupCustomView;
        private boolean _normalizeProteinGroups;
        private String _pivotType = PivotType.run.toString();
        private String _targetProtein;
        private Integer _targetSeqId;
        private String _targetProteinMsg;
        private String _matchingSeqIds;
        private String _matchingProtNames;

        public String getTargetProteinMsg()
        {
            return _targetProteinMsg;
        }

        public void setTargetProteinMsg(String targetProteinMsg)
        {
            _targetProteinMsg = targetProteinMsg;
        }

        public Integer getTargetSeqId()
        {
            return _targetSeqId;
        }

        public void setTargetSeqId(Integer targetSeqId)
        {
            _targetSeqId = targetSeqId;
        }

        public String getTargetProtein()
        {
            return _targetProtein;
        }

        public void setTargetProtein(String targetProtein)
        {
            _targetProtein = targetProtein;
        }

        public String getMatchingSeqIds()
        {
            return _matchingSeqIds;
        }

        public void setMatchingSeqIds(String matchingSeqIds)
        {
            _matchingSeqIds = matchingSeqIds;
        }

        public String getMatchingProtNames()
        {
            return _matchingProtNames;
        }

        public void setMatchingProtNames(String matchingProtNames)
        {
            _matchingProtNames = matchingProtNames;
        }
        public String getPeptideFilterType()
        {
            return _peptideFilterType;
        }

        public boolean isNoPeptideFilter()
        {
            return !isCustomViewPeptideFilter() && !isPeptideProphetFilter();
        }

        public boolean isPeptideProphetFilter()
        {
            return ProphetFilterType.probability.toString().equals(getPeptideFilterType());
        }

        public boolean isCustomViewPeptideFilter()
        {
            return ProphetFilterType.customView.toString().equals(getPeptideFilterType());
        }

        public void setPeptideFilterType(String peptideFilterType)
        {
            _peptideFilterType = peptideFilterType;
        }

        public boolean isNoProteinGroupFilter()
        {
            return !isCustomViewProteinGroupFilter() && !isProteinProphetFilter();
        }

        public boolean isProteinProphetFilter()
        {
            return ProphetFilterType.probability.toString().equals(getProteinGroupFilterType());
        }

        public boolean isCustomViewProteinGroupFilter()
        {
            return ProphetFilterType.customView.toString().equals(getProteinGroupFilterType());
        }


        public String getProteinGroupFilterType()
        {
            return _proteinGroupFilterType;
        }

        public void setProteinGroupFilterType(String proteinGroupFilterType)
        {
            _proteinGroupFilterType = proteinGroupFilterType;
        }

        public Float getProteinProphetProbability()
        {
            return _proteinProphetProbability;
        }

        public void setProteinProphetProbability(Float proteinProphetProbability)
        {
            _proteinProphetProbability = proteinProphetProbability;
        }

        public Float getPeptideProphetProbability()
        {
            return _peptideProphetProbability;
        }

        public void setPeptideProphetProbability(Float peptideProphetProbability)
        {
            _peptideProphetProbability = peptideProphetProbability;
        }

        public void setDefaultPeptideCustomView(String defaultPeptideCustomView)
        {
            _defaultPeptideCustomView = defaultPeptideCustomView;
        }

        public String getPeptideCustomViewName(ViewContext context)
        {
            String result = context.getRequest().getParameter(PEPTIDES_FILTER_VIEW_NAME);
            if (result == null)
            {
                result = _defaultPeptideCustomView;
            }
            if ("".equals(result))
            {
                return null;
            }
            return result;
        }

        public String getDefaultProteinGroupCustomView()
        {
            return _defaultProteinGroupCustomView;
        }

        public void setDefaultProteinGroupCustomView(String defaultProteinGroupCustomView)
        {
            _defaultProteinGroupCustomView = defaultProteinGroupCustomView;
        }

        public String getProteinGroupCustomViewName(ViewContext context)
        {
            String result = context.getRequest().getParameter(PROTEIN_GROUPS_FILTER_VIEW_NAME);
            if (result == null)
            {
                result = _defaultProteinGroupCustomView;
            }
            if ("".equals(result))
            {
                return null;
            }
            return result;
        }

        public boolean isOrCriteriaForEachRun()
        {
            return _orCriteriaForEachRun;
        }

        public void setOrCriteriaForEachRun(boolean orCriteriaForEachRun)
        {
            _orCriteriaForEachRun = orCriteriaForEachRun;
        }

        public void appendPeptideFilterDescription(StringBuilder title, ViewContext context)
        {
            if (null != getTargetSeqId() && null != getTargetProtein())
            {
                title.append("Protein ");
                title.append(getTargetProtein());
                title.append(",  ");
            }
             if (isPeptideProphetFilter() && getPeptideProphetProbability() != null)
            {
                title.append("PeptideProphet >= ");
                title.append(getPeptideProphetProbability());
            }
            else if (isCustomViewPeptideFilter())
            {
                title.append("\"");
                title.append(getPeptideCustomViewName(context) == null ? "<default>" : getPeptideCustomViewName(context));
                title.append("\" peptide filter");
            }
            else
            {
                title.append("No peptide filter");
            }
        }

        public boolean isNormalizeProteinGroups()
        {
            return _normalizeProteinGroups;
        }

        public void setNormalizeProteinGroups(boolean normalizeProteinGroups)
        {
            _normalizeProteinGroups = normalizeProteinGroups;
        }

        @NotNull
        public PivotType getPivotTypeEnum()
        {
            if (_pivotType == null)
            {
                return PivotType.run;
            }
            return PivotType.valueOf(_pivotType);
        }

        public String getPivotType()
        {
            return _pivotType;
        }

        public void setPivotType(String pivotType)
        {
            _pivotType = pivotType;
        }

        private static void validateTargetProtein(PeptideFilteringComparisonForm form, ViewContext context)
        {
            form.setMatchingProtNames(null);
            form.setMatchingSeqIds(null);
            form.setTargetProteinMsg(null);

            if (form.getTargetProtein() != null)
            {
                ArrayList<Integer> matchingIds = new ArrayList<Integer>();
                StringBuffer sbIds = new StringBuffer();
                StringBuffer sbNames = new StringBuffer();
                if (form.getTargetSeqId() == null) // new search)
                {
                    Results rsMatches = null;
                    SequencesTableInfo tableInfo = new SequencesTableInfo(new MS2Schema(context.getUser(), context.getContainer()));
                    tableInfo.addProteinNameFilter(form.getTargetProtein(), false);
                    List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
                    cols.add(tableInfo.getColumn("SeqId"));
                    cols.add(tableInfo.getColumn("BestName"));
                    try
                    {
                        rsMatches = Table.select(tableInfo, cols, null, null);
                        String sep = "";
                        for (int i = 0; i < rsMatches.getSize(); i++)
                        {
                            rsMatches.next();
                            matchingIds.add(rsMatches.getInt(1));
                            sbNames.append(sep);
                            sbIds.append(sep);
                            sbNames.append(rsMatches.getString(2));
                            sbIds.append(rsMatches.getInt(1));
                            sep = ",";
                            if (i == MAX_MATCHING_PROTEINS)
                                break;
                        }
                    }
                    catch (SQLException e)
                    {
                        throw new RuntimeSQLException(e);
                    }
                    finally
                    {
                        if (rsMatches != null)
                        {
                            try
                            {
                                rsMatches.close();
                            }
                            catch (SQLException ignored)
                            {
                            }
                        }
                    }
                    if (matchingIds.size() == 1)
                    {
                        form.setTargetProtein(sbNames.toString());
                        form.setTargetSeqId(matchingIds.get(0));
                    }
                    else
                    {
                        if (matchingIds.size() == 0)
                            form.setTargetProteinMsg("No matching protein found. ");
                        else if (matchingIds.size() >= MAX_MATCHING_PROTEINS)
                            form.setTargetProteinMsg(" More than " + MAX_MATCHING_PROTEINS + " returned.  Try a more specific search string");
                        else
                        {
                            form.setTargetProteinMsg("Matching proteins found: ");
                            form.setMatchingProtNames(sbNames.toString());
                            form.setMatchingSeqIds(sbIds.toString());
                        }
                    }
                }
                else
                {
                    // use the seqId it came in with
                    sbNames.append(form.getTargetProtein());
                    sbIds.append(form.getTargetSeqId());
                }
            }
            else    // no protein name entered  or name cleared
            {
                form.setTargetSeqId(null);
            }
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class CompareProteinProphetQueryAction extends RunListHandlerAction<PeptideFilteringComparisonForm, ComparisonCrosstabView>
    {
        private PeptideFilteringComparisonForm _form;

        public CompareProteinProphetQueryAction()
        {
            super(PeptideFilteringComparisonForm.class);
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors) throws Exception
        {
            _form = form;
            return super.getView(form, errors);
        }

        protected ModelAndView getHtmlView(PeptideFilteringComparisonForm form, BindException errors) throws Exception
        {
            ComparisonCrosstabView gridView = createInitializedQueryView(form, errors, false, null);

            Map<String, String> prefs = getPreferences(CompareProteinProphetQuerySetupAction.class);
            prefs.put(PeptideFilteringFormElements.peptideFilterType.name(), form.getPeptideFilterType());
            prefs.put(PeptideFilteringFormElements.proteinGroupFilterType.name(), form.getProteinGroupFilterType());
            prefs.put(PeptideFilteringFormElements.orCriteriaForEachRun.name(), Boolean.toString(form.isOrCriteriaForEachRun()));
            prefs.put(PEPTIDES_FILTER_VIEW_NAME, form.getPeptideCustomViewName(getViewContext()));
            prefs.put(PROTEIN_GROUPS_FILTER_VIEW_NAME, form.getProteinGroupCustomViewName(getViewContext()));
            prefs.put(PIVOT_TYPE_NAME, form.getPivotTypeEnum().toString());
            prefs.put(NORMALIZE_PROTEIN_GROUPS_NAME, Boolean.toString(form.isNormalizeProteinGroups()));
            prefs.put(PeptideFilteringFormElements.peptideProphetProbability.name(), form.getPeptideProphetProbability() == null ? null : form.getPeptideProphetProbability().toString());
            prefs.put(PeptideFilteringFormElements.proteinProphetProbability.name(), form.getProteinProphetProbability() == null ? null : form.getProteinProphetProbability().toString());
            if (!getUser().isGuest())
            {
                // Non-guests are stored in the database, guests get it stored in their session
                PropertyManager.saveProperties(prefs);
            }

            Map<String, String> props = new HashMap<String, String>();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put(PEPTIDES_FILTER_VIEW_NAME, getViewContext().getActionURL().getParameter(PEPTIDES_FILTER_VIEW_NAME));
            props.put(PROTEIN_GROUPS_FILTER_VIEW_NAME, getViewContext().getActionURL().getParameter(PROTEIN_GROUPS_FILTER_VIEW_NAME));
            props.put("comparisonName", "ProteinProphetCrosstab");
            GWTView gwtView = new GWTView(org.labkey.ms2.client.MS2VennDiagramView.class, props);
            gwtView.setTitle("Comparison Overview");
            gwtView.setFrame(WebPartView.FrameType.PORTAL);
            gwtView.enableExpandCollapse("ProteinProphetQueryCompare", true);

            gridView.setTitle("Comparison Details");
            gridView.setFrame(WebPartView.FrameType.PORTAL);

            return new VBox(gwtView, gridView);
        }

        protected ComparisonCrosstabView createQueryView(PeptideFilteringComparisonForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList(), false, getViewContext());
            schema.setRuns(runs);
            if (form.isNormalizeProteinGroups())
            {
                return new NormalizedProteinProphetCrosstabView(schema, form, getViewContext().getActionURL());
            }
            else
            {
                return new ProteinProphetCrosstabView(schema, form, getViewContext().getActionURL());
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (_form != null)
            {
                ActionURL setupURL = new ActionURL(CompareProteinProphetQuerySetupAction.class, getContainer());
                setupURL.addParameter(PeptideFilteringFormElements.peptideFilterType, _form.getPeptideFilterType());
                setupURL.addParameter(PeptideFilteringFormElements.proteinGroupFilterType, _form.getProteinGroupFilterType());
                if (_form.getPeptideProphetProbability() != null)
                {
                    setupURL.addParameter(PeptideFilteringFormElements.peptideProphetProbability, _form.getPeptideProphetProbability().toString());
                }
                if (_form.getProteinProphetProbability() != null)
                {
                    setupURL.addParameter(PeptideFilteringFormElements.proteinProphetProbability, _form.getProteinProphetProbability().toString());
                }
                setupURL.addParameter(PeptideFilteringFormElements.runList, _form.getRunList() == null ? -1 : _form.getRunList());
                setupURL.addParameter(PeptideFilteringFormElements.orCriteriaForEachRun, _form.isOrCriteriaForEachRun());
                setupURL.addParameter(PEPTIDES_FILTER_VIEW_NAME, _form.getPeptideCustomViewName(getViewContext()));
                setupURL.addParameter(NORMALIZE_PROTEIN_GROUPS_NAME, _form.isNormalizeProteinGroups());
                setupURL.addParameter(PIVOT_TYPE_NAME, _form.getPivotTypeEnum().toString());
                root.addChild("Setup Compare ProteinProphet", setupURL);
            }
            setHelpTopic("compareProteinProphet");
            return root.addChild("Compare ProteinProphet");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ComparePeptideQueryAction extends RunListHandlerAction<PeptideFilteringComparisonForm, ComparisonCrosstabView>
    {
        private PeptideFilteringComparisonForm _form;

        @Override
        public void validate(PeptideFilteringComparisonForm form, BindException errors)
        {
            super.validate(form, errors);
            PeptideFilteringComparisonForm.validateTargetProtein(form, getViewContext());
        }

        public ComparePeptideQueryAction()
        {
            super(PeptideFilteringComparisonForm.class);
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors) throws Exception
        {
            _form = form;
            return super.getView(form, errors);
        }

        protected ModelAndView getHtmlView(PeptideFilteringComparisonForm form, BindException errors) throws Exception
        {
            if (null != form.getTargetProteinMsg())
            {
                //Todo: are these addParams needed?
                ActionURL setupURL= getViewContext().getActionURL().clone();
                setupURL.setAction(MS2Controller.ComparePeptideQuerySetupAction.class);

                setupURL.deleteParameter(PeptideFilteringFormElements.targetProteinMsg.name());
                setupURL.deleteParameter(PeptideFilteringFormElements.matchingProtNames.name());
                setupURL.deleteParameter(PeptideFilteringFormElements.matchingSeqIds.name());

                setupURL.addParameter(PeptideFilteringFormElements.targetProteinMsg.name(),form.getTargetProteinMsg());
                setupURL.addParameter(PeptideFilteringFormElements.matchingProtNames.name(),form.getMatchingProtNames());
                setupURL.addParameter(PeptideFilteringFormElements.matchingSeqIds.name(),form.getMatchingSeqIds());
                return HttpView.redirect(setupURL);
            }
            ComparisonCrosstabView view = createInitializedQueryView(form, errors, false, null);

            Map<String, String> prefs = getPreferences(ComparePeptideQuerySetupAction.class);
            prefs.put(PeptideFilteringFormElements.peptideFilterType.name(), form.getPeptideFilterType());
            prefs.put(PEPTIDES_FILTER_VIEW_NAME, form.getPeptideCustomViewName(getViewContext()));
            prefs.put(PeptideFilteringFormElements.peptideProphetProbability.name(), form.getPeptideProphetProbability() == null ? null : form.getPeptideProphetProbability().toString());
            prefs.put(PeptideFilteringFormElements.targetProtein.name(), form.getTargetProtein() == null ? null : form.getTargetProtein());

            if (!getUser().isGuest())
            {
                // Non-guests are stored in the database, guests get it stored in their session
                PropertyManager.saveProperties(prefs);
            }

            Map<String, String> props = new HashMap<String, String>();
   /*
            //CONSIDER:  can we support charge state filters?
            ActionURL nextURL = getViewContext().getActionURL();
            Map<String, String> dynamicViewMap = getViewMap(true,true);
            if (dynamicViewMap.keySet().contains(form.getPeptideCustomViewName(getViewContext())))
            {
                 nextURL.setRawQuery(nextURL.getRawQuery() + '&' + dynamicViewMap.get(form.getPeptideCustomViewName(getViewContext())));
            }
              props.put("originalURL", nextURL.toString());
    */
            VBox result = new VBox();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put(PEPTIDES_FILTER_VIEW_NAME, getViewContext().getActionURL().getParameter(PEPTIDES_FILTER_VIEW_NAME));
            props.put("comparisonName", "PeptideCrosstab");
            ActionURL url = getViewContext().getActionURL();
            if (null == url.getParameter("targetProtein") || url.getParameter("targetProtein").length()==0)
            {
                GWTView gwtView = new GWTView(org.labkey.ms2.client.MS2VennDiagramView.class, props);
                gwtView.setTitle("Comparison Overview");
                gwtView.setFrame(WebPartView.FrameType.PORTAL);
                gwtView.enableExpandCollapse("PeptideQueryCompare", true);
                result.addView(gwtView);
            } 
            view.setTitle("Comparison Details");
            view.setFrame(WebPartView.FrameType.PORTAL);

            result.addView(view);
            return result;
        }

        protected ComparisonCrosstabView createQueryView(PeptideFilteringComparisonForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList(), false, getViewContext());
            schema.setRuns(runs);
            return new PeptideCrosstabView(schema, form, getViewContext().getActionURL());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (_form != null)
            {
                ActionURL setupURL = new ActionURL(ComparePeptideQuerySetupAction.class, getContainer());
                setupURL.addParameter(PeptideFilteringFormElements.peptideFilterType, _form.getPeptideFilterType());
                if (_form.getPeptideProphetProbability() != null)
                {
                    setupURL.addParameter(PeptideFilteringFormElements.peptideProphetProbability, _form.getPeptideProphetProbability().toString());
                }

                setupURL.addParameter(PeptideFilteringFormElements.runList, _form.getRunList() == null ? -1 : _form.getRunList());
                setupURL.addParameter(PEPTIDES_FILTER_VIEW_NAME, _form.getPeptideCustomViewName(getViewContext()));
                root.addChild("Setup Compare Peptides", setupURL);
                StringBuilder title = new StringBuilder("Compare Peptides: ");
                _form.appendPeptideFilterDescription(title, getViewContext());
                return root.addChild(title.toString());
            }
            return root.addChild("Compare Peptides");
        }
    }

    // extraFormHtml gets inserted between the view dropdown and the button.
    private HttpView pickView(ActionURL nextURL, String viewInstructions, HttpView embeddedView, int runListId, String buttonText)
    {
        JspView<PickViewBean> pickView = new JspView<PickViewBean>("/org/labkey/ms2/pickView.jsp", new PickViewBean());

        PickViewBean bean = pickView.getModelBean();

        nextURL.deleteFilterParameters("button");
        nextURL.deleteFilterParameters("button.x");
        nextURL.deleteFilterParameters("button.y");

        bean.nextURL = nextURL;
        bean.select = renderViewSelect(false);
        bean.extraOptionsView = embeddedView;
        bean.viewInstructions = viewInstructions;
        bean.runList = runListId;
        bean.buttonText = buttonText;

        return pickView;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PickExportRunsView extends AbstractRunListCreationAction<RunListForm>
    {
        public PickExportRunsView()
        {
            super(RunListForm.class, true);
        }

        public ModelAndView getView(RunListForm form, BindException errors, int runListId)
        {
            JspView extraExportView = new JspView("/org/labkey/ms2/extraExportOptions.jsp");
            return pickView(getViewContext().cloneActionURL().setAction(ApplyExportRunsViewAction.class), "Select a view to apply a filter to all the runs and to indicate what columns to export.", extraExportView, runListId, "Export");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Export Runs", getPageConfig(), "exportRuns");
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportRunsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            List<MS2Run> runs;
            try
            {
                runs = RunListCache.getCachedRuns(form.getRunList(), true, getViewContext());
            }
            catch (RunListException e)
            {
                errors.addError(new LabkeyError(e));
                SimpleErrorView view = new SimpleErrorView(errors);
                renderInTemplate(getViewContext(), this, getPageConfig(), view);
                return;
            }

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), runs.toArray(new MS2Run[runs.size()]));
            ActionURL currentURL = getViewContext().cloneActionURL();
            SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentURL, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, getViewContext().getUser());

            if (form.getExportFormat() != null && form.getExportFormat().startsWith("Excel"))
            {
                peptideView.exportToExcel(form, response, null);
                return;
            }

            if ("TSV".equals(form.getExportFormat()))
            {
                peptideView.exportToTSV(form, response, null, null);
                return;
            }

            if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
            {
                if (peptideView instanceof FlatPeptideView)
                    exportSpectra(runs, currentURL, peptideFilter, form.getExportFormat().toLowerCase());
                else
                    exportProteinsAsSpectra(runs, currentURL, form.getExportFormat().toLowerCase(), peptideView, null, getViewContext().getUser());
                return;
            }

            if ("AMT".equals(form.getExportFormat()))
            {
                peptideView.exportToAMT(form, response, null);
                return;
            }

            throw new IllegalArgumentException("Unsupported export type: " + form.getExportFormat());
        }
    }


    private void exportSpectra(List<MS2Run> runs, ActionURL currentURL, SimpleFilter filter, String extension) throws IOException
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.addURLSort(currentURL, MS2Manager.getDataRegionNamePeptides());
        SpectrumIterator iter = new ResultSetSpectrumIterator(runs, filter, sort);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowCompareAction extends SimpleViewAction<ExportForm>
    {
        private StringBuilder _title = new StringBuilder();

        public ModelAndView getView(ExportForm form, BindException errors) throws Exception
        {
            return compareRuns(form.getRunList(), false, _title, form.getColumn(), errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, _title.toString(), getPageConfig(), "compareRuns");
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportCompareToExcel extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            ModelAndView view = compareRuns(form.getRunList(), true, null, form.getColumn(), errors);
            if (view != null)
            {
                throw new ExportException(view);
            }
        }
    }

    public static class RunListForm extends QueryViewAction.QueryExportForm
    {
        private Integer _runList;
        private boolean _experimentRunIds;

        public Integer getRunList()
        {
            return _runList;
        }

        public void setRunList(Integer runList)
        {
            _runList = runList;
        }

        public boolean isExperimentRunIds()
        {
            return _experimentRunIds;
        }

        public void setExperimentRunIds(boolean experimentRunIds)
        {
            _experimentRunIds = experimentRunIds;
        }
    }

    public static class SpectraCountForm extends PeptideFilteringComparisonForm
    {
        private String _spectraConfig;

        public String getSpectraConfig()
        {
            return _spectraConfig;
        }

        public void setSpectraConfig(String spectraConfig)
        {
            _spectraConfig = spectraConfig;
        }
    }

    public static final String PEPTIDES_FILTER = "PeptidesFilter";
    public static final String PEPTIDES_FILTER_VIEW_NAME = PEPTIDES_FILTER + "." + QueryParam.viewName.toString();
    public static final String PROTEIN_GROUPS_FILTER = "ProteinGroupsFilter";
    public static final String PROTEIN_GROUPS_FILTER_VIEW_NAME = PROTEIN_GROUPS_FILTER + "." + QueryParam.viewName.toString();
    public static final String NORMALIZE_PROTEIN_GROUPS_NAME = "normalizeProteinGroups";
    public static final String PIVOT_TYPE_NAME = "pivotType";

    @RequiresPermissionClass(ReadPermission.class)
    public abstract class AbstractRunListCreationAction<FormType extends RunListForm> extends SimpleViewAction<FormType>
    {
        private final boolean _requiresSameType;

        protected AbstractRunListCreationAction(Class<FormType> formClass, boolean requiresSameType)
        {
            super(formClass);
            _requiresSameType = requiresSameType;
        }

        public final ModelAndView getView(FormType form, BindException errors) throws ServletException
        {
            ActionURL currentURL = getViewContext().getActionURL();
            int runListId;
            try
            {
                if (form.getRunList() == null)
                {
                    runListId = RunListCache.cacheSelectedRuns(_requiresSameType, form, getViewContext());
                    ActionURL redirectURL = currentURL.clone();
                    redirectURL.addParameter("runList", Integer.toString(runListId));
                    throw new RedirectException(redirectURL);
                }
                else
                {
                    runListId = form.getRunList().intValue();
                    RunListCache.getCachedRuns(runListId, false, getViewContext());
                }

                return getView(form, errors, runListId);
            }
            catch (RunListException e)
            {
                e.addErrors(errors);
                return new SimpleErrorView(errors);
            }
        }

        protected abstract ModelAndView getView(FormType form, BindException errors, int runListId);
    }

    private Map<String, String> getPreferences(Class<? extends AbstractRunListCreationAction> setupActionClass)
    {
        if (getUser().isGuest())
        {
            String attributeKey = setupActionClass.getName() + "." + getContainer().getId();
            Map<String, String> prefs = (Map<String, String>)getViewContext().getSession().getAttribute(attributeKey);
            if (prefs == null)
            {
                prefs = new HashMap<String, String>();
                getViewContext().getSession().setAttribute(attributeKey, prefs);
            }
            return prefs;
        }
        else
        {
            return PropertyManager.getWritableProperties(getUser().getUserId(), getContainer().getId(), setupActionClass.getName(), true);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SpectraCountSetupAction extends AbstractRunListCreationAction<SpectraCountForm>
    {
        public SpectraCountSetupAction()
        {
            super(SpectraCountForm.class, false);
        }

        protected SpectraCountForm getCommand(HttpServletRequest request) throws Exception
        {
            SpectraCountForm form = super.getCommand(request);
            Map<String, String> prefs = getPreferences(SpectraCountSetupAction.class);
            form.setPeptideFilterType(prefs.get(PeptideFilteringFormElements.peptideFilterType.name()) == null ? "none" : prefs.get(PeptideFilteringFormElements.peptideFilterType.name()));
            form.setSpectraConfig(prefs.get(PeptideFilteringFormElements.spectraConfig.name()));
            form.setDefaultPeptideCustomView(prefs.get(PEPTIDES_FILTER_VIEW_NAME));
            if (prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name()) != null)
            {
                try
                {
                    form.setPeptideProphetProbability(new Float(prefs.get(PeptideFilteringFormElements.peptideProphetProbability.name())));
                }
                catch (NumberFormatException ignored) {}
            }
            return form;
        }

        public ModelAndView getView(SpectraCountForm form, BindException errors, int runListId)
        {
            CompareOptionsBean<SpectraCountForm> bean = new CompareOptionsBean<SpectraCountForm>(new ActionURL(SpectraCountAction.class, getContainer()), runListId, form);

            return new JspView<CompareOptionsBean>("/org/labkey/ms2/compare/spectraCountOptions.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Spectra Count Options");
        }
    }

    public abstract class RunListHandlerAction<FormType extends RunListForm, ViewType extends QueryView> extends QueryViewAction<FormType, ViewType>
    {
        protected List<MS2Run> _runs;

        protected RunListHandlerAction(Class<FormType> formClass)
        {
            super(formClass);
        }

        public ModelAndView getView(FormType form, BindException errors) throws Exception
        {
            if (form.getRunList() == null)
            {
                errors.addError(new LabkeyError("Could not find the list of selected runs for comparison. Please reselect the runs."));
                return new SimpleErrorView(errors);
            }
            try
            {
                _runs = RunListCache.getCachedRuns(form.getRunList(), false, getViewContext());
            }
            catch (RunListException e)
            {
                e.addErrors(errors);
                return new SimpleErrorView(errors);
            }
            return super.getView(form, errors);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SpectraCountAction extends RunListHandlerAction<SpectraCountForm, QueryView>
    {
        private SpectraCountConfiguration _config;
        private SpectraCountForm _form;

        public SpectraCountAction()
        {
            super(SpectraCountForm.class);
        }

        protected QueryView createQueryView(SpectraCountForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            _form = form;
            _config = SpectraCountConfiguration.findByTableName(form.getSpectraConfig());
            if (_config == null)
            {
                throw new NotFoundException("Could not find spectra count config: " + form.getSpectraConfig());
            }

            Map<String, String> prefs = getPreferences(SpectraCountSetupAction.class);
            prefs.put(PeptideFilteringFormElements.peptideFilterType.name(), form.getPeptideFilterType());
            prefs.put(PeptideFilteringFormElements.spectraConfig.name(), form.getSpectraConfig());
            prefs.put(PEPTIDES_FILTER_VIEW_NAME, form.getPeptideCustomViewName(getViewContext()));
            prefs.put(PeptideFilteringFormElements.peptideProphetProbability.name(), form.getPeptideProphetProbability() == null ? null : form.getPeptideProphetProbability().toString());
            if (!getUser().isGuest())
            {
                // Real users have their preferences stored in the database, guests keep it in session
                PropertyManager.saveProperties(prefs);
            }

            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            schema.setRuns(_runs);

            QuerySettings settings = schema.getSettings(getViewContext(), "SpectraCount", _config.getTableName());
            settings.setAllowChooseQuery(false);
            QueryView view = new SpectraCountQueryView(schema, settings, _config, _form);
            // ExcelWebQueries won't be part of the same HTTP session so we won't have access to the run list anymore
            view.setAllowExportExternalQuery(false);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root = appendRootNavTrail(root, null, getPageConfig(), "spectraCount");
            if (_form != null)
            {
                ActionURL setupURL = new ActionURL(SpectraCountSetupAction.class, getContainer());
                setupURL.addParameter(PeptideFilteringFormElements.peptideFilterType, _form.getPeptideFilterType());
                if (_form.getPeptideProphetProbability() != null)
                {
                    setupURL.addParameter(PeptideFilteringFormElements.peptideProphetProbability, _form.getPeptideProphetProbability().toString());
                }
                setupURL.addParameter(PEPTIDES_FILTER_VIEW_NAME, _form.getPeptideCustomViewName(getViewContext()));
                setupURL.addParameter(PeptideFilteringFormElements.runList, _form.getRunList());
                setupURL.addParameter(PeptideFilteringFormElements.spectraConfig, _form.getSpectraConfig());

                root.addChild("Spectra Count Options", setupURL);
                StringBuilder title = new StringBuilder("Spectra Counts: ");
                title.append(_config.getDescription());
                title.append(", ");
                _form.appendPeptideFilterDescription(title, getViewContext());
                root.addChild(title.toString());
            }
            return root;
        }
    }

    private ModelAndView compareRuns(int runListIndex, boolean exportToExcel, StringBuilder title, String column, BindException errors) throws ServletException, SQLException
    {
        ActionURL currentURL = getViewContext().getActionURL();

        List<MS2Run> runs;
        try
        {
            runs = RunListCache.getCachedRuns(runListIndex, false, getViewContext());
        }
        catch (RunListException e)
        {
            e.addErrors(errors);
            return new SimpleErrorView(errors);
        }

        for (MS2Run run : runs)
        {
            Container c = run.getContainer();
            if (c == null || !c.hasPermission(getUser(), ReadPermission.class))
            {
                throw new UnauthorizedException();
            }
        }

        CompareQuery query = CompareQuery.getCompareQuery(column, currentURL, runs, getViewContext().getUser());
        if (query == null)
        {
            errors.addError(new LabkeyError("You must specify a comparison type"));
            return new SimpleErrorView(errors);
        }

        query.checkForErrors(errors);

        if (errors.getErrorCount() > 0)
        {
            return new SimpleErrorView(errors);
        }

        List<RunColumn> gridColumns = query.getGridColumns();
        CompareDataRegion rgn = query.getCompareGrid(exportToExcel);

        List<String> runCaptions = new ArrayList<String>(runs.size());
        for (MS2Run run : runs)
            runCaptions.add(run.getDescription());

        int offset = 1;

        if (exportToExcel)
        {
            ResultSet rs = rgn.getResultSet();
            CompareExcelWriter ew = new CompareExcelWriter(new ResultsImpl(rs), rgn.getDisplayColumns());
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
            for (Pair<String, String> sqlSummary : query.getSQLSummaries(getViewContext().getUser()))
            {
                headers.add(sqlSummary.getKey() + ": " + sqlSummary.getValue());
            }
            headers.add("");
            ew.setHeaders(headers);
            ew.write(getViewContext().getResponse());
        }
        else
        {
            rgn.setOffset(offset);
            rgn.setColSpan(query.getColumnsPerRun());
            rgn.setMultiColumnCaptions(runCaptions);

            HttpView filterView = new CurrentFilterView(query, getViewContext().getUser());

            GridView compareView = new GridView(rgn, errors);
            rgn.setShowPagination(false);
            compareView.setResultSet(rgn.getResultSet());
            compareView.getDataRegion().setButtonBarPosition(DataRegion.ButtonBarPosition.BOTH);

            title.append(query.getComparisonDescription());

            return new VBox(filterView, compareView);
        }

        return null;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class CompareServiceAction extends GWTServiceAction
    {
        protected BaseRemoteService createService()
        {
            return new CompareServiceImpl(getViewContext());
        }
    }


    @RequiresLogin
    public class ExportHistoryAction extends ExportAction
    {
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            TableInfo tinfo = MS2Manager.getTableInfoHistory();
            ExcelWriter ew = new ExcelWriter(MS2Manager.getSchema(), "SELECT * FROM " + MS2Manager.getTableInfoHistory() + " ORDER BY Date");
            ew.setColumns(tinfo.getColumns());
            ew.setSheetName("MS2 History");
            ew.write(response);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

            if (null != peptide)
            {
                validateRun(peptide.getRun());

                response.setDateHeader("Expires", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
                response.setHeader("Pragma", "");
                response.setContentType("image/png");
                peptide.renderGraph(response, form.getTolerance(), form.getxStartDouble(), form.getxEnd(), form.getWidth(), form.getHeight());
            }
            else
            {
                throw new NotFoundException();
            }
        }
    }


    @RequiresSiteAdmin
    public class ReloadFastaAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            int[] ids = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));

            FastaDbLoader.updateSeqIds(ids);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    @RequiresSiteAdmin
    public class DeleteDataBasesAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            Set<String> fastaIds = DataRegionSelection.getSelected(getViewContext(), true);
            String idList = StringUtils.join(fastaIds, ',');
            Integer[] validIds = Table.executeArray(ProteinManager.getSchema(), "SELECT FastaId FROM " + ProteinManager.getTableInfoFastaAdmin() + " WHERE (FastaId <> 0) AND (Runs IS NULL) AND (FastaId IN (" + idList + "))", new Object[]{}, Integer.class);

            for (int id : validIds)
                ProteinManager.deleteFastaFile(id);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    public static class FastaParsingForm
    {
        private String _header;

        public String getHeader()
        {
            if (_header != null && _header.length() > 0 && _header.startsWith(">"))
            {
                return _header.substring(1);
            }
            return _header;
        }

        public void setHeader(String headers)
        {
            _header = headers;
        }
    }

    @RequiresSiteAdmin
    public class TestFastaParsingAction extends SimpleViewAction<FastaParsingForm>
    {
        public ModelAndView getView(FastaParsingForm form, BindException errors) throws Exception
        {
            return new JspView<FastaParsingForm>("/org/labkey/ms2/testFastaParsing.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendProteinAdminNavTrail(root, "Test FASTA header parsing", getPageConfig(), null);
            return root;
        }
    }

    public static class BlastForm
    {
        private String _blastServerBaseURL;

        public String getBlastServerBaseURL()
        {
            return _blastServerBaseURL;
        }

        public void setBlastServerBaseURL(String blastServerBaseURL)
        {
            _blastServerBaseURL = blastServerBaseURL;
        }
    }

    @RequiresSiteAdmin
    public class ShowProteinAdminAction extends FormViewAction<BlastForm>
    {
        @Override
        public ModelAndView getView(BlastForm form, boolean reshow, BindException errors) throws Exception
        {
            JspView<String> blastView = new JspView<String>("/org/labkey/ms2/blastAdmin.jsp", AppProps.getInstance().getBLASTServerBaseURL(), errors);
            blastView.setTitle("BLAST Configuration");

            GridView grid = getFastaAdminGrid();
            grid.setTitle("FASTA Files");
            GridView annots = new GridView(getAnnotInsertsGrid(), errors);
            annots.setTitle("Protein Annotations Loaded");

            return new VBox(blastView, grid, annots);
        }

        @Override
        public URLHelper getSuccessURL(BlastForm o)
        {
            return new ActionURL(ShowProteinAdminAction.class, ContainerManager.getRoot());
        }

        @Override
        public void validateCommand(BlastForm target, Errors errors) {}

        @Override
        public boolean handlePost(BlastForm o, BindException errors) throws Exception
        {
            WriteableAppProps props = AppProps.getWriteableInstance();
            props.setBLASTServerBaseURL(o.getBlastServerBaseURL());
            props.save();
            return true;
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
            ActionURL showURL = new ActionURL(ShowAnnotInsertDetailsAction.class, getContainer());
            String detailURL = showURL.getLocalURIString() + "insertId=${InsertId}";
            rgn.getDisplayColumn("insertId").setURL(detailURL);
            rgn.addDisplayColumn(threadControl1);
            rgn.setMaxRows(MAX_INSERTIONS_DISPLAY_ROWS);
            rgn.setShowRecordSelectors(true);

            ButtonBar bb = new ButtonBar();

            ActionButton delete = new ActionButton(DeleteAnnotInsertEntriesAction.class, "Delete");
            delete.setRequiresSelection(true, "Are you sure you want to remove this entry from the list?\\n(Note: actual annotations won't be deleted.)", "Are you sure you want to remove these entries from the list?\\n(Note: actual annotations won't be deleted.)"
            );
            delete.setActionType(ActionButton.Action.GET);
            bb.add(delete);

            ActionButton insertAnnots = new ActionButton(new ActionURL(InsertAnnotsAction.class, getContainer()), "Load New Annot File");
            insertAnnots.setActionType(ActionButton.Action.LINK);
            bb.add(insertAnnots);

            ActionButton testFastaHeader = new ActionButton(new ActionURL(TestFastaParsingAction.class, getContainer()), "Test FASTA Header Parsing");
            testFastaHeader.setActionType(ActionButton.Action.LINK);
            bb.add(testFastaHeader);

            bb.add(new ActionButton(ReloadSPOMAction.class, "Reload SWP Org Map"));

            ActionButton reloadGO = new ActionButton(LoadGoAction.class, (GoLoader.isGoLoaded().booleanValue() ? "Reload" : "Load") + " GO");
            reloadGO.setActionType(ActionButton.Action.LINK);
            bb.add(reloadGO);

            rgn.setButtonBar(bb);
            return rgn;
        }

        private GridView getFastaAdminGrid()
        {
            DataRegion rgn = new DataRegion();
            rgn.setColumns(ProteinManager.getTableInfoFastaAdmin().getColumns("FileName, Loaded, FastaId, Runs"));
            String runsURL = new ActionURL(ShowAllRunsAction.class, ContainerManager.getRoot()) + "?" + MS2Manager.getDataRegionNameRuns() + ".FastaId~eq=${FastaId}";
            rgn.getDisplayColumn("Runs").setURL(runsURL);
            rgn.setFixedWidthColumns(false);
            rgn.setShowRecordSelectors(true);

            GridView result = new GridView(rgn, (BindException)null);
            result.getRenderContext().setBaseSort(new Sort("FastaId"));

            ButtonBar bb = new ButtonBar();

            ActionButton delete = new ActionButton(new ActionURL(DeleteDataBasesAction.class, getContainer()), "Delete");
            delete.setActionType(ActionButton.Action.POST);
            delete.setRequiresSelection(true);
            bb.add(delete);

            ActionButton reload = new ActionButton(ReloadFastaAction.class, "Reload FASTA");
            reload.setActionType(ActionButton.Action.POST);
            reload.setRequiresSelection(true);
            bb.add(reload);

            ActionButton testFastaHeader = new ActionButton(new ActionURL(TestFastaParsingAction.class, getContainer()), "Test FASTA Header Parsing");
            testFastaHeader.setActionType(ActionButton.Action.LINK);
            bb.add(testFastaHeader);

            MenuButton setBestNameMenu = new MenuButton("Set Protein Best Name...");
            ActionURL setBestNameURL = new ActionURL(SetBestNameAction.class, getContainer());

            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.LOOKUP_STRING.toString());
            setBestNameMenu.addMenuItem("to name from FASTA", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.IPI.toString());
            setBestNameMenu.addMenuItem("to IPI (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT.toString());
            setBestNameMenu.addMenuItem("to Swiss-Prot Name (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT_ACCN.toString());
            setBestNameMenu.addMenuItem("to Swiss-Prot Accession (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.GEN_INFO.toString());
            setBestNameMenu.addMenuItem("to GI number (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));

            bb.add(setBestNameMenu);

            rgn.setButtonBar(bb, DataRegion.MODE_GRID);
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            PageFlowUtil.urlProvider(AdminUrls.class).appendAdminNavTrail(root, "Protein Database Admin", null);
            return root;
        }
    }

    public static class SetBestNameForm
    {
        public enum NameType
        { LOOKUP_STRING, IPI, SWISS_PROT, SWISS_PROT_ACCN, GEN_INFO }

        private String _nameType;

        public String getNameType()
        {
            return _nameType;
        }

        public NameType lookupNameType()
        {
            return NameType.valueOf(getNameType());
        }

        public void setNameType(String nameType)
        {
            _nameType = nameType;
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetBestNameAction extends FormHandlerAction<SetBestNameForm>
    {
        public void validateCommand(SetBestNameForm form, Errors errors)
        {
        }

        public boolean handlePost(SetBestNameForm form, BindException errors) throws Exception
        {
            int[] fastaIds = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));
            SetBestNameRunnable runnable = new SetBestNameRunnable(fastaIds, form.lookupNameType());
            JobRunner.getDefault().execute(runnable);
            return true;
        }

        public ActionURL getSuccessURL(SetBestNameForm form)
        {
            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportSelectedProteinGroupsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            ViewContext ctx = getViewContext();
            List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

            exportProteinGroups(response, form, proteins);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportProteinGroupsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);
            exportProteinGroups(response, form, null);
        }
    }


    private void exportProteinGroups(HttpServletResponse response, ExportForm form, List<String> proteins) throws Exception
    {
        MS2Run run = validateRun(form);
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        String where = null;
        if (proteins != null && !proteins.isEmpty())
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
            peptideView.exportToExcel(form, response, proteins);
//            exportProteinGroupsToExcel(getActionURL(), form, what, where);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, response, proteins, null);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, response, proteins);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewContext().getActionURL(), form.getExportFormat().toLowerCase(), peptideView, where, getViewContext().getUser());
        }
    }

    private void exportProteinsAsSpectra(List<MS2Run> runs, ActionURL currentURL, String extension, AbstractMS2RunView peptideView, String where, User user) throws IOException
    {
        SpectrumIterator iter = null;
        try
        {
            iter = new ProteinResultSetSpectrumIterator(runs, currentURL, peptideView, where, user);
            SpectrumRenderer sr;

            if ("pkl".equals(extension))
                sr = new PklSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);
            else
                sr = new DtaSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);

            sr.render(iter);
            sr.close();
        }
        finally
        {
            if (iter != null)
            {
                iter.close();
            }
        }

    }


    private void exportProteins(ExportForm form, HttpServletResponse response, String extraWhere, List<String> proteins) throws Exception
    {
        MS2Run run = validateRun(form);
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        if ("Excel".equals(form.getExportFormat()))
        {
            peptideView.exportToExcel(form, response, proteins);
        }
        else if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, response, proteins, null);
        }
        else if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, response, proteins);
        }
        else if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
        {
            exportProteinsAsSpectra(Arrays.asList(run), getViewContext().getActionURL(), form.getExportFormat().toLowerCase(), peptideView, extraWhere, getViewContext().getUser());
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportAllProteinsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            exportProteins(form, response, null, null);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportSelectedProteinsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            ViewContext ctx = getViewContext();
            List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

            if (null != proteins)
            {
                StringBuffer where = new StringBuffer("Protein IN (");

                for (int i = 0; i < Math.min(proteins.size(), ExcelWriter.MAX_ROWS); i++)
                {
                    // Escape all single quotes in the protein names
                    // TODO: Use IN FilterClause
                    String protein = proteins.get(i).replaceAll("'", "\\\\'");

                    if (i > 0)
                        where.append(",");

                    where.append('\'');
                    where.append(protein);
                    where.append('\'');
                }

                where.append(")");

                exportProteins(form, response, where.toString(), proteins);
            }
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class DoProteinSearchAction extends QueryViewAction<ProteinSearchForm, QueryView>
    {
        private static final String PROTEIN_DATA_REGION = "ProteinSearchResults";
        private static final String POTENTIAL_PROTEIN_DATA_REGION = "PotentialProteins";

        public DoProteinSearchAction()
        {
            super(ProteinSearchForm.class);
        }

        protected QueryView createQueryView(ProteinSearchForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            if (PROTEIN_DATA_REGION.equalsIgnoreCase(dataRegion))
            {
                return createProteinGroupSearchView(form, errors);
            }
            else if (POTENTIAL_PROTEIN_DATA_REGION.equalsIgnoreCase(dataRegion))
            {
                return createProteinSearchView(form, errors);
            }

            throw new IllegalArgumentException("Unsupported dataRegion name" + dataRegion);
        }

        private QueryView createProteinSearchView(ProteinSearchForm form, BindException errors)
            throws ServletException
        {
            UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);
            QuerySettings proteinsSettings = schema.getSettings(getViewContext(), POTENTIAL_PROTEIN_DATA_REGION);
            proteinsSettings.setQueryName(MS2Schema.TableType.Sequences.toString());
            proteinsSettings.setAllowChooseQuery(false);
            QueryView proteinsView = new QueryView(schema, proteinsSettings, errors);
            // Disable R and other reporting until there's an implementation that respects the search criteria
            proteinsView.setViewItemFilter(ReportService.EMPTY_ITEM_LIST);

            proteinsView.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
            SequencesTableInfo sequencesTableInfo = (SequencesTableInfo)proteinsView.getTable();
            Integer[] seqIds = getSeqIds(form);
            if (seqIds.length <= 500)
            {
                sequencesTableInfo.addSeqIdFilter(seqIds);
            }
            else
            {
                sequencesTableInfo.addProteinNameFilter(form.getIdentifier(), form.isExactMatch());
                if (form.isRestrictProteins())
                {
                    sequencesTableInfo.addContainerCondition(getContainer(), getUser(), true);
                }
            }
            proteinsView.setTitle("Matching Proteins");
            return proteinsView;
        }

        private QueryView createProteinGroupSearchView(final ProteinSearchForm form, BindException errors) throws ServletException
        {
            UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);
            QuerySettings groupsSettings = schema.getSettings(getViewContext(), PROTEIN_DATA_REGION, MS2Schema.HiddenTableType.ProteinGroupsForSearch.toString());
            groupsSettings.setAllowChooseQuery(false);
            QueryView groupsView = new QueryView(schema, groupsSettings, errors)
            {
                protected TableInfo createTable()
                {
                    ProteinGroupTableInfo table = ((MS2Schema)getSchema()).createProteinGroupsForSearchTable();
                    table.addPeptideFilter(form, getViewContext());
                    Integer[] seqIds = getSeqIds(form);
                    if (seqIds.length <= 500)
                    {
                        table.addSeqIdFilter(seqIds);
                    }
                    else
                    {
                        table.addProteinNameFilter(form.getIdentifier(), form.isExactMatch());
                    }
                    table.addContainerCondition(getContainer(), getUser(), form.isIncludeSubfolders());

                    return table;
                }

            };
            groupsView.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
            // Disable R and other reporting until there's an implementation that respects the search criteria
            groupsView.setViewItemFilter(ReportService.EMPTY_ITEM_LIST);

            groupsView.setTitle("Protein Group Results");
            return groupsView;
        }

        private Integer[] getSeqIds(ProteinSearchForm form)
        {
            SequencesTableInfo tableInfo = new SequencesTableInfo(new MS2Schema(getUser(), getContainer()));
            tableInfo.addProteinNameFilter(form.getIdentifier(), form.isExactMatch());
            if (form.isRestrictProteins())
            {
                tableInfo.addContainerCondition(getContainer(), getUser(), true);
            }
            try
            {
                return Table.executeArray(tableInfo, tableInfo.getColumn("SeqId"), null, null, Integer.class);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        protected ModelAndView getHtmlView(ProteinSearchForm form, BindException errors) throws Exception
        {
            HttpServletRequest request = getViewContext().getRequest();
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
                ActionURL url = getViewContext().cloneActionURL();
                url.deleteParameter("minimumProbability");
                url.deleteParameter("maximumErrorRate");
                throw new RedirectException(url + "&" + filter.toQueryString("ProteinSearchResults"));
            }

            QueryView proteinsView = createInitializedQueryView(form, errors, false, POTENTIAL_PROTEIN_DATA_REGION);
            QueryView groupsView = createInitializedQueryView(form, errors, false, PROTEIN_DATA_REGION);

            ProteinSearchWebPart searchView = new ProteinSearchWebPart(true, form);
            if (getViewContext().getRequest().getParameter("ProteinSearchResults.GroupProbability~gte") != null)
            {
                try
                {
                    form.setMinimumProbability(Float.parseFloat(request.getParameter("ProteinSearchResults.GroupProbability~gte")));
                }
                catch (NumberFormatException ignored) {}
            }
            if (request.getParameter("ProteinSearchResults.ErrorRate~lte") != null)
            {
                try
                {
                    form.setMaximumErrorRate(Float.parseFloat(request.getParameter("ProteinSearchResults.ErrorRate~lte")));
                }
                catch (NumberFormatException ignored) {}
            }
            proteinsView.enableExpandCollapse("ProteinSearchProteinMatches", true);
            groupsView.enableExpandCollapse("ProteinSearchGroupMatches", false);

            return new VBox(searchView, proteinsView, groupsView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Protein Search Results", getPageConfig(), "proteinSearch");
        }
    }

    public static class ProteinSearchForm extends QueryViewAction.QueryExportForm
    {
        private String _identifier;
        private Float _minimumProbability;
        private Float _maximumErrorRate;
        private String _peptideFilterType = "none";
        private Float _peptideProphetProbability;
        private boolean _includeSubfolders;
        private boolean _exactMatch;
        private boolean _restrictProteins;
        private String _defaultCustomView;

        public String getDefaultCustomView()
        {
            return _defaultCustomView;
        }

        public void setDefaultCustomView(String defaultCustomView)
        {
            _defaultCustomView = defaultCustomView;
        }

        public boolean isExactMatch()
        {
            return _exactMatch;
        }

        public void setExactMatch(boolean exactMatch)
        {
            _exactMatch = exactMatch;
        }

        public String getPeptideFilterType()
        {
            return _peptideFilterType;
        }

        public void setPeptideFilterType(String peptideFilterType)
        {
            _peptideFilterType = peptideFilterType;
        }

        public Float getPeptideProphetProbability()
        {
            return _peptideProphetProbability;
        }

        public void setPeptideProphetProbability(Float peptideProphetProbability)
        {
            _peptideProphetProbability = peptideProphetProbability;
        }

        public boolean isNoPeptideFilter()
        {
            return !isCustomViewPeptideFilter() && !isPeptideProphetFilter();
        }

        public boolean isPeptideProphetFilter()
        {
            return ProphetFilterType.probability.toString().equals(getPeptideFilterType());
        }

        public boolean isCustomViewPeptideFilter()
        {
            return ProphetFilterType.customView.toString().equals(getPeptideFilterType());
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

        public boolean isRestrictProteins()
        {
            return _restrictProteins;
        }

        public void setRestrictProteins(boolean restrictProteins)
        {
            _restrictProteins = restrictProteins;
        }

        public String getCustomViewName(ViewContext context)
        {
            String result = context.getRequest().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME);
            if (result == null)
            {
                result = _defaultCustomView;
            }
            if ("".equals(result))
            {
                return null;
            }
            return result;
        }

        public static ProteinSearchForm createDefault()
        {
            ProteinSearchForm result = new ProteinSearchForm();
            result.setIncludeSubfolders(true);
            result.setRestrictProteins(true);
            result.setExactMatch(true);
            return result;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ExportAllPeptidesAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            exportPeptides(form, response, false);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ExportSelectedPeptidesAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            exportPeptides(form, response, true);
        }
    }


    private void exportPeptides(ExportForm form, HttpServletResponse response, boolean selected) throws Exception
    {
        MS2Run run = validateRun(form);

        ActionURL currentURL = getViewContext().getActionURL();
        AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);

        // Need to create a filter for 1) extra filter and 2) selected peptides
        // URL filter is applied automatically (except for DTA/PKL)
        SimpleFilter baseFilter = ProteinManager.getPeptideFilter(currentURL, ProteinManager.EXTRA_FILTER, getViewContext().getUser(), run);

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

        if ("Excel".equals(form.getExportFormat()))
        {
            peptideView.exportToExcel(form, response, exportRows);
            return;
        }

        if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, response, exportRows, null);
            return;
        }

        if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, response, exportRows);
            return;
        }

        // Add URL filter manually
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentURL, ProteinManager.URL_FILTER, getViewContext().getUser(), run));

        if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
            exportSpectra(Arrays.asList(run), currentURL, baseFilter, form.getExportFormat().toLowerCase());
    }

    public static class ExportForm extends RunForm
    {
        private String _column;
        private String _exportFormat;
        private int _runList;

        public String getExportFormat()
        {
            return _exportFormat;
        }

        public void setExportFormat(String exportFormat)
        {
            _exportFormat = exportFormat;
        }

        public int getRunList()
        {
            return _runList;
        }

        public void setRunList(int runList)
        {
            _runList = runList;
        }

        public String getColumn()
        {
            return _column;
        }

        public void setColumn(String column)
        {
            _column = column;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideProphetDistributionPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            if (form.charge < 1 || form.charge > 3)
            {
                throw new NotFoundException("Unable to chart charge state " + form.charge);
            }
            PeptideProphetGraphs.renderDistribution(response, summary, form.charge, form.cumulative);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideProphetObservedVsModelPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);
            if (form.charge < 1 || form.charge > 3)
            {
                throw new NotFoundException("Unable to chart charge state " + form.charge);
            }
            PeptideProphetGraphs.renderObservedVsModel(response, summary, form.charge, form.cumulative);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideProphetObservedVsPPScorePlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            PeptideProphetGraphs.renderObservedVsPPScore(response, getContainer(), form.run, form.charge, form.cumulative);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideProphetSensitivityPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            PeptideProphetGraphs.renderSensitivityGraph(response, summary);
        }
    }


    public static class PeptideProphetForm extends RunForm
    {
        private int charge;
        private boolean cumulative = false;

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


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideProphetDetailsAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            MS2Run run = validateRun(form);

            String title = "Peptide Prophet Details";
            setTitle(title);
            getPageConfig().setTemplate(PageConfig.Template.Print);

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            JspView<PeptideProphetDetailsBean> result = new JspView<PeptideProphetDetailsBean>("/org/labkey/ms2/showPeptideProphetDetails.jsp", new PeptideProphetDetailsBean(run, summary, ShowPeptideProphetSensitivityPlotAction.class, title));
            result.setFrame(WebPartView.FrameType.PORTAL);
            result.setTitle("PeptideProphet Details: " + run.getDescription());
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    public static class PeptideProphetDetailsBean
    {
        public MS2Run run;
        public SensitivitySummary summary;
        public Class<? extends Controller> action;
        public String title;

        public PeptideProphetDetailsBean(MS2Run run, SensitivitySummary summary, Class<? extends Controller> action, String title)
        {
            this.run = run;
            this.summary = summary;
            this.action = action;
            this.title = title + " " + run.getDescription();
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinProphetSensitivityPlotAction extends ExportAction<RunForm>
    {
        public void export(RunForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            validateRun(form);

            ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);

            PeptideProphetGraphs.renderSensitivityGraph(response, summary);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinProphetDetailsAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            MS2Run run = validateRun(form);

            String title = "Protein Prophet Details";
            setTitle(title);
            getPageConfig().setTemplate(PageConfig.Template.Print);

            ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);
            JspView<PeptideProphetDetailsBean> result = new JspView<PeptideProphetDetailsBean>("/org/labkey/ms2/showSensitivityDetails.jsp", new PeptideProphetDetailsBean(run, summary, ShowProteinProphetSensitivityPlotAction.class, title));
            result.setFrame(WebPartView.FrameType.PORTAL);
            result.setTitle(title  + run.getDescription());
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresSiteAdmin
    public class PurgeRunsAction extends RedirectAction
    {
        private int _days;

        public ActionURL getSuccessURL(Object o)
        {
            return getShowMS2AdminURL(_days);
        }

        public boolean doAction(Object o, BindException errors) throws Exception
        {
            _days = getDays();

            MS2Manager.purgeDeleted(_days);

            return true;
        }

        public void validateCommand(Object target, Errors errors)
        {
        }
    }


    public static ActionURL getShowMS2AdminURL(Integer days)
    {
        ActionURL url = new ActionURL(ShowMS2AdminAction.class, ContainerManager.getRoot());

        if (null != days)
            url.addParameter("days", days.intValue());

        return url;
    }


    @RequiresSiteAdmin
    public class ShowMS2AdminAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            MS2AdminBean bean = new MS2AdminBean();

            bean.days = getDays();
            bean.stats = MS2Manager.getStats(bean.days);
            bean.purgeStatus = MS2Manager.getPurgeStatus();
            bean.successfulURL = showRunsURL(false, 1);
            bean.inProcessURL = showRunsURL(false, 0);
            bean.failedURL = showRunsURL(false, 2);
            bean.deletedURL = showRunsURL(true, null);

            JspView<MS2AdminBean> result = new JspView<MS2AdminBean>("/org/labkey/ms2/ms2Admin.jsp", bean);
            result.setFrame(WebPartView.FrameType.PORTAL);
            result.setTitle("MS2 Data Overview");
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            PageFlowUtil.urlProvider(AdminUrls.class).appendAdminNavTrail(root, "MS2 Admin", null);
            return root;
        }
    }


    private ActionURL showRunsURL(Boolean deleted, Integer statusId)
    {
        ActionURL url = new ActionURL(ShowAllRunsAction.class, ContainerManager.getRoot());

        if (null != deleted)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".Deleted~eq", deleted.booleanValue() ? "1" : "0");

        if (null != statusId)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".StatusId~eq", String.valueOf(statusId));

        return url;
    }


    public static class MS2AdminBean
    {
        public ActionURL successfulURL;
        public ActionURL inProcessURL;
        public ActionURL failedURL;
        public ActionURL deletedURL;
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


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowAllRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            DataRegion rgn = new DataRegion();
            rgn.setName(MS2Manager.getDataRegionNameRuns());
            ColumnInfo containerColumnInfo = MS2Manager.getTableInfoRuns().getColumn("Container");
            ContainerDisplayColumn cdc = new ContainerDisplayColumn(containerColumnInfo, true);
            cdc.setEntityIdColumn(containerColumnInfo);
            cdc.setCaption("Folder");

            ActionURL containerURL = getViewContext().cloneActionURL().setAction(ShowListAction.class);

            // We don't want ActionURL to encode ${ContainerPath}, so set a dummy value and use string substitution
            String urlString = containerURL.setExtraPath("ContainerPath").getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/");
            cdc.setURLExpression(StringExpressionFactory.create(urlString, false));
            rgn.addDisplayColumn(cdc);

            DataColumn descriptionColumn = new DataColumn(MS2Manager.getTableInfoRuns().getColumn("Description")) {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    if (null != ctx.get("ContainerPath") && !((Boolean)ctx.get("deleted")).booleanValue())
                        super.renderGridCellContents(ctx, out);
                    else
                        out.write(getFormattedValue(ctx));
                }
            };
            ActionURL showRunURL = new ActionURL(ShowRunAction.class, ContainerManager.getRoot());
            StringBuilder sb = new StringBuilder(showRunURL.getLocalURIString());
            int i = sb.lastIndexOf("/");
            sb.replace(i, i, "\\$\\{ContainerPath}/");
            sb.append("run=${Run}");
            descriptionColumn.setURLExpression(StringExpressionFactory.create(sb.toString(), false));
            rgn.addDisplayColumn(descriptionColumn);

            rgn.addColumns(MS2Manager.getTableInfoRuns().getColumns("Path, Created, Deleted, StatusId, Status, PeptideCount, SpectrumCount, FastaId"));

            GridView gridView = new GridView(rgn, errors);
            gridView.getRenderContext().setUseContainerFilter(false);
            SimpleFilter runFilter = new SimpleFilter();

            if (!getUser().isAdministrator())
            {
                runFilter.addInClause("Container", ContainerManager.getIds(getUser(), ReadPermission.class));
            }

            gridView.setFilter(runFilter);
            gridView.setTitle("Show All Runs");

            setTitle("Show All Runs");

            return gridView;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;  // TODO: admin navtrail
        }
    }


    public static class ColumnForm extends RunForm
    {
        private boolean saveDefault = false;

        public boolean getSaveDefault()
        {
            return saveDefault;
        }

        public void setSaveDefault(boolean saveDefault)
        {
            this.saveDefault = saveDefault;
        }
    }


    private ActionURL getPickPeptideColumnsURL(MS2Run run, String currentColumns, ActionURL returnURL)
    {
        ActionURL url = new ActionURL(PickPeptideColumnsAction.class, getContainer());
        url.addParameter("run", run.getRun());
        if (null != currentColumns)
            url.addParameter("columns", currentColumns);
        url.addReturnURL(returnURL);
        return url;
    }


    public static ActionURL getPickPeptideColumnsPostURL(Container c, ActionURL returnURL, boolean saveDefault)
    {
        ActionURL url = new ActionURL(PickPeptideColumnsAction.class, c);
        url.addReturnURL(returnURL);
        if (saveDefault)
            url.addParameter("saveDefault", "1");
        return url;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PickPeptideColumnsAction extends FormViewAction<ColumnForm>
    {
        private MS2Run _run;
        private ActionURL _returnURL;

        public ModelAndView getView(ColumnForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form);
            _returnURL = form.getReturnActionURL();

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), _run);

            JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickPeptideColumns.jsp", new PickColumnsBean());
            pickColumns.setFrame(WebPartView.FrameType.PORTAL);
            pickColumns.setTitle("Choose Peptide Columns");
            PickColumnsBean bean = pickColumns.getModelBean();
            bean.commonColumns = _run.getCommonPeptideColumnNames();
            bean.proteinProphetColumns = _run.getProteinProphetPeptideColumnNames();
            bean.quantitationColumns = _run.getQuantitationPeptideColumnNames();

            // Put a space between each name
            bean.defaultColumns = peptideView.getPeptideColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
            bean.currentColumns = peptideView.getPeptideColumnNames(form.getColumns()).replaceAll(" ", "").replaceAll(",", ", ");
            bean.returnURL = _returnURL;

            getPageConfig().setFocusId("columns");
            return pickColumns;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendRunNavTrail(root, _run, _returnURL, "Customize View", getPageConfig(), "pickPeptideColumns");
            return root;
        }

        public void validateCommand(ColumnForm target, Errors errors)
        {
        }

        public boolean handlePost(ColumnForm form, BindException errors) throws Exception
        {
            _returnURL = form.getReturnActionURL();
            String columnNames = form.getColumns();
            if (columnNames == null)
            {
                columnNames = "";
            }
            columnNames = columnNames.replaceAll(" ", "");

            if (form.getSaveDefault())
            {
                MS2Run run = MS2Manager.getRun(_returnURL.getParameter("run"));
                if (run == null)
                {
                    throw new NotFoundException("Could not find run with id " + _returnURL.getParameter("run"));
                }
                AbstractMS2RunView view = getPeptideView(_returnURL.getParameter("grouping"), run);
                view.savePeptideColumnNames(run.getType(), columnNames);
                _returnURL.deleteParameter("columns");
            }
            else
                _returnURL.replaceParameter("columns", columnNames);

            return true;
        }

        public ActionURL getSuccessURL(ColumnForm form)
        {
            return _returnURL;
        }
    }


    public static class PickColumnsBean
    {
        public String commonColumns;
        public String proteinProphetColumns;
        public String quantitationColumns;
        public String defaultColumns;
        public String currentColumns;
        public ActionURL returnURL;
    }


    private ActionURL getPickProteinColumnsURL(MS2Run run, String currentColumns, ActionURL returnURL)
    {
        ActionURL url = new ActionURL(PickProteinColumnsAction.class, getContainer());
        url.addParameter("run", run.getRun());
        if (null != currentColumns)
            url.addParameter("proteinColumns", currentColumns);
        url.addReturnURL(returnURL);
        return url;
    }


    public static ActionURL getPickProteinColumnsPostURL(Container c, ActionURL returnURL, boolean saveDefault)
    {
        ActionURL url = new ActionURL(PickProteinColumnsAction.class, c);
        url.addReturnURL(returnURL);
        if (saveDefault)
            url.addParameter("saveDefault", "1");
        return url;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PickProteinColumnsAction extends FormViewAction<ColumnForm>
    {
        private MS2Run _run;
        private ActionURL _returnURL;

        public ModelAndView getView(ColumnForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form);

            _returnURL = form.getReturnActionURL();
            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), _run);

            JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickProteinColumns.jsp", new PickColumnsBean());
            pickColumns.setFrame(WebPartView.FrameType.PORTAL);
            pickColumns.setTitle("Pick Protein Columns");
            PickColumnsBean bean = pickColumns.getModelBean();

            bean.commonColumns = MS2Run.getCommonProteinColumnNames();
            bean.proteinProphetColumns = MS2Run.getProteinProphetProteinColumnNames();
            bean.quantitationColumns = _run.getQuantitationProteinColumnNames();

            // Put a space between each name
            bean.defaultColumns = peptideView.getProteinColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
            bean.currentColumns = peptideView.getProteinColumnNames(form.getProteinColumns()).replaceAll(" ", "").replaceAll(",", ", ");
            bean.returnURL = _returnURL;

            getPageConfig().setFocusId("columns");
            return pickColumns;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, _returnURL, "Customize View", getPageConfig(), "pickProteinColumns");
        }

        public void validateCommand(ColumnForm target, Errors errors)
        {
        }

        public boolean handlePost(ColumnForm form, BindException errors) throws Exception
        {
            _returnURL = form.getReturnActionURL();
            String columnNames = form.getColumns();
            if (columnNames == null)
            {
                columnNames = "";
            }
            columnNames = columnNames.replaceAll(" ", "");

            if (form.getSaveDefault())
            {
                MS2Run run = MS2Manager.getRun(_returnURL.getParameter("run"));
                AbstractMS2RunView view = getPeptideView(_returnURL.getParameter("grouping"), run);
                view.saveProteinColumnNames(run.getType(), columnNames);
                _returnURL.deleteParameter("proteinColumns");
            }
            else
                _returnURL.replaceParameter("proteinColumns", columnNames);

            return true;
        }

        public ActionURL getSuccessURL(ColumnForm columnForm)
        {
            return _returnURL;
        }
    }


/*    @RequiresPermissionClass(ReadPermission.class)
    public class SaveProteinColumnsAction extends RedirectAction<ColumnForm>
    {
        private ActionURL _returnURL;

        public ActionURL getSuccessURL(ColumnForm columnForm)
        {
            return _returnURL;
        }

        public boolean doAction(ColumnForm form, BindException errors) throws Exception
        {
            _returnURL = form.getReturnActionURL();
            String columnNames = form.getColumns();
            if (columnNames == null)
            {
                columnNames = "";
            }
            columnNames = columnNames.replaceAll(" ", "");

            if (form.getSaveDefault())
            {
                MS2Run run = MS2Manager.getRun(_returnURL.getParameter("run"));
                AbstractMS2RunView view = getPeptideView(_returnURL.getParameter("grouping"), run);
                view.saveProteinColumnNames(run.getType(), columnNames);
                _returnURL.deleteParameter("proteinColumns");
            }
            else
                _returnURL.replaceParameter("proteinColumns", columnNames);

            return true;
        }

        public void validateCommand(ColumnForm target, Errors errors)
        {
        }
    }
*/

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

    @RequiresLogin
    @RequiresPermissionClass(ReadPermission.class)
    public class SaveViewAction extends FormViewAction<MS2ViewForm>
    {
        private MS2Run _run;
        private ActionURL _returnURL;

        public void validateCommand(MS2ViewForm target, Errors errors)
        {
        }

        public ModelAndView getView(MS2ViewForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form);

            _returnURL = getViewContext().cloneActionURL().setAction(ShowRunAction.class);
            JspView<SaveViewBean> saveView = new JspView<SaveViewBean>("/org/labkey/ms2/saveView.jsp", new SaveViewBean());
            SaveViewBean bean = saveView.getModelBean();
            bean.returnURL = _returnURL;
            bean.canShare = getContainer().hasPermission(getUser(), InsertPermission.class);

            ActionURL newURL = bean.returnURL.clone().deleteParameter("run");
            bean.viewParams = newURL.getRawQuery();

            getPageConfig().setFocusId("name");

            return saveView;
        }

        public boolean handlePost(MS2ViewForm form, BindException errors) throws Exception
        {
            String viewParams = (null == form.getViewParams() ? "" : form.getViewParams());

            String name = form.name;
            PropertyManager.PropertyMap m;
            if (form.isShared() && getContainer().hasPermission(getUser(), InsertPermission.class))
                m = PropertyManager.getWritableProperties(getContainer().getId(), MS2_VIEWS_CATEGORY, true);
            else
                m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

            m.put(name, viewParams);
            PropertyManager.saveProperties(m);

            return true;
        }

        public URLHelper getSuccessURL(MS2ViewForm form)
        {
            return form.getReturnURLHelper();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, _returnURL, "Save View", getPageConfig(), "viewRun");
        }
    }


    public static class SaveViewBean
    {
        public ActionURL returnURL;
        public boolean canShare;
        public String viewParams;
    }


    public static class MS2ViewForm extends RunForm
    {
        private String viewParams;
        private String name;
        private boolean shared;

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public void setViewParams(String viewParams)
        {
            this.viewParams = viewParams;
        }

        public String getViewParams()
        {
            return this.viewParams;
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


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinAction extends SimpleViewAction<DetailsForm>
    {
        private MS2Run _run;
        private Protein _protein;

        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            int runId;
            int seqId;
            if (form.run != 0)
            {
                runId = form.run;
                seqId = form.getSeqIdInt();
            }
            else if (form.getPeptideId() != 0)
            {
                MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());
                if (peptide != null)
                {
                    runId = peptide.getRun();
                    seqId = peptide.getSeqId() == null ? 0 : peptide.getSeqId().intValue();
                }
                else
                {
                    throw new NotFoundException("Peptide not found");
                }
            }
            else
            {
                seqId = form.getSeqIdInt();
                runId = 0;
            }

            ActionURL currentURL = getViewContext().getActionURL();

            if (0 == seqId)
                throw new NotFoundException("Protein sequence not found");

            _protein = ProteinManager.getProtein(seqId);
            if (_protein == null)
            {
                throw new NotFoundException("Could not find protein with SeqId " + seqId);
            }

            QueryPeptideMS2RunView peptideQueryView = null;

            // runId is not set when linking from compare
            if (runId != 0)
            {
                _run = validateRun(runId);

                peptideQueryView = new QueryPeptideMS2RunView(getViewContext(), _run);

                // Set the protein name used in this run's FASTA file; we want to include it in the view.
                _protein.setLookupString(form.getProtein());
            }

            getPageConfig().setTemplate(PageConfig.Template.Print);

            return new ProteinsView(currentURL, _run, form, new Protein[] {_protein}, null, peptideQueryView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild(getProteinTitle(_protein, true));
        }
    }


    /**
     * Used by link on SeqHits column of peptides grid view, calculates all proteins within the
     * fasta for the current run that have the given peptide sequence. No peptides grid shown.
     */
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowAllProteinsAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + form.getPeptideId());

            MS2Run run = validateRun(peptide.getRun());

            setTitle("Proteins Containing " + peptide);
            getPageConfig().setTemplate(PageConfig.Template.Print);

            Protein[] proteins = ProteinManager.getProteinsContainingPeptide(run.getFastaId(), peptide);
            ActionURL currentURL = getViewContext().cloneActionURL();

            ProteinsView view = new ProteinsView(currentURL, run, form, proteins, new String[]{peptide.getTrimmedPeptide()}, null);
            HttpView summary = new HtmlView("<table><tr><td><span class=\"navPageHeader\">All protein sequences in FASTA file " + run.getFastaFileName() + " that contain the peptide " + peptide + "</span></td></tr></table>");
            return new VBox(summary, view);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinGroupAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            // May have a runId, a group number, and an indistinguishableGroupId, or might just have a
            // proteinGroupId
            if (form.getProteinGroupId() != null)
            {
                ProteinGroupWithQuantitation group = MS2Manager.getProteinGroup(form.getProteinGroupId().intValue());
                if (group != null)
                {
                    ProteinProphetFile file = MS2Manager.getProteinProphetFile(group.getProteinProphetFileId());
                    if (file != null)
                    {
                        form.run = file.getRun();

                        MS2Run run = validateRun(form);

                        ActionURL url = getViewContext().cloneActionURL();
                        url.deleteParameter("proteinGroupId");
                        url.replaceParameter("run", Integer.toString(form.run));
                        url.replaceParameter("groupNumber", Integer.toString(group.getGroupNumber()));
                        url.replaceParameter("indistinguishableCollectionId", Integer.toString(group.getIndistinguishableCollectionId()));
                        url.setContainer(run.getContainer());

                        return HttpView.redirect(url);
                    }
                }
            }

            MS2Run run1 = validateRun(form);

            ProteinProphetFile proteinProphet = run1.getProteinProphetFile();
            if (proteinProphet == null)
            {
                throw new NotFoundException();
            }
            ProteinGroupWithQuantitation group = proteinProphet.lookupGroup(form.getGroupNumber(), form.getIndistinguishableCollectionId());
            if (group == null)
            {
                throw new NotFoundException();
            }
            Protein[] proteins = group.lookupProteins();

            setTitle(run1.getDescription());

            // todo:  does the grid filter affect the list of proteins displayed?
            QueryPeptideMS2RunView peptideQueryView = new QueryPeptideMS2RunView(getViewContext(), run1);

            VBox view = new ProteinsView(getViewContext().getActionURL(), run1, form, proteins, null, peptideQueryView);
            JspView summaryView = new JspView<ProteinGroupWithQuantitation>("/org/labkey/ms2/showProteinGroup.jsp", group);
            summaryView.setTitle("Protein Group Details");
            summaryView.setFrame(WebPartView.FrameType.PORTAL);

            return new VBox(summaryView, view);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    public static class ProteinViewBean
    {
        public Protein protein;
        public boolean showPeptides;
        public MS2Run run;
        public String showRunUrl;
        public boolean enableAllPeptidesFeature;
        public static final String ALL_PEPTIDES_URL_PARAM = "allPeps";
    }


    private static class ProteinsView extends VBox
    {
        private ProteinsView(ActionURL currentURL, MS2Run run, DetailsForm form, Protein[] proteins, String[] peptides, QueryPeptideMS2RunView peptideView) throws Exception
        {
            // Limit to 100 proteins
            int proteinCount = Math.min(100, proteins.length);
            // string search:  searching for a peptide string in the proteins of a given run
            boolean stringSearch = (null != peptides);
            // don't show the peptides grid or the coveage map for the Proteins matching a peptide or the no run case (e.g click on a protein Name in Matching Proteins grid of search results)            
            boolean showPeptides = !stringSearch && run != null;
            SimpleFilter allPeptidesQueryFilter = null;
            AbstractQueryMS2RunView.AbstractMS2QueryView gridView = null;

            ActionURL targetURL = currentURL.clone();
            ProteinViewBean bean = new ProteinViewBean();
            // the all peptides matching applies to peptides matching a single protein.  Don't
            // offer it as a choice in the case of protein groups

            bean.enableAllPeptidesFeature = !("proteinprophet".equalsIgnoreCase(form.getGrouping()) || proteinCount > 1 || !showPeptides);

            if (showPeptides)
            {
                try
                {
                    allPeptidesQueryFilter = getAllPeptidesFilter(getViewContext(), targetURL, run);
                    gridView = peptideView.createGridView(allPeptidesQueryFilter);
                    peptides = Table.executeArray(gridView.getTable(), "Peptide", allPeptidesQueryFilter, new Sort("Peptide"), String.class);
                }
                catch (RuntimeSQLException e)
                {
                    if (e.getSQLException() instanceof SQLGenerationException)
                    {
                        throw new NotFoundException("Invalid filter: " + e.getSQLException().getMessage());
                    }
                    throw e;
                }
            }

            for (int i = 0; i < proteinCount; i++)
            {
                addView(new HtmlView("<a name=\"Protein" + i + "\"></a>"));
                proteins[i].setPeptides(peptides);
                proteins[i].setShowEntireFragmentInCoverage(stringSearch);
                bean.protein = proteins[i];
                bean.showPeptides = showPeptides;
                JspView proteinSummary = new JspView<ProteinViewBean>("/org/labkey/ms2/protein.jsp", bean);
                proteinSummary.setTitle(getProteinTitle(proteins[i], true));
                proteinSummary.enableExpandCollapse("ProteinSummary", false);
                addView(proteinSummary);
                //TODO:  do something sensible for a single seqid and no run.
                JspView<ProteinViewBean> sequenceView;
                bean.run = run;
                if (showPeptides && !form.isSimpleSequenceView())
                {
                    sequenceView = new JspView<ProteinViewBean>("/org/labkey/ms2/proteinCoverageMap.jsp", bean);
                }
                else
                {
                    sequenceView = new JspView<ProteinViewBean>("/org/labkey/ms2/proteinSequence.jsp", bean);
                }
                sequenceView.enableExpandCollapse("ProteinCoverageMap", false);
                sequenceView.setTitle("Protein Sequence");
                addView(sequenceView);

                // Add annotations
                AnnotView annotations = new AnnotView(proteins[i]);
                annotations.enableExpandCollapse("ProteinAnnotationsView", true);
                addView(annotations);
            }

            if (showPeptides)
            {
                List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
                sqlSummaries.add(new Pair<String, String>("Peptide Filter", allPeptidesQueryFilter.getFilterText()));
                sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(targetURL, MS2Manager.getDataRegionNamePeptides()).getSortText()));
                Set<String> distinctPeptides = new HashSet<String>(Arrays.asList(peptides));
                sqlSummaries.add(new Pair<String, String>("Peptide Counts", peptides.length + " total, " + distinctPeptides.size() + " distinct"));
                CurrentFilterView peptideCountsView = new CurrentFilterView(null, sqlSummaries);
                peptideCountsView.setFrame(FrameType.NONE);
                gridView.setFrame(FrameType.NONE);
                VBox vBox = new VBox(peptideCountsView, new HtmlView("<a name=\"Peptides\" />"), gridView);
                vBox.setFrame(FrameType.PORTAL);
                vBox.setTitle("Peptides");
                vBox.enableExpandCollapse("Peptides", false);
                addView(vBox);
            }
        }
    }

    /**
     *  need to surface filters that are saved in the peptides view so that the coverage map will adhere
     * to them.  so the idea is to push the saved view parameters onto the URL, then get the SQL where claise
     * from the URL, where the saved view filters clause will be augmented by any filters set by the
     * user on the column header.
     * Both the coverage map(s) and the peptide grid now go through this method to get their set of peptides
     */
    private static SimpleFilter getAllPeptidesFilter(ViewContext ctx, ActionURL currentUrl, MS2Run run )
    {
        User user = ctx.getUser();
        Container c = ctx.getContainer();

        QueryDefinition queryDef = QueryService.get().createQueryDef(user, c, MS2Schema.SCHEMA_NAME, MS2Schema.TableType.Peptides.toString());
        String viewName = currentUrl.getParameter(MS2Manager.getDataRegionNamePeptides() + "." + "viewName");
        CustomView view = queryDef.getCustomView(user, ctx.getRequest(), viewName);
        if (view != null && view.hasFilterOrSort() && currentUrl.getParameter(MS2Manager.getDataRegionNamePeptides() + "." + QueryParam.ignoreFilter) == null)
        {
            view.applyFilterAndSortToURL(currentUrl, MS2Manager.getDataRegionNamePeptides());
        }
        SimpleFilter filter = ProteinManager.getPeptideFilter(currentUrl,
                ProteinManager.URL_FILTER + ProteinManager.PROTEIN_FILTER + ProteinManager.EXTRA_FILTER, ctx.getUser(), run);

        // Clean up the filter to remove any columns that aren't available in this query-based Peptides view
        // The legacy views may include some columns like GeneName that aren't available, and leaving them
        // in the filter causes a SQLException
        TableInfo peptidesTable = QueryService.get().getUserSchema(user, c, MS2Schema.SCHEMA_NAME).getTable(MS2Schema.TableType.Peptides.toString());
        SimpleFilter result = new SimpleFilter();
        for (SimpleFilter.FilterClause filterClause : filter.getClauses())
        {
            boolean legit = true;
            for (String columnName : filterClause.getColumnNames())
            {
                if (QueryService.get().getColumns(peptidesTable, Collections.singleton(FieldKey.fromString(columnName))).isEmpty())
                {
                    legit = false;
                    break;
                }
            }
            if (legit)
            {
                result.addClause(filterClause);
            }
        }
        return result;
    }

    /**
     * Exports a simple HTML document that excel can open and transform into something that looks like the html version
     * of the protein coverage map.  Can't use CSS tags.
     */
    @RequiresPermissionClass(ReadPermission.class)
    public class ExportProteinCoverageMapAction extends SimpleViewAction<DetailsForm>
    {

        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            MS2Run ms2Run;
            Protein protein;
            protein = ProteinManager.getProtein(form.getSeqIdInt());
            if (protein == null)
                throw new NotFoundException("Could not find protein with SeqId " + form.getSeqIdInt());
            ms2Run = validateRun(form);

            ActionURL targetURL=getViewContext().getActionURL().clone();

            SimpleFilter allPeptidesQueryFilter = getAllPeptidesFilter(getViewContext(), targetURL, ms2Run);
            QueryPeptideMS2RunView qpmv = new QueryPeptideMS2RunView(getViewContext(), ms2Run);
            AbstractQueryMS2RunView.AbstractMS2QueryView qv = qpmv.createGridView(allPeptidesQueryFilter);
            String[] peptides = Table.executeArray(qv.getTable(), "Peptide", allPeptidesQueryFilter, new Sort("Peptide"), String.class);

            protein.setPeptides(peptides);
            protein.setShowEntireFragmentInCoverage(false);

            HttpServletResponse resp = getViewContext().getResponse();
            resp.reset();
            resp.setContentType("text/html");
            String filename = FileUtil.makeFileNameWithTimestamp(protein.getBestName(), "htm");
            resp.setHeader("Content-disposition", "attachment; filename=\"" + filename +"\"");

            protein.setForCoverageMapExport(true);
            boolean allpeptides = ProteinManager.showAllPeptides(getViewContext().getActionURL(), getViewContext().getUser());
            PrintWriter pw = resp.getWriter();
            pw.write("<html><body>");
            pw.write("<p> Protein: &nbsp; " + protein.getBestName() + (allpeptides ? "  (all matching peptides) " : "  (search engine matches) ") + "<br/> ");
            pw.write("Run: &nbsp; " + (null!=ms2Run.getDescription()?  ms2Run.getDescription() : ms2Run.getRun()) +  "<br/> ");
            pw.write("Peptide Filter: &nbsp; " + allPeptidesQueryFilter.getFilterText() + "</p> ");

            pw.write(protein.getCoverageMap(ms2Run, null).toString());
            pw.write("</body></html>");
            resp.flushBuffer();

            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    /**
     * Displays a peptide grid filtered on a trimmed peptide.  target of the onclick event of a peptide coverage bar
     * in a protein coverage map
     */
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptidePopupAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            MS2Run ms2Run;
            ms2Run = validateRun(form);
            MS2Run[] runs = new MS2Run[] { ms2Run };
            QueryPeptideMS2RunView peptideView = new QueryPeptideMS2RunView(getViewContext(), runs);
            WebPartView gv = peptideView.createGridView(form);
            VBox vBox = new VBox();
            vBox.setFrame(WebPartView.FrameType.DIALOG);           
            vBox.addView(gv);
            return vBox;
        }


    public NavTree appendNavTrail(NavTree root)
    {
        return null;
    }
}
    public static class PieSliceSectionForm
    {
        private String _sliceTitle;
        private String _sqids;

        public String getSliceTitle()
        {
            return _sliceTitle;
        }

        public void setSliceTitle(String sliceTitle)
        {
            _sliceTitle = sliceTitle;
        }

        public String getSqids()
        {
            return _sqids;
        }

        public void setSqids(String sqids)
        {
            _sqids = sqids;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PieSliceSectionAction extends SimpleViewAction<PieSliceSectionForm>
    {
        private PieSliceSectionForm _form;

        public ModelAndView getView(PieSliceSectionForm form, BindException errors) throws Exception
        {
            _form = form;
            VBox vbox = new VBox();

            if (form.getSliceTitle() == null || form.getSqids() == null)
            {
                throw new NotFoundException();
            }

            String accn = form.getSliceTitle().split(" ")[0];
            String sliceDefinition = ProteinDictionaryHelpers.getGODefinitionFromAcc(accn);
            if (StringUtils.isBlank(sliceDefinition))
                sliceDefinition = "Miscellaneous or Defunct Category";
            String html = "<font size=\"+1\">" + PageFlowUtil.filter(sliceDefinition) + "</font>";
            HttpView definitionView = new HtmlView("Definition", html);
            vbox.addView(definitionView);

            String sqids = form.getSqids();
            String sqidArr[] = sqids.split(",");
            for (String curSqid : sqidArr)
            {
                int curSeqId = Integer.parseInt(curSqid);
                Protein protein = ProteinManager.getProtein(curSeqId);
                vbox.addView(new AnnotView(protein));
            }

            return vbox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Pieslice Details for: " + _form.getSliceTitle());
        }
    }


    private static class AnnotView extends JspView<AnnotViewBean>
    {
        // TODO: Pass in Protein object
        private AnnotView(Protein protein) throws Exception
        {
            super("/org/labkey/ms2/protAnnots.jsp", getBean(protein));
            setTitle("Annotations");
        }

        private static AnnotViewBean getBean(Protein protein) throws Exception
        {
            int seqId = protein.getSeqId();

            MultiMap<String, String> identifiers = ProteinManager.getIdentifiersFromId(seqId);

            /* collect header info */
            String SeqName = protein.getBestName(); // ProteinManager.getSeqParamFromId("BestName", seqId);
            String SeqDesc = protein.getDescription(); // ProteinManager.getSeqParamFromId("Description", seqId);
            Collection<String> GeneNames = identifiers.get("genename");
            /* collect first table info */
            Collection<String> GenBankIds = identifiers.get("genbank");
            Collection<String> SwissProtNames = identifiers.get("swissprot");
            Collection<String> EnsemblIDs = identifiers.get("ensembl");
            Collection<String> GIs = identifiers.get("gi");
            Collection<String> SwissProtAccns = identifiers.get(IdentifierType.SwissProtAccn.name().toLowerCase());
            Collection<String> IPIds = identifiers.get("ipi");
            Collection<String> RefSeqIds = identifiers.get("refseq");
            Collection<String> GOCategories = identifiers.get("go");

            HashSet<String> allGbIds = new HashSet<String>();
            if (null != GenBankIds)
                allGbIds.addAll(GenBankIds);
            if (null != RefSeqIds)
                allGbIds.addAll(RefSeqIds);

            Set<String> allGbURLs = new HashSet<String>();

            for (String ident : allGbIds)
            {
                String url = ProteinManager.makeFullAnchorString(
                        ProteinManager.makeIdentURLStringWithType(ident, "Genbank"),
                        "protWindow",
                        ident);
                allGbURLs.add(url);
            }

            // It is convenient to strip the version numbers from the IPI identifiers
            // and this may cause some duplications.  Use a hash-set to compress
            // duplicates
            if (null != IPIds && !IPIds.isEmpty())
            {
                Set<String> IPIset = new HashSet<String>();

                for (String idWithoutVersion : IPIds)
                {
                    int dotIndex = idWithoutVersion.indexOf(".");
                    if (dotIndex != -1) idWithoutVersion = idWithoutVersion.substring(0, dotIndex);
                    IPIset.add(idWithoutVersion);
                }
                IPIds = IPIset;
            }

            AnnotViewBean bean = new AnnotViewBean();

            /* info from db into view */
            bean.seqName = SeqName;
            bean.seqDesc = SeqDesc;
            bean.geneName = StringUtils.join(ProteinManager.makeFullAnchorStringArray(GeneNames, "protWindow", "GeneName"), ", ");
            bean.seqOrgs = ProteinManager.getOrganismsFromId(seqId);
            bean.genBankUrls = allGbURLs;
            bean.swissProtNames = ProteinManager.makeFullAnchorStringArray(SwissProtNames, "protWindow", "SwissProt");
            bean.swissProtAccns = ProteinManager.makeFullAnchorStringArray(SwissProtAccns, "protWindow", "SwissProtAccn");
            bean.GIs = ProteinManager.makeFullAnchorStringArray(GIs, "protWindow", "GI");
            bean.ensemblIds = ProteinManager.makeFullAnchorStringArray(EnsemblIDs, "protWindow", "Ensembl");
            bean.goCategories = ProteinManager.makeFullGOAnchorStringArray(GOCategories, "protWindow");
            bean.IPI = ProteinManager.makeFullAnchorStringArray(IPIds, "protWindow", "IPI");

            return bean;
        }
    }


    public static String getProteinTitle(Protein p, boolean includeBothNames)
    {
        if (null == p.getLookupString())
            return p.getBestName();

        if (!includeBothNames || p.getLookupString().equalsIgnoreCase(p.getBestName()))
            return p.getLookupString();

        return p.getLookupString() + " (" + p.getBestName() + ")";
    }


    public static class AnnotViewBean
    {
        public String seqName;
        public String seqDesc;
        public String geneName;
        public Set<String> seqOrgs;
        public Set<String> genBankUrls;
        public String[] swissProtNames;
        public String[] swissProtAccns;
        public String[] GIs;
        public String[] ensemblIds;
        public String[] goCategories;
        public String[] IPI;
    }

    protected void showElutionGraph(HttpServletResponse response, DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
    {
        long peptideId = form.getPeptideId();
        MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

        if (peptide == null)
            throw new NotFoundException("Could not find peptide with RowId " + peptideId);

        int runId = peptide.getRun();

        MS2Run run = validateRun(runId);

        PeptideQuantitation quantitation = peptide.getQuantitation();
        if (quantitation == null)
        {
            renderErrorImage("No quantitation data for this peptide", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
            return;
        }
        response.setDateHeader("Expires", 0);
        response.setContentType("image/png");

        File f = quantitation.findScanFile();
        if (f != null)
        {
            ElutionGraph g = new ElutionGraph();
            int charge = form.getQuantitationCharge() == Integer.MIN_VALUE ? peptide.getCharge() : form.getQuantitationCharge();
            if (charge < 1 || charge > PeptideQuantitation.MAX_CHARGE)
            {
                renderErrorImage("Invalid charge state: " + charge, response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
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
                renderErrorImage("No relevant MS1 scans found in spectra file", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
            }
            else
            {
                g.render(response.getOutputStream());
            }
        }
        else
        {
            renderErrorImage("Could not open spectra file to get MS1 scans", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
        }
    }

    private void renderErrorImage(String errorMessage, HttpServletResponse response, int width, int height)
            throws IOException
    {
        Graph g = new Graph(new float[0], new float[0], width, height)
        {
            protected void initializeDataPoints(Graphics2D g) {}
            protected void renderDataPoint(Graphics2D g, double x, double y) {}
        };
        g.setNoDataErrorMessage(errorMessage);
        g.render(response);
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowLightElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            showElutionGraph(response, form, true, false);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowHeavyElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            showElutionGraph(response, form, false, true);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowCombinedElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            showElutionGraph(response, form, true, true);
        }
    }

    @RequiresSiteAdmin
    public class MascotTestAction extends SimpleViewAction<TestMascotForm>
    {
        public ModelAndView getView(TestMascotForm form, BindException errors) throws Exception
        {
            String originalMascotServer = form.getMascotServer();
            MascotClientImpl mascotClient = new MascotClientImpl(form.getMascotServer(), null,
                form.getMascotUserAccount(), form.getMascotUserPassword());
            mascotClient.setProxyURL(form.getMascotHTTPProxy());
            mascotClient.findWorkableSettings(true);
            form.setStatus(mascotClient.getErrorCode());

            String message;
            if (0 == mascotClient.getErrorCode())
            {
                if ("".equals(mascotClient.getErrorString()))
                {
                    message = "Test passed.";
                }
                else
                {
                    message = mascotClient.getErrorString();
                }
                form.setParameters(mascotClient.getParameters());
            }
            else
            {
                message = "Test failed.";
                message = message + "<br>" + mascotClient.getErrorString();
            }

            form.setMessage(message);
            form.setMascotServer(originalMascotServer);
            form.setMascotUserPassword(("".equals(form.getMascotUserPassword())) ? "" : "***");  // do not show password in clear

            getPageConfig().setTemplate(PageConfig.Template.Dialog);
            return new JspView<TestMascotForm>("/org/labkey/ms2/testMascot.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    public static class TestMascotForm
    {
        private String _mascotServer = "";
        private String _mascotUserAccount = "";
        private String _mascotUserPassword = "";
        private String _mascotHTTPProxy = "";
        private int _status;
        private String _parameters = "";
        private String _message;

        public String getMascotUserAccount()
        {
            return _mascotUserAccount;
        }

        public void setMascotUserAccount(String mascotUserAccount)
        {
            _mascotUserAccount = mascotUserAccount;
        }

        public String getMascotUserPassword()
        {
            return _mascotUserPassword;
        }

        public void setMascotUserPassword(String mascotUserPassword)
        {
            _mascotUserPassword = mascotUserPassword;
        }

        public String getMascotServer()
        {
            return _mascotServer;
        }

        public void setMascotServer(String mascotServer)
        {
            _mascotServer = mascotServer;
        }

        public String getMascotHTTPProxy()
        {
            return _mascotHTTPProxy;
        }

        public void setMascotHTTPProxy(String mascotHTTPProxy)
        {
            _mascotHTTPProxy = mascotHTTPProxy;
        }

        public String getMessage()
        {
            return _message;
        }

        public void setMessage(String message)
        {
            _message = message;
        }

        public int getStatus()
        {
            return _status;
        }

        public void setStatus(int status)
        {
            _status = status;
        }

        public String getParameters()
        {
            return _parameters;
        }

        public void setParameters(String parameters)
        {
            _parameters = parameters;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ApplyRunViewAction extends SimpleRedirectAction<MS2ViewForm>
    {
        public ActionURL getRedirectURL(MS2ViewForm form) throws Exception
        {
            // Redirect to have Spring fill in the form and ensure that the DataRegion JavaScript sees the showRun action
            return getApplyViewForwardURL(form, ShowRunAction.class);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ApplyExportRunsViewAction extends SimpleForwardAction<MS2ViewForm>
    {
        public ActionURL getForwardURL(MS2ViewForm form) throws Exception
        {
            // Forward without redirect: this lets Spring fill in the form but preserves the post data
            return getApplyViewForwardURL(form, ExportRunsAction.class);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ApplyCompareViewAction extends SimpleRedirectAction<MS2ViewForm>
    {
        public ActionURL getRedirectURL(MS2ViewForm form) throws Exception
        {
            ActionURL redirectURL = getApplyViewForwardURL(form, ShowCompareAction.class);

            redirectURL.deleteParameter("submit.x");
            redirectURL.deleteParameter("submit.y");
            redirectURL.deleteParameter("viewParams");

            return redirectURL;
        }
    }


    private ActionURL getApplyViewForwardURL(MS2ViewForm form, Class<? extends Controller> action)
    {
        // Add the "view params" (which were posted as a single param) to the URL params.
        ActionURL forwardURL = getViewContext().cloneActionURL();
        forwardURL.setRawQuery(forwardURL.getRawQuery() + (null == form.viewParams ? "" : "&" + form.viewParams));
        return forwardURL.setAction(action);
    }


    @RequiresSiteAdmin
    public class ReloadSPOMAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ProteinDictionaryHelpers.loadProtSprotOrgMap();

            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    private static final StringKeyCache<PieJChartHelper> PIE_CHART_CACHE = CacheManager.getSharedCache();

    @RequiresPermissionClass(ReadPermission.class)
    public class DoOnePeptideChartAction extends ExportAction
    {
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            HttpServletRequest req = getViewContext().getRequest();
            String helperName = req.getParameter("helpername");

            if (null == helperName)
                throw new NotFoundException("Parameter \"helpername\" is missing");

            response.setContentType("image/png");
            OutputStream out = response.getOutputStream();

            PieJChartHelper pjch = PIE_CHART_CACHE.get(helperName);

            if (null == pjch)
                throw new NotFoundException("Pie chart was not found.");

            try
            {
                pjch.renderAsPNG(out);
            }
            catch (Exception e)
            {
                _log.error("Chart rendering failed", e);
            }
            finally
            {
                PIE_CHART_CACHE.remove(helperName);
            }
        }
    }


    public static class AddRunForm
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


    @RequiresNoPermission
    public class AddRunAction extends SimpleRedirectAction<AddRunForm>
    {
        public ActionURL getRedirectURL(AddRunForm form) throws Exception
        {
            Container c = getContainer();
            ActionURL url;
            File f = null;

            if ("Show Runs".equals(getViewContext().getActionURL().getParameter("list")))
            {
                if (c == null)
                {
                    throw new NotFoundException();
                }
                else
                    return getShowListURL(c);
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
                    ViewBackgroundInfo info = getViewBackgroundInfo();

                    // TODO: Clean this up.
                    AbstractMS2SearchProtocolFactory protocolFactory =
                            AbstractMS2SearchProtocolFactory.fromFile(AbstractMS2SearchPipelineProvider.class, f);

                    PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(getContainer());
                    int run;
                    if (MascotSearchProtocolFactory.get().getClass().equals(protocolFactory.getClass()))
                    {
                        run = MS2Manager.addMascotRunToQueue(info, f, form.getDescription(), pipeRoot).getRunId();
                    }
                    else
                    {
                        run = MS2Manager.addRunToQueue(info, f, form.getDescription(), pipeRoot).getRunId();
                    }

                    if (run == -1)
                    {
                        throw new NotFoundException();
                    }

                    url = getShowListURL(c);
                    url.addParameter(MS2Manager.getDataRegionNameExperimentRuns() + ".Run~eq", Integer.toString(run));

                    return url;
                }
                else
                {
                    return getAddFileRunStatusErrorURL("Automated upload disabled");
                }
            }
            else
            {
                return getAddFileRunStatusURL();
            }
        }
    }


    // TODO: Use form
    @RequiresPermissionClass(ReadPermission.class)
    public class AddExtraFilterAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ViewContext ctx = getViewContext();
            HttpServletRequest request = ctx.getRequest();
            ActionURL url = ctx.cloneActionURL();
            url.setAction(ShowRunAction.class);

            MS2Run run = MS2Manager.getRun(request.getParameter("run"));

            if (null == run)
                throw new NotFoundException("Run not found: " + request.getParameter("run"));

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

            if (request.getParameter("grouping") != null)
            {
                url.addParameter("grouping", request.getParameter("grouping"));
            }

            if (request.getParameter("expanded") != null)
            {
                url.addParameter("expanded", "1");
            }

            return url;
        }
    }


    // Parse parameter to float, returning 0 for any parsing exceptions
    private void parseChargeScore(ViewContext ctx, ActionURL url, String digit, String paramName)
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


    @RequiresPermissionClass(InsertPermission.class)
    public class ImportProteinProphetAction extends SimpleRedirectAction<PipelinePathForm>
    {
        public ActionURL getRedirectURL(PipelinePathForm form) throws Exception
        {
            for (File f : form.getValidatedFiles(getContainer()))
            {
                if (f.isFile())
                {
                    ProteinProphetPipelineJob job = new ProteinProphetPipelineJob(getViewBackgroundInfo(), f, form.getPipeRoot(getContainer()));
                    PipelineService.get().queueJob(job);
                }
                else
                {
                    throw new NotFoundException("Expected a file but found a directory: " + f.getName());
                }
            }

            return PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(getContainer());
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class DiscriminateScoreAction extends SimpleRedirectAction<RunForm>
    {
        public ActionURL getRedirectURL(RunForm form) throws Exception
        {
            ActionURL url = new ActionURL(ScoringController.DiscriminateAction.class, getContainer());
            url.addParameter("runId", form.getRun());
            return url;
        }
    }


    public static class RunForm extends ReturnUrlForm
    {
        public enum PARAMS
        {
            run, expanded, grouping
        }

        int run = 0;
        int fraction = 0;
        int tryptic;
        boolean expanded = false;
        boolean exportAsWebPage;
        String grouping;
        String columns;
        String proteinColumns;
        String proteinGroupingId;

        public void setExpanded(boolean expanded)
        {
            this.expanded = expanded;
        }

        public boolean getExpanded()
        {
            return this.expanded;
        }

        public void setRun(int run)
        {
            this.run = run;
        }

        public int getRun()
        {
            return run;
        }

        public int getFraction()
        {
            return fraction;
        }

        public void setFraction(int fraction)
        {
            this.fraction = fraction;
        }

        public void setTryptic(int tryptic)
        {
            this.tryptic = tryptic;
        }

        public int getTryptic()
        {
            return tryptic;
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

        public ActionURL getReturnActionURL()
        {
            ActionURL result;
            try
            {
                result = super.getReturnActionURL();
                if (result != null)
                {
                    return result;
                }
            }
            catch (Exception e)
            {
                // Bad URL -- fall through
            }

            // Bad or missing returnUrl -- go to showRun or showList
            Container c = HttpView.currentContext().getContainer();

            if (0 != run)
                return getShowRunURL(c, run);
            else
                return getShowListURL(c);
        }
    }


    @RequiresSiteAdmin
    public class InsertAnnotsAction extends FormViewAction<LoadAnnotForm>
    {
        public void validateCommand(LoadAnnotForm target, Errors errors)
        {
        }

        public ModelAndView getView(LoadAnnotForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<LoadAnnotForm>("/org/labkey/ms2/insertAnnots.jsp", form, errors);
        }

        public boolean handlePost(LoadAnnotForm form, BindException errors) throws Exception
        {
            String fname = form.getFileName();
            if (fname == null)
            {
                errors.addError(new LabkeyError("Please enter a file path."));
                return false;
            }
            try
            {
                new File(fname).getCanonicalFile();
            }
            catch (IOException e)
            {
                String error = "Invalid file path";
                if (e.getMessage() != null)
                {
                    error = error + " (" + e.getMessage() + ")";
                }
                errors.addError(new LabkeyError(error));
                return false;
            }
            String comment = form.getComment();

            DefaultAnnotationLoader loader;

            //TODO: this style of dealing with different file types must be repaired.
            if ("uniprot".equalsIgnoreCase(form.getFileType()))
            {
                loader = new XMLProteinLoader(fname, form.isClearExisting());
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
                errors.addError(new LabkeyError(e.getMessage()));
                return false;
            }

            loader.parseInBackground();

            return true;
        }

        public ActionURL getSuccessURL(LoadAnnotForm loadAnnotForm)
        {
            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendProteinAdminNavTrail(root, "Load Protein Annotations", getPageConfig(), null);
            return root;
        }
    }


    public static class LoadAnnotForm
    {
        private String _fileType = "uniprot";
        private String _comment;
        private String _fileName;
        private String _defaultOrganism = "Unknown unknown";
        private String _shouldGuess = "1";
        private boolean _clearExisting;

        public void setFileType(String ft)
        {
            _fileType = ft;
        }

        public String getFileType()
        {
            return _fileType;
        }

        public void setFileName(String file)
        {
            _fileName = file;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setComment(String s)
        {
            _comment = s;
        }

        public String getComment()
        {
            return _comment;
        }

        public String getDefaultOrganism()
        {
            return _defaultOrganism;
        }

        public void setDefaultOrganism(String o)
        {
            _defaultOrganism = o;
        }

        public String getShouldGuess()
        {
            return _shouldGuess;
        }

        public void setShouldGuess(String shouldGuess)
        {
            _shouldGuess = shouldGuess;
        }

        public boolean isClearExisting()
        {
            return _clearExisting;
        }

        public void setClearExisting(boolean clearExisting)
        {
            _clearExisting = clearExisting;
        }
    }


    @RequiresSiteAdmin
    public class DeleteAnnotInsertEntriesAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            int[] ids = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));

            for (int id : ids)
                ProteinManager.deleteAnnotationInsertion(id);

            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    public static class AnnotationInsertionForm
    {
        private int _insertId;

        public int getInsertId()
        {
            return _insertId;
        }

        public void setInsertId(int insertId)
        {
            _insertId = insertId;
        }
    }

    @RequiresSiteAdmin
    public class ShowAnnotInsertDetailsAction extends SimpleViewAction<AnnotationInsertionForm>
    {
        AnnotationInsertion _insertion;

        public ModelAndView getView(AnnotationInsertionForm form, BindException errors) throws Exception
        {
            AnnotationInsertion[] insertions = Table.executeQuery(ProteinManager.getSchema(), "SELECT * FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=?", new Object[] { form.getInsertId() }, AnnotationInsertion.class);
            if (insertions.length == 0)
            {
                throw new NotFoundException();
            }
            assert insertions.length == 1;
            _insertion = insertions[0];

            return new JspView<AnnotationInsertion>("/org/labkey/ms2/annotLoadDetails.jsp", _insertion);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendProteinAdminNavTrail(root, _insertion.getFiletype() + " Annotation Insertion Details: " + _insertion.getFilename(), getPageConfig(), null);
            return null;
        }
    }


    private ActionURL getAddFileRunStatusURL()
    {
        return new ActionURL(AddFileRunStatusAction.class, ContainerManager.getRoot());
    }


    private ActionURL getAddFileRunStatusErrorURL(String message)
    {
        ActionURL url = getAddFileRunStatusURL();
        url.addParameter("error", message);
        return url;
    }


    @RequiresNoPermission
    public class AddFileRunStatusAction extends ExportAction
    {
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            ActionURL url = getViewContext().getActionURL();
            String status = null;
            response.setContentType("text/plain");

            String path = url.getParameter("path");
            if (path != null)
            {
                path = PipelineJobService.statusPathOf(path);
                PipelineStatusFile sf = PipelineService.get().getStatusFile(path);
                if (sf == null)
                    status = "ERROR->path=" + path + ",message=Job not found in database";
/*            else if (run.getDeleted())
                status = "ERROR->run=" + runId + ",message=Run deleted"; */
                else
                {
                    String[] parts = (sf.getInfo() == null ?
                            new String[0] : sf.getInfo().split(","));
                    StringBuilder sb = new StringBuilder(sf.getStatus());
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
            else if (url.getParameter("error") != null)
            {
                status = "ERROR->message=" + url.getParameter("error");
            }
            else
            {
                // Old MS2-only code.  Still supports Comet searches.
                int runId = 0;
                String runParam = url.getParameter("run");
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
        }
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


    @RequiresSiteAdmin
    public class AnnotThreadControlAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            HttpServletRequest req = getViewContext().getRequest();
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
                    Thread.sleep(2000);
                }
                if (ftypeToRecover.equalsIgnoreCase("fasta"))
                {
                    FastaDbLoader fdbl = new FastaDbLoader(new File(fnameToRecover));
                    fdbl.parseInBackground(threadId);
                    Thread.sleep(2000);
                }
            }

            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowParamsFileAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            MS2Run run = validateRun(form);

            File paramsFile = null;
            // First check if we can find the merged set of parameters (project and protocol) that we used for this search
            String lsid = run.getExperimentRunLSID();
            if (lsid != null)
            {
                ExpRun expRun = ExperimentService.get().getExpRun(lsid);
                if (expRun != null)
                {
                    for (Map.Entry<ExpData, String> entry : expRun.getDataInputs().entrySet())
                    {
                        if (AbstractMS2SearchTask.JOB_ANALYSIS_PARAMETERS_ROLE_NAME.equalsIgnoreCase(entry.getValue()))
                        {
                            paramsFile = entry.getKey().getFile();
                            break;
                        }
                    }
                }
            }
            if (paramsFile == null || !NetworkDrive.exists(paramsFile))
            {
                // If not, fall back on the default name
                paramsFile = new File(run.getPath() + "/" + run.getParamsFileName());
            }
            PageFlowUtil.streamFile(response, paramsFile, false);
        }
    }


    @RequiresNoPermission
    public class UpdateShowPeptideAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ViewContext ctx = getViewContext();

            ActionURL redirectURL = ctx.cloneActionURL().setAction(ShowPeptideAction.class);
            String queryString = (String)ctx.get("queryString");
            redirectURL.setRawQuery(queryString);

            String xStart = (String)ctx.get("xStart");
            String xEnd = (String)ctx.get("xEnd");

            if ("".equals(xStart))
                redirectURL.deleteParameter("xStart");
            else
                redirectURL.replaceParameter("xStart", xStart);

            if ("".equals(xEnd))
                redirectURL.deleteParameter("xEnd");
            else
                redirectURL.replaceParameter("xEnd", xEnd);

            return redirectURL;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowGZFileAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + form.getPeptideId());

            validateRun(peptide.getRun());

            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();

            MS2GZFileRenderer renderer = new MS2GZFileRenderer(peptide, form.getExtension());

            if (!renderer.render(out))
            {
                MS2GZFileRenderer.renderFileHeader(out, MS2GZFileRenderer.getFileNameInGZFile(MS2Manager.getFraction(peptide.getFraction()), peptide.getScan(), peptide.getCharge(), form.extension));
                out.println(renderer.getLastErrorMessage());
            }
        }
    }


    public static class DetailsForm extends RunForm
    {
        private long peptideId;
        private int rowIndex = -1;
        private int height = 400;
        private int width = 600;
        private double tolerance = 1.0;
        private double xStart = Double.MIN_VALUE;
        private double xEnd = Double.MAX_VALUE;
        private int seqId;
        private String extension;
        private String protein;
        private int quantitationCharge = Integer.MIN_VALUE;
        private int groupNumber;
        private int indistinguishableCollectionId;
        private Integer proteinGroupId;
        private boolean simpleSequenceView = false;

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

        public void setPeptideId(long peptideId)
        {
            this.peptideId = peptideId;
        }

        public long getPeptideId()
        {
            return this.peptideId;
        }

        public void setxStart(String xStart)
        {
            try
            {
                this.xStart = Double.parseDouble(xStart);
            }
            catch (NumberFormatException ignored) {}
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
            catch (NumberFormatException ignored) {}
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

        public boolean isSimpleSequenceView()
        {
            return simpleSequenceView;
        }

        public void setSimpleSequenceView(boolean simpleSequenceView)
        {
            this.simpleSequenceView = simpleSequenceView;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class ToggleValidQuantitationAction extends RedirectAction<DetailsForm>
    {
        @Override
        public URLHelper getSuccessURL(DetailsForm detailsForm)
        {
            ActionURL result = getViewContext().getActionURL().clone();
            result.setAction(ShowPeptideAction.class);
            return result;
        }

        @Override
        public boolean doAction(DetailsForm form, BindException errors) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + form.getPeptideId());

            validateRun(peptide.getRun());

            PeptideQuantitation quantitation = peptide.getQuantitation();
            if (quantitation == null)
            {
                throw new NotFoundException("No quantitation data found for peptide");
            }

            // Toggle its validation state
            quantitation.setInvalidated(quantitation.includeInProteinCalc());
            Table.update(getUser(), MS2Manager.getTableInfoQuantitation(), quantitation, quantitation.getPeptideId());

            for (ProteinGroupWithQuantitation proteinGroup : MS2Manager.getProteinGroupsWithPeptide(peptide))
            {
                proteinGroup.recalcQuantitation(getUser());
            }

            return true;
        }

        @Override
        public void validateCommand(DetailsForm target, Errors errors) {}
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class EditElutionGraphAction extends FormViewAction<ElutionProfileForm>
    {
        public void validateCommand(ElutionProfileForm target, Errors errors)
        {
        }

        public ModelAndView getView(ElutionProfileForm form, boolean reshow, BindException errors) throws Exception
        {
            long peptideId = form.getPeptideId();
            MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + peptideId);

            MS2Run run = validateRun(peptide.getRun());

            PeptideQuantitation quant = peptide.getQuantitation();

            EditElutionGraphContext ctx = new EditElutionGraphContext(quant.getLightElutionProfile(peptide.getCharge()), quant.getHeavyElutionProfile(peptide.getCharge()), quant, getViewContext().getActionURL(), peptide);
            return new JspView<EditElutionGraphContext>("/org/labkey/ms2/editElution.jsp", ctx, errors);
        }

        public boolean handlePost(ElutionProfileForm form, BindException errors) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideId());

            if (peptide == null)
                throw new NotFoundException("Could not find peptide with RowId " + form.getPeptideId());

            MS2Run run = validateRun(peptide.getRun());

            PeptideQuantitation quant = peptide.getQuantitation();
            if (quant == null)
            {
                throw new NotFoundException("No quantitation data found for peptide " + form.getPeptideId());
            }

            boolean validRanges = quant.resetRanges(form.getLightFirstScan(), form.getLightLastScan(), form.getHeavyFirstScan(), form.getHeavyLastScan(), peptide.getCharge());
            if (validRanges)
            {
                Table.update(getUser(), MS2Manager.getTableInfoQuantitation(), quant, quant.getPeptideId());
                return true;
            }
            else
            {
                errors.addError(new LabkeyError("Invalid elution profile range"));
                return false;
            }
        }

        public ActionURL getSuccessURL(ElutionProfileForm detailsForm)
        {
            ActionURL url = getViewContext().getActionURL().clone();
            url.setAction(ShowPeptideAction.class);
            return url;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Edit Elution Profile");
        }
    }


    public static class MS2UrlsImpl implements MS2Urls
    {
        public ActionURL getShowPeptideUrl(Container container)
        {
            return new ActionURL(MS2Controller.ShowPeptideAction.class, container);
        }

        public ActionURL getShowRunUrl(MS2Run run)
        {
            return new ActionURL(ShowRunAction.class, run.getContainer()).addParameter("run", run.getRun());
        }

        public ActionURL getShowListUrl(Container container)
        {
            return new ActionURL(ShowListAction.class, container);
        }

        public ActionURL getShowProteinAdminUrl()
        {
            return new ActionURL(ShowProteinAdminAction.class, ContainerManager.getRoot());
        }

        public static MS2UrlsImpl get()
        {
            return (MS2UrlsImpl) PageFlowUtil.urlProvider(MS2Urls.class);
        }
    }


    public class CompareOptionsBean<Form extends PeptideFilteringComparisonForm>
    {
        private final FilterView _peptideView;
        private final FilterView _proteinGroupView;
        private final ActionURL _targetURL;
        private final int _runList;
        private final Form _form;

        public CompareOptionsBean(ActionURL targetURL, int runList, Form form)
        {
            _targetURL = targetURL;
            _runList = runList;
            _form = form;
            _peptideView = new FilterView(getViewContext(), true);
            _proteinGroupView = new FilterView(getViewContext(), false);
        }

        public FilterView getPeptideView()
        {
            return _peptideView;
        }

        public FilterView getProteinGroupView()
        {
            return _proteinGroupView;
        }

        public ActionURL getTargetURL()
        {
            return _targetURL;
        }

        public int getRunList()
        {
            return _runList;
        }

        public Form getForm()
        {
            return _form;
        }
    }

    @RequiresSiteAdmin
    public class AttachFilesUpgradeAction extends FormViewAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public ModelAndView getView(Object o, boolean reshow, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/ms2/pipeline/attachMSPictureFiles.jsp");
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            if (root == null || !root.isValid())
            {
                throw new NotFoundException("No pipeline root found for " + getContainer());
            }

            ViewBackgroundInfo info = getViewBackgroundInfo();
            PipelineJob job = new MSPictureUpgradeJob(info, root);
            PipelineService.get().getPipelineQueue().addJob(job);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Attach mspicture Files to Existing MS2 Runs");
        }
    }

    @RequiresSiteAdmin
    public class ImportMSScanCountsUpgradeAction extends FormViewAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public ModelAndView getView(Object o, boolean reshow, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/ms2/pipeline/importMSScanCounts.jsp");
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            if (root == null || !root.isValid())
            {
                throw new NotFoundException("No pipeline root found for " + getContainer());
            }

            ViewBackgroundInfo info = getViewBackgroundInfo();
            PipelineJob job = new ImportScanCountsUpgradeJob(info, root);
            PipelineService.get().getPipelineQueue().addJob(job);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Load MS scan counts");
        }
    }
}
