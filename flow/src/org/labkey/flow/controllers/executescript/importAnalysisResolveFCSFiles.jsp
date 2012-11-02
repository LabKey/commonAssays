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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.WorkspaceData" %>
<%@ page import="org.labkey.flow.analysis.model.PCWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.ResolvedSamplesData" %>
<%@ page import="org.labkey.flow.controllers.executescript.ResolvedConfirmGridView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    WorkspaceData workspaceData = form.getWorkspace();
    ResolvedSamplesData resolvedData = form.getResolvedSamples();

    ResolvedConfirmGridView view = new ResolvedConfirmGridView(resolvedData, null);

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isAdministrator() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>

<p>Please verify all resolved FCS files from previously imported runs.
    If a sample from the analysis couldn't be resolved or was incorrectly resolved, you may correct it by
    selecting the approrpiate FCS file from the dropdown.
    All checked rows will be imported.
</p>
<hr/>

<p></p>
<%
    include(view, out);
%>
<br/>

