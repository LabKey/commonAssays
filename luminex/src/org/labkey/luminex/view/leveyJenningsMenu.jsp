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
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    LeveyJenningsMenuView me = HttpView.currentView();
    LeveyJenningsMenuView.Bean bean = me.getModelBean();
%>

<p style="padding-top: 10px">
    Click on a titration or single point control to view its Levey-Jennings report.
    Click on an antigen to filter the Titrations and Single Point Controls lists to those used with that antigen.
</p>
<div style="display: flex; gap: 30px; align-items: flex-start">
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr>
        <th scope="col" style="padding: 5px; vertical-align: top; text-align: left" class="labkey-column-header">
            <strong>Titrations</strong>
        </th>
        <th scope="col" style="padding: 5px; vertical-align: top; text-align: left" class="labkey-column-header">
            <strong>Single Point Controls</strong>
        </th>
    </tr>
    <tr>
        <td style="padding: 5px; vertical-align: top">
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
        <td style="padding: 5px; vertical-align: top">
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
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr>
        <th scope="col" style="padding: 5px; vertical-align: top; text-align: left" class="labkey-column-header">
            <strong>Antigens</strong>
        </th>
    </tr>
    <tr>
        <td style="padding: 5px; vertical-align: top">
            <% if (bean.getAnalyte() != null) {
                ActionURL clearUrl = new ActionURL(LuminexController.LeveyJenningsMenuAction.class, getContainer());
                clearUrl.addParameter("rowId", bean.getProtocol().getRowId());
            %>
                Selected: <strong><%=h(bean.getAnalyte())%></strong> <%=iconLink("fa fa-times", "Clear selection", clearUrl)%> <%
            }
            else if (bean.getAnalytes().isEmpty()) { %>
                <em>No antigens</em> <%
            }
            else
            {
                for (String analyte : bean.getAnalytes())
                {
                    ActionURL url = new ActionURL(LuminexController.LeveyJenningsMenuAction.class, getContainer());
                    url.addParameter("rowId", bean.getProtocol().getRowId());
                    url.addParameter("analyte", analyte);
                    %><%= link(analyte, url) %><br/><%
                }
            }%>
        </td>
    </tr>
</table>
</div>