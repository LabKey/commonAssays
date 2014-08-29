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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="org.labkey.luminex.LuminexController.DefaultValuesForm" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("dataviews"));
        return resources;
    }
%>
<%
    JspView<DefaultValuesForm> me = (JspView<DefaultValuesForm>) HttpView.currentView();
    DefaultValuesForm bean = me.getModelBean();
    List<String> analytes = bean.getAnalytes();
    List<String> positivityThresholds = bean.getPositivityThresholds();
    List<String> negativeBeads = bean.getNegativeBeads();
%>
<labkey:form action="<%=getViewContext().getActionURL()%>" method="post">
    <p>Update default values:</p>
    <table>
        <tr>
            <th><div class="labkey-form-label">Analyte</div></th>
            <th><div class="labkey-form-label">Positivity Threshold</div></th>
            <th><div class="labkey-form-label">Negative Bead</div></th>
        </tr>

        <% for (int i=0; i<analytes.size(); i++ ) { %>
        <tr>
            <td><input name="analytes" value="<%=h(analytes.get(i))%>" size=30></td>
            <td><input name="positivityThresholds" value="<%=h(positivityThresholds.get(i))%>" size=20></td>
            <td><input name="negativeBeads" value="<%=h(negativeBeads.get(i))%>" size=30></td>
        </tr>
        <% } %>
    </table>
    <br>
    <table>
        <tr>
            <td><%= button("Save Defaults").submit(true) %></td>
            <td><%= button("Cancel").submit(true) %></td>
            <td><%= button("Upload TSV").href(new ActionURL(LuminexController.ImportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()).addReturnURL(getViewContext().getActionURL()))%></td>
            <td><%= button("Export TSV").href(new ActionURL(LuminexController.ExportDefaultValuesAction.class, getContainer()).addParameter("rowId", bean.getProtocol().getRowId()))%></td>
        </tr>
    </table>
</labkey:form>
