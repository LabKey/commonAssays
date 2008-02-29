package org.labkey.ms2;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMapping;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.labkey.api.action.*;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.*;
import org.labkey.api.query.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.common.tools.MS2Modification;
import org.labkey.common.tools.PeptideProphetSummary;
import org.labkey.common.tools.SensitivitySummary;
import org.labkey.common.util.Pair;
import org.labkey.ms2.compare.*;
import org.labkey.ms2.peptideview.*;
import org.labkey.ms2.pipeline.*;
import org.labkey.ms2.pipeline.mascot.MascotClientImpl;
import org.labkey.ms2.pipeline.mascot.MascotSearchProtocolFactory;
import org.labkey.ms2.pipeline.sequest.SequestClientImpl;
import org.labkey.ms2.pipeline.tandem.XTandemRun;
import org.labkey.ms2.protein.*;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.NullOutputStream;
import org.labkey.ms2.protein.tools.PieJChartHelper;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.query.*;
import org.labkey.ms2.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.*;
import java.net.URI;
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
    private static DefaultActionResolver _actionResolver = new DefaultActionResolver(MS2Controller.class);
    private static Logger _log = Logger.getLogger(MS2Controller.class);
    private static final String MS2_VIEWS_CATEGORY = "MS2Views";
    private static final int MAX_INSERTIONS_DISPLAY_ROWS = 1000; // Limit annotation table insertions to 1000 rows
    private static final String SHARED_VIEW_SUFFIX = " (Shared)";
    static final String CAPTION_SCORING_BUTTON = "Compare Scoring";

    public MS2Controller()
    {
        super();
        setActionResolver(_actionResolver);
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
                ActionURL url = getViewContext().getActionURL().clone();
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


    private NavTree appendRootNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "ms2" : helpTopic, HelpTopic.Area.CPAS));
        root.addChild("MS2 Runs", getShowListUrl(getContainer()));
        if (null != title)
            root.addChild(title);
        return root;
    }


    private NavTree appendRunNavTrail(NavTree root, MS2Run run, String title, PageConfig page, String helpTopic)
    {
        appendRootNavTrail(root, null, page, helpTopic);
        root.addChild(run.getDescription(), getShowRunUrl(getContainer(), run.getRun()));
        if (null != title)
            root.addChild(title);
        return root;
    }


    private NavTree appendAdminNavTrail(NavTree root, String adminPageTitle, ActionURL adminPageURL, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "ms2" : helpTopic, HelpTopic.Area.CPAS));
        root.addChild("Admin Console", new ActionURL("admin", "showAdmin", ""));
        root.addChild(adminPageTitle, adminPageURL);
        root.addChild(title);
        return root;
    }


    private NavTree appendProteinAdminNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        return appendAdminNavTrail(root, "Protein Database Admin", MS2UrlsImpl.get().getShowProteinAdminUrl(), title, page, helpTopic);
    }


    private NavTree appendMS2AdminNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        return appendAdminNavTrail(root, "MS2 Admin", getShowMS2AdminURL(null), title, page, helpTopic);
    }


    private AbstractMS2RunView getPeptideView(String grouping, MS2Run... runs) throws ServletException
    {
        return MS2RunViewType.getViewType(grouping).createView(getViewContext(), runs);
    }


    public static ActionURL getBeginUrl(Container c)
    {
        return new ActionURL(BeginAction.class, c);
    }


    public static ActionURL getPeptideChartUrl(Container c, ProteinDictionaryHelpers.GoTypes chartType)
    {
        ActionURL url = new ActionURL(PeptideChartsAction.class, c);
        url.addParameter("chartType", chartType.toString());
        return url;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o)
        {
            return getShowListUrl(getContainer());
        }
    }


    public static ActionURL getShowListUrl(Container c)
    {
        return new ActionURL(ShowListAction.class, c);
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ActionURL currentUrl = getViewContext().cloneActionURL();
            DataRegion rgn = new DataRegion();
            rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
            rgn.setColumns(MS2Manager.getTableInfoExperimentRuns().getColumns("Description, Path, Created, Status, ExperimentRunLSID, ExperimentRunRowId, ProtocolName, PeptideCount, NegativeHitCount"));
            for (int i = 4; i < rgn.getDisplayColumnList().size(); i++)
                rgn.getDisplayColumn(i).setVisible(false);


            currentUrl.setAction("showRun");
            currentUrl.deleteParameters();
            rgn.getDisplayColumn(0).setURL(currentUrl.getLocalURIString() + "&run=${Run}");
            // TODO: What does this do?
            rgn.setFixedWidthColumns(false);
            rgn.setShowRecordSelectors(true);

            GridView gridView = new GridView(rgn);
            gridView.setTitle("MS2 Runs");
            gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
            gridView.setSort(MS2Manager.getRunsBaseSort());

            ButtonBar bb = getListButtonBar(getContainer(), gridView);
            rgn.addColumn(new HideShowScoringColumn(bb));
            rgn.setButtonBar(bb, DataRegion.MODE_GRID);

            ProteinSearchWebPart searchView = new ProteinSearchWebPart(true);

            ActionURL url = getViewContext().cloneActionURL();
            url.deleteParameters();
            url.setPageFlow("protein");
            url.setAction("begin.view");

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
        compareMenu.setDisplayPermission(ACL.PERM_READ);

        ActionURL proteinProphetURL = new ActionURL(MS2Controller.CompareProteinProphetSetupAction.class, container);
        String selectionKey = view.getDataRegion().getSelectionKey();
        proteinProphetURL.addParameter("selectionKey", selectionKey);
        if (experimentRunIds)
        {
            proteinProphetURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("ProteinProphet", view.createVerifySelectedScript(proteinProphetURL, "runs"));

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
        compareMenu.addMenuItem("Peptide", view.createVerifySelectedScript(peptidesURL, "runs"));

        ActionURL proteinProphetQueryURL = new ActionURL(MS2Controller.CompareProteinProphetQuerySetupAction.class, container);
        if (experimentRunIds)
        {
            proteinProphetQueryURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("ProteinProphet (Query)", view.createVerifySelectedScript(proteinProphetQueryURL, "runs"));

        ActionURL spectraURL = new ActionURL(MS2Controller.SpectraCountSetupAction.class, container);
        if (experimentRunIds)
        {
            spectraURL.addParameter("experimentRunIds", "true");
        }
        compareMenu.addMenuItem("Spectra Count", view.createVerifySelectedScript(spectraURL, "runs"));
        return compareMenu;
    }

    private ButtonBar getListButtonBar(Container c, GridView view)
    {
        ButtonBar bb = new ButtonBar();

        bb.add(createCompareMenu(c, view, false));

        ActionButton compareScoring = new ActionButton("", CAPTION_SCORING_BUTTON);
        compareScoring.setScript("return verifySelected(this.form, \"" + ActionURL.toPathString("MS2-Scoring", "compare", c.getPath())+ "\", \"get\", \"runs\")");
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
        deleteRuns.setScript("return verifySelected(this.form, \"deleteRuns.view\", \"post\", \"runs\")");
        deleteRuns.setActionType(ActionButton.Action.GET);
        deleteRuns.setDisplayPermission(ACL.PERM_DELETE);
        bb.add(deleteRuns);

        return bb;
    }


    public static ActionURL getShowRunUrl(Container c, int runId)
    {
        ActionURL url = new ActionURL(ShowRunAction.class, c);
        url.addParameter(RunForm.PARAMS.run, String.valueOf(runId));
        return url;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            if (!isAuthorized(form.run))
                return null;

            ActionURL currentUrl = getViewContext().getActionURL();
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
            }

            VBox vBox = new VBox();
            JspView scriptView = new JspView<String>("/org/labkey/ms2/nestedGridScript.jsp", dataRegionName);
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
            _run = run;

            getPageConfig().setExploratoryFeatures(exploratoryFeatures);

            return vBox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendRunNavTrail(root, _run, null, getPageConfig(), "viewRuns");
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
        private FilterHeaderView(ActionURL currentUrl, RunForm form, MS2Run run) throws ServletException, SQLException
        {
            super("/org/labkey/ms2/filterHeader.jsp", new FilterHeaderBean());

            FilterHeaderBean bean = getModelBean();

            bean.run = run;
            bean.applyViewUrl = clearFilter(currentUrl).setAction("applyRunView");
            bean.applyView = renderViewSelect(0, true, ACL.PERM_READ, true);
            bean.saveViewUrl = currentUrl.clone().setAction("saveView");
            bean.manageViewsUrl = currentUrl.clone().setAction("manageViews");
            bean.pickPeptideColumnsUrl = currentUrl.clone().setAction("pickPeptideColumns");
            bean.pickProteinColumnsUrl = currentUrl.clone().setAction("pickProteinColumns");
            bean.viewTypes = MS2RunViewType.getTypesForRun(run);
            bean.currentViewType = MS2RunViewType.getViewType(form.getGrouping());
            bean.expanded = form.getExpanded();

            String chargeFilterParamName = run.getChargeFilterParamName();
            ActionURL extraFilterUrl = currentUrl.clone().setAction("addExtraFilter.post");
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


        private ActionURL clearFilter(ActionURL currentUrl)
        {
            ActionURL newUrl = currentUrl.clone();
            String run = newUrl.getParameter("run");
            newUrl.deleteParameters();
            if (null != run)
                newUrl.addParameter("run", run);
            return newUrl;
        }
    }


    public static class FilterHeaderBean
    {
        public MS2Run run;
        public ActionURL applyViewUrl;
        public StringBuilder applyView;
        public ActionURL saveViewUrl;
        public ActionURL manageViewsUrl;
        public ActionURL pickPeptideColumnsUrl;
        public ActionURL pickProteinColumnsUrl;
        public ActionURL extraFilterUrl;
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


    private Map<String, Object> getViewMap(boolean includeUser, boolean includeShared, boolean mapValue)
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


    // Render current user's MS2Views in a list box with a submit button beside.  Size == 0 is a dropdown, Size > 0 is a
    // multi-select normal list box.  postValue determines whether to post the value or the name of selected view(s)
    //
    // Caller is responsible for wrapping this in a <form> and (if desired) a <table>
    private StringBuilder renderViewSelect(int height, boolean postValue, int sharedPerm, boolean selectCurrent)
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

        String currentViewParams = getViewContext().cloneActionURL().deleteParameter("run").getRawQuery();

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


    private String modificationHref(MS2Run run)
    {
        // Need to make the pop-up window wider on SSL connections since Firefox insists on displaying the full server name
        // in the status bar and spreads out the content unnecessarily.
        int width = ("https".equals(getViewContext().getActionURL().getScheme()) ? 175 : 100);

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


    public static class RenameForm
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


    public static ActionURL getRenameRunUrl(Container c, int runId)
    {
        ActionURL url = new ActionURL(RenameRunAction.class, c);
        return url.addParameter("run", String.valueOf(runId));
    }


    @RequiresPermission(ACL.PERM_UPDATE)
    public class RenameRunAction extends FormViewAction<RenameForm>
    {
        private MS2Run _run;

        public void validateCommand(RenameForm target, Errors errors)
        {
        }

        public ModelAndView getView(RenameForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = MS2Manager.getRun(form.getRun());
            String description = form.getDescription();
            if (description == null || description.length() == 0)
                description = _run.getDescription();

            RenameBean bean = new RenameBean();
            bean.run = _run;
            bean.description = description;

            // TODO: Set focus

            return new JspView<RenameBean>("/org/labkey/ms2/renameRun.jsp", bean);
        }

        public boolean handlePost(RenameForm form, BindException errors) throws Exception
        {
            MS2Manager.renameRun(form.getRun(), form.getDescription());
            return true;
        }

        public ActionURL getSuccessURL(RenameForm form)
        {
            return getShowRunUrl(getContainer(), form.getRun());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Rename Run", getPageConfig(), null);
        }
    }


    public class RenameBean
    {
        public MS2Run run;
        public String description;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            long peptideId = form.getPeptideIdLong();
            MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

            if (peptide == null)
                return HttpView.throwNotFoundMV("Could not find peptide with RowId " + peptideId);

            int runId = peptide.getRun();

            if (!isAuthorized(runId))
                return HttpView.throwUnauthorizedMV();

            ActionURL currentUrl = getViewContext().getActionURL();

            int sqlRowIndex = form.getRowIndex();
            int rowIndex = sqlRowIndex - 1;  // Switch 1-based, JDBC row index to 0-based row index for array lookup

            MS2Run run = MS2Manager.getRun(runId);
            long[] peptideIndex = getPeptideIndex(currentUrl, run);
            rowIndex = MS2Manager.verifyRowIndex(peptideIndex, rowIndex, peptideId);

            peptide.init(form.getTolerance(), form.getxStartDouble(), form.getxEnd());

            ActionURL previousUrl = null;
            ActionURL nextUrl = null;
            ActionURL showGzUrl = null;

            // Display next and previous only if we have a cached index and a valid pointer
            if (null != peptideIndex && -1 != rowIndex)
            {
                if (0 == rowIndex)
                    previousUrl = null;
                else
                {
                    previousUrl = getViewContext().cloneActionURL();
                    previousUrl.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex - 1]));
                    previousUrl.replaceParameter("rowIndex", String.valueOf(sqlRowIndex - 1));
                }

                if (rowIndex == (peptideIndex.length - 1))
                    nextUrl = null;
                else
                {
                    nextUrl = getViewContext().cloneActionURL();
                    nextUrl.replaceParameter("peptideId", String.valueOf(peptideIndex[rowIndex + 1]));
                    nextUrl.replaceParameter("rowIndex", String.valueOf(sqlRowIndex + 1));
                }

                showGzUrl = getViewContext().cloneActionURL();
                showGzUrl.deleteParameter("seqId");
                showGzUrl.deleteParameter("rowIndex");
                showGzUrl.setAction("showGZFile");
            }

            setTitle(peptide.toString());
            getPageConfig().setTemplate(PageConfig.Template.Print);

            ShowPeptideContext ctx = new ShowPeptideContext(form, run, peptide, currentUrl, previousUrl, nextUrl, showGzUrl, modificationHref(run), getContainer(), getUser());
            return new JspView<ShowPeptideContext>("/org/labkey/ms2/showPeptide.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    private long[] getPeptideIndex(ActionURL currentUrl, MS2Run run) throws SQLException, ServletException
    {
        AbstractMS2RunView view = getPeptideView(currentUrl.getParameter("grouping"), run);
        return view.getPeptideIndex(currentUrl);
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowModificationsAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            if (!isAuthorized(form.run))
                return null;

            MS2Run run = MS2Manager.getRun(form.run);

            Map<String, String> fixed = new TreeMap<String, String>();
            Map<String, String> var = new TreeMap<String, String>();

            for (MS2Modification mod : run.getModifications())
            {
                if (mod.getVariable())
                    var.put(mod.getAminoAcid() + mod.getSymbol(), Formats.f3.format(mod.getMassDiff()));
                else
                    fixed.put(mod.getAminoAcid(), Formats.f3.format(mod.getMassDiff()));
            }

            getPageConfig().setTemplate(PageConfig.Template.Print);
            getPageConfig().setTitle("Modifications");
            getPageConfig().setMinimumWidth(100);

            ModificationBean bean = new ModificationBean();
            bean.fixed = fixed;
            bean.var = var;

            JspView view = new JspView<ModificationBean>("/org/labkey/ms2/modifications.jsp", bean);
            view.setFrame(WebPartView.FrameType.NONE);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    public static class ModificationBean
    {
        public Map<String, String> fixed;
        public Map<String, String> var;
    }


    public static ActionURL getLoadGoURL()
    {
        return new ActionURL(LoadGoAction.class);
    }


    @RequiresSiteAdmin
    public class LoadGoAction extends FormViewAction
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
            setHelpTopic(new HelpTopic("annotations", HelpTopic.Area.CPAS));
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
            protected List<TabInfo> getTabList()
            {
                return Arrays.asList(new TabInfo("Automatic", "automatic", getLoadGoURL()), new TabInfo("Manual", "manual", getLoadGoURL()));
            }

            protected HttpView getTabView(String tabId) throws Exception
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
        ActionURL url = new ActionURL(GoStatusAction.class);
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
            setHelpTopic(new HelpTopic("annotations", HelpTopic.Area.CPAS));
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


    @RequiresPermission(ACL.PERM_READ)
    public class PeptideChartsAction extends SimpleViewAction<ChartForm>
    {
        private ProteinDictionaryHelpers.GoTypes _goChartType;

        public ModelAndView getView(ChartForm form, BindException errors) throws Exception
        {
            ViewContext ctx = getViewContext();
            ActionURL queryUrl = ctx.cloneActionURL();
            String queryString = (String) ctx.get("queryString");
            queryUrl.setRawQuery(queryString);
            int runId = Integer.parseInt(queryUrl.getParameter("run"));

            if (!isAuthorized(runId))
                return null;

            MS2Run run = MS2Manager.getRun(runId);

            _goChartType = ProteinDictionaryHelpers.GTypeStringToEnum(form.getChartType());

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

            String chartTitle = "GO " + _goChartType + " Classifications";
            SQLFragment fragment = peptideView.getProteins(queryUrl, run, form);
            PieJChartHelper pjch = PieJChartHelper.prepareGOPie(chartTitle, fragment, _goChartType);
            pjch.renderAsPNG(new NullOutputStream());

            GoChartBean bean = new GoChartBean();
            bean.run = run;
            bean.chartTitle = chartTitle;
            bean.goChartType = _goChartType;
            bean.peptideFilterInfo = peptideFilterInfo;
            bean.proteinFilterInfo = proteinFilterInfo;
            bean.proteinGroupFilterInfo = proteinGroupFilterInfo;
            bean.imageMap = ImageMapUtilities.getImageMap("pie1", pjch.getChartRenderingInfo());
            bean.queryString = queryString;
            bean.grouping = form.getGrouping();
            bean.pieHelperObjName = "piechart-" + (new Random().nextInt(1000000000));
            bean.chartUrl = new ActionURL(DoOnePeptideChartAction.class, getContainer()).addParameter("ctype", _goChartType.toString()).addParameter("helpername", bean.pieHelperObjName);

            Cache.getShared().put(bean.pieHelperObjName, pjch, Cache.HOUR * 2);

            return new JspView<GoChartBean>("/org/labkey/ms2/peptideChart.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "GO " + _goChartType + " Chart", getPageConfig(), null);
        }
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
        public ActionURL chartUrl;
        public String imageMap;
        public String queryString;
        public String grouping;
    }


    @RequiresPermission(ACL.PERM_NONE)
    public class GetProteinGroupingPeptides extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            MS2Run run = MS2Manager.getRun(form.getRun());
            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), run);
            getPageConfig().setTemplate(PageConfig.Template.None);

            return peptideView.getPeptideViewForProteinGrouping(form.getProteinGroupingId(), form.getColumns());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ManageViewsAction extends SimpleViewAction
    {
        private MS2Run _run;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ActionURL runUrl = getViewContext().cloneActionURL().setAction("showRun");

            _run = MS2Manager.getRun(runUrl.getParameter("run"));
            if (null == _run)
            {
                return HttpView.throwNotFoundMV("Could not find run " + runUrl.getParameter("run"));
            }

            ActionURL postUrl = getViewContext().cloneActionURL();
            postUrl.setAction("deleteViews");
            postUrl.deleteParameter("x");
            postUrl.deleteParameter("y");

            ManageViewsBean bean = new ManageViewsBean();
            bean.postUrl = postUrl;
            bean.select = renderViewSelect(10, false, ACL.PERM_DELETE, false);

            return new JspView<ManageViewsBean>("/org/labkey/ms2/manageViews.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Manage Views", getPageConfig(), "viewRun");
        }
    }


    public static class ManageViewsBean
    {
        public ActionURL postUrl;
        public StringBuilder select;
    }


    public static class PickViewBean
    {
        public ActionURL nextUrl;
        public StringBuilder select;
        public HttpView extraOptionsView;
        public String viewInstructions;
        public int runList;
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

            ActionURL nextUrl = getViewContext().cloneActionURL().setAction("applyCompareView");
            return pickView(nextUrl, "Select a view to apply a filter to all the runs.", extraCompareOptions, runListId);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild(_description);
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ComparePeptidesSetupAction extends LegacyCompareSetupAction
    {
        public ComparePeptidesSetupAction()
        {
            super("/org/labkey/ms2/compare/comparePeptidesOptions.jsp", "Compare Peptides Setup");
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class CompareSearchEngineProteinSetupAction extends LegacyCompareSetupAction
    {
        public CompareSearchEngineProteinSetupAction()
        {
            super("/org/labkey/ms2/compare/compareSearchEngineProteinOptions.jsp", "Compare Search Engine Protein Setup");
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class CompareProteinProphetSetupAction extends LegacyCompareSetupAction
    {
        public CompareProteinProphetSetupAction()
        {
            super("/org/labkey/ms2/compare/compareProteinProphetOptions.jsp", "Compare ProteinProphet Setup");
        }
    }



    @RequiresPermission(ACL.PERM_READ)
    public class CompareProteinProphetQuerySetupAction extends AbstractRunListCreationAction<PeptideFilteringComparisonForm>
    {
        public CompareProteinProphetQuerySetupAction()
        {
            super(PeptideFilteringComparisonForm.class, false);
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors, int runListId)
        {
            QuerySettings peptidesSettings = new QuerySettings(new ActionURL(), PEPTIDES_FILTER);
            peptidesSettings.setQueryName(MS2Schema.PEPTIDES_TABLE_NAME);
            QueryView peptidesView = new QueryView(new MS2Schema(getUser(), getContainer()), peptidesSettings);

            CompareOptionsBean bean = new CompareOptionsBean(peptidesView, new ActionURL(CompareProteinProphetQueryAction.class, getContainer()), runListId, form);

            return new JspView<CompareOptionsBean>("/org/labkey/ms2/compare/compareProteinProphetQueryOptions.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare ProteinProphet (Query) Options");
        }
    }

    public static class PeptideFilteringComparisonForm extends RunListForm
    {
        private String _peptideFilterType = "none";
        private Float _peptideProphetProbability;
        private boolean _orCriteriaForEachRun;

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
            return "peptideProphet".equals(getPeptideFilterType());
        }

        public boolean isCustomViewPeptideFilter()
        {
            return "customView".equals(getPeptideFilterType());
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

        public String getCustomViewName(ViewContext context)
        {
            return context.getActionURL().getParameter(PEPTIDES_FILTER_VIEW_NAME);
        }

        public boolean isOrCriteriaForEachRun()
        {
            return _orCriteriaForEachRun;
        }

        public void setOrCriteriaForEachRun(boolean orCriteriaForEachRun)
        {
            _orCriteriaForEachRun = orCriteriaForEachRun;
        }
    }

    @RequiresPermission(ACL.PERM_NONE)
    public class OldCompareProteinProphetQueryAction extends RunListHandlerAction<PeptideFilteringComparisonForm, CompareProteinsView>
    {
        public OldCompareProteinProphetQueryAction()
        {
            super(PeptideFilteringComparisonForm.class);
        }

        protected ModelAndView getHtmlView(PeptideFilteringComparisonForm form, BindException errors) throws Exception
        {
            CompareProteinsView view = createInitializedQueryView(form, errors, false);
            if (!view.getErrors().isEmpty())
                return _renderErrors(view.getErrors());

            HtmlView helpView = new HtmlView("Comparison Details", "<div style=\"width: 800px;\"><p>To change the columns shown and set filters, use the Customize View link below. Add protein-specific columns, or expand <em>Run</em> to see the values associated with individual runs, like probability. To set a filter, select the Filter tab, add column, and filter it based on the desired threshold.</p></div>");

            Map<String, String> props = new HashMap<String, String>();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put("comparisonName", view.getComparisonName());
            GWTView gwtView = new GWTView("org.labkey.ms2.MS2VennDiagramView", props);
            gwtView.setTitle("Comparison Overview");
            gwtView.setFrame(WebPartView.FrameType.PORTAL);
            gwtView.enableExpandCollapse("ProteinProphetQueryCompare", true);

            return new VBox(gwtView, helpView, view);
        }

        protected CompareProteinsView createQueryView(PeptideFilteringComparisonForm form, BindException errors, boolean forExport) throws Exception
        {
            String viewName = form.getCustomViewName(getViewContext());
            return new CompareProteinsView(getViewContext(), form.getRunList(), forExport, viewName);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare ProteinProphet (OldQuery)");
        }
    }

    @RequiresPermission(ACL.PERM_NONE)
    public class CompareProteinProphetQueryAction extends RunListHandlerAction<PeptideFilteringComparisonForm, ProteinProphetCrosstabView>
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
            ProteinProphetCrosstabView view = createInitializedQueryView(form, errors, false);
//            if (!view.getErrors().isEmpty())
//                return _renderErrors(view.getErrors());

            HtmlView helpView = new HtmlView("Comparison Details", "<div style=\"width: 800px;\"><p>To change the columns shown and set filters, use the Customize View link below. Add protein-specific columns, or expand <em>Run</em> to see the values associated with individual runs, like probability. To set a filter, select the Filter tab, add column, and filter it based on the desired threshold.</p></div>");

            Map<String, String> props = new HashMap<String, String>();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put("comparisonName", "ProteinProphetCrosstab");
            GWTView gwtView = new GWTView(org.labkey.ms2.client.MS2VennDiagramView.class, props);
            gwtView.setTitle("Comparison Overview");
            gwtView.setFrame(WebPartView.FrameType.PORTAL);
            gwtView.enableExpandCollapse("ProteinProphetQueryCompare", true);

            return new VBox(gwtView, helpView, view);
        }

        protected ProteinProphetCrosstabView createQueryView(PeptideFilteringComparisonForm form, BindException errors, boolean forExport) throws Exception
        {
            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList(), false, getViewContext());
            schema.setRuns(runs);
            return new ProteinProphetCrosstabView(schema, form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL setupURL = new ActionURL(CompareProteinProphetQuerySetupAction.class, getContainer());
            setupURL.addParameter("peptideFilterType", _form.getPeptideFilterType());
            if (_form.getPeptideProphetProbability() != null)
            {
                setupURL.addParameter("peptideProphetProbability", _form.getPeptideProphetProbability().toString());
            }
            setupURL.addParameter("runList", _form.getRunList());
            root.addChild("MS2 Dashboard");
            root.addChild("Setup Compare ProteinProphet", setupURL);
            return root.addChild("Compare ProteinProphet (Query)");
        }
    }


    // extraFormHtml gets inserted between the view dropdown and the button.
    private HttpView pickView(ActionURL nextUrl, String viewInstructions, HttpView embeddedView, int runListId)
    {
        JspView<PickViewBean> pickView = new JspView<PickViewBean>("/org/labkey/ms2/pickView.jsp", new PickViewBean());

        PickViewBean bean = pickView.getModelBean();

        nextUrl.deleteFilterParameters("button");
        nextUrl.deleteFilterParameters("button.x");
        nextUrl.deleteFilterParameters("button.y");

        bean.nextUrl = nextUrl;
        bean.select = renderViewSelect(0, true, ACL.PERM_READ, false);
        bean.extraOptionsView = embeddedView;
        bean.viewInstructions = viewInstructions;
        bean.runList = runListId;

        return pickView;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class PickExportRunsView extends AbstractRunListCreationAction<RunListForm>
    {
        public PickExportRunsView()
        {
            super(RunListForm.class, true);
        }

        public ModelAndView getView(RunListForm form, BindException errors, int runListId)
        {
            JspView extraExportView = new JspView("/org/labkey/ms2/extraExportOptions.jsp");
            return pickView(getViewContext().cloneActionURL().setAction("applyExportRunsView"), "Select a view to apply a filter to all the runs and to indicate what columns to export.", extraExportView, runListId);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Export Runs", getPageConfig(), "exportRuns");
        }
    }


    @RequiresPermission(ACL.PERM_INSERT)
    public class MoveRunsAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ActionURL currentUrl = getViewContext().cloneActionURL();
            String moveRuns = currentUrl.getParameter("moveRuns");
            String[] idStrings = moveRuns.split(",");
            List<Integer> ids = new ArrayList<Integer>();
            for (String idString : idStrings)
            {
                ids.add(new Integer(idString));
            }
            List<MS2Run> runs = MS2Manager.lookupRuns(ids, false, getUser());
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

            return currentUrl;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportRunsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            List<MS2Run> runs;
            try
            {
                runs = RunListCache.getCachedRuns(form.getRunList(), true, getViewContext());
            }
            catch (RunListException e)
            {
                _renderErrors(e.getMessages());  // TODO: throw
                return;
            }


            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), runs.toArray(new MS2Run[runs.size()]));
            ActionURL currentUrl = getViewContext().cloneActionURL();
            SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

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
                    exportSpectra(runs, currentUrl, peptideFilter, form.getExportFormat().toLowerCase());
                else
                    exportProteinsAsSpectra(runs, currentUrl, form.getExportFormat().toLowerCase(), peptideView, null);
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


    private void exportSpectra(List<MS2Run> runs, ActionURL currentUrl, SimpleFilter filter, String extension) throws IOException
    {
        Sort sort = ProteinManager.getPeptideBaseSort();
        sort.applyURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        SpectrumIterator iter = new ResultSetSpectrumIterator(runs, filter, sort);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowCompareAction extends SimpleViewAction<ExportForm>
    {
        private StringBuilder _title = new StringBuilder();

        public ModelAndView getView(ExportForm form, BindException errors) throws Exception
        {
            return compareRuns(form.getRunList(), false, _title, form.getColumn());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, _title.toString(), getPageConfig(), "compareRuns");
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportCompareToExcel extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            compareRuns(form.getRunList(), true, null, form.getColumn());
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

    public static class SpectraCountForm extends RunListForm
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
    
    @RequiresPermission(ACL.PERM_READ)
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
            List<String> errorMessages = new ArrayList<String>();
            ActionURL currentURL = getViewContext().getActionURL();
            int runListId;
            if (form.getRunList() == null)
            {
                try
                {
                    runListId = RunListCache.cacheSelectedRuns(_requiresSameType, form, getViewContext());
                    ActionURL redirectURL = currentURL.clone();
                    redirectURL.addParameter("runList", Integer.toString(runListId));
                    HttpView.throwRedirect(redirectURL);
                }
                catch (RunListException e)
                {
                    return _renderErrors(e.getMessages());
                }
            }
            else
            {
                runListId = form.getRunList().intValue();
            }

            if (!errorMessages.isEmpty())
                return _renderErrors(errorMessages);

            return getView(form, errors, runListId);
        }

        protected abstract ModelAndView getView(FormType form, BindException errors, int runListId);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class SpectraCountSetupAction extends AbstractRunListCreationAction<PeptideFilteringComparisonForm>
    {
        public SpectraCountSetupAction()
        {
            super(PeptideFilteringComparisonForm.class, false);
        }

        public ModelAndView getView(PeptideFilteringComparisonForm form, BindException errors, int runListId)
        {
            QuerySettings spectraCountSettings = new QuerySettings(new ActionURL(), PEPTIDES_FILTER);
            spectraCountSettings.setQueryName(MS2Schema.PEPTIDES_TABLE_NAME);
            QueryView spectraCountView = new QueryView(new MS2Schema(getUser(), getContainer()), spectraCountSettings);

            CompareOptionsBean bean = new CompareOptionsBean(spectraCountView, new ActionURL(SpectraCountAction.class, getContainer()), runListId, form);

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
            try
            {
                _runs = RunListCache.getCachedRuns(form.getRunList(), false, getViewContext());
            }
            catch (RunListException e)
            {
                return _renderErrors(e.getMessages());
            }
            return super.getView(form, errors);
        }
    }

    @RequiresPermission(ACL.PERM_NONE)
    public class SpectraCountAction extends RunListHandlerAction<SpectraCountForm, QueryView>
    {
        public SpectraCountAction()
        {
            super(SpectraCountForm.class);
        }

        protected QueryView createQueryView(SpectraCountForm form, BindException errors, boolean forExport) throws Exception
        {
            QuerySettings settings = new QuerySettings(getViewContext().getActionURL(), "SpectraCount");
            settings.setAllowChooseQuery(false);
            final SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(form.getSpectraConfig());
            if (config == null)
            {
                HttpView.throwNotFound("Could not find spectra count config: " + form.getSpectraConfig());
            }

            String viewName = getViewContext().getActionURL().getParameter(PEPTIDES_FILTER_VIEW_NAME);
            if ("".equals(viewName))
            {
                viewName = null;
            }
            MS2Schema schema = new MS2Schema(getUser(), getContainer());

            settings.setQueryName(config.getTableName());

            schema.setRuns(_runs);
            QueryView view = new SpectraCountQueryView(schema, settings, config, viewName, form.getRunList().intValue());
            view.setShowRReportButton(true);
            view.setTitle("Spectra Counts");
            // ExcelWebQueries won't be part of the same HTTP session so we won't have access to the run list anymore
            view.setAllowExcelWebQuery(false);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }

    }

    private ModelAndView compareRuns(int runListIndex, boolean exportToExcel, StringBuilder title, String column) throws ServletException, SQLException
    {
        ActionURL currentUrl = getViewContext().getActionURL();

        List<MS2Run> runs;
        try
        {
            runs = RunListCache.getCachedRuns(runListIndex, false, getViewContext());
        }
        catch (RunListException e)
        {
            return _renderErrors(e.getMessages());
        }

        for (MS2Run run : runs)
        {
            Container c = ContainerManager.getForId(run.getContainer());
            if (c == null || !c.hasPermission(getUser(), ACL.PERM_READ))
            {
                return HttpView.throwUnauthorizedMV();
            }
        }

        CompareQuery query = CompareQuery.getCompareQuery(column, currentUrl, runs);
        if (query == null)
            return _renderError("You must specify a comparison type");

        List<String> errors = new ArrayList<String>();
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
            ew.write(getViewContext().getResponse());
        }
        else
        {
            rgn.setOffset(offset);
            rgn.setColSpan(query.getColumnsPerRun());
            rgn.setMultiColumnCaptions(runCaptions);

            HttpView filterView = new CurrentFilterView(query);

            GridView compareView = new GridView(rgn);
            rgn.setShowPagination(false);
            compareView.setResultSet(rgn.getResultSet());

            title.append(query.getComparisonDescription());

            return new VBox(filterView, compareView);
        }

        return null;
    }


    @RequiresPermission(ACL.PERM_READ)
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
        public void export(Object o, HttpServletResponse response) throws Exception
        {
            TableInfo tinfo = MS2Manager.getTableInfoHistory();
            ExcelWriter ew = new ExcelWriter(MS2Manager.getSchema(), "SELECT * FROM " + MS2Manager.getTableInfoHistory() + " ORDER BY Date");
            ew.setColumns(tinfo.getColumns());
            ew.setSheetName("MS2 History");
            ew.write(response);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

            if (null != peptide)
            {
                if (!isAuthorized(peptide.getRun()))
                    return;

                response.setDateHeader("Expires", System.currentTimeMillis() + DateUtils.MILLIS_PER_HOUR);
                response.setHeader("Pragma", "");
                response.setContentType("image/png");
                peptide.renderGraph(response, form.getTolerance(), form.getxStartDouble(), form.getxEnd(), form.getWidth(), form.getHeight());
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
            HttpServletRequest request = getViewContext().getRequest();
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


    @RequiresSiteAdmin
    public class ShowProteinAdminAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            GridView grid = getFastaAdminGrid();
            grid.setTitle("FASTA Files");

            grid.getViewContext().setPermissions(ACL.PERM_READ);

            GridView annots = new GridView(getAnnotInsertsGrid());
            annots.setTitle("Protein Annotations Loaded");

            annots.getViewContext().setPermissions(ACL.PERM_READ);

            return new VBox(grid, annots);
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
            ActionURL showUrl = getViewContext().cloneActionURL();
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

            ActionButton insertAnnots = new ActionButton(new ActionURL(InsertAnnotsAction.class, getContainer()), "Load New Annot File");
            insertAnnots.setActionType(ActionButton.Action.LINK);
            bb.add(insertAnnots);

            bb.add(new ActionButton("reloadSPOM.post", "Reload SWP Org Map"));

            ActionButton reloadGO = new ActionButton("loadGo.view", (GoLoader.isGoLoaded().booleanValue() ? "Reload" : "Load") + " GO");
            reloadGO.setActionType(ActionButton.Action.LINK);
            bb.add(reloadGO);

            rgn.setButtonBar(bb);
            return rgn;
        }

        private GridView getFastaAdminGrid()
        {
            DataRegion rgn = new DataRegion();
            rgn.setColumns(ProteinManager.getTableInfoFastaAdmin().getColumns("FileName, Loaded, FastaId, Runs"));
            String runsUrl = ActionURL.toPathString("MS2", "showAllRuns", (String)null) + "?" + MS2Manager.getDataRegionNameRuns() + ".FastaId~eq=${FastaId}";
            rgn.getDisplayColumn("Runs").setURL(runsUrl);
            rgn.setFixedWidthColumns(false);
            rgn.setShowRecordSelectors(true);

            GridView result = new GridView(rgn);
            result.getRenderContext().setBaseSort(new Sort("FastaId"));

            ButtonBar bb = new ButtonBar();

            ActionButton delete = new ActionButton("", "Delete");
            delete.setScript(result.createVerifySelectedScript(new ActionURL(DeleteDataBasesAction.class, getContainer()), "FASTA files"));
            delete.setActionType(ActionButton.Action.GET);
            bb.add(delete);

            ActionButton reload = new ActionButton("button", "Reload FASTA");
            reload.setScript(result.createVerifySelectedScript(new ActionURL(ReloadFastaAction.class, getContainer()), "FASTA files"));
            reload.setActionType(ActionButton.Action.GET);
            bb.add(reload);

            MenuButton setBestNameMenu = new MenuButton("Set Protein BestName...");
            ActionURL setBestNameURL = new ActionURL(SetBestNameAction.class, getContainer());

            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.LOOKUP_STRING.toString());
            setBestNameMenu.addMenuItem("to name from FASTA", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.IPI.toString());
            setBestNameMenu.addMenuItem("to IPI (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT.toString());
            setBestNameMenu.addMenuItem("to SwissProt (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT_ACCN.toString());
            setBestNameMenu.addMenuItem("to SwissProtAccn (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));

            bb.add(setBestNameMenu);

            rgn.setButtonBar(bb, DataRegion.MODE_GRID);
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setTitle("Protein Database Admin");  // TODO: Admin nav trail
            return root;
        }
    }

    public static class SetBestNameForm
    {
        private enum NameType
        { LOOKUP_STRING, IPI, SWISS_PROT, SWISS_PROT_ACCN };

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

    @RequiresPermission(ACL.PERM_ADMIN)
    public class SetBestNameAction extends FormHandlerAction<SetBestNameForm>
    {
        public void validateCommand(SetBestNameForm form, Errors errors)
        {
        }

        public boolean handlePost(SetBestNameForm form, BindException errors) throws Exception
        {
            int[] fastaIds = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));

            for (int fastaId : fastaIds)
            {
                SQLFragment identifierSQL;
                switch (form.lookupNameType())
                {
                    case IPI:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.IPI + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case SWISS_PROT:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.SwissProt + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case SWISS_PROT_ACCN:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.SwissProtAccn + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case LOOKUP_STRING:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(fs.LookupString) FROM " + ProteinManager.getTableInfoFastaSequences() + " fs ");
                        identifierSQL.append(" WHERE fs.SeqId = " + ProteinManager.getTableInfoSequences() + ".SeqId");
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected NameType: " + form.lookupNameType());
                }

                SQLFragment sql = new SQLFragment("UPDATE " + ProteinManager.getTableInfoSequences() + " SET BestName = (");
                sql.append(identifierSQL);
                sql.append(") WHERE " + ProteinManager.getTableInfoSequences() + ".SeqId IN (SELECT fs.SeqId FROM " + ProteinManager.getTableInfoFastaSequences() + " fs WHERE FastaId = " + fastaId + ") AND ");
                sql.append("(");
                sql.append(identifierSQL);
                sql.append(") IS NOT NULL");
                Table.execute(ProteinManager.getSchema(), sql);
            }
            return true;
        }

        public ActionURL getSuccessURL(SetBestNameForm form)
        {
            return MS2UrlsImpl.get().getShowProteinAdminUrl();
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportSelectedProteinGroupsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            ViewContext ctx = getViewContext();
            List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

            exportProteinGroups(response, form, proteins);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportProteinGroupsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            if (isAuthorized(form.run))
                exportProteinGroups(response, form, null);
        }
    }


    private void exportProteinGroups(HttpServletResponse response, ExportForm form, List<String> proteins) throws Exception
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
            exportProteinsAsSpectra(Arrays.asList(run), getViewContext().getActionURL(), form.getExportFormat().toLowerCase(), peptideView, where);
        }
    }

    private void exportProteinsAsSpectra(List<MS2Run> runs, ActionURL currentUrl, String extension, AbstractMS2RunView peptideView, String where) throws IOException
    {
        SpectrumIterator iter = new ProteinResultSetSpectrumIterator(runs, currentUrl, peptideView, where);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();
    }


    private void exportProteins(ExportForm form, HttpServletResponse response, String extraWhere, List<String> proteins) throws Exception
    {
        MS2Run run = MS2Manager.getRun(form.getRun());
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
            exportProteinsAsSpectra(Arrays.asList(run), getViewContext().getActionURL(), form.getExportFormat().toLowerCase(), peptideView, extraWhere);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportAllProteinsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            if (isAuthorized(form.run))
                exportProteins(form, response, null, null);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportSelectedProteinsAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            ViewContext ctx = getViewContext();
            List<String> proteins = ctx.getList(DataRegion.SELECT_CHECKBOX_NAME);

            if (null != proteins)
            {
                StringBuffer where = new StringBuffer("Protein IN (");

                for (int i = 0; i < Math.min(proteins.size(), ExcelWriter.MAX_ROWS); i++)
                {
                    // Escape all single quotes in the protein names
                    // TODO: Use (?, ?, ...) and JDBC parameters instead -- use IN FilterClause
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


    @RequiresPermission(ACL.PERM_READ)
    public class DoProteinSearchAction extends SimpleViewAction<ProteinSearchForm>
    {
        public ModelAndView getView(ProteinSearchForm form, BindException errors) throws Exception
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
                HttpView.throwRedirect(url + "&" + filter.toQueryString("ProteinSearchResults"));
            }

            QueryView proteinsView = createProteinSearchView(form);
            QueryView groupsView = createProteinGroupSearchView(form);

            ProteinSearchWebPart searchView = new ProteinSearchWebPart(true);
            searchView.getModelBean().setIdentifier(form.getIdentifier());
            searchView.getModelBean().setIncludeSubfolders(form.isIncludeSubfolders());
            searchView.getModelBean().setExactMatch(form.isExactMatch());
            searchView.getModelBean().setRestrictProteins(form.isRestrictProteins());
            if (getViewContext().getRequest().getParameter("ProteinSearchResults.GroupProbability~gte") != null)
            {
                try
                {
                    searchView.getModelBean().setMinProbability(Float.parseFloat(request.getParameter("ProteinSearchResults.GroupProbability~gte")));
                }
                catch (NumberFormatException e) {}
            }
            if (request.getParameter("ProteinSearchResults.ErrorRate~lte") != null)
            {
                try
                {
                    searchView.getModelBean().setMaxErrorRate(Float.parseFloat(request.getParameter("ProteinSearchResults.ErrorRate~lte")));
                }
                catch (NumberFormatException e) {}
            }

            return new VBox(searchView, proteinsView, groupsView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Protein Search Results", getPageConfig(), "proteinSearch");
        }
    }


    private QueryView createProteinGroupSearchView(final ProteinSearchForm form) throws ServletException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);
        QuerySettings groupsSettings = new QuerySettings(getViewContext().getActionURL(), "ProteinSearchResults");
        groupsSettings.setSchemaName(schema.getSchemaName());
        groupsSettings.setQueryName(MS2Schema.PROTEIN_GROUPS_FOR_SEARCH_TABLE_NAME);
        groupsSettings.setAllowChooseQuery(false);
        QueryView groupsView = new QueryView(schema, groupsSettings)
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

                ActionURL excelURL = getViewContext().cloneActionURL();
                excelURL.setAction("exportProteinGroupSearchToExcel.view");
                ActionButton excelButton = new ActionButton("Export to Excel", excelURL);
                bar.add(excelButton);

                ActionURL tsvURL = getViewContext().cloneActionURL();
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
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);
        QuerySettings proteinsSettings = schema.getSettings(getViewContext().getActionURL(), "PotentialProteins");
        proteinsSettings.setQueryName(MS2Schema.SEQUENCES_TABLE_NAME);
        proteinsSettings.setAllowChooseQuery(false);
        QueryView proteinsView = new QueryView(schema, proteinsSettings)
        {
            protected void populateButtonBar(DataView view, ButtonBar bar)
            {
                super.populateButtonBar(view, bar);

                ActionURL excelURL = getViewContext().cloneActionURL();
                excelURL.setAction("exportProteinSearchToExcel.view");
                ActionButton excelButton = new ActionButton("Export to Excel", excelURL);
                bar.add(excelButton);

                ActionURL tsvURL = getViewContext().cloneActionURL();
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
        if (form.isRestrictProteins())
        {
            sequencesTableInfo.addContainerCondition(getContainer(), getUser(), true);
        }
        proteinsView.setTitle("Matching Proteins");
        return proteinsView;
    }


    public static class ProteinSearchForm
    {
        private String _identifier;
        private Float _minimumProbability;
        private Float _maximumErrorRate;
        private boolean _includeSubfolders;
        private boolean _exactMatch;
        private boolean _restrictProteins;

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

        public boolean isRestrictProteins()
        {
            return _restrictProteins;
        }

        public void setRestrictProteins(boolean restrictProteins)
        {
            _restrictProteins = restrictProteins;
        }
    }


    @RequiresPermission(ACL.PERM_NONE)      // TODO: Check this
    public class ExportProteinSearchToExcel extends ExportAction<ProteinSearchForm>
    {
        public void export(ProteinSearchForm form, HttpServletResponse response) throws Exception
        {
            QueryView view = createProteinSearchView(form);
            ExcelWriter excelWriter = view.getExcelWriter();
            excelWriter.setFilenamePrefix("ProteinSearchResults");
            excelWriter.write(response);
        }
    }


    @RequiresPermission(ACL.PERM_NONE)      // TODO: Check this
    public class ExportProteinSearchToTSVAction extends ExportAction<ProteinSearchForm>
    {
        public void export(ProteinSearchForm form, HttpServletResponse response) throws Exception
        {
            QueryView view = createProteinSearchView(form);
            TSVGridWriter tsvWriter = view.getTsvWriter();
            tsvWriter.setFilenamePrefix("ProteinSearchResults");
            tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
            tsvWriter.write(response);
        }
    }


    @RequiresPermission(ACL.PERM_NONE)      // TODO: Check this
    public class ExportProteinGroupSearchToExcelAction extends ExportAction<ProteinSearchForm>
    {
        public void export(ProteinSearchForm form, HttpServletResponse response) throws Exception
        {
            QueryView view = createProteinGroupSearchView(form);
            ExcelWriter excelWriter = view.getExcelWriter();
            excelWriter.setFilenamePrefix("ProteinGroupSearchResults");
            excelWriter.write(response);
        }
    }


    @RequiresPermission(ACL.PERM_NONE)      // TODO: Check this
    public class ExportProteinGroupSearchToTSVAction extends ExportAction<ProteinSearchForm>
    {
        public void export(ProteinSearchForm form, HttpServletResponse response) throws Exception
        {
            QueryView view = createProteinGroupSearchView(form);
            TSVGridWriter tsvWriter = view.getTsvWriter();
            tsvWriter.setFilenamePrefix("ProteinGroupSearchResults");
            tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
            tsvWriter.write(response);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportAllPeptidesAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportPeptides(form, response, false);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportSelectedPeptidesAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportPeptides(form, response, true);
        }
    }


    private void exportPeptides(ExportForm form, HttpServletResponse response, boolean selected) throws Exception
    {
        if (!isAuthorized(form.run))
            return;

        MS2Run run = MS2Manager.getRun(form.run);

        ActionURL currentUrl = getViewContext().getActionURL();
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
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER, run));

        if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
            exportSpectra(Arrays.asList(run), currentUrl, baseFilter, form.getExportFormat().toLowerCase());
    }


    @Deprecated
    private HttpView _renderErrors(List<String> messages) throws ServletException
    {
        getViewContext().requiresPermission(ACL.PERM_READ);
        StringBuilder sb = new StringBuilder("<table class=\"DataRegion\">");

        for (String message : messages)
        {
            sb.append("<tr><td>");
            sb.append(message);
            sb.append("</td></tr>");
        }
        sb.append("</table>");
        HtmlView view = new HtmlView(sb.toString());
        return view;
    }


    @Deprecated
    private HttpView _renderError(String message) throws ServletException
    {
        return _renderErrors(Arrays.asList(message));
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


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideProphetDistributionPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            PeptideProphetGraphs.renderDistribution(response, summary, form.charge, form.cumulative);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideProphetObservedVsModelPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            PeptideProphetGraphs.renderObservedVsModel(response, summary, form.charge, form.cumulative);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideProphetObservedVsPPScorePlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            PeptideProphetGraphs.renderObservedVsPPScore(response, getContainer(), form.run, form.charge, form.cumulative);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideProphetSensitivityPlotAction extends ExportAction<PeptideProphetForm>
    {
        public void export(PeptideProphetForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            PeptideProphetGraphs.renderSensitivityGraph(response, summary);
        }
    }


    public static class PeptideProphetForm extends RunForm
    {
        private int charge;
        private boolean cumulative = false;

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


    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeptideProphetDetailsAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            if (!isAuthorized(form.run))
                return null;

            _run = MS2Manager.getRun(form.run);

            PeptideProphetSummary summary = MS2Manager.getPeptideProphetSummary(form.run);

            return new JspView<PeptideProphetDetailsBean>("/org/labkey/ms2/showPeptideProphetDetails.jsp", new PeptideProphetDetailsBean(_run, summary, "showPeptideProphetSensitivityPlot.view"));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Peptide Prophet Details", getPageConfig(), null);
        }
    }


    public static class PeptideProphetDetailsBean
    {
        public MS2Run run;
        public SensitivitySummary summary;
        public String action;

        public PeptideProphetDetailsBean(MS2Run run, SensitivitySummary summary, String action)
        {
            this.run = run;
            this.summary = summary;
            this.action = action;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowProteinProphetSensitivityPlotAction extends ExportAction<RunForm>
    {
        public void export(RunForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);

            PeptideProphetGraphs.renderSensitivityGraph(response, summary);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowProteinProphetDetailsAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            if (!isAuthorized(form.run))
                return null;

            _run = MS2Manager.getRun(form.run);
            ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);
            return new JspView<PeptideProphetDetailsBean>("/org/labkey/ms2/showSensitivityDetails.jsp", new PeptideProphetDetailsBean(_run, summary, "showProteinProphetSensitivityPlot.view"));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Protein Prophet Details", getPageConfig(), null);
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
        ActionURL url = new ActionURL(ShowMS2AdminAction.class);

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
            bean.successfulUrl = showRunsUrl(false, 1);
            bean.inProcessUrl = showRunsUrl(false, 0);
            bean.failedUrl = showRunsUrl(false, 2);
            bean.deletedUrl = showRunsUrl(true, null);

            return new JspView<MS2AdminBean>("/org/labkey/ms2/ms2Admin.jsp", bean);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("MS2 Admin");  // TODO: Admin trail
            return root;
        }
    }


    private ActionURL showRunsUrl(Boolean deleted, Integer statusId)
    {
        ActionURL url = new ActionURL("MS2", "showAllRuns", (String)null);

        if (null != deleted)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".Deleted~eq", deleted.booleanValue() ? "1" : "0");

        if (null != statusId)
            url.addParameter(MS2Manager.getDataRegionNameRuns() + ".StatusId~eq", String.valueOf(statusId));

        return url;
    }


    public static class MS2AdminBean
    {
        public ActionURL successfulUrl;
        public ActionURL inProcessUrl;
        public ActionURL failedUrl;
        public ActionURL deletedUrl;
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


    @RequiresPermission(ACL.PERM_NONE)
    public class ShowAllRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            DataRegion rgn = new DataRegion();
            rgn.setName(MS2Manager.getDataRegionNameRuns());
            ContainerDisplayColumn cdc = new ContainerDisplayColumn(MS2Manager.getTableInfoRuns().getColumn("Container"));
            cdc.setCaption("Folder");

            ActionURL containerURL = getViewContext().cloneActionURL().setAction("showList");

            // We don't want ActionURL to encode ${ContainerPath}, so set a dummy value and use string substitution
            String urlString = containerURL.setExtraPath("ContainerPath").getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/");
            cdc.setURL(urlString);
            rgn.addColumn(cdc);

            DataColumn descriptionColumn = new DataColumn(MS2Manager.getTableInfoRuns().getColumn("Description")) {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    if (null != ctx.get("ContainerPath"))
                        super.renderGridCellContents(ctx, out);
                    else
                        out.write(getFormattedValue(ctx));
                }
            };
            ActionURL showRunUrl = new ActionURL("MS2", "showRun", "ContainerPath");
            String showUrlString = showRunUrl.getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/") + "run=${Run}";
            descriptionColumn.setURL(showUrlString);
            rgn.addColumn(descriptionColumn);

            rgn.addColumns(MS2Manager.getTableInfoRuns().getColumns("Path, Created, Deleted, StatusId, Status, PeptideCount, SpectrumCount, FastaId"));

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

            setTitle("Show All Runs");

            return gridView;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;  // TODO: admin navtrail
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class SavePeptideColumnsAction extends RedirectAction<ColumnForm>
    {
        ActionURL _returnURL;

        public ActionURL getSuccessURL(ColumnForm columnForm)
        {
            return _returnURL;
        }

        public boolean doAction(ColumnForm form, BindException errors) throws Exception
        {
            _returnURL = getViewContext().cloneActionURL();
            _returnURL.setAction("showRun");
            _returnURL.setRawQuery(form.getQueryString());
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
                    HttpView.throwNotFoundMV("Could not find run with id " + _returnURL.getParameter("run"));
                }
                AbstractMS2RunView view = getPeptideView(_returnURL.getParameter("grouping"), run);
                view.savePeptideColumnNames(run.getType(), columnNames);
            }
            else
                _returnURL.replaceParameter("columns", columnNames);

            return true;
        }

        public void validateCommand(ColumnForm target, Errors errors)
        {
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class SaveProteinColumnsAction extends RedirectAction<ColumnForm>
    {
        private ActionURL _returnUrl;

        public ActionURL getSuccessURL(ColumnForm columnForm)
        {
            return _returnUrl;
        }

        public boolean doAction(ColumnForm form, BindException errors) throws Exception
        {
            _returnUrl = getViewContext().cloneActionURL();
            _returnUrl.setAction("showRun");
            _returnUrl.setRawQuery(form.getQueryString());
            String columnNames = form.getColumns();
            if (columnNames == null)
            {
                columnNames = "";
            }
            columnNames = columnNames.replaceAll(" ", "");

            if (form.getSaveDefault())
            {
                MS2Run run = MS2Manager.getRun(_returnUrl.getParameter("run"));
                AbstractMS2RunView view = getPeptideView(_returnUrl.getParameter("grouping"), run);
                view.saveProteinColumnNames(run.getType(), columnNames);
            }
            else
                _returnUrl.replaceParameter("proteinColumns", columnNames);

            return true;
        }

        public void validateCommand(ColumnForm target, Errors errors)
        {
        }
    }


    public static class ColumnForm
    {
        private boolean saveDefault = false;
        private String queryString;
        private String columns;

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


    @RequiresPermission(ACL.PERM_READ)
    public class PickPeptideColumnsAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            ActionURL url = getViewContext().cloneActionURL();
            _run = MS2Manager.getRun(form.run);
            if (_run == null)
            {
                return HttpView.throwNotFoundMV("Could not find run");
            }

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), _run);

            JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickPeptideColumns.jsp", new PickColumnsBean());
            PickColumnsBean bean = pickColumns.getModelBean();
            bean.commonColumns = _run.getCommonPeptideColumnNames();
            bean.proteinProphetColumns = _run.getProteinProphetPeptideColumnNames();
            bean.quantitationColumns = _run.getQuantitationPeptideColumnNames();

            // Put a space between each name
            bean.defaultColumns = peptideView.getPeptideColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
            bean.currentColumns = peptideView.getPeptideColumnNames(form.getColumns()).replaceAll(" ", "").replaceAll(",", ", ");

            url.deleteParameter("columns");

            bean.queryString = url.getRawQuery();
            url.deleteParameters().setAction("savePeptideColumns");
            bean.saveUrl = url;
            bean.saveDefaultUrl = url.clone().addParameter("saveDefault", "1");

            return pickColumns;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendRunNavTrail(root, _run, "Pick Peptide Columns", getPageConfig(), "pickPeptideColumns");
            return root;
        }
    }


    public static class PickColumnsBean
    {
        public String commonColumns;
        public String proteinProphetColumns;
        public String quantitationColumns;
        public String defaultColumns;
        public String currentColumns;
        public String queryString;
        public ActionURL saveUrl;
        public ActionURL saveDefaultUrl;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class PickProteinColumnsAction extends SimpleViewAction<RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            // TODO: NavTrail URL: cloneActionURL().setAction("showRun")
            ActionURL url = getViewContext().cloneActionURL();
            _run = MS2Manager.getRun(form.run);
            if (_run == null)
            {
                return HttpView.throwNotFoundMV("Could not find run " + form.run);
            }

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), _run);

            JspView<PickColumnsBean> pickColumns = new JspView<PickColumnsBean>("/org/labkey/ms2/pickProteinColumns.jsp", new PickColumnsBean());
            PickColumnsBean bean = pickColumns.getModelBean();

            bean.commonColumns = MS2Run.getCommonProteinColumnNames();
            bean.proteinProphetColumns = MS2Run.getProteinProphetProteinColumnNames();
            bean.quantitationColumns = _run.getQuantitationProteinColumnNames();

            // Put a space between each name
            bean.defaultColumns = peptideView.getProteinColumnNames(null).replaceAll(" ", "").replaceAll(",", ", ");
            bean.currentColumns = peptideView.getProteinColumnNames(form.getProteinColumns()).replaceAll(" ", "").replaceAll(",", ", ");

            url.deleteParameter("proteinColumns");

            bean.queryString = url.getRawQuery();
            url.deleteParameters().setAction("saveProteinColumns");
            bean.saveUrl = url;
            bean.saveDefaultUrl = url.clone().addParameter("saveDefault", "1");

            return pickColumns;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Pick Protein Columns", getPageConfig(), "pickProteinColumns");
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


    @RequiresPermission(ACL.PERM_READ)
    public class SaveViewAction extends FormViewAction<MS2ViewForm>
    {
        private MS2Run _run;

        public void validateCommand(MS2ViewForm target, Errors errors)
        {
        }

        public ModelAndView getView(MS2ViewForm form, boolean reshow, BindException errors) throws Exception
        {
            if (!isAuthorized(form.getRun()))
            {
                return null;
            }

            JspView<SaveViewBean> saveView = new JspView<SaveViewBean>("/org/labkey/ms2/saveView.jsp", new SaveViewBean());
            SaveViewBean bean = saveView.getModelBean();
            bean.returnUrl = getViewContext().cloneActionURL().setAction("showRun");
            bean.canShare = getContainer().hasPermission(getUser(), ACL.PERM_INSERT);

            ActionURL newUrl = bean.returnUrl.clone().deleteParameter("run");
            bean.viewParams = newUrl.getRawQuery();

            _run = MS2Manager.getRun(form.getRun());

            return saveView;
        }

        public boolean handlePost(MS2ViewForm form, BindException errors) throws Exception
        {
            String viewParams = (null == form.getViewParams() ? "" : form.getViewParams());

            String name = form.name;
            PropertyManager.PropertyMap m;
            if (form.isShared() && getContainer().hasPermission(getUser(), ACL.PERM_INSERT))
                m = PropertyManager.getWritableProperties(0, getContainer().getId(), MS2_VIEWS_CATEGORY, true);
            else
                m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

            m.put(name, viewParams);
            PropertyManager.saveProperties(m);

            return true;
        }

        public ActionURL getSuccessURL(MS2ViewForm form)
        {
            return new ActionURL(form.getReturnUrl());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, "Save View", getPageConfig(), "viewRun");
        }
    }


    public static class SaveViewBean
    {
        public ActionURL returnUrl;
        public boolean canShare;
        public String viewParams;
    }


    public static class MS2ViewForm
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
            return run;
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


    @RequiresPermission(ACL.PERM_READ)
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
            else if (form.getPeptideIdLong() != 0)
            {
                MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
                if (peptide != null)
                {
                    runId = peptide.getRun();
                    seqId = peptide.getSeqId() == null ? 0 : peptide.getSeqId().intValue();
                }
                else
                {
                    return HttpView.throwNotFoundMV("Peptide not found");
                }
            }
            else
            {
                seqId = form.getSeqIdInt();
                runId = 0;
            }

            ActionURL currentUrl = getViewContext().getActionURL();

            if (0 == seqId)
                return HttpView.throwNotFoundMV("Protein sequence not found");

            _protein = ProteinManager.getProtein(seqId);

            AbstractMS2RunView peptideView = null;

            if (runId != 0)
            {
                _run = MS2Manager.getRun(runId);
                if (!isAuthorized(runId))
                {
                    return HttpView.throwUnauthorizedMV();
                }

                peptideView = new StandardProteinPeptideView(getViewContext(), _run);

                // Set the protein name used in this run's FASTA file; we want to include this in the view.
                _protein.setLookupString(form.getProtein());
                getPageConfig().setTemplate(PageConfig.Template.Print);
            }
            else
            {
                getPageConfig().setTemplate(PageConfig.Template.Home);  // This is used in links from compare
            }

            return new ProteinsView(currentUrl, _run, form, new Protein[] {_protein}, null, peptideView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
                appendRunNavTrail(root, _run, getProteinTitle(_protein, true), getPageConfig(), "showProtein");
            else
                appendRootNavTrail(root, getProteinTitle(_protein, true), getPageConfig(), "showProtein");

            return root;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowAllProteinsAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            if (!isAuthorized(form.run))
                return null;

            long peptideId = form.getPeptideIdLong();

            if (peptideId == 0)
                return HttpView.throwNotFoundMV("No peptide specified");

            MS2Peptide peptide = MS2Manager.getPeptide(peptideId);

            if (null == peptide)
                return HttpView.throwNotFoundMV("Could not locate peptide with this ID: " + peptideId);

            MS2Run run = MS2Manager.getRun(form.run);

            setTitle("Proteins Containing " + peptide);       // TODO: Add this text to the view
            getPageConfig().setTemplate(PageConfig.Template.Print);

            Protein[] proteins = ProteinManager.getProteinsContainingPeptide(run.getFastaId(), peptide);
            ActionURL currentUrl = getViewContext().cloneActionURL();
            AbstractMS2RunView peptideView = new StandardProteinPeptideView(getViewContext(), run);
            return new ProteinsView(currentUrl, run, form, proteins, new String[]{peptide.getTrimmedPeptide()}, peptideView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowProteinGroupAction extends SimpleViewAction<DetailsForm>
    {
        private MS2Run _run;

        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
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
                        ActionURL url = getViewContext().cloneActionURL();
                        url.deleteParameter("proteinGroupId");
                        url.replaceParameter("run", Integer.toString(form.run));
                        url.replaceParameter("groupNumber", Integer.toString(group.getGroupNumber()));
                        url.replaceParameter("indistinguishableCollectionId", Integer.toString(group.getIndistinguishableCollectionId()));
                        url.setExtraPath(c.getPath());

                        return HttpView.redirect(url);
                    }
                }
            }

            if (!isAuthorized(form.run))
                return null;

            _run = MS2Manager.getRun(form.run);

            ProteinProphetFile proteinProphet = _run.getProteinProphetFile();
            if (proteinProphet == null)
            {
                return HttpView.throwNotFoundMV();
            }
            ProteinGroupWithQuantitation group = proteinProphet.lookupGroup(form.getGroupNumber(), form.getIndistinguishableCollectionId());
            if (group == null)
            {
                return HttpView.throwNotFoundMV();
            }
            Protein[] proteins = group.lookupProteins();

            AbstractMS2RunView peptideView = new ProteinProphetPeptideView(getViewContext(), _run);
            VBox view = new ProteinsView(getViewContext().getActionURL(), _run, form, proteins, null, peptideView);         // TODO: clone?
            JspView summaryView = new JspView<ProteinGroupWithQuantitation>("/org/labkey/ms2/showProteinGroup.jsp", group);

            return new VBox(summaryView, view);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendRunNavTrail(root, _run, "Protein Group Details", getPageConfig(), "showProteinGroup");
            return root;
        }
    }


    public static class ProteinViewBean
    {
        public Protein protein;
        public boolean showPeptides;
    }


    private static class ProteinsView extends VBox
    {
        private ProteinsView(ActionURL currentUrl, MS2Run run, DetailsForm form, Protein[] proteins, String[] peptides, AbstractMS2RunView peptideView) throws Exception
        {
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
                addView(new HtmlView("<a name=\"Protein" + i + "\"/>"));

                ProteinViewBean bean = new ProteinViewBean();
                proteins[i].setPeptides(peptides);
                proteins[i].setShowEntireFragmentInCoverage(stringSearch);
                bean.protein = proteins[i];
                bean.showPeptides = showPeptides;
                JspView proteinSummary = new JspView<ProteinViewBean>("/org/labkey/ms2/protein.jsp", bean);
                proteinSummary.setTitle(getProteinTitle(proteins[i], true));
                addView(proteinSummary);

                // Add annotations
                addView(new AnnotView(null, proteins[i].getSeqId()));
            }

            if (showPeptides)
            {
                List<Pair<String, String>> sqlSummaries = new ArrayList<Pair<String, String>>();
                String peptideFilterString = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run).getFilterText();
                sqlSummaries.add(new Pair<String, String>("Peptide Filter", peptideFilterString));
                sqlSummaries.add(new Pair<String, String>("Peptide Sort", new Sort(currentUrl, MS2Manager.getDataRegionNamePeptides()).getSortText()));
                CurrentFilterView peptideFilter = new CurrentFilterView(null, sqlSummaries);
                peptideFilter.setTitle("Peptides");
                addView(peptideFilter);

                GridView peptidesGridView = peptideView.createPeptideViewForGrouping(form);
                peptidesGridView.getDataRegion().removeColumnsFromDisplayColumnList("Description,Protein,GeneName,SeqId");
                addView(peptidesGridView);
            }
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class PieSliceSectionAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            VBox vbox = new VBox();
            HttpServletRequest req = getViewContext().getRequest();

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
                vbox.addView(new AnnotView(curTitle, curSeqId));
            }

            getPageConfig().setTitle("Pieslice Details for: " + req.getParameter("sliceTitle"));

            return vbox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;  // TODO: add nav trail, once we pass run url along
        }
    }


    private static class AnnotView extends JspView<AnnotViewBean>
    {
        // TODO: Pass in Protein object
        private AnnotView(String title, int seqId) throws Exception
        {
            super("/org/labkey/ms2/protAnnots.jsp", getBean(seqId));

            if (title != null)
                setTitle("Annotations for " + title);
        }

        private static AnnotViewBean getBean(int seqId) throws Exception
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
            String SwissProtAccns[] = ProteinManager.getIdentifiersFromId(IdentifierType.SwissProtAccn, seqId);
            String GOCategories[] = ProteinManager.getGOCategoriesFromId(seqId);
            String IPIds[] = ProteinManager.getIdentifiersFromId("IPI", seqId);
            String RefSeqIds[] = ProteinManager.getIdentifiersFromId("REFSEQ", seqId);

            HashSet<String> allGbIds = new HashSet<String>();
            allGbIds.addAll(Arrays.asList(GenBankIds));
            allGbIds.addAll(Arrays.asList(RefSeqIds));

            Set<String> allGbURLs = new HashSet<String>();

            for (String ident : allGbIds)
            {
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


    @RequiresPermission(ACL.PERM_READ)
    public class DeleteViewsAction extends RedirectAction
    {
        public ActionURL getSuccessURL(Object o)
        {
            return getViewContext().cloneActionURL().setAction("manageViews");
        }

        public boolean doAction(Object o, BindException errors) throws Exception
        {
            List<String> viewNames = getViewContext().getList("viewParams");

            if (null != viewNames)
            {
                PropertyManager.PropertyMap m = PropertyManager.getWritableProperties(getUser().getUserId(), ContainerManager.getRoot().getId(), MS2_VIEWS_CATEGORY, true);

                for (String viewName : viewNames)
                    m.remove(viewName);

                PropertyManager.saveProperties(m);

                // NOTE: If names collide between shared and user-specific view names (unlikely since we append "(Shared)" to
                // project views only the shared names will be seen and deleted. Local names ending in "(Shared)" are shadowed
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

            return true;
        }

        public void validateCommand(Object target, Errors errors)
        {
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowHierarchyAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ActionURL currentUrl = getViewContext().cloneActionURL();
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

            return new HtmlView(html.toString());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Hierarchy", getPageConfig(), "ms2RunsList");
        }
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


    protected void showElutionGraph(HttpServletResponse response, DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
    {
        if (!isAuthorized(form.run))
            return;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

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
                if (charge < 1 || charge > Quantitation.MAX_CHARGE)
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
        else
        {
            HttpView.throwNotFound("Could not find peptide with id " + form.getPeptideIdLong());
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
        g.render(response.getOutputStream());
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowLightElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            showElutionGraph(response, form, true, false);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowHeavyElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            showElutionGraph(response, form, false, true);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowCombinedElutionGraphAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            showElutionGraph(response, form, true, true);
        }
    }


    @RequiresPermission(ACL.PERM_DELETE)
    public class DeleteRunsAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            Set<String> runIds = DataRegionSelection.getSelected(getViewContext(), true);

            if (null != runIds)
                MS2Manager.markAsDeleted(MS2Manager.parseIds(runIds), getContainer(), getUser());

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return getShowListUrl(getContainer());
        }
    }


    @RequiresSiteAdmin
    public class MascotTestAction extends SimpleViewAction<TestMascotForm>
    {
        public ModelAndView getView(TestMascotForm form, BindException errors) throws Exception
        {
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
            form.setPassword(("".equals(form.getPassword())) ? "" : "***");  // do not show password in clear

            getPageConfig().setTemplate(PageConfig.Template.Dialog);
            return new JspView<TestMascotForm>("/org/labkey/ms2/testMascot.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
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


    @RequiresSiteAdmin
    public class SequestTestAction extends SimpleViewAction<TestSequestForm>
    {
        public ModelAndView getView(TestSequestForm form, BindException errors) throws Exception
        {
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

            getPageConfig().setTemplate(PageConfig.Template.Dialog);
            return new JspView<TestSequestForm>("/org/labkey/ms2/testSequest.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
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


    @RequiresPermission(ACL.PERM_READ)
    public class ApplyRunViewAction extends SimpleRedirectAction<MS2ViewForm>
    {
        public ActionURL getRedirectURL(MS2ViewForm form) throws Exception
        {
            // Redirect to have Spring fill in the form and ensure that the DataRegion JavaScript sees the showRun action
            return getApplyViewForwardUrl(form, "showRun");
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ApplyExportRunsViewAction extends SimpleForwardAction<MS2ViewForm>
    {
        public ActionURL getForwardURL(MS2ViewForm form) throws Exception
        {
            // Forward without redirect: this lets Spring fill in the form but preserves the post data
            return getApplyViewForwardUrl(form, "exportRuns");
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ApplyCompareViewAction extends SimpleRedirectAction<MS2ViewForm>
    {
        public ActionURL getRedirectURL(MS2ViewForm form) throws Exception
        {
            ActionURL redirectURL = getApplyViewForwardUrl(form, "showCompare");

            redirectURL.deleteParameter("submit.x");
            redirectURL.deleteParameter("submit.y");
            redirectURL.deleteParameter("viewParams");

            return redirectURL;
        }
    }


    private ActionURL getApplyViewForwardUrl(MS2ViewForm form, String action)
    {
        // Add the "view params" (which were posted as a single param) to the URL params.
        ActionURL forwardUrl = getViewContext().cloneActionURL();
        forwardUrl.setRawQuery(forwardUrl.getRawQuery() + (null == form.viewParams ? "" : "&" + form.viewParams));
        return forwardUrl.setAction(action);
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


    @RequiresPermission(ACL.PERM_READ)
    public class DoOnePeptideChartAction extends ExportAction
    {
        public void export(Object o, HttpServletResponse response) throws Exception
        {
            HttpServletRequest req = getViewContext().getRequest();
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
        }
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


    @RequiresPermission(ACL.PERM_NONE)
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
                    HttpView.throwNotFound();
                else
                    return getShowListUrl(c);
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

                    int run;
                    if (MascotSearchProtocolFactory.get().getClass().equals(protocolFactory.getClass()))
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
                        HttpView.throwNotFound();

                    url = getShowListUrl(c);
                    url.addParameter(MS2Manager.getDataRegionNameExperimentRuns() + ".Run~eq", Integer.toString(run));
                }
                else if (!AppProps.getInstance().hasPipelineCluster())
                {
                    url = new ActionURL("MS2", "addFileRunStatus", "");
                    url.addParameter("error", "Automated upload disabled.");
                }
                else
                {
                    if (!form.isExperiment())
                    {
                        int run = MS2Manager.addRunToQueue(getViewBackgroundInfo(),
                                f, form.getDescription(), true).getRunId();
                        if (run == -1)
                            HttpView.throwNotFound();

                        url = new ActionURL("MS2", "addFileRunStatus", "");
                        url.addParameter("run", Integer.toString(run));
                    }
                    else
                    {
                        // Make sure container exists.
                        c = ContainerManager.ensureContainer(getViewContext().getActionURL().getExtraPath());
                        if (null == c)
                            HttpView.throwNotFound();

                        PipelineService service = PipelineService.get();
                        PipeRoot pr = service.findPipelineRoot(c);
                        if (pr == null)
                            HttpView.throwUnauthorized();

                        String protocolName = form.getProtocol();
                        File dirData = new File(form.getDataDir());
                        if (!NetworkDrive.exists(dirData))
                            HttpView.throwNotFound();

                        AbstractMS2SearchProtocolFactory protocolFactory =
                                AbstractMS2SearchProtocolFactory.fromFile(AbstractMS2SearchPipelineProvider.class, f);

                        File dirSeqRoot = new File(MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer()));
                        File dirAnalysis = protocolFactory.getAnalysisDir(dirData, protocolName);
                        File fileParameters = protocolFactory.getParametersFile(dirData, protocolName);
                        AbstractMS2SearchProtocol protocol = protocolFactory.loadInstance(fileParameters);
                        String baseName = FileUtil.getBaseName(f, 2);

                        File[] filesMzXML;
                        if (!"all".equals(baseName))
                        {
                            filesMzXML = new File[] { MS2PipelineManager.getMzXMLFile(dirData, baseName) };
                        }
                        else
                        {
                            // Anything that is running or complete.
                            Map<File, FileStatus> mzXMLFileStatus = MS2PipelineManager.getAnalysisFileStatus(dirData, dirAnalysis, getContainer());
                            List<File> fileList = new ArrayList<File>();
                            for (File fileMzXML : mzXMLFileStatus.keySet())
                            {
                                FileStatus status = mzXMLFileStatus.get(fileMzXML);
                                if (status.equals(FileStatus.COMPLETE) || status.equals(FileStatus.RUNNING))
                                    fileList.add(fileMzXML);
                            }
                            filesMzXML = fileList.toArray(new File[fileList.size()]);

                            if (filesMzXML.length == 0)
                                HttpView.throwNotFound();
                        }

                        protocol.setDirSeqRoot(dirSeqRoot);
                        try
                        {
                            protocol.validate(pr.getUri(c));

                            PipelineJob job = protocol.createPipelineJob(getViewBackgroundInfo(),
                                    filesMzXML,
                                    fileParameters,
                                    true);

                            PipelineService.get().queueJob(job);

                            url = new ActionURL("MS2", "addFileRunStatus", "");
                            url.addParameter("path", job.getLogFile().getAbsolutePath());
                        }
                        catch (PipelineProtocol.PipelineValidationException e)
                        {
                            url = new ActionURL("MS2", "addFileRunStatus", "");
                            url.addParameter("error", e.getMessage());
                        }
                    }
                }
            }
            else
            {
                url = new ActionURL("MS2", "addFileRunStatus", "");
            }

            return url;
        }
    }


    // TODO: Use form
    @RequiresPermission(ACL.PERM_READ)
    public class AddExtraFilterAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ViewContext ctx = getViewContext();
            HttpServletRequest request = ctx.getRequest();
            ActionURL url = ctx.cloneActionURL();
            url.setAction("showRun.view");

            MS2Run run = MS2Manager.getRun(request.getParameter("run"));
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


    @RequiresPermission(ACL.PERM_UPDATE)
    public class SaveElutionProfileAction extends SimpleRedirectAction<ElutionProfileForm>
    {
        public ActionURL getRedirectURL(ElutionProfileForm form) throws Exception
        {
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

            ActionURL url = getViewContext().getActionURL().clone();
            url.setAction("showPeptide.view");
            if (!validRanges)
            {
                url.addParameter("elutionProfileError", "Invalid elution profile range");
            }

            return url;
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


    @RequiresPermission(ACL.PERM_INSERT)
    public class ImportProteinProphetAction extends SimpleRedirectAction<ImportProteinProphetForm>
    {
        public ActionURL getRedirectURL(ImportProteinProphetForm form) throws Exception
        {
            PipelineService service = PipelineService.get();
            Container c = getContainer();

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
                ProteinProphetPipelineJob job = new ProteinProphetPipelineJob(getViewBackgroundInfo(), f);
                service.queueJob(job);
            }
            else
            {
                HttpView.throwNotFound("Unable to open the file '" + form.getPath() + "' to load as a ProteinProphet file");
            }

            return new ActionURL("Project", "begin", c.getPath());
        }
    }


    public static class ImportProteinProphetForm
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


    @RequiresPermission(ACL.PERM_READ)
    public class DiscriminateScoreAction extends SimpleRedirectAction<RunForm>
    {
        public ActionURL getRedirectURL(RunForm form) throws Exception
        {
            ActionURL url = new ActionURL("MS2-Scoring", "discriminate", getContainer());
            url.addParameter("runId", form.getRun());
            return url;
        }
    }


    public static class RunForm extends FormData
    {
        enum PARAMS
        {
            run
        }

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
            String comment = form.getComment();

            DefaultAnnotationLoader loader;

            //TODO: this style of dealing with different file types must be repaired.
            if ("uniprot".equalsIgnoreCase(form.getFileType()))
            {
                loader = new XMLProteinLoader(fname, form.isClearExisting());
            }
            else if ("fasta".equalsIgnoreCase(form.getFileType()))
            {
                FastaDbLoader fdbl = new FastaDbLoader(fname == null ? null : new File(fname));
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
                errors.addError(new ObjectError("main", null, null, e.getMessage()));
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
        private BindException _errors;

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
                return HttpView.throwNotFoundMV();
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


    @RequiresPermission(ACL.PERM_NONE)
    public class AddFileRunStatusAction extends ExportAction
    {
        public void export(Object o, HttpServletResponse response) throws Exception
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


    @RequiresPermission(ACL.PERM_DELETE)
    public class SelectMoveLocationAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            List<String> moveRuns = getViewContext().getList(DataRegion.SELECT_CHECKBOX_NAME);

            ActionURL url = new ActionURL(SelectMoveLocationAction.class, getContainer()).addParameter("moveRuns", StringUtils.join(moveRuns, ','));

            if ("true".equals(getViewContext().getRequest().getParameter("ExperimentRunIds")))
                url.addParameter("ExperimentRunIds", "true");

            return url;
        }
    }


    @RequiresPermission(ACL.PERM_DELETE)
    public class PickMoveLocationAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ActionURL moveURL = new ActionURL(MoveRunsAction.class);
            final Container originalContainer = getContainer();
            ContainerTree ct = new ContainerTree("/", getUser(), ACL.PERM_INSERT, moveURL)
            {
                protected void renderCellContents(StringBuilder html, Container c, ActionURL url)
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

            getPageConfig().setTitle("Move Runs");
            getPageConfig().setTemplate(PageConfig.Template.Dialog);

            return new HtmlView(html.toString());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
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


    @RequiresPermission(ACL.PERM_READ)
    public class ShowParamsFileAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            MS2Run run = MS2Manager.getRun(form.getRun());

            try
            {
                // TODO: Ensure drive?
                PageFlowUtil.streamFile(response, run.getPath() + "/" + run.getParamsFileName(), false);
            }
            catch (Exception e)
            {
                response.getWriter().print("Error retrieving file: " + e.getMessage());
            }
        }
    }


    @RequiresPermission(ACL.PERM_NONE)
    public class UpdateShowPeptideAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            ViewContext ctx = getViewContext();

            ActionURL redirectURL = ctx.cloneActionURL().setAction("showPeptide");
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


    @RequiresPermission(ACL.PERM_READ)
    public class ShowGZFileAction extends ExportAction<DetailsForm>
    {
        public void export(DetailsForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

            if (null == peptide)
            {
                // This should only happen if an old, cached link is being used... a saved favorite or google bot with fraction=x&scan=y&charge=z instead of peptideId
                throw new NotFoundException("Couldn't find peptide " + form.getPeptideIdLong() + ". " + getViewContext().getActionURL().toString());
            }

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


    @RequiresPermission(ACL.PERM_UPDATE)
    public class EditElutionGraphAction extends SimpleViewAction<DetailsForm>
    {
        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            if (!isAuthorized(form.run))
                return null;

            MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
            Quantitation quant = peptide.getQuantitation();

            EditElutionGraphContext ctx = new EditElutionGraphContext(quant.getLightElutionProfile(peptide.getCharge()), quant.getHeavyElutionProfile(peptide.getCharge()), quant, getViewContext().getActionURL(), peptide);
            return new JspView<EditElutionGraphContext>("/org/labkey/ms2/editElution.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
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
            return new ActionURL(MS2Controller.ShowPeptideAction.class, ContainerManager.getForId(run.getContainer())).addParameter("run", run.getRun());
        }

        public ActionURL getShowProteinAdminUrl()
        {
            return new ActionURL(ShowProteinAdminAction.class);
        }



        public static MS2UrlsImpl get()
        {
            return (MS2UrlsImpl) PageFlowUtil.urlProvider(MS2Urls.class);
        }
    }


    public class CompareOptionsBean
    {
        private final QueryView _peptideView;
        private final ActionURL _targetURL;
        private final int _runList;
        private final PeptideFilteringComparisonForm _form;

        public CompareOptionsBean(QueryView peptideView, ActionURL targetURL, int runList, PeptideFilteringComparisonForm form)
        {
            _peptideView = peptideView;
            _targetURL = targetURL;
            _runList = runList;
            _form = form;
        }

        public QueryView getPeptideView()
        {
            return _peptideView;
        }

        public ActionURL getTargetURL()
        {
            return _targetURL;
        }

        public int getRunList()
        {
            return _runList;
        }

        public PeptideFilteringComparisonForm getForm()
        {
            return _form;
        }
    }
}
