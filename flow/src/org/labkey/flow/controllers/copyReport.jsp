<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.controllers.ReportsController.CopyForm" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    Container c = getContainer();

    Pair<CopyForm, FlowReport> bean = (Pair<CopyForm, FlowReport>)getModelBean();
    CopyForm form = bean.first;
    FlowReport report = bean.second;

    ActionURL copyURL = new ActionURL(ReportsController.CopyAction.class, c).addParameter("reportId", report.getReportId().toString());
    if (form.getReturnUrl() != null)
        copyURL.addReturnURL(form.getReturnActionURL());
%>
<labkey:errors/>

<labkey:form id="copyReport" action="<%=copyURL%>" method="POST">
    What would you like to name the new report?
    <p>
    <input type="text" id="reportName" name="reportName" value="<%=h(form.getReportName())%>" size="50">
    <p>
    <%= button("Copy").submit(true) %>
    <%= form.getReturnUrl() == null || form.getReturnUrl().isEmpty()? button("Cancel").href(ReportsController.BeginAction.class, getContainer()) : button("Cancel").href(form.getReturnUrl()) %>
</labkey:form>
