/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.*;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.reports.FilterFlowReport;
import org.labkey.flow.reports.FlowReport;
import org.labkey.flow.reports.ControlsQCReport;
import org.labkey.flow.reports.FlowReportJob;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 5:15:39 PM
 */
public class ReportsController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(ReportsController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ReportsController.class);

    public ReportsController() throws Exception
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


    @RequiresPermissionClass(ReadPermission.class)
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
            root.addChild("Reports", new ActionURL(BeginAction.class,getViewContext().getContainer()));
            return root;
        }
    }


    public static class CreateReportForm
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

    private abstract static class CreateOrUpdateAction<FORM> extends FormApiAction<FORM>
    {
        FlowReport r;

        public abstract void initReport(FORM form) throws Exception;

        @Override
        protected String getCommandClassMethodName()
        {
            return "initReport";
        }

        @Override
        public ModelAndView getView(FORM form, BindException errors) throws Exception
        {
            initReport(form);
            return r.getConfigureForm(getViewContext());
        }

        @Override
        public ApiResponse execute(FORM form, BindException errors) throws Exception
        {
            initReport(form);
            r.updateProperties(getPropertyValues(), errors, false);

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

    @RequiresPermissionClass(InsertPermission.class)
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
            root.addChild("Create new report");
            return root;
        }
    }
    
    @RequiresPermissionClass(UpdatePermission.class)
    public static class UpdateAction extends CreateOrUpdateAction<IdForm>
    {
        @Override
        public void initReport(IdForm form) throws Exception
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

    @RequiresPermissionClass(UpdatePermission.class)
    public static class CopyAction extends FormHandlerAction<IdForm>
    {
        FlowReport r;

        public void validateCommand(IdForm target, Errors errors)
        {
        }

        public boolean handlePost(IdForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId,null);
            r.getDescriptor().setReportName("Copy of " + r.getDescriptor().getReportName());
            int id = ReportService.get().saveReport(getViewContext(), null, r);
            r.getDescriptor().setProperty(ReportDescriptor.Prop.reportId, new DbReportIdentifier(id).toString());
            return true;
        }

        public ActionURL getSuccessURL(IdForm idForm)
        {
            return new ActionURL(UpdateAction.class, getViewContext().getContainer()).addParameter("reportId",r.getReportId().toString());
        }
    }


    @RequiresPermissionClass(DeletePermission.class)
    public static class DeleteAction extends ConfirmAction<IdForm>
    {
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
        }

        public ActionURL getSuccessURL(IdForm idForm)
        {
            return new ActionURL(BeginAction.class,getViewContext().getContainer());
        }

        public boolean handlePost(IdForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            ReportService.get().deleteReport(getViewContext(), r);
            return true;
        }
    }
    

    @RequiresPermissionClass(ReadPermission.class)
    public class ExecuteAction extends SimpleViewAction<IdForm>
    {
        FlowReport r;

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            r.updateProperties(getPropertyValues(), errors, true);

            ModelAndView view;
            if (errors.hasErrors())
            {
                view = new JspView<IdForm>("/org/labkey/flow/view/errors.jsp", form, errors);
            }
            else if (r.saveToDomain())
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
                // Synchronous report
                view = r.renderReport(getViewContext());
            }

            return new VBox(new SelectReportView(form), view);
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("View QC report: " + r.getDescriptor().getReportName());
            return root;
        }
    }


    public static class IdForm implements HasViewContext
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

    public static FlowReport getReport(ViewContext context, IdForm form) throws Exception
    {
        try
        {
            if (null == form.getReportId())
            {
                throw new NotFoundException();
            }
            Report r = form.getReportId().getReport();
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
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    public static FlowReport createReport(String reportType)
    {
        Report report = ReportService.get().createReportInstance(reportType);
        if (report == null)
            throw new IllegalArgumentException("report type not registered");

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
