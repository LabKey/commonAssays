<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.data.FlowLog" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.controllers.run.RunForm"%>
<%@ page import="org.labkey.flow.view.FlowQueryView"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page extends="org.labkey.flow.controllers.run.RunController.Page" %>
<%
    RunForm form = (RunForm) __form;
    FlowQueryView view = new FlowQueryView(form);
    include(view, out);%>

<%
    FlowCompensationMatrix comp = getCompensationMatrix();
    if (comp != null)
{ %>
<%=buttonLink("Show Compensation", comp.urlFor(CompensationController.Action.showCompensation))%>
<% } %>
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
<% }
    ViewURLHelper urlShowRunGraph = new ViewURLHelper("Experiment", "showRunGraph", getContainer());
    urlShowRunGraph.addParameter("rowId", Integer.toString(getRun().getRunId()));
%>
<p>
    <a href="<%=h(urlShowRunGraph)%>">Experiment Run Graph</a><br>
</p>
