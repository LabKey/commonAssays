<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.fhcrc.cpas.flow.script.xml.ScriptDocument" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController.Action" %>
<%@ page extends="org.labkey.flow.controllers.executescript.AnalysisScriptController.Page" %>
<%
    FlowScript analysisScript = getScript();
%>
<% if (analysisScript.hasStep(FlowProtocolStep.keywords))
{ %>
<h4>Experiment run upload settings: '<%=h(analysisScript.getName())%>'</h4>

<p>The upload settings tells the flow module which keywords to read from FCS files when they are uploaded. In the
    future,
    the upload specification may describe additional processing to be performed on the files at upload time.</p>
<a href="<%=analysisScript.urlFor(ScriptController.Action.editRun)%>">Edit the upload settings</a><br>

<%
    if (getContainer().hasPermission(getUser(), ACL.PERM_UPDATE))
    { %>
<% }
    return;
}
%>
<h4>Analysis script: '<%=h(analysisScript.getName())%>'</h4>
<p>Analysis scripts tell the Flow Module how to calculate the compensation matrix, what gates to apply, what statistics
to calculate, and what graphs to generate.</p>
<%
    boolean hasCompensation = false;
    boolean hasAnalysis = false;

    try
    {
        ScriptDocument script = analysisScript.getAnalysisScriptDocument();
        if (script != null && script.getScript() != null)
        {
            hasCompensation = script.getScript().getCompensationCalculation() != null;
            hasAnalysis = script.getScript().getAnalysis() != null;
        }
    }
    catch (Exception e)
    {

    }
%>
<p>
    <b>Compensation Calculation</b><br>
    <% if (hasCompensation)
    {%>
    <a href="<%=h(analysisScript.urlFor(Action.chooseRunsToAnalyze, FlowProtocolStep.calculateCompensation))%>">Calculate some
        compensation matrices.</a><br>
    <% }
    else
    { %>
    This analysis script does not contain a compensation calculation section.
    <% } %>
</p>

<p>
    <b>Analysis</b><br>
    <% if (hasAnalysis)
    { %>
    <a href="<%=h(analysisScript.urlFor(Action.chooseRunsToAnalyze, FlowProtocolStep.analysis))%>">Analyze some flow runs</a>
    <br>
    <% } else { %>
    This analysis script does not contain an analysis section.
    <% } %>
</p>

<p>
    <a href="<%=h(analysisScript.urlFor(ScriptController.Action.begin))%>">Edit the analysis script.</a><br>
    <a href="<%=h(analysisScript.urlFor(ScriptController.Action.copy))%>">Make a copy of this analysis script.</a><br>
    <a href="<%=h(analysisScript.urlFor(ScriptController.Action.delete))%>">Delete this analysis script.</a><br>
</p>

