/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.module.Module;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryParseException;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.AdminConsoleAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AbstractActionPermissionTest;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.settings.AdminConsole.SettingsLinkType;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.TestContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.FlowModule;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.webparts.OverviewWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.URI;

@Marshal(Marshaller.Jackson)
public class FlowController extends BaseFlowController
{
    private static final Logger _log = LogManager.getLogger(FlowController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(FlowController.class);

    public FlowController()
    {
        setActionResolver(_actionResolver);
    }

    public static void registerAdminConsoleLinks()
    {
        AdminConsole.addLink(SettingsLinkType.Premium, "flow cytometry", new ActionURL(FlowAdminAction.class, ContainerManager.getRoot()), AdminOperationsPermission.class);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            if (getContainer().getFolderType() instanceof FlowFolderType)
            {
                ActionURL startUrl = urlProvider(ProjectUrls.class).getStartURL(getContainer());
                startUrl.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
                throw new RedirectException(startUrl);
            }

            return new OverviewWebPart(getViewContext());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            getFlowNavStart(getPageConfig(), getViewContext());
        }
    }


    // HACK: FlowPropertySet can be very slow the first time, let's attempt to precache
    public static void initFlow(final Container c)
    {
        JobRunner.getDefault().execute(() -> {
            AttributeCache.STATS.byContainer(c);
            AttributeCache.GRAPHS.byContainer(c);
            AttributeCache.KEYWORDS.byContainer(c);
        });
    }


    @RequiresPermission(ReadPermission.class)
    public class QueryAction extends SimpleViewAction
    {
        String query;
        FlowExperiment experiment;
        FlowRun run;

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            FlowQuerySettings settings = new FlowQuerySettings(getViewContext().getBindPropertyValues(), "query");
            query = settings.getQueryName();
            if (null == query)
            {
                throw new NotFoundException("Query name required.");
            }
            FlowSchema schema = new FlowSchema(getViewContext());
            QueryDefinition queryDef = settings.getQueryDef(schema);

            if (queryDef == null)
            {
                throw new NotFoundException("Query definition '" + settings.getQueryName() + "' in flow schema not found");
            }
            try
            {
                if (schema.getTable(settings.getQueryName(), false) == null)
                {
                    throw new NotFoundException("Query name '" + settings.getQueryName() + "' in flow schema not found");
                }
            }
            catch (QueryParseException qpe)
            {
                throw new NotFoundException(qpe.getMessage());
            }

            experiment = schema.getExperiment();
            run = schema.getRun();
//            script = schema.getScript();

            QueryView view = schema.createView(getViewContext(), settings);
            if (view.getQueryDef() == null)
            {
                throw new NotFoundException("Query definition '" + settings.getQueryName() + "' in flow schema not found");
            }
            if (view.getTable() == null)
            {
                throw new NotFoundException("Query table '" + settings.getQueryName() + "' in flow schema not found");
            }
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (experiment == null)
                root.addChild("All Analysis Folders", getActionURL());
            else
                root.addChild(experiment.getLabel(), getActionURL());

            if (run != null)
                root.addChild(run.getLabel(), run.urlShow());

