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
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.*" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.exp.api.ExpMaterial"%>
<%@ page import="java.text.DecimalFormat" %>
<%@ page extends="org.labkey.flow.controllers.well.WellController.Page" %>
<%
    FlowWell well = getWell();
    FlowWell fcsFile = well.getFCSFile();
    FlowScript script = well.getScript();
    FlowCompensationMatrix matrix = well.getCompensationMatrix();

%>
<table class="labkey-form">
    <% if (getRun() == null) { %>
    <tr><td colspan="2">The run has been deleted.</td></tr>
    <% } else { %>
    <tr><td>Run Name:</td><td><%=h(getRun().getName())%></td></tr>
    <% } %>
    <tr><td>Well Name:</td><td><%=h(well.getName())%></td></tr>
    <% if (fcsFile != null && fcsFile != well)
    { %>
    <tr><td>FCS File:</td><td><a href="<%=h(fcsFile.urlShow())%>"><%=h(fcsFile.getName())%></a></td></tr>
    <% } %>
    <tr><td>Comment:</td><td><%=h(well.getComment())%></tr>
    <% if (script != null) { %>
    <tr><td>Analysis Script:</td><td><a href="<%=h(script.urlShow())%>"><%=h(script.getName())%></a></td></tr>
    <% } %>
    <% if (matrix != null) { %>
    <tr><td>Compensation Matrix:</td><td><a href="<%=h(matrix.urlShow())%>"><%=h(matrix.getName())%></a></td></tr>
    <% } %>
    <% for (ExpMaterial sample : well.getSamples())
    { %>
    <tr><td><%=h(sample.getSampleSet().getName())%></td>
        <td><a href="<%=h(sample.detailsURL())%>"><%=h(sample.getName())%></a></td>
    </tr>
    <% } %>
</table>
    <%
    if (getKeywords().size() > 0)
    {
    %>
<div style="overflow:auto;height:400px">
    <table class="labkey-form">
    <tr><th colspan="2">Keywords</th></tr>
    <% for (Map.Entry<String, String> keyword : getKeywords().entrySet())
    { %>
    <tr><td><%=h(keyword.getKey())%></td><td><%=h(keyword.getValue())%></td></tr>
    <% } %>
</table>
    </div>
<% } %>
<%
    if (getContainer().hasPermission(getUser(), ACL.PERM_UPDATE))
    {
%>
<%=buttonLink("edit", well.urlFor(WellController.Action.editWell))%><br>
<% } %>
<% if (getStatistics().size() > 0)
{
    DecimalFormat fmt = new DecimalFormat("#,##0.####");
%>
<div style="overflow:auto;height:400px">
<table>
    <tr><th colspan="2">Statistics</th></tr>
    <% for (Map.Entry<StatisticSpec, Double> statistic : getStatistics().entrySet())
    { %>
    <tr><td><%=h(statistic.getKey().toString())%></td><td><%=fmt.format(statistic.getValue())%></td></tr>
    <% } %>
</table>
    </div>
<% } %>

<% for (GraphSpec graph : getGraphs())
{ %>
<img src="<%=h(getWell().urlFor(WellController.Action.showGraph))%>&amp;graph=<%=u(graph.toString())%>">
<% } %>
<% List<FlowWell> analyses = getWell().getFCSAnalyses();
    if (analyses.size() > 0)
    { %>
<table class="labkey-form"><tr><th colspan="3">Analyses performed on this file:</th></tr>
    <tr><th>FCS Analysis Name</th><th>Run Analysis Name</th><th>Analysis Name</th></tr>
    <% for (FlowWell analysis : analyses)
    {
        FlowRun run = analysis.getRun();
        FlowExperiment experiment = run.getExperiment();
    %>
    <tr><td><a href="<%=h(analysis.urlShow())%>"><%=h(analysis.getLabel())%></a></td>
        <td><%=h(run.getLabel())%></td>
        <td><%=experiment == null ? "" : h(experiment.getLabel())%></td>
    </tr>
    <% } %>
</table>
<% } %>

<% if (well.getFCSURI() == null) { %>
    <p>There is no file on disk for this well.</p>
<% } else { %>
    <p><a href="<%=h(getWell().urlFor(WellController.Action.chooseGraph))%>">More Graphs</a><br>
    <a href="<%=h(getWell().urlFor(WellController.Action.showFCS))%>&amp;mode=keywords">Keywords from the FCS file</a></p>
<% } %>