<%
/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.reports.report.ReportIdentifier" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.flow.reports.FlowReportManager" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4ClientApi"); // needed for mask
    }
%>
<%
    ViewContext context = getViewContext();
    User user = getUser();
    Container c = getContainer();

    boolean canEdit = c.hasPermission(user, UpdatePermission.class);

    ReportIdentifier id = ((ReportsController.IdForm) HttpView.currentModel()).getReportId();

    Collection<FlowReport> reports = FlowReportManager.getFlowReports(c, user);
    %><p><select onchange="Select_onChange(this.value)"><%
    for (FlowReport r : reports)
    {
        ReportDescriptor d = r.getDescriptor();
        boolean selected = id != null && id.equals(d.getReportId());
        %><option<%=selected(selected)%> value="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></option><%
    }
    %></select>

    <% if (canEdit && id != null) { %>
    <%= button("Edit").href(id.getReport(context).getEditReportURL(context)) %>
    <% } %>
    </p>

<script type="text/javascript">
    function Select_onChange(url)
    {
        Ext4.getBody().mask();
        window.location=url;
    }
</script>
