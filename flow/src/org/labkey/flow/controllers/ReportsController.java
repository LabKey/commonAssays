/*
 * Copyright (c) 2009 LabKey Corporation
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
import org.labkey.api.reports.Report;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.DbReportIdentifier;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.ReportIdentifier;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.*;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.reports.FlowReport;
import org.labkey.flow.reports.ControlsQCReport;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 5:15:39 PM
 */
public class ReportsController extends BaseFlowController<ReportsController.Action>
{
    private static Logger _log = Logger.getLogger(ReportsController.class);

    public enum Action
    {
        begin,
        create,
        edit,
        delete,
        execute
    }

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(ReportsController.class);


    public ReportsController() throws Exception
    { 
        super();
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


    private static class FormView extends HttpView
    {
        IdForm _form;
        Class _post;
        
        FormView(Class post, IdForm form, HttpView body)
        {
            _form = form;
            _post = post;
            setBody(body);
        }
        
        @Override
        protected void renderInternal(Object model, PrintWriter out) throws Exception
        {
            ActionURL url = new ActionURL(_post, getViewContext().getContainer());
            if (null != _form && null != _form.getReportId())
                url.addParameter("reportId", _form.getReportId().toString());

//            out.print("<form method=POST action='");
//            out.print(url.getEncodedLocalURIString());
//            out.print("'><input type=submit><br>");
//            out.print("<input name='name' value=''><br>");
            include(getBody(),out);
//            out.print("</form>");
        }
    }
    

/*
    @RequiresPermissionClass(InsertPermission.class)
    public static class CreateAction extends FlowExtAction<IdForm>
    {
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            FlowReport r = new ControlsQCReport();
            return new FormView(CreateAction.class, form, r.getConfigureForm());
        }

        public ActionURL getSuccessURL(IdForm form)
        {
            return form.url(ExecuteAction.class);
        }

        public void validateCommand(IdForm target, Errors errors)
        {

        }

        public ApiResponse execute(IdForm form, BindException errors) throws Exception
        {
            FlowReport r = new ControlsQCReport();
            r.updateProperties(getPropertyValues(), errors, false);
            int id = ReportService.get().saveReport(getViewContext(), r.getDescriptor().getReportName(), r);
            form.setReportId(new DbReportIdentifier(id));
            ApiSimpleResponse ret = new ApiSimpleResponse(r.getDescriptor().getProperties());
            ret.put("reportId", form.getReportId().toString());
            return ret;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            root.addChild("Create new report");
            return root;
        }
    }
*/

    @RequiresPermissionClass(UpdatePermission.class)
    public static class UpdateAction extends FormApiAction<IdForm>
    {
        FlowReport r;

        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (null == form.getReportId())
                r = new ControlsQCReport();
            else
                r = getReport(getViewContext(), form);
            return new FormView(UpdateAction.class, form, r.getConfigureForm());
        }

        public ApiResponse execute(IdForm form, BindException errors) throws Exception
        {
            if (null == form.getReportId())
                r = new ControlsQCReport();
            else
                r = getReport(getViewContext(), form);
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

        public NavTree appendNavTrail(NavTree root)
        {
            new BeginAction(getViewContext()).appendNavTrail(root);
            if (r.getReportId() == null)
                root.addChild("Create new report");
            else
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
            return new HtmlView("Delete report: " + PageFlowUtil.filter(r.getDescriptor().getReportName()) + "?");
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
    public static class ExecuteAction extends SimpleViewAction<IdForm>
    {
        FlowReport r;
        
        public void validateCommand(IdForm target, Errors errors)
        {
        }

        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            r = getReport(getViewContext(), form);
            r.updateProperties(getPropertyValues(), errors, true);
            ModelAndView Rview = r.renderReport(getViewContext());
            return new VBox(new SelectReportView(form), Rview);
        }

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
                HttpView.throwNotFound();
            Report r = form.getReportId().getReport();
            if (null == r || !(r instanceof FlowReport))
                HttpView.throwNotFound();
            if (!r.getDescriptor().getContainerId().equals(context.getContainer().getId()))
                HttpView.throwNotFound();
            return (FlowReport)r;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }



    public static class SelectReportView extends JspView<IdForm>
    {
        SelectReportView(IdForm form)
        {
            super(ReportsController.class, "selectReport.jsp", form);
        }
    }
}
