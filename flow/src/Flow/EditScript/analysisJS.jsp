<%@ page import="java.util.Map"%><%@ page import="com.labkey.flow.web.SubsetSpec"%><%@ page import="com.labkey.flow.model.*"%><%@ page import="org.fhcrc.cpas.flow.data.FlowWell"%><%@ page import="org.fhcrc.cpas.flow.script.FlowAnalyzer"%><%@ page import="com.labkey.flow.web.FCSAnalyzer"%><%@page contentType="text/javascript"%>
<%@page extends="Flow.EditScript.EditGatesPage" %>
<%!
String jsGate(Population pop)
{
    if (complexGate(pop))
        return "null";
    if (pop.getGates().size() == 0)
        return "null";
    Gate gate = pop.getGates().get(0);
    if (!(gate instanceof PolygonGate))
        return "null";
    PolygonGate polyGate = (PolygonGate) gate;
    Polygon poly = polyGate.getPolygon();
    StringBuilder ret = new StringBuilder("{");
    ret.append("xAxis:'" + polyGate.getX() + "',");
    ret.append("yAxis:'" + polyGate.getY() + "',");
    ret.append("points:[");
    for (int i = 0; i < poly.X.length; i ++)
        {
        if (i != 0)
            ret.append(",");
        ret.append("{x : " + poly.X[i] + ",y:" + poly.Y[i] + "}");
        }
    ret.append("]}");
    return ret.toString();
}
boolean complexGate(Population pop)
{
    if (pop.getGates().size() == 0)
        return false;
    if (pop.getGates().size() > 1)
        return true;
    if (!(pop.getGates().get(0) instanceof PolygonGate))
        return true;
    return false;
}
String q(Object val)
{
    if (val == null)
        return "''";
    return "'" + val + "'";
}
%>
var wellId = <%=form.well.getWellId()%>;
var scriptId = <%=form.analysisScript.getScriptId()%>;
var parameters = [];

<%
PopulationSet analysis = form.getAnalysis();
Map<SubsetSpec, Population> populations = form.getPopulations();
for (Map.Entry<String, String> entry : form.getParameters().entrySet()) { %>
parameters.push({ name : <%=q(entry.getKey())%>, label : <%=q(entry.getValue())%>});
<% } %>
var populations = {};
populations[''] = {name : 'Ungated', id: '', children: {}};
    <% for (Map.Entry<SubsetSpec, Population> entry : populations.entrySet()) {
        Population pop = entry.getValue();
    %>
    populations[<%=q(entry.getKey())%>] = { name : <%=q(pop.getName())%>,
        id : <%=q(entry.getKey())%>,
        gate: <%=jsGate(pop)%>,
        complexGate: <%=complexGate(pop)%>,
        parent: populations[<%=q(entry.getKey().getParent())%>],
        children : {}
    };
    populations[<%=q(entry.getKey().getParent())%>].children[<%=q(pop.getName())%>] = populations[<%=q(entry.getKey())%>];
    <% } %>

var subsetWellMap = {};
<%
// For the compensation calculation, we want to automatically select the right sample if they pick a gate which is used
// for a particular channel.
if (analysis instanceof CompensationCalculation && form.run != null)
{
    FlowWell[] wells = form.run.getWells();
    FCSKeywordData[] data = new FCSKeywordData[wells.length];
    for (int i = 0; i < wells.length; i ++)
    {
        data[i] = FCSAnalyzer.get().readAllKeywords(FlowAnalyzer.getFCSRef(wells[i]));
    }
    for (SubsetSpec subset : populations.keySet())
    {
        FlowWell wellMatch = getReleventWell(subset, (CompensationCalculation) analysis, wells, data);
        if (wellMatch != null)
        {%>
        subsetWellMap[<%=quote(subset.toString())%>] = <%=wellMatch.getWellId()%>;        
        <%}
    }
}
%>
