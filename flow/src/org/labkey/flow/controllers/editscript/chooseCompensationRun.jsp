<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page extends="org.labkey.flow.controllers.editscript.CompensationCalculationPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas"%>
<form action="<%=form.analysisScript.urlFor(ScriptController.Action.chooseCompensationRun)%>" method="POST">
<p>
    In order to define the compensation calculation, you need to tell <%=FlowModule.getLongProductName()%> which keyword
    values identify which compensation well.  Choose an experiment run which has compensation controls in it.  On the next page
    you will then have the opportunity to choose which keywords in that experiment run identify the compensation controls.
</p>
<p>
    Which experiment run do you want to use?<br>
    <select name="selectedRunId">
        <cpas:options value="<%=0%>" map="<%=form.getExperimentRuns()%>"/> 
    </select>
</p>
<cpas:button text="Next Step" />
</form>