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
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QueryViewCustomizer;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.WellData;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.common.util.Pair;
import org.labkey.nab.query.NabRunDataTable;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

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
            NabUploadWizardAction.class
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
            return HttpView.redirect(new ActionURL("assay", "begin.view", getViewContext().getContainer()));
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
        private Set<String> _hiddenRunColumns;
        List<SampleResult> _sampleResults;

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
            _hiddenRunColumns.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
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
            ActionURL pageUrl = new ActionURL("NabAssay", "details", _run.getContainer());
            pageUrl.addParameter("rowId", "" + _run.getRowId());
            String discussionTitle = "Discuss Run " + _run.getRowId() + ": " + _run.getName();
            String entityId = _run.getLSID();
            DiscussionService.Service service = DiscussionService.get();
            return service.getDisussionArea(context,
                    entityId, pageUrl, discussionTitle, true, false);
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

        public List<SampleResult> getSampleResults()
        {
            if (_sampleResults == null)
            {
                _sampleResults = new ArrayList<SampleResult>();
                Map<String, Map<PropertyDescriptor, Object>> sampleProperties = getSampleProperties();
                for (DilutionSummary summary : _assay.getSummaries())
                {
                    String specimenId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
                    Double visitId = (Double) summary.getWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                    String participantId = (String) summary.getWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                    Date visitDate = (Date) summary.getWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
                    String key = getMaterialKey(specimenId, participantId, visitId, visitDate);
                    Map<PropertyDescriptor, Object> properties = sampleProperties.get(key);
                    _sampleResults.add(new SampleResult(summary, key, properties));
                }
            }
            return _sampleResults;
        }

        private Map<String, Map<PropertyDescriptor, Object>> getSampleProperties()
        {
            Map<String, Map<PropertyDescriptor, Object>> samplePropertyMap = new HashMap<String, Map<PropertyDescriptor, Object>>();

            Collection<ExpMaterial> inputs = _run.getMaterialInputs().keySet();
            PropertyDescriptor[] samplePropertyDescriptors = _provider.getSampleWellGroupColumns(_protocol);

            PropertyDescriptor sampleIdPD = null;
            PropertyDescriptor visitIdPD = null;
            PropertyDescriptor participantIdPD = null;
            PropertyDescriptor datePD = null;
            for (PropertyDescriptor property : samplePropertyDescriptors)
            {
                if (property.getName().equals(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME))
                    sampleIdPD = property;
                else if (property.getName().equals(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME))
                    participantIdPD = property;
                else if (property.getName().equals(AbstractAssayProvider.VISITID_PROPERTY_NAME))
                    visitIdPD = property;
                else if (property.getName().equals(AbstractAssayProvider.DATE_PROPERTY_NAME))
                    datePD = property;
            }

            for (ExpMaterial material : inputs)
            {
                Map<PropertyDescriptor, Object> sampleProperties = new TreeMap<PropertyDescriptor, Object>(new PropertyDescriptorComparator());
                for (PropertyDescriptor property : _provider.getSampleWellGroupColumns(_protocol))
                {
                    if (property != sampleIdPD && property != visitIdPD && property != participantIdPD && property != datePD)
                        sampleProperties.put(property, material.getProperty(property));
                }
                String key = getMaterialKey((String) material.getProperty(sampleIdPD),
                        (String) material.getProperty(participantIdPD), (Double) material.getProperty(visitIdPD), (Date) material.getProperty(datePD));
                samplePropertyMap.put(key, sampleProperties);
            }
            return samplePropertyMap;
        }

        public int getRunId()
        {
            return _run.getRowId();
        }

    }

    public static class SampleResult
    {
        private DilutionSummary _dilutionSummary;
        private String _materialKey;
        private Map<PropertyDescriptor, Object> _properties;

        public SampleResult(DilutionSummary dilutionSummary, String materialKey, Map<PropertyDescriptor, Object> properties)
        {
            _dilutionSummary = dilutionSummary;
            _materialKey = materialKey;
            _properties = properties;
        }

        public DilutionSummary getDilutionSummary()
        {
            return _dilutionSummary;
        }

        public String getKey()
        {
            return _materialKey;
        }

        public Map<PropertyDescriptor, Object> getProperties()
        {
            return _properties;
        }
    }

    protected static String getMaterialKey(String specimenId, String participantId, Double visitId, Date date)
    {
        if (specimenId != null)
            return specimenId;
        else if (visitId == null && date != null)
            return "Ptid " + participantId + ", Date " + date;
        else
            return "Ptid " + participantId + ", Vst " + visitId;
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
            super(protocol, provider, false);
            if (getViewContext().hasPermission(ACL.PERM_INSERT))
                _links.put("new run", provider.getUploadWizardURL(getContainer(), protocol));
            ActionURL downloadURL = new ActionURL("NabAssay", "downloadDatafile", getContainer()).addParameter("rowId", runId);
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
            if (run == null || !run.getContainer().equals(getContainer()))
                HttpView.throwNotFound("Run " + form.getRowId() + " does not exist.");
            Luc5Assay assay = NabDataHandler.getAssayResults(run);
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
            ActionURL assayListURL = AssayService.get().getAssayListURL(_run.getContainer());
            ActionURL runListURL = AssayService.get().getAssayRunsURL(_run.getContainer(), _protocol);
            ActionURL runDataURL = AssayService.get().getAssayDataURL(_run.getContainer(), _protocol, _run.getRowId());
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
                QueryView dataView = AssayService.get().createRunDataView(_context, _protocol);
                dataView.setQueryViewCustomizer(new NabAssayProvider.NabQueryViewCustomizer(NabRunDataTable.RUN_ID_COLUMN_NAME)
                {
                    public void customize(DataView view)
                    {
                        super.customize(view);
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
                    }
                });
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
            List<DilutionSummary> summaries = NabDataHandler.getDilutionSummaries(objectIds);
            for (DilutionSummary summary :summaries)
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/multiRunGraph.jsp",
                    new GraphSelectedBean(getViewContext(), _protocol, summaries.toArray(new DilutionSummary[summaries.size()]), toArray(cutoffSet), objectIds));

            return new VBox(new AssayHeaderView(_protocol, AssayService.get().getProvider(_protocol), false), multiGraphView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = AssayService.get().getAssayListURL(getContainer());
            ActionURL runListURL = AssayService.get().getAssayRunsURL(getContainer(), _protocol);
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
                ActionURL reuploadURL = new ActionURL("NabAssay", "nabUploadWizard", getContainer());
                reuploadURL.addParameter("dataFile", file.getPath());
                HttpView.throwRedirect(reuploadURL);
            }
            else
                HttpView.throwRedirect(AssayService.get().getAssayRunsURL(getContainer(), run.getProtocol()));
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
            List<DilutionSummary> summaries = NabDataHandler.getDilutionSummaries(ids);
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
            Luc5Assay assay = NabDataHandler.getAssayResults(run);
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

    private void renderChartPNG(HttpServletResponse response, DilutionSummary[] summaries, int[] cutoffs, boolean lockAxes) throws IOException
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

    private void renderChartPNG(HttpServletResponse response, Luc5Assay assay, boolean lockAxes) throws IOException
    {
        renderChartPNG(response, assay.getSummaries(), assay.getCutoffs(), lockAxes);
    }

    private void renderChartPNG(HttpServletResponse response, List<Pair<String, DilutionSummary>> dilutionSummaries, int[] cutoffs, boolean lockAxes) throws IOException
    {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, "Percentage", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
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

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : cutoffs)
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));
        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 425, 300);
    }
}
