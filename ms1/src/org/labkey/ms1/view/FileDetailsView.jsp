<%
/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.api.ExpData" %>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.model.DataFile" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<DataFile> me = (JspView<DataFile>) HttpView.currentView();
    DataFile dataFile = me.getModelBean();
    ExpData expData = (null == dataFile ? null : dataFile.getExpData());

    ActionURL urlDownload = urlProvider(ExperimentUrls.class).getShowFileURL(getContainer());
%>
<% if (null != dataFile && null != expData) { %>
<table>
    <tr>
        <td>Data File:</td>
        <% urlDownload.addParameter("rowId", expData.getRowId()); %>
        <td><%=h(expData.getDataFileUrl())%>
            <%=textLink("download", urlDownload)%>
        </td>
    </tr>
    <tr>
        <td>Source MzXML:</td>
        <%
            urlDownload.deleteParameters();
            List<? extends ExpData> inputs = expData.getRun().getInputDatas(null, null);

            for (ExpData input : inputs)
            {
                if (input.getDataFileUrl() != null && input.getDataFileUrl().equalsIgnoreCase(dataFile.getMzXmlUrl()))
                    urlDownload.addParameter("rowId", input.getRowId());
            }
        %>
        <td><%=h(dataFile.getMzXmlUrl())%>
            <%=textLink("download", urlDownload)%> <% //TODO: "Open in msInspect" link? %>
        </td>
    </tr>
</table>
<% } %>