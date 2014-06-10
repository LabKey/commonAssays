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
<%@ page import="org.labkey.flow.data.FlowDataObject" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.AttributeForm form = (AttributeController.AttributeForm)getModelBean();
    org.labkey.flow.persist.AttributeCache.Entry entry = form.getEntry();

    AttributeCache.Entry aliased = entry.getAliasedEntry();
    Collection<AttributeCache.Entry> aliases = entry.getAliases();
    Collection<FlowDataObject> usages = entry.getUsages();;

    ActionURL detailsURL = getActionURL();
%>

<% if (aliased != null) { %>
<b>Alias of:</b><br>
<a href='<%=h(detailsURL.clone().replaceParameter(AttributeController.Param.rowId.name(), String.valueOf(aliased.getRowId())))%>'><%=h(aliased.getName())%></a>
<p>
<% } %>

<% if (!aliases.isEmpty()) { %>
<b>Aliases:</b>
<ul>
    <% for (org.labkey.flow.persist.AttributeCache.Entry alias : aliases) { %>
    <li><a href='<%=detailsURL.clone().replaceParameter(AttributeController.Param.rowId.name(), String.valueOf(alias.getRowId()))%>'><%=h(alias.getName())%></a> </li>
    <% } %>
</ul>
<p>
<% } %>

<b>Usages:</b>
<% if (usages == null || usages.isEmpty()) { %>
<div class="labkey-error">no usages</div>
<% } else { %>
<ul>
    <% for (FlowDataObject usage : usages) { %>
    <li><a href='<%=usage.urlShow()%>'><%=h(usage.getLabel())%></a></li>
    <% } %>
</ul>
<% } %>

