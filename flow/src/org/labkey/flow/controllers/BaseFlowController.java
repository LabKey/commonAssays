/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.script.FlowJob;
import org.labkey.flow.webparts.FlowFolderType;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.ArrayList;

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
        return FlowScript.fromURL(getActionURL(), getRequest());
    }

    public FlowRun getRun() throws Exception
    {
        FlowRun ret = FlowRun.fromURL(getActionURL());
        return ret;
    }

    protected Forward executeScript(FlowJob job) throws Exception
    {
        FlowProtocol.ensureForContainer(getUser(), job.getContainer());
        PipelineService service = PipelineService.get();
        service.getPipelineQueue().addJob(job);

        ActionURL forward = job.getStatusHref().clone();
        putParam(forward, FlowParam.redirect, 1);
        forward.setFragment("end");
        return new ViewForward(forward);
    }

    static public NavTrailConfig getFlowNavConfig(ViewContext context, FlowObject object, String title, Enum action)
    {
        ArrayList<NavTree> children = new ArrayList<NavTree>();

        while (object != null)
        {
            children.add(0, new NavTree(object.getLabel(), object.urlShow()));
            object = object.getParent();
        }

        if (context.getContainer().getFolderType() instanceof FlowFolderType)
        {
            children.add(0, new NavTree("Dashboard", new ActionURL("Project", "begin", context.getContainer())));
        }
        else
        {
            ActionURL url = PageFlowUtil.urlFor(FlowController.Action.begin, context.getContainer());
            children.add(0, new NavTree(FlowModule.getShortProductName(), url.clone()));
        }

        NavTrailConfig ntc = new NavTrailConfig(context);
        if (title == null)
        {
            NavTree last = children.remove(children.size() - 1);
            title = last.getKey();
        }
        ntc.setExtraChildren(children.toArray(new NavTree[0]));
        ntc.setTitle(title);
//        ntc.setHelpTopic(new HelpTopic(PageFlowUtil.helpTopic(action), HelpTopic.Area.FLOW));
        ntc.setHelpTopic(new HelpTopic("flowDefault", HelpTopic.Area.FLOW));
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
