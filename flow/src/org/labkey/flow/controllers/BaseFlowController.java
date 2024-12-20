/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.HasPageConfig;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegion;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.util.HelpTopic;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;


public abstract class BaseFlowController extends SpringActionController
{
    public static String DEFAULT_HELP_TOPIC = "flowDefault";

    protected JspBase getFlowPage(String jspPath)
    {
        return (JspBase)JspLoader.createPage(jspPath);
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

    public void checkContainer(FlowObject<?> obj)
    {
        if (obj != null)
        {
            obj.checkContainer(getContainer(),getUser(),getActionURL());
        }
    }

    protected ActionURL executeScript(FlowJob job) throws Exception
    {
        try (var ignore = SpringActionController.ignoreSqlUpdates())
        {
            FlowProtocol.ensureForContainer(getUser(), job.getContainer());
            PipelineService service = PipelineService.get();
            service.queueJob(job);

            ActionURL forward = job.urlStatus();
            putParam(forward, FlowParam.redirect, 1);
            return forward;
        }
    }

    public String getHelpTopic()
    {
        return DEFAULT_HELP_TOPIC;
    }

    // override to append root nav to all paths -- except in root
    @Override
    protected void addNavTrail(Controller action, NavTree root)
    {
        if (!getContainer().isRoot())
        {
            PageConfig page = null;
            if (action instanceof HasPageConfig)
                page = ((HasPageConfig) action).getPageConfig();
            root.addChild(getFlowNavStart(page, getViewContext()));
        }
        super.addNavTrail(action, root);
    }

    public void addFlowNavTrail(PageConfig page, NavTree root, @Nullable FlowObject<?> object, String title)
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
    }


    public NavTree getFlowNavStart(PageConfig page, ViewContext context)
    {
        NavTree project;
        if (context.getContainer().getFolderType() instanceof FlowFolderType)
        {
            ActionURL url = urlProvider(ProjectUrls.class).getBeginURL(context.getContainer());
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

    abstract public class FlowMutatingAction<FORM extends FlowObjectForm<?>> extends FormViewAction<FORM>
    {
        FORM _form;

        public void setForm(FORM form)
        {
            _form = form;
        }

        protected abstract String getPageTitle();

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, _form.getFlowObject(), getPageTitle());
        }
    }

    abstract public class FlowAction<FORM extends FlowObjectForm<?>> extends SimpleViewAction<FORM>
    {
        FORM _form;

        @Override
        public void validate(FORM form, BindException errors)
        {
            _form = form;
        }

        protected abstract String getPageTitle();

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, _form.getFlowObject(), getPageTitle());
        }
    }

    abstract static public class FlowPage extends JspBase
    {
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

    protected void putParam(ActionURL url, Enum<?> param, String value)
    {
        url.replaceParameter(param.toString(), value);
    }

    protected void putParam(ActionURL url, Enum<?> param, int value)
    {
        putParam(url, param, Integer.toString(value));
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
