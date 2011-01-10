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
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% 
    RunForm form = (RunForm) HttpView.currentModel();
    FlowRun run = form.getRun();
%>
<labkey:errors />
<form action="<%=h(run.urlFor(RunController.MoveToWorkspaceAction.class))%>" method="POST">
    <p>After you move this run into the workspace, you will be able to edit the gates on individual FCS files.</p>
    <labkey:button text="Move" />
    <labkey:button text="Cancel" href="<%=run.urlShow()%>" />
</form>