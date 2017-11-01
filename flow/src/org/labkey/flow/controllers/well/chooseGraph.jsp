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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="org.labkey.flow.analysis.web.ScriptAnalyzer" %>
<%@ page import="org.labkey.flow.analysis.web.SubsetSpec" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page import="org.labkey.flow.controllers.well.ChooseGraphForm" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="org.labkey.flow.script.FlowAnalyzer" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ChooseGraphForm form = (ChooseGraphForm) __form;

    boolean hasScripts = false;
    boolean hasComps = false;
    FlowWell well = form.getWell();
    Map<Integer, String> scriptOptions = new LinkedHashMap();
    if (form.getScript() == null)
    {
        scriptOptions.put(0, "None");
    }
    FlowScript wellScript = well.getScript();
    if (wellScript != null)
    {
        scriptOptions.put(wellScript.getScriptId(), wellScript.getName());
        hasScripts = true;
    }
    FlowScript[] scripts = FlowScript.getScripts(getContainer());
    Arrays.sort(scripts);
    for (FlowScript script : scripts)
    {
        if (scriptOptions.containsKey(script.getScriptId()))
        {
            continue;
        }
        if (script.hasStep(FlowProtocolStep.analysis) || script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            scriptOptions.put(script.getScriptId(), script.getName());
            hasScripts = true;
        }
    }

    Map<Integer, String> compOptions = new LinkedHashMap();
    if (form.getCompensationMatrix() == null)
    {
        compOptions.put(0, "None");
    }
    FlowCompensationMatrix wellCompensationMatrix = well.getCompensationMatrix();
    if (wellCompensationMatrix != null)
    {
        compOptions.put(wellCompensationMatrix.getCompId(), wellCompensationMatrix.getLabel());
        hasComps = true;
    }
    List<FlowCompensationMatrix> comps = FlowCompensationMatrix.getCompensationMatrices(getContainer());
    Collections.sort(comps);
    for (FlowCompensationMatrix comp : comps)
    {
        if (compOptions.containsKey(comp.getCompId()))
            continue;
        compOptions.put(comp.getCompId(), comp.getLabel(true));
        hasComps = true;
    }
    FlowScript script = form.getScript();
    FlowCompensationMatrix matrix = form.getCompensationMatrix();

    FlowProtocolStep step = FlowProtocolStep.fromActionSequence(form.getActionSequence());
    List<FlowProtocolStep> steps = new ArrayList();
    if (script != null)
    {
        if (script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            steps.add(FlowProtocolStep.calculateCompensation);
        }
        if (script.hasStep(FlowProtocolStep.analysis))
        {
            steps.add(FlowProtocolStep.analysis);
        }
    }
    if (steps.size() > 0 && !steps.contains(step))
    {
        step = steps.get(0);
    }
%>
<form>
    <input type="hidden" name="wellId" value="<%=form.getWellId()%>">
    <table class="lk-fields-table">
        <% if (hasScripts)
        {
        %>
        <tr><td>Analysis Script:</td><td><select name="<%=FlowParam.scriptId%>" onchange="this.form.submit()">
            <labkey:options value="<%=form.getScriptId()%>" map="<%=scriptOptions%>"/>
        </select></td></tr>
        <%
            }
        %>
        <% if (steps.size() > 1)
        { %>
        <tr><td>Analysis Step:</td><td><select name="<%=FlowParam.actionSequence%>" onchange="this.form.submit()">
            <% for (FlowProtocolStep s : steps)
            { %>
            <option value="<%=s.getDefaultActionSequence()%>"<%=selected(s == step)%>><%=h(s.getLabel())%></option>
            <% } %>
        </select></td></tr>
        <% } %>
        <% if (hasComps)
        {
        %>
        <tr><td>Compensation Matrix:</td><td><select name="compId" onchange="this.form.submit()">
            <labkey:options value="<%=form.getCompId()%>" map="<%=compOptions%>"/>
        </select></td></tr>
        <% } %>
    </table>
</form>

<%
    Collection<SubsetSpec> subsets = Collections.emptyList();
    if (script != null)
    {
        subsets = ScriptAnalyzer.getSubsets(script.getAnalysisScript(), step == FlowProtocolStep.analysis, step == FlowProtocolStep.calculateCompensation, false);
    }
    Map<String, String> parameters = FlowAnalyzer.getParameters(well, matrix == null ? null : matrix.getCompensationMatrix());
%>


<form id="chooseGraph">
    <input type="hidden" name="wellId" value="<%=form.getWellId()%>">
    <input type="hidden" name="scriptId" value="<%=form.getScriptId()%>">
    <input type="hidden" name="compId" value="<%=form.getCompId()%>">
    <table class="lk-fields-table">
        <tr><th>Subset</th><th>X-Axis</th><th>Y-Axis</th></tr>
        <tr>
            <td><select name="subset">
                <option value="">Ungated</option>
                <% for (SubsetSpec subset : subsets)
                { %>
                <option value="<%=h(subset)%>"<%=selected(subset.toString().equals(form.getSubset()))%>><%=h(subset)%></option>
                <% } %>
            </select>
            </td>
            <td>
                <select name="xaxis">
                    <% for (Map.Entry<String, String> param : parameters.entrySet())
                    {%>
                    <option value="<%=h(param.getKey())%>"<%=selected(param.getKey().equals(form.getXaxis()))%>><%=h(param.getValue())%></option>
                    <% } %>
                </select>

            </td>
            <td>
                <select name="yaxis">
                    <option value="">[[histogram]]</option>
                    <% for (Map.Entry<String, String> param : parameters.entrySet())
                    {%>
                    <option value="<%=h(param.getKey())%>"<%=selected(param.getKey().equals(form.getYaxis()))%>><%=h(param.getValue())%></option>
                    <% } %>
                </select>
            </td>
        </tr>

    </table>
    <input class="labkey-button" type="submit" value="Show Graph">
</form>
<%
    if (form.getXaxis() != null)
    {
        String[] params;
        if (form.getYaxis() != null)
        {
            params = new String[]{form.getXaxis(), form.getYaxis()};
        }
        else
        {
            params = new String[]{form.getXaxis()};
        }
        GraphSpec graphspec = new GraphSpec(SubsetSpec.fromEscapedString(form.getSubset()), params);
        ActionURL urlGenerateGraph = new ActionURL(WellController.GenerateGraphAction.class, getContainer());
        well.addParams(urlGenerateGraph);
        if (script != null)
        {
            script.addParams(urlGenerateGraph);
        }
        if (matrix != null)
        {
            matrix.addParams(urlGenerateGraph);
        }
        if (step != null)
        {
            step.addParams(urlGenerateGraph);
        }
        urlGenerateGraph.addParameter("graph", graphspec.toString());

%>
<script type="text/javascript" src="<%=getContextPath()%>/Flow/util.js"></script>
<br/>
<p><img src="<%=h(urlGenerateGraph)%>" onerror="flowImgError(this);"></p>
<% } %>
