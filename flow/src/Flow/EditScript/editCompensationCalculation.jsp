<%@ page import="org.fhcrc.cpas.util.PageFlowUtil" %>
<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page import="java.util.*" %>
<%@ page import="com.labkey.flow.model.FlowJoWorkspace"%>
<%@ page import="com.labkey.flow.model.Analysis"%>
<%@ page extends="Flow.EditScript.CompensationCalculationPage" %>
<script type="text/javascript" src="<%=request.getContextPath()%>/Flow/editCompensationCalculation.js"></script>
<script type="text/javascript">
    var keywordValueSubsetListMap = {};
    var subsetList = {};
    <%
    for (FlowJoWorkspace.SampleInfo sample : form.workspace.getSamples())
    {
        Analysis analysis = form.workspace.getSampleAnalysis(sample);
        if (analysis != null)
        {
    %>subsetList['<%=sample.getSampleId()%>'] = <%=javascriptArray(getSubsetNames(analysis))%>;<%
        }
    }

for (Map.Entry<String, Map<String, FlowJoWorkspace.SampleInfo>> keywordEntry : keywordValueSampleMap.entrySet())
{
 String keyword = keywordEntry.getKey();%>
    keywordValueSubsetListMap['<%=keyword%>'] = {};
    <%
    for (Map.Entry<String, FlowJoWorkspace.SampleInfo> valueEntry : keywordEntry.getValue().entrySet()) {
    String value = valueEntry.getKey();
    %>
    keywordValueSubsetListMap['<%=keyword%>']['<%=value%>'] = subsetList['<%=valueEntry.getValue().getSampleId()%>'];
    <% }
    }
    %>

</script>

<form method="POST" action="<%=formAction(ScriptController.Action.editCompensationCalculation)%>">
    <input type="hidden" name="workspaceObject" value="<%=PageFlowUtil.encodeObject(form.workspace)%>">
    <table class="normal" border="1"><tr><th rowspan="2">Channel</th><th colspan="3">Positive</th><th colspan="3">
        Negative</th></tr>
        <tr><th>Keyword</th><th>Value</th><th>Subset</th><th>Keyword</th><th>Value</th><th>Subset</th></tr>
        <% for (int i = 0; i < form.parameters.length; i ++)
        {
            String parameter = form.parameters[i];
        %>
        <tr><td><%=h(parameter)%></td>
            <td><%=selectKeywordNames(Sign.positive, i)%></td>
            <td><%=selectKeywordValues(Sign.positive, i)%></td>
            <td><%=selectSubsets(Sign.positive, i)%></td>
            <td><%=selectKeywordNames(Sign.negative, i)%></td>
            <td><%=selectKeywordValues(Sign.negative, i)%></td>
            <td><%=selectSubsets(Sign.negative, i)%></td>
            <% if (i == 0) { %>
                <td><input type="button" value="Universal" onclick="universalNegative()"></td>
            <% } %>
        </tr>
        <% } %>
    </table>
    <input type="Submit" value="Submit">
</form>
<p><b>Instructions:</b></p>
<p>
    For each parameter which requires compensation, specify the keyword name and value
    which are to be used to identify the compensation control in experiment runs.
</p>
<p>
    This page only allows you to choose keyword/value pairs that uniquely identify a
    sample in the workspace.  If you do not see the keyword that you would like to use,
    this might be because the workspace that you uploaded contained more than one sample
    with that keyword value.  Choose a different keyword at this point.
</p>
