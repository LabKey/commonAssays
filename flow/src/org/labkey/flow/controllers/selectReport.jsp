<%
/*
 * Copyright (c) 2009 LabKey Corporation
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
<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="org.labkey.api.reports.report.ReportIdentifier" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = context.getUser();
    Container c = context.getContainer();

    ReportIdentifier id = ((ReportsController.IdForm) HttpView.currentModel()).getReportId();

    ReportService.I svc = ReportService.get();
    Report[] all = svc.getReports(user, c);
    TreeMap<String,Report> reports = new TreeMap<String,Report>(String.CASE_INSENSITIVE_ORDER);
    for (Report r : all)
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        reports.put(r.getDescriptor().getReportName(), r);
    }
    %><select onchange="Select_onChange(this.value)"><%
    for (Report r : reports.values())
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        ReportDescriptor d = r.getDescriptor();
        boolean selected = id != null && id.equals(d.getReportId());
        %><option <%=selected?"selected":""%> value="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></option><%
    }
    %></select>
<script type="text/javascript">
    function Select_onChange(url)
    {
        Ext.getBody().mask();
        window.location=url;
    }
</script>