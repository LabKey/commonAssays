<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToAnalyzeForm"%>
<%@ page import="org.labkey.api.data.DataRegion"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController.Action" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="java.util.HashSet" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% ChooseRunsToAnalyzeForm form = (ChooseRunsToAnalyzeForm) __form; %>
<form method="POST" action="analyzeSelectedRuns.post">
    <%=errors()%>
    <p>What do you want to call the new analysis?<br>
        <% String name = form.ff_analysisName;
            if (StringUtils.isEmpty(name))
            {
                Set<String> namesInUse = new HashSet();
                for (FlowExperiment experiment : FlowExperiment.getExperiments(getContainer()))
                {
                    namesInUse.add(experiment.getName().toLowerCase());
                }
                String baseName = "Analysis";
                name = baseName;
                int i = 0;
                while (namesInUse.contains(name.toLowerCase()))
                {
                    i ++;
                    name = baseName + i;
                }
            }
        %>

        <input type="text" name="ff_analysisName" value="<%=h(name)%>">
    </p>

    <cpas:button text="Analyze runs" action="<%=Action.analyzeSelectedRuns%>"/>
    <cpas:button text="Go back" action="<%=Action.chooseRunsToAnalyze%>"/>
    <% for (int runid : form.getSelectedRunIds()) { %>
    <input type="hidden" name="<%=DataRegion.SELECT_CHECKBOX_NAME%>" value="<%=runid%>">
    <input type="hidden" name="scriptId" value="<%=form.getProtocol().getScriptId()%>">
    <input type="hidden" name="actionSequence" value="<%=form.getProtocolStep().getDefaultActionSequence()%>">
    <input type="hidden" name="ff_compensationMatrixOption" value="<%=h(form.ff_compensationMatrixOption)%>">
    <% } %>
</form>
