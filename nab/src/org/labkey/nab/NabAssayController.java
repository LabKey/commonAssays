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

package org.labkey.nab;

import jxl.format.PaperSize;
import jxl.write.*;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.data.*;
import org.labkey.api.defaults.DefaultValueService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.nab.NabUrls;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.*;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.*;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Number;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class NabAssayController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(NabAssayController.class,
            NabUploadWizardAction.class,
            GetNabRunsAction.class,
            GetStudyNabGraphURLAction.class,
            StudyNabGraphAction.class,
            GetStudyNabRunsAction.class
        );

    public NabAssayController()
    {
        super();
        setActionResolver(_resolver);
    }

    public static class NabUrlsImpl implements NabUrls
    {
        @Override
        public ActionURL getSampleXLSTemplateURL(Container container, ExpProtocol protocol)
        {
            ActionURL url = new ActionURL(SampleSpreadsheetTemplateAction.class, container);
            url.addParameter("protocol", protocol.getRowId());
            return url;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getViewContext().getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class RenderAssayForm
    {
        private boolean _newRun;
        private int _rowId = -1;
        private DilutionCurve.FitType _fitType;

        public boolean isNewRun()
        {
            return _newRun;
        }

        public void setNewRun(boolean newRun)
        {
            _newRun = newRun;
        }

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }

        public String getFitType()
        {
            return _fitType != null ? _fitType.name() : null;
        }

        public void setFitType(String fitType)
        {
            _fitType = fitType != null ? DilutionCurve.FitType.valueOf(fitType) : null;
        }

        public DilutionCurve.FitType getFitTypeEnum()
        {
            return _fitType;
        }
    }

    public static class GraphForm extends RenderAssayForm
    {
        private int _firstSample = 0;
        private int _maxSamples = -1;
        private int _height = -1;
        private int _width = -1;

        public int getFirstSample()
        {
            return _firstSample;
        }

        public void setFirstSample(int firstSample)
        {
            _firstSample = firstSample;
        }

        public int getMaxSamples()
        {
            return _maxSamples;
        }

        public void setMaxSamples(int maxSamples)
        {
            _maxSamples = maxSamples;
        }

        public int getHeight()
        {
            return _height;
        }

        public void setHeight(int height)
        {
            _height = height;
        }

        public int getWidth()
        {
            return _width;
        }

        public void setWidth(int width)
        {
            _width = width;
        }
    }

    private static class DuplicateDataFileRunView extends RunListQueryView
    {
        private Luc5Assay _assay;
        private ExpRun _run;

        public DuplicateDataFileRunView(Luc5Assay assay, ExpRun run, ExpProtocol protocol, ViewContext context)
        {
            super(protocol, context);
            setShowExportButtons(false);
            _assay = assay;
            _run = run;
        }

        public DataView createDataView()
        {
            DataView view = super.createDataView();
            DataRegion rgn = view.getDataRegion();
            ButtonBar bar = rgn.getButtonBar(DataRegion.MODE_GRID);
            ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
            selectButton.setDisplayPermission(InsertPermission.class);
            bar.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(InsertPermission.class);
            bar.add(clearButton);

            ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete", DataRegion.MODE_GRID, ActionButton.Action.POST);
            deleteButton.setRequiresSelection(true);
            deleteButton.setDisplayPermission(DeletePermission.class);
            bar.add(deleteButton);

            SimpleFilter filter;
            if (view.getRenderContext().getBaseFilter() instanceof SimpleFilter)
            {
                filter = (SimpleFilter) view.getRenderContext().getBaseFilter();
            }
            else
            {
                filter = new SimpleFilter(view.getRenderContext().getBaseFilter());
            }
            filter.addCondition("Name", _assay.getDataFile().getName());
            filter.addCondition("RowId", _run.getRowId(), CompareType.NEQ);
            view.getRenderContext().setBaseFilter(filter);
            return view;
        }

        public void setRun(ExpRun run)
        {
            _run = run;
        }
    }

    public static class RenderAssayBean
    {
        private ViewContext _context;
        private NabAssayRun _assay;
        private boolean _newRun;
        private boolean _printView;
        private Set<String> _hiddenRunColumns;
        private Map<String, Object> _displayProperties;
        private DilutionCurve.FitType _fitType;
        private Boolean _dupFile = null;

        public RenderAssayBean(ViewContext context, NabAssayRun assay, DilutionCurve.FitType fitType, boolean newRun, boolean printView)
        {
            _context = context;
            _assay = assay;
            _newRun = newRun;
            _fitType = fitType;
            _printView = printView;
            _hiddenRunColumns = new HashSet<String>();
            _hiddenRunColumns.add(ExpRunTable.Column.RunGroups.name());
            _hiddenRunColumns.add(ExpRunTable.Column.Links.name());
            _hiddenRunColumns.add(ExpRunTable.Column.Flag.name());
            _hiddenRunColumns.add(AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME);
            _hiddenRunColumns.add(AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME);
            _hiddenRunColumns.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
        }

        public Map<String, Object> getRunDisplayProperties()
        {
            if (_displayProperties == null)
            {
                Map<PropertyDescriptor, Object> allProperties = _assay.getRunDisplayProperties(_context);
                _displayProperties = new LinkedHashMap<String, Object>();
                for (Map.Entry<PropertyDescriptor, Object> entry : allProperties.entrySet())
                {
                    PropertyDescriptor property = entry.getKey();
                    if (!_hiddenRunColumns.contains(property.getName()))
                    {
                        if (NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME.equals(property.getName()) && _fitType != null)
                        {
                            _displayProperties.put(property.getNonBlankCaption(), _fitType.getLabel());
                        }
                        else
                        {
                            Object value = entry.getValue();
                            if (value != null)
                            {
                                _displayProperties.put(property.getNonBlankCaption(), formatValue(property, value));
                            }
                        }
                    }
                }
            }
            return _displayProperties;
        }

        public Object formatValue(PropertyDescriptor pd, Object value)
        {
            if (pd.getFormat() != null)
            {
                if (pd.getPropertyType() == PropertyType.DOUBLE)
                {
                    DecimalFormat format = new DecimalFormat(pd.getFormat());
                    value = format.format(value);
                }
                if (pd.getPropertyType() == PropertyType.DATE_TIME)
                {
                    DateFormat format = new SimpleDateFormat(pd.getFormat());
                    value = format.format((Date) value);
                }
            }
            else if (pd.getPropertyType() == PropertyType.DATE_TIME && value instanceof Date)
            {
                Date date = (Date) value;
                if (date.getHours() == 0 &&
                        date.getMinutes() == 0 &&
                        date.getSeconds() == 0)
                {
                    value = DateUtil.formatDate(date);
                }
                else
                {
                    value = DateUtil.formatDateTime(date);
                }
            }
            return value;
        }


        public List<NabAssayRun.SampleResult> getSampleResults()
        {
            return _assay.getSampleResults();
        }

        public DilutionCurve.FitType getFitType()
        {
            return _fitType;
        }

        public NabAssayRun getAssay()
        {
            return _assay;
        }

        public boolean isNewRun()
        {
            return _newRun;
        }

        private boolean isDuplicateDataFile()
        {
            if (_dupFile == null)
            {
                ResultSet rs = null;
                try
                {
                    SimpleFilter filter = new SimpleFilter("ProtocolLsid", _assay.getProtocol().getLSID());
                    filter.addCondition("Name", _assay.getDataFile().getName());
                    filter.addCondition("RowId", _assay.getRun().getRowId(), CompareType.NEQ);
                    rs = Table.select(ExperimentService.get().getTinfoExperimentRun(), Table.ALL_COLUMNS, filter, null);
                    _dupFile = rs.next();
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
                finally
                {
                    if (rs != null)
                        try { rs.close(); } catch (SQLException e) { /* fall through */ }
                }
            }
            return _dupFile;
        }

        public QueryView getDuplicateDataFileView(ViewContext context)
        {
            if (isDuplicateDataFile())
            {
                return new DuplicateDataFileRunView(_assay, _assay.getRun(), _assay.getProtocol(), context);
            }
            else
                return null;
        }

        public HttpView getControlsView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/controlSummary.jsp", this);
        }

        public HttpView getCutoffsView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/cutoffDilutions.jsp", this);
        }

        public HttpView getGraphView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/runGraph.jsp", this);
        }

        public HttpView getSamplePropertiesView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/sampleProperties.jsp", this);
        }

        public HttpView getRunPropertiesView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/runProperties.jsp", this);
        }

        public HttpView getSampleDilutionsView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/sampleDilutions.jsp", this);
        }

        public HttpView getPlateDataView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/plateData.jsp", this);
        }

        public HttpView getRunNotesView()
        {
            return new JspView<RenderAssayBean>("/org/labkey/nab/view/runNotes.jsp", this);
        }

        public boolean needsCurveNote()
        {
            return _assay.getRenderedCurveFitType() != _assay.getSavedCurveFitType();
        }

        public boolean needsNewRunNote()
        {
            return  !isPrintView() && isNewRun();
        }

        public boolean needsDupFileNote()
        {
            return !isPrintView() &&  isDuplicateDataFile();
        }

        public boolean needsNotesView()
        {
            return needsCurveNote() || needsNewRunNote() || needsDupFileNote();
        }

        public boolean isPrintView()
        {
            return _printView;
        }

        public HttpView getDiscussionView(ViewContext context)
        {
            ExpRun run = _assay.getRun();
            ActionURL pageUrl = new ActionURL(DetailsAction.class, run.getContainer());
            pageUrl.addParameter("rowId", "" + run.getRowId());
            String discussionTitle = "Discuss Run " + run.getRowId() + ": " + run.getName();
            String entityId = run.getLSID();
            DiscussionService.Service service = DiscussionService.get();
            return service.getDisussionArea(context,
                    entityId, pageUrl, discussionTitle, true, false);
        }

        public int getRunId()
        {
            return _assay.getRun().getRowId();
        }

    }

    public static class HeaderBean
    {
        private ActionURL _printURL;
        private ActionURL _datafileURL;
        private boolean _writer;

        public HeaderBean(ViewContext context, ActionURL printLink, ActionURL dataFileLink)
        {
            _printURL = printLink;
            _datafileURL = dataFileLink;
            _writer = context.getContainer().hasPermission(context.getUser(), InsertPermission.class);
        }

        public boolean showPrintView()
        {
            return _printURL != null;
        }

        public ActionURL getPrintURL()
        {
            return _printURL;
        }

        public ActionURL getDatafileURL()
        {
            return _datafileURL;
        }

        public boolean showNewRunLink()
        {
            return _writer;
        }
    }

    protected NabAssayProvider getProvider(ExpRun run)
    {
        AssayProvider provider = AssayService.get().getProvider(run.getProtocol());
        if (!(provider instanceof NabAssayProvider))
            throw new IllegalArgumentException("Run " + run.getRowId() + " is not a NAb run.");
        return (NabAssayProvider) provider;
    }

    protected NabDataHandler getDataHandler(ExpRun run)
    {
        return getProvider(run).getDataHandler();
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadDatafileAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            if (form.getRowId() < 0)
            {
                throw new NotFoundException("No run specified");
            }
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }
            File file = getDataHandler(run).getDataFile(run);
            if (file == null)
            {
                throw new NotFoundException("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
            }
            PageFlowUtil.streamFile(getViewContext().getResponse(), file, true);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException("Not Yet Implemented");
        }
    }

    private class NabDetailsHeaderView extends AssayHeaderView
    {
        private int _runId;
        public NabDetailsHeaderView(ExpProtocol protocol, AssayProvider provider, int runId)
        {
            super(protocol, provider, true, true, null);
            _runId = runId;
        }

        @Override
        public List<NavTree> getLinks()
        {
            List<NavTree> links = new ArrayList<NavTree>();

            links.add(new NavTree("View Runs", PageFlowUtil.addLastFilterParameter(PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getViewContext().getContainer(), _protocol, _containerFilter))));
            links.add(new NavTree("View Results", PageFlowUtil.addLastFilterParameter(PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getViewContext().getContainer(), _protocol, _containerFilter))));

            if (getViewContext().hasPermission(InsertPermission.class))
            {
                links.add(new NavTree(AbstractAssayProvider.IMPORT_DATA_LINK_NAME, _provider.getImportURL(getContainer(), _protocol)));

                if (getViewContext().hasPermission(DeletePermission.class))
                {
                    ActionURL reRunURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                    reRunURL.addParameter("rowId", _protocol.getRowId());
                    reRunURL.addParameter("reRunId", _runId);
                    links.add(new NavTree("Delete and Re-import", reRunURL));
                }
            }

            NavTree changeCurveMenu = new NavTree("Change Curve Type");
            for (DilutionCurve.FitType type : DilutionCurve.FitType.values())
            {
                ActionURL changeCurveURL = getViewContext().cloneActionURL();
                changeCurveURL.replaceParameter("fitType", type.name());
                changeCurveMenu.addChild(type.getLabel(), changeCurveURL);
            }
            links.add(changeCurveMenu);

            ActionURL downloadURL = new ActionURL(DownloadDatafileAction.class, getContainer()).addParameter("rowId", _runId);
            links.add(new NavTree("Download Datafile", downloadURL));
            links.add(new NavTree("Print", getViewContext().cloneActionURL().addParameter("_print", "true")));
            return links;
        }
    }

    private Date getCustomViewModifiedDate(ExpRun run)
    {
        CustomView runView = QueryService.get().getCustomView(getUser(), getContainer(),
               AssaySchema.NAME, AssayService.get().getRunsTableName(run.getProtocol()), NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);
        if (runView != null)
            return runView.getModified();
        return null;
    }

    private NabAssayRun getCachedRun(ExpRun run)
    {
        Pair<NabAssayRun, Date> cache = (Pair<NabAssayRun, Date>) getViewContext().getSession().getAttribute(LAST_NAB_RUN_KEY);
        if (cache == null)
            return null;
        NabAssayRun assay = cache.getKey();
        Date customViewModified = getCustomViewModifiedDate(run);
        // There's no custom view, so it can't have been modified since we cached the run:
        if (customViewModified == null)
            return assay;
        // Check the view modification time against the time we cached the run.  If the view has been changed since
        // the run was cached, return null.
        Date cachedDate = cache.getValue();
        if (cachedDate.after(customViewModified))
            return assay;
        return null;
    }

    private NabAssayRun getNabAssayRun(ExpRun run, DilutionCurve.FitType fit) throws ExperimentException
    {
        // cache last NAb assay run in session.  This speeds up the case where users bring up details view and
        // then immediately hit the 'print' button.
        NabAssayRun assay = getCachedRun(run);
        if (fit != null || assay == null ||
                (assay.getRunRowId() != null && run.getRowId() != assay.getRunRowId().intValue()) ||
                (assay.getRun() != null && run.getRowId() != assay.getRun().getRowId()))
        {
            try
            {
                assay = getDataHandler(run).getAssayResults(run, getUser(), fit);
                if (assay != null && fit == null)
                    getViewContext().getSession().setAttribute(LAST_NAB_RUN_KEY, new Pair<NabAssayRun, Date>(assay, new Date()));
            }
            catch (SinglePlateNabDataHandler.MissingDataFileException e)
            {
                throw new NotFoundException(e.getMessage());
            }
        }
        return assay;
    }

    private static final String LAST_NAB_RUN_KEY = NabAssayController.class.getName() + "/LastNAbRun";

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class DetailsAction extends SimpleViewAction<RenderAssayForm>
    {
        private int _runRowId;
        private ExpProtocol _protocol;

        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            _runRowId = form.getRowId();
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }
            if (!run.getContainer().equals(getContainer()))
            {
                // Need to redirect
                ActionURL newURL = getViewContext().getActionURL().clone();
                newURL.setContainer(run.getContainer());
                throw new RedirectException(newURL);
            }

            // 8128 : NAb should show only print details view if user doesn't have permission to container
            // Using the permissions annotations, we've already checked that the user has permissions
            // at this point.  However, if the user can view the dataset but not the container,
            // lots of links will be broken. The workaround for now is to redirect to a print view.
            if (!isPrint() && !getContainer().hasPermission(getUser(), ReadPermission.class))
            {
                throw new RedirectException(getViewContext().getActionURL().clone().addParameter("_print", true));
            }

            NabAssayRun assay = getNabAssayRun(run, form.getFitTypeEnum());
            _protocol = run.getProtocol();
            AbstractPlateBasedAssayProvider provider = (AbstractPlateBasedAssayProvider) AssayService.get().getProvider(_protocol);

            HttpView view = new JspView<RenderAssayBean>("/org/labkey/nab/view/runDetails.jsp",
                    new RenderAssayBean(getViewContext(), assay, form.getFitTypeEnum(), form.isNewRun(), isPrint()));
            if (!isPrint())
                view = new VBox(new NabDetailsHeaderView(_protocol, provider, _runRowId), view);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getContainer(), _protocol, _runRowId);
            return root.addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _runRowId + " Details");
        }
    }

    public static class GraphSelectedForm
    {
        private int _protocolId;
        private int[] _id;
        private String _captionColumn;
        private String _chartTitle;
        private DilutionCurve.FitType _fitType;
        private int _height = -1;
        private int _width = -1;

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }

        public int getProtocolId()
        {
            return _protocolId;
        }

        public void setProtocolId(int protocolId)
        {
            _protocolId = protocolId;
        }

        public String getCaptionColumn()
        {
            return _captionColumn;
        }

        public void setCaptionColumn(String captionColumn)
        {
            _captionColumn = captionColumn;
        }

        public String getChartTitle()
        {
            return _chartTitle;
        }

        public void setChartTitle(String chartTitle)
        {
            _chartTitle = chartTitle;
        }

        public String getFitType()
        {
            return _fitType != null ? _fitType.name() : null;
        }

        public void setFitType(String fitType)
        {
            _fitType = fitType != null ? DilutionCurve.FitType.valueOf(fitType) : null;
        }

        public DilutionCurve.FitType getFitTypeEnum()
        {
            return _fitType;
        }

        public int getHeight()
        {
            return _height;
        }

        public void setHeight(int height)
        {
            _height = height;
        }

        public int getWidth()
        {
            return _width;
        }

        public void setWidth(int width)
        {
            _width = width;
        }
    }

    public static class GraphSelectedBean
    {
        private ViewContext _context;
        private int[] _cutoffs;
        private ExpProtocol _protocol;
        private int[] _dataObjectIds;
        private QueryView _queryView;
        private int[] _graphableIds;
        private String _captionColumn;
        private String _chartTitle;

        public GraphSelectedBean(ViewContext context, ExpProtocol protocol, int[] cutoffs, int[] dataObjectIds, String captionColumn, String chartTitle)
        {
            _context = context;
            _cutoffs = cutoffs;
            _protocol = protocol;
            _dataObjectIds = dataObjectIds;
            _captionColumn = captionColumn;
            _chartTitle = chartTitle;
        }

        public int[] getCutoffs()
        {
            return _cutoffs;
        }

        public ExpProtocol getProtocol()
        {
            return _protocol;
        }

        public String getCaptionColumn()
        {
            return _captionColumn;
        }

        public String getChartTitle()
        {
            return _chartTitle;
        }

        public int[] getGraphableObjectIds() throws IOException, SQLException
        {
            if (_graphableIds == null)
            {
                QueryView dataView = getQueryView();
                ResultSet rs = null;
                try
                {
                    rs = dataView.getResultSet();
                    Set<Integer> graphableIds = new HashSet<Integer>();
                    while (rs.next())
                        graphableIds.add(rs.getInt("ObjectId"));
                    _graphableIds = new int[graphableIds.size()];
                    int i = 0;
                    for (Integer id : graphableIds)
                        _graphableIds[i++] = id.intValue();
                }
                finally
                {
                    if (rs != null) try { rs.close(); } catch (SQLException e) {}
                }
            }
            return _graphableIds;
        }

        public QueryView getQueryView()
        {
            if (_queryView == null)
            {
                AssayProvider provider = AssayService.get().getProvider(_protocol);
                QueryView dataView = new NabAssayProvider.NabResultsQueryView(_protocol, _context, provider)
                {
                    public DataView createDataView()
                    {
                        DataView view = super.createDataView();
                        SimpleFilter filter = new SimpleFilter();
                        SimpleFilter existingFilter = (SimpleFilter) view.getRenderContext().getBaseFilter();
                        if (existingFilter != null)
                            filter.addAllClauses(existingFilter);
                        List<Integer> objectIds = new ArrayList<Integer>(_dataObjectIds.length);
                        for (int dataObjectId : _dataObjectIds)
                            objectIds.add(new Integer(dataObjectId));
                        filter.addInClause("ObjectId", objectIds);
                        view.getDataRegion().setRecordSelectorValueColumns("ObjectId");
                        view.getRenderContext().setBaseFilter(filter);
                        return view;
                    }
                };
                _queryView = dataView;
            }
            return _queryView;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class GraphSelectedAction extends SimpleViewAction<GraphSelectedForm>
    {
        private ExpProtocol _protocol;
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            _protocol = ExperimentService.get().getExpProtocol(form.getProtocolId());
            if (_protocol == null)
            {
                throw new NotFoundException();
            }
            int[] objectIds;
            if (form.getId() != null)
                objectIds = form.getId();
            else
            {
                Set<String> objectIdStrings = DataRegionSelection.getSelected(getViewContext(), false);
                if (objectIdStrings == null || objectIdStrings.size() == 0)
                {
                    throw new NotFoundException("No samples specified.");
                }
                objectIds = new int[objectIdStrings.size()];
                int idx = 0;
                for (String objectIdString : objectIdStrings)
                    objectIds[idx++] = Integer.parseInt(objectIdString);
            }

            Set<Integer> cutoffSet = new HashSet<Integer>();
            NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(_protocol);
            Map<DilutionSummary, NabAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getUser(), form.getFitTypeEnum(), objectIds);
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }

            GraphSelectedBean bean = new GraphSelectedBean(getViewContext(), _protocol, toArray(cutoffSet), objectIds, form.getCaptionColumn(), form.getChartTitle());

            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/multiRunGraph.jsp", bean);

            return new VBox(new AssayHeaderView(_protocol, provider, false, true, null), multiGraphView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol);
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild("Graph Selected Specimens");
        }
    }

    public static class DeleteRunForm
    {
        private int _rowId;
        private boolean _reupload;

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }

        public boolean isReupload()
        {
            return _reupload;
        }

        public void setReupload(boolean reupload)
        {
            _reupload = reupload;
        }
    }

    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteRunAction extends SimpleViewAction<DeleteRunForm>
    {
        public ModelAndView getView(DeleteRunForm deleteRunForm, BindException errors) throws Exception
        {
            if (deleteRunForm.getRowId() == 0)
            {
                throw new NotFoundException("No run specified");
            }
            ExpRun run = ExperimentService.get().getExpRun(deleteRunForm.getRowId());
            if (run == null)
                throw new NotFoundException("Run " + deleteRunForm.getRowId() + " does not exist.");
            File file = null;
            if (deleteRunForm.isReupload())
            {
                file = getDataHandler(run).getDataFile(run);
                if (file == null)
                {
                    throw new NotFoundException("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
                }
            }

            run.delete(getUser());

            if (deleteRunForm.isReupload())
            {
                ActionURL reuploadURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                reuploadURL.addParameter("dataFile", file.getPath());
                throw new RedirectException(reuploadURL);
            }
            throw new RedirectException(PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), run.getProtocol()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException("Expected redirect did not occur.");
        }
    }

    private int[] toArray(Collection<Integer> integerList)
    {
        int[] arr = new int[integerList.size()];
        int i = 0;
        for (Integer cutoff : integerList)
            arr[i++] = cutoff.intValue();
        return arr;
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class MultiGraphAction extends SimpleViewAction<GraphSelectedForm>
    {
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            int[] ids = form.getId();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(form.getProtocolId());
            NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
            Map<DilutionSummary, NabAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getUser(), form.getFitTypeEnum(), ids);
            Set<Integer> cutoffSet = new HashSet<Integer>();
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }

            NabGraph.Config config = new NabGraph.Config();
            config.setCutoffs(toArray(cutoffSet));
            config.setLockAxes(false);
            config.setCaptionColumn(form.getCaptionColumn());
            config.setChartTitle(form.getChartTitle());
            if (form.getHeight() > 0)
                config.setHeight(form.getHeight());
            if (form.getWidth() > 0)
            config.setWidth(form.getWidth());
            NabGraph.renderChartPNG(getViewContext().getResponse(), summaries, config);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class GraphAction extends SimpleViewAction<GraphForm>
    {
        public ModelAndView getView(GraphForm form, BindException errors) throws Exception
        {
            if (form.getRowId() == -1)
                throw new NotFoundException("Run ID not specified.");
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            NabAssayRun assay = getNabAssayRun(run, form.getFitTypeEnum());
            if (assay == null)
                throw new NotFoundException("Could not load NAb results for run " + form.getRowId() + ".");

            NabGraph.Config config = new NabGraph.Config();
            config.setCutoffs(assay.getCutoffs());
            config.setLockAxes(assay.isLockAxes());
            config.setFirstSample(form.getFirstSample());
            config.setMaxSamples(form.getMaxSamples());
            if (form.getHeight() > 0)
                config.setHeight(form.getHeight());
            if (form.getWidth() > 0)
                config.setWidth(form.getWidth());

            NabGraph.renderChartPNG(getViewContext().getResponse(), assay, config);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class SampleSpreadsheetForm
    {
        private int _protocol;

        public int getProtocol()
        {
            return _protocol;
        }

        public void setProtocol(int protocol)
        {
            _protocol = protocol;
        }
    }

    private static class SampleTemplateWriter extends ExcelWriter
    {
        private Container _container;
        private User _user;
        private Domain _sampleDomain;
        private List<WellGroupTemplate> _sampleGroups;

        public SampleTemplateWriter(Container container, User user, Domain sampleDomain, List<WellGroupTemplate> sampleGroups)
        {
            _sampleDomain = sampleDomain;
            _sampleGroups = sampleGroups;
            _container = container;
            _user = user;
        }

        @Override
        public void renderSheet(WritableWorkbook workbook, int sheetNumber)
        {
            WritableSheet sheet = workbook.createSheet(getSheetName(), sheetNumber);
            sheet.getSettings().setPaperSize(PaperSize.LETTER);

            try
            {
                // Render the header row:
                List<String> headers = new ArrayList<String>();
                headers.add(PlateSampleFilePropertyHelper.WELLGROUP_COLUMN);
                headers.add(PlateSampleFilePropertyHelper.PLATELOCATION_COLUMN);

                Map<DomainProperty, Object> defaultValues = DefaultValueService.get().getDefaultValues(_container, _sampleDomain);
                Map<Integer, Object> columnToDefaultValue = new HashMap<Integer, Object>();
                for (DomainProperty property : _sampleDomain.getProperties())
                {
                    columnToDefaultValue.put(headers.size(), defaultValues.get(property));
                    headers.add(property.getName());
                }

                for (int column = 0; column < headers.size(); column++)
                {
                    String header = headers.get(column);
                    WritableCell cell = new Label(column, 0, header, getBoldFormat());
                    sheet.addCell(cell);
                }

                // Render the rows, which just contain well group names:
                for (int group = 0; group < _sampleGroups.size(); group++)
                {
                    int row = group + 1;
                    WellGroupTemplate sample = _sampleGroups.get(group);
                    WritableCell wellgroupNameCell = new Label(0, row, sample.getName());
                    sheet.addCell(wellgroupNameCell);
                    WritableCell plateLocationCell = new Label(1, row, sample.getPositionDescription());
                    sheet.addCell(plateLocationCell);
                    for (int column = 2; column < headers.size(); column++)
                    {
                        Object defaultValue = columnToDefaultValue.get(column);
                        if (defaultValue != null)
                        {
                            WritableCell defaultValueCell;
                            if (defaultValue instanceof Number)
                                defaultValueCell = new jxl.write.Number(column, row, ((Number) defaultValue).doubleValue());
                            else if (defaultValue instanceof Date)
                                defaultValueCell = new DateTime(column, row, (Date) defaultValue);
                            else if (defaultValue instanceof Boolean)
                                defaultValueCell = new jxl.write.Boolean(column, row, ((Boolean) defaultValue).booleanValue());
                            else
                                defaultValueCell = new Label(column, row, defaultValue.toString());
                            sheet.addCell(defaultValueCell);
                        }
                    }
                }
            }
            catch (WriteException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SampleSpreadsheetTemplateAction extends ExportAction<SampleSpreadsheetForm>
    {
        @Override
        public void export(SampleSpreadsheetForm sampleSpreadsheetForm, HttpServletResponse response, BindException errors) throws Exception
        {
            ViewContext context = getViewContext();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(sampleSpreadsheetForm.getProtocol());
            if (protocol == null)
            {
                throw new NotFoundException("Protocol " + sampleSpreadsheetForm.getProtocol() + " does not exist.");
            }

            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider == null || !(provider instanceof NabAssayProvider))
            {
                String message = "Protocol " + sampleSpreadsheetForm.getProtocol() + " is not a NAb protocol: " +
                        (protocol != null ? protocol.getName() : "null");
                throw new NotFoundException(message);
            }
            NabAssayProvider nabProvider = ((NabAssayProvider) provider);
            Domain sampleDomain = nabProvider.getSampleWellGroupDomain(protocol);
            PlateTemplate template = nabProvider.getPlateTemplate(context.getContainer(), protocol);
            if (template == null)
            {
                throw new NotFoundException("The plate template for this assay design could not be found.  It may have been deleted by an administrator.");
            }
            List<WellGroupTemplate> sampleGroups = new ArrayList<WellGroupTemplate>();
            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.SPECIMEN)
                    sampleGroups.add(group);
            }

            ExcelWriter xl = new SampleTemplateWriter(getViewContext().getContainer(), getUser(), sampleDomain, sampleGroups);
            xl.setFilenamePrefix("metadata");
            xl.write(response);
        }
    }
}
