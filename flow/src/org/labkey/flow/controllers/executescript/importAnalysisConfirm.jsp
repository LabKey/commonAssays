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
<%@ page import="org.labkey.api.query.QueryParam" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.io.File" %>
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
<input type="hidden" name="createAnalysis" id="createAnalysis" value="<%=h(form.isCreateAnalysis())%>">

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

<% if (form.isCreateAnalysis()) { %>
<input type="hidden" name="newAnalysisName" id="newAnalysisName" value="<%=h(form.getNewAnalysisName())%>">
<% } else { %>
<input type="hidden" name="existingAnalysisId" id="existingAnalysisId" value="<%=h(form.getExistingAnalysisId())%>">
<% } %>

<p>You are about to import the analysis from the workspace with the following settings:</p>
<%
    FlowJoWorkspace workspace = form.getWorkspace().getWorkspaceObject();
%>
<ul>
    <li style="padding-bottom:0.5em;">
        <b>Analysis Engine:</b>
        <% if (form.getSelectAnalysisEngine() == null || form.getSelectAnalysisEngine().equals("noEngine")) { %>
            No analysis engine selected
        <% } else if (form.getSelectAnalysisEngine().equals("rEngine")) { %>
            External R analysis engine
        <% } %>
    </li>
    <li style="padding-bottom:0.5em;">
        <b>Import Groups:</b> <%=form.getImportGroupNames() == null ? "<em>All Samples</em>" : h(form.getImportGroupNames())%>
    </li>
    <% if ("rEngine".equals(form.getSelectAnalysisEngine()) && form.isrEngineNormalization()) { %>
    <li style="padding-bottom:0.5em;">
        <b>Normalization Options:</b>
        <table border="0" style="margin-left:1em;">
            <tr>
                <td><b>Reference Sample:</b></td>
                <td><%=h(form.getrEngineNormalizationReference())%></td>
            </tr>
            <tr>
                <td><b>Normalize Parameters:</b></td>
                <td>
                <% if (form.getrEngineNormalizationParameters() != null) { %>
                    <%=h(form.getrEngineNormalizationParameters())%>
                <% } else { %>
                    <em>All parameters</em>
                <% } %>
                </td>
            </tr>
        </table>
    </li>
    <% } %>
    <li style="padding-bottom:0.5em;">
        <% if (form.isCreateAnalysis()) { %>
        <b>New Analysis Folder:</b> <%=h(form.getNewAnalysisName())%>
        <% } else { %>
        <b>Existing Analysis Folder:</b>
        <% FlowExperiment experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId()); %>
        <a href="<%=experiment.urlShow()%>" target="_blank"><%=h(experiment.getName())%></a>
        <% } %>
    </li>
    <%
        FlowRun keywordRun = FlowRun.fromRunId(form.getExistingKeywordRunId());
        if (keywordRun != null) {
            String keywordRunPath = pipeRoot.relativePath(new File(keywordRun.getPath()));
    %>
    <li style="padding-bottom:0.5em;">
        <b>Existing FCS File run:</b>
        <a href="<%=keywordRun.urlShow().addParameter(QueryParam.queryName, FlowTableType.FCSFiles.toString())%>" target="_blank" title="Show FCS File run in a new window"><%=h(keywordRun.getName())%></a>
    </li>
    <li style="padding-bottom:0.5em;">
        <b>FCS File Path:</b> <%=h(keywordRunPath)%>
    </li>
    <%
    } else {
    %>
    <li style="padding-bottom:0.5em;">
        <b>Existing FCS File run:</b> <i>none set</i>
    </li>
    <li style="padding-bottom:0.5em;">
        <b>FCS File Path:</b>
        <% if (form.getRunFilePathRoot() == null) { %>
        <i>none set</i>
        <% } else { %>
        <%=h(form.getRunFilePathRoot())%>
        <% } %>
    </li>
    <%
        }
    %>
    <li style="padding-bottom:0.5em;">
        <%
            String name = form.getWorkspace().getPath();
            if (name == null)
                name = form.getWorkspace().getName();
        %>
        <b>Workspace:</b> <%=h(name)%><br/>
        <table border="0" style="margin-left:1em;">
            <tr>
                <td><b>Sample Count:</b></td>
                <td><%=h(workspace.getSamples().size())%></td>
            </tr>
            <tr>
                <td><b>Comp. Matrices:</b></td>
                <td><%=workspace.getCompensationMatrices().size()%></td>
            </tr>
            <tr>
                <td><b>Parameters:</b></td>
                <td><%=h(StringUtils.join(workspace.getParameters(), ", "))%></td>
            </tr>
        </table>
    </li>
</ul>

<%=generateBackButton()%>
<%=generateSubmitButton("Finish")%>
<%=generateButton("Cancel", cancelUrl)%>

