
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToAnalyzeForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsView" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.flow.data.*"%>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController"%>
<%@ page import="org.labkey.flow.analysis.model.PopulationSet"%>
<%@ taglib uri="http://cpas.fhcrc.org/taglib/cpas" prefix="cpas" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%!
    String select(String name, Object curVal, Collection<? extends FlowObject> objs, String nullLabel)
    {
        return PFUtil.strSelect(name, FlowObject.idLabelsFor(objs, nullLabel), curVal);
    }
%>
<% ChooseRunsToAnalyzeForm form = (ChooseRunsToAnalyzeForm) __form;
    form.populate();
    ChooseRunsView view = new ChooseRunsView(form);
    Collection<FlowExperiment> targetExperiments = new ArrayList(form.getAvailableAnalyses());
    targetExperiments.add(null);
    PopulationSet analysis = form.getProtocol().getCompensationCalcOrAnalysis(form.getProtocolStep());
%>
<cpas:errors/>
<form class="normal" method="POST" action="<%=PFUtil.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze, getContainer())%>">
    <table>
        <tr><td>Analysis script to use:</td>
            <td><select name="scriptId" onchange="this.form.submit()">
                <cpas:options value="<%=form.getProtocol().getScriptId()%>"
                              map="<%=FlowObject.idLabelsFor(form.getAvailableGateDefinitionSets())%>"/>
            </select></td></tr>
        <tr><td>Analysis step to perform:</td>
            <td><select name="actionSequence" onchange="this.form.submit()">
                <cpas:options value="<%=form.getProtocolStep().getDefaultActionSequence()%>"
                              map="<%=form.getAvailableSteps(form.getProtocol())%>"/>
            </select></td></tr>
        <tr><td>Analysis to put results in:</td>
            <td><select name="ff_targetExperimentId" onchange="this.form.submit()">
                <cpas:options value="<%=form.ff_targetExperimentId == null ? null : Integer.valueOf(form.ff_targetExperimentId)%>"
                              map="<%=FlowObject.idLabelsFor(targetExperiments, "<create new>")%>"/>
            </select></td>
        </tr>
        <% if (analysis.requiresCompensationMatrix())
        { %>
        <tr><td>Compensation matrix to use:</td>
            <td><select name="ff_compensationMatrixOption" onchange="this.form.submit()">
                <cpas:options value="<%=form.ff_compensationMatrixOption%>" map="<%=form.getCompensationMatrixOptions()%>" />
            </select></td>
        </tr>
        <% } %>
    </table>
</form>
<%include(view, out);%>
