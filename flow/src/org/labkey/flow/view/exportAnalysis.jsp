<%
/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.module.ModuleLoader" %>
<%@ page import="org.labkey.api.module.ModuleProperty" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.persist.AnalysisSerializer" %>
<%@ page import="org.labkey.flow.view.ExportAnalysisForm" %>
<%@ page import="java.util.EnumMap" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<ExportAnalysisForm> me = (JspView<ExportAnalysisForm>) HttpView.currentView();
    ExportAnalysisForm bean = me.getModelBean();

    Map<AnalysisSerializer.Options, String> exportFormats = new EnumMap<>(AnalysisSerializer.Options.class);
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySample, "Grouped by sample");
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySamplePopulation, "Grouped by sample and population");
    exportFormats.put(AnalysisSerializer.Options.FormatGroupBySamplePopulationParameter, "Grouped by sample, popuplation, and parameter");
    exportFormats.put(AnalysisSerializer.Options.FormatRowPerStatistic, "One row per sample and statistic");

    Map<ExportAnalysisForm.SendTo, String> sendToOptions = new EnumMap<>(ExportAnalysisForm.SendTo.class);
    sendToOptions.put(ExportAnalysisForm.SendTo.Browser, "Browser");
    sendToOptions.put(ExportAnalysisForm.SendTo.PipelineZip, "Pipeline root as zip");
    sendToOptions.put(ExportAnalysisForm.SendTo.PipelineFiles, "Pipeline root as files");

    FlowModule module = ModuleLoader.getInstance().getModule(FlowModule.class);
    if (module.getExportToScriptCommandLine(getContainer()) != null)
        sendToOptions.put(ExportAnalysisForm.SendTo.Script, "External script as files");

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

<table class="lk-fields-table">
    <tr>
        <td class="labkey-export-tab-options" valign="top">Include:</td>
        <td class="labkey-export-tab-options">
            <input id="includeFCSFiles" name="includeFCSFiles" type="checkbox" <%=checked(bean.isIncludeFCSFiles())%> /> FCS Files<br>
            <input id="includeGraphs" name="includeGraphs" type="checkbox" <%=checked(bean.isIncludeGraphs())%> /> Graphs<br>
            <input id="includeCompensation" name="includeCompensation" type="checkbox" <%=checked(bean.isIncludeCompensation())%> /> Compensation
        </td>
        <td class="labkey-export-tab-options" style="padding-left:2em;">
            <input id="includeKeywords" name="includeKeywords" type="checkbox" <%=checked(bean.isIncludeKeywords())%> /> Keywords<br>
            <input id="includeStatistics" name="includeStatistics" type="checkbox" <%=checked(bean.isIncludeStatistics())%> onchange="toggleShortNames(this.checked)" /> Statistics<br>
            <span style="font-size: smaller; padding-left: 20px;"><input id="useShortStatNames" name="useShortStatNames" type="checkbox" <%=checked(bean.isUseShortStatNames())%> /> Short stat name</span>
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
        <td>Export to:</td>
        <td colspan=2>
            <select name="sendTo">
                <labkey:options value="<%=bean.getSendTo()%>" map="<%=sendToOptions%>" />
            </select>
        </td>
    </tr>
    <tr>
        <td></td>
        <td>
            <% if (bean.getRunId() != null || bean.getWellId() != null) { %>
            <%= button("Export").submit(true).attributes("rel='nofollow'")%>
            <% } else { %>
            <%= button("Export").submit(true).onClick("return verifySelected(this.form, '" + exportURL + "', 'POST', " + q(selectionType) + ")").attributes("rel='nofollow'") %>
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


