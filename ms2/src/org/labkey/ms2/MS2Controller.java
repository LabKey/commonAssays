package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.action.*;
import org.labkey.api.data.*;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.common.util.Pair;
import org.labkey.ms2.compare.CompareQuery;
import org.labkey.ms2.peptideview.AbstractMS2RunView;
import org.labkey.ms2.peptideview.MS2RunViewType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

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
    static final String SHARED_VIEW_SUFFIX = " (Shared)";  // TODO: Make private once Spring conversion is done
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
                ViewURLHelper url = getViewContext().getViewURLHelper().clone();
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


    private void populatePageConfig(PageConfig page, String title, String helpTopic, boolean exploratoryFeatures)
    {
        page.setTitle(title);
        page.setHelpTopic(new HelpTopic(helpTopic != null ? helpTopic : "ms2", HelpTopic.Area.CPAS));
        page.setExploratoryFeatures(exploratoryFeatures);
    }


    private AbstractMS2RunView getPeptideView(String grouping, MS2Run... runs) throws ServletException
    {
        return MS2RunViewType.getViewType(grouping).createView(getViewContext(), runs);
    }


    public static ViewURLHelper getBeginUrl(Container c)
    {
        return c.urlFor(BeginAction.class);
    }


    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleRedirectAction
    {
        public ViewURLHelper getRedirectURL(Object o)
        {
            return getShowListUrl(getContainer());
        }
    }


    private NavTree appendRootNavTrail(NavTree root)
    {
        root.addChild("MS2 Runs", getShowListUrl(getContainer()));
        return root;
    }


    public static ViewURLHelper getShowListUrl(Container c)
    {
        return c.urlFor(ShowListAction.class);
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ButtonBar bb = getListButtonBar(getContainer());
            ViewURLHelper currentUrl = getViewContext().cloneViewURLHelper();
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

            ViewURLHelper url = getViewContext().cloneViewURLHelper();
            url.deleteParameters();
            url.setPageFlow("protein");
            url.setAction("begin.view");

            populatePageConfig(getPageConfig(), "MS2 Runs", "ms2RunsList", false);

            return new VBox(searchView, gridView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRootNavTrail(root);
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

        ActionButton compareScoring = new ActionButton("", MS2Controller.CAPTION_SCORING_BUTTON);
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


    public static ViewURLHelper getShowRunUrl(Container c, int runId)
    {
        return c.urlFor(ShowRunAction.class, "run", String.valueOf(runId));
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

            ViewURLHelper currentUrl = getViewContext().getViewURLHelper();
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
            _run = run;

            populatePageConfig(getPageConfig(), run.getDescription(), "viewRun", exploratoryFeatures);

            return vBox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            appendRootNavTrail(root);
            root.addChild(_run.getDescription(), getShowRunUrl(getContainer(), _run.getRun()));
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
        private FilterHeaderView(ViewURLHelper currentUrl, OldMS2Controller.RunForm form, MS2Run run) throws ServletException, SQLException
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


        private ViewURLHelper clearFilter(ViewURLHelper currentUrl)
        {
            ViewURLHelper newUrl = currentUrl.clone();
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


    // TODO: Make constructors private once spring conversion is done
    public static class CurrentFilterView extends JspView<CurrentFilterView.CurrentFilterBean>
    {
        CurrentFilterView(String[] headers, List<Pair<String, String>> sqlSummaries)
        {
            super("/org/labkey/ms2/currentFilter.jsp", new CurrentFilterBean(headers, sqlSummaries));
        }

        CurrentFilterView(CompareQuery query)
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

        String currentViewParams = getViewContext().cloneViewURLHelper().deleteParameter("run").getRawQuery();

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
        int width = ("https".equals(getViewContext().getViewURLHelper().getScheme()) ? 175 : 100);

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
}
