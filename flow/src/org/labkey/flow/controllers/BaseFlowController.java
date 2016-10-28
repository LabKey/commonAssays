/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.labkey.api.action.HasPageConfig;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegion;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.PageConfig;
import org.labkey.flow.FlowModule;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.script.FlowJob;
import org.labkey.flow.webparts.FlowFolderType;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;


public abstract class BaseFlowController extends SpringActionController
{
    public static HelpTopic DEFAULT_HELP_TOPIC = new HelpTopic("flowDefault");
    
    protected BaseFlowController.FlowPage getFlowPage(String name)
    {
        return getFlowPage(name, getClass().getPackage());
    }

    protected BaseFlowController.FlowPage getFlowPage(String name, Package thePackage)
    {
        BaseFlowController.FlowPage ret = (BaseFlowController.FlowPage) JspLoader.createPage(thePackage.getName(), name);
        ret._controller = this;
        return ret;
    }

    protected FlowScript getScript()
    {
        return FlowScript.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
    }

    public FlowRun getRun()
    {
        FlowRun ret;
        ret = FlowRun.fromURL(getActionURL(), getContainer(), getUser());
        return ret;
    }

    public void checkContainer(FlowObject obj)
    {
        if (obj != null)
        {
            obj.checkContainer(getContainer(),getUser(),getActionURL());
        }
    }

    protected ActionURL executeScript(FlowJob job) throws Exception, PipelineValidationException
    {
        FlowProtocol.ensureForContainer(getUser(), job.getContainer());
        PipelineService service = PipelineService.get();
        service.queueJob(job);

        ActionURL forward = job.getStatusHref().clone();
        putParam(forward, FlowParam.redirect, 1);
        return forward;
    }

    public HelpTopic getHelpTopic()
    {
        return DEFAULT_HELP_TOPIC;
    }

    // override to append root nav to all paths
    @Override
    protected void appendNavTrail(Controller action, NavTree root)
    {
        PageConfig page = null;
        if (action instanceof HasPageConfig)
            page = ((HasPageConfig)action).getPageConfig();
        root.addChild(getFlowNavStart(page, getViewContext()));
        super.appendNavTrail(action, root);
    }

    public NavTree appendFlowNavTrail(PageConfig page, NavTree root, FlowObject object, String title)
    {
        ArrayList<NavTree> children = new ArrayList<>();
        while (object != null)
        {
            children.add(0, new NavTree(object.getLabel(), object.urlShow()));
            object = object.getParent();
        }

        root.addChildren(children);
        if (title != null)
            root.addChild(title);

        if (page.getHelpTopic() == HelpTopic.DEFAULT_HELP_TOPIC)
            page.setHelpTopic(getHelpTopic());

        return root;
    }


    public NavTree getFlowNavStart(PageConfig page, ViewContext context)
    {
        NavTree project;
        if (context.getContainer().getFolderType() instanceof FlowFolderType)
        {
            ActionURL url = PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(context.getContainer());
            url.replaceParameter(DataRegion.LAST_FILTER_PARAM, "true");
            project = new NavTree("Dashboard", url);
            if (page.getHelpTopic() == HelpTopic.DEFAULT_HELP_TOPIC)
                page.setHelpTopic(getHelpTopic());
        }
        else
        {
            ActionURL url = new ActionURL(FlowController.BeginAction.class, context.getContainer());
            project = new NavTree(FlowModule.getShortProductName(), url.clone());
        }
        return project;
    }

    abstract public class FlowAction<FORM extends FlowObjectForm> extends SimpleViewAction<FORM>
    {
        FORM _form;

        @Override
        public void validate(FORM form, BindException errors)
        {
            _form = form;
        }

        protected abstract String getPageTitle();

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, _form.getFlowObject(), getPageTitle());
        }
    }

    abstract static public class FlowPage<C extends BaseFlowController> extends JspBase
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

        public String getContainerPath()
        {
            return getContainer().getPath();
        }
    }


    protected ActionURL urlFor(Class<? extends Controller> actionClass)
    {
        return new ActionURL(actionClass, getContainer());
    }


    protected int getIntParam(FlowParam param)
    {
        String value = getParam(param);
        if (value == null)
            return 0;
        try
        {
            return Integer.valueOf(value).intValue();
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
    }

    protected String getParam(FlowParam param)
    {
        return getRequest().getParameter(param.toString());
    }

    protected void putParam(ActionURL url, Enum param, String value)
    {
        url.replaceParameter(param.toString(), value);
    }

    protected void putParam(ActionURL url, Enum param, int value)
    {
        putParam(url, param, Integer.toString(value));
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
        return getActionURL().getExtraPath();
    }

    public HttpServletRequest getRequest()
    {
        return getViewContext().getRequest();
    }

    public ActionURL getActionURL()
    {
        return getViewContext().getActionURL();
    }
}
