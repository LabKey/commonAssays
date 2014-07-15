<%
/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.announcements.DiscussionService" %>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.api.data.Table" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.query.QueryForm" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="org.labkey.flow.controllers.executescript.ScriptOverview" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.query.FlowSchema" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="org.labkey.flow.view.SetCommentView" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        // TODO: --Ext3-- This should be declared as part of the included views
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("clientapi/ext3"));
        return resources;
    }
%>
<%
    final FlowScript script = (FlowScript)getModelBean();
    ViewContext context = getViewContext();
    ActionURL url = getActionURL();

    boolean canEdit = context.hasPermission(UpdatePermission.class);
%>
Analysis scripts may have up to two sections in them.
The compensation calculation describes how to locate the compensation controls in each run, and which gates need to be applied to them.
The analysis section describes which gates in the analysis, as well as the statistics that need to be calculated, and the graphs that need to be drawn.
<p>
<% if (canEdit || script.getExpObject().getComment() != null) { %>
    Script Comment: <% include(new SetCommentView(script), out); %>
<% } %>
</p>
<%
    ScriptOverview overview = new ScriptOverview(getUser(), getContainer(), script);
%>
<%=text(overview.toString())%>

<div>
<% if (script.getRunCount() > 0) {
    boolean showRuns = FlowPreference.showRuns.getBooleanValue(request);
    if (showRuns) {
        %><labkey:link href='<%=url.clone().replaceParameter("showRuns", "0")%>' text="Hide Runs"/><br/><%
        QueryForm form = new QueryForm();
        form.setViewContext(context);
        form.setSchemaName(FlowSchema.SCHEMANAME);
        form.setQueryName(FlowTableType.Runs.toString());

        QueryView view = new QueryView(form, null);
        view.setShadeAlternatingRows(true);
        view.setShowPagination(false);
        view.setShowBorders(true);
        view.setShowRecordSelectors(false);
        view.setShowExportButtons(false);
        view.getSettings().setMaxRows(Table.ALL_ROWS);
        view.getSettings().setAllowChooseView(false);
        view.getSettings().setAllowCustomizeView(false);
        view.getSettings().getBaseFilter().addCondition(FieldKey.fromParts("AnalysisScript", "RowId"), script.getScriptId(), CompareType.EQUAL);
        include(view, out);
    } else {
        %><labkey:link href='<%=url.clone().replaceParameter("showRuns", "1")%>' text="Show Runs"/><%
    }
} %>
</div>

<%
    DiscussionService.Service service = DiscussionService.get();
    DiscussionService.DiscussionView discussion = service.getDisussionArea(
            context,
            script.getLSID(),
            script.urlShow(),
            "Discussion of " + script.getLabel(),
            false, true);
    include(discussion, out);
%>

