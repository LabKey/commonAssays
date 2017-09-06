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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.view.template.ClientDependencies"%>
<%@ page import="org.labkey.flow.analysis.model.PopulationSet"%>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToAnalyzeForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsView" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowObject" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        // TODO: --Ext3-- This should be declared as part of the included views
        dependencies.add("clientapi/ext3");
        // TODO: ColumnAnalyticsProvider dependencies should be coming from the ChooseRunsView data region
        dependencies.add("vis/vis");
        dependencies.add("vis/ColumnVisualizationAnalytics.js");
        dependencies.add("vis/ColumnVisualizationAnalytics.css");
        dependencies.add("query/ColumnQueryAnalytics.js");
        dependencies.add("query/ColumnSummaryStatistics");
    }

    String select(String name, Object curVal, Collection<? extends FlowObject> objs, String nullLabel)
    {
        return PageFlowUtil.strSelect(name, FlowObject.idLabelsFor(objs, nullLabel), curVal);
    }
%>
<%
    JspView<ChooseRunsToAnalyzeForm> me = (JspView<ChooseRunsToAnalyzeForm>) HttpView.currentView();
    ChooseRunsToAnalyzeForm form = me.getModelBean();
    ChooseRunsView view = new ChooseRunsView(form);
    Collection<FlowExperiment> targetExperiments = new ArrayList(form.getAvailableAnalyses());
    targetExperiments.add(null);
    PopulationSet analysis = form.getProtocol().getCompensationCalcOrAnalysis(form.getProtocolStep());
%>
<style type="text/css">
    .disabledRow td, .disabledRow td a, .disabledRow td a:link
    {
        color: gray;
    }
</style>
<labkey:errors/>
<labkey:form method="POST" action="<%=new ActionURL(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, getContainer())%>">
    <table class="lk-fields-table">
        <tr><td>Analysis script to use:</td>
            <td><select name="scriptId" onchange="this.form.submit()">
                <labkey:options value="<%=form.getProtocol().getScriptId()%>"
                              map="<%=FlowObject.idLabelsFor(form.getAvailableGateDefinitionSets())%>"/>
            </select></td></tr>
        <tr><td>Analysis step to perform:</td>
            <td><select name="actionSequence" onchange="this.form.submit()">
                <labkey:options value="<%=form.getProtocolStep().getDefaultActionSequence()%>"
                              map="<%=form.getAvailableSteps(form.getProtocol())%>"/>
            </select></td></tr>
        <tr><td>Analysis folder to put results in:</td>
            <td><select name="ff_targetExperimentId" onchange="this.form.submit()">
                <labkey:options value="<%=form.ff_targetExperimentId == null ? null : Integer.valueOf(form.ff_targetExperimentId)%>"
                              map="<%=FlowObject.idLabelsFor(targetExperiments, \"<create new>\")%>"/>
            </select></td>
        </tr>
        <% if (analysis.requiresCompensationMatrix())
        { %>
        <tr><td>Compensation matrix to use:</td>
            <td><select name="ff_compensationMatrixOption" onchange="this.form.submit()">
                <labkey:options value="<%=form.ff_compensationMatrixOption%>" map="<%=form.getCompensationMatrixOptions()%>" />
            </select></td>
        </tr>
        <% } %>
    </table>
    <br/>
    <%include(view, out);%>
</labkey:form>
