<%
/*
 * Copyright (c) 2006-2010 LabKey Corporation
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
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    Map<String, Object> runProperties = bean.getRunDisplayProperties();
    int columnCount = 2;
%>
<table width="100%">
    <%
        Iterator<Map.Entry<String, Object>> propertyIt = runProperties.entrySet().iterator();
        Pair<String, Object>[] entries = new Pair[runProperties.size()];
        for (int i = 0; i < entries.length; i++)
        {
            Map.Entry<String, Object> entry = propertyIt.next();
            entries[i] = new Pair<String, Object>(entry.getKey(), entry.getValue());
        }

        int longestColumn = (int) Math.ceil(entries.length/2.0);
        for (int row = 0; row < longestColumn; row++)
        {
    %>
        <tr>
        <%
            for (int col = 0; col < columnCount; col++)
            {
                int index = col*longestColumn + row;
                if (index < entries.length)
                {
                    Pair<String, Object> property = index < entries.length ? entries[index] : null;
            %>
                <th style="text-align:left"><%= property != null ? h(property.getKey()) : "&nbsp;"  %></th>
                <td><%= property != null ? h(property.getValue()) : "&nbsp;"  %></td>
            <%
                }
            }
        %>
        </tr>
    <%
        }
    %>
</table>
