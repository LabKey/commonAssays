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
<%@ page import="org.labkey.api.collections.CaseInsensitiveTreeMap" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.NavTree" %>
<%@ page import="org.labkey.api.view.PopupMenu" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.reports.ControlsQCReport" %>
<%@ page import="org.labkey.flow.reports.PositivityFlowReport" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.ICSMetadata" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.flow.reports.FlowReportManager" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = getUser();
    Container c = getContainer();

    boolean canEdit = c.hasPermission(user, UpdatePermission.class);

    ActionURL currentURL = getActionURL();
    ActionURL copyURL = new ActionURL(ReportsController.CopyAction.class, c).addReturnURL(currentURL);
    ActionURL deleteURL = new ActionURL(ReportsController.DeleteAction.class, c).addReturnURL(currentURL);

    Collection<FlowReport> reports = FlowReportManager.getFlowReports(c, user);
    Map<String, List<FlowReport>> reportsByType = new TreeMap<>();

    for (FlowReport r : reports)
    {
        ReportDescriptor d = r.getDescriptor();
        String type = d.getReportType();

        List<FlowReport> rs = reportsByType.get(type);
        if (rs == null)
            reportsByType.put(type, rs = new ArrayList<>(reports.size()));

        rs.add(r);
    }
%>
<style>
table.reports td {
    padding-left: 15px;
}
</style>

<table class='reports lk-fields-table'>
<%
    for (List<FlowReport> rs : reportsByType.values())
    {
        if (rs.size() == 0)
            continue;

        %>
        <tr><td colspan='3' style='height:2.0em;vertical-align:bottom;padding-left:0px'><b><%=h(rs.get(0).getTypeDescription())%></b></td></tr>
        <%

        for (FlowReport r : rs)
        {
            ReportDescriptor d = r.getDescriptor();
            String id = d.getReportId().toString();
            ActionURL editURL = r.getEditReportURL(context);

            copyURL.replaceParameter("reportId", id);
            deleteURL.replaceParameter("reportId", id);

            String description = d.getReportDescription();
            %><tr>
            <td><a href="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></a></td>
            <td style='min-width:80px'><%=h(description == null ? " " : description)%></td>
            <%
            if (canEdit){
                NavTree navtree = new NavTree("manage", (String)null);
                navtree.addChild("Edit", editURL);
                navtree.addChild("Copy", copyURL);
                navtree.addChild("Delete", deleteURL);
                navtree.addChild("Execute", r.getRunReportURL(context).addParameter("confirm", true).addReturnURL(currentURL));
                PopupMenu menu = new PopupMenu(navtree, PopupMenu.Align.LEFT, PopupMenu.ButtonStyle.TEXT);
            %><td><% menu.render(out); %></td>
            <%}%>
            </tr><%
        }

    }

    %>
    </table>
    <p>
    <% if (canEdit) { %>
        <%= button("create qc report").href(new ActionURL(ReportsController.CreateAction.class, c).addParameter(ReportDescriptor.Prop.reportType, ControlsQCReport.TYPE)) %>
        <%= button("create positivity report").href(new ActionURL(ReportsController.CreateAction.class, c).addParameter(ReportDescriptor.Prop.reportType, PositivityFlowReport.TYPE)) %>
    <% } %>
