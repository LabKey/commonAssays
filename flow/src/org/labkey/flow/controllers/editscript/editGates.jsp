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
<%@ page extends="org.labkey.flow.controllers.editscript.EditGatesPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%
    Map<SubsetSpec, Population> populations = form.getPopulations();
    ViewURLHelper urlSave = urlFor(ScriptController.Action.saveGate);
    ViewURLHelper urlGraph = urlFor(ScriptController.Action.graphWindow);
    Map<Integer, String> runOptions = new LinkedHashMap();
    for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
    {
        runOptions.put(run.getRunId(), run.getName());
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
%>
<%=pageHeader(ScriptController.Action.editGates)%>
<link rel="stylesheet" href="<%=resourceURL("editor.css")%>">
<script type="text/javascript" src="<%=resourceURL("util.js")%>"></script>
<script type="text/javascript" src="<%=resourceURL("editGates.js")%>"></script>
<script type="text/javascript" src="<%=urlFor(ScriptController.Action.analysisJS)%>"></script>
<script type="text/javascript">
    var g_formAction = '<%=urlSave.toString()%>';
    var g_urlGraphWindow = '<%=urlGraph%>';
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
    <tr><td colspan="3">Population: <select id="subset" onchange="setPopulation(getValue(this))">
        <option value=""<%=StringUtils.isEmpty(form.subset) ? " selected" : ""%>>Ungated</option>
        <%for (Map.Entry<SubsetSpec, Population> entry : populations.entrySet())
        {
        %>
        <option value="<%=h(entry.getKey().toString())%>"<%=entry.getKey().toString().equals(form.subset) ? " selected" : ""%>><%=h(entry.getKey().toString())%></option>
        <% } %>
    </select>
        <a href="#" onclick="createNewPopulation();return false"><%=PageFlowUtil.buttonImg("new")%></a>
    </td></tr>
    <tr>
        <td>
            <% FlowWell[] wells;
                if (form.step == FlowProtocolStep.analysis)
                {
                    wells = form.run.getWellsToBeAnalyzed(FlowProtocol.getForContainer(getContainer()));
                }
                else
                {
                    wells = form.run.getWells();
                }
            %>
            <select id="wells" size="<%=Math.min(25, wells.length)%>" style="height:400px" onChange="setWell(getValue(this))">
                <% for (FlowWell well : wells) { %>
                <option value="<%=well.getWellId()%>" <%= well.getWellId() == form.well.getWellId() ? " selected" : ""%>><%=h(form.getWellLabel(well))%></option>
            <% } %>
            </select>
        </td>
        <td valign="top">
            <iframe src="<%=resourceURL("blankGraphWindow.html")%>" frameborder="0" style="border:none" id="graph" name="graph" width="410" height="410"></iframe>
        </td>
        <td valign="top" id="polygon">
        </td>
    </tr>
</table>


