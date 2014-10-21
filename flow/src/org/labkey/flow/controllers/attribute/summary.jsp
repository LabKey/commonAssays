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
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.FlowManager.FlowEntry" %>
<%@ page import="org.labkey.flow.data.AttributeType" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.AttributeForm form = (AttributeController.AttributeForm)getModelBean();
    AttributeType type = form.getAttributeType();

    Map<FlowEntry, Collection<FlowEntry>> aliasMap = FlowManager.get().getAliases(getContainer(), type);
    ActionURL summaryURL = getActionURL();

    ActionURL editURL = new ActionURL(AttributeController.EditAction.class, getContainer()).addParameter(AttributeController.Param.type, type.name()).addReturnURL(summaryURL);
    ActionURL detailsURL = new ActionURL(AttributeController.DetailsAction.class, getContainer()).addParameter(AttributeController.Param.type, type.name()).addReturnURL(summaryURL);
    ActionURL deleteURL = new ActionURL(AttributeController.DeleteAction.class, getContainer()).addParameter(AttributeController.Param.type, type.name()).addReturnURL(summaryURL);
    ActionURL makePrimaryURL = new ActionURL(AttributeController.MakePrimaryAction.class, getContainer()).addParameter(AttributeController.Param.type, type.name()).addReturnURL(summaryURL);
    ActionURL aliasURL = new ActionURL(AttributeController.CreateAliasAction.class, getContainer()).addParameter(AttributeController.Param.type, type.name()).addReturnURL(summaryURL);
%>

<table>

    <%
        for (Map.Entry<FlowEntry, Collection<FlowEntry>> entry : aliasMap.entrySet())
        {
            FlowEntry primary = entry.getKey();
            Collection<FlowEntry> aliases = entry.getValue();

            Map<Integer, Number> counts = FlowManager.get().getUsageCount(primary._type, primary._rowId);
            long totalCount = 0;
            for (Number count : counts.values())
                totalCount += count.longValue();

            Number primaryUsages = counts.get(primary._rowId);
    %>
    <tr>
        <td>
            <%=h(primary._name)%>
        </td>
        <td>
            <% if (primaryUsages != null && primaryUsages.intValue() > 0) { %>
            <i>(<a href='<%=detailsURL.clone().addParameter(AttributeController.Param.rowId, primary._rowId)%>'><%=primaryUsages%> usages</a>)</i>
            <% } else { %>
            <i class="labkey-error">(unused)</i>
            <% } %>
        </td>
        <td>
            <labkey:link href='<%=editURL.clone().addParameter(AttributeController.Param.rowId, primary._rowId)%>' text="edit"/>
            <%--<% if (totalCount == 0) { %>--%>
            <%--<labkey:link href='<%=deleteURL.clone().addParameter(AttributeController.Param.rowId, primary._rowId)%>' text="delete"/>--%>
            <%--<% } %>--%>
        </td>
    </tr>
        <%
            for (FlowEntry alias : aliases)
            {
                Number usages = counts.get(alias._rowId);
        %>
    <tr>
        <td style="padding-left: 1.5em;">
            <%=h(alias._name)%>
        </td>
        <td>
            <% if (usages != null && usages.intValue() > 0) { %>
            <i>(<a href='<%=detailsURL.clone().addParameter(AttributeController.Param.rowId, alias._rowId)%>'><%=usages%> usages</a>)</i>
            <% } else { %>
            <i class="labkey-error">(unused)</i>
            <% } %>
        </td>
        <td>
            <labkey:link href='<%=editURL.clone().addParameter(AttributeController.Param.rowId, alias._rowId)%>' text="edit"/>
            <%--<labkey:link href='<%=deleteURL.clone().addParameter(AttributeController.Param.rowId, alias._rowId)%>' text="delete"/>--%>
            <%--<labkey:link href='<%=makePrimaryURL.clone().addParameter(AttributeController.Param.rowId, alias._rowId)%>' text="make primary"/>--%>
        </td>
    </tr>
        <%
            }
        %>
    <tr>
        <td style="padding-left: 1.5em;">
        </td>
        <td>
            <labkey:link href='<%=aliasURL.clone().addParameter(AttributeController.Param.rowId, primary._rowId)%>' text="create alias"/>
        </td>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <%
        }
    %>

</table>

