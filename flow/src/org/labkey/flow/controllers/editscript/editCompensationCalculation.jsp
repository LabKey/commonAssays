<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace"%>
<%@ page import="org.labkey.flow.analysis.model.Analysis"%>
<%@ page import="org.apache.commons.collections.map.MultiValueMap" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<script type="text/javascript" src="<%=request.getContextPath()%>/Flow/editCompensationCalculation.js"></script>
<script type="text/javascript">
function o() { var o = {}; for (var i = 0; i < arguments.length; i += 2) o[arguments[i]] = arguments[i + 1]; return o; }
var SS = []; // SUBSETS
<%
    // these arrays are duplicated a lot so only output each one once
    MultiValueMap arrays = new MultiValueMap();
    for (FlowJoWorkspace.SampleInfo sample : form.workspace.getSamples())
    {
        Analysis analysis = form.workspace.getSampleAnalysis(sample);
        if (analysis != null)
        {
            String array = javascriptArray(getSubsetNames(analysis));
            arrays.put(array,sample.getSampleId());
        }
    }
    HashMap<String,Integer> sampleToSubsetArray = new HashMap<String,Integer>();
    Integer index = 0;
    for (String jsArray : (Set<String>)arrays.keySet())
    {
        %>SS.push(<%=jsArray%>);<%
        out.println();
        Collection<String> sampleids = (Collection<String>)arrays.get(jsArray);
        for (String sampleid : sampleids)
            sampleToSubsetArray.put(sampleid,index);
        index = index.intValue() + 1;
    }
%>
var KV = {}; // KEYWORD->VALUE->SUBSET
<%
    for (Map.Entry<String, Map<String, FlowJoWorkspace.SampleInfo>> keywordEntry : keywordValueSampleMap.entrySet())
    {
        String keyword = keywordEntry.getKey();
        %>KV['<%=keyword%>']=o(<%
        String and="";
        for (Map.Entry<String, FlowJoWorkspace.SampleInfo> valueEntry : keywordEntry.getValue().entrySet())
        {
            String value = valueEntry.getKey();
            String sampleId = valueEntry.getValue().getSampleId(); 
            %><%=and%>'<%=value%>', SS[<%=sampleToSubsetArray.get(sampleId)%>]<%
            and = ",";
        }
        %>);<%
        out.println();
    }
%>
var keywordValueSubsetListMap = KV;
</script>
<p><b>Instructions:</b></p>
<p>
    For each parameter which requires compensation, specify the keyword name and value
    which are to be used to identify the compensation control in experiment runs.
</p>
<p>
    <b>If you do not see the keyword you are looking for:</b><br>
    This page only allows you to choose keyword/value pairs that uniquely identify a
    sample in the workspace.  If you do not see the keyword that you would like to use,
    this might be because the workspace that you uploaded contained more than one sample
    with that keyword value.  Use FlowJo to save a workspace that contains only one set of
    compensation controls, and upload that new workspace.
</p>

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

