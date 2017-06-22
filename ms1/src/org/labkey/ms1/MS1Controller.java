/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

package org.labkey.ms1;

import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.GWTServiceAction;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.*;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.ms1.MS1Urls;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.settings.AdminConsole.*;
import org.labkey.ms1.model.*;
import org.labkey.ms1.query.*;
import org.labkey.ms1.view.*;
import org.labkey.ms1.client.MS1VennDiagramView;
import org.labkey.api.util.Pair;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.beans.MutablePropertyValues;

import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * This controller is the entry point for all web pages specific to the MS1
 * module. Each action is represented by a nested class named as such:
 * [action]Action
 * @author DaveS
 */
public class MS1Controller extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(MS1Controller.class);

    public MS1Controller() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    public static void registerAdminConsoleLinks()
    {
        AdminConsole.addLink(SettingsLinkType.Management, "ms1", getShowAdminURL(), AdminPermission.class);
    }

    /**
     * Begin action for the MS1 Module. Displays a list of msInspect feature finding runs
     */
    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        // Invoked via reflection
        @SuppressWarnings("UnusedDeclaration")
        public BeginAction()
        {
        }

        public BeginAction(ViewContext ctx)
        {
            setViewContext(ctx);
        }

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return ExperimentService.get().createExperimentRunWebPart(getViewContext(), MS1Module.EXP_RUN_TYPE);
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
    public static class ShowFeaturesForm extends QueryViewAction.QueryExportForm
    {
        public enum ParamNames
        {
            runId,
            pepSeq
        }

        private int _runId = -1;
        private String _pepSeq = null;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
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
    public abstract class BaseFeaturesViewAction<FORM extends QueryViewAction.QueryExportForm, VIEW extends QueryView> extends QueryViewAction<FORM, VIEW>
    {
        public BaseFeaturesViewAction(Class<FORM> formClass)
        {
            super(formClass);
        }

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
            return getFeaturesView((FORM)errors.getTarget(), null, false);
        }

        protected abstract FeaturesView getFeaturesView(FORM form, BindException bindErrors, boolean forExport) throws Exception;
    }

    public abstract class BasicFeaturesViewAction<FORM extends QueryViewAction.QueryExportForm> extends BaseFeaturesViewAction<FORM, FeaturesView>
    {
        public BasicFeaturesViewAction(Class<FORM> formClass)
        {
            super(formClass);
        }

        protected FeaturesView createQueryView(FORM form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            return getFeaturesView(form, errors, forExport);
        }
    }

    /**
     * Action to show the features for a given experiment run
     */
    @RequiresPermission(ReadPermission.class)
    public class ShowFeaturesAction extends BasicFeaturesViewAction<ShowFeaturesForm>
    {
        public static final String PARAM_RUNID = "runId";
        private ShowFeaturesForm _form;

        public ShowFeaturesAction()
        {
            super(ShowFeaturesForm.class);
        }

        public ShowFeaturesAction(ViewContext ctx)
        {
            this();
            setViewContext(ctx);
        }

        protected FeaturesView getFeaturesView(ShowFeaturesForm form, BindException bindErrors, boolean forExport)
        {
            _form = form;
            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer()),
                                                        form.getRunId());
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                featuresView.getBaseFilters().add(new PeptideFilter(form.getPepSeq(), true));
            featuresView.setForExport(forExport);
            return featuresView;
        }

        protected ModelAndView getHtmlView(ShowFeaturesForm form, BindException errors) throws Exception
        {
            //this action requires that a specific experiment run has been specified
            if(!form.runSpecified())
                return HttpView.redirect(new ActionURL(MS1Controller.BeginAction.class, getContainer()));

            //ensure that the experiment run is valid and exists within the current container
            ExpRun run = ExperimentService.get().getExpRun(form.getRunId());
            if(null == run || !(run.getContainer().equals(getContainer())))
                throw new NotFoundException("Experiment run " + form.getRunId() + " does not exist in " + getContainer().getPath());

            MS1Manager mgr = MS1Manager.get();

            //determine if there is peak data available for these features
            MS1Manager.PeakAvailability peakAvail = mgr.isPeakDataAvailable(form.getRunId());

            //create the features view
            FeaturesView featuresView = createInitializedQueryView(form, errors, false, null);
            
            featuresView.setTitle("Features from " + run.getName());

            //get the corresponding file Id and initialize a software view if there is software info
            //also create a file details view
            JspView<Software[]> softwareView = null;
            JspView<DataFile> fileDetailsView = null;
            Integer fileId = mgr.getFileIdForRun(form.getRunId(), MS1Manager.FILETYPE_FEATURES);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId);
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<>("/org/labkey/ms1/view/softwareView.jsp", swares);
                    softwareView.setTitle("Processing Software Information");
                }

                DataFile dataFile = mgr.getDataFile(fileId);
                if(null != dataFile)
                {
                    fileDetailsView = new JspView<>("/org/labkey/ms1/view/FileDetailsView.jsp", dataFile);
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
            return new BeginAction(getViewContext()).appendNavTrail(root).addChild("Features from Run", getUrl(runId, container));
        }

        public ActionURL getUrl(int runId, Container container)
        {
            ActionURL url = new ActionURL(MS1Controller.ShowFeaturesAction.class, container);
            url.addParameter(ShowFeaturesForm.ParamNames.runId.name(), runId);
            url.addParameter(DataRegion.LAST_FILTER_PARAM, "true");
            return url;
        }
    } //class ShowFeaturesAction

    /**
     * Action to show the peaks for a given experiment run and scan number
     */
    @RequiresPermission(ReadPermission.class)
    public class ShowPeaksAction extends QueryViewAction<PeaksViewForm, PeaksView>
    {
        private int _runId = -1;

        public ShowPeaksAction()
        {
            super(PeaksViewForm.class);
        }

        protected PeaksView createQueryView(PeaksViewForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            Feature feature = form.getFeature();
            return new PeaksView(getViewContext(), new MS1Schema(getUser(), getContainer()),
                                                feature.getExpRun(), feature,
                                                null == form.getScanFirst() ? feature.getScanFirst().intValue() : form.getScanFirst().intValue(),
                                                null == form.getScanLast() ? feature.getScanLast().intValue() : form.getScanLast().intValue());
        }

        public ModelAndView getHtmlView(PeaksViewForm form, BindException errors) throws Exception
        {
            if(-1 == form.getFeatureId())
                return HttpView.redirect(new ActionURL(MS1Controller.BeginAction.class, getContainer()));

            //get the feature and ensure that it is valid and that its ExpRun is valid
            //and that it exists within the current container
            Feature feature = form.getFeature();
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getContainer().getPath());

            ExpRun expRun = feature.getExpRun();
            if(null == expRun)
                throw new NotFoundException("Could not find the experiment run for feature id '" + form.getFeatureId() + "'.");

            //because we are now showing features from sub-folders in search results, the specified
            //feature/run may exist in a different container than the current one. If so, redirect to
            //the appropriate container
            if(!(expRun.getContainer().equals(getContainer())))
            {
                ActionURL redir = getViewContext().getActionURL().clone();
                redir.setContainer(expRun.getContainer());
                return HttpView.redirect(redir);
            }

            //ensure that we have a scanFirst and scanLast value for this feature
            //if we don't, we can't filter the peaks to a reasonable subset
            if(null == feature.getScanFirst() || null == feature.getScanLast())
                return new HtmlView("The peaks for this feature cannot be displayed because the first and last scan number for the feature were not supplied.");

            //get the peaks view
            PeaksView peaksView = createInitializedQueryView(form, errors, false, null);

            //if software information is available, create and initialize the software view
            //also the data file information view
            JspView<Software[]> softwareView = null;
            JspView<DataFile> fileDetailsView = null;
            MS1Manager mgr = MS1Manager.get();
            Integer fileId = mgr.getFileIdForRun(expRun.getRowId(), MS1Manager.FILETYPE_PEAKS);
            if(null != fileId)
            {
                Software[] swares = mgr.getSoftware(fileId);
                if(null != swares && swares.length > 0)
                {
                    softwareView = new JspView<>("/org/labkey/ms1/view/softwareView.jsp", swares);
                    softwareView.setTitle("Software Information");
                }

                DataFile dataFile = mgr.getDataFile(fileId);
                if(null != dataFile)
                {
                    fileDetailsView = new JspView<>("/org/labkey/ms1/view/FileDetailsView.jsp", dataFile);
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
            return new ShowFeaturesAction(getViewContext()).appendNavTrail(root, _runId).addChild("Peaks for Feature");
        }
    } //class ShowFeaturesAction

    /**
     * Action to show the related MS2 peptide(s) for the specified feature
     */
    @RequiresPermission(ReadPermission.class)
    public class ShowMS2PeptideAction extends SimpleViewAction<MS2PeptideForm>
    {
        public ModelAndView getView(MS2PeptideForm form, BindException errors) throws Exception
        {
            if(null == form || form.getFeatureId() < 0)
                return HttpView.redirect(new ActionURL(MS1Controller.BeginAction.class, getContainer()));

            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                return new HtmlView("Invalid Feature Id: " + form.getFeatureId());

            Peptide[] peptides = feature.getMatchingPeptides();
            if(null == peptides || 0 == peptides.length)
                return new HtmlView("The corresponding MS2 peptide information was not found in the database. Ensure that it has been imported before attempting to view the MS2 peptide.");

            Peptide pepFirst = peptides[0];
            ActionURL url = PageFlowUtil.urlProvider(MS2Urls.class).getShowPeptideUrl(getContainer());
            url.addParameter("run", String.valueOf(pepFirst.getRun()));
            url.addParameter("peptideId", String.valueOf(pepFirst.getRowId()));
            url.addParameter("rowIndex", 1);

            //add a filter for MS2 scan so that the showPeptide view will know to enable or
            //disable its <<prev and next>> buttons based on how many peptides were actually
            //matched.
            return HttpView.redirect(url + "&MS2Peptides.Scan~eq=" + feature.getMs2Scan() + "&MS2Peptides.Charge~eq=" + feature.getMs2Charge());
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
    @RequiresPermission(ReadPermission.class)
    public class ShowFeatureDetailsAction extends SimpleViewAction<FeatureDetailsForm>
    {
        private FeatureDetailsForm _form = null;

        public ModelAndView getView(FeatureDetailsForm form, BindException errors) throws Exception
        {
            //get the feature
            Feature feature = MS1Manager.get().getFeature(form.getFeatureId());
            if(null == feature)
                throw new NotFoundException("Feature " + form.getFeatureId() + " does not exist within " + getContainer().getPath());

            //ensure that the run for this feature exists within the current container
            //and that the runid parameter matches
            ExpRun run = feature.getExpRun();
            if(null == run)
                throw new NotFoundException("Could not find the experiment run for feature id '" + form.getFeatureId() + "'.");

            //because we are now showing features from sub-folders in search results, the specified
            //feature/run may exist in a different container than the current one. If so, redirect to
            //the appropriate container
            if(!(run.getContainer().equals(getContainer())))
            {
                ActionURL redir = getViewContext().getActionURL().clone();
                redir.setContainer(run.getContainer());
                return HttpView.redirect(redir);
            }

            //create and initialize a new features view so that we can know which features
            //are immediately before and after the current one
            //this gives the illusion to the user that they are stepping through the same list of
            //features they were viewing on the previous screen (the showFeatures action)
            FeaturesView featuresView = getSourceFeaturesView(form.getSrcActionUrl());

            //get the previous and next feature ids (ids should be -1 if there isn't a prev or next)
            int[] prevNextFeatureIds;
            if(null != featuresView)
                prevNextFeatureIds = featuresView.getPrevNextFeature(form.getFeatureId());
            else
                prevNextFeatureIds = new int[]{-1,-1};

            FeatureDetailsModel model = new FeatureDetailsModel(feature, prevNextFeatureIds[0],
                    prevNextFeatureIds[1], form.getSrcUrl(), form.getMzWindowLow(), form.getMzWindowHigh(),
                    form.getScanWindowLow(), form.getScanWindowHigh(), form.getScan(), 
                    getContainer(), getViewContext().getActionURL());

            //cache the form so we can build the nav trail
            _form = form;

            return new JspView<>("/org/labkey/ms1/view/FeatureDetailView.jsp", model);
        }

        private FeaturesView getSourceFeaturesView(ActionURL url) throws Exception
        {
            if(null == url)
                return null;

            Controller action = _actionResolver.resolveActionName(MS1Controller.this, url.getAction());
            if(null == action || !(action instanceof BaseFeaturesViewAction))
                return null;

            BaseFeaturesViewAction fvaction = (BaseFeaturesViewAction)action;

            //reset the current container on the action's view context to match
            //the container of the source url
            ViewContext vctx = new ViewContext();
            vctx.setContainer(ContainerManager.getForPath(url.getExtraPath()));
            vctx.setUser(getUser());
            fvaction.setViewContext(vctx);
            
            return fvaction.getFeaturesView(url);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root = new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("Features List", _form.getSrcUrl());
            root.addChild("Feature Details");
            return root;
        }
    }

    /**
     * Action to render a particular chart--typically called from an img tag
     */
    @RequiresPermission(ReadPermission.class)
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

    public static ActionURL getShowAdminURL()
    {
        return new ActionURL(ShowAdminAction.class, ContainerManager.getRoot());
    }

    @RequiresPermission(AdminPermission.class)
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
            return new JspView<>("/org/labkey/ms1/view/AdminView.jsp", ctx);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            PageFlowUtil.urlProvider(AdminUrls.class).appendAdminNavTrail(root, "MS1 Admin", null);
            return root;
        }
    }

    public static class PeptideFilterSearchForm extends ProteinService.PeptideSearchForm
    {
        @Override
        public PeptideFilter createFilter(String sequenceColumnName)
        {
            return new PeptideFilter(getPepSeq(), isExact(), sequenceColumnName, null);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class PepSearchAction extends BaseFeaturesViewAction<PeptideFilterSearchForm, QueryView>
    {
        public PepSearchAction()
        {
            super(PeptideFilterSearchForm.class);
        }

        protected QueryView createQueryView(PeptideFilterSearchForm pepSearchForm, BindException bindErrors, boolean forExport, String dataRegion) throws Exception
        {
            if(FeaturesView.DATAREGION_NAME.equalsIgnoreCase(dataRegion))
                return getFeaturesView(pepSearchForm, bindErrors, forExport);
            else if (PeptidesView.DATAREGION_NAME.equalsIgnoreCase(dataRegion))
                return getPeptidesView(pepSearchForm, bindErrors, forExport);

            for (ProteinService.QueryViewProvider<ProteinService.PeptideSearchForm> viewProvider : ServiceRegistry.get().getService(ProteinService.class).getPeptideSearchViews())
            {
                if (viewProvider.getDataRegionName().equalsIgnoreCase(dataRegion))
                {
                    return viewProvider.createView(getViewContext(), pepSearchForm, bindErrors);
                }
            }

            throw new NotFoundException("Unknown data region: " + dataRegion);
        }

        protected FeaturesView getFeaturesView(PeptideFilterSearchForm form, BindException bindErrors, boolean forExport) throws Exception
        {
            ArrayList<FeaturesFilter> baseFilters = new ArrayList<>();
            baseFilters.add(new ContainerFeaturesFilter(getContainer(), form.isSubfolders(), getUser()));
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                baseFilters.add(new PeptideFilter(form.getPepSeq(), form.isExact()));
            if(null != form.getRunIds() && form.getRunIds().length() > 0)
                baseFilters.add(new RunFilter(form.getRunIds()));

            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer(),
                                            !(form.isSubfolders())), baseFilters, true);
            featuresView.setTitle("Matching MS1 Features");
            featuresView.setForExport(forExport);
            return featuresView;
        }

        protected PeptidesView getPeptidesView(PeptideFilterSearchForm form, BindException bindErrors, boolean forExport) throws Exception
        {
            //create the peptide search results view
            //get a peptides table so that we can get the public schema and query name for it
            TableInfo peptidesTable = MS2Service.get().createPeptidesTableInfo(getUser(), getContainer());
            PeptidesView pepView = new PeptidesView(MS2Service.get().createSchema(getUser(), getContainer()), peptidesTable.getPublicName());
            pepView.setSearchSubfolders(form.isSubfolders());
            if(null != form.getPepSeq() && form.getPepSeq().length() > 0)
                pepView.setPeptideFilter(new PeptideFilter(form.getPepSeq(), form.isExact()));
            pepView.setTitle("Matching MS2 Peptides");
            pepView.enableExpandCollapse("peptides", false);
            return pepView;
        }

        public ModelAndView getHtmlView(PeptideFilterSearchForm form, BindException errors) throws Exception
        {
            //create the search view
            PepSearchModel searchModel = new PepSearchModel(getContainer(), form.getPepSeq(),
                    form.isExact(), form.isSubfolders(), form.getRunIds());
            JspView<PepSearchModel> searchView = new JspView<>("/org/labkey/ms1/view/PepSearchView.jsp", searchModel);
            searchView.setTitle("Search Criteria");

            //if no search terms were specified, return just the search view
            if(searchModel.noSearchTerms())
            {
                searchModel.setErrorMsg("You must specify at least one Peptide Sequence");
                return searchView;
            }

            VBox result = new VBox(searchView);
            if (getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MS1Module.class)))
            {
                //create the features view
                FeaturesView featuresView = (FeaturesView)createInitializedQueryView(form, errors, false, FeaturesView.DATAREGION_NAME);
                featuresView.enableExpandCollapse("features", false);

                //create the peptide search results view
                PeptidesView pepView = (PeptidesView)createInitializedQueryView(form, errors, false, PeptidesView.DATAREGION_NAME);

                result.addView(featuresView);
                result.addView(pepView);
            }

            for (ProteinService.QueryViewProvider<ProteinService.PeptideSearchForm> viewProvider : ServiceRegistry.get().getService(ProteinService.class).getPeptideSearchViews())
            {
                QueryView queryView = viewProvider.createView(getViewContext(), form, errors);
                if (queryView != null)
                    result.addView(queryView);
            }

            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Peptide Search Results");
        }
    } //PepSearchAction

    public static class SimilarSearchForm extends QueryViewAction.QueryExportForm
    {
        public enum ParamNames
        {
            featureId,
            mzSource,
            timeSource,
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

        private Integer _featureId = null;
        private Double _mzSource = null;
        private Double _timeSource = null;
        private double _mzOffset = 5;
        private MzOffsetUnits _mzUnits = MzOffsetUnits.ppm;
        private double _timeOffset = 30;
        private TimeOffsetUnits _timeUnits = TimeOffsetUnits.rt;
        private boolean _subfolders = false;
        private Feature _feature = null;

        public Integer getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(Integer featureId)
        {
            _featureId = featureId;
        }

        public Feature getFeature()
        {
            if(null == _feature && null != _featureId)
                _feature = MS1Manager.get().getFeature(_featureId.intValue());
            return _feature;
        }

        public Double getMzSource()
        {
            if(null == _mzSource)
            {
                Feature feature = getFeature();
                if(null != feature)
                    _mzSource = feature.getMz();
            }

            return _mzSource;
        }

        public void setMzSource(Double mzSource)
        {
            _mzSource = mzSource;
        }

        public Double getTimeSource()
        {
            if(null == _timeSource)
            {
                Feature feature = getFeature();
                if(null != feature)
                    _timeSource = (_timeUnits == TimeOffsetUnits.rt ? feature.getTime() : new Double(feature.getScan().doubleValue()));
            }
            return _timeSource;
        }

        public void setTimeSource(Double timeSource)
        {
            _timeSource = timeSource;
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

        public boolean canSearch()
        {
            return _mzSource != null && _timeSource != null;
        }

    } //SimilarSearchForm

    /**
     * Action for finding features similar to a specified feature id
     */
    @RequiresPermission(ReadPermission.class)
    public class SimilarSearchAction extends BasicFeaturesViewAction<SimilarSearchForm>
    {
        public SimilarSearchAction()
        {
            super(SimilarSearchForm.class);
        }

        protected FeaturesView getFeaturesView(SimilarSearchForm form, BindException bindErrors, boolean forExport) throws Exception
        {
            ArrayList<FeaturesFilter> baseFilters = new ArrayList<>();
            baseFilters.add(new ContainerFeaturesFilter(getContainer(), form.isSubfolders(), getUser()));
            baseFilters.add(new MzFilter(form.getMzSource(), form.getMzOffset(), form.getMzUnits()));
            if(SimilarSearchForm.TimeOffsetUnits.rt == form.getTimeUnits())
                baseFilters.add(new RetentionTimeFilter(form.getTimeSource() - form.getTimeOffset(),
                        form.getTimeSource() + form.getTimeOffset()));
            else
                baseFilters.add(new ScanFilter((int)form.getTimeSource().doubleValue() - (int)form.getTimeOffset(),
                        (int)form.getTimeSource().doubleValue() + (int)(form.getTimeOffset())));

            FeaturesView featuresView = new FeaturesView(new MS1Schema(getUser(), getContainer(),
                                            !(form.isSubfolders())), baseFilters, true);
            featuresView.setTitle("Search Results");
            featuresView.setForExport(forExport);
            return featuresView;
        }

        public ModelAndView getHtmlView(SimilarSearchForm form, BindException errors) throws Exception
        {
            SimilarSearchModel searchModel = new SimilarSearchModel(getContainer(), form.getFeature(),
                    form.getMzSource(), form.getTimeSource(),
                    form.getMzOffset(), form.getMzUnits(),
                    form.getTimeOffset(), form.getTimeUnits(),
                    form.isSubfolders());
            JspView<SimilarSearchModel> searchView = new JspView<>("/org/labkey/ms1/view/SimilarSearchView.jsp", searchModel);
            searchView.setTitle("Find Features Where");

            //if we don't have enough search info, just return the search view
            if(!form.canSearch())
                return searchView;

            //create the features view
            FeaturesView featuresView = createInitializedQueryView(form, errors, false, null);

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

    @RequiresPermission(ReadPermission.class)
    public class CompareRunsSetupAction extends SimpleViewAction<CompareRunsSetupForm>
    {
        public ModelAndView getView(CompareRunsSetupForm compareRunsSetupForm, BindException errors) throws Exception
        {
            Set<String> selectedRuns = DataRegionSelection.getSelected(getViewContext(), true);
            if (selectedRuns.size() < 1)
                return HttpView.redirect(new BeginAction(getViewContext()).getUrl().getLocalURIString());

            ActionURL url = new ActionURL(CompareRunsAction.class, getContainer());
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

    @RequiresPermission(ReadPermission.class)
    public class CompareRunsAction extends QueryViewAction<CompareRunsForm,CompareRunsView>
    {
        public CompareRunsAction()
        {
            super(CompareRunsForm.class);
        }

        protected CompareRunsView createQueryView(CompareRunsForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            return new CompareRunsView(new MS1Schema(getUser(), getContainer()), form.getRunIdArray(), getViewContext().getActionURL());
        }

        protected ModelAndView getHtmlView(CompareRunsForm form, BindException errors) throws Exception
        {
            Map<String, String> props = new HashMap<>();
            props.put("originalURL", getViewContext().getActionURL().toString());
            props.put("comparisonName", MS1VennDiagramView.FEATURES_BY_PEPTIDE);
            GWTView gwtView = new GWTView(org.labkey.ms1.client.MS1VennDiagramView.class, props);
            gwtView.setTitle("Comparison Overview");
            gwtView.setFrame(WebPartView.FrameType.PORTAL);
            gwtView.enableExpandCollapse(MS1VennDiagramView.FEATURES_BY_PEPTIDE + "Overview", true);

            CompareRunsView queryView = (CompareRunsView)super.getHtmlView(form, errors);
            queryView.setTitle("Comparison Details");
            queryView.setFrame(WebPartView.FrameType.PORTAL);
            queryView.enableExpandCollapse(MS1VennDiagramView.FEATURES_BY_PEPTIDE + "Details", false);
            return new VBox(gwtView, queryView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare Runs");
        }
    }

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
            return _srcUrl == null ? null : new ActionURL(_srcUrl);
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

    public static class PeaksViewForm extends QueryViewAction.QueryExportForm
    {
        public enum ParamNames
        {
            featureId,
            scanFirst,
            scanLast
        }

        private int _featureId = -1;
        private Integer _scanFirst = null;
        private Integer _scanLast = null;
        private Feature _feature = null;
        private ExpRun _expRun = null;

        public int getFeatureId()
        {
            return _featureId;
        }

        public void setFeatureId(int featureId)
        {
            _featureId = featureId;
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

        public Feature getFeature() throws SQLException
        {
            if(null == _feature && _featureId >= 0)
                _feature = MS1Manager.get().getFeature(_featureId);
            return _feature;
        }

        public ExpRun getExpRun() throws SQLException
        {
            if(null == _expRun)
            {
                Feature feature = getFeature();
                if(null != feature)
                    _expRun = feature.getExpRun();
            }
            return _expRun;
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
                url.addParameter(ProteinService.PeptideSearchForm.ParamNames.pepSeq.name(), sequence);
            return url;
        }
    }

    public static String createVerifySelectedScript(DataView view, ActionURL url)
    {
        //copied from MS2Controller--perhaps we should move this to API?
        return "javascript: if (verifySelected(" + view.getDataRegion().getJavascriptFormReference() + ", '" + url.getLocalURIString() + "', 'post', 'runs')) { " + view.getDataRegion().getJavascriptFormReference() + ".submit(); }";
    }

    @RequiresPermission(ReadPermission.class)
    public class CompareServiceAction extends GWTServiceAction
    {
        protected BaseRemoteService createService()
        {
            return new CompareServiceImpl(getViewContext());
        }
    }
} //class MS1Controller