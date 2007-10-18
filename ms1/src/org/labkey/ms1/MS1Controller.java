package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.api.query.QueryView;
import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * This controller is the entry point for all web pages specific to the MS1
 * module. Each action is represented by a nested class named as such:
 * [action]Action
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
     * @param action Name of action
     * @return URL helper for the action in this controller and current container
     */
    protected ViewURLHelper getViewURLHelper(String action)
    {
        return new ViewURLHelper(MS1Module.CONTROLLER_NAME, action, getContainer());
    }

    /**
     * Adds the begin step to a NavTree
     * @param root The root of the NavTree
     * @return Modified NavTree
     */
    protected NavTree addBeginChild(NavTree root)
    {
        return root.addChild("MS1 Runs", getViewURLHelper("begin.view"));
    }

    /**
     * Adds the Features view step to a NavTree
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
            return addBeginChild(root);
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

            MS1Manager mgr = MS1Manager.get();

            //determine if there is peak data available for these features
            boolean peaksAvailable = mgr.isPeakDataAvailable(form.getRunId());

            //create the features view
            FeaturesView featuresView = new FeaturesView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                            form.getRunId(), peaksAvailable, 
                                                            isExportRequest(form.getExport()));

            //if there is an export request, export and return
            if(isExportRequest(form.getExport()))
                return exportQueryView(featuresView, form.getExport());

            //get the corresponding file Id and initialize a software view
            JspView softwareView = null;
            Integer fileId = mgr.getFileIdForRun(form.getRunId(), MS1Manager.FILETYPE_FEATURES);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId.intValue());
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<Software[]>("/org/labkey/ms1/softwareView.jsp", swares);
                    softwareView.setTitle("Software Information");
                }
            }

            //save the form so that we have access to it in the appendNavTrail method
            _form = form;

            if(null != softwareView)
                return new VBox(softwareView, featuresView);
            else
                return featuresView;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            addBeginChild(root);
            if(null != _form)
                addFeaturesChild(root, _form.getRunId());

            _form = null;
            return root;
        }
    } //class ShowFeaturesAction

    /**
     * Action to show the peaks for a given experiment run and scan number
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowPeaksAction extends SimpleViewAction<FeatureIdForm>
    {
        public static final String PARAM_FEATUREID = "featureId";
        private FeatureIdForm _form;

        public ModelAndView getView(FeatureIdForm form, BindException errors) throws Exception
        {
            _form = null;
            if(-1 == form.getRunId() && -1 == form.getFeatureId())
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            MS1Manager mgr = MS1Manager.get();

            //get the feature
            Feature feature = mgr.getFeature(form.getFeatureId());
            if(null == feature)
                return new HtmlView("The Feature Id '" + form.getFeatureId() + "' was not found in the database.");

            //get the experiment run
            ExpRun expRun = feature.getExpRun();
            if(null == expRun)
                return new HtmlView("The Experiment run this Feature was not found in the database.");

            Integer[] scanIds = mgr.getScanIdsFromRunScans(form.getRunId(), new Integer[]{feature.getScanFirst(), feature.getScanLast()});
            if(null == scanIds || scanIds.length < 2 || null == scanIds[0] || null == scanIds[1])
                return new HtmlView("The supporting peak data for this Feature have not yet been uploaded to the database.");

            //initialize the PeaksView
            PeaksView peaksView = new PeaksView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                expRun, feature);

            //if there is an export parameter, do the export and return
            if(isExportRequest(form.getExport()))
                return exportQueryView(peaksView, form.getExport());

            //if software information is available, create and initialize the software view
            JspView softwareView = null;
            Integer fileId = mgr.getFileIdForRun(form.getRunId(), MS1Manager.FILETYPE_PEAKS);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId.intValue());
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<Software[]>("/org/labkey/ms1/softwareView.jsp", swares);
                    softwareView.setTitle("Software Information");
                }
            }

            //save the form so we have access to the params when we build the nav trail
            _form = form;

            if(null != softwareView)
                return new VBox(softwareView, peaksView);
            else
                return peaksView;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            addBeginChild(root);
            if(null != _form)
            {
                addFeaturesChild(root, _form.getRunId());
                addPeaksChild(root, _form.getRunId(), _form.getFeatureId());
            }
            _form = null;
            return root;
        }
    } //class ShowFeaturesAction

    @RequiresPermission(ACL.PERM_READ)
    public class ShowMS2PeptideAction extends SimpleViewAction<FeatureIdForm>
    {
        public ModelAndView getView(FeatureIdForm featureIdForm, BindException errors) throws Exception
        {
            if(null == featureIdForm || featureIdForm.getFeatureId() < 0)
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            //get the first matching peptide for the given feature id
            FeaturePeptideLink link = MS1Manager.get().getFeaturePeptideLink(featureIdForm.getFeatureId());
            if(null == link)
                return new HtmlView("The corresponding MS2 peptide information was not found in the database. Ensure that it has been imported before attempting to view the MS2 peptide.");

            ViewURLHelper url = new ViewURLHelper("ms2", "showPeptide", getContainer());
            url.addParameter("run", String.valueOf(link.getMs2Run()));
            url.addParameter("peptideId", link.getPeptideId());
            url.addParameter("rowIndex", 1);

            //add a filter for MS2 scan so that the showPeptide view will know to enable or
            //disable it's <<prev and next>> buttons based on how many peptides were actually
            //matched.
            return HttpView.redirect(url + "&MS2Peptides.Scan~eq=" + link.getScan());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            //if this gets called, then we couldn't find the peptide and
            //displayed the message returned in the HtmlView above.
            return root.addChild("Associated Peptide Not Found");
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeatureDetailsAction extends SimpleViewAction<FeatureIdForm>
    {

        public ModelAndView getView(FeatureIdForm form, BindException errors) throws Exception
        {
            if(form.getFeatureId() < 0)
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));

            _form = form;

            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            FeaturesView featuresView = new FeaturesView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                            form.getRunId(), false, true);

            int[] prevNextFeatureIds = featuresView.getPrevNextFeature(form.getFeatureId());
            FeatureDetailsViewContext ctx = new FeatureDetailsViewContext(feature, prevNextFeatureIds[0], prevNextFeatureIds[1]);
            
            return new JspView<FeatureDetailsViewContext>("/org/labkey/ms1/FeatureDetailView.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if(null != _form)
                addFeaturesChild(root, _form.getRunId());
            _form = null;
            return root.addChild("Feature Details");
        }
        private FeatureIdForm _form;
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowChartAction extends SimpleViewAction<ChartForm>
    {
        public ModelAndView getView(ChartForm form, BindException errors) throws Exception
        {
            FeatureChart chart = null;
            String type = form.getType();
            if(type.equalsIgnoreCase("spectrum"))
                chart = new SpectrumChart(form.getRunId(), form.getScan(), form.getMzLow(), form.getMzHigh());
            else if(type.equalsIgnoreCase("bubble"))
                chart = new RetentionMassChart(form.getRunId(), form.getMzLow(), form.getMzHigh(),
                                                form.getScanFirst(), form.getScanLast());
            else if(type.equalsIgnoreCase("elution"))
                chart = new ElutionChart(form.getRunId(), form.getMzLow(), form.getMzHigh(),
                                                form.getScanFirst(), form.getScanLast());

            if(null != chart)
            {
                getViewContext().getResponse().setContentType("image/png");
                chart.render(getViewContext().getResponse().getOutputStream());
            }

            //no need to return a view since this is only called from an <img> tag
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
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

    public static class RunScanForm
    {
        private int _runId = -1;
        private int _scan = -1;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public int getScan()
        {
            return _scan;
        }

        public void setScan(int scan)
        {
            _scan = scan;
        }
    } //class RunScanForm

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
    }
} //class MS1Controller