<%@ page import="org.labkey.flow.script.FlowAnalyzer" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="org.labkey.flow.analysis.web.SubsetSpec" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.controllers.well.ChooseGraphForm" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ChooseGraphForm form = (ChooseGraphForm) __form;

    List<FlowScript> scripts = new ArrayList();
    for (FlowScript script : FlowScript.getScripts(getContainer()))
    {
        if (script.hasStep(FlowProtocolStep.analysis) || script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            scripts.add(script);
        }
    }
    List<FlowCompensationMatrix> comps = FlowCompensationMatrix.getCompensationMatrices(getContainer());

    FlowWell well = form.getWell();
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
    <table>
        <% if (scripts.size() != 0)
        {
            Map<Integer, String> scriptOptions = new LinkedHashMap();
            if (script == null)
            {
                scriptOptions.put(0, "None");
            }
            for (FlowScript s : scripts)
            {
                scriptOptions.put(s.getScriptId(), s.getLabel());
            }%>
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
            <option value="<%=s.getDefaultActionSequence()%>"<%= s == step ? " selected" : ""%>><%=s.getLabel()%></option>
            <% } %>
        </select></td></tr>
        <% } %>
        <% if (comps.size() != 0)
        {
            Map<Integer, String> compOptions = new LinkedHashMap();
            if (matrix == null)
            {
                compOptions.put(0, "None");
            }
            for (FlowCompensationMatrix comp : comps)
            {
                compOptions.put(comp.getCompId(), comp.getLabel(true));
            }
        %>
        <tr><td>Compensation Matrix:</td><td><select name="compId" onchange="this.form.submit()">
            <labkey:options value="<%=form.getCompId()%>" map="<%=compOptions%>"/>
        </select></td></tr>
        <% } %>
    </table>
</form>

<%
    Collection<SubsetSpec> subsets = Collections.EMPTY_LIST;
    if (script != null)
    {
        subsets = FlowAnalyzer.getSubsets(script.getAnalysisScript(), step);
    }
    Map<String, String> parameters = FlowAnalyzer.getParameters(well, matrix == null ? null : matrix.getCompensationMatrix());
%>


<form>
    <input type="hidden" name="wellId" value="<%=form.getWellId()%>">
    <input type="hidden" name="scriptId" value="<%=form.getScriptId()%>">
    <input type="hidden" name="compId" value="<%=form.getCompId()%>">
    <table><tr><th>Subset</th><th>X-Axis</th><th>Y-Axis</th></tr>
        <tr>
            <td><select name="subset">
                <option value="">Ungated</option>
                <% for (SubsetSpec subset : subsets)
                { %>
                <option value="<%=h(subset)%>" <%=subset.toString().equals(form.getSubset()) ? " selected" : ""%>><%=h(subset)%></option>
                <% } %>
            </select>
            </td>
            <td>
                <select name="xaxis">
                    <% for (Map.Entry<String, String> param : parameters.entrySet())
                    {%>
                    <option value="<%=h(param.getKey())%>" <%=param.getKey().equals(form.getXaxis()) ? " selected" : ""%>><%=h(param.getValue())%></option>
                    <% } %>
                </select>

            </td>
            <td>
                <select name="yaxis">
                    <option value="">[[histogram]]</option>
                    <% for (Map.Entry<String, String> param : parameters.entrySet())
                    {%>
                    <option value="<%=h(param.getKey())%>" <%=param.getKey().equals(form.getYaxis()) ? " selected" : ""%>><%=h(param.getValue())%></option>
                    <% } %>
                </select>
            </td>
        </tr>

    </table>
    <input type="submit" value="Show Graph">
</form>
<%
    if (form.getXaxis() != null)
    {
        String[] params;
        if (form.getYaxis() != null)
        {
            params = new String[] { form.getXaxis(), form.getYaxis() };
        }
        else
        {
            params = new String[] { form.getXaxis() };
        }
        GraphSpec graphspec = new GraphSpec(SubsetSpec.fromString(form.getSubset()), params);
        ViewURLHelper urlGenerateGraph = PFUtil.urlFor(WellController.Action.generateGraph, getContainer());
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
<p><img src="<%=h(urlGenerateGraph)%>"></p>
<% } %>
