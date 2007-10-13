package org.labkey.nab;

import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.*;
import org.labkey.api.study.WellData;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.*;
import org.labkey.api.data.*;
import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QueryViewCustomizer;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.common.util.Pair;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.ChartColor;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.List;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.IOException;
import java.io.File;
import java.awt.*;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class NabAssayController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(NabAssayController.class,
            new Action(NabUploadWizardAction.class)
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
            return HttpView.redirect(new ViewURLHelper("assay", "begin.view", getViewContext().getContainer()));
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

    public static class RenderAssayBean
    {
        private Luc5Assay _assay;
        private boolean _newRun;
        private boolean _printView;
        private ExpProtocol _protocol;
        private PlateBasedAssayProvider _provider;
        private ExpRun _run;
        private Map<PropertyDescriptor, Object> _runProperties;
        private List<Pair<String, Map<PropertyDescriptor, Object>>> _sampleProperties;
        private Set<String> _hiddenRunColumns;

        public RenderAssayBean(Luc5Assay assay, boolean newRun, boolean printView, ExpProtocol protocol, PlateBasedAssayProvider provider, ExpRun run)
        {
            _run = run;
            _provider = provider;
            _protocol = protocol;
            _assay = assay;
            _newRun = newRun;
            _printView = printView;
            _hiddenRunColumns = new HashSet<String>();
            _hiddenRunColumns.add(AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME);
            _hiddenRunColumns.add(AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME);
            for (String name : NabAssayProvider.CUTOFF_PROPERTIES)
                _hiddenRunColumns.add(name);
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
                SimpleFilter filter = new SimpleFilter("ProtocolLsid", _protocol.getLSID());
                filter.addCondition("Name", _assay.getDataFile().getName());
                filter.addCondition("RowId", _run.getRowId(), CompareType.NEQ);
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
                QueryView runsView = ExperimentService.get().createExperimentRunWebPart(context, new AssayRunFilter(_protocol, context.getContainer()), false);
                runsView.setShowExportButtons(false);
                runsView.setQueryViewCustomizer(new QueryViewCustomizer()
                {
                    public void customize(DataView view)
                    {
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
                    }
                });
                return runsView;
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
            ViewURLHelper pageUrl = new ViewURLHelper("NabAssay", "details", _run.getContainer());
            pageUrl.addParameter("rowId", "" + _run.getRowId());
            String discussionTitle = "Discuss Run " + _run.getRowId() + ": " + _run.getName();
            String entityId = _run.getLSID();
            DiscussionService.Service service = DiscussionService.get();
            return service.getDisussionArea(context, context.getContainer(), context.getUser(),
                    entityId, pageUrl, discussionTitle);
        }

        private class PropertyDescriptorComparator implements Comparator<PropertyDescriptor>
        {
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2)
            {
                String o1Str = o1.getLabel();
                if (o1Str == null)
                    o1Str = o1.getName();
                String o2Str = o2.getLabel();
                if (o2Str == null)
                    o2Str = o2.getName();
                return o1Str.compareToIgnoreCase(o2Str);
            }
        }

        public Map<PropertyDescriptor, Object> getRunProperties()
        {
            if (_runProperties == null)
            {
                _runProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
                for (PropertyDescriptor property : _provider.getUploadSetColumns(_protocol))
                {
                    if (!_hiddenRunColumns.contains(property.getName()))
                        _runProperties.put(property, _run.getProperty(property));
                }
                for (PropertyDescriptor property : _provider.getRunPropertyColumns(_protocol))
                {
                    if (!_hiddenRunColumns.contains(property.getName()))
                        _runProperties.put(property, _run.getProperty(property));
                }
                ColumnInfo runName = ExperimentService.get().getTinfoExperimentRun().getColumn("Name");
                if (runName != null)
                    _runProperties.put(new PropertyDescriptor(runName, _run.getContainer()), _run.getName());
            }
            return Collections.unmodifiableMap(_runProperties);
        }

        public List<Pair<String, Map<PropertyDescriptor, Object>>> getSampleProperties()
        {
            if (_sampleProperties == null)
            {
                List<ExpMaterial> inputs = _run.getMaterialInputs();
                _sampleProperties = new ArrayList<Pair<String, Map<PropertyDescriptor, Object>>>();
                PropertyDescriptor[] samplePropertyDescriptors = _provider.getSampleWellGroupColumns(_protocol);

                PropertyDescriptor sampleIdPD = null;
                PropertyDescriptor visitIdPD = null;
                PropertyDescriptor participantIdPD = null;
                for (PropertyDescriptor property : samplePropertyDescriptors)
                {
                    if (property.getName().equals(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME))
                        sampleIdPD = property;
                    else if (property.getName().equals(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME))
                        participantIdPD = property;
                    else if (property.getName().equals(AbstractAssayProvider.VISITID_PROPERTY_NAME))
                        visitIdPD = property;
                }

                for (ExpMaterial material : inputs)
                {
                    Map<PropertyDescriptor, Object> sampleProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
                    for (PropertyDescriptor property : _provider.getSampleWellGroupColumns(_protocol))
                    {
                        if (property != sampleIdPD && property != visitIdPD && property != participantIdPD)
                            sampleProperties.put(property, material.getProperty(property));
                    }
                    String key = getMaterialKey((String) material.getProperty(sampleIdPD),
                            (String) material.getProperty(participantIdPD), (Double) material.getProperty(visitIdPD));
                    _sampleProperties.add(new Pair<String, Map<PropertyDescriptor, Object>>(key, sampleProperties));
                }
            }
            return _sampleProperties;
        }

        public int getRunId()
        {
            return _run.getRowId();
        }
    }

    protected static String getMaterialKey(String specimenId, String participantId, Double visitId)
    {
        if (specimenId != null)
            return specimenId;
        return "Ptid " + participantId + ", Vst " + visitId;
    }

    public static class HeaderBean
    {
        private ViewURLHelper _printURL;
        private ViewURLHelper _datafileURL;
        private boolean _writer;

        public HeaderBean(ViewContext context, ViewURLHelper printLink, ViewURLHelper dataFileLink)
        {
            _printURL = printLink;
            _datafileURL = dataFileLink;
            _writer = context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT);
        }

        public boolean showPrintView()
        {
            return _printURL != null;
        }

        public ViewURLHelper getPrintURL()
        {
            return _printURL;
        }

        public ViewURLHelper getDatafileURL()
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
            super(protocol, provider);
            if (getViewContext().hasPermission(ACL.PERM_INSERT))
                _links.put("new run", provider.getUploadWizardURL(getContainer(), protocol).toString());
            ViewURLHelper downloadURL = new ViewURLHelper("NabAssay", "downloadDatafile", getContainer()).addParameter("rowId", runId);
            _links.put("download datafile", downloadURL.toString());
            _links.put("print", getViewContext().cloneViewURLHelper().addParameter("_print", "true").toString());
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class DetailsAction extends SimpleViewAction<RenderAssayForm>
    {
        private ExpRun _run;
        private ExpProtocol _protocol;

        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            Luc5Assay assay = getCachedAssay(form.getRowId());
            _run = ExperimentService.get().getExpRun(form.getRowId());
            _protocol = _run.getProtocol();
            PlateBasedAssayProvider provider = (PlateBasedAssayProvider) AssayService.get().getProvider(_protocol);

            HttpView view = new JspView<RenderAssayBean>("/org/labkey/nab/runDetails.jsp",
                    new RenderAssayBean(assay, form.isNewRun(), isPrint(), _protocol, provider, _run));
            if (!isPrint())
                view = new VBox(new NabDetailsHeaderView(_protocol, provider, _run.getRowId()), view);
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ViewURLHelper assayListURL = AssayService.get().getAssayListURL(_run.getContainer());
            ViewURLHelper runListURL = AssayService.get().getAssayRunsURL(_run.getContainer(), _protocol);
            ViewURLHelper runDataURL = AssayService.get().getAssayDataURL(_run.getContainer(), _protocol, _run.getRowId());
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _run.getRowId() + " Details");
        }
    }

    private Luc5Assay getCachedAssay(int rowId) throws Exception
    {
        Luc5Assay assay = _cachedAssay;
        if (assay == null || assay.getRunRowId() == null || assay.getRunRowId().intValue() != rowId)
        {
            ExpRun run = ExperimentService.get().getExpRun(rowId);
            assay = NabDataHandler.getAssayResults(run);
            _cachedAssay = assay;
        }
        return assay;
    }

    @RequiresPermission(ACL.PERM_READ)
    public class GraphAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            Luc5Assay assay = getCachedAssay(form.getRowId());

            List<Pair<String, DilutionSummary>> summaries = new ArrayList<Pair<String, DilutionSummary>>();
            for (int i = 0; i < assay.getSummaries().length; i++)
            {
                DilutionSummary summary = assay.getSummaries()[i];

                String sampleId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
                String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                String key = getMaterialKey(sampleId, participantId, visitId);
                summaries.add(new Pair<String, DilutionSummary>(key, summary));
            }
            renderChartPNG(getViewContext().getResponse(), summaries, assay.getCutoffs());
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    protected Luc5Assay _cachedAssay = null;
    protected DilutionSummary[] _cachedSummaries = null;
    private static final Color[] GRAPH_COLORS = {
            ChartColor.BLUE,
            ChartColor.RED,
            ChartColor.DARK_GREEN,
            ChartColor.DARK_YELLOW,
            ChartColor.MAGENTA
    };

    private void renderChartPNG(HttpServletResponse response, List<Pair<String, DilutionSummary>> dilutionSummaries, int[] cutoffs) throws IOException
        {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, "Percentage", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setDataset(1, pointDataset);
        plot.getRenderer(0).setStroke(new BasicStroke(1.5f));
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

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : cutoffs)
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));
        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 425, 300);
    }
}
