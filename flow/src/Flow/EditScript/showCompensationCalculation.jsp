<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page import="org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowRun" %>
<%@ page import="Flow.FlowModule" %>
<%@ page extends="Flow.EditScript.CompensationCalculationPage" %>
<form method="POST" action="<%=formAction(ScriptController.Action.editCompensationCalculation)%>"
      enctype="multipart/form-data">
    <p>
        The compensation calculation tells <%=FlowModule.getLongProductName()%> how
        to identify the compensation controls in an experiment run, and what gates
        to apply.  A compensation control is identified as having a particular value
        for a specific keyword.
    </p>

    <p>
        The normal way to define the compensation calculation is to upload a Flow Jo XML workspace<br>
        <input type="file" name="workspaceFile"><br>
        This workspace should contain only one set of compensation controls.</p>
        <% FlowRun[] runs = FlowRun.getRunsForContainer(getContainer());
            if (runs.length > 0)
            { %>
    <p>Another way to specify which keywords identify the compensation controls is to
        choose an existing experiment run.  You can use this if you have
        used the online gate editor to define the gates.<br>
        <select name="selectedRunId">
            <option value="0"></option>
            <% for (FlowRun run : runs)
            { %>
            <option value="<%=run.getRunId()%>"><%=h(run.getName())%></option>
            <% } %>
        </select>
    </p>
        <% } %>
        <input type="submit" value="Submit">
</form>

<%  CompensationCalculationDef calc = compensationCalculationDef();
    if (calc == null)
    {
%><p>There is no compensation calculation defined.</p>
<% }
else
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
        <td><%=h(form.positiveSubset[i])%></td>
        <td><%=h(form.negativeKeywordName[i])%></td>
        <td><%=h(form.negativeKeywordValue[i])%></td>
        <td><%=h(form.negativeSubset[i])%></td>
    </tr>
    <% } %>
</table>
<% } %>
</form>