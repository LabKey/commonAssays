<%@ page import="org.fhcrc.cpas.flow.script.FlowAnalyzer" %>
<%@ page import="com.labkey.flow.web.GraphSpec" %>
<%@ page import="com.labkey.flow.web.SubsetSpec" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowScript" %>
<%@ page import="Flow.Well.ChooseGraphForm" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.fhcrc.cpas.flow.util.PFUtil" %>
<%@ page import="Flow.Well.WellController" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowWell" %>
<%@ page import="java.util.*" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowProtocolStep" %>
<%@ page import="Flow.FlowParam" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
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
            <cpas:options value="<%=form.getScriptId()%>" map="<%=scriptOptions%>"/>
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
            <cpas:options value="<%=form.getCompId()%>" map="<%=compOptions%>"/>
        </select></td></tr>
        <% } %>
    </table>
</form>

<%
    List<String> subsets = Collections.EMPTY_LIST;
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
                <% for (String subset : subsets)
                { %>
                <option value="<%=h(subset)%>" <%=subset.equals(form.getSubset()) ? " selected" : ""%>><%=h(subset)%></option>
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
        GraphSpec graphspec = new GraphSpec(SubsetSpec.fromString(form.getSubset()), form.getXaxis(), form.getYaxis());
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
