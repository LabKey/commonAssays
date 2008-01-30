package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.api.ms1.MS1Urls;
import org.labkey.ms1.model.*;
import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;
import org.labkey.ms1.query.*;
import org.labkey.ms1.view.*;
import org.labkey.common.util.Pair;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.MutablePropertyValues;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.sql.SQLException;

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
            export,
            pepSeq
        }

        private int _runId = -1;
        private String _export = "";
        private String _pepSeq = null;

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

        public String getPepSeq()
        {
            return _pepSeq;
        }

        public void setPepSeq(String pepSeq)
        {
            _pepSeq = pepSeq;
        }
    }

    /**
     * Base class for any view that primarily displays a FeaturesView.
     *
     * The ShowFeatureDetailsAction uses these methods to obtain a features
     * view that is initialized in the same way as the one the user came
     * from. This allows the details view to determine the next and previous
     * feature IDs, which drive the prev/next buttons on the UI
     */
    public abstract class BaseFeaturesViewAction<FORM> extends SimpleViewAction<FORM>
    {
        @SuppressWarnings("unchecked")
        protected FeaturesView getFeaturesView(ActionURL url) throws Exception
        {
            //TODO: implement PropertyValues on ActionURL
            MutablePropertyValues props = new MutablePropertyValues();
            for(Pair<String,String> param : url.getParameters())
            {
                props.addPropertyValue(param.getKey(), param.getValue());
            }

            BindException errors = defaultBindParameters((FORM)createCommand(), props);
            return getFeaturesView((FORM)errors.getTarget());
        }

        protected abstract FeaturesView getFeaturesView(FORM form) throws Exception;
    }

    /**
     * Action to show the features for a given experiment run
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeaturesAction extends BaseFeaturesViewAction<ShowFeaturesForm>
    {
        public static final String PARAM_RUNID = "runId";
        private ShowFeaturesForm _form;

        protected FeaturesView getFeaturesView(ShowFeaturesForm form) throws Exception
        {
            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getViewContext().getContainer()),
                                                        getViewContext().getContainer());
            featuresView.getBaseFilters().add(new RunFilter(form.getRunId()));
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                featuresView.getBaseFilters().add(new PeptideFilter(form.getPepSeq(), true));
            return featuresView;
        }

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
            FeaturesView featuresView = getFeaturesView(form);
            
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
            return appendNavTrail(root, runId, getContainer());
        }

        public NavTree appendNavTrail(NavTree root, int runId, Container container)
        {
            return new BeginAction().appendNavTrail(root).addChild("Features from Run", getUrl(runId, container));
        }

        public ActionURL getUrl(int runId, Container container)
        {
            ActionURL url = new ActionURL(MS1Controller.ShowFeaturesAction.class, container);
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
            PeaksView peaksView = new PeaksView(getViewContext(), new MS1Schema(getUser(), getViewContext().getContainer()),
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
            ActionURL url = new ActionURL("MS2", "showPeptide", getViewContext().getContainer());
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
    public class ShowFeatureDetailsAction extends SimpleViewAction<FeatureDetailsForm>
    {
        public static final String ACTION_NAME = "showFeatureDetails";

        private FeatureDetailsForm _form = null;

        public ModelAndView getView(FeatureDetailsForm form, BindException errors) throws Exception
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
            FeaturesView featuresView = getSourceFeaturesView(form.getSrcActionUrl());

            //get the previous and next feature ids (ids will be -1 if there isn't a prev or next)
            int[] prevNextFeatureIds = featuresView.getPrevNextFeature(form.getFeatureId());
            FeatureDetailsModel model = new FeatureDetailsModel(feature, prevNextFeatureIds[0],
                    prevNextFeatureIds[1], form.getSrcUrl(), form.getMzWindowLow(), form.getMzWindowHigh(),
                    form.getScanWindowLow(), form.getScanWindowHigh(), form.getScan(), 
                    getViewContext().getContainer(), getViewContext().getActionURL());

            //cache the form so we can build the nav trail
            _form = form;

            return new JspView<FeatureDetailsModel>("/org/labkey/ms1/view/FeatureDetailView.jsp", model);
        }

        private FeaturesView getSourceFeaturesView(ActionURL url) throws Exception
        {
            BaseFeaturesViewAction action = (BaseFeaturesViewAction)_actionResolver.resolveActionName(MS1Controller.this, url.getAction());
            if(null == action)
                return null;

            //reset the current container on the action's view context to match
            //the container of the source url
            ViewContext vctx = new ViewContext();
            vctx.setContainer(ContainerManager.getForPath(url.getExtraPath()));
            action.setViewContext(vctx);
            
            return action.getFeaturesView(url);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root = new BeginAction().appendNavTrail(root);
            root.addChild("Features List", _form.getSrcUrl());
            root.addChild("Feature Details");
            return root;
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
            export,
            runIds
        }

        private String _pepSeq = "";
        private boolean _exact = false;
        private String _export = "";
        private boolean _subfolders = false;
        private String _runIds = null;

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

        public String getRunIds()
        {
            return _runIds;
        }

        public void setRunIds(String runIds)
        {
            _runIds = runIds;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class PepSearchAction extends BaseFeaturesViewAction<PepSearchForm>
    {
        protected FeaturesView getFeaturesView(PepSearchForm form) throws Exception
        {
            ArrayList<FeaturesFilter> baseFilters = new ArrayList<FeaturesFilter>();
            baseFilters.add(new ContainerFilter(getViewContext().getContainer(), form.isSubfolders(), getUser()));
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                baseFilters.add(new PeptideFilter(form.getPepSeq(), form.isExact()));
            if(null != form.getRunIds() && form.getRunIds().length() > 0)
                baseFilters.add(new RunFilter(form.getRunIds()));

            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getViewContext().getContainer(),
                                            !(form.isSubfolders())), baseFilters);
            featuresView.setTitle("Search Results");
            return featuresView;
        }

        public ModelAndView getView(PepSearchForm form, BindException errors) throws Exception
        {
            //create the search view
            PepSearchModel searchModel = new PepSearchModel(getViewContext().getContainer(), form.getPepSeq(),
                    form.isExact(), form.isSubfolders(), form.getRunIds());
            JspView<PepSearchModel> searchView = new JspView<PepSearchModel>("/org/labkey/ms1/view/PepSearchView.jsp", searchModel);
            searchView.setTitle("Search Criteria");

            //if no search terms were specified, return just the search view
            if(searchModel.noSearchTerms())
            {
                searchModel.setErrorMsg("You must specify at least one Peptide Sequence");
                return searchView;
            }

            //create the features view
            FeaturesView featuresView = getFeaturesView(form);

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
    } //PepSearchAction

    public static class SimilarSearchForm
    {
        public enum ParamNames
        {
            featureId,
            mzOffset,
            mzUnits,
            timeOffset,
            timeUnits,
            subfolders
        }

        public enum MzOffsetUnits
        {
            ppm,
            mz
        }

        public enum TimeOffsetUnits
        {
            rt,
            scans
        }

        private int _featureId = -1;
        private double _mzOffset = 0;
        private MzOffsetUnits _mzUnits = MzOffsetUnits.ppm;
        private double _timeOffset = 30;
        private TimeOffsetUnits _timeUnits = TimeOffsetUnits.rt;
        private boolean _subfolders = false;
        private String _export;
        private Feature _feature = null;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }

        public Feature getFeature() throws SQLException
        {
            if(null == _feature)
                _feature = MS1Manager.get().getFeature(_featureId);
            return _feature;
        }

        public double getMzOffset()
        {
            return _mzOffset;
        }

        public void setMzOffset(double mzOffset)
        {
            _mzOffset = mzOffset;
        }

        public MzOffsetUnits getMzUnits()
        {
            return _mzUnits;
        }

        public void setMzUnits(MzOffsetUnits mzUnits)
        {
            _mzUnits = mzUnits;
        }

        public double getTimeOffset()
        {
            return _timeOffset;
        }

        public void setTimeOffset(double timeOffset)
        {
            _timeOffset = timeOffset;
        }

        public TimeOffsetUnits getTimeUnits()
        {
            return _timeUnits;
        }

        public void setTimeUnits(TimeOffsetUnits timeUnits)
        {
            _timeUnits = timeUnits;
        }

        public boolean isSubfolders()
        {
            return _subfolders;
        }

        public void setSubfolders(boolean subfolders)
        {
            _subfolders = subfolders;
        }

        public String getExport()
        {
            return _export;
        }

        public void setExport(String export)
        {
            _export = export;
        }

        /**
         * Returns a url with default parameter values for mzOffset, mzUnits,
         * timeOffset, and timeUnits. Callers can then add the featureId
         * parameter.
         *
         * @param container The current container
         * @return A default ActionURL
         */
        public static ActionURL getDefaultUrl(Container container)
        {
            ActionURL ret = new ActionURL(SimilarSearchAction.class, container);
            ret.addParameter(ParamNames.mzOffset.name(), 5);
            ret.addParameter(ParamNames.mzUnits.name(), MzOffsetUnits.ppm.name());
            ret.addParameter(ParamNames.timeOffset.name(), 30);
            ret.addParameter(ParamNames.timeUnits.name(), TimeOffsetUnits.rt.name());
            return ret;
        }
    } //SimilarSearchForm

    /**
     * Action for finding features similar to a specified feature id
     */
    @RequiresPermission(ACL.PERM_READ)
    public class SimilarSearchAction extends BaseFeaturesViewAction<SimilarSearchForm>
    {
        protected FeaturesView getFeaturesView(SimilarSearchForm form) throws Exception
        {
            Feature feature = form.getFeature();

            ArrayList<FeaturesFilter> baseFilters = new ArrayList<FeaturesFilter>();
            baseFilters.add(new ContainerFilter(getViewContext().getContainer(), form.isSubfolders(), getUser()));
            baseFilters.add(new MzFilter(feature.getMz().doubleValue(), form.getMzOffset(), form.getMzUnits()));
            if(SimilarSearchForm.TimeOffsetUnits.rt == form.getTimeUnits())
                baseFilters.add(new RetentionTimeFilter(feature, form.getTimeOffset()));
            else
                baseFilters.add(new ScanFilter(feature, (int)(form.getTimeOffset())));

            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getViewContext().getContainer(),
                                            !(form.isSubfolders())), baseFilters);
            featuresView.setTitle("Search Results");
            return featuresView;
        }

        public ModelAndView getView(SimilarSearchForm form, BindException errors) throws Exception
        {
            //if no feature id was passed, redir to begin
            if(form.getFeatureId() < 0)
                return HttpView.redirect(new BeginAction().getUrl());

            //get the source feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Invalid feature id");

            SimilarSearchModel searchModel = new SimilarSearchModel(feature, getViewContext().getContainer(),
                    form.getMzOffset(), form.getMzUnits(), form.getTimeOffset(), form.getTimeUnits(),
                    form.isSubfolders());
            JspView<SimilarSearchModel> searchView = new JspView<SimilarSearchModel>("/org/labkey/ms1/view/SimilarSearchView.jsp", searchModel);

            //create the features view
            FeaturesView featuresView = getFeaturesView(form);

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
            return root.addChild("Find Similar Features");
        }
    }

    public static class CompareRunsSetupForm
    {
    }

    @RequiresPermission(ACL.PERM_READ)
    public class CompareRunsSetupAction extends SimpleViewAction<CompareRunsSetupForm>
    {
        public ModelAndView getView(CompareRunsSetupForm compareRunsSetupForm, BindException errors) throws Exception
        {
            Set<String> selectedRuns = DataRegionSelection.getSelected(getViewContext(), true);
            if(null == selectedRuns || selectedRuns.size() < 1)
                return HttpView.redirect(new BeginAction().getUrl().getLocalURIString());

            ActionURL url = new ActionURL(CompareRunsAction.class, getViewContext().getContainer());
            StringBuilder runIds = new StringBuilder();
            String sep = "";
            for(String run : selectedRuns)
            {
                runIds.append(sep);
                runIds.append(run);
                sep = ",";
            }

            url.addParameter(CompareRunsForm.ParamNames.runIds.name(), runIds.toString());
            return HttpView.redirect(url);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    public static class CompareRunsForm extends QueryViewAction.QueryExportForm
    {
        public enum ParamNames
        {
            runIds
        }

        private String _runIds = null;

        public String getRunIds()
        {
            return _runIds;
        }

        public void setRunIds(String runIds)
        {
            this._runIds = runIds;
        }

        public int[] getRunIdArray()
        {
            return PageFlowUtil.toInts(_runIds.split(","));
        }
    }

    @RequiresPermission(ACL.PERM_NONE)
    public class CompareRunsAction extends QueryViewAction<CompareRunsForm,CompareRunsView>
    {
        public CompareRunsAction()
        {
            super(CompareRunsForm.class);
        }

        protected CompareRunsView createQueryView(CompareRunsForm form, BindException errors, boolean forExport) throws Exception
        {
            return new CompareRunsView(new MS1Schema(getUser(), getViewContext().getContainer()), form.getRunIdArray());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare Runs");
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

    public static class FeatureDetailsForm
    {
        public enum ParamNames
        {
            featureId,
            srcUrl,
            mzWindowLow,
            mzWindowHigh,
            scanWindowLow,
            scanWindowHigh,
            scan
        }

        private int _featureId = -1;
        private String _srcUrl;
        private double _mzWindowLow = -1;
        private double _mzWindowHigh = 5;
        private int _scanWindowLow = 0;
        private int _scanWindowHigh = 0;
        private int _scan = -1;


        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
        }

        public String getSrcUrl()
        {
            return _srcUrl;
        }

        public void setSrcUrl(String srcUrl)
        {
            _srcUrl = srcUrl;
        }

        public ActionURL getSrcActionUrl()
        {
            return new ActionURL(_srcUrl);
        }

        public double getMzWindowLow()
        {
            return _mzWindowLow;
        }

        public void setMzWindowLow(double mzWindowLow)
        {
            _mzWindowLow = mzWindowLow;
        }

        public double getMzWindowHigh()
        {
            return _mzWindowHigh;
        }

        public void setMzWindowHigh(double mzWindowHigh)
        {
            _mzWindowHigh = mzWindowHigh;
        }

        public int getScanWindowLow()
        {
            return _scanWindowLow;
        }

        public void setScanWindowLow(int scanWindowLow)
        {
            _scanWindowLow = scanWindowLow;
        }

        public int getScanWindowHigh()
        {
            return _scanWindowHigh;
        }

        public void setScanWindowHigh(int scanWindowHigh)
        {
            _scanWindowHigh = scanWindowHigh;
        }

        public int getScan()
        {
            return _scan;
        }

        public void setScan(int scan)
        {
            _scan = scan;
        }
    } //FeatureDetailsForm

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

    public static class MS1UrlsImpl implements MS1Urls
    {
        public ActionURL getPepSearchUrl(Container container)
        {
            return getPepSearchUrl(container, null);
        }

        public ActionURL getPepSearchUrl(Container container, String sequence)
        {
            ActionURL url = new ActionURL(PepSearchAction.class, container);
            if(null != sequence)
                url.addParameter(PepSearchForm.ParamNames.pepSeq.name(), sequence);
            return url;
        }
    }

    public static String createVerifySelectedScript(DataView view, ActionURL url)
    {
        //copied from MS2Controller--perhaps we should move this to API?
        return "javascript: if (verifySelected(document.forms[\"" + view.getDataRegion().getName() + "\"], \"" + url.getLocalURIString() + "\", \"post\", \"runs\")) { document.forms[\"" + view.getDataRegion().getName() + "\"].submit(); }";
    }

} //class MS1Controller