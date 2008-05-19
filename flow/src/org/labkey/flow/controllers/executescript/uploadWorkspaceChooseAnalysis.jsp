<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.UploadWorkspaceResultsForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ViewContext context = getViewContext();
    Container container = context.getContainer();

    UploadWorkspaceResultsForm form = (UploadWorkspaceResultsForm)getModelBean();
    Map<Integer, String> analyses = new LinkedHashMap<Integer, String>();
    for (FlowExperiment analysis : FlowExperiment.getAnalyses(container))
    {
        analyses.put(analysis.getExperimentId(), analysis.getName());
    }

%>
<labkey:errors />
<form action="<%=h(container.urlFor(AnalysisScriptController.Action.uploadWorkspaceChooseAnalysis))%>" method="POST" enctype="multipart/form-data">
    <% for (Map.Entry<String, String> entry : form.getWorkspace().getHiddenFields().entrySet())
    {%>
    <input type="hidden" name="workspace.<%=entry.getKey()%>" value="<%=h(entry.getValue())%>">
    <% } %>
    <input type="hidden" name="ff_confirm" value="true" />
    <p>The statistics in this workspace that have been calculated will be uploaded to <%=FlowModule.getLongProductName()%>.</p>
    <% if (analyses.size() == 0) { %>
    <p><%=FlowModule.getLongProductName()%> organizes results into different "analysis folders".  The same FCS file should only
    be analyzed once in a given analysis folder.  If you want to analyze the same FCS file in two different ways, those results
    should be put into different analysis folders.<br>

    What do you want to call the new analysis folder?  You will be able to use this name for multiple uploaded workspaces.<br>
    <input type="text" name="ff_newAnalysisName" value="<%=h(form.ff_newAnalysisName)%>">
    </p>
    <% } else { %>
    <p>Which analysis folder do you want to put the results into?<br>
        <select name="ff_existingAnalysisId">
            <labkey:options value="<%=form.ff_existingAnalysisId%>" map="<%=analyses%>" />
        </select><br>
        or create a new analysis folder named:<br>
        <input type="text" name="ff_newAnalysisName" value="<%=h(form.ff_newAnalysisName)%>">
    </p>
    <% } %>
    <labkey:button text="Upload Results" />
</form>