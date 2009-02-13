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

import org.apache.beehive.netui.pageflow.FormData;
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
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.WellData;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.*;
import org.labkey.common.util.Pair;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    @RequiresPermission(ACL.PERM_READ)
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
            selectButton.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(clearButton);

            ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete Selected", DataRegion.MODE_GRID, ActionButton.Action.POST);
            deleteButton.setDisplayPermission(ACL.PERM_DELETE);
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
        private NabAssayRun _assay;
        private boolean _newRun;
        private boolean _printView;
        private Set<String> _hiddenRunColumns;
        private Map<PropertyDescriptor, Object> _displayProperties;


        public RenderAssayBean(NabAssayRun assay, boolean newRun, boolean printView)
        {
            _assay = assay;
            _newRun = newRun;
            _printView = printView;
            _hiddenRunColumns = new HashSet<String>();
            _hiddenRunColumns.add(AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME);
            _hiddenRunColumns.add(AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME);
            _hiddenRunColumns.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
        }

        public Map<PropertyDescriptor, Object> getRunProperties()
        {
            if (_displayProperties == null)
            {
                Map<PropertyDescriptor, Object> allProperties = _assay.getRunProperties();
                _displayProperties = new LinkedHashMap<PropertyDescriptor, Object>();
                for (Map.Entry<PropertyDescriptor, Object> entry : allProperties.entrySet())
                {
                    if (!_hiddenRunColumns.contains(entry.getKey().getName()))
                        _displayProperties.put(entry.getKey(), entry.getValue());
                }
            }
            return _displayProperties;
        }

        public List<NabAssayRun.SampleResult> getSampleResults()
        {
            return _assay.getSampleResults();
        }

        public Luc5Assay getAssay()
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

    public static String getMaterialKey(String specimenId, String participantId, Double visitId, Date date)
    {
        if (specimenId != null)
            return specimenId;
        else if (visitId == null && date != null)
        {
            if (date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0)
                return participantId + ", " + DateUtil.formatDate(date);
            else
                return participantId + ", " + DateUtil.formatDateTime(date);
        }
        else
            return participantId + ", Vst " + visitId;
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
            _writer = context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT);
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

    @RequiresPermission(ACL.PERM_READ)
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
                for (Map.Entry<String, Class<? extends Controller>> entry : provider.getImportActions().entrySet())
                {
                    _links.put(entry.getKey().toLowerCase(), PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), protocol, entry.getValue()));
                }

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


    @RequiresPermission(ACL.PERM_READ)
    public class DetailsAction extends SimpleViewAction<RenderAssayForm>
    {
        private ExpRun _run;
        private ExpProtocol _protocol;

        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
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
            NabAssayRun assay = null;
            try
            {
                assay = NabDataHandler.getAssayResults(run, getUser());
            }
            catch (NabDataHandler.MissingDataFileException e)
            {
                HttpView.throwNotFound(e.getMessage());
            }
            _run = ExperimentService.get().getExpRun(form.getRowId());
            _protocol = _run.getProtocol();
            AbstractPlateBasedAssayProvider provider = (AbstractPlateBasedAssayProvider) AssayService.get().getProvider(_protocol);

            HttpView view = new JspView<RenderAssayBean>("/org/labkey/nab/runDetails.jsp",
                    new RenderAssayBean(assay, form.isNewRun(), isPrint()));
            if (!isPrint())
                view = new VBox(new NabDetailsHeaderView(_protocol, provider, _run.getRowId()), view);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(_run.getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(_run.getContainer(), _protocol);
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(_run.getContainer(), _protocol, _run.getRowId());
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _run.getRowId() + " Details");
        }
    }

    public static class GraphSelectedForm extends FormData
    {
        private int _protocolId;
        private int[] _id;

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
    }

    public static class GraphSelectedBean
    {
        private ViewContext _context;
        private DilutionSummary[] _dilutionSummaries;
        private int[] _cutoffs;
        private ExpProtocol _protocol;
        private int[] _dataObjectIds;
        private QueryView _queryView;
        private int[] _graphableIds;

        public GraphSelectedBean(ViewContext context, ExpProtocol protocol, DilutionSummary[] dilutions, int[] cutoffs, int[] dataObjectIds)
        {
            _context = context;
            _dilutionSummaries = dilutions;
            _cutoffs = cutoffs;
            _protocol = protocol;
            _dataObjectIds = dataObjectIds;
        }

        public DilutionSummary[] getDilutionSummaries()
        {
            return _dilutionSummaries;
        }

        public int[] getCutoffs()
        {
            return _cutoffs;
        }

        public ExpProtocol getProtocol()
        {
            return _protocol;
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

    @RequiresPermission(ACL.PERM_READ)
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
            List<DilutionSummary> summaries = NabDataHandler.getDilutionSummaries(getUser(), objectIds);
            for (DilutionSummary summary :summaries)
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/multiRunGraph.jsp",
                    new GraphSelectedBean(getViewContext(), _protocol, summaries.toArray(new DilutionSummary[summaries.size()]), toArray(cutoffSet), objectIds));

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

    @RequiresPermission(ACL.PERM_DELETE)
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

    @RequiresPermission(ACL.PERM_READ)
    public class MultiGraphAction extends SimpleViewAction<GraphSelectedForm>
    {
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            int[] ids = form.getId();
            List<DilutionSummary> summaries = NabDataHandler.getDilutionSummaries(getUser(), ids);
            Set<Integer> cutoffSet = new HashSet<Integer>();
            for (DilutionSummary summary :summaries)
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            renderChartPNG(getViewContext().getResponse(), summaries.toArray(new DilutionSummary[summaries.size()]), toArray(cutoffSet), false);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class GraphAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            Luc5Assay assay = NabDataHandler.getAssayResults(run, getUser());
            renderChartPNG(getViewContext().getResponse(), assay, assay.isLockAxes());
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

    private void renderChartPNG(HttpServletResponse response, DilutionSummary[] summaries, int[] cutoffs, boolean lockAxes) throws IOException, DilutionCurve.FitFailedException
    {
        List<Pair<String, DilutionSummary>> summaryMap = new ArrayList<Pair<String, DilutionSummary>>();
        for (DilutionSummary summary : summaries)
        {
            String sampleId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
            String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
            Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
            Date date = (Date) summary.getWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
            String key = getMaterialKey(sampleId, participantId, visitId, date);
            summaryMap.add(new Pair<String, DilutionSummary>(key, summary));
        }
        renderChartPNG(response, summaryMap, cutoffs, lockAxes);
    }

    private void renderChartPNG(HttpServletResponse response, Luc5Assay assay, boolean lockAxes) throws IOException, DilutionCurve.FitFailedException
    {
        renderChartPNG(response, assay.getSummaries(), assay.getCutoffs(), lockAxes);
    }

    private void renderChartPNG(HttpServletResponse response, List<Pair<String, DilutionSummary>> dilutionSummaries, int[] cutoffs, boolean lockAxes) throws IOException, DilutionCurve.FitFailedException
    {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, "Percent Neutralization", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
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

            XYSeries curvedSeries = new XYSeries(sampleId);
            DilutionCurve.DoublePoint[] curve = summary.getCurve();
            for (DilutionCurve.DoublePoint point : curve)
                curvedSeries.add(point.getX(), point.getY());
            curvesDataset.addSeries(curvedSeries);
            if (currentColor != null)
                plot.getRenderer(1).setSeriesPaint(curvesDataset.getSeriesCount() - 1, currentColor);
        }

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution/Concentration"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : cutoffs)
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));
        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 425, 300);
    }
}
