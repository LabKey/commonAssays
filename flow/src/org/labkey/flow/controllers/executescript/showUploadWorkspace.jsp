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
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ViewContext context = getViewContext();
    Container container = context.getContainer();
%>
<labkey:errors />
<p>You can upload a FlowJo workspace that contains statistics that FlowJo has calculated.  To do this, use FlowJo to save
the workspace as XML.</p>
<form action="<%=urlFor(AnalysisScriptController.Action.showUploadWorkspace)%>" method="POST" enctype="multipart/form-data">
    <p>Which file do you want to upload?<br>
    <input type="file" name="workspace.file">
    </p>
    <labkey:button text="Upload" />
</form>
<% if (PipelineService.get().findPipelineRoot(container) != null) { %>
<hr>
<p>Alternatively, if your workspace has been saved as XML on the file server, you can browse the pipeline directories and
find your workspace.  If there are FCS files are in the same directory as the workspace, then the FlowJo results will be
linked to FCS files.  You will be able to use <%=FlowModule.getLongProductName()%> to see additional graphs, or
calculate additional statistics.<br>
    <labkey:button href="<%=urlFor(AnalysisScriptController.Action.browseForWorkspace)%>" text="Browse the pipeline" />
</p>
<% } %>