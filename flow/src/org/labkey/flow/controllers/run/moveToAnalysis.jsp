<%@ page import="org.labkey.flow.controllers.run.MoveToAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page extends="org.labkey.api.jsp.FormPage"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<% MoveToAnalysisForm form = (MoveToAnalysisForm) __form;
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
<form action="<%=h(run.urlFor(RunController.Action.moveToAnalysis))%>" method="POST">
    <p>When you move this run into an analysis, the statistics and graphs will be recalculated.  Which analysis do you
    want to put the results in?<br>
    <select name="experimentId"><labkey:options value="<%=form.getExperimentId()%>" map="<%=analyses%>" /></select>
    </p>
    <labkey:button text="Move Run" />
    <labkey:button text="Cancel" href="<%=run.urlShow()%>"/>
</form>