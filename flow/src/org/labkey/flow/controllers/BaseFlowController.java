package org.labkey.flow.controllers;

import org.labkey.api.jsp.JspLoader;
import org.labkey.api.jsp.FormPage;
import org.labkey.flow.data.*;
import org.labkey.flow.script.ScriptJob;
import org.labkey.flow.util.PFUtil;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusManager;
import org.labkey.api.view.*;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.apache.beehive.netui.pageflow.Forward;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.io.IOException;

public class BaseFlowController<A extends Enum<A>> extends BaseController<A, FlowParam>
{
    protected FlowPage getFlowPage(String name) throws Exception
    {
        return getFlowPage(name, getClass().getPackage());
    }

    protected FlowPage getFlowPage(String name, Package thePackage) throws Exception
    {
        FlowPage ret = (FlowPage) JspLoader.createPage(getRequest(), thePackage.getName(), name);
        ret._controller = this;
        return ret;
    }

    protected FlowScript getScript() throws Exception
    {
        return FlowScript.fromURL(getViewURLHelper(), getRequest());
    }

    public FlowRun getRun() throws Exception
    {
        FlowRun ret = FlowRun.fromURL(getViewURLHelper());
        return ret;
    }

    protected Forward executeScript(ScriptJob job, FlowScript script) throws Exception
    {
        FlowProtocol.ensureForContainer(getUser(), getContainer());
        PipelineService service = PipelineService.get();
        service.getPipelineQueue().addJob(job);

        ViewURLHelper forward = job.getStatusHref().clone();
        putParam(forward, FlowParam.redirect, 1);
        forward.setFragment("end");
        return new ViewForward(forward);
    }

    static public NavTrailConfig getFlowNavConfig(ViewContext context, FlowObject object, String title, Enum action)
    {
        ArrayList<Pair> children = new ArrayList();
        ViewURLHelper url = PFUtil.urlFor(FlowController.Action.begin, context.getContainer().getPath());

        while (object != null)
        {
            children.add(0, new Pair(object.getLabel(), object.urlShow()));
            object = object.getParent();
        }
        children.add(0, new Pair(FlowModule.getShortProductName(), url.clone()));


        NavTrailConfig ntc = new NavTrailConfig(context);
        if (title == null)
        {
            Map.Entry<String, ViewURLHelper> last = children.remove(children.size() - 1);
            title = last.getKey();
        }
        ntc.setExtraChildren(children.toArray(new Pair[0]));
        ntc.setTitle(title);
        ntc.setHelpTopic(PFUtil.helpTopic(action));
        return ntc;

    }

    protected NavTrailConfig getNavTrailConfig(FlowObject object, String title, A action) throws SQLException
    {
        return getFlowNavConfig(getViewContext(), object, title, action);
    }

    abstract static public class FlowPage<C extends BaseFlowController> extends BaseFlowPage
    {
        C _controller;

        public void setPageFlow(C controller)
        {
            _controller = controller;
        }

        public C getPageFlow()
        {
            return _controller;
        }

        public User getUser()
        {
            return _controller.getUser();
        }

        public Container getContainer() throws ServletException
        {
            return _controller.getContainer();
        }

        public String getContainerPath() throws ServletException
        {
            return getContainer().getPath();
        }

        public ViewURLHelper urlFor(Enum action) throws ServletException
        {
            return PFUtil.urlFor(action, getContainer().getPath());
        }

    }

    protected Forward renderInTemplate(HttpView view, FlowObject object, String title, A action) throws Exception
    {
        return renderInTemplate(view, getContainer(), getNavTrailConfig(object, title, action));
    }

    protected Forward renderPage(ViewForm form, String jspFile, FlowObject object, String title, A action) throws Exception
    {
        return renderInTemplate(FormPage.getView(getClass(), form, jspFile), object, title, action);
    }
}
