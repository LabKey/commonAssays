<%@ page import="Flow.EditScript.ScriptController.Action"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowScript"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowProtocolStep"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page extends="Flow.EditScript.ScriptController.Page"%>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% FlowScript script = getScript();
boolean hasRun = script.hasStep(FlowProtocolStep.keywords);
boolean hasCompensation = script.hasStep(FlowProtocolStep.calculateCompensation);
boolean hasAnalysis = script.hasStep(FlowProtocolStep.analysis);
int runCount = script.getRunCount();
%>

<div class="normal">
<p>Welcome to the Flow Analysis Script Designer</p>
    <% if (runCount != 0) { %>
    <p>
        <b>Note: this analysis script cannot be edited because it has been used by <%=runCount%> runs.
        To edit this script you must first either delete the runs in which it has been used, or make a copy of this analysis script.</b>
    </p>
    <% } %>
<p>Script Description:<br>
    <% String description = script.getExpObject().getComment();
    if (StringUtils.isEmpty(description)) {
        description = "<none>";
    } %>
    <%=h(description)%> <cpas:link text="edit" href="<%=urlFor(Action.editProperties)%>"/></p>
<% if (hasRun) { %>
<p><b>Experiment Run Upload Settings</b><br>
    The experiment run upload settings section specifies which keywords need to be read from your FCS files.<br>
    <a href="<%=urlFor(Action.editRun)%>&actionSequence=10">Edit the upload settings</a><br>
</p>
<% } %>
<% if (hasCompensation || !hasRun) { %>
<p><b>Compensation Calculation</b><br>
    The compensation calculation section specifies how to identify the compensation wells on the plate, and which gates to apply<br>
    <a href="<%=urlFor(Action.editCompensationCalculation)%>&actionSequence=20">Define the compensation keywords</a><br>
    <a href="<%=urlFor(Action.editGates)%>&actionSequence=20">Edit gates or add populations</a><br>
    <a href="<%=urlFor(Action.editGateTree)%>&actionSequence=20">Remove, or rename populations</a><br>
</p>
<% } %>
<% if (hasAnalysis || !hasRun) { %>
<p><b>Analysis</b><br>
    The analysis section specifies which gates to apply to all samples, which statistics to calculate, and which graphs to generate.<br>
    <a href="<%=urlFor(Action.uploadAnalysis)%>&actionSequence=30">Upload a Flow Jo workspace</a><br>
    <a href="<%=urlFor(Action.editAnalysis)%>&actionSequence=30">Choose statistics and graphs</a><br>
    <a href="<%=urlFor(Action.editGates)%>&actionSequence=30">Edit gates or add populations</a><br>
    <a href="<%=urlFor(Action.editGateTree)%>&actionSequence=30">Remove, or rename populations</a><br>
</p>
<% } %>
<p>
    The Analysis Script is an XML file that you can also edit by hand.<br>
    <a href="<%=urlFor(Action.editScript)%>">Source View</a>
</p>
<p>
    Some pages show data from a particular run.  You can use
    <a href="<%=urlFor(Action.chooseSample)%>">this page</a>
    to choose which experiment run to use.
</p>
</div>