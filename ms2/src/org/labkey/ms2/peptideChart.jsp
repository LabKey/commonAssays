<%
/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.ProteinDictionaryHelpers.GoTypes" %>
<%@ page import="org.labkey.api.data.SimpleFilter" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.GoChartBean bean = ((JspView<MS2Controller.GoChartBean>) HttpView.currentView()).getModelBean();
%>
<% for (Map.Entry<String, SimpleFilter> entry : bean.filterInfos.entrySet()) { %>
    <%= h(entry.getKey()) %>: <%= h(entry.getValue().getFilterText())%><br/>
<% } %>

<% if (bean.foundData) { %>
    <form name="chartForm" action="peptideCharts.view">
    <%=bean.imageMap%>
    <table align="left">
    <tr>
        <td>
            <table>
                <tr>
                    <td valign="middle">Chart type:</td>
                    <td valign="middle">
                        <select name="chartType" id="chartType">
                            <option value="<%=GoTypes.CELL_LOCATION%>"<%=GoTypes.CELL_LOCATION == bean.goChartType ? " selected" : ""%>><%=GoTypes.CELL_LOCATION%></option>
                            <option value="<%=GoTypes.FUNCTION%>"<%=GoTypes.FUNCTION == bean.goChartType ? " selected" : ""%>><%=GoTypes.FUNCTION%></option>
                            <option value="<%=GoTypes.PROCESS%>"<%=GoTypes.PROCESS == bean.goChartType ? " selected" : ""%>><%=GoTypes.PROCESS%></option>
                        </select>
                    </td>
                    <td valign="middle"><%=generateSubmitButton("Submit")%></td>
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
    </form>

<% } else { %>
    No matching Gene Ontology annotations found. Be sure that you have loaded full gene annotations for the relevant proteins. 
<% } %>
