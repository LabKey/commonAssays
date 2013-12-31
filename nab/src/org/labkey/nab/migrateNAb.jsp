<%
/*
 * Copyright (c) 2012 LabKey Corporation
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
<%@ page import="org.labkey.api.data.DataRegion"%>
<%@ page import="org.labkey.api.data.DisplayColumn"%>
<%@ page import="org.labkey.api.data.RenderContext" %>
<%@ page import="org.labkey.api.exp.api.ExpProtocol" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<Map<ExpProtocol, List<DisplayColumn>>> me = (JspView<Map<ExpProtocol, List<DisplayColumn>>>) HttpView.currentView();
    Map<ExpProtocol, List<DisplayColumn>> protocols = me.getModelBean();
    ViewContext context = getViewContext();
%>
<p style="width: 500px">
    This will migrate NAb runs created by the legacy implementation into the newer assay-based representation. It will
    also re-copy any data rows that have already been copied to a study dataset. It will not delete any legacy NAb
    runs, nor will it delete any existing legacy NAb study datasets.
</p>
<p>
<% if (protocols.isEmpty()) { %>
    No NAb assay designs found.
<%
}
else
{
    RenderContext renderContext = new RenderContext(context);
    renderContext.setMode(DataRegion.MODE_INSERT);
    for (Map.Entry<ExpProtocol, List<DisplayColumn>> entry : protocols.entrySet())
    {
        ExpProtocol protocol = entry.getKey();
        List<DisplayColumn> columns = entry.getValue(); %>
            <h2><%= h(protocol.getName()) %></h2>
            <form method="POST">
                <input type="hidden" name="protocolId" value="<%= protocol.getRowId()%> " />
                <table>
                    <% for (DisplayColumn column : columns)
                    { %>
                    <tr>
                        <% column.renderDetailsCaptionCell(renderContext, out); %>
                        <% column.renderInputCell(renderContext, out, 1); %>
                    </tr><%

                    } %>
                </table>
                <labkey:button text="Migrate" />
            </form>
        <hr/>
    <% } %>
<% }%>
