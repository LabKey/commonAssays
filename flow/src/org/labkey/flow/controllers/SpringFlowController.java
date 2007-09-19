package org.labkey.flow.controllers;

import org.labkey.api.jsp.JspLoader;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.view.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.action.SpringActionController;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.script.ScriptJob;
import org.labkey.flow.webparts.FlowFolderType;
import org.apache.beehive.netui.pageflow.Forward;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;


public class SpringFlowController<A extends Enum, P extends Enum> extends SpringActionController
{
    protected SpringFlowController.FlowPage getFlowPage(String name) throws Exception
    {
        return getFlowPage(name, getClass().getPackage());
    }

    protected SpringFlowController.FlowPage getFlowPage(String name, Package thePackage) throws Exception
    {
        SpringFlowController.FlowPage ret = (SpringFlowController.FlowPage) JspLoader.createPage(getRequest(), thePackage.getName(), name);
        ret._controller = this;
        return ret;
    }

    protected FlowScript getScript() throws Exception
    {
        return FlowScript.fromURL(getViewURLHelper(), getRequest());
    }

    public FlowRun getRun() throws Exception
    {
        FlowRun ret;
        ret = FlowRun.fromURL(getViewURLHelper());
        return ret;
    }

    protected Forward executeScript(ScriptJob job, FlowScript script) throws Exception
    {
        FlowProtocol.ensureForContainer(getUser(), job.getContainer());
        PipelineService service = PipelineService.get();
        service.getPipelineQueue().addJob(job);

        ViewURLHelper forward = job.getStatusHref().clone();
        putParam(forward, FlowParam.redirect, 1);
        forward.setFragment("end");
        return new ViewForward(forward);
    }


    // override to append root nav to all paths
    @Override
    protected void appendNavTrail(Controller action, NavTree root)
    {
        root.addChild(getFlowNavStart(getViewContext()));
        super.appendNavTrail(action, root);
    }
    

    public NavTree appendFlowNavTrail(NavTree root, ViewContext context, FlowObject object, String title, Enum action)
    {
        ArrayList<NavTree> children = new ArrayList<NavTree>();
        while (object != null)
        {
            children.add(0, new NavTree(object.getLabel(), object.urlShow()));
            object = object.getParent();
        }

        root.addChildren(children);
        root.addChild(title);

// UNDONE
//        ntc.setHelpTopic(new HelpTopic(PFUtil.helpTopic(action), HelpTopic.Area.FLOW));
        return root;
    }


    public NavTree appendFlowNavTrail(NavTree root, FlowObject object, String title, A action)
    {
        return appendFlowNavTrail(root, getViewContext(), object, title, action);
    }


    public NavTree getFlowNavStart(ViewContext context)
    {
        NavTree project;
        if (context.getContainer().getFolderType() instanceof FlowFolderType)
        {
            project = new NavTree("Dashboard", new ViewURLHelper("Project", "begin", context.getContainer()));
        }
        else
        {
            ViewURLHelper url = PageFlowUtil.urlFor(FlowController.Action.begin, context.getContainer().getPath());
            project = new NavTree(FlowModule.getShortProductName(), url.clone());
        }
        return project;
    }


    abstract static public class FlowPage<C extends SpringFlowController> extends JspBase
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
            return PageFlowUtil.urlFor(action, getContainer().getPath());
        }

    }

//    protected Forward renderInTemplate(HttpView view, FlowObject object, String title, A action) throws Exception
//    {
//        return renderInTemplate(view, getContainer(), getNavTrailConfig(object, title, action));
//    }
//
//    protected Forward renderPage(ViewForm form, String jspFile, FlowObject object, String title, A action) throws Exception
//    {
//        return renderInTemplate(FormPage.getView(getClass(), form, jspFile), object, title, action);
//    }


    public ViewURLHelper urlFor(Enum action)
    {
        return PageFlowUtil.urlFor(action, getContainerPath());
    }

    protected int getIntParam(P param)
    {
        String value = getParam(param);
        if (value == null)
            return 0;
        return Integer.valueOf(value).intValue();
    }

    protected String getParam(P param)
    {
        return getRequest().getParameter(param.toString());
    }

    protected void putParam(ViewURLHelper helper, Enum param, String value)
    {
        helper.replaceParameter(param.toString(), value);
    }

    protected void putParam(ViewURLHelper helper, Enum param, int value)
    {
        putParam(helper, param, Integer.toString(value));
    }

    protected boolean hasParameter(String name)
    {
        if (getRequest().getParameter(name) != null)
            return true;
        if (getRequest().getParameter(name + ".x") != null)
            return true;
        return false;
    }



    public String getContainerPath()
    {
        return getViewURLHelper().getExtraPath();
    }

    public Container getContainer()
    {
        return getViewContext().getContainer();
    }

    public HttpServletRequest getRequest()
    {
        return getViewContext().getRequest();
    }

    public ViewURLHelper getViewURLHelper()
    {
        return getViewContext().getViewURLHelper();
    }

    public User getUser()
    {
        return getViewContext().getUser();
    }
}
