package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.ms1.maintenance.PurgeTask;
import org.labkey.ms1.model.DataFile;
import org.labkey.ms1.model.Feature;
import org.labkey.ms1.model.Peptide;
import org.labkey.ms1.model.Software;
import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.view.*;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * This controller is the entry point for all web pages specific to the MS1
 * module. Each action is represented by a nested class named as such:
 * [action]Action
 * @author DaveS
 */
public class MS1Controller extends SpringActionController
{
    static Logger _log = Logger.getLogger(MS1Controller.class);
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(MS1Controller.class);

    public MS1Controller() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    /**
     * Returns a url helper for the specified action within this controller and current contianer
     *
     * @param action Name of action
     * @return URL helper for the action in this controller and current container
     */
    protected ViewURLHelper getViewURLHelper(String action)
    {
        return new ViewURLHelper(MS1Module.CONTROLLER_NAME, action, getContainer());
    }

    /**
     * Adds the begin step to a NavTree
     *
     * @param root The root of the NavTree
     * @return Modified NavTree
     */
    protected NavTree addBeginChild(NavTree root)
    {
        return root.addChild("MS1 Runs", getViewURLHelper("begin.view"));
    }

    /**
     * Adds the Features view step to a NavTree
     *
     * @param root The root of the NavTree
     * @param runId The runId parameter
     * @return Modified NavTree
     */
    protected NavTree addFeaturesChild(NavTree root, int runId)
    {
        ViewURLHelper url = getViewURLHelper("showFeatures.view");
        url.addParameter(ShowFeaturesAction.PARAM_RUNID, runId);
        url.addParameter(".lastFilter", "true");
        return root.addChild("Features from Run", url);
    }

    /**
     * Adds a the peaks view step to the NavTree
     *
     * @param root  The root of the NavTree
     * @param runId The runId parameter
     * @param featureId  The featureId parameter
     * @return Modified NavTree
     */
    protected NavTree addPeaksChild(NavTree root, int runId, int featureId)
    {
        ViewURLHelper url = getViewURLHelper("showPeaks.view");
        url.addParameter(ShowFeaturesAction.PARAM_RUNID, runId);
        url.addParameter(ShowPeaksAction.PARAM_FEATUREID, featureId);
        url.addParameter(".lastFilter", "true"); 
        return root.addChild("Peaks from Feature", url);
    }

    /**
     * Exports a QueryView (or derived class) to Excel, TSV, or Print, depending
     * on the value of the format parameter.
     *
     * @param view The view to export
     * @param format The export format (may be "excel", "tsv", or "print")
     * @return A null ModelAndView suitable for returning from the Action's getView() method
     * @throws Exception Thrown from QueryView's export methods
     */
    protected ModelAndView exportQueryView(QueryView view, String format) throws Exception
    {
        if(format.equalsIgnoreCase("excel"))
            view.exportToExcel(getViewContext().getResponse());
        else if(format.equalsIgnoreCase("tsv"))
            view.exportToTsv(getViewContext().getResponse());
        else if(format.equalsIgnoreCase("print"))
        {
            view.setPrintView(true);
            PrintTemplate template = new PrintTemplate(view, view.getTitle());
            template.getModelBean().setShowPrintDialog(true);
            HttpView.include(template, getViewContext().getRequest(), getViewContext().getResponse());
        }

        return null;
    }

    /**
     * Returns true if the parameter exportParam is set to either "excel", "tsv", or "print" (ignoring case)
     *
     * @param exportParam The export parameter's value
     * @return True if set to an export command
     */
    protected boolean isExportRequest(String exportParam)
    {
        return (null != exportParam && exportParam.length() > 0 &&
                (exportParam.equalsIgnoreCase("excel") 
                || exportParam.equalsIgnoreCase("tsv")
                || exportParam.equalsIgnoreCase("print")
                ));
    }

    /**
     * Begin action for the MS1 Module. Displays a list of msInspect feature finding runs
     */
    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return ExperimentService.get().createExperimentRunWebPart(getViewContext(),MS1Module.EXP_RUN_FILTER, true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("MS1 Runs", getUrl());
        }

