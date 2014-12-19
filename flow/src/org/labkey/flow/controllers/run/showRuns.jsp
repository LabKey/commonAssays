<%
/*
 * Copyright (c) 2009-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.security.permissions.UpdatePermission"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.flow.controllers.run.RunsForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.view.FlowQueryView" %>
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
        resources.add(ClientDependency.fromPath("clientapi/ext3"));
        return resources;
    }
%>
<%
    RunsForm form = (RunsForm) HttpView.currentModel();
    FlowExperiment experiment = form.getExperiment();

    boolean canEdit = getViewContext().hasPermission(UpdatePermission.class);
%>
<% if (experiment != null && (canEdit || experiment.getExpObject().getComment() != null)) { %>
<p>
    Analysis Folder Comment: <% include(new SetCommentView(experiment), out); %>
</p>
<% } %>
<%
    FlowQueryView view = new FlowQueryView(form);
    include(view, out);
%>
