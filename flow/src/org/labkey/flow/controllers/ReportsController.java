/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.api.action.*;
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
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.*;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.reports.FilterFlowReport;
import org.labkey.flow.reports.FlowReport;
import org.labkey.flow.reports.FlowReportJob;
import org.labkey.flow.reports.FlowReportManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;

/**
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 5:15:39 PM
 */
public class ReportsController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(ReportsController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ReportsController.class);

    public ReportsController()
    { 
        setActionResolver(_actionResolver);
    }


    public static class BeginView extends JspView
    {
        public BeginView()
        {
            super(ReportsController.class, "reports.jsp", null);
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
        
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new BeginView();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Reports", new ActionURL(BeginAction.class, getContainer()));
            return root;
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
        public ModelAndView getView(FORM form, BindException errors) throws Exception
        {
            initReport(form);
            return r.getConfigureForm(getViewContext(), form.getReturnActionURL());
        }

        @Override
        public ApiResponse execute(FORM form, BindException errors) throws Exception
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
        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("Create new report: " + r.getTypeDescription());
            return root;
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

        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("Edit report: " + r.getDescriptor().getReportName());
            return root;
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

        public void validateCommand(CopyForm form, Errors errors)
        {
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
        public ModelAndView getView(CopyForm form, boolean reshow, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            if (form.getReportName() == null || form.getReportName().length() == 0)
                form.setReportName("Copy of " + r.getDescriptor().getReportName());
            getPageConfig().setFocusId("reportName");
            return new JspView<>(ReportsController.class, "copyReport.jsp", Pair.of(form, r), errors);
        }

        public boolean handlePost(CopyForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId, null);
            r.getDescriptor().setReportName(form.getReportName());
            int id = ReportService.get().saveReport(getViewContext(), null, r);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId, new DbReportIdentifier(id).toString());
            return true;
        }

        public ActionURL getSuccessURL(CopyForm idForm)
        {
            ActionURL url = new ActionURL(UpdateAction.class, getContainer()).addParameter("reportId", r.getReportId().toString());
            if (idForm.getReturnActionURL() != null)
                url.addReturnURL(idForm.getReturnActionURL());
            return url;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("Copy report: " + r.getDescriptor().getReportName());
            return root;
        }
    }


    @RequiresPermission(DeletePermission.class)
    public static class DeleteAction extends ConfirmAction<IdForm>
    {
        IdForm _form;
        FlowReport r;

        public ModelAndView getConfirmView(IdForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);

            StringBuilder sb = new StringBuilder();
            sb.append("Delete report: ").append(PageFlowUtil.filter(r.getDescriptor().getReportName())).append("?");

            if (r.saveToDomain())
                sb.append(" All saved report results will also be deleted.");

            return new HtmlView(sb.toString());
        }

        public void validateCommand(IdForm idForm, Errors errors)
        {
            _form = idForm;
        }

        @Override
        public URLHelper getCancelUrl()
        {
            return _form.getReturnURLHelper(new ActionURL(BeginAction.class, getContainer()));
        }

        public ActionURL getSuccessURL(IdForm form)
        {
            return form.getReturnActionURL(new ActionURL(BeginAction.class, getContainer()));
        }

        public boolean handlePost(IdForm form, BindException errors) throws Exception
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
                    // Run report in background, redirect to pipeline status page
                    ViewBackgroundInfo info = getViewBackgroundInfo();
                    PipeRoot pipeRoot = PipelineService.get().getPipelineRootSetting(getContainer());
                    FlowReportJob job = new FlowReportJob((FilterFlowReport)r, info, pipeRoot);
                    PipelineService.get().queueJob(job);
                    throw new RedirectException(PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlBegin(getContainer()));
                }
                else
                {
                    // Prompt for confirmation
                    view = new JspView<>(ReportsController.class, "confirmExecuteReport.jsp", Pair.of(form, r), errors);
                }
            }
            else
            {
                // Synchronous report
                view = r.renderReport(getViewContext());
            }

            return new VBox(new SelectReportView(form), view);
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("View report: " + r.getDescriptor().getReportName());
            return root;
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

        public void setViewContext(ViewContext context)
        {
            _context = context;
        }

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
            super(ReportsController.class, "selectReport.jsp", form);
        }
    }
}