        public ViewURLHelper getUrl()
        {
            return new ViewURLHelper(MS1Module.CONTROLLER_NAME, "begin.view", getContainer());
        }
    } //class BeginAction

    /**
     * Action to show the features for a given experiment run
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeaturesAction extends SimpleViewAction<RunIdForm>
    {
        public static final String PARAM_RUNID = "runId";
        private RunIdForm _form;

        public ModelAndView getView(RunIdForm form, BindException errors) throws Exception
        {
            _form = null;
            if(-1 == form.getRunId())
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            //ensure that the experiment run is valid and exists within the current container
            ExpRun run = ExperimentService.get().getExpRun(form.getRunId());
            if(null == run || !(run.getContainer().equals(getViewContext().getContainer())))
                throw new NotFoundException("Experiment run " + form.getRunId() + " does not exist in " + getViewContext().getContainer().getPath());

            MS1Manager mgr = MS1Manager.get();

            //determine if there is peak data available for these features
            MS1Manager.PeakAvailability peakAvail = mgr.isPeakDataAvailable(form.getRunId());

            //create the features view
            FeaturesView featuresView = new FeaturesView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                            form.getRunId(),
                                                            (peakAvail == MS1Manager.PeakAvailability.Available),
                                                            isExportRequest(form.getExport()));

            //if there is an export request, export and return
            if(isExportRequest(form.getExport()))
                return exportQueryView(featuresView, form.getExport());

            //get the corresponding file Id and initialize a software view if there is software info
            //also create a file details view
            JspView<Software[]> softwareView = null;
            JspView<DataFile> fileDetailsView = null;
            Integer fileId = mgr.getFileIdForRun(form.getRunId(), MS1Manager.FILETYPE_FEATURES);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId.intValue());
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<Software[]>("/org/labkey/ms1/view/softwareView.jsp", swares);
                    softwareView.setTitle("Processing Software Information");
                }

                DataFile dataFile = mgr.getDataFile(fileId.intValue());
                if(null != dataFile)
                {
                    fileDetailsView = new JspView<DataFile>("/org/labkey/ms1/view/FileDetailsView.jsp", dataFile);
                    fileDetailsView.setTitle("Data File Information");
                }
            }

            //save the form so that we have access to it in the appendNavTrail method
            _form = form;

            //build up the views and return
            VBox views = new VBox();

            if(peakAvail == MS1Manager.PeakAvailability.PartiallyAvailable)
                views.addView(new JspView("/org/labkey/ms1/view/PeakWarnView.jsp"));
            if(null != fileDetailsView)
                views.addView(fileDetailsView);
            if(null != softwareView)
                views.addView(softwareView);

            views.addView(featuresView);

            return views;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendNavTrail(root, _form.getRunId());
        }

        public NavTree appendNavTrail(NavTree root, int runId)
        {
            return new BeginAction().appendNavTrail(root).addChild("Features", getUrl(runId));
        }

        public ViewURLHelper getUrl(int runId)
        {
            ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showFeatures.view", getContainer());
            url.addParameter(ShowFeaturesAction.PARAM_RUNID, runId);
            url.addParameter(".lastFilter", "true");
            return url;
        }
    } //class ShowFeaturesAction

    /**
     * Action to show the peaks for a given experiment run and scan number
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeaksAction extends SimpleViewAction<PeaksViewForm>
    {
        public static final String PARAM_FEATUREID = "featureId";
        private PeaksViewForm _form;

        public ModelAndView getView(PeaksViewForm form, BindException errors) throws Exception
        {
            if(-1 == form.getRunId() && -1 == form.getFeatureId())
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            MS1Manager mgr = MS1Manager.get();

            //get the feature and ensure that it is valid and that it's ExpRun is valid
            //and that it exists within the current container
            Feature feature = mgr.getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getViewContext().getContainer().getPath());

            ExpRun expRun = feature.getExpRun();
            if(null == expRun || !(expRun.getContainer().equals(getViewContext().getContainer()))
                    || expRun.getRowId() != form.getRunId())
                throw new NotFoundException("The experiment run " + form.getRunId() + " was not found in " + getViewContext().getContainer().getPath());

            //ensure that we have a scanFirst and scanLast value for this feature
            //if we don't, we can't filter the peaks to a reasonable subset
            if(null == feature.getScanFirst() || null == feature.getScanLast())
                return new HtmlView("The peaks for this feature cannot be displayed because the first and last scan number for the feature were not supplied.");

            //initialize the PeaksView
            //the form's scanFirst/Last can override the feature's scanFirst/Last
            PeaksView peaksView = new PeaksView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                expRun, feature,
                                                null == form.getScanFirst() ? feature.getScanFirst().intValue() : form.getScanFirst().intValue(),
                                                null == form.getScanLast() ? feature.getScanLast().intValue() : form.getScanLast().intValue());

            //if there is an export parameter, do the export and return
            if(isExportRequest(form.getExport()))
                return exportQueryView(peaksView, form.getExport());

            //if software information is available, create and initialize the software view
            //also the data file information view
            JspView<Software[]> softwareView = null;
            JspView<DataFile> fileDetailsView = null;
            Integer fileId = mgr.getFileIdForRun(expRun.getRowId(), MS1Manager.FILETYPE_PEAKS);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId.intValue());
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<Software[]>("/org/labkey/ms1/view/softwareView.jsp", swares);
                    softwareView.setTitle("Software Information");
                }

                DataFile dataFile = mgr.getDataFile(fileId.intValue());
                if(null != dataFile)
                {
                    fileDetailsView = new JspView<DataFile>("/org/labkey/ms1/view/FileDetailsView.jsp", dataFile);
                    fileDetailsView.setTitle("Data File Information");
                }
            }

            //save the form so we have access to the params when we build the nav trail
            _form = form;

            //build up the views and return
            VBox views = new VBox();
            if(null != fileDetailsView)
                views.addView(fileDetailsView);
            if(null != softwareView)
                views.addView(softwareView);

            views.addView(peaksView);
            return views;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendNavTrail(root, _form.getRunId(), _form.getFeatureId());
        }

        public NavTree appendNavTrail(NavTree root, int runId, int featureId)
        {
            return new ShowFeaturesAction().appendNavTrail(new BeginAction().appendNavTrail(root), runId).addChild("Peaks",
                    getUrl(runId, featureId));
        }

        public ViewURLHelper getUrl(int runId, int featureId)
        {
            ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showPeaks.view", getContainer());
            url.addParameter(ShowFeaturesAction.PARAM_RUNID, runId);
            url.addParameter(ShowPeaksAction.PARAM_FEATUREID, featureId);
            url.addParameter(".lastFilter", "true"); 
            return url;
        }
    } //class ShowFeaturesAction

    /**
     * Action to show the related MS2 peptide(s) for the specified feature
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowMS2PeptideAction extends SimpleViewAction<MS2PeptideForm>
    {
        public ModelAndView getView(MS2PeptideForm form, BindException errors) throws Exception
        {
            if(null == form || form.getFeatureId() < 0)
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                return new HtmlView("Invalid Feature Id: " + form.getFeatureId());

            Peptide[] peptides = feature.getMatchingPeptides();
            if(null == peptides || 0 == peptides.length)
                return new HtmlView("The corresponding MS2 peptide information was not found in the database. Ensure that it has been imported before attempting to view the MS2 peptide.");

            Peptide pepFirst = peptides[0];
            ViewURLHelper url = new ViewURLHelper("MS2", "showPeptide", getContainer());
            url.addParameter("run", String.valueOf(pepFirst.getRun()));
            url.addParameter("peptideId", String.valueOf(pepFirst.getRowId()));
            url.addParameter("rowIndex", 1);

            //add a filter for MS2 scan so that the showPeptide view will know to enable or
            //disable it's <<prev and next>> buttons based on how many peptides were actually
            //matched.
            return HttpView.redirect(url + "&MS2Peptides.Scan~eq=" + pepFirst.getScan());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            //if this gets called, then we couldn't find the peptide and
            //displayed the message returned in the HtmlView above.
            return root.addChild("Associated Peptide Not Found");
        }
    }

    /**
     * Action to show the feature details view (with all the charts)
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeatureDetailsAction extends SimpleViewAction<FeatureIdForm>
    {

        public ModelAndView getView(FeatureIdForm form, BindException errors) throws Exception
        {
            if(null == form || form.getFeatureId() < 0 || form.getRunId() < 0)
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getViewContext().getContainer().getPath());

            //ensure that the run for this feature exists within the current container
            //and that the runid parameter matches
            ExpRun run = feature.getExpRun();
            if(null == run || run.getRowId() != form.getRunId() || !(run.getContainer().equals(getViewContext().getContainer())))
                    throw new NotFoundException("Experiment run " + form.getRunId() + " does not exist within " + getViewContext().getContainer().getPath());

            //create and initialize a new features view so that we can know which features
            //are immediately before and after the current one
            //this gives the illusion to the user that they are stepping through the same list of
            //features they were viewing on the previous screen (the showFeatures action)
            FeaturesView featuresView = new FeaturesView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                            form.getRunId(), false, true);

            //get the previous and next feature ids (ids will be -1 if there isn't a prev or next)
            int[] prevNextFeatureIds = featuresView.getPrevNextFeature(form.getFeatureId());
            FeatureDetailsViewContext ctx = new FeatureDetailsViewContext(feature, prevNextFeatureIds[0], prevNextFeatureIds[1]);

            //save the form for the nav trail
            _form = form;

            return new JspView<FeatureDetailsViewContext>("/org/labkey/ms1/view/FeatureDetailView.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendNavTrail(root, _form.getRunId(), _form.getFeatureId());
        }

        public NavTree appendNavTrail(NavTree root, int runId, int featureId)
        {
            return new ShowFeaturesAction().appendNavTrail(new BeginAction().appendNavTrail(root), runId).addChild("Feature Details",
                    getUrl(runId, featureId));
        }

        public ViewURLHelper getUrl(int runId, int featureId)
        {
            ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showFeatureDetails.view", getContainer());
            url.addParameter(ShowFeaturesAction.PARAM_RUNID, runId);
            url.addParameter(ShowPeaksAction.PARAM_FEATUREID, featureId);
            return url;
        }

        private FeatureIdForm _form;
    }

    /**
     * Action to render a particular chart--typically called from an img tag
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowChartAction extends SimpleViewAction<ChartForm>
    {
        public ModelAndView getView(ChartForm form, BindException errors) throws Exception
        {
            if(null == form || form.getFeatureId() < 0 || form.getRunId() < 0)
                return null;

            FeatureChart chart = null;
            String type = form.getType();
            if(type.equalsIgnoreCase("spectrum"))
                chart = new SpectrumChart(form.getRunId(), form.getScan(), form.getMzLow(), form.getMzHigh());
            else if(type.equalsIgnoreCase("bubble"))
                chart = new RetentionMassChart(form.getRunId(), form.getMzLow(), form.getMzHigh(),
                                                form.getScanFirst(), form.getScanLast(), form.getScan());
            else if(type.equalsIgnoreCase("elution"))
                chart = new ElutionChart(form.getRunId(), form.getMzLow(), form.getMzHigh(),
                                                form.getScanFirst(), form.getScanLast());

            if(null != chart)
            {
                getViewContext().getResponse().setContentType("image/png");
                chart.render(getViewContext().getResponse().getOutputStream(),
                                form.getChartWidth(), form.getChartHeight());
            }

            //no need to return a view since this is only called from an <img> tag
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class ShowAdminAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            AdminViewContext ctx = new AdminViewContext(MS1Manager.get().getDeletedFileCount());
            if(getProperty("purgeNow", "false").equals("true") && ctx.getNumDeleted() > 0)
            {
                MS1Manager.get().startManualPurge();
                ctx.setPurgeRunning(true);
            }
            return new JspView<AdminViewContext>("/org/labkey/ms1/view/AdminView.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("MS1 Admin");
            return root;
        }
    }

    /**
     * Action to import an msInspect TSV features file
     */
    @RequiresPermission(ACL.PERM_INSERT)
    public class ImportMsInspectAction extends SimpleViewAction<ImportForm>
    {
        public ModelAndView getView(ImportForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipelineService service = PipelineService.get();

            File f = null;
            String path = form.getPath();
            if (path != null)
            {
                // Figure out the pipeline root for this container, which might
                // be set directly on this container, or on a parent container
                PipeRoot pr = service.findPipelineRoot(c);
                if (pr != null)
                {
                    // The path to the file to load will be relative to the pipeline root
                    URI uriData = URIUtil.resolve(pr.getUri(c), path);
                    f = new File(uriData);
                }
            }


            if (null != f && f.exists() && f.isFile())
            {
                // Assuming we can find the job, put a job in the queue to load this run
                ViewBackgroundInfo info = new ViewBackgroundInfo(getContainer(), getUser(), getViewContext().getViewURLHelper());
                info = service.getJobBackgroundInfo(info, f);
                MSInspectImportPipelineJob job = new MSInspectImportPipelineJob(info, f);
                service.queueJob(job);

                // Be sure to use the job's container for forwarding.
                // Depending on the pipeline setup for this container, the job
                // may actually load the run into a child container
                c = info.getContainer();
            }
            else
            {
                throw new FileNotFoundException("Unable to open the msInspect feature file '" + form.getPath() + "'.");
            }

            ViewURLHelper url = new ViewURLHelper(getViewContext().getRequest(), "Project", "begin", c.getPath());
            return HttpView.redirect(url);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root; //since the action always redirects, just return what was passed in
        }
    } //class ImportMsInspectAction

    /**
     * Form used by the ImportMsInspectAction
     */
    public static class ImportForm
    {
        private String _path;

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }
    } //class ImportForm

    /**
     * Form used by the ShowFeaturesAction
     */
    public static class RunIdForm
    {
        private int _runId = -1;
        private String _export = null;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getExport()
        {
            return _export;
        }

        public void setExport(String export)
        {
            _export = export;
        }
    } //class RunIDForm

    public static class FeatureIdForm
    {
        private int _featureId = -1;
        private int _runId = -1;
        private String _export = null;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getExport()
        {
            return _export;
        }

        public void setExport(String export)
        {
            _export = export;
        }
    }

    public static class MS2PeptideForm
    {
        private int _featureId = -1;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }
    }

    public static class PeaksViewForm
    {
        private int _featureId = -1;
        private int _runId = -1;
        private String _export = null;
        private Integer _scanFirst = null;
        private Integer _scanLast = null;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getExport()
        {
            return _export;
        }

        public void setExport(String export)
        {
            _export = export;
        }

        public Integer getScanFirst()
        {
            return _scanFirst;
        }

        public void setScanFirst(Integer scanFirst)
        {
            _scanFirst = scanFirst;
        }

        public Integer getScanLast()
        {
            return _scanLast;
        }

        public void setScanLast(Integer scanLast)
        {
            _scanLast = scanLast;
        }
    }

    public static class ChartForm
    {
        private int _featureId = -1;
        private int _runId = -1;
        private int _scan = 0;
        private double _mzLow = 0;
        private double _mzHigh = 0;
        private String _type;
        private int _scanFirst = 0;
        private int _scanLast = 0;
        private int _chartWidth = 425;
        private int _chartHeight = 300;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getType()
        {
            return _type;
        }

        public void setType(String type)
        {
            _type = type;
        }

        public int getScan()
        {
            return _scan;
        }

        public void setScan(int scan)
        {
            _scan = scan;
        }

        public int getScanFirst()
        {
            return _scanFirst;
        }

        public void setScanFirst(int scanFirst)
        {
            _scanFirst = scanFirst;
        }

        public int getScanLast()
        {
            return _scanLast;
        }

        public void setScanLast(int scanLast)
        {
            _scanLast = scanLast;
        }

        public double getMzLow()
        {
            return _mzLow;
        }

        public void setMzLow(double mzLow)
        {
            _mzLow = mzLow;
        }

        public double getMzHigh()
        {
            return _mzHigh;
        }

        public void setMzHigh(double mzHigh)
        {
            _mzHigh = mzHigh;
        }

        public int getChartWidth()
        {
            return _chartWidth;
        }

        public void setChartWidth(int chartWidth)
        {
            _chartWidth = chartWidth;
        }

        public int getChartHeight()
        {
            return _chartHeight;
        }

        public void setChartHeight(int chartHeight)
        {
            _chartHeight = chartHeight;
        }
    }

} //class MS1Controller