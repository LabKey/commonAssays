<%@ page buffer="none" %>
<%@ page import="org.labkey.flow.data.FlowLog" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm"%>
<%@ page import="org.labkey.flow.view.FlowQueryView"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.run.RunController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    RunForm form = (RunForm) __form;
    FlowQueryView view = new FlowQueryView(form);
    include(view, out);%>

<%
    FlowLog[] logs = getRun().getLogs();
    if (logs.length != 0)
    {%>
<p>Log Files: <%
    for (FlowLog log : logs)
    { %>
    <br><a href="<%=h(log.urlShow())%>"><%=h(log.getName())%></a>
    <%} %>
</p>
<% } %>
<% if (getContainer().hasPermission(getUser(), ACL.PERM_UPDATE) &&
    (getRun().getStep() == FlowProtocolStep.analysis || getRun().isInWorkspace()))
    {
        if (getRun().isInWorkspace()) { %>
            <p>
                This run is in the workspace.<br>
                <labkey:link href="<%=getRun().urlFor(ScriptController.Action.gateEditor)%>" text="Edit Gates on Individual Wells" /><br>
                When you are finished editing the gates, you can recalculate the statistics and move this run back into an analysis.<br>
                <labkey:link href="<%=getRun().urlFor(RunController.Action.moveToAnalysis)%>" text="Finish editing gates" />
            </p>
        <% } else { %>
            <p>
                You can modify the gates on individual FCS files in this run.<br>
                <labkey:link href="<%=getRun().urlFor(RunController.Action.moveToWorkspace)%>" text="Move this run to the workspace." /><br>
            </p>

    <% } } %>
<p>
    <%
        FlowCompensationMatrix comp = getCompensationMatrix();
        if (comp != null)
    { %>
    <labkey:link text="Show Compensation" href="<%=comp.urlFor(CompensationController.Action.showCompensation)%>"/><br>
    <% } %>
    <%  ViewURLHelper urlShowRunGraph = new ViewURLHelper("Experiment", "showRunGraph", getContainer());
        urlShowRunGraph.addParameter("rowId", Integer.toString(getRun().getRunId()));
    %>
    <labkey:link href="<%=h(urlShowRunGraph)%>" text="Experiment Run Graph"/><br>
    <labkey:link href="<%=getRun().urlFor(RunController.Action.download)%>" text="Download FCS Files" /><br>
</p>
