package org.labkey.ms2;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
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
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.common.tools.MS2Modification;
import org.labkey.common.tools.PeptideProphetSummary;
import org.labkey.common.tools.SensitivitySummary;
import org.labkey.common.util.Pair;
import org.labkey.ms2.compare.CompareDataRegion;
import org.labkey.ms2.compare.CompareExcelWriter;
import org.labkey.ms2.compare.CompareQuery;
import org.labkey.ms2.compare.RunColumn;
import org.labkey.ms2.peptideview.*;
import org.labkey.ms2.pipeline.*;
import org.labkey.ms2.protein.FastaDbLoader;
import org.labkey.ms2.protein.IdentifierType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.tools.GoLoader;
import org.labkey.ms2.protein.tools.NullOutputStream;
import org.labkey.ms2.protein.tools.PieJChartHelper;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.protocol.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.protocol.MS2SearchPipelineProtocol;
import org.labkey.ms2.protocol.MascotSearchProtocolFactory;
import org.labkey.ms2.query.*;
import org.labkey.ms2.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
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
    private static DefaultActionResolver _actionResolver = new BeehivePortingActionResolver(OldMS2Controller.class, MS2Controller.class);
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
            ButtonBar bb = getListButtonBar(getContainer());
            ActionURL currentUrl = getViewContext().cloneActionURL();
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
            gridView.setTitle("MS2 Runs");
            gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
            gridView.setSort(MS2Manager.getRunsBaseSort());

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


    private ButtonBar getListButtonBar(Container c)
    {
        ButtonBar bb = new ButtonBar();

        ActionButton compareRuns = new ActionButton("button", "Compare");
        compareRuns.setScript("return verifySelected(this.form, \"compare.view\", \"post\", \"runs\")");
        compareRuns.setActionType(ActionButton.Action.GET);
        compareRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareRuns);

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
        url.addParameter(OldMS2Controller.RunForm.PARAMS.run, String.valueOf(runId));
        return url;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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
            appendRunNavTrail(root, _run, null, getPageConfig(), "viewRun");
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
        private FilterHeaderView(ActionURL currentUrl, OldMS2Controller.RunForm form, MS2Run run) throws ServletException, SQLException
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
    public class ShowPeptideAction extends SimpleViewAction<OldMS2Controller.DetailsForm>
    {
        public ModelAndView getView(OldMS2Controller.DetailsForm form, BindException errors) throws Exception
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
    public class ShowModificationsAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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


    public static ActionURL getLoadGoUrl()
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
            return new JspView("/org/labkey/ms2/loadGo.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setTitle("Load GO Annotations");
            setHelpTopic(new HelpTopic("annotations", HelpTopic.Area.CPAS));
            return null;  // TODO: Admin navtrail
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            GoLoader loader = GoLoader.getGoLoader();

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
            return getGoStatusUrl(_message);
        }
    }


    private ActionURL getGoStatusUrl(String message)
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
            if (!isAuthorized(form.run))
                return null;

            _goChartType = ProteinDictionaryHelpers.GTypeStringToEnum(form.getChartType());

            MS2Run run = MS2Manager.getRun(form.run);

            ViewContext ctx = getViewContext();
            ActionURL queryUrl = ctx.cloneActionURL();
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
            bean.chartUrl = new ActionURL("ms2", "doOnePeptideChart", getContainer()).addParameter("ctype", _goChartType.toString()).addParameter("helpername", bean.pieHelperObjName);

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
    public class GetProteinGroupingPeptides extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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
        public String extraHtml;
        public String viewInstructions;
        public int runList;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class CompareAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
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

            ActionURL nextUrl = getViewContext().cloneActionURL().setAction("applyCompareView");
            return pickView(nextUrl, "Select a view to apply a filter to all the runs.", sb.toString(), false);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Compare Runs", getPageConfig(), "compareRuns");
        }
    }


    // extraFormHtml gets inserted between the view dropdown and the button.
    private HttpView pickView(ActionURL nextUrl, String viewInstructions, String extraFormHtml, boolean requireSameType) throws Exception
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

        return pickView;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class PickExportRunsView extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            String extraFormHtml =
                "<tr><td><br>Choose an export format:</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"Excel\" checked=\"checked\">Excel (limited to 65,535 rows)</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"ExcelBare\">Excel with minimal header text (limited to 65,535 rows)</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"TSV\">TSV</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"DTA\">Spectra as DTA</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"PKL\">Spectra as PKL</td></tr>\n" +
                "<tr><td><input type=\"radio\" name=\"exportFormat\" value=\"AMT\">AMT (Accurate Mass &amp; Time) file</td></tr>\n";

            return pickView(getViewContext().cloneActionURL().setAction("applyExportRunsView"), "Select a view to apply a filter to all the runs and to indicate what columns to export.", extraFormHtml, true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root, "Export Runs", getPageConfig(), "exportRuns");
        }
    }


    // TODO: Store this in session state!!  Must provide way for this cache to shrink

    // Stash lists of run ids in session state.  Use object Id as index into map, and pass the Id on the URL.  We can't stash these
    // lists on the URL because it could be too large (we support exporting/comparing hundreds of runs).  We can't post the data
    // and forward through the applyView process because we must redirect to end up on the right action (otherwise the filter box
    // JavaScript will call the wrong action).  Plus, DataRegion sorting uses GET, so we'd lose the list of runs after sorting.
    private static Map<Integer, List<Integer>> _runListCache = new HashMap <Integer, List<Integer>>(10);

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
        boolean experimentRunIds = "true".equals(getViewContext().getRequest().getParameter("ExperimentRunIds"));
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

            return currentUrl;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ExportRunsAction extends ExportAction<ExportForm>   // TODO: Convert to ExportAction
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            List<String> errorStrings = new ArrayList<String>();
            List<MS2Run> runs = getCachedRuns(form.getRunList(), errorStrings, true);

            if (!errorStrings.isEmpty())
                _renderErrors(errorStrings);  // TODO: throw

            AbstractMS2RunView peptideView = getPeptideView(form.getGrouping(), runs.toArray(new MS2Run[runs.size()]));
            ActionURL currentUrl = getViewContext().cloneActionURL();
            SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER);

            if (form.getExportFormat() != null && form.getExportFormat().startsWith("Excel"))
            {
                peptideView.exportToExcel(form, response, null);
            }

            if ("TSV".equals(form.getExportFormat()))
            {
                peptideView.exportToTSV(form, response, null, null);
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
                peptideView.exportToAMT(form, response, null);
            }
        }
    }


    private Forward exportSpectra(List<MS2Run> runs, ActionURL currentUrl, SimpleFilter filter, String extension) throws IOException
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

        return null;
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowCompareAction extends SimpleViewAction<ExportForm>
    {
        private StringBuilder _title = new StringBuilder();

        public ModelAndView getView(ExportForm form, BindException errors) throws Exception
        {
            return compareRuns(form.getRunList(), false, _title);
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
            compareRuns(form.getRunList(), true, null);
        }
    }


    private ModelAndView compareRuns(int runListIndex, boolean exportToExcel, StringBuilder title) throws Exception
    {
        ActionURL currentUrl = getViewContext().getActionURL();
        String column = currentUrl.getParameter("column");            // TODO: add to form
        boolean isQueryProteinProphet = "query".equalsIgnoreCase(column);
        boolean isQueryPeptides = "querypeptides".equalsIgnoreCase(column);

        if (isQueryProteinProphet || isQueryPeptides)
        {
            AbstractRunCompareView view = isQueryPeptides ? new ComparePeptidesView(getViewContext(), this, runListIndex, false) : new CompareProteinsView(getViewContext(), this, runListIndex, false);

            if (!view.getErrors().isEmpty())
                return _renderErrors(view.getErrors());

            HtmlView helpView = new HtmlView("Comparison Details", "<div style=\"width: 800px;\"><p>To change the columns shown and set filters, use the Customize View link below. Add protein-specific columns, or expand <em>Run</em> to see the values associated with individual runs, like probability. To set a filter, select the Filter tab, add column, and filter it based on the desired threshold.</p></div>");

            Map<String, String> props = new HashMap<String, String>();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put("comparisonName", view.getComparisonName());
            GWTView gwtView = new GWTView("org.labkey.ms2.RunComparator", props);
            VBox vbox = new VBox(gwtView, helpView, view);

            title.append("Compare Runs");

            return vbox;
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
                return HttpView.throwUnauthorizedMV();
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
            ew.write(getViewContext().getResponse());
        }
        else
        {
            rgn.setOffset(offset);
            rgn.setColSpan(query.getColumnsPerRun());
            rgn.setMultiColumnCaptions(runCaptions);

            HttpView filterView = new CurrentFilterView(query);

            GridView compareView = new GridView(rgn);
            compareView.setResultSet(rgn.getResultSet());

            title.append(query.getComparisonDescription());

            return new VBox(filterView, compareView);
        }

        return null;
    }


    @RequiresPermission(ACL.PERM_NONE)  // CompareProteinsView does permissions checking on all runs
    public class ExportQueryProteinProphetCompareToExcelAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportQueryCompareToExcel(new CompareProteinsView(getViewContext(), MS2Controller.this, form.getRunList(), true));
        }
    }


    @RequiresPermission(ACL.PERM_NONE)  // CompareProteinsView does permissions checking on all runs
    public class ExportQueryProteinProphetCompareToTSVAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportQueryCompareToTSV(new CompareProteinsView(getViewContext(), MS2Controller.this, form.getRunList(), true), form.isExportAsWebPage());
        }
    }


    @RequiresPermission(ACL.PERM_NONE)  // CompareProteinsView does permissions checking on all runs
    public class ExportQueryPeptideCompareToExcelAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportQueryCompareToExcel(new ComparePeptidesView(getViewContext(), MS2Controller.this, form.getRunList(), true));
        }
    }


    @RequiresPermission(ACL.PERM_NONE)  // CompareProteinsView does permissions checking on all runs
    public class ExportQueryPeptideCompareToTSVAction extends ExportAction<ExportForm>
    {
        public void export(ExportForm form, HttpServletResponse response) throws Exception
        {
            exportQueryCompareToTSV(new ComparePeptidesView(getViewContext(), MS2Controller.this, form.getRunList(), true), form.isExportAsWebPage());
        }
    }


    private void exportQueryCompareToExcel(AbstractRunCompareView view) throws Exception
    {
        if (!view.getErrors().isEmpty())
            _renderErrorsForward(view.getErrors());

        ExcelWriter excelWriter = view.getExcelWriter();
        excelWriter.setFilenamePrefix("CompareRuns");
        excelWriter.write(getViewContext().getResponse());
    }


    private void exportQueryCompareToTSV(AbstractRunCompareView view, boolean exportAsWebPage) throws Exception
    {
        if (!view.getErrors().isEmpty())
            _renderErrorsForward(view.getErrors());

        TSVGridWriter tsvWriter = view.getTsvWriter();
        tsvWriter.setExportAsWebPage(exportAsWebPage);
        tsvWriter.setFilenamePrefix("CompareRuns");
        tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
        tsvWriter.write(getViewContext().getResponse());
    }


    @RequiresPermission(ACL.PERM_READ)
    public class CompareServiceAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            CompareServiceImpl service = new CompareServiceImpl(getViewContext(), MS2Controller.this);
            service.doPost(getViewContext().getRequest(), getViewContext().getResponse());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
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
    public class ShowGraphAction extends ExportAction<OldMS2Controller.DetailsForm>
    {
        public void export(OldMS2Controller.DetailsForm form, HttpServletResponse response) throws Exception
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
    public class UpdateSeqIdsAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            HttpServletRequest request = getViewContext().getRequest();
            String[] fastaIds = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);

            List<Integer> ids = new ArrayList<Integer>(fastaIds.length);

            for (String fastaId : fastaIds)
            {
                int id = Integer.parseInt(fastaId);
                if (0 != id)
                    ids.add(id);
            }

            FastaDbLoader.updateSeqIds(ids);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl();
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
            String[] fastaIds = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
            String idList = StringUtils.join(fastaIds, ',');
            Integer[] validIds = Table.executeArray(ProteinManager.getSchema(), "SELECT FastaId FROM " + ProteinManager.getTableInfoFastaAdmin() + " WHERE (FastaId <> 0) AND (Runs IS NULL) AND (FastaId IN (" + idList + "))", new Object[]{}, Integer.class);

            for (int id : validIds)
                ProteinManager.deleteFastaFile(id);

            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl();
        }
    }


    public static ActionURL getShowProteinAdminUrl()
    {
        return new ActionURL(ShowProteinAdminAction.class);
    }


    @RequiresSiteAdmin
    public class ShowProteinAdminAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            GridView grid = new GridView(getFastaAdminGrid());
            grid.setTitle("FASTA Files");

            grid.getViewContext().setPermissions(ACL.PERM_READ);

            GridView annots = new GridView(getAnnotInsertsGrid());
            annots.setTitle("Protein Annotations Loaded");

            annots.getViewContext().setPermissions(ACL.PERM_READ);

            return new VBox(grid, annots);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            setTitle("Protein Database Admin");  // TODO: Admin nav trail
            return root;
        }
    }


    private DataRegion getFastaAdminGrid()
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(ProteinManager.getTableInfoFastaAdmin().getColumns("FileName, Loaded, FastaId, Runs"));
        String runsUrl = ActionURL.toPathString("MS2", "showAllRuns", (String)null) + "?" + MS2Manager.getDataRegionNameRuns() + ".FastaId~eq=${FastaId}";
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

        bb.add(new ActionButton("insertAnnots.post", "Load New Annot File"));
        bb.add(new ActionButton("reloadSPOM.post", "Reload SWP Org Map"));
        ActionButton reloadGO = new ActionButton("loadGo.view", "Load or Reload GO");
        reloadGO.setActionType(ActionButton.Action.LINK);
        bb.add(reloadGO);

        rgn.setButtonBar(bb);
        return rgn;
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

    private Forward exportProteinsAsSpectra(List<MS2Run> runs, ActionURL currentUrl, String extension, AbstractMS2RunView peptideView, String where) throws IOException
    {
        SpectrumIterator iter = new ProteinResultSetSpectrumIterator(runs, currentUrl, peptideView, where);

        SpectrumRenderer sr;

        if ("pkl".equals(extension))
            sr = new PklSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);
        else
            sr = new DtaSpectrumRenderer(getViewContext().getResponse(), "spectra", extension);

        sr.render(iter);
        sr.close();

        return null;
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
        QuerySettings groupsSettings = new QuerySettings(getViewContext().getActionURL(), getViewContext().getRequest(), "ProteinSearchResults");
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
        QuerySettings proteinsSettings = new QuerySettings(getViewContext().getActionURL(), getViewContext().getRequest(), "PotentialProteins");
        proteinsSettings.setQueryName(MS2Schema.SEQUENCES_TABLE_NAME);
        proteinsSettings.setAllowChooseQuery(false);
        QueryView proteinsView = new QueryView(getViewContext(), QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME), proteinsSettings)
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


    private Forward exportPeptides(ExportForm form, HttpServletResponse response, boolean selected) throws Exception
    {
        if (!isAuthorized(form.run))
            return null;

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
            return null;
        }

        if ("TSV".equals(form.getExportFormat()))
        {
            peptideView.exportToTSV(form, response, exportRows, null);
            return null;
        }

        if ("AMT".equals(form.getExportFormat()))
        {
            peptideView.exportToAMT(form, response, exportRows);
            return null;
        }

        // Add URL filter manually
        baseFilter.addAllClauses(ProteinManager.getPeptideFilter(currentUrl, ProteinManager.URL_FILTER, run));

        if ("DTA".equals(form.getExportFormat()) || "PKL".equals(form.getExportFormat()))
            return exportSpectra(Arrays.asList(run), currentUrl, baseFilter, form.getExportFormat().toLowerCase());

        return null;
    }


    @Deprecated
    private HttpView _renderErrors(List<String> messages) throws Exception
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
        return view;
    }


    @Deprecated
    private HttpView _renderError(String message) throws Exception
    {
        return _renderErrors(Arrays.asList(message));
    }


    @Deprecated
    private Forward _renderErrorsForward(List<String> messages) throws Exception
    {
        return null;
    }


    public static class ExportForm extends OldMS2Controller.RunForm
    {
        private String exportFormat;
        private int runList;

        public String getExportFormat()
        {
            return exportFormat;
        }

        public void setExportFormat(String exportFormat)
        {
            this.exportFormat = exportFormat;
        }

        public int getRunList()
        {
            return runList;
        }

        public void setRunList(int runList)
        {
            this.runList = runList;
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


    public static class PeptideProphetForm extends OldMS2Controller.RunForm
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
    public class ShowPeptideProphetDetailsAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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
    public class ShowProteinProphetSensitivityPlotAction extends ExportAction<OldMS2Controller.RunForm>
    {
        public void export(OldMS2Controller.RunForm form, HttpServletResponse response) throws Exception
        {
            if (!isAuthorized(form.run))
                return;

            ProteinProphetFile summary = MS2Manager.getProteinProphetFileByRun(form.run);

            PeptideProphetGraphs.renderSensitivityGraph(response, summary);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowProteinProphetDetailsAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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
            ActionURL url = getViewContext().cloneActionURL();
            url.setAction("showMS2Admin");
            url.deleteParameters();
            url.addParameter("days", String.valueOf(_days));

            return url;
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

            ActionURL containerUrl = getViewContext().cloneActionURL().setAction("showList");

            // We don't want ActionURL to encode ${ContainerPath}, so set a dummy value and use string substitution
            String urlString = containerUrl.setExtraPath("ContainerPath").getLocalURIString().replaceFirst("/ContainerPath/", "\\$\\{ContainerPath}/");
            cdc.setURL(urlString);
            rgn.addColumn(cdc);
            rgn.addColumns(MS2Manager.getTableInfoRuns().getColumns("Description, Path, Created, Deleted, StatusId, Status, PeptideCount, SpectrumCount, FastaId"));

            ActionURL showRunUrl = new ActionURL("MS2", "showRun", "ContainerPath");
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
    public class PickPeptideColumnsAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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
    public class PickProteinColumnsAction extends SimpleViewAction<OldMS2Controller.RunForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.RunForm form, BindException errors) throws Exception
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


    public static class ChartForm extends OldMS2Controller.RunForm
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
    public class ShowProteinAction extends SimpleViewAction<OldMS2Controller.DetailsForm>
    {
        private MS2Run _run;
        private Protein _protein;

        public ModelAndView getView(OldMS2Controller.DetailsForm form, BindException errors) throws Exception
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
    public class ShowAllProteinsAction extends SimpleViewAction<OldMS2Controller.DetailsForm>
    {
        public ModelAndView getView(OldMS2Controller.DetailsForm form, BindException errors) throws Exception
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
    public class ShowProteinGroupAction extends SimpleViewAction<OldMS2Controller.DetailsForm>
    {
        private MS2Run _run;

        public ModelAndView getView(OldMS2Controller.DetailsForm form, BindException errors) throws Exception
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
        private ProteinsView(ActionURL currentUrl, MS2Run run, OldMS2Controller.DetailsForm form, Protein[] proteins, String[] peptides, AbstractMS2RunView peptideView) throws Exception
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


    protected void showElutionGraph(HttpServletResponse response, OldMS2Controller.DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
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
    public class ShowLightElutionGraphAction extends ExportAction<OldMS2Controller.DetailsForm>
    {
        public void export(OldMS2Controller.DetailsForm form, HttpServletResponse response) throws Exception
        {
            showElutionGraph(response, form, true, false);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowHeavyElutionGraphAction extends ExportAction<OldMS2Controller.DetailsForm>
    {
        public void export(OldMS2Controller.DetailsForm form, HttpServletResponse response) throws Exception
        {
            showElutionGraph(response, form, false, true);
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowCombinedElutionGraphAction extends ExportAction<OldMS2Controller.DetailsForm>
    {
        public void export(OldMS2Controller.DetailsForm form, HttpServletResponse response) throws Exception
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
            List<String> runIds = getViewContext().getList(DataRegion.SELECT_CHECKBOX_NAME);

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

            return getShowProteinAdminUrl();
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
                    return MS2Controller.getShowListUrl(c);
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
                        HttpView.throwNotFound();

                    url = MS2Controller.getShowListUrl(c);
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

                        MS2SearchPipelineProtocol protocol = protocolFactory.loadInstance(fileParameters);

                        PipelineJob job = protocol.createPipelineJob(getViewBackgroundInfo(),
                                dirSeqRoot,
                                filesMzXML,
                                fileParameters,
                                true);

                        PipelineService.get().queueJob(job);

                        url = new ActionURL("MS2", "addFileRunStatus", "");
                        url.addParameter("path", job.getLogFile().getAbsolutePath());
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


    public static class ElutionProfileForm extends OldMS2Controller.DetailsForm
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
}
