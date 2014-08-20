<%
/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.run.MoveToAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page extends="org.labkey.api.jsp.FormPage"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<%
    MoveToAnalysisForm form = (MoveToAnalysisForm) HttpView.currentModel();
    FlowRun run = form.getRun();
    Map<Integer, String> analyses = new LinkedHashMap();
    for (FlowExperiment experiment : FlowExperiment.getAnalyses(getContainer()))
    {
        analyses.put(experiment.getExperimentId(), experiment.getName());
    }
%>
<% if (analyses.size() == 0) { %>
There are no analyses to put this run in.

<%
    return; } %>
<labkey:form action="<%=h(run.urlFor(RunController.MoveToAnalysisAction.class))%>" method="POST">
    <p>When you move this run into an analysis, the statistics and graphs will be recalculated.  Which analysis do you
    want to put the results in?<br>
    <select name="experimentId"><labkey:options value="<%=form.getExperimentId()%>" map="<%=analyses%>" /></select>
    </p>
    <labkey:button text="Move Run" />
    <labkey:button text="Cancel" href="<%=run.urlShow()%>"/>
</labkey:form>