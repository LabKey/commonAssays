/*
 * Copyright (c) 2009-2019 LabKey Corporation
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.FormApiAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.reports.Report;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.DbReportIdentifier;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.ReportIdentifier;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.reports.FilterFlowReport;
import org.labkey.flow.reports.FlowReport;
import org.labkey.flow.reports.FlowReportJob;
import org.labkey.flow.reports.FlowReportManager;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 5:15:39 PM
 */
public class ReportsController extends BaseFlowController
{
    private static final Logger _log = LogManager.getLogger(ReportsController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ReportsController.class);

    public ReportsController()
    { 
        setActionResolver(_actionResolver);
    }


    public static class BeginView extends JspView
    {
        public BeginView()
        {
            super("/org/labkey/flow/controllers/reports.jsp");
            setTitle("Flow Reports");
            setTitleHref(new ActionURL(BeginAction.class, getViewContext().getContainer()));
        }
    }


    @RequiresPermission(ReadPermission.class)
    public static class BeginAction extends SimpleViewAction
    {
        public BeginAction()
        {
        }

        public BeginAction(ViewContext context)
        {
            setViewContext(context);
        }
        
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            return new BeginView();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Reports", new ActionURL(BeginAction.class, getContainer()));
        }
    }


    public static class CreateReportForm extends ReturnUrlForm
    {
        private String _reportType;

        public String getReportType()
        {
            return _reportType;
        }

        public void setReportType(String reportType)
        {
            _reportType = reportType;
        }
    }

    private abstract static class CreateOrUpdateAction<FORM extends ReturnUrlForm> extends FormApiAction<FORM>
    {
        FlowReport r;

        @Override
        public void validateForm(FORM form, Errors errors)
        {
            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            if (protocol == null)
                errors.reject(ERROR_MSG, "No flow protocol in this container.  Please upload FCS files to create a flow protocol.");
        }

        public abstract void initReport(FORM form);

        @Override
        protected String getCommandClassMethodName()
        {
            return "initReport";
        }

        @Override
        public ModelAndView getView(FORM form, BindException errors)
        {
            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            if (protocol == null)
                return new SimpleErrorView(errors);

            initReport(form);
            return r.getConfigureForm(getViewContext(), form.getReturnActionURL(), form.getCancelActionURL());
        }

        @Override
        public ApiResponse execute(FORM form, BindException errors)
        {
            initReport(form);
            r.updateProperties(getViewContext(), getPropertyValues(), errors, false);
            if (errors.hasErrors())
                return null;

            int id = ReportService.get().saveReport(getViewContext(), null, r);
            ReportIdentifier dbid = r.getReportId();
            if (null == dbid)
                dbid = new DbReportIdentifier(id);
            ApiSimpleResponse ret = new ApiSimpleResponse(r.getDescriptor().getProperties());
            ret.put("reportId", dbid.toString());
            ret.put("success",Boolean.TRUE);
            return ret;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public static class CreateAction extends CreateOrUpdateAction<CreateReportForm>
    {
        @Override
        public void initReport(CreateReportForm form)
        {
            r = createReport(form.getReportType());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).addNavTrail(root);
            if (r != null)
                root.addChild("Create new report: " + r.getTypeDescription());
        }
    }
    
    @RequiresPermission(UpdatePermission.class)
    public static class UpdateAction extends CreateOrUpdateAction<IdForm>
    {
        @Override
        public void initReport(IdForm form)
        {
            r = getReport(getViewContext(), form);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).addNavTrail(root);
            if (r != null)
                root.addChild("Edit report: " + r.getDescriptor().getReportName());
        }
    }

    public static class CopyForm extends IdForm
    {
        private String _reportName;

        public String getReportName()
        {
            return _reportName;
        }

        public void setReportName(String reportName)
        {
            _reportName = reportName;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public static class CopyAction extends FormViewAction<CopyForm>
    {
        FlowReport r;

        @Override
        public void validateCommand(CopyForm form, Errors errors)
        {
            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            if (protocol == null)
            {
                errors.reject(ERROR_MSG, "No flow protocol in this container.  Please upload FCS files to create a flow protocol.");
                return;
            }

            if (form.getReportName() == null || form.getReportName().length() == 0)
            {
                errors.rejectValue("reportName", ERROR_MSG, "Report name must not be empty");
                return;
            }

            Collection<FlowReport> reports = FlowReportManager.getFlowReports(getContainer(), getUser());
            for (FlowReport report : reports)
            {
                if (form.getReportName().equalsIgnoreCase(report.getDescriptor().getReportName()))
                {
                    errors.rejectValue("reportName", ERROR_MSG, "There is already a report with the name '" + form.getReportName() + "' in the current folder.");
                    return;
                }
            }
        }

        @Override
        public ModelAndView getView(CopyForm form, boolean reshow, BindException errors)
        {
            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            if (protocol == null)
                return new SimpleErrorView(errors);

            r = getReport(getViewContext(), form);
            if (form.getReportName() == null || form.getReportName().length() == 0)
                form.setReportName("Copy of " + r.getDescriptor().getReportName());
            getPageConfig().setFocusId("reportName");
            return new JspView<>("/org/labkey/flow/controllers/copyReport.jsp", Pair.of(form, r), errors);
        }

        @Override
        public boolean handlePost(CopyForm form, BindException errors)
        {
            r = getReport(getViewContext(), form);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId, null);
            r.getDescriptor().setReportName(form.getReportName());
            int id = ReportService.get().saveReport(getViewContext(), null, r);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId, new DbReportIdentifier(id).toString());
            return true;
        }

        @Override
        public ActionURL getSuccessURL(CopyForm idForm)
        {
            ActionURL url = new ActionURL(UpdateAction.class, getContainer()).addParameter("reportId", r.getReportId().toString());
            if (idForm.getReturnActionURL() != null)
                url.addReturnURL(idForm.getReturnActionURL());
            return url;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).addNavTrail(root);
            if (r != null)
            root.addChild("Copy report: " + r.getDescriptor().getReportName());
        }
    }


    @RequiresPermission(DeletePermission.class)
    public static class DeleteAction extends ConfirmAction<IdForm>
    {
        IdForm _form;
        FlowReport r;

        @Override
        public ModelAndView getConfirmView(IdForm form, BindException errors)
        {
            r = getReport(getViewContext(), form);

            StringBuilder sb = new StringBuilder();
            sb.append("Delete report: ").append(PageFlowUtil.filter(r.getDescriptor().getReportName())).append("?");

            if (r.saveToDomain())
                sb.append(" All saved report results will also be deleted.");

            return new HtmlView(sb.toString());
        }

        @Override
        public void validateCommand(IdForm idForm, Errors errors)
        {
            _form = idForm;
        }

        @Override
        public URLHelper getCancelUrl()
        {
            return _form.getReturnURLHelper(new ActionURL(BeginAction.class, getContainer()));
        }

        @Override
        public ActionURL getSuccessURL(IdForm form)
        {
            return form.getReturnActionURL(new ActionURL(BeginAction.class, getContainer()));
        }

        @Override
        public boolean handlePost(IdForm form, BindException errors)
        {
            r = getReport(getViewContext(), form);
            ReportService.get().deleteReport(getViewContext(), r);
            return true;
        }
    }

    public static class ExecuteForm extends IdForm
    {
        private boolean _confirm = false;

        public boolean isConfirm()
        {
            return _confirm;
        }

        public void setConfirm(boolean confirm)
        {
            _confirm = confirm;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ExecuteAction extends SimpleViewAction<ExecuteForm>
    {
        FlowReport r;

        @Override
        public ModelAndView getView(ExecuteForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            r.updateProperties(getViewContext(), getPropertyValues(), errors, true);

            ModelAndView view;
            if (errors.hasErrors())
            {
                view = new JspView<IdForm>("/org/labkey/flow/view/errors.jsp", form, errors);
            }
            else if (r.saveToDomain())
            {
                if (form.isConfirm())
                {
                    try (var ignore = SpringActionController.ignoreSqlUpdates())
                    {
                        // Run report in background, redirect to pipeline status page
                        ViewBackgroundInfo info = getViewBackgroundInfo();
                        PipeRoot pipeRoot = PipelineService.get().getPipelineRootSetting(getContainer());
                        FlowReportJob job = new FlowReportJob((FilterFlowReport)r, info, pipeRoot);
                        PipelineService.get().queueJob(job);

                        // navigate to the job status page
                        int jobId = PipelineService.get().getJobId(getUser(), getContainer(), job.getJobGUID());
                        throw new RedirectException(PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), jobId));
                    }
                }
                else
                {
                    // Prompt for confirmation
                    view = new JspView<>("/org/labkey/flow/controllers/confirmExecuteReport.jsp", Pair.of(form, r), errors);
                }
            }
            else
            {
                // ajax execute report via POST
                view = new JspView("/org/labkey/flow/controllers/ajaxExecuteReport.jsp", Pair.of(form, r), errors);
            }

            return new VBox(new SelectReportView(form), view);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).addNavTrail(root);
            root.addChild("View report: " + r.getDescriptor().getReportName());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ExecuteReportAction extends MutatingApiAction<ExecuteForm>
    {
        @Override
        public Object execute(ExecuteForm form, BindException errors) throws Exception
        {
            FlowReport r = getReport(getViewContext(), form);
            r.updateProperties(getViewContext(), getPropertyValues(), errors, true);

            if (r.saveToDomain())
            {
                errors.addError(new LabKeyError("FlowReports that save to domains are executed in pipeline jobs"));
            }

            if (errors.hasErrors())
                return null;

            HttpView view = r.renderReport(getViewContext());

            LinkedHashSet<ClientDependency> dependencies = view.getClientDependencies();
            LinkedHashSet<String> cssScripts = new LinkedHashSet<>();

            LinkedHashSet<String> includes = new LinkedHashSet<>();
            LinkedHashSet<String> implicitIncludes = new LinkedHashSet<>();
            PageFlowUtil.getJavaScriptFiles(getContainer(), dependencies, includes, implicitIncludes);

            MockHttpServletResponse mr = new MockHttpServletResponse();
            mr.setCharacterEncoding(StringUtilsLabKey.DEFAULT_CHARSET.displayName());
            view.render(getViewContext().getRequest(), mr);

            if (mr.getStatus() != HttpServletResponse.SC_OK)
            {
                view.render(getViewContext().getRequest(), getViewContext().getResponse());
                return null;
            }

            Map<String, Object> resultProperties = new HashMap<>();
            resultProperties.put("html", mr.getContentAsString());
            resultProperties.put("requiredJsScripts", includes);
            resultProperties.put("requiredCssScripts", cssScripts);
            resultProperties.put("implicitJsIncludes", implicitIncludes);
            resultProperties.put("moduleContext", PageFlowUtil.getModuleClientContext(getViewContext(), dependencies));
            return new ApiSimpleResponse(resultProperties);
        }

    }


    public static class IdForm extends ReturnUrlForm implements HasViewContext
    {
        private DbReportIdentifier _id;

        public DbReportIdentifier getReportId() {return _id;}

        public void setReportId(DbReportIdentifier id) {_id = id;}


        public ActionURL url(Class action)
        {
            ActionURL url = new ActionURL(action, getViewContext().getContainer());
            url.addParameter("reportId", _id.toString());
            return url;
        }

        ViewContext _context = null;

        @Override
        public void setViewContext(ViewContext context)
        {
            _context = context;
        }

        @Override
        public ViewContext getViewContext()
        {
            return _context;
        }
    }

    public static FlowReport getReport(ViewContext context, IdForm form)
    {
        if (null == form.getReportId())
        {
            throw new NotFoundException();
        }
        Report r = form.getReportId().getReport(context);
        if (null == r || !(r instanceof FlowReport))
        {
            throw new NotFoundException();
        }
        if (!r.getDescriptor().getContainerId().equals(context.getContainer().getId()))
        {
            throw new NotFoundException();
        }
        return (FlowReport)r;
    }


    public static FlowReport createReport(String reportType)
    {
        Report report = ReportService.get().createReportInstance(reportType);
        if (report == null)
            throw new NotFoundException("report type not registered");

        if (!(report instanceof FlowReport))
            throw new IllegalArgumentException("expected flow report type");

        return (FlowReport)report;
    }


    public static class SelectReportView extends JspView<IdForm>
    {
        SelectReportView(IdForm form)
        {
            super("/org/labkey/flow/controllers/selectReport.jsp", form);
        }
    }
}
