<%@ page import="org.labkey.flow.data.FlowLog" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm"%>
<%@ page import="org.labkey.flow.view.FlowQueryView"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page extends="org.labkey.flow.controllers.run.RunController.Page" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas"%>
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
<p>
    <%
        FlowCompensationMatrix comp = getCompensationMatrix();
        if (comp != null)
    { %>
    <cpas:link text="Show Compensation" href="<%=comp.urlFor(CompensationController.Action.showCompensation)%>"/><br>
    <% } %>
    <%  ViewURLHelper urlShowRunGraph = new ViewURLHelper("Experiment", "showRunGraph", getContainer());
        urlShowRunGraph.addParameter("rowId", Integer.toString(getRun().getRunId()));
    %>
    <cpas:link href="<%=h(urlShowRunGraph)%>" text="Experiment Run Graph"/><br>
</p>
