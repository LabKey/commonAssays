<%
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
%>
<%@ page buffer="none" %>
<%@ page import="org.labkey.api.announcements.DiscussionService" %>
<%@ page import="org.labkey.api.attachments.Attachment"%>
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.data.ContainerManager"%>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.study.Study" %>
<%@ page import="org.labkey.api.study.StudyService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowProperty" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.view.FlowQueryView" %>
<%@ page import="org.labkey.flow.view.SetCommentView" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        // TODO: --Ext3-- This should be declared as part of the included views
        dependencies.add("clientapi/ext3");
        // TODO: ColumnAnalyticsProvider dependencies should be coming from the FlowQueryView data region
        dependencies.add("vis/vis");
        dependencies.add("vis/ColumnVisualizationAnalytics.js");
        dependencies.add("vis/ColumnVisualizationAnalytics.css");
        dependencies.add("query/ColumnQueryAnalytics.js");
        dependencies.add("query/ColumnSummaryStatistics");
        dependencies.add("announcements/discuss");
    }
%>
<%
    RunForm form = (RunForm) HttpView.currentModel();
    ViewContext context = getViewContext();

    final FlowRun run = form.getRun();
    FlowCompensationMatrix comp = run.getCompensationMatrix();

    boolean canEdit = context.hasPermission(UpdatePermission.class);

    if (canEdit || run.getExpObject().getComment() != null)
    {
        %><p> Run Comment: <% include(new SetCommentView(run), out); %></p><%
    }

    String targetStudyId = (String)run.getProperty(FlowProperty.TargetStudy);
    if (targetStudyId != null && targetStudyId.length() > 0)
    {
        Container c = ContainerManager.getForId(targetStudyId);
        if (c != null)
        {
            Study study = StudyService.get().getStudy(c);
            if (study != null)
            {
                ActionURL studyURL = urlProvider(ProjectUrls.class).getBeginURL(c);
                %><p> Target Study: <%=link(study.getLabel(), studyURL)%></p><%
            }
        }
    }

    FlowQueryView view = new FlowQueryView(form);
    include(view, out);

    // UNDONE: link to audit log for keyword changes
 %><p><%

    if (comp != null)
    {
        %><%=link("Show Compensation", comp.urlFor(CompensationController.ShowCompensationAction.class))%><br><%
    }

    ActionURL urlShowRunGraph = urlProvider(ExperimentUrls.class).getShowRunGraphURL(run.getExperimentRun()); %>
    <%=link("Experiment Run Graph", urlShowRunGraph)%><br><%

    ActionURL showFileURL = run.getDownloadWorkspaceURL();
    if (showFileURL != null)
    {
        %><%=link("Download Workspace XML File", showFileURL)%><br/><%
    }

    User user = getUser();
    if (user != null && !user.isGuest())
    {
        if (run.getPath() != null)
        {
        %><%=link("Download FCS Files", run.urlDownload()).nofollow()%><br><%
        }
        %><%=link("Download Analysis zip", run.urlFor(RunController.ExportAnalysis.class).addParameter("selectionType", "runs")).nofollow()%><br><%
    }


    for (Attachment a : run.getAttachments())
    {
        %><div>
        <a href="<%=h(run.getAttachmentDownloadURL(a))%>"><i class="<%=h(Attachment.getFileIconFontCls(a.getName()))%>"></i> <%=h(a.getName())%></a>
        </div><%
    }

    DiscussionService service = DiscussionService.get();
    if (service != null)
    {
        DiscussionService.DiscussionView discussion = service.getDiscussionArea(
                getViewContext(),
                run.getLSID(),
                run.urlShow(),
                "Discussion of " + run.getLabel(),
                false, true);
        if (discussion != null)
            include(discussion, out);
    }
%>
</p>
