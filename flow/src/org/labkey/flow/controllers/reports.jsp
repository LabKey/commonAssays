<%
/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="org.labkey.flow.reports.ControlsQCReport" %>
<%@ page import="org.labkey.flow.reports.PositivityFlowReport" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = context.getUser();
    Container c = context.getContainer();

    boolean canEdit = c.hasPermission(user, UpdatePermission.class);
    ActionURL copyURL = new ActionURL(ReportsController.CopyAction.class, c);

    ReportService.I svc = ReportService.get();
    Report[] all = svc.getReports(user, c);
    TreeMap<String, Report> reports = new CaseInsensitiveTreeMap<Report>();

    for (Report r : all)
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        reports.put(r.getDescriptor().getReportName(), r);
    }

    %><table><%
    for (Report r : reports.values())
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        ReportDescriptor d = r.getDescriptor();
        String id = d.getReportId().toString();
        ActionURL editURL = r.getEditReportURL(context);
        copyURL.replaceParameter("reportId", id);
        String description = d.getReportDescription();
        %><tr>
        <td><a href="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></a></td>
        <td><%=h(description)%></td><%
        if (canEdit){
        %><td><%=generateButton("edit", editURL)%></td>
        <td><form id="form<%=h(id)%>" method=POST action="<%=h(copyURL.getLocalURIString())%>"><%=generateSubmitButton("copy")%></form></td>
        <%}%>
        </tr><%
    }
    %>
    <tr><td><%=textLink("create qc report", new ActionURL(ReportsController.UpdateAction.class, c).addParameter(ReportDescriptor.Prop.reportType, ControlsQCReport.TYPE))%></td></tr>
    <tr><td><%=textLink("create positivity report", new ActionURL(ReportsController.UpdateAction.class, c).addParameter(ReportDescriptor.Prop.reportType, PositivityFlowReport.TYPE))%></td></tr>
    </table>
