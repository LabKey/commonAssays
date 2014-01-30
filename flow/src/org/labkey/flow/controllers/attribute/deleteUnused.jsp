<%
/*
 * Copyright (c) 2014 LabKey Corporation
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
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.labkey.flow.data.AttributeType" %>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.DeleteUnusedBean bean = (AttributeController.DeleteUnusedBean)getModelBean();
%>

<labkey:errors/>
<p>Are you sure you want to delete the unused attributes in this folder?</p>

<% if (!bean.unusedKeywords.isEmpty()) { %>
<p>Keywords:</p>
<ul>
    <% for (FlowManager.FlowEntry entry : bean.unusedKeywords) { %>
    <li><%=h(entry._name)%></li>
    <% } %>
</ul>
<% } %>

<% if (!bean.unusedStats.isEmpty()) { %>
<p>Statistics:</p>
<ul>
    <% for (FlowManager.FlowEntry entry : bean.unusedStats) { %>
    <li><%=h(entry._name)%></li>
    <% } %>
</ul>
<% } %>

<% if (!bean.unusedGraphs.isEmpty()) { %>
<p>Graphs:</p>
<ul>
<% for (FlowManager.FlowEntry entry : bean.unusedGraphs) { %>
    <li><%=h(entry._name)%></li>
<% } %>
</ul>
<% } %>

