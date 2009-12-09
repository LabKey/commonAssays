/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.WellData;
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
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class NabAssayController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(NabAssayController.class,
            NabUploadWizardAction.class,
            GetNabRunsAction.class
        );

    public NabAssayController()
    {
        super();
        setActionResolver(_resolver);
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


        public RenderAssayBean(ViewContext context, NabAssayRun assay, boolean newRun, boolean printView)
        {
            _context = context;
            _assay = assay;
            _newRun = newRun;
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
                        Object value = entry.getValue();
                        if (value != null)
                        {
                            _displayProperties.put(property.getNonBlankCaption(), formatValue(property, value));
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
            ResultSet rs = null;
            try
            {
                SimpleFilter filter = new SimpleFilter("ProtocolLsid", _assay.getProtocol().getLSID());
                filter.addCondition("Name", _assay.getDataFile().getName());
                filter.addCondition("RowId", _assay.getRun().getRowId(), CompareType.NEQ);
                rs = Table.select(ExperimentService.get().getTinfoExperimentRun(), Table.ALL_COLUMNS, filter, null);
                return rs.next();
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

        public QueryView getDuplicateDataFileView(ViewContext context)
        {
            if (isDuplicateDataFile())
            {
                return new DuplicateDataFileRunView(_assay, _assay.getRun(), _assay.getProtocol(), context);
            }
            else
                return null;
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

    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadDatafileAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            if (form.getRowId() < 0)
                HttpView.throwNotFound("No run specified");
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
                HttpView.throwNotFound("Run " + form.getRowId() + " does not exist.");
            File file = NabDataHandler.getDataFile(run);
            if (file == null)
                HttpView.throwNotFound("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
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
        public NabDetailsHeaderView(ExpProtocol protocol, AssayProvider provider, int runId)
        {
            super(protocol, provider, false, null);
            if (getViewContext().hasPermission(ACL.PERM_INSERT))
            {
                _links.put(AbstractAssayProvider.IMPORT_DATA_LINK_NAME, provider.getImportURL(getContainer(), protocol));

                if (getViewContext().hasPermission(ACL.PERM_DELETE))
                {
                    ActionURL reRunURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                    reRunURL.addParameter("rowId", protocol.getRowId());
                    reRunURL.addParameter("reRunId", runId);
                    _links.put("delete and re-import", reRunURL);
                }
            }
            ActionURL downloadURL = new ActionURL(DownloadDatafileAction.class, getContainer()).addParameter("rowId", runId);
            _links.put("download datafile", downloadURL);
            _links.put("print", getViewContext().cloneActionURL().addParameter("_print", "true"));
        }
    }

    private NabAssayRun getNabAssayRun(ExpRun run) throws ExperimentException
    {
        // cache last NAb assay run in session.  This speeds up the case where users bring up details view and
        // then immediately hit the 'print' button.
        NabAssayRun assay = (NabAssayRun) getViewContext().getSession().getAttribute(LAST_NAB_RUN_KEY);
        if (assay == null ||
                (assay.getRunRowId() != null && run.getRowId() != assay.getRunRowId().intValue()) ||
                (assay.getRun() != null && run.getRowId() != assay.getRun().getRowId()))
        {
            try
            {
                assay = NabDataHandler.getAssayResults(run, getUser());
                if (assay != null)
                    getViewContext().getSession().setAttribute(LAST_NAB_RUN_KEY, assay);
            }
            catch (NabDataHandler.MissingDataFileException e)
            {
                HttpView.throwNotFound(e.getMessage());
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
                HttpView.throwNotFound("Run " + form.getRowId() + " does not exist.");
                return null;
            }
            if (!run.getContainer().equals(getContainer()))
            {
                // Need to redirect
                ActionURL newURL = getViewContext().getActionURL().clone();
                newURL.setContainer(run.getContainer());
                HttpView.throwRedirect(newURL);
            }

            // 8128 : NAb should show only print details view if user doesn't have permission to container
            // Using the permissions annotations, we've already checked that the user has permissions
            // at this point.  However, if the user can view the dataset but not the container,
            // lots of links will be broken. The workaround for now is to redirect to a print view.
            if (!isPrint() && !getContainer().hasPermission(getUser(), ReadPermission.class))
                HttpView.throwRedirect(getViewContext().getActionURL().clone().addParameter("_print", true));

            NabAssayRun assay = getNabAssayRun(run);
            _protocol = run.getProtocol();
            AbstractPlateBasedAssayProvider provider = (AbstractPlateBasedAssayProvider) AssayService.get().getProvider(_protocol);

            HttpView view = new JspView<RenderAssayBean>("/org/labkey/nab/runDetails.jsp",
                    new RenderAssayBean(getViewContext(), assay, form.isNewRun(), isPrint()));
            if (!isPrint())
                view = new VBox(new NabDetailsHeaderView(_protocol, provider, _runRowId), view);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol);
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getContainer(), _protocol, _runRowId);
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _runRowId + " Details");
        }
    }

    public static class GraphSelectedForm
    {
        private int _protocolId;
        private int[] _id;
        private String _captionColumn;
        private String _chartTitle;

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
                    rs = dataView.getResultset();
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
                HttpView.throwNotFound();
            int[] objectIds;
            if (form.getId() != null)
                objectIds = form.getId();
            else
            {
                Set<String> objectIdStrings = DataRegionSelection.getSelected(getViewContext(), false);
                if (objectIdStrings == null || objectIdStrings.size() == 0)
                    HttpView.throwNotFound("No samples specified.");
                objectIds = new int[objectIdStrings.size()];
                int idx = 0;
                for (String objectIdString : objectIdStrings)
                    objectIds[idx++] = Integer.parseInt(objectIdString);
            }

            Set<Integer> cutoffSet = new HashSet<Integer>();
            Map<DilutionSummary, NabAssayRun> summaries = NabDataHandler.getDilutionSummaries(getUser(), objectIds);
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }

            GraphSelectedBean bean = new GraphSelectedBean(getViewContext(), _protocol, toArray(cutoffSet), objectIds, form.getCaptionColumn(), form.getChartTitle());

            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/multiRunGraph.jsp", bean);

            return new VBox(new AssayHeaderView(_protocol, AssayService.get().getProvider(_protocol), false, null), multiGraphView);
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
                HttpView.throwNotFound("No run specified");
            ExpRun run = ExperimentService.get().getExpRun(deleteRunForm.getRowId());
            if (run == null)
                HttpView.throwNotFound("Run " + deleteRunForm.getRowId() + " does not exist.");
            File file = null;
            if (deleteRunForm.isReupload())
            {
                file = NabDataHandler.getDataFile(run);
                if (file == null)
                    HttpView.throwNotFound("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
            }

            ExperimentService.get().deleteExperimentRunsByRowIds(getContainer(), getUser(), deleteRunForm.getRowId());

            if (deleteRunForm.isReupload())
            {
                ActionURL reuploadURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                reuploadURL.addParameter("dataFile", file.getPath());
                HttpView.throwRedirect(reuploadURL);
            }
            else
                HttpView.throwRedirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), run.getProtocol()));
            return null;
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
    public class MultiGraphAction extends SimpleViewAction<GraphSelectedForm>
    {
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            int[] ids = form.getId();
            Map<DilutionSummary, NabAssayRun> summaries = NabDataHandler.getDilutionSummaries(getUser(), ids);
            Set<Integer> cutoffSet = new HashSet<Integer>();
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            renderChartPNG(getViewContext().getResponse(), summaries, toArray(cutoffSet), false, form.getCaptionColumn(), form.getChartTitle());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class GraphAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            NabAssayRun assay = getNabAssayRun(run);
            renderChartPNG(getViewContext().getResponse(), assay, assay.isLockAxes(), null, null);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    private static final Color[] GRAPH_COLORS = {
            ChartColor.BLUE,
            ChartColor.RED,
            ChartColor.DARK_GREEN,
            ChartColor.DARK_YELLOW,
            ChartColor.MAGENTA
    };

    private String getDefaultCaption(DilutionSummary summary, boolean longForm)
    {
        String sampleId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
        String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
        Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
        Date date = (Date) summary.getWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
        NabMaterialKey materialKey = new NabMaterialKey(sampleId, participantId, visitId, date);
        return materialKey.getDisplayString(longForm);
    }

    private String formatCaption(Object captionValue)
    {
        if (captionValue instanceof Date)
        {
            Date date = (Date) captionValue;
            if (date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0)
                return DateUtil.formatDate(date);
            else
                return DateUtil.formatDateTime(date);
        }
        else
            return captionValue.toString();
    }

    private void renderChartPNG(HttpServletResponse response, Map<DilutionSummary, NabAssayRun> summaries, int[] cutoffs, boolean lockAxes, String captionColumn, String chartTitle) throws IOException, DilutionCurve.FitFailedException
    {
        boolean longCaptions = false;
        Set<String> shortCaptions = new HashSet<String>();
        for (DilutionSummary summary : summaries.keySet())
        {
            String shortCaption = getDefaultCaption(summary, false);
            if (shortCaptions.contains(shortCaption))
                longCaptions = true;
            shortCaptions.add(shortCaption);
        }
        List<Pair<String, DilutionSummary>> summaryMap = new ArrayList<Pair<String, DilutionSummary>>();
        for (Map.Entry<DilutionSummary, NabAssayRun> sampleEntry : summaries.entrySet())
        {
            String caption = null;
            DilutionSummary summary = sampleEntry.getKey();
            if (captionColumn != null)
            {
                Object value = summary.getWellGroup().getProperty(captionColumn);
                if (value != null)
                    caption = formatCaption(value);
                else
                {
                    Map<PropertyDescriptor, Object> runProperties = sampleEntry.getValue().getRunProperties();
                    for (Map.Entry<PropertyDescriptor, Object> runProperty : runProperties.entrySet())
                    {
                        if (captionColumn.equals(runProperty.getKey().getName()) && runProperty.getValue() != null)
                            caption = formatCaption(runProperty.getValue());
                    }
                }
            }
            if (caption == null || caption.length() == 0)
                caption = getDefaultCaption(summary, longCaptions);
            summaryMap.add(new Pair<String, DilutionSummary>(caption, summary));
        }
        renderChartPNG(response, summaryMap, cutoffs, lockAxes, chartTitle);
    }

    private void renderChartPNG(HttpServletResponse response, NabAssayRun assay, boolean lockAxes, String captionColumn, String chartTitle) throws IOException, DilutionCurve.FitFailedException
    {
        Map<DilutionSummary, NabAssayRun> samples = new LinkedHashMap<DilutionSummary, NabAssayRun>();
        for (DilutionSummary summary : assay.getSummaries())
            samples.put(summary, assay);
        renderChartPNG(response, samples, assay.getCutoffs(), lockAxes, captionColumn, chartTitle);
    }

    private void renderChartPNG(HttpServletResponse response, List<Pair<String, DilutionSummary>> dilutionSummaries, int[] cutoffs, boolean lockAxes, String chartTitle) throws IOException, DilutionCurve.FitFailedException
    {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, null, "Percent Neutralization", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setDataset(1, pointDataset);
        plot.getRenderer(0).setStroke(new BasicStroke(1.5f));
        if (lockAxes)
            plot.getRangeAxis().setRange(-20, 120);
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(true, true);
        plot.setRenderer(1, pointRenderer);
        pointRenderer.setStroke(new BasicStroke(
                0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[]{4.0f, 4.0f}, 0.0f));
        plot.getRenderer(0).setSeriesVisibleInLegend(false);
        pointRenderer.setShapesFilled(true);
        for (Pair<String, DilutionSummary> summaryEntry : dilutionSummaries)
        {
            String sampleId = summaryEntry.getKey();
            DilutionSummary summary = summaryEntry.getValue();
            XYSeries pointSeries = new XYSeries(sampleId);
            for (WellData well : summary.getWellData())
            {
                double percentage = 100 * summary.getPercent(well);
                double dilution = summary.getDilution(well);
                pointSeries.add(dilution, percentage);
            }
            pointDataset.addSeries(pointSeries);
            int pointDatasetCount = pointDataset.getSeriesCount();
            Color currentColor;
            if (pointDatasetCount <= GRAPH_COLORS.length)
            {
                currentColor = GRAPH_COLORS[pointDatasetCount - 1];
                plot.getRenderer(0).setSeriesPaint(pointDatasetCount - 1, currentColor);
            }
            else
                currentColor = (Color) plot.getRenderer(0).getSeriesPaint(pointDatasetCount - 1);

            try
            {
                DilutionCurve.DoublePoint[] curve = summary.getCurve();
                XYSeries curvedSeries = new XYSeries(sampleId);
                for (DilutionCurve.DoublePoint point : curve)
                    curvedSeries.add(point.getX(), point.getY());
                curvesDataset.addSeries(curvedSeries);
                if (currentColor != null)
                    plot.getRenderer(1).setSeriesPaint(curvesDataset.getSeriesCount() - 1, currentColor);
            }
            catch (DilutionCurve.FitFailedException e)
            {
                // fall through; we'll just graph those that can be graphed.
            }
        }

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution/Concentration"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : cutoffs)
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));
        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 425, 300);
    }
}
