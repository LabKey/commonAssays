<%
    /*
    * Copyright (c) 2011-2012 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.persist.AnalysisSerializer" %>
<%@ page import="java.util.EnumMap" %>
<%@ page import="org.labkey.flow.view.ExportAnalysisForm" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<ExportAnalysisForm> me = (JspView<ExportAnalysisForm>) HttpView.currentView();
    ExportAnalysisForm bean = me.getModelBean();
    Container container = getViewContext().getContainer();
    
    Map<AnalysisSerializer.Options, String> exportFormats = new EnumMap<>(AnalysisSerializer.Options.class);
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySample, "Grouped by sample");
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySamplePopulation, "Grouped by sample and population");
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySamplePopulationParameter, "Grouped by sample, popuplation, and parameter");
    exportFormats.put(AnalysisSerializer.Options.FormatRowPerStatistic, "One row per sample and statistic");

    boolean renderForm = bean._renderForm;
    String selectionType = bean.getSelectionType() == null ? "runs" : bean.getSelectionType();
    ActionURL exportURL = urlFor(RunController.ExportAnalysis.class).addParameter("selectionType", selectionType);
%>
<script>
    function toggleShortNames(checked)
    {
        var checkboxes = document.getElementsByName("useShortStatNames");
        for (var i = 0; i < checkboxes.length; i++)
        {
            checkboxes[i].disabled = !checked;
        }
    }
</script>

<% if (renderForm) { %>
    <form action='<%=exportURL%>' method='POST'>
<% } %>

<table class="labkey-export-tab-contents">
    <tr>
        <td class="labkey-export-tab-options">Include:</td>
        <td class="labkey-export-tab-options">
            <input id="includeFCSFiles" name="includeFCSFiles" type="checkbox" /> <label for="includeFCSFiles">FCS Files</label><br>
            <input id="includeGraphs" name="includeGraphs" type="checkbox" checked /> <label for="includeGraphs">Graphs</label><br>
            <input id="includeCompensation" name="includeCompensation" type="checkbox" checked /> <label for="includeCompensation">Compensation</label>
        </td>
        <td class="labkey-export-tab-options" style="padding-left:2em;">
            <input id="includeKeywords" name="includeKeywords" type="checkbox" /> <label for="includeKeywords">Keywords</label><br>
            <input id="includeStatistics" name="includeStatistics" type="checkbox" checked onchange="toggleShortNames(this.checked)" /> <label for="includeStatistics">Statistics</label><br>
            &nbsp;&nbsp;&nbsp;&nbsp;<span style="font-size: smaller;">(<input id="useShortStatNames" name="useShortStatNames" type="checkbox" checked /> <label for="useShortStatNames">Short stat names</label>)</span>
        </td>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td>TSV Format:</td>
        <td colspan=2>
            <select name="exportFormat">
                <labkey:options value="<%=bean.getExportFormat()%>" map="<%=exportFormats%>" />
            </select>
        </td>
    </tr>
    <tr>
        <td></td>
        <td>
            <% if (bean.getRunId() != null || bean.getWellId() != null) { %>
            <%=PageFlowUtil.generateSubmitButton("Export", null, "rel='nofollow'")%>
            <% } else { %>
            <%=PageFlowUtil.generateSubmitButton("Export", "return verifySelected(this.form, '" + exportURL + "', 'POST', " + q(selectionType) + ")", "rel='nofollow'")%>
            <% } %>
        </td>
    </tr>
</table>

<%
    if (bean.getRunId() != null)
    {
        for (int runId : bean.getRunId())
        {
            %><input type="hidden" name="runId" value="<%= runId %>" /><%
        }
    }
    if (bean.getWellId() != null)
    {
        for (int wellId : bean.getWellId())
        {
            %><input type="hidden" name="wellId" value="<%= wellId %>" /><%
        }
    }
%>

<% if (renderForm) { %>
    </form>
<% } %>


