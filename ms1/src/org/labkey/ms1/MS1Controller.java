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
import org.labkey.api.view.template.PageConfig;
import org.labkey.ms1.model.*;
import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;
import org.labkey.ms1.query.*;
import org.labkey.ms1.view.*;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;

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
    public ActionURL getActionURL(String action)
    {
        return new ActionURL(MS1Module.CONTROLLER_NAME, action, getContainer());
    }

    /**
     * Exports a QueryView (or derived class) to Excel, TSV, or Print, depending
     * on the value of the format parameter.
     *
     * @param view The view to export
     * @param config The page configuration object for the action
     * @param format The export format (may be "excel", "tsv", or "print")
     * @return A null ModelAndView suitable for returning from the Action's getView() method
     * @throws Exception Thrown from QueryView's export methods
     */
    protected ModelAndView exportQueryView(QueryView view, PageConfig config, String format) throws Exception
    {
        if(format.equalsIgnoreCase("excel"))
            view.exportToExcel(getViewContext().getResponse());
        else if(format.equalsIgnoreCase("tsv"))
            view.exportToTsv(getViewContext().getResponse());
        else if(format.equalsIgnoreCase("print"))
        {
            view.setPrintView(true);
            config.setTemplate(PageConfig.Template.Print);
            config.setShowPrintDialog(true);
            return view;
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

        public ActionURL getUrl()
        {
            return new ActionURL(MS1Controller.BeginAction.class, getContainer());
        }
    } //class BeginAction

    /**
     * Form class for the ShowFeaturesAction
     */
    public static class ShowFeaturesForm
    {
        public enum ParamNames
        {
            runId,
            export
        }

        private int _runId = -1;
        private String _export = "";

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

        public boolean runSpecified()
        {
            return _runId >= 0;
        }
    }

    /**
     * Action to show the features for a given experiment run
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeaturesAction extends SimpleViewAction<ShowFeaturesForm>
    {
        public static final String PARAM_RUNID = "runId";
        private ShowFeaturesForm _form;

        public ModelAndView getView(ShowFeaturesForm form, BindException errors) throws Exception
        {
            //this action requires that a sepcific experiment run has been specified
            if(!form.runSpecified())
                return HttpView.redirect(MS1Controller.this.getActionURL("begin"));

            //ensure that the experiment run is valid and exists within the current container
            ExpRun run = ExperimentService.get().getExpRun(form.getRunId());
            if(null == run || !(run.getContainer().equals(getViewContext().getContainer())))
                throw new NotFoundException("Experiment run " + form.getRunId() + " does not exist in " + getViewContext().getContainer().getPath());

            MS1Manager mgr = MS1Manager.get();

            //determine if there is peak data available for these features
            MS1Manager.PeakAvailability peakAvail = mgr.isPeakDataAvailable(form.getRunId());

            //create the features view
            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer()),
                                                        getContainer());
            featuresView.getBaseFilters().add(new RunFilter(form.getRunId()));
            
            featuresView.setTitle("Features from " + run.getName());

            //if there is an export request, export and return
            if(isExportRequest(form.getExport()))
            {
                featuresView.setForExport(true);
                return exportQueryView(featuresView, getPageConfig(), form.getExport());
            }

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
            return appendNavTrail(root, runId, null);
        }

        public NavTree appendNavTrail(NavTree root, int runId, Container container)
        {
            return new BeginAction().appendNavTrail(root).addChild("Features from Run", getUrl(runId, container));
        }

        public ActionURL getUrl(int runId, Container container)
        {
            ActionURL url = new ActionURL(MS1Controller.ShowFeaturesAction.class,
                    null == container ? getContainer() : container);
            url.addParameter(ShowFeaturesForm.ParamNames.runId.name(), runId);
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
        public static final String ACTION_NAME = "showPeaks";

        private int _runId = -1;

        public ModelAndView getView(PeaksViewForm form, BindException errors) throws Exception
        {
            if(-1 == form.getFeatureId())
                return HttpView.redirect(MS1Controller.this.getActionURL("begin"));

            MS1Manager mgr = MS1Manager.get();

            //get the feature and ensure that it is valid and that it's ExpRun is valid
            //and that it exists within the current container
            Feature feature = mgr.getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getViewContext().getContainer().getPath());

            ExpRun expRun = feature.getExpRun();
            if(null == expRun)
                throw new NotFoundException("Could not find the experiment run for feature id '" + form.getFeatureId() + "'.");

            //because we are now showing features from sub-folders in search results, the specified
            //feature/run may exist in a different container than the current one. If so, redirect to
            //the appropriate container
            if(!(expRun.getContainer().equals(getViewContext().getContainer())))
            {
                ActionURL redir = getViewContext().getActionURL().clone();
                redir.setExtraPath(expRun.getContainer().getPath());
                return HttpView.redirect(redir);
            }

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
                return exportQueryView(peaksView, getPageConfig(), form.getExport());

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

            //cache the run id so we can build the nav trail
            _runId = expRun.getRowId();

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
            return new ShowFeaturesAction().appendNavTrail(root, _runId).addChild("Peaks for Feature");
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
                return HttpView.redirect(MS1Controller.this.getActionURL("begin"));

            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                return new HtmlView("Invalid Feature Id: " + form.getFeatureId());

            Peptide[] peptides = feature.getMatchingPeptides();
            if(null == peptides || 0 == peptides.length)
                return new HtmlView("The corresponding MS2 peptide information was not found in the database. Ensure that it has been imported before attempting to view the MS2 peptide.");

            Peptide pepFirst = peptides[0];
            ActionURL url = new ActionURL("MS2", "showPeptide", getContainer());
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
        public static final String ACTION_NAME = "showFeatureDetails";

        private int _runId = -1;

        public ModelAndView getView(FeatureIdForm form, BindException errors) throws Exception
        {
            if(null == form || form.getFeatureId() < 0)
                return HttpView.redirect(MS1Controller.this.getActionURL("begin"));

            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getViewContext().getContainer().getPath());

            //ensure that the run for this feature exists within the current container
            //and that the runid parameter matches
            ExpRun run = feature.getExpRun();
            if(null == run)
                throw new NotFoundException("Could not find the experiment run for feature id '" + form.getFeatureId() + "'.");

            //because we are now showing features from sub-folders in search results, the specified
            //feature/run may exist in a different container than the current one. If so, redirect to
            //the appropriate container
            if(!(run.getContainer().equals(getViewContext().getContainer())))
            {
                ActionURL redir = getViewContext().getActionURL().clone();
                redir.setExtraPath(run.getContainer().getPath());
                return HttpView.redirect(redir);
            }

            //create and initialize a new features view so that we can know which features
            //are immediately before and after the current one
            //this gives the illusion to the user that they are stepping through the same list of
            //features they were viewing on the previous screen (the showFeatures action)
            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer()),
                    FeaturesFilterFactory.createFilters(getViewContext().getActionURL(), getUser()));

            //get the previous and next feature ids (ids will be -1 if there isn't a prev or next)
            int[] prevNextFeatureIds = featuresView.getPrevNextFeature(form.getFeatureId());
            FeatureDetailsViewContext ctx = new FeatureDetailsViewContext(feature, prevNextFeatureIds[0], prevNextFeatureIds[1]);

            //cache the experiment run id so we can build the nav trail
            _runId = run.getRowId();

            return new JspView<FeatureDetailsViewContext>("/org/labkey/ms1/view/FeatureDetailView.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return new ShowFeaturesAction().appendNavTrail(root, _runId).addChild("Feature Details");
        }
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
     * Form used for PepSearchAction
     */
    public static class PepSearchForm
    {
        public enum ParamNames
        {
            pepSeq,
            exact,
            subfolders,
            export
        }

        private String _pepSeq = "";
        private boolean _exact = false;
        private String _export = "";
        private boolean _subfolders = false;

        public String getPepSeq()
        {
            return _pepSeq;
        }

        public void setPepSeq(String pepSeq)
        {
            _pepSeq = pepSeq;
        }

        public boolean isExact()
        {
            return _exact;
        }

        public void setExact(boolean exact)
        {
            _exact = exact;
        }

        public String getExport()
        {
            return _export;
        }

        public void setExport(String export)
        {
            _export = export;
        }

        public boolean isSubfolders()
        {
            return _subfolders;
        }

        public void setSubfolders(boolean subfolders)
        {
            _subfolders = subfolders;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class PepSearchAction extends SimpleViewAction<PepSearchForm>
    {
        public ModelAndView getView(PepSearchForm form, BindException errors) throws Exception
        {
            //create the search view
            PepSearchModel searchModel = new PepSearchModel(getContainer(), form.getPepSeq(), form.isExact(), form.isSubfolders());
            JspView<PepSearchModel> searchView = new JspView<PepSearchModel>("/org/labkey/ms1/view/PepSearchView.jsp", searchModel);
            searchView.setTitle("Search Criteria");

            //if no search terms were specified, return just the search view
            if(searchModel.noSearchTerms())
            {
                searchModel.setErrorMsg("You must specify at least one Peptide Sequence");
                return searchView;
            }

            //create the features view
            ArrayList<FeaturesFilter> baseFilters = new ArrayList<FeaturesFilter>();
            baseFilters.add(new ContainerFilter(getContainer(), form.isSubfolders(), getUser()));
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                baseFilters.add(new PeptideFilter(form.getPepSeq(), form.isExact()));

            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer(),
                                            !(form.isSubfolders())), baseFilters);
            featuresView.setTitle("Search Results");

            //if there is an export request, export and return
            if(isExportRequest(form.getExport()))
            {
                featuresView.setForExport(true);
                return exportQueryView(featuresView, getPageConfig(), form.getExport());
            }

            return new VBox(searchView, featuresView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Feature Search Results");
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
                MSInspectImportPipelineJob job = new MSInspectImportPipelineJob(getViewBackgroundInfo(), f);
                service.queueJob(job);
            }
            else
            {
                throw new FileNotFoundException("Unable to open the msInspect feature file '" + form.getPath() + "'.");
            }

            ActionURL url = new ActionURL("Project", "begin", c.getPath());
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

    public static class FeatureIdForm
    {
        private int _featureId = -1;
        private String _export = null;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
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