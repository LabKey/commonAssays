<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.analysis.model.AutoCompensationScript" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    boolean canEdit = form.canEdit();
    Map<FieldKey, String> fieldOptions = getFieldOptions();
    Map<String, String> opOptions = form.getOpOptions();
    int clauseCount = Math.max(form.ff_filter_field.length, 3);
%>
<labkey:errors/>
<script type="text/javascript" src="<%=h(request.getContextPath())%>/Flow/editCompensationCalculation.js"></script>
<script type="text/javascript">
function o() { var o = {}; for (var i = 0; i < arguments.length; i += 2) o[arguments[i]] = arguments[i + 1]; return o; }
var parameters = <%=text(javascriptArray(form.parameters))%>
var AutoComp = {};
<%
    boolean hasAutoCompScripts = form.workspace.getAutoCompensationScripts().size() > 0;
    for (AutoCompensationScript autoComp : form.workspace.getAutoCompensationScripts())
    {
        %>AutoComp[<%=PageFlowUtil.jsString(autoComp.getName())%>]={<%

        // 'criteria' : [ primarykKeyword, secondaryKeyword, secondaryValue ]
        AutoCompensationScript.MatchingCriteria criteria = autoComp.getCriteria();
        if (criteria != null)
        {
        %>'criteria' : <%=text(javascriptArray(criteria.getPrimaryKeyword(), criteria.getSecondaryKeyword(), criteria.getSecondaryValue()))%>,
        <%
        }

        %>'params' : {<%
        // params : { 'param name' : [ searchKeyword, searchValue, positiveSubset, negativeSubset ] }
        String and = "\n";
        for (AutoCompensationScript.ParameterDefinition param : autoComp.getParameters().values())
        {
            %><%=text(and)%><%= PageFlowUtil.jsString(param.getParameterName())%> : <%=text(javascriptArray(
                param.getSearchKeyword(), param.getSearchValue(), param.getPositiveGate(), param.getNegativeGate()))%><%
            and = ",\n";
        }
        %>}
        };<%
        out.println();
    }
%>
var SS = []; // SUBSETS
<%
    Map<Integer, Integer> hashToIndexMap = new TreeMap();
    Integer index = 0;
    for (Map<String, List<String>> valueSubsets : keywordValueSampleMap.values())
    {
        for (List<String> subsets : valueSubsets.values())
        {
            int subsetHash = subsets.hashCode();
            if (!hashToIndexMap.containsKey(subsetHash))
            {
                hashToIndexMap.put(subsetHash, index);
                index = index.intValue() + 1;
                %>SS.push(<%=text(javascriptArray(subsets))%>);<%
                out.println();
            }
        }
    }
%>
var KV = {}; // KEYWORD->VALUE->SUBSET
<%
    for (Map.Entry<String, Map<String, List<String>>> keywordEntry : keywordValueSampleMap.entrySet())
    {
        String keyword = keywordEntry.getKey();
        %>KV[<%=PageFlowUtil.jsString(keyword)%>]=o(<%
        String and="";
        for (Map.Entry<String, List<String>> valueEntry : keywordEntry.getValue().entrySet())
        {
            String value = valueEntry.getKey();
            int subsetHash = valueEntry.getValue().hashCode();
            %><%=text(and)%><%=PageFlowUtil.jsString(value)%>, SS[<%=hashToIndexMap.get(subsetHash)%>]<%
            and = ",";
        }
        %>);<%
        out.println();
    }
%>
var keywordValueSubsetListMap = KV; 
</script>

<labkey:form method="POST" action="<%=h(formAction(ScriptController.EditCompensationCalculationAction.class))%>">