            root.addChild(query);
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class NewFolderAction extends FormViewAction<NewFolderForm>
    {
        Container destContainer;

        @Override
        public void validateCommand(NewFolderForm form, Errors errors)
        {
        }

        private void checkPerms() throws UnauthorizedException
        {
            if (getContainer().getParent() == null || getContainer().getParent().isRoot())
            {
                throw new UnauthorizedException();
            }
            if (!getContainer().getParent().hasPermission(getUser(), AdminPermission.class))
            {
                throw new UnauthorizedException();
            }
        }

        @Override
        public ModelAndView getView(NewFolderForm form, boolean reshow, BindException errors)
        {
            checkPerms();
            getPageConfig().setFocusId("folderName");
            return new JspView<>("/org/labkey/flow/controllers/newFolder.jsp", form, errors);
        }

        @Override
        public boolean handlePost(NewFolderForm form, BindException errors) throws Exception
        {
            checkPerms();
            Container parent = getContainer().getParent();

            String folderName = StringUtils.trimToNull(form.getFolderName());
            StringBuilder error = new StringBuilder();
            if (!Container.isLegalName(folderName, parent.isRoot(), error))
            {
                errors.rejectValue("folderName", ERROR_MSG, error.toString());
                return false;
            }
            if (parent.hasChild(folderName))
            {
                errors.rejectValue("folderName", ERROR_MSG, "There is already a folder with the name '" + folderName + "'");
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

            destContainer = ContainerManager.createContainer(parent, folderName);
            destContainer.setActiveModules(getContainer().getActiveModules(), getUser());
            destContainer.setFolderType(getContainer().getFolderType(), getUser());
            destContainer.setDefaultModule(flowModule);
            FlowProtocol srcProtocol = FlowProtocol.getForContainer(getContainer());
            if (srcProtocol != null)
            {
                if (form.isCopyProtocol())
                {
                    FlowProtocol destProtocol = FlowProtocol.ensureForContainer(getUser(), destContainer);
                    destProtocol.setFCSAnalysisNameExpr(getUser(), srcProtocol.getFCSAnalysisNameExpr());
                    destProtocol.setSampleTypeJoinFields(getUser(), srcProtocol.getSampleTypeJoinFields());
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

        @Override
        public ActionURL getSuccessURL(NewFolderForm newFolderForm)
        {
            return urlProvider(ProjectUrls.class).getStartURL(destContainer);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("New Folder", new ActionURL(NewFolderAction.class, getContainer()));
        }
    }

    @AdminConsoleAction
    @RequiresPermission(AdminOperationsPermission.class)
    public class FlowAdminAction extends FormViewAction<FlowAdminForm>
    {
        @Override
        public void validateCommand(FlowAdminForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(FlowAdminForm form, boolean reshow, BindException errors)
        {
            getPageConfig().setFocusId("workingDirectory");
            return new JspView<>("/org/labkey/flow/controllers/flowAdmin.jsp", form, errors);
        }

        @Override
        public boolean handlePost(FlowAdminForm form, BindException errors)
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
                FlowSettings.setDeleteFiles(form.isDeleteFiles());
            }
            catch (Exception e)
            {
                errors.reject(ERROR_MSG, "An exception occurred: " + e);
                _log.error("Error", e);
                return false;
            }
            return true;
        }

        @Override
        public ActionURL getSuccessURL(FlowAdminForm flowAdminForm)
        {
            return urlProvider(AdminUrls.class).getAdminConsoleURL();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            urlProvider(AdminUrls.class).addAdminNavTrail(root, "Flow Module Settings", getClass(), getContainer());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class SavePreferencesAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            FlowPreference.update(getRequest());
            URI uri = new URI(getRequest().getContextPath() + "/_.gif");
            return HttpView.redirect(uri.toString());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    // redirect to appropriate download URL
    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends SimpleViewAction<FlowDataObjectForm>
    {
        @Override
        public ModelAndView getView(FlowDataObjectForm form, BindException errors)
        {
            FlowDataObject fdo = form.getFlowObject();
            if (fdo == null)
                throw new NotFoundException();

            checkContainer(fdo);

            throw new RedirectException(fdo.urlDownload());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class MetricsAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            Container c = getContainer();
            if (c.isRoot())
            {
                return success(FlowManager.get().getUsageMetrics());
            }
            else
            {
                return success(FlowManager.get().getUsageMetrics(getUser(), c, true));
            }
        }
    }

    public static class TestCase extends AbstractActionPermissionTest
    {
        @Override
        public void testActionPermissions()
        {
            User user = TestContext.get().getUser();
            assertTrue(user.hasSiteAdminPermission());

            FlowController controller = new FlowController();

            // @RequiresPermission(ReadPermission.class)
            assertForReadPermission(user,
                controller.new BeginAction(),
                controller.new QueryAction(),
                controller.new SavePreferencesAction()
            );

            // @RequiresPermission(AdminPermission.class)
            assertForAdminPermission(user,
                controller.new NewFolderAction()
            );

            // @AdminConsoleAction
            // @RequiresPermission(AdminOperationsPermission.class)
            assertForAdminOperationsPermission(ContainerManager.getRoot(), user,
                controller.new FlowAdminAction()
            );
        }
    }
}
