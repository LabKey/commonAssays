<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%!
    String subsetLink(String subset)
    {
        if (StringUtils.isEmpty(subset))
        {
            return "Ungated";
        }
        ViewURLHelper ret = form.analysisScript.urlFor(ScriptController.Action.editGates, FlowProtocolStep.calculateCompensation);
        ret.addParameter("subset", subset);
        return "<a href=\"" + h(ret) + "\">" + h(subset) + "</a>";
    }
%>

<%  CompensationCalculationDef calc = compensationCalculationDef();
    if (calc != null)
    {
%>
<table class="normal" border="1"><tr><th rowspan="2">Channel</th><th colspan="3">Positive</th><th colspan="3">
    Negative</th></tr>
    <tr><th>Keyword</th><th>Value</th><th>Subset</th><th>Keyword</th><th>Value</th><th>Subset</th></tr>
    <% for (int i = 0; i < form.parameters.length; i++)
    {
        String channel = form.parameters[i];
        if (channel == null)
            continue;
    %>
    <tr><td><%=h(channel)%></td>
        <td><%=h(form.positiveKeywordName[i])%></td>
        <td><%=h(form.positiveKeywordValue[i])%></td>
        <td><%=subsetLink(form.positiveSubset[i])%></td>
        <td><%=h(form.negativeKeywordName[i])%></td>
        <td><%=h(form.negativeKeywordValue[i])%></td>
        <td><%=subsetLink(form.negativeSubset[i])%></td>
    </tr>
    <% } %>
</table>
<% } %>
<% if (form.canEdit()) { %>
    <p>
        This compensation calculation may be edited in a number of ways:<br>
        <cpas:link text="Upload a Flow Jo workspace" href="<%=form.analysisScript.urlFor(ScriptController.Action.uploadCompensationCalculation)%>" /><br>
        <cpas:link text="Switch keywords or gates" href="<%=form.analysisScript.urlFor(ScriptController.Action.chooseCompensationRun)%>" /><br>
        <cpas:link text="Move or define gates" href="<%=form.analysisScript.urlFor(ScriptController.Action.editGates, FlowProtocolStep.calculateCompensation)%>" /><br>
        <cpas:link href="<%=form.analysisScript.urlFor(ScriptController.Action.editGateTree, FlowProtocolStep.calculateCompensation)%>" text="Rename gates" /><br>
    </p>
<% } %>
