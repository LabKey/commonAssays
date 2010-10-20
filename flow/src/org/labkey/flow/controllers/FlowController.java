/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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
package org.labkey.flow.controllers;

import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryParseException;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.settings.AdminConsole.SettingsLinkType;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.flow.FlowModule;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.query.AttributeCache;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.script.FlowJob;
import org.labkey.flow.view.JobStatusView;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.webparts.OverviewWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.URI;

public class FlowController extends BaseFlowController<FlowController.Action>
{
    private static final Logger _log = Logger.getLogger(FlowController.class);

    public enum Action
    {
        begin,
        cancelJob,
        showJobs,
        showStatusJob,
        executeQuery,
        showStatusFile,
        flowAdmin,
        newFolder,
        errors,
        setFlag,
        savePreferences
    }

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(FlowController.class);

    public FlowController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    public static void registerAdminConsoleLinks()
    {
        AdminConsole.addLink(SettingsLinkType.Configuration, "flow cytometry", new ActionURL(FlowAdminAction.class, ContainerManager.getRoot()));
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            if (getContainer().getFolderType() instanceof FlowFolderType)
            {
                ActionURL startUrl = PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(getContainer());
                startUrl.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
                HttpView.throwRedirect(startUrl);
            }
            HttpView v = new OverviewWebPart(getViewContext());
            return v;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return getFlowNavStart(getPageConfig(), getViewContext());
        }
    }


    // HACK: FlowPropertySet can be very slow the first time, let's attempt to precache
    public static void initFlow(final Container c)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                AttributeCache.STATS.getAttrValues(c, null);
                AttributeCache.GRAPHS.getAttrValues(c, null);
                AttributeCache.KEYWORDS.getAttrValues(c, null);
            }
        };
        JobRunner.getDefault().execute(r);
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class QueryAction extends SimpleViewAction
    {
        String query;
        FlowExperiment experiment;
//        FlowScript script;
        FlowRun run;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            FlowQuerySettings settings = new FlowQuerySettings(getViewContext().getBindPropertyValues(), "query");
            query = settings.getQueryName();
            if (null == query)
            {
                HttpView.throwNotFound("Query name required.");
                return null;
            }
            FlowSchema schema = new FlowSchema(getViewContext());
            QueryDefinition queryDef = settings.getQueryDef(schema);

            if (queryDef == null)
            {
                HttpView.throwNotFound("Query definition '" + settings.getQueryName() + "' in flow schema not found");
            }
            try
            {
                if (schema.getTable(settings.getQueryName(), false) == null)
                    HttpView.throwNotFound("Query name '" + settings.getQueryName() + "' in flow schema not found");
            }
            catch (QueryParseException qpe)
            {
                HttpView.throwNotFound(qpe.getMessage());
            }

            experiment = schema.getExperiment();
            run = schema.getRun();
//            script = schema.getScript();

            QueryView view = schema.createView(getViewContext(), settings);
            if (view.getQueryDef() == null)
            {
                HttpView.throwNotFound("Query definition '" + settings.getQueryName() + "' in flow schema not found");
            }
            if (view.getTable() == null)
            {
                HttpView.throwNotFound("Query table '" + settings.getQueryName() + "' in flow schema not found");
            }
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (experiment == null)
                root.addChild("All Analysis Folders", new ActionURL(QueryAction.class, getContainer()));
            else
                root.addChild(experiment.getLabel(), experiment.urlShow());

//            if (script != null)
//                root.addChild(script.getLabel(), script.urlShow());

            if (run != null)
                root.addChild(run.getLabel(), run.urlShow());

            root.addChild(query);
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowStatusJobAction extends SimpleViewAction<StatusJobForm>
    {
        public void validate(StatusJobForm form, BindException errors)
        {
            String statusFile = form.getStatusFile();
            if (statusFile == null)
            {
                errors.rejectValue("statusFile", ERROR_MSG, "Status file not specified.");
            }
        }

        public ModelAndView getView(StatusJobForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
            {
                return new JspView<StatusJobForm>("/org/labkey/flow/view/errors.jsp", form, errors);
            }
            else
            {
                PipelineStatusFile psf = PipelineService.get().getStatusFile(form.getStatusFile());
                if (psf == null)
                {
                    errors.rejectValue("statusFile", ERROR_MSG, "Status not found.");
                    return new JspView<StatusJobForm>("/org/labkey/flow/view/errors.jsp", form, errors);
                }

                if (PipelineJob.COMPLETE_STATUS.equals(psf.getStatus()))
                {
                    if (form.getRedirect() != null)
                    {
                        String redirect = psf.getDataUrl();
                        if (redirect != null)
                        {
                            HttpView.throwRedirect(new ActionURL(psf.getDataUrl()));
                        }
                    }
                }

                if (psf.isActive())
                {
                    // Take 1 second longer each time to refresh.
                    int refresh = form.getRefresh();
                    if (refresh == 0)
                    {
                        if (form.getRedirect() == null)
                            refresh = 30;
                        else
                            refresh = 1;
                    }
                    else
                    {
                        refresh++;
                    }
                    ActionURL helper = getViewContext().cloneActionURL();
                    helper.replaceParameter("refresh", Integer.toString(refresh));
                    helper.setFragment("end");
                    getViewContext().getResponse().setHeader("Refresh", refresh + ";URL=" + helper.toString());
                }
                return new JobStatusView(psf, findJob(form.getStatusFile()));
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Status File", new ActionURL(ShowStatusJobAction.class, getContainer()));
            return root;
        }

    }

    public static class StatusJobForm
    {
        private String _statusFile;
        private String _redirect;
        private int _refresh;

        public String getStatusFile()
        {
            return _statusFile;
        }

        public void setStatusFile(String statusFile)
        {
            _statusFile = statusFile;
        }

        public String getRedirect()
        {
            return _redirect;
        }

        public void setRedirect(String redirect)
        {
            _redirect = redirect;
        }

        public int getRefresh()
        {
            return _refresh;
        }

        public void setRefresh(int refresh)
        {
            _refresh = refresh;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class ShowJobsAction extends SimpleViewAction<Object>
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView<Object>(FlowController.class, "runningJobs.jsp", null, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Running Flow Jobs", new ActionURL(ShowJobsAction.class, getContainer()));
            return root;
        }
    }

    FlowJob findJob(String statusFile) throws Exception
    {
        PipelineService service = PipelineService.get();
        try
        {
            PipelineJob job = service.getPipelineQueue().findJobInMemory(getContainer(), statusFile);
            if (job instanceof FlowJob)
                return (FlowJob) job;
        }
        catch (UnsupportedOperationException e)
        {
            // Enterprise pipeline does not have this information
            // in memory.
        }
        return null;
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class CancelJobAction extends SimpleViewAction<CancelJobForm>
    {
        public ModelAndView getView(CancelJobForm form, BindException errors) throws Exception
        {
            if (form.getStatusFile() == null)
            {
                errors.rejectValue("statusFile", ERROR_MSG, "Job " + form.getStatusFile() + " not found.");
            }
            else
            {
                PipelineStatusFile sf = PipelineService.get().getStatusFile(form.getStatusFile());
                if (sf == null)
                {
                    errors.rejectValue("statusFile", ERROR_MSG, "Job " + form.getStatusFile() + " not found.");
                }
                else
                {
                    Container c = getContainer();
                    PipelineService.get().getPipelineQueue().cancelJob(c, sf.getJobId());

                    // Attempting to stay consistent with previously existing code, create a FlowJob
                    // from the job store, and go to its status href.  This URL is not set in the
                    // PipelineStatusFile until the job completes.  Apparently FlowJobs have useful
                    // information to show at this URL, even when the job has not completed.
                    FlowJob job = (FlowJob) PipelineJobService.get().getJobStore().getJob(sf.getJobId());
                    if (job != null)
                        return HttpView.redirect(job.getStatusHref());
                    else if (sf.getDataUrl() != null)
                        return HttpView.redirect(sf.getDataUrl());
                    errors.rejectValue("statusFile", ERROR_MSG, "Data for " + form.getStatusFile() + " not found.");
                }
            }
            return new JspView<CancelJobForm>("/org/labkey/flow/view/errors.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Error Cancelling Job", new ActionURL(ShowJobsAction.class, getContainer()));
            return root;
        }
    }

    public static class CancelJobForm
    {
        private String _statusFile;

        public String getStatusFile()
        {
            return _statusFile;
        }

        public void setStatusFile(String statusFile)
        {
            _statusFile = statusFile;
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class NewFolderAction extends FormViewAction<NewFolderForm>
    {
        Container destContainer;

        public void validateCommand(NewFolderForm form, Errors errors)
        {
        }

        private void checkPerms() throws UnauthorizedException
        {
            if (getContainer().getParent() == null || getContainer().getParent().isRoot())
                HttpView.throwUnauthorized();
            if (!getContainer().getParent().hasPermission(getUser(), AdminPermission.class))
            {
                HttpView.throwUnauthorized();
            }
        }

        public ModelAndView getView(NewFolderForm form, boolean reshow, BindException errors) throws Exception
        {
            checkPerms();
            getPageConfig().setFocusId("folderName");
            return new JspView<NewFolderForm>(FlowController.class, "newFolder.jsp", form, errors);
        }

        public boolean handlePost(NewFolderForm form, BindException errors) throws Exception
        {
            checkPerms();
            Container parent = getContainer().getParent();

            if (parent.hasChild(form.getFolderName()))
            {
                errors.rejectValue("folderName", ERROR_MSG, "There is already a folder with the name '" + form.getFolderName() + "'");
                return false;
            }
            StringBuilder error = new StringBuilder();
            if (!Container.isLegalName(form.getFolderName(), error))
            {
                errors.rejectValue("folderName", ERROR_MSG, error.toString());
                return false;
            }
            FlowModule flowModule = null;
            for (Module module : getContainer().getActiveModules())
            {
                if (module instanceof FlowModule)
                {
                    flowModule = (FlowModule) module;
                }
            }
            if (flowModule == null)
            {
                errors.reject(ERROR_MSG, "A new folder cannot be created because the flow module is not active.");
                return false;
            }

            destContainer = ContainerManager.createContainer(parent, form.getFolderName());
            destContainer.setActiveModules(getContainer().getActiveModules());
            destContainer.setFolderType(getContainer().getFolderType());
            destContainer.setDefaultModule(flowModule);
            FlowProtocol srcProtocol = FlowProtocol.getForContainer(getContainer());
            if (srcProtocol != null)
            {
                if (form.isCopyProtocol())
                {
                    FlowProtocol destProtocol = FlowProtocol.ensureForContainer(getUser(), destContainer);
                    destProtocol.setFCSAnalysisNameExpr(getUser(), srcProtocol.getFCSAnalysisNameExpr());
                    destProtocol.setSampleSetJoinFields(getUser(), srcProtocol.getSampleSetJoinFields());
                    destProtocol.setFCSAnalysisFilter(getUser(), srcProtocol.getFCSAnalysisFilterString());
                    destProtocol.setICSMetadata(getUser(), srcProtocol.getICSMetadataString());
                }
                for (String analysisScriptName : form.getCopyAnalysisScript())
                {
                    FlowScript srcScript = FlowScript.fromName(getContainer(), analysisScriptName);
                    if (srcScript != null)
                    {
                        FlowScript.create(getUser(), destContainer, srcScript.getName(), srcScript.getAnalysisScript());
                    }
                }
            }
            return true;
        }

        public ActionURL getSuccessURL(NewFolderForm newFolderForm)
        {
            return PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(destContainer);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("New Folder", new ActionURL(NewFolderAction.class, getContainer()));
            return root;
        }
    }

    @RequiresSiteAdmin
    public class FlowAdminAction extends FormViewAction<FlowAdminForm>
    {
        public void validateCommand(FlowAdminForm target, Errors errors)
        {
        }

        public ModelAndView getView(FlowAdminForm form, boolean reshow, BindException errors) throws Exception
        {
            getPageConfig().setFocusId("workingDirectory");
            return new JspView<FlowAdminForm>(FlowController.class, "flowAdmin.jsp", form, errors);
        }

        public boolean handlePost(FlowAdminForm form, BindException errors) throws Exception
        {
            if (form.getWorkingDirectory() != null)
            {
                File dir = new File(form.getWorkingDirectory());
                if (!dir.exists())
                {
                    errors.rejectValue("workingDirectory", ERROR_MSG, "Path does not exist: " + form.getWorkingDirectory());
                    return false;
                }
                if (!dir.isDirectory())
                {
                    errors.rejectValue("workingDirectory", ERROR_MSG, "Path is not a directory: " + form.getWorkingDirectory());
                    return false;
                }
            }
            try
            {
                FlowSettings.setWorkingDirectoryPath(form.getWorkingDirectory());
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, "An exception occurred:" + e);
                _log.error("Error", e);
                return false;
            }
            return true;
        }

        public ActionURL getSuccessURL(FlowAdminForm flowAdminForm)
        {
            return PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Flow Module Settings");
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SavePerferencesAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            FlowPreference.update(getRequest());
            URI uri = new URI(getRequest().getContextPath() + "/_.gif");
            return HttpView.redirect(uri.toString());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

}
