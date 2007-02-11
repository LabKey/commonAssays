<%@ page import="org.labkey.flow.analysis.web.SubsetSpec" %>
<%@ page import="org.labkey.flow.analysis.model.Population" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.data.*" %>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page extends="org.labkey.flow.controllers.editscript.EditGatesPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%
    Map<SubsetSpec, Population> populations = form.getPopulations();

    Map<Integer, String> runOptions = form.getExperimentRuns();
    FlowRun run = form.getRun();
    if (run == null)
    {
        %>
There are no experiment runs in this folder.
<%
        return;
    }
    Map<Integer, String> compOptions = new LinkedHashMap();
    if (form.step == FlowProtocolStep.analysis)
    {
        List<FlowCompensationMatrix> matrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
        boolean sameExperiment = FlowDataObject.sameExperiment(matrices);
        for (FlowCompensationMatrix matrix : matrices)
        {
            compOptions.put(matrix.getCompId(), matrix.getLabel(!sameExperiment));
        }
    }
    Map<Integer, String> wellOptions = run.getWells(FlowProtocol.getForContainer(getContainer()), form.getAnalysis(), form.step);
    int wellId = FlowPreference.editScriptWellId.getIntValue(request);
    FlowWell well = FlowWell.fromWellId(wellId);
    if (well == null || run.getRunId() != well.getRun().getRunId())
    {
        well = FlowWell.fromWellId(wellOptions.keySet().iterator().next());
    }
    ViewURLHelper urlSave = urlFor(ScriptController.Action.saveGate);
    well.addParams(urlSave);
    ViewURLHelper urlGraph = urlFor(ScriptController.Action.graphWindow);
    well.addParams(urlGraph);
%>
<%=pageHeader(ScriptController.Action.editGates)%>
<link rel="stylesheet" href="<%=resourceURL("editor.css")%>">
<script type="text/javascript" src="<%=resourceURL("util.js")%>"></script>
<script type="text/javascript" src="<%=resourceURL("editGates.js")%>"></script>
<%
    ViewURLHelper urlAnalysisJS = urlFor(ScriptController.Action.analysisJS);
    run.addParams(urlAnalysisJS);
%>
<script type="text/javascript" src="<%=urlFor(ScriptController.Action.analysisJS)%>"></script>
<script type="text/javascript">
    var g_formAction = '<%=urlSave.toString()%>';
    var g_urlGraphWindow = '<%=urlGraph%>';
    var g_urlInstructions = '<%=resourceURL("blankGraphWindow.html")%>';
    window.setTimeout(editGatesOnLoad, 1);
    function editGatesOnLoad()
    {
        try
        {
            initGraph('<%=StringUtils.trimToEmpty(form.subset)%>', '<%=StringUtils.trimToEmpty(form.xAxis)%>', '<%=StringUtils.trimToEmpty(form.yAxis)%>');
        }
        catch (e)
        {
            <%-- On rare occasions, there's an exception that gets thrown here, probably having something
            to do with timing.--%>
        }
    }
</script>

<br>
<table class="normal">
    <tr><td colspan="3">Current Population: <select id="subset" onchange="setPopulation(getValue(this))">
        <option value=""<%=StringUtils.isEmpty(form.subset) ? " selected" : ""%>>Ungated</option>
        <%for (Map.Entry<SubsetSpec, Population> entry : populations.entrySet())
        {
        %>
        <option value="<%=h(entry.getKey().toString())%>"<%=entry.getKey().toString().equals(form.subset) ? " selected" : ""%>><%=h(entry.getKey().toString())%></option>
        <% } %>
    </select>
        <a href="#" onclick="createNewPopulation();return false"><%=PageFlowUtil.buttonImg("Define child population")%></a>
    </td></tr>
    <tr>
        <td>
            <select id="wells" size="<%=Math.min(25, wellOptions.size())%>" style="height:400px" onChange="setWell(getValue(this))">
                <cpas:options value="<%=well.getWellId()%>" map="<%=wellOptions%>"/>
            </select>
        </td>
        <td valign="top">
            <iframe src="<%=resourceURL("blankGraphWindow.html")%>" frameborder="0" style="border:none" id="graph" name="graph" width="410" height="410"></iframe>
        </td>
        <td valign="top" id="polygon">
        </td>
    </tr>
    <tr><td colspan="3">
    <form action="<%=form.analysisScript.urlFor(ScriptController.Action.editGates, form.step)%>" method="POST" style="display:inline">
        Experiment Run: <select name="<%=FlowParam.runId%>">
        <cpas:options value="<%=form.getRun().getRunId()%>" map="<%=runOptions%>" />
        </select>
        <% if (!compOptions.isEmpty()) { %>
        Compensation Matrix:
        <select name="<%=FlowParam.compId%>">
            <cpas:options value="<%=form.getCompensationMatrix().getCompId()%>" map="<%=compOptions%>"/>
        </select>
        <% } %>
        <cpas:button text="reload" />
    </form>
    </td></tr>
</table>


