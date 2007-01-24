<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.*" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.exp.api.ExpMaterial"%>
<%@ page extends="org.labkey.flow.controllers.well.WellController.Page" %>
<%
    FlowWell well = getWell();
    FlowWell fcsFile = well.getFCSFile();
%>
<table class="normal">
    <% if (getRun() == null) { %>
    <tr><td colspan="2">The run has been deleted.</td></tr>
    <% } else { %>
    <tr><td>Run Name:</td><td><%=h(getRun().getName())%></td></tr>
    <% } %>
    <tr><td>Well Name:</td><td><%=h(well.getName())%></td></tr>
    <% if (fcsFile != null)
    { %>
    <tr><td>FCS File:</td><td><a href="<%=h(fcsFile.urlShow())%>"><%=h(fcsFile.getName())%></a></td></tr>
    <% } %>
    <tr><td>Comment:</td><td><%=h(well.getComment())%></tr>
    <%
        if (getKeywords().size() > 0)
        {
    %>
    <% for (ExpMaterial sample : well.getSamples())
    { %>
    <tr><td><%=h(sample.getSampleSet().getName())%></td>
        <td><a href="<%=h(sample.detailsURL())%>"><%=h(sample.getName())%></a></td>
    </tr>
    <% } %>
    <tr><th colspan="2">Keywords</th></tr>
    <% for (Map.Entry<String, String> keyword : getKeywords().entrySet())
    { %>
    <tr><td><%=h(keyword.getKey())%></td><td><%=h(keyword.getValue())%></td></tr>
    <% }
    } %>
</table>
<%
    if (getContainer().hasPermission(getUser(), ACL.PERM_UPDATE))
    {
%>
<%=buttonLink("edit", well.urlFor(WellController.Action.editWell))%><br>
<% } %>
<% if (getStatistics().size() > 0)
{ %>
<table class="normal">
    <tr><th colspan="2">Statistics</th></tr>
    <% for (Map.Entry<StatisticSpec, Double> statistic : getStatistics().entrySet())
    { %>
    <tr><td><%=h(statistic.getKey().toString())%></td><td><%=statistic.getValue()%></td></tr>
    <% } %>
</table>
<% } %>

<% for (GraphSpec graph : getGraphs())
{ %>
<img src="<%=h(getWell().urlFor(WellController.Action.showGraph))%>&amp;graph=<%=u(graph.toString())%>">
<% } %>
<% List<FlowWell> analyses = getWell().getFCSAnalyses();
    if (analyses.size() > 0)
    { %>
<table class="normal"><tr><th colspan="3">Analyses performed on this file:</th></tr>
    <tr><th>FCS Analysis Name</th><th>Run Analysis Name</th><th>Analysis Name</th></tr>
    <% for (FlowWell analysis : analyses)
    { %>
    <tr><td><a href="<%=h(analysis.urlShow())%>"><%=h(analysis.getLabel())%></a></td>
        <td><%=h(analysis.getRun().getLabel())%></td>
        <td><%=h(analysis.getRun().getExperiment().getLabel())%></td>
    </tr>
    <% } %>
</table>
<% } %>

<p class="normal"><a href="<%=h(getWell().urlFor(WellController.Action.chooseGraph))%>">More Graphs</a><br>
    <a href="<%=h(getWell().urlFor(WellController.Action.showFCS))%>&amp;mode=keywords">All Keywords</a></p>