<% if (hasAutoCompScripts) { %>
        <labkey:panel title="Choose AutoCompensation script">
            <p>Your FlowJo workspace contains AutoCompensation scripts; you can optionally
            select a script from the drop down below to quickly populate the compensation
            calculation form fields.</p>
            <select name="selectAutoCompScript" onchange="populateAutoComp(this);">
            <%
                %><option value=""></option><%
                for (AutoCompensationScript autoComp : form.workspace.getAutoCompensationScripts())
                {
                    %>
                    <option value="<%=h(autoComp.getName())%>"<%=selected(autoComp.getName().equals(form.selectAutoCompScript))%>><%=h(autoComp.getName())%></option><%
                }
            %>
            </select>
        </labkey:panel>
<% } %>

        <labkey:panel title="Analyze FCS Files where">
            <p>Filters may optionally be applied to this analysis script.  The set of keyword and
            value pairs <i>must all</i> match in the FCS header to be included in the analysis.
            You can change the filter later by editing the script settings from the
            analysis script start page.</p>
            <table class="lk-fields-table">
                <tr>
                    <td>&nbsp;</td>
                    <td style="font-weight: bold;">Keyword</td>
                    <td style="font-weight: bold;">Operator</td>
                    <td style="font-weight: bold;">Value</td>
                </tr>
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
                        <td><%= text(i == 0 ? "" : "and") %></td>
                    <% if (canEdit) { %>
                        <td><select name="ff_filter_field"><labkey:options value="<%=field%>" map="<%=fieldOptions%>" /></select></td>
                        <td><select name="ff_filter_op"><labkey:options value="<%=op%>" map="<%=opOptions%>" /></select></td>
                        <td><input type="text" name="ff_filter_value" value="<%=h(value)%>"></td>
                    <% } else { %>
                        <td><%=h(fieldOptions.get(field))%></td>
                        <td><%=h(opOptions.get(op))%></td>
                        <td><%=h(value)%></td>
                    <% } %>
                    </tr>
                    <%
                }
                %>
            </table>
        </labkey:panel>

        <labkey:panel title="Select compensation">
            <p>For each parameter which requires compensation, specify the keyword name and value
            which are to be used to identify the compensation control in experiment runs.</p>
            <p><b>If you do not see the keyword you are looking for:</b><br>
            This page only allows you to choose keyword/value pairs that uniquely identify a
            sample in the workspace.  If you do not see the keyword that you would like to use,
            this might be because the workspace that you uploaded contained more than one sample
            with that keyword value.  Use FlowJo to save a workspace template with AutoCompensation scripts or
            a workspace containing only one set of compensation controls, and upload that new workspace.
            </p>
            <table class="labkey-data-region-legacy labkey-show-borders">
                <tr><th rowspan="2">Channel</th><th colspan="3">Positive</th><th colspan="3">Negative</th></tr>
                <tr><th>Keyword</th><th>Value</th><th>Subset</th><th>Keyword</th><th>Value</th><th>Subset</th></tr>
                <%
                    for (int i = 0; i < form.parameters.length; i ++)
                    {
                        String parameter = form.parameters[i];
                %>
                <tr id="<%=h(parameter)%>" class="<%=getShadeRowClass(i % 2 == 0)%>">
                    <td><%=h(parameter)%></td>
                    <td><%=text(selectKeywordNames(Sign.positive, i))%></td>
                    <td><%=text(selectKeywordValues(Sign.positive, i))%></td>
                    <td><%=text(selectSubsets(Sign.positive, i))%></td>
                    <td><%=text(selectKeywordNames(Sign.negative, i))%></td>
                    <td><%=text(selectKeywordValues(Sign.negative, i))%></td>
                    <td>
                        <%=text(selectSubsets(Sign.negative, i))%>
                        <% if (i == 0) { %>
                            <input class="labkey-button" type="button" value="Universal" onclick="universalNegative()">
                        <% } %>
                    </td>
                </tr>
                <%
                    }
                %>
            </table>
        </labkey:panel>
<%
String[] analysisNames = this.getGroupAnalysisDisplayNames();
if (analysisNames.length > 0)
{
%>
        <labkey:panel title="Choose source of gating">
            <p>You can choose to use the gating from either the sample identified
            by the unique keywork/value pair from the compensation
            calculation above <i>or</i> from one of the named group's in the workspace.</p>
            <p>By default, the sample's gating will be used.  However, if this is a
            <b>workspace template</b>, you will most likely need to select a group name
            from the drop down that has gating for the given subsets.</p>
            <select name="selectGroupName" title="Use gating either from the sample or from a group">
                <option value="" title="Use gating from the sample identified by the Keyword/Value pair">Sample</option>
                <% for (String group : analysisNames)
                { %>
                <option value="<%=h(group)%>"<%=selected(group.equals(form.selectGroupName))%>>Group <%=h(group)%></option>
                <% } %>
            </select>
        </labkey:panel>
<% } %>

    <input type="hidden" name="workspaceObject" value="<%=PageFlowUtil.encodeObject(form.workspace)%>">
    <input class="labkey-button" type="Submit" value="Submit">
</labkey:form>

