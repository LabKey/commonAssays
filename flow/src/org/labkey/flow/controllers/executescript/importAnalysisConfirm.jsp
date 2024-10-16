<%
/*
 * Copyright (c) 2011-2018 LabKey Corporation
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
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.data.SimpleFilter" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.study.Study" %>
<%@ page import="org.labkey.api.study.StudyService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.analysis.model.ISampleInfo" %>
<%@ page import="org.labkey.flow.analysis.model.IWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.SelectedSamples" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.query.FlowSchema" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="static org.labkey.flow.controllers.executescript.AnalysisScriptController.BACK_BUTTON_ACTION" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);

    List<String> selectedSamples = new ArrayList<>(form.getSelectedSamples().getRows().size());
    for (Map.Entry<String, SelectedSamples.ResolvedSample> entry : form.getSelectedSamples().getRows().entrySet())
    {
        SelectedSamples.ResolvedSample resolvedSample = entry.getValue();
        if (resolvedSample.isSelected())
            selectedSamples.add(entry.getKey());
    }

    String targetStudyLabel = null;
    if (form.getTargetStudy() != null && form.getTargetStudy().length() > 0)
    {
        Set<Study> studies = StudyService.get().findStudy(form.getTargetStudy(), getUser());
        Study study = studies.iterator().next();
        targetStudyLabel = study.getContainer().getPath() + " (" + study.getLabel() + ")";
    }
%>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=form.getSelectFCSFilesOption()%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
<input type="hidden" name="resolving" value="<%=form.isResolving()%>">
<input type="hidden" name="selectAnalysisEngine" id="selectAnalysisEngine" value="<%=form.getSelectAnalysisEngine()%>">
<input type="hidden" name="createAnalysis" id="createAnalysis" value="<%=form.isCreateAnalysis()%>">

<%--
<% for (int i = 0; form.getEngineOptionFilterKeyword() != null && i < form.getEngineOptionFilterKeyword().length; i++) { %>
<input type="hidden" name="engineOptionFilterKeyword" value="<%=h(form.getEngineOptionFilterKeyword()[i])%>">
<input type="hidden" name="engineOptionFilterOp" value="<%=h(form.getEngineOptionFilterOp()[i])%>">
<input type="hidden" name="engineOptionFilterValue" value="<%=h(form.getEngineOptionFilterValue()[i])%>">
<% } %>
--%>

<input type="hidden" name="importGroupNames" value="<%=h(form.getImportGroupNames())%>"/>

<% if (form.isCreateAnalysis()) { %>
<input type="hidden" name="newAnalysisName" id="newAnalysisName" value="<%=h(form.getNewAnalysisName())%>">
<% } else { %>
<input type="hidden" name="existingAnalysisId" id="existingAnalysisId" value="<%=form.getExistingAnalysisId()%>">
<% } %>

<input type="hidden" name="targetStudy" id="targetStudy" value="<%=h(form.getTargetStudy())%>">

<p>You are about to import the analysis from the workspace with the following settings:</p>
<%
    IWorkspace workspace = form.getWorkspace().getWorkspaceObject();
%>
<ul>
    <li style="padding-bottom:0.5em;">
        <%
            String name = form.getWorkspace().getOriginalPath();
            if (name == null)
                name = form.getWorkspace().getPath();
            if (name == null)
                name = form.getWorkspace().getName();

            List<? extends ISampleInfo> allSamples = workspace.getSamples();
            boolean allSelected = allSamples.size() == selectedSamples.size();
        %>
        <b><%=h(workspace.getKindName())%>:</b> <%=h(name)%><br/>
        <table border="0" style="margin-left:1em;">
            <tr>
                <td><b>Samples:</b></td>
                <td><%=h(allSelected ? String.format("All %d selected", allSamples.size()) : String.format("%d of %d selected", selectedSamples.size(), allSamples.size()))%></td>
            </tr>
            <tr>
                <td><b>Comp. Matrices:</b></td>
                <td><%=workspace.getCompensationMatrices().size()%></td>
            </tr>
            <tr>
                <td><b>Parameters:</b></td>
                <td><%=h(StringUtils.join(workspace.getParameterNames(), ", "))%></td>
            </tr>
        </table>
    </li>

    <%
    if (form.isResolving() && !form.getSelectedSamples().getRows().isEmpty()) {
    %>
    <li style="padding-bottom:0.5em;">
        <b>Existing FCS files:</b>
        <%
            Set<String> rowIds = new HashSet<>();
            for (String sampleId : selectedSamples)
            {
                SelectedSamples.ResolvedSample sample = form.getSelectedSamples().getRows().get(sampleId);
                if (sample.isSelected() && sample.hasMatchedFile())
                    rowIds.add(String.valueOf(sample.getMatchedFile()));
            }
            SimpleFilter filter = new SimpleFilter(new FieldKey(null, "RowId"), rowIds, CompareType.IN);
            FlowSchema schema = new FlowSchema(context);
            ActionURL url = schema.urlFor(QueryAction.executeQuery, FlowTableType.FCSFiles);
            filter.applyToURL(url, QueryView.DATAREGIONNAME_DEFAULT);
        %>
        <a href="<%=h(url)%>" target="_blank" title="Show FCS files"><%=rowIds.size()%> FCS files</a>
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
        <% if (form.isCreateAnalysis()) { %>
        <b>New Analysis Folder:</b> <%=h(form.getNewAnalysisName())%>
        <% } else { %>
        <b>Existing Analysis Folder:</b>
        <% FlowExperiment experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId()); %>
        <a href="<%=h(experiment.urlShow())%>" target="_blank"><%=h(experiment.getName())%></a>
        <% } %>
    </li>

    <% if (targetStudyLabel != null) { %>
    <li style="padding-bottom:0.5em;">
        <b>Target Study: </b> <%=h(targetStudyLabel)%>
    </li>
    <% } %>
</ul>

<p>
    <%= button("Cancel").href(cancelUrl) %>
    &nbsp;&nbsp;
    <%= button("Back").submit(true).primary(false).onClick(BACK_BUTTON_ACTION) %>
    <%= button("Finish").submit(true) %>
</p>
