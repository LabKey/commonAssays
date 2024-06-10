<%
/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.data.SimpleFilter" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Controller.PeptideChartsAction" %>
<%@ page import="org.labkey.ms2.protein.tools.GoHelpers.GoTypes" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.GoChartBean bean = ((JspView<MS2Controller.GoChartBean>) HttpView.currentView()).getModelBean();
%>
<% for (Map.Entry<String, SimpleFilter> entry : bean.filterInfos.entrySet()) { %>
    <%= h(entry.getKey()) %>: <%= entry.getValue().getFilterText().isEmpty() ? unsafe("<em>No filters applied</em>") : h(entry.getValue().getFilterText()) %><br/>
<% } %>

<% if (bean.foundData) { %>
    <labkey:form name="chartForm" action="<%=urlFor(PeptideChartsAction.class)%>">
    <%=bean.imageMap%>
    <table align="left">
    <tr>
        <td>
            <table>
                <tr>
                    <td valign="middle">Chart type:</td>
                    <td valign="middle">
                        <select name="chartType" id="chartType">
                            <option value="<%=h(GoTypes.CELL_LOCATION)%>"<%=selected(GoTypes.CELL_LOCATION == bean.goChartType)%>><%=h(GoTypes.CELL_LOCATION)%></option>
                            <option value="<%=h(GoTypes.FUNCTION)%>"<%=selected(GoTypes.FUNCTION == bean.goChartType)%>><%=h(GoTypes.FUNCTION)%></option>
                            <option value="<%=h(GoTypes.PROCESS)%>"<%=selected(GoTypes.PROCESS == bean.goChartType)%>><%=h(GoTypes.PROCESS)%></option>
                        </select>
                    </td>
                    <td valign="middle"><%= button("Submit").submit(true) %></td>
                </tr>
            </table>
        </td>
    </tr>
    <tr><td>
        <img src="<%=h(bean.chartURL)%>" width="800" height="800" alt="GO Chart" usemap="#pie1">
        <input type="hidden" name="run" value="<%=bean.run.getRun()%>">
        <input type="hidden" name="queryString" value="<%=h(bean.queryString)%>">
        <input type="hidden" name="grouping" value="<%=h(bean.grouping)%>">
    </td></tr>
    </table>
    </labkey:form>

<% } else { %>
    No matching Gene Ontology annotations found. Be sure that you have loaded full gene annotations for the relevant proteins. 
<% } %>
