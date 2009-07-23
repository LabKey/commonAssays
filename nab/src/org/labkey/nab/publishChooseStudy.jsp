<%
/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.PublishBean> me = (JspView<NabController.PublishBean>) HttpView.currentView();
    NabController.PublishBean bean = me.getModelBean();
%>
<form action="publish.view" method="GET">
    <input type="hidden" name="plateIds" value="<%= bean.isPlateIds() %>">
    <%
        for (Integer plateId : bean.getIds())
        {
    %>
        <input type="hidden" name="id" value="<%= plateId %>">
    <%
        }
    %>
    <table>
        <tr>
            <td>Choose Target Study:</td>
            <td>
                <select name="targetContainerId">
                <%
                    for (Map.Entry<Container, String> entry : bean.getValidTargets().entrySet())
                    {
                %>
                    <option value="<%= h(entry.getKey().getId()) %>"><%= h(entry.getKey().getPath()) %> (<%= h(entry.getValue()) %>)</option>
                <%
                    }
                %>
                </select>
            </td>
        </tr>
    </table>
     <%= generateSubmitButton("Next") %> <%= generateButton("Cancel", "begin.view") %>
</form>