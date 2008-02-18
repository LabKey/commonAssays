<%@ page import="org.apache.commons.collections.map.MultiValueMap" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.analysis.model.Analysis"%>
<%@ page import="org.labkey.flow.analysis.model.AutoCompensationScript"%>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    boolean canEdit = form.canEdit();
    Map<FieldKey, String> fieldOptions = form.getFieldOptions();
    Map<String, String> opOptions = form.getOpOptions();
    int clauseCount = Math.max(form.ff_filter_field.length + 2, 4);
%>
<script type="text/javascript" src="<%=request.getContextPath()%>/Flow/editCompensationCalculation.js"></script>
<script type="text/javascript">
function o() { var o = {}; for (var i = 0; i < arguments.length; i += 2) o[arguments[i]] = arguments[i + 1]; return o; }
var parameters = <%=javascriptArray(form.parameters)%>
var AutoComp = {};
<%
    for (AutoCompensationScript autoComp : form.workspace.getAutoCompensationScripts())
    {
        %>AutoComp['<%=autoComp.getName()%>']={<%

        // 'criteria' : [ primarykKeyword, secondaryKeyword, secondaryValue ]
        AutoCompensationScript.MatchingCriteria criteria = autoComp.getCriteria();
        if (criteria != null)
        {
        %>'criteria' : <%=javascriptArray(criteria.getPrimaryKeyword(), criteria.getSecondaryKeyword(), criteria.getSecondaryValue())%>,
        <%
        }

        %>'params' : {<%
        // params : { 'param name' : [ searchKeyword, searchValue, positiveSubset, negativeSubset ] }
        String and = "\n";
        for (AutoCompensationScript.ParameterDefinition param : autoComp.getParameters().values())
        {
            %><%=and%>'<%= param.getParameterName()%>' : <%=javascriptArray(
                param.getSearchKeyword(), param.getSearchValue(), param.getPositiveGate(), param.getNegativeGate())%><%
            and = ",\n";
        }
        %>}
        };<%
        out.println();
    }
%>
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
<p><h2>Instructions:</h2>
    For each parameter which requires compensation, specify the keyword name and value
    which are to be used to identify the compensation control in experiment runs.
    If your FlowJo workspace uses AutoCompensation scripts, you can select the script
    from the drop down below to quickly populate the form fields.
</p>
<p>
    Filters may be applied to this analysis script.  The set of keyword and
    value pairs <i>must</i> all match in the FCS header to be included in the analysis.
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

    <p>
    <b>AutoCompensation script:</b><br/>
    <select name="selectAutoCompScript" onchange="populateAutoComp(this);">
    <%
        %><option value="" selected></option><%
        for (AutoCompensationScript autoComp : form.workspace.getAutoCompensationScripts())
        {
            %>
            <option value="<%=autoComp.getName()%>"><%=autoComp.getName()%></option><%
        }
    %>
    </select>
    </p>

    <p>
    <b>Filter FCS files by keyword:</b><br/>
    <table class="normal">
        <tr><th/><th>Keyword</th><th/><th>Value</th></tr>
        <%
        for (int i = 0; i < clauseCount; i++)
        {
            FieldKey field = null;
            String op = null;
            String value = null;

            if (i < form.ff_filter_field.length)
            {
                field = form.ff_filter_field[i];
                op = form.ff_filter_op[i];
                value = form.ff_filter_value[i];
            }

            if (!canEdit && field == null)
                continue;

            %>
            <tr>
                <td class="normal"><%= i == 0 ? "" : "and" %></td>
            <% if (canEdit) { %>
                <td class="normal"><select name="ff_filter_field"><labkey:options value="<%=field%>" map="<%=fieldOptions%>" /></select></td>
                <td class="normal"><select name="ff_filter_op"><labkey:options value="<%=op%>" map="<%=opOptions%>" /></select></td>
                <td class="normal"><input type="text" name="ff_filter_value" value="<%=h(value)%>"></td>
            <% } else { %>
                <td class="normal"><%=fieldOptions.get(field)%></td>
                <td class="normal"><%=opOptions.get(op)%></td>
                <td class="normal"><%=h(value)%></td>
            <% } %>
            </tr>
            <%
        }
        %>
    </table>
    </p>

    <p>
    <b>Select Compensation:</b><br/>
    <table class="normal" border="1">
        <tr><th rowspan="2">Channel</th><th colspan="3">Positive</th><th colspan="3">Negative</th></tr>
        <tr><th>Keyword</th><th>Value</th><th>Subset</th><th>Keyword</th><th>Value</th><th>Subset</th></tr>
        <% for (int i = 0; i < form.parameters.length; i ++)
        {
            String parameter = form.parameters[i];
        %>
        <tr id="<%=h(parameter)%>">
            <td><%=h(parameter)%></td>
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
    </p>
    
    <input type="hidden" name="workspaceObject" value="<%=PageFlowUtil.encodeObject(form.workspace)%>">
    <input type="Submit" value="Submit">
</form>

