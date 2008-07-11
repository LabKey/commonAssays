<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page buffer="none" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page import="org.labkey.flow.controllers.run.RunController"%>
<%@ page import="org.labkey.flow.controllers.run.RunForm"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowLog" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.view.FlowQueryView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    RunForm form = (RunForm) HttpView.currentModel();
    ViewContext context = HttpView.currentContext();
    FlowRun run = form.getRun();
    FlowCompensationMatrix comp = run.getCompensationMatrix();
    FlowQueryView view = new FlowQueryView(form);
    include(view, out);%>

<%
    FlowLog[] logs = run.getLogs();
    if (logs.length != 0)
    {%>
<p>Log Files: <%
    for (FlowLog log : logs)
    { %>
    <br><a href="<%=h(log.urlShow())%>"><%=h(log.getName())%></a>
    <%} %>
</p>
<% } %>
<% if (context.getContainer().hasPermission(context.getUser(), ACL.PERM_UPDATE) &&
    (run.getStep() == FlowProtocolStep.analysis || run.isInWorkspace()))
    {
        if (run.isInWorkspace()) { %>
            <p>
                This run is in the workspace.<br>
                <labkey:link href="<%=run.urlFor(ScriptController.Action.gateEditor)%>" text="Edit Gates on Individual Wells" /><br>
                When you are finished editing the gates, you can recalculate the statistics and move this run back into an analysis.<br>
                <labkey:link href="<%=run.urlFor(RunController.Action.moveToAnalysis)%>" text="Finish editing gates" />
            </p>
        <% } else { %>
            <p>
                You can modify the gates on individual FCS files in this run.<br>
                <labkey:link href="<%=run.urlFor(RunController.Action.moveToWorkspace)%>" text="Move this run to the workspace" /><br>
            </p>

    <% } } %>
<p>
    <%
        if (comp != null)
    { %>
    <labkey:link text="Show Compensation" href="<%=comp.urlFor(CompensationController.Action.showCompensation)%>"/><br>
    <% } %>
    <%  ActionURL urlShowRunGraph = new ActionURL("Experiment", "showRunGraph", context.getContainer());
        urlShowRunGraph.addParameter("rowId", Integer.toString(run.getRunId()));
    %>
    <labkey:link href="<%=h(urlShowRunGraph)%>" text="Experiment Run Graph"/><br>
    <%if (run.getPath() != null) {%>
    <labkey:link href="<%=run.urlFor(RunController.Action.download)%>" text="Download FCS Files" /><br>
    <% } %>
</p>
