<%
/*
 * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.flow.reports.FilterFlowReport" %>
<%@ page import="org.labkey.flow.reports.PositivityFlowReport" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.controllers.ReportsController.ExecuteForm" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.flow.data.ICSMetadata" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ViewContext context = getViewContext();
    Container c = context.getContainer();

    Pair<ExecuteForm, FlowReport> bean = (Pair<ExecuteForm, FlowReport>)getModelBean();
    ExecuteForm form = bean.first;
    FlowReport report = bean.second;
%>
<labkey:errors/>

<form id="confirmExecuteReport" action="<%=new ActionURL(ReportsController.ExecuteAction.class, c).addParameter("reportId", report.getReportId().toString()).addParameter("confirm", "true")%>" method="POST">
    The <%=h(report.getDescriptor().getReportName())%> runs in the background and will save results to the database when finished.
    <p>
    Execute the background report?
    <p>
    <%= generateSubmitButton("Execute Report") %>
    <%= form.getReturnUrl() == null || form.getReturnUrl().isEmpty()? generateButton("Cancel", "begin.view") : generateButton("Cancel", form.getReturnUrl())%>
</form>

