/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;
import org.labkey.flow.script.*;
import org.labkey.flow.view.JobStatusView;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.webparts.OverviewWebPart;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowScript;
import org.labkey.api.security.ACL;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.module.Module;
import org.labkey.api.jsp.FormPage;

import java.util.*;
import java.io.File;
import java.net.URI;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class FlowController extends BaseFlowController<FlowController.Action>
{
    private static Logger _log = Logger.getLogger(FlowController.class);

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

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        if (getContainer().getFolderType() instanceof FlowFolderType)
        {
            ActionURL forward = new ActionURL("Project", "begin", getContainer());
            forward.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
            return new ViewForward(forward);
        }
        requiresPermission(ACL.PERM_READ);
        return includeView(new HomeTemplate(getViewContext(), new OverviewWebPart(getViewContext()), getNavTrailConfig(null, FlowModule.getLongProductName(), Action.begin)));
    }

    @Jpf.Action
    protected Forward showStatusJob() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        String statusFile = getParam(FlowParam.statusFile);
        if (statusFile == null)
        {
            return renderError("Status file not specified.");
        }
        PipelineStatusFile psf = PipelineService.get().getStatusFile(statusFile);
        ScriptJob job = findJob(statusFile);

        if (PipelineJob.COMPLETE_STATUS.equals(psf.getStatus()))
        {
            if (getParam(FlowParam.redirect) != null)
            {
                String redirect = psf.getDataUrl();
                if (redirect != null)
                {
                    return new ViewForward(new ActionURL(psf.getDataUrl()));
                }
            }
        }
        if (job != null && !job.isComplete())
        {
            // Take 1 second longer each time to refresh.
            int refresh = getIntParam(FlowParam.refresh);
            if (refresh == 0)
            {
                if (getParam(FlowParam.redirect) == null)
                    refresh = 30;
                else
                    refresh = 1;
            }
            else
            {
                refresh ++;
            }
            ActionURL helper = cloneActionURL();
            helper.replaceParameter("refresh", Integer.toString(refresh));
            helper.setFragment("end");
            getResponse().setHeader("Refresh", refresh + ";URL=" + helper.toString());
        }
        HttpView view = new JobStatusView(psf, job);
        return includeView(new HomeTemplate(getViewContext(), view, getNavTrailConfig(null, "Status File", Action.showStatusFile)));
    }

    protected Forward renderError(String error) throws Exception
    {
        return renderErrors(Arrays.asList(new String[]{error}));
    }

    private Forward renderErrors(List errors) throws Exception
    {
        StringBuffer html = new StringBuffer();

        html.append(StringUtils.join(errors.iterator(), "<br>"));

        HttpView view = new HtmlView(html.toString());
        return includeView(new HomeTemplate(getViewContext(), view, getNavTrailConfig(null, "error", Action.errors)));
    }


    public ViewContext getViewContext()
    {
        return super.getViewContext();
    }

    @Jpf.Action
    protected Forward showJobs(ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        return renderInTemplate(FormPage.getView(FlowController.class, form, "runningJobs.jsp"), getContainer(), "Running Flow Jobs");
    }

    ScriptJob findJob(String statusFile) throws Exception
    {
        PipelineService service = PipelineService.get();
        PipelineJob job = service.getPipelineQueue().findJob(getContainer(), statusFile);
        if (job instanceof ScriptJob)
            return (ScriptJob) job;
        return null;
    }

    @Jpf.Action
    protected Forward cancelJob() throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        String statusFile = getParam(FlowParam.statusFile);
        ScriptJob job = findJob(statusFile);
        if (job == null)
        {
            return renderError("Job " + statusFile + " not found.");
        }
        PipelineService service = PipelineService.get();
        service.getPipelineQueue().cancelJob(getContainer(), job.getJobId());
        return new ViewForward(job.getStatusHref());
    }

    @Jpf.Action
    protected Forward newFolder(NewFolderForm form) throws Exception
    {
        requiresAdmin();
        if (getContainer().getParent() == null || getContainer().getParent().isRoot())
            HttpView.throwUnauthorized();
        if (!getContainer().getParent().hasPermission(getUser(), ACL.PERM_ADMIN))
        {
            HttpView.throwUnauthorized();
        }
        if (isPost())
        {
            Forward forward = doNewFolder(form);
            if (forward != null)
            {
                return forward;
            }
        }
        HomeTemplate template = new HomeTemplate(getViewContext(), getContainer(), FormPage.getView(FlowController.class, form, "newFolder.jsp"), getNavTrailConfig(null, "New Folder", Action.newFolder));
        template.getModelBean().setFocus("forms[0].ff_folderName");
        return includeView(template);
    }

    protected Forward doNewFolder(NewFolderForm form) throws Exception
    {
        Container parent = getContainer().getParent();

        if (parent.hasChild(form.ff_folderName))
        {
            addError("There is already a folder with the name '" + form.ff_folderName + "'");
            return null;
        }
        StringBuffer error = new StringBuffer();
        if (!Container.isLegalName(form.ff_folderName, error))
        {
            addError(error.toString());
            return null;
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
            addError("A new folder cannot be created because the flow module is not active.");
            return null;
        }

        Container destContainer = ContainerManager.createContainer(parent, form.ff_folderName);
        destContainer.setActiveModules(getContainer().getActiveModules());
        destContainer.setFolderType(getContainer().getFolderType());
        destContainer.setDefaultModule(flowModule);
        FlowProtocol srcProtocol = FlowProtocol.getForContainer(getContainer());
        if (srcProtocol != null)
        {
            if (form.ff_copyProtocol)
            {
                FlowProtocol destProtocol = FlowProtocol.ensureForContainer(getUser(), destContainer);
                destProtocol.setFCSAnalysisNameExpr(getUser(), srcProtocol.getFCSAnalysisNameExpr());
                destProtocol.setSampleSetJoinFields(getUser(), srcProtocol.getSampleSetJoinFields());
                destProtocol.setFCSAnalysisFilter(getUser(), srcProtocol.getFCSAnalysisFilterString());
            }
            for (String analysisScriptName : form.ff_copyAnalysisScript)
            {
                FlowScript srcScript = FlowScript.fromName(getContainer(), analysisScriptName);
                if (srcScript != null)
                {
                    FlowScript.create(getUser(), destContainer, srcScript.getName(), srcScript.getAnalysisScript());
                }
            }
        }
        ActionURL forward = PageFlowUtil.urlFor(Action.begin, destContainer);
        return new ViewForward(forward);
    }


    protected boolean addError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionError("Error", error));
        return true;
    }

    @Jpf.Action
    protected Forward flowAdmin(FlowAdminForm form) throws Exception
    {
        requiresAdmin(ContainerManager.getRoot().getId());
        if (isPost())
        {
            Forward forward = updateFlowSettings(form);
            if (forward != null)
                return forward;
        }
        return renderInTemplate(FormPage.getView(FlowController.class, form, "flowAdmin.jsp"), ContainerManager.getRoot(), "Flow Module Settings");
    }

    protected Forward updateFlowSettings(FlowAdminForm form)
    {
        boolean errors = false;
        if (form.ff_workingDirectory != null)
        {
            File dir = new File(form.ff_workingDirectory);
            if (!dir.exists())
            {
                errors = addError("Path does not exist.");
                return null;
            }
            if (!dir.isDirectory())
            {
                errors = addError("Path is not a directory.");
                return null;
            }
        }
        try
        {
            FlowSettings.setWorkingDirectoryPath(form.ff_workingDirectory);
        }
        catch (Exception e)
        {
            errors = addError("An exception occurred:" + e);
            _log.error("Error", e);
        }
        if (errors)
        {
            return null;
        }
        return new ViewForward(new ActionURL("admin", "begin", ""));
    }

    @Jpf.Action
    protected Forward savePreferences() throws Exception
    {
        FlowPreference.update(getRequest());
        return new Forward(new URI(getRequest().getContextPath() + "/_.gif"));
    }
}
