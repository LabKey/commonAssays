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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
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
<%@ page import="org.labkey.flow.analysis.model.Workspace" %>
<%@ page import="org.labkey.api.data.SimpleFilter" %>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.flow.data.FlowFCSFile" %>
<%@ page import="org.labkey.flow.query.FlowSchema" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.labkey.flow.controllers.executescript.ResolvedSamplesData" %>
<%@ page import="org.labkey.api.data.DataRegion" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisEngine" %>
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

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
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
<input type="hidden" name="rEngineNormalizationSubsets" value="<%=h(form.getrEngineNormalizationSubsets())%>"/>
<input type="hidden" name="rEngineNormalizationParameters" value="<%=h(form.getrEngineNormalizationParameters())%>"/>

<% if (form.isCreateAnalysis()) { %>
<input type="hidden" name="newAnalysisName" id="newAnalysisName" value="<%=h(form.getNewAnalysisName())%>">
<% } else { %>
<input type="hidden" name="existingAnalysisId" id="existingAnalysisId" value="<%=h(form.getExistingAnalysisId())%>">
<% } %>

<p>You are about to import the analysis from the workspace with the following settings:</p>
<%
    Workspace workspace = form.getWorkspace().getWorkspaceObject();
%>
<ul>
    <li style="padding-bottom:0.5em;">
        <b>Analysis Engine:</b>
        <% if (form.getSelectAnalysisEngine() == null || AnalysisEngine.FlowJoWorkspace == form.getSelectAnalysisEngine()) { %>
            No analysis engine selected
        <% } else if (AnalysisEngine.R == form.getSelectAnalysisEngine()) { %>
            External R analysis engine <%=text(form.isrEngineNormalization() ? "with normalization" : "without normalization")%>
        <% } %>
    </li>
    <li style="padding-bottom:0.5em;">
        <b>Import Groups:</b> <%=text(form.getImportGroupNames() == null ? "<em>All Samples</em>" : h(StringUtils.join(form.getImportGroupNameList(), ", ")))%>
    </li>
    <% if (AnalysisEngine.R == form.getSelectAnalysisEngine() && form.isrEngineNormalization()) { %>
    <li style="padding-bottom:0.5em;">
        <b>Normalization Options:</b>
        <table border="0" style="margin-left:1em;">
            <tr>
                <td><b>Reference Sample:</b></td>
                <td><%=h(form.getrEngineNormalizationReference())%></td>
            </tr>
            <tr>
                <td><b>Normalize Subsets:</b></td>
                <td>
                    <% if (form.getrEngineNormalizationSubsets() != null) { %>
                    <%=h(StringUtils.join(form.getrEngineNormalizationSubsetList(), ", "))%>
                    <% } else { %>
                    <em>All parameters</em>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td><b>Normalize Parameters:</b></td>
                <td>
                <% if (form.getrEngineNormalizationParameters() != null) { %>
                    <%=h(StringUtils.join(form.getrEngineNormalizationParameterList(), ", "))%>
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
    } else if (!form.getResolvedSamples().getRows().isEmpty()) {
    %>
    <li style="padding-bottom:0.5em;">
        <b>Existing FCS files:</b>
        <%
            Set<String> rowIds = new HashSet<String>();
            for (ResolvedSamplesData.ResolvedSample sample : form.getResolvedSamples().getRows().values())
            {
                if (sample.isSelected() && sample.getMatchedFile() != null)
                    rowIds.add(String.valueOf(sample.getMatchedFile()));
            }
            SimpleFilter filter = new SimpleFilter(new FieldKey(null, "RowId"), rowIds, CompareType.IN);
            FlowSchema schema = new FlowSchema(context);
            ActionURL url = schema.urlFor(QueryAction.executeQuery, FlowTableType.FCSFiles);
            filter.applyToURL(url, QueryView.DATAREGIONNAME_DEFAULT);
        %>
        <a href="<%=url%>" target="_blank" title="Show FCS files"><%=h(rowIds.size())%> FCS files</a>
    </li>
    <%
    } else {
    %>
    <li style="padding-bottom:0.5em;">
        <b>Existing FCS File run:</b> <i>none set</i>
    </li>
    <li style="padding-bottom:0.5em;">
        <b>FCS File Path:</b>
        <% if (form.getKeywordDir() == null || form.getKeywordDir().length == 0) { %>
        <i>none set</i>
        <% } else { %>
        <%=h(form.getKeywordDir()[0])%>
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

