<%@ page import="com.labkey.flow.web.SubsetSpec" %>
<%@ page import="com.labkey.flow.model.Population" %>
<%@ page import="java.util.Map" %>
<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowWell" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page extends="Flow.EditScript.EditGatesPage" %>
<%
    Map<SubsetSpec, Population> populations = form.getPopulations();
    ViewURLHelper urlSave = urlFor(ScriptController.Action.saveGate);
    ViewURLHelper urlGraph = urlFor(ScriptController.Action.graphWindow);
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
    <tr><td colspan="3"><select id="subset" onchange="setPopulation(getValue(this))">
        <option value="">Ungated</option>
        <%for (Map.Entry<SubsetSpec, Population> entry : populations.entrySet())
        {
        %>
        <option value="<%=h(entry.getKey().toString())%>"><%=h(entry.getKey().toString())%></option>
        <% } %>
    </select>
        <a href="#" onclick="createNewPopulation();return false"><%=PageFlowUtil.buttonImg("new")%></a>
    </td></tr>
    <tr>
        <td>
            <% FlowWell[] wells = form.run.getWells(); %>
            <select id="wells" size="<%=Math.min(25, wells.length)%>" style="height:400px" onChange="setWell(getValue(this))">
                <% for (FlowWell well : wells) { %>
                <option value="<%=well.getWellId()%>" <%= well.getWellId() == form.well.getWellId() ? " selected" : ""%>><%=h(well.getName())%></option>
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


