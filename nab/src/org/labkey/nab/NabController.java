package org.labkey.nab;

import jxl.read.biff.BiffException;
import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
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
import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.attachments.AttachmentForm;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayPublishService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.api.view.template.PrintTemplate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class NabController extends ViewController
{
    public static final String NAB_PIPELINE_DIR = "Luc5";

    static Logger _log = Logger.getLogger(NabController.class);

    /*
     * This method represents the point of entry into the pageflow
     */
    @Jpf.Action
    protected Forward begin(BeginForm form) throws Exception
    {
        if (!getUser().isGuest() && !getContainer().hasPermission(getUser(), ACL.PERM_INSERT))
            return new ViewForward(cloneActionURL().setAction("runs"));
        requiresPermission(ACL.PERM_INSERT);

        UploadAssayForm assayForm;
        if (form.isReset())
        {
            assayForm = new UploadAssayForm(true);
            NabManager.get().saveAsLastInputs(getViewContext(), null);
        }
        else
        {
            assayForm = NabManager.get().getLastInputs(getViewContext());
            if (form.getPlateTemplate() != null)
                assayForm.setPlateTemplate(form.getPlateTemplate());
        }

        return editRunParameters(assayForm);
    }

    public static class BeginForm extends FormData
    {
        private boolean _reset;
        private String _plateTemplate;

        public boolean isReset()
        {
            return _reset;
        }

        public void setReset(boolean reset)
        {
            _reset = reset;
        }

        public String getPlateTemplate()
        {
            return _plateTemplate;
        }

        public void setPlateTemplate(String plateTemplate)
        {
            _plateTemplate = plateTemplate;
        }
    }

    @Jpf.Action
    protected Forward editRunParameters(UploadAssayForm assayForm) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        JspView<UploadAssayForm> assayView = new JspView<UploadAssayForm>("/org/labkey/nab/runProperties.jsp", assayForm);
        return _renderInTemplate(assayView, "Create Nab Run");
    }


    @Jpf.Action
    protected Forward runs() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        PlateQueryView previousRuns = PlateService.get().getPlateGridView(getViewContext());

        List<ActionButton> buttons = new ArrayList<ActionButton>();

        ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
        selectButton.setDisplayPermission(ACL.PERM_INSERT);
        buttons.add(selectButton);

        ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
        clearButton.setDisplayPermission(ACL.PERM_INSERT);
        buttons.add(clearButton);

        ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete Selected", DataRegion.MODE_GRID, ActionButton.Action.POST);
        deleteButton.setDisplayPermission(ACL.PERM_DELETE);
        buttons.add(deleteButton);

        if (!AssayPublishService.get().getValidPublishTargets(getUser(), ACL.PERM_INSERT).isEmpty())
        {
            ActionButton publishButton = new ActionButton("publishPlatesChooseStudy.view", "Copy Selected to Study", DataRegion.MODE_GRID, ActionButton.Action.POST);
            publishButton.setDisplayPermission(ACL.PERM_INSERT);
            buttons.add(publishButton);
        }
        
        previousRuns.setButtons(buttons);
        return _renderInTemplate(previousRuns, "Nab Runs");
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "editRunParameters.do", name = "validate"))
    protected Forward upload(UploadAssayForm assayForm) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        for (int i = 0; i < assayForm.getSampleInfos().length; i++)
        {
            SampleInfo info = assayForm.getSampleInfos()[i];
            if (i > 0)
            {
                if (assayForm.getRunSettings().isSameMethod())
                    info.setMethodName(assayForm.getSampleInfos()[0].getMethodName());
                if (assayForm.getRunSettings().isSameFactor())
                    info.setFactor(assayForm.getSampleInfos()[0].getFactor());
                if (assayForm.getRunSettings().isSameInitialValue())
                    info.setInitialDilution(assayForm.getSampleInfos()[0].getInitialDilution());
            }
        }

        FormFile datafile = assayForm.getUploadFile();

        SafeTextConverter.PercentConverter[] possibleCutoffs = assayForm.getRunSettings().getCutoffs();
        Set<Integer> cutoffSet = new HashSet<Integer>();
        // eliminate duplicates and blank values:
        for (SafeTextConverter.PercentConverter possibleCutoff : possibleCutoffs)
        {
            if (possibleCutoff.getValue() != null)
                cutoffSet.add(possibleCutoff.getValue());
        }
        int[] cutoffs = new int[cutoffSet.size()];
        int idx = 0;
        for (Integer cutoff : cutoffSet)
            cutoffs[idx++] = cutoff;

        NabManager.get().saveAsLastInputs(getViewContext(), assayForm);
        Luc5Assay assay;
        try
        {
            assay = NabManager.get().saveResults(getContainer(), getUser(),
                    assayForm.getPlateTemplate(), assayForm.getMetadata(),
                    assayForm.getSampleInfos(), cutoffs, datafile);
        }
        catch (BiffException e)
        {
            // this is a hacky way to deal with a file format problem, but it lets us get away with
            // only reading the xls once, rather than the double-read that would be required to validate
            // the file format in our validate method.
            return new ViewForward(getActionURL().relativeUrl("begin", "error=" + PageFlowUtil.encode("Data file format error: " + e.getMessage())));
        }

        _cachedAssay = assay;
        ActionURL displayURL = cloneActionURL();
        displayURL.deleteParameters();
        displayURL.setAction("display");
        displayURL.addParameter("rowId", Integer.toString(assay.getRunRowId()));
        displayURL.addParameter("newRun", "true");
        return new ViewForward(displayURL);
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

    private Forward renderDetailPage(Luc5Assay assay, boolean newRun, boolean printView) throws Exception
    {
        JspView<RenderAssayBean> assayView = new JspView<RenderAssayBean>("/org/labkey/nab/runResults.jsp",
                new RenderAssayBean(assay, newRun, printView));
        if (printView)
            return _renderInTemplate(assayView, "NAB Run Details: " + assay.getName(), null, true);
        else
        {
            ActionURL printURL = cloneActionURL();
            printURL.addParameter("print", "true");
            printURL.deleteParameter("newRun");
            return _renderInTemplate(assayView, "NAB Run Details", printURL, false, assay.getPlate());
        }
    }

    public static class RenderAssayBean
    {
        private Luc5Assay _assay;
        private boolean _newRun;
        private boolean _printView;

        public RenderAssayBean(Luc5Assay assay, boolean newRun, boolean printView)
        {
            _assay = assay;
            _newRun = newRun;
            _printView = printView;
        }

        public Luc5Assay getAssay()
        {
            return _assay;
        }

        public boolean isNewRun()
        {
            return _newRun;
        }

        public PlateQueryView getDuplicateDataFileView(ViewContext context, Luc5Assay assay)
        {
            SimpleFilter filter = new SimpleFilter("Property/DataFile", assay.getPlate().getProperty("DataFile"));
            filter.addCondition("RowId", assay.getRunRowId(), CompareType.NEQ);
            PlateQueryView duplicateDataFileView = PlateService.get().getPlateGridView(context, filter);
            duplicateDataFileView.setShowExportButtons(false);
            ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
            selectButton.setDisplayPermission(ACL.PERM_INSERT);
            List<ActionButton> buttons = new ArrayList<ActionButton>();
            buttons.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(ACL.PERM_INSERT);
            buttons.add(clearButton);

            ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete Selected", DataRegion.MODE_GRID, ActionButton.Action.POST);
            deleteButton.setDisplayPermission(ACL.PERM_DELETE);
            buttons.add(deleteButton);
            duplicateDataFileView.setButtons(buttons);
            duplicateDataFileView.addHiddenFormField("runId", "" + assay.getRunRowId());
            duplicateDataFileView.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
            return duplicateDataFileView;
        }

        public boolean isPrintView()
        {
            return _printView;
        }

        public HttpView getDiscussionView(ViewContext context)
        {
            ActionURL pageUrl = new ActionURL("Nab", "display", _assay.getPlate().getContainer());
            pageUrl.addParameter("rowId", "" + _assay.getRunRowId());
            String discussionTitle = "Discuss Run " + _assay.getRunRowId() + ": " + _assay.getName();
            String entityId = _assay.getPlate().getEntityId();
            DiscussionService.Service service = DiscussionService.get();
            return service.getDisussionArea(context, context.getContainer(), context.getUser(),
                    entityId, pageUrl, discussionTitle, true);
        }
    }

    public static class GraphSelectedForm extends FormData
    {
        private int[] _id;
        private boolean _print;

        public boolean isPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }
    }

    public static class RenderAssayForm extends RowIdForm
    {
        private boolean _print;
        private boolean _newRun;

        public boolean isPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }

        public boolean isNewRun()
        {
            return _newRun;
        }

        public void setNewRun(boolean newRun)
        {
            _newRun = newRun;
        }
    }

    @Jpf.Action
    protected Forward display(RenderAssayForm assayForm) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        if (assayForm.getRowId() < 0)
            return new ViewForward(getActionURL().relativeUrl("begin", null));
        return renderDetailPage(getCachedAssay(assayForm.getRowId()), assayForm.isNewRun(), assayForm.isPrint());
    }

    private Luc5Assay getCachedAssay(int rowId) throws Exception
    {
        Luc5Assay assay = _cachedAssay;
        if (assay == null || assay.getRunRowId() != rowId)
        {
            assay = NabManager.get().loadFromDatabase(getUser(), getContainer(), rowId);
            _cachedAssay = assay;
        }
        return assay;
    }

    private DilutionSummary[] getDilutionSummaries(int[] wellGroupIds) throws SQLException, ServletException
    {
        List<DilutionSummary> summaries = new ArrayList<DilutionSummary>(wellGroupIds.length);
        Set<Integer> cutoffSet = new HashSet<Integer>();
        for (int wellgroupId : wellGroupIds)
        {
            try
            {
                DilutionSummary summary = NabManager.get().getDilutionSummary(getContainer(), wellgroupId);
                summaries.add(summary);
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            catch (NumberFormatException e)
            {
                _log.warn("Bad post to graphSelected", e);
            }
        }
        return summaries.toArray(new DilutionSummary[summaries.size()]);
    }

    private int[] getCutoffs(DilutionSummary[] summaries)
    {
        Set<Integer> cutoffSet = new HashSet<Integer>();
        for (DilutionSummary summary : summaries)
        {
            try
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            catch (NumberFormatException e)
            {
                _log.warn("Bad post to graphSelected", e);
            }
        }
        List<Integer> sortedUniqueCutoffs = new ArrayList<Integer>(cutoffSet);
        Collections.sort(sortedUniqueCutoffs);
        int[] cutoffs = new int[sortedUniqueCutoffs.size()];
        for (int i = 0; i < cutoffs.length; i++)
            cutoffs[i] = sortedUniqueCutoffs.get(i);
        return cutoffs;
    }

    @Jpf.Action
    protected Forward graphSelected(GraphSelectedForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        int[] wellGroupIds;
        if (form.getId() != null)
            wellGroupIds = form.getId();
        else
        {
            Set<String> wellgroupIdStrings = DataRegionSelection.getSelected(getViewContext(), false);
            if (wellgroupIdStrings == null || wellgroupIdStrings.size() == 0)
                return null;
            wellGroupIds = new int[wellgroupIdStrings.size()];
            int idx = 0;
            for (String wellgroupIdString : wellgroupIdStrings)
                wellGroupIds[idx++] = Integer.parseInt(wellgroupIdString);

        }
        DilutionSummary[] summaries = getDilutionSummaries(wellGroupIds);
        int[] cutoffList = getCutoffs(summaries);
        _cachedSummaries = summaries;
        JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/legacyGraph.jsp",
                new GraphSelectedBean(summaries, cutoffList));

        ActionURL printLink = null;
        if (!form.isPrint())
        {
            printLink = cloneActionURL();
            printLink.addParameter("print", "true");
            for (int id : wellGroupIds)
                printLink.addParameter("id", Integer.toString(id));
        }
        return _renderInTemplate(multiGraphView, "Graph Selected Specimens", printLink, form.isPrint());
    }

    public static String intString(double d)
    {
        return String.valueOf((int) Math.round(d));
    }

    public static String percentString(double d)
    {
        return intString(d * 100) + "%";
    }

    public static class GraphSelectedBean
    {
        private DilutionSummary[] _dilutionSummaries;
        private int[] _cutoffs;

        public GraphSelectedBean(DilutionSummary[] dilutions, int[] cutoffs)
        {
            _dilutionSummaries = dilutions;
            _cutoffs = cutoffs;
        }

        public DilutionSummary[] getDilutionSummaries()
        {
            return _dilutionSummaries;
        }

        public int[] getCutoffs()
        {
            return _cutoffs;
        }
    }


    private void renderChartPNG(DilutionSummary[] dilutionSummaries, int[] cutoffs) throws IOException
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
        for (DilutionSummary summary : dilutionSummaries)
        {
            XYSeries pointSeries = new XYSeries(summary.getSampleId());
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

            XYSeries curvedSeries = new XYSeries(summary.getSampleId());
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
        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(), chart, 425, 300);
    }

    @Jpf.Action
    protected Forward download(AttachmentForm form) throws IOException, ServletException, SQLException
    {
        requiresPermission(ACL.PERM_READ);

        Plate plate = PlateService.get().getPlate(getContainer(), form.getEntityId());

        AttachmentService.get().download(getResponse(), plate, form.getName());

        return null;
    }

    @Jpf.Action
    protected Forward renderChart(RowIdForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        Luc5Assay assay = getCachedAssay(form.getRowId());
        renderChartPNG(assay.getSummaries(), assay.getCutoffs());
        return null;
    }

    public static class RenderMultiChartFrom extends FormData
    {
        private int[] _wellGroupId;

        public int[] getWellGroupId()
        {
            return _wellGroupId;
        }

        public void setWellGroupId(int[] wellGroupId)
        {
            _wellGroupId = wellGroupId;
        }
    }

    @Jpf.Action
    protected Forward renderMultiRunChart(RenderMultiChartFrom form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        int[] ids = form.getWellGroupId();
        DilutionSummary[] summaries = _cachedSummaries;
        boolean cacheValid = true;
        if (summaries != null && summaries.length == ids.length)
        {
            for (int i = 0; i < summaries.length && cacheValid; i++)
            {
                if (ids[i] != summaries[i].getWellGroup().getRowId())
                    cacheValid = false;
            }
        }
        else
            cacheValid = false;

        if (!cacheValid)
            summaries = getDilutionSummaries(ids);

        int[] cutoffs = getCutoffs(summaries);
        renderChartPNG(summaries, cutoffs);
        return null;
    }

    public static class LSIDForm extends FormData
    {
        private String _lsid;

        public String getLsid()
        {
            return _lsid;
        }

        public void setLsid(String lsid)
        {
            _lsid = lsid;
        }
    }

    public static class RunIdForm extends FormData
    {
        private Integer _runId;

        public Integer getRunId()
        {
            return _runId;
        }

        public void setRunId(Integer runId)
        {
            _runId = runId;
        }
    }

    @Jpf.Action
    protected Forward deleteRuns(RunIdForm form) throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);
        Set<String> rowids = DataRegionSelection.getSelected(getViewContext(), true);
        if (rowids != null)
        {
            for (String rowidStr : rowids)
            try
            {
                int rowid = Integer.parseInt(rowidStr);
                NabManager.get().deletePlate(getContainer(), rowid);
            }
            catch (NumberFormatException e)
            {
                _log.warn("Bad post to delete runs.", e);
            }
        }
        if (form.getRunId() != null)
            return new ViewForward(getActionURL().relativeUrl("display", "rowId=" + form.getRunId()));
        else
            return new ViewForward(getActionURL().relativeUrl("runs", null));
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "publishVerify.do", name = "validate"))
    protected Forward handlePublish(PublishForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        Container targetContainer = ContainerManager.getForId(form.getTargetContainerId());
        if (!targetContainer.hasPermission(getUser(), ACL.PERM_INSERT))
            HttpView.throwUnauthorized();

        boolean dateBased = AssayPublishService.get().getTimepointType(targetContainer) == TimepointType.DATE;
        Set<String> includedSamples = new HashSet<String>();
        for (String sampleId : form.getIncludedSampleIds())
            includedSamples.add(sampleId);
        if (!includedSamples.isEmpty())
        {
            List<Plate> plates = new ArrayList<Plate>();
            List<Map<String, Object>> sampleProperties = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < form.getSampleIds().length; i++)
            {
                String sampleId = form.getSampleIds()[i];
                if (includedSamples.contains(sampleId))
                {
                    int wellGroupId = form.getId()[i];
                    WellGroup sample = PlateService.get().getWellGroup(getContainer(), wellGroupId);
                    if (sample != null)
                    {
                        plates.add(sample.getPlate());
                        String ptid = form.getParticipantIds()[i];
                        String visitIdString = null;
                        Double visitId = null;
                        String dateString = null;
                        Date date = null;
                        if (dateBased)
                        {
                            dateString = form.getDates()[i];
                            date = (Date) ConvertUtils.convert(dateString, Date.class);
                        }
                        else
                        {
                            visitIdString = form.getSequenceNums()[i];
                        // parse the double here: we've already verified that it's convertable in 'validate'.
                            visitId = Double.parseDouble(visitIdString);
                        }

                        Map<String, Object> samplePropertyMap = new HashMap<String, Object>();
                        for (String property : sample.getPropertyNames())
                            samplePropertyMap.put(property, sample.getProperty(property));

                        for (String property : sample.getPlate().getPropertyNames())
                            samplePropertyMap.put(property, sample.getPlate().getProperty(property));
                        samplePropertyMap.put("participantid", ptid);
                        if (dateBased)
                            samplePropertyMap.put("date", date);
                        else
                            samplePropertyMap.put("sequencenum", visitId);
                        samplePropertyMap.put("sourceLsid", sample.getLSID());
                        String virusName = (String) samplePropertyMap.get(NabManager.PlateProperty.VirusName.name());
                        String virusId = (String) samplePropertyMap.get(NabManager.PlateProperty.VirusId.name());
                        String virusKey = (virusId != null && virusId.length() > 0) ? virusId : "Unknown";
                        if (virusName != null && virusName.length() > 0)
                            virusKey +=  " (" + virusName + ")";
                        samplePropertyMap.put(NabManager.PlateProperty.VirusId.name(), virusKey);
                        sampleProperties.add(samplePropertyMap);
                    }
                }
            }
            if (!sampleProperties.isEmpty())
            {
                List<String> errors = new ArrayList<String>();
                ActionURL helper = AssayPublishService.get().publishAssayData(getUser(), getContainer(), targetContainer,
                        "NAB", null, sampleProperties.toArray(new Map[sampleProperties.size()]),
                        NabManager.get().getPropertyTypes(plates),
                        NabManager.PlateProperty.VirusId.name(), errors);

                if (errors != null && !errors.isEmpty())
                {
                    ActionErrors actionErrors = PageFlowUtil.getActionErrors(getRequest(), true);
                    for (String error : errors)
                        actionErrors.add("main", new ActionMessage("Error", error));
                    return publishVerify(form);
                }
                else
                    return new ViewForward(helper);
            }
        }
        ActionErrors actionErrors = PageFlowUtil.getActionErrors(getRequest(), true);
        actionErrors.add("main", new ActionMessage("Error", "At least one sample must be selected to copy to a study."));
        return publishVerify(form);
    }

    @Jpf.Action
    protected Forward publishVerify(PublishForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        Container targetContainer = ContainerManager.getForId(form.getTargetContainerId());
        if (!targetContainer.hasPermission(getUser(), ACL.PERM_INSERT))
            HttpView.throwUnauthorized();

        List<WellGroup> sampleList = new ArrayList<WellGroup>();
        if (form.isPlateIds())
        {
            for (Integer plateId : form.getId())
            {
                Plate plate = PlateService.get().getPlate(getContainer(), plateId);
                if (plate != null)
                {
                    for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        sampleList.add(sampleGroup);
                }
            }
        }
        else
        {
            for (Integer wellGroupId : form.getId())
            {
                WellGroup group = PlateService.get().getWellGroup(getContainer(), wellGroupId);
                if (group != null)
                {
                    assert group.getType() == WellGroup.Type.SPECIMEN : "Expected only specimen well groups";
                    sampleList.add(group);
                }
            }
        }

        List<WellGroup> sortedGroups = new ArrayList<WellGroup>();
        for (WellGroup wellgroup : sampleList)
            sortedGroups.add(wellgroup);
        Collections.sort(sortedGroups, new Comparator<WellGroup>()
        {
            public int compare(WellGroup group1, WellGroup group2)
            {
                String sampleId1 = (String) group1.getProperty(NabManager.SampleProperty.SampleId.name());
                String sampleId2 = (String) group2.getProperty(NabManager.SampleProperty.SampleId.name());
                return sampleId1.compareToIgnoreCase(sampleId2);
            }
        });

        Map<WellGroup, ParticipantVisit> sampleInfoMap = new LinkedHashMap<WellGroup, ParticipantVisit>();
        for (WellGroup wellgroup : sortedGroups)
        {
            String sampleId = (String) wellgroup.getProperty(NabManager.SampleProperty.SampleId.name());
            ParticipantVisit sampleInfo = form.getReshowData(sampleId);
            if (sampleInfo == null)
                sampleInfo = SpecimenService.get().getSampleInfo(targetContainer, sampleId);
            sampleInfoMap.put(wellgroup, sampleInfo);
        }

        JspView<PublishVerifyBean> publishVerifyView = new JspView<PublishVerifyBean>("/org/labkey/nab/publishVerify.jsp",
                new PublishVerifyBean(targetContainer, sampleInfoMap));
        return _renderInTemplate(publishVerifyView, "Associate Data");
    }

    private List<Integer> getCheckboxIds()
    {
        Set<String> idStrings = DataRegionSelection.getSelected(getViewContext(), false);
        List<Integer> ids = new ArrayList<Integer>();
        if (idStrings == null)
            return ids;
        for (String rowIdStr : idStrings)
        {
            try
            {
                ids.add(Integer.parseInt(rowIdStr));
            }
            catch (NumberFormatException e)
            {
                // fall through: we'll continue with the valid plate ids.
            }
        }
        return ids;
    }


    @Jpf.Action
    protected Forward publishWellGroupsChooseStudy() throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        List<Integer> wellgroupIds = getCheckboxIds();
        if (wellgroupIds.isEmpty())
        {
            ActionErrors actionErrors = PageFlowUtil.getActionErrors(getRequest(), true);
            actionErrors.add("main", new ActionMessage("Error", "You must select at least one specimen to copy to a study."));
            return sampleList();
        }
        else
        {
            JspView<PublishBean> chooseStudyView = new JspView<PublishBean>("/org/labkey/nab/publishChooseStudy.jsp",
                    new PublishBean(getViewContext(), wellgroupIds, false));
            return _renderInTemplate(chooseStudyView, "Choose Target Study");
        }
    }

    @Jpf.Action
    protected Forward publishPlatesChooseStudy() throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        List<Integer> plateIds = getCheckboxIds();
        if (plateIds.isEmpty())
        {
            ActionErrors actionErrors = PageFlowUtil.getActionErrors(getRequest(), true);
            actionErrors.add("main", new ActionMessage("Error", "You must select at least one row to copy to a study."));
            return runs();
        }
        else
        {
            JspView<PublishBean> chooseStudyView = new JspView<PublishBean>("/org/labkey/nab/publishChooseStudy.jsp",
                    new PublishBean(getViewContext(), plateIds, true));
            return _renderInTemplate(chooseStudyView, "Choose Target Study");
        }
    }

    public static class PublishSampleInfo implements ParticipantVisit
    {
        private String _ptid;
        private String _specimenID;
        private String _visitID;
        private String _dateString;

        public PublishSampleInfo(String ptid, String sampleId, String sequenceNum, String dateString)
        {
            _visitID = sequenceNum;
            _specimenID = sampleId;
            _ptid = ptid;
            _dateString = dateString;
        }

        public String getParticipantID()
        {
            return _ptid;
        }

        public String getSpecimenID()
        {
            return _specimenID;
        }

        public String getVisitIDString()
        {
            return _visitID;
        }

        public Double getVisitID()
        {
            try
            {
                if (_visitID == null)
                    return null;
                else
                    return Double.parseDouble(_visitID);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        public ExpMaterial getMaterial()
        {
            throw new UnsupportedOperationException("Not Implemented for PublishSampleInfo");
        }

        public Date getDate()
        {
            try
            {
                if (null == _dateString)
                    return null;
                return (Date) ConvertUtils.convert(_dateString, Date.class);
            }
            catch (ConversionException x)
            {
                return null;
            }
        }

        public String getDateString()
        {
            return _dateString;
        }
    }

    public static class PublishForm extends FormData
    {
        private String[] _includedSampleIds;
        private String[] _sequenceNums;
        private String[] _sampleIds;
        private String[] _participantIds;
        private String[] _dates;
        private int[] _id;
        private boolean _plateIds;
        private String _targetContainerId;

        public String[] getIncludedSampleIds()
        {
            return _includedSampleIds;
        }

        public void setIncludedSampleIds(String[] includedSampleIds)
        {
            _includedSampleIds = includedSampleIds;
        }

        public String[] getParticipantIds()
        {
            return _participantIds;
        }

        public void setParticipantIds(String[] participantIds)
        {
            _participantIds = participantIds;
        }

        public String[] getSampleIds()
        {
            return _sampleIds;
        }

        public void setSampleIds(String[] sampleIds)
        {
            _sampleIds = sampleIds;
        }

        public String[] getSequenceNums()
        {
            return _sequenceNums;
        }

        public void setSequenceNums(String[] sequenceNums)
        {
            _sequenceNums = sequenceNums;
        }


        public String[] getDates()
        {
            return _dates;
        }

        public void setDates(String[] dates)
        {
            _dates = dates;
        }

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }

        public boolean isPlateIds()
        {
            return _plateIds;
        }

        public void setPlateIds(boolean plateIds)
        {
            _plateIds = plateIds;
        }

        public String getTargetContainerId()
        {
            return _targetContainerId;
        }

        public void setTargetContainerId(String targetContainerId)
        {
            _targetContainerId = targetContainerId;
        }

        public ParticipantVisit getReshowData(final String sampleId)
        {
            if (_sampleIds == null)
                return null;

            for (int index = 0; index < _sampleIds.length; index++)
            {
                if (_sampleIds[index].equals(sampleId))
                    return new PublishSampleInfo(_participantIds[index], sampleId, _sequenceNums[index], _dates[index]);
            }
            return null;
        }

        @Override
        public ActionErrors validate(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            ActionErrors errors = new ActionErrors();
            Set<String> selectedSamples = new HashSet<String>();
            if (getIncludedSampleIds() == null)
                errors.add("main", new ActionMessage("Error", "You must select at least one specimen to copy to a study."));
            else
            {
                for (String sample : getIncludedSampleIds())
                    selectedSamples.add(sample);
                for (int i = 0; i < _sampleIds.length; i++)
                {
                    if (!selectedSamples.contains(_sampleIds[i]))
                        continue;
                    if (_participantIds[i] == null || _participantIds[i].length() == 0)
                        errors.add("main", new ActionMessage("Error", "Participant ID is required for sample " + _sampleIds[i]));
                    if (AssayPublishService.get().getTimepointType(ContainerManager.getForId(getTargetContainerId())) == TimepointType.VISIT)
                    {
                        if (_sequenceNums[i] == null)
                            errors.add("main", new ActionMessage("Error", "Visit Sequence number is required for sample " + _sampleIds[i]));
                        else
                        {
                            try
                            {
                                double d = Double.parseDouble(_sequenceNums[i]);
                            }
                            catch (NumberFormatException e)
                            {
                                errors.add("main", new ActionMessage("Error", "Visit Sequence numbers should be decimal values.  The following is invalid: " + _sequenceNums[i]));
                            }
                        }
                    }
                    else
                    {
                        if (_dates[i] == null)
                            errors.add("main", new ActionMessage("Error", "Date is required for sample " + _sampleIds[i]));
                        else
                        {
                            try
                            {
                                Date d = (Date) ConvertUtils.convert(_dates[i], Date.class);
                            }
                            catch (ConversionException e)
                            {
                                errors.add("main", new ActionMessage("Error", "The following is not a valid date: " + _dates[i]));
                            }
                        }

                    }
                }
            }
            return errors;
        }
    }

    public static class PublishVerifyBean
    {
        private List<String> _sampleProperties;
        private Map<WellGroup, ParticipantVisit> _sampleInfoMap;
        private Container _targetContainer;
        private DecimalFormat _decimalFormat = new DecimalFormat("0.##");
        private List<String> _uploadErrors;
        
        public PublishVerifyBean(Container targetContainer, Map<WellGroup, ParticipantVisit> sampleInfoMap)
        {
            _sampleInfoMap = sampleInfoMap;
            _targetContainer = targetContainer;
            Set<String> propertySet = new HashSet<String>();
            for (WellGroup group : sampleInfoMap.keySet())
                propertySet.addAll(group.getPropertyNames());
            propertySet.remove(NabManager.SampleProperty.SampleId.name());
            _sampleProperties = new ArrayList<String>(propertySet);
            Collections.sort(_sampleProperties);
        }

        public Map<WellGroup, ParticipantVisit> getSampleInfoMap()
        {
            return _sampleInfoMap;
        }

        public Container getTargetContainer()
        {
            return _targetContainer;
        }

        public List<String> getSampleProperties()
        {
            return _sampleProperties;
        }

        public String getSampleIdCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.SpecimenGlobalUniqueId);
        }

        public String getVisitIdCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.VisitId);
        }

        public String getParticipantCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.ParticpantId);
        }

        public List<String> getUploadErrors()
        {
            return _uploadErrors;
        }

        public void setUploadErrors(List<String> uploadErrors)
        {
            _uploadErrors = uploadErrors;
        }

        public String format(Object obj)
        {
            if (obj == null)
                return null;
            if (obj instanceof Double|| obj instanceof Float)
                return _decimalFormat.format(obj);
            if (obj instanceof Date)
                return DateUtil.formatDate((Date) obj);
            return obj.toString();
        }
    }

    public static class PublishBean
    {
        private User _user;
        private List<Integer> _ids;
        private boolean _plateIds;

        public PublishBean(ViewContext context, List<Integer> ids, boolean plateIds)
        {
            _user = context.getUser();
            _ids = ids;
            _plateIds = plateIds;
        }

        public Map<Container, String> getValidTargets()
        {
            return AssayPublishService.get().getValidPublishTargets(_user, ACL.PERM_INSERT);
        }

        public List<Integer> getIds()
        {
            return _ids;
        }

        public boolean isPlateIds()
        {
            return _plateIds;
        }
    }

    @Jpf.Action
    protected Forward deleteRun(RowIdForm form) throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);
        NabManager.get().deletePlate(getContainer(), form.getRowId());
        return new ViewForward(getActionURL().relativeUrl("begin", null));
    }

    @Jpf.Action
    protected Forward sampleList() throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        PlateQueryView queryView = PlateService.get().getWellGroupGridView(getViewContext(), WellGroup.Type.SPECIMEN);

        List<ActionButton> buttons = new ArrayList<ActionButton>();
        ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
        selectButton.setDisplayPermission(ACL.PERM_INSERT);
        buttons.add(selectButton);

        ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
        clearButton.setDisplayPermission(ACL.PERM_INSERT);
        buttons.add(clearButton);

        ActionButton graphSelectedButton = new ActionButton("button", "Graph Selected");
        graphSelectedButton.setScript("return verifySelected(this.form, \"graphSelected.view\", \"post\", \"rows\")");
        graphSelectedButton.setActionType(ActionButton.Action.GET);
        buttons.add(graphSelectedButton);

        if (!AssayPublishService.get().getValidPublishTargets(getUser(), ACL.PERM_INSERT).isEmpty())
        {
            ActionButton publishButton = new ActionButton("publishWellGroupsChooseStudy.view", "Copy Selected to Study", DataRegion.MODE_GRID, ActionButton.Action.POST);
            publishButton.setDisplayPermission(ACL.PERM_INSERT);
            buttons.add(publishButton);
        }
        
        queryView.setButtons(buttons);

        return _renderInTemplate(queryView, "NAB Runs By Sample");
    }

    private Forward _renderInTemplate(HttpView view, String title) throws Exception
    {
        return _renderInTemplate(view, title, null, false, null);
    }

    private Forward _renderInTemplate(HttpView view, String title, ActionURL printLink, boolean isPrintView) throws Exception
    {
        return _renderInTemplate(view, title, printLink, isPrintView, null);
    }

    private Forward _renderInTemplate(HttpView view, String title, ActionURL printLink, boolean isPrintView, Plate dataFilePlate) throws Exception
    {
        HttpView template;
        if (isPrintView)
            template = new PrintTemplate(view, title);
        else
        {
            ActionURL customizeLink = null;
            if (view instanceof PlateQueryView)
                customizeLink = ((PlateQueryView) view).getCustomizeURL();
            JspView<HeaderBean> headerView = new JspView<HeaderBean>("/org/labkey/nab/header.jsp",
                    new HeaderBean(getViewContext(), printLink,
                            dataFilePlate != null ? NabManager.get().getDataFileDownloadLink(dataFilePlate) : null, customizeLink));
            VBox box = new VBox(headerView, view);
            NavTrailConfig config = new NavTrailConfig(getViewContext());
            config.setTitle(title);
            template = new HomeTemplate(getViewContext(), getContainer(), box, config);
        }
        includeView(template);
        return null;
    }

    public static class HeaderBean
    {
        private ActionURL _printURL;
        private ActionURL _datafileURL;
        private ActionURL _customizeURL;
        private boolean _writer;

        public HeaderBean(ViewContext context, ActionURL printLink, ActionURL dataFileLink, ActionURL customizeLink)
        {
            _printURL = printLink;
            _datafileURL = dataFileLink;
            _customizeURL = customizeLink;
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

        public ActionURL getCustomizeURL()
        {
            return _customizeURL;
        }
    }

    public static class UploadAssayForm extends ViewForm
    {
        private SampleInfo[] _sampleInfos;
        private String _fileName;
        private RunMetadata _metadata = new RunMetadata();
        private RunSettings _runSettings;
        private String _plateTemplate;

        public UploadAssayForm()
        {
            this(false);
        }

        public UploadAssayForm(boolean returnDefaultForUnsetBools)
        {
            _runSettings = new RunSettings(returnDefaultForUnsetBools);
            reset(null, getContext().getRequest());
        }


        @Override
        public ActionErrors validate(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            ActionErrors errors = new ActionErrors();
            FormFile dataFile = getUploadFile();
            if (null == _fileName && null == dataFile)
                errors.add("main", new ActionMessage("Error", "Please upload a file."));
            
            List<String> templateErrors = NabManager.get().isValidNabPlateTemplate(getContext().getContainer(),
                    getContext().getUser(), getPlateTemplate());
            for (String templateError : templateErrors)
                errors.add("main", new ActionMessage("Error", templateError));

            if (getMetadata().getExperimentDateString() != null)
            {
                try
                {
                    long dateTime = DateUtil.parseDateTime(getMetadata().getExperimentDateString());
                    getMetadata().setExperimentDate(new Date(dateTime));
                }
                catch (ConversionException e)
                {
                    errors.add("main", new ActionMessage("Error", "Could not parse experiment date: " +
                            getMetadata().getExperimentDateString() + ". Please re-enter in a standard date format."));
                }
            }

            SafeTextConverter.PercentConverter[] cutoffs = getRunSettings().getCutoffs();
            for (SafeTextConverter.PercentConverter cutoff : cutoffs)
            {
                if (cutoff.getValue() == null && cutoff.getText() != null)
                    errors.add("main", new ActionMessage("Error", cutoff.getText() + " is not a valid cutoff value."));
                if (cutoff.getValue() != null && (cutoff.getValue() < -100 || cutoff.getValue() > 200))
                    errors.add("main", new ActionMessage("Error", "Cutoff percentages must be between -100 and 200 percent.  " + cutoff.getText() + " is not a valid cutoff value."));
            }

            if (dataFile != null && getRunSettings().isInferFromFile())
            {
                String filename = dataFile.getFileName();

                if (filename.toLowerCase().endsWith(".xls"))
                    getMetadata().setFileId(filename.substring(0, filename.length() - 4));
                else
                    getMetadata().setFileId(filename);
                int pos;
                // trim down to the numeric portion of the filename, eliminating leading characters
                // and anything following a dot or semicolon:
                if ((pos = filename.indexOf(';')) >= 0)
                    filename = filename.substring(0, pos);
                if ((pos = filename.indexOf('.')) >= 0)
                    filename = filename.substring(0, pos);
                pos = 0;
                while (pos < filename.length() && !Character.isDigit(filename.charAt(pos)))
                    pos++;
                if (pos < filename.length())
                {
                    filename = filename.substring(pos);
                    int len = filename.length();
                    // we expect an eight digit date: four digits for the year, two each for day/month:
                    if (len == 8)
                    {
                        String dateString = filename.substring(0, 4) + "-" + filename.substring(4, 6) +
                                "-" + filename.substring(6, 8);
                        try
                        {
                            Date date = DateUtil.parseDateTime(dateString, "yyyy-MM-dd");
                            getMetadata().setExperimentDate(date);
                        }
                        catch (ParseException e)
                        {
                            errors.add("main", new ActionMessage("Error", "Could not parse experiment date from filename."));
                        }
                    }
                    else if (len == 6)
                    {
                        String dateString = filename.substring(0, 2) + "-" + filename.substring(2, 4) +
                                "-" + filename.substring(4, 6);
                        try
                        {
                            Date date = DateUtil.parseDateTime(dateString, "yy-MM-dd");
                            getMetadata().setExperimentDate(date);
                        }
                        catch (ParseException e)
                        {
                            errors.add("main", new ActionMessage("Error", "Could not parse experiment date from filename."));
                        }
                    }
                    else
                        errors.add("main", new ActionMessage("Error", "Filename was an unxpected length; cannot infer experiment properties."));
                }
                else
                    errors.add("main", new ActionMessage("Error", "Filename was in an unexpected format; cannot infer experiment properties."));
            }

            boolean missingSampleId = false;
            for (SampleInfo info : _sampleInfos)
            {
                if (info.getSampleId() == null || info.getSampleId().length() == 0)
                    missingSampleId = true;
                else
                {
                    if (info.getInitialDilution() == null || info.getInitialDilution() < 0)
                        errors.add("main", new ActionMessage("Error", info.getSampleId() + " initial dilution/concentration is invalid."));
                    if (info.getFactor() == null)
                        errors.add("main", new ActionMessage("Error", info.getSampleId() + " dilution/concentration factor is invalid."));
                }
            }
            if (missingSampleId)
                errors.add("main", new ActionMessage("Error", "All samples must be given an ID."));

            return errors;
        }

        @Override
        public void reset(ActionMapping actionMapping, HttpServletRequest request)
        {
            _plateTemplate = request.getParameter("plateTemplate");
            resetSpecimens();
            resetDefaultProperties();
        }

        private void resetSpecimens()
        {
            PlateTemplate template = getActivePlateTemplate(getContext().getContainer(), getContext().getUser());
            int specimenCount = template.getWellGroupCount(WellGroup.Type.SPECIMEN);
            _sampleInfos = new SampleInfo[specimenCount];
            for (int i = 0; i < _sampleInfos.length; i++)
                _sampleInfos[i] = new SampleInfo(null);
        }

        public PlateTemplate getActivePlateTemplate(Container container, User user)
        {
            try
            {
                PlateTemplate template = null;
                if (_plateTemplate != null)
                    template = PlateService.get().getPlateTemplate(container, _plateTemplate);

                if (template == null)
                {
                    template = NabManager.get().ensurePlateTemplate(container, user);
                    _plateTemplate = template.getName();
                }
                return template;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        
        public PlateTemplate[] getPlateTemplates(Container container, User user)
        {
            try
            {
                PlateTemplate[] templates = PlateService.get().getPlateTemplates(container);
                if (templates == null || templates.length == 0)
                {
                    PlateTemplate defaultTemplate = NabManager.get().ensurePlateTemplate(container, user);
                    templates = new PlateTemplate[] { defaultTemplate };
                }
                return templates;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getPlateTemplate()
        {
            return _plateTemplate;
        }

        public void setPlateTemplate(String plateTemplate)
        {
            String previous = _plateTemplate;
            _plateTemplate = plateTemplate;
            if (!plateTemplate.equals(previous))
            {
                resetSpecimens();
                resetDefaultProperties();
            }
        }

        private void resetDefaultProperties()
        {
            PlateTemplate template = getActivePlateTemplate(getContext().getContainer(), getContext().getUser());
            Object experimentDate = template.getProperty("ExperimentDate");
            if (experimentDate instanceof Date)
                getMetadata().setExperimentDate((Date) experimentDate);
            else
                getMetadata().setExperimentDateString(experimentDate != null ? experimentDate.toString() : null);
            getMetadata().setExperimentId((String)template.getProperty("ExperimentId"));
            getMetadata().setExperimentPerformer((String)template.getProperty("ExperimentPerformer"));
            getMetadata().setFileId((String)template.getProperty("FileId"));
            getMetadata().setHostCell((String)template.getProperty("HostCell"));
            getMetadata().setIncubationTime((String)template.getProperty("IncubationTime"));
            getMetadata().setPlateNumber((String)template.getProperty("PlateNumber"));
            getMetadata().setStudyName((String)template.getProperty("StudyName"));
            getMetadata().setVirusId((String)template.getProperty("VirusId"));
            getMetadata().setVirusName((String)template.getProperty("VirusName"));
        }

        public SampleInfo[] getSampleInfos()
        {
            return _sampleInfos;
        }

        public void setSampleInfos(SampleInfo[] sampleInfos)
        {
            this._sampleInfos = sampleInfos;
        }

        public FormFile getUploadFile()
        {
            MultipartRequestHandler handler = getMultipartRequestHandler();
            if (null == handler)
                return null;

            Map<String, FormFile> formFiles = handler.getFileElements();
            if (null == formFiles)
                return null;

            FormFile formFile = formFiles.get("dataFile");
            if (null == formFile)
                return null;

            if (0 == formFile.getFileSize())
                return null;

            return formFile;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            this._fileName = fileName;
        }

        public RunMetadata getMetadata()
        {
            return _metadata;
        }

        public void setMetadata(RunMetadata metadata)
        {
            _metadata = metadata;
        }

        public RunSettings getRunSettings()
        {
            return _runSettings;
        }

        public void setRunSettings(RunSettings runSettings)
        {
            _runSettings = runSettings;
        }
    }

    public static class RowIdForm extends FormData
    {
        private int _rowId = -1;

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }
    }
}