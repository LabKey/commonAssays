<%
/*
 * Copyright (c) 2013-2026 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.luminex.LeveyJenningsForm" %>
<%@ page import="org.labkey.luminex.LeveyJenningsMenuView" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    LeveyJenningsMenuView me = HttpView.currentView();
    LeveyJenningsMenuView.Bean bean = me.getModelBean();
%>

<p style="padding-top: 10px">
    Click on an antigen to filter the Titrations and Single Point Controls lists below to those used with that antigen.
</p>
<%
    List<String> analytes = bean.getAnalytes();
    int analytesPerColumn = 8;
    int analyteColumnCount = Math.max(1, (analytes.size() + analytesPerColumn - 1) / analytesPerColumn);
%>
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr>
        <th scope="col" colspan="<%=analyteColumnCount%>" style="padding: 5px; vertical-align: top; text-align: left" class="labkey-column-header">
            <strong>Antigens</strong>
        </th>
    </tr>
    <tr>
        <% if (bean.getAnalyte() != null) {
            ActionURL clearUrl = new ActionURL(LuminexController.LeveyJenningsMenuAction.class, getContainer());
            clearUrl.addParameter("rowId", bean.getProtocol().getRowId());
        %>
        <td style="padding: 5px; vertical-align: top">
            Selected: <strong><%=h(bean.getAnalyte())%></strong> <%=iconLink("fa fa-times", "Clear selection", clearUrl)%>
        </td>
        <% } else if (analytes.isEmpty()) { %>
        <td style="padding: 5px; vertical-align: top"><em>No antigens</em></td>
        <% } else {
            for (int col = 0; col < analytes.size(); col += analytesPerColumn)
            { %>
        <td style="padding: 5px; vertical-align: top"><%
                for (String analyte : analytes.subList(col, Math.min(col + analytesPerColumn, analytes.size())))
                {
                    ActionURL url = new ActionURL(LuminexController.LeveyJenningsMenuAction.class, getContainer());
                    url.addParameter("rowId", bean.getProtocol().getRowId());
                    url.addParameter("analyte", analyte);
                    %><%= link(analyte, url) %><br/><%
                }%>
        </td>
            <% }
        } %>
    </tr>
</table>

<p style="padding-top: 10px">
    Click on a titration or single point control to view its Levey-Jennings report.
</p>
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr>
        <td valign="top" style="padding: 5px" class="labkey-column-header">
            <strong>Titrations</strong>
        </td>
        <td valign="top" style="padding: 5px" class="labkey-column-header">
            <strong>Single Point Controls</strong>
        </td>
    </tr>
    <tr>
        <td valign="top" style="padding: 5px" >
            <% if (bean.getTitrations().isEmpty()) { %>
                <em>No titrations</em> <%
            }
            for (String titrationName : bean.getTitrations())
            {
                ActionURL url = new ActionURL(LuminexController.LeveyJenningsReportAction.class, getContainer());
                url.addParameter("rowId", bean.getProtocol().getRowId());
                url.addParameter("controlName", titrationName);
                url.addParameter("controlType", LeveyJenningsForm.ControlType.Titration.toString());
                if (bean.getAnalyte() != null)
                    url.addParameter("analyte", bean.getAnalyte());
                %><%= link(titrationName, url) %><br/><%
            }%>
        </td>
        <td valign="top" style="padding: 5px" >
            <% if (bean.getSinglePointControls().isEmpty()) { %>
                <em>No single point controls</em> <%
            }
            for (String singlePointControl : bean.getSinglePointControls())
            {
                ActionURL url = new ActionURL(LuminexController.LeveyJenningsReportAction.class, getContainer());
                url.addParameter("rowId", bean.getProtocol().getRowId());
                url.addParameter("controlName", singlePointControl);
                url.addParameter("controlType", LeveyJenningsForm.ControlType.SinglePoint.toString());
                if (bean.getAnalyte() != null)
                    url.addParameter("analyte", bean.getAnalyte());
                %><%= link(singlePointControl, url) %><br/><%
            }%>
        </td>
    </tr>
</table>