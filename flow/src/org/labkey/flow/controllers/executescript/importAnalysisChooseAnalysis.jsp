<%
    /*
    * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="java.io.File" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isAdministrator() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>

<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<input type="hidden" name="runFilePathRoot" id="runFilePathRoot" value="<%=h(form.getRunFilePathRoot())%>">
<input type="hidden" name="selectAnalysisEngine" id="selectAnalysisEngine" value="<%=h(form.getSelectAnalysisEngine())%>">

<%--
<% for (int i = 0; form.getEngineOptionFilterKeyword() != null && i < form.getEngineOptionFilterKeyword().length; i++) { %>
<input type="hidden" name="engineOptionFilterKeyword" value="<%=h(form.getEngineOptionFilterKeyword()[i])%>">
<input type="hidden" name="engineOptionFilterOp" value="<%=h(form.getEngineOptionFilterOp()[i])%>">
<input type="hidden" name="engineOptionFilterValue" value="<%=h(form.getEngineOptionFilterValue()[i])%>">
<% } %>
--%>

<input type="hidden" name="importGroupNames" value="<%=h(form.getImportGroupNames())%>"/>
<input type="hidden" name="rEngineNormalization" value="<%=h(form.isrEngineNormalization())%>"/>
<input type="hidden" name="rEngineNormalizationReference" value="<%=h(form.getrEngineNormalizationReference())%>"/>
<input type="hidden" name="rEngineNormalizationParameters" value="<%=h(form.getrEngineNormalizationParameters())%>"/>

<p>The statistics in this workspace that have been calculated by FlowJo will be imported to <%=FlowModule.getLongProductName()%>.<br><br>
    <%=FlowModule.getLongProductName()%> organizes results into different "analysis folders".  The same set of FCS file should only
    be analyzed once in a given analysis folder.  If you want to analyze the same FCS file in two different ways,
    those results should be put into different analysis folders.</p>
<hr/>
<%
    FlowExperiment[] analyses = FlowExperiment.getAnalyses(container);
    FlowExperiment firstNonDisabledAnalysis = null;
    Map<Integer, String> disabledAnalyses = new HashMap<Integer, String>();
    if (pipeRoot != null)
    {
        File runFilePathRoot = null;
        if (form.getRunFilePathRoot() != null)
        {
            runFilePathRoot = pipeRoot.resolvePath(form.getRunFilePathRoot());
        }
        else if (form.getExistingKeywordRunId() > 0)
        {
            FlowRun keywordsRun = FlowRun.fromRunId(form.getExistingKeywordRunId());
            runFilePathRoot = new File(keywordsRun.getPath());
        }

        if (runFilePathRoot != null)
        {
            String relativeRunFilePathRoot = pipeRoot.relativePath(runFilePathRoot);
            for (FlowExperiment analysis : analyses)
            {
                try
                {
                    if (analysis.hasRun(runFilePathRoot, null))
                        disabledAnalyses.put(analysis.getExperimentId(), "The '" + analysis.getName() + "' analysis folder already contains the FCS files from '" + relativeRunFilePathRoot + "'.");
                    else if (firstNonDisabledAnalysis == null)
                        firstNonDisabledAnalysis = analysis;
                }
                catch (SQLException _) { }
            }
        }
    }

    String newAnalysisName = form.getNewAnalysisName();
    if (StringUtils.isEmpty(newAnalysisName))
    {
        Set<String> namesInUse = new HashSet<String>();
        for (FlowExperiment analysis : analyses)
            namesInUse.add(analysis.getName().toLowerCase());

        String baseName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
        newAnalysisName = baseName;
        int nameIndex = 0;
        while (namesInUse.contains(newAnalysisName.toLowerCase()))
        {
            nameIndex++;
            newAnalysisName = baseName+nameIndex;
        }
    }
%>
<div style="padding-left: 2em; padding-bottom: 1em;">
    <% if (analyses.length == 0 || analyses.length == disabledAnalyses.size()) { %>
    What do you want to call the new analysis folder?  You will be able to use this name for multiple uploaded workspaces.<br><br>
    <input type="text" name="newAnalysisName" value="<%=h(newAnalysisName)%>">
    <input type="hidden" name="createAnalysis" value="true"/>
    <% } else { %>
    <p>
    <table>
        <tr>
            <td valign="top">
                <input type="radio" id="chooseExistingAnalysis" name="createAnalysis" value="false" checked="true">
            </td>
            <td>
                <label for="chooseExistingAnalysis">Choose an analysis folder to put the results into:</label>
                <br>
                <select name="existingAnalysisId" onfocus="document.forms.importAnalysis.chooseExistingAnalysis.checked = true;">
                    <%
                        FlowExperiment recentAnalysis = FlowExperiment.getMostRecentAnalysis(container);
                        int selectedId = 0;
                        if (firstNonDisabledAnalysis != null)
                            selectedId = firstNonDisabledAnalysis.getExperimentId();
                        if (recentAnalysis != null && !disabledAnalyses.containsKey(recentAnalysis.getExperimentId()))
                            selectedId = recentAnalysis.getExperimentId();

                        for (FlowExperiment analysis : analyses)
                        {
                            String disabledReason = disabledAnalyses.get(analysis.getExperimentId());
                    %>
                    <option value="<%=h(analysis.getExperimentId())%>"
                            <%=disabledReason == null && analysis.getExperimentId() == selectedId ? "selected":""%>
                            <%=disabledReason != null ? "disabled=\"disabled\" title=\"" + h(disabledReason) + "\"":""%>>
                        <%=h(analysis.getName())%>
                    </option>
                    <%
                        }
                    %>
                </select>
                <br><br>
            </td>
        </tr>
        <tr>
            <td valign="top">
                <input type="radio" id="chooseNewAnalysis" name="createAnalysis" value="true">
            </td>
            <td>
                <label for="chooseNewAnalysis">Create a new analysis folder:</label>
                <br>
                <input type="text" name="newAnalysisName" value="<%=h(newAnalysisName)%>" onfocus="document.forms.importAnalysis.chooseNewAnalysis.checked = true;">
                <br><br>
            </td>
        </tr>
    </table>
    <% } %>
</div>

