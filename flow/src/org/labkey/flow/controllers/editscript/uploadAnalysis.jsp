<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace.StatisticSet" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.UploadAnalysisPage" %>
<%!
    private String statOption(StatisticSet option)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("<input type=\"checkbox\" name=\"ff_statisticSet\" value=\"");
        ret.append(option);
        ret.append("\"");

        if (form.ff_statisticSet.contains(option))
        {
            ret.append(" checked");
        }
        ret.append(">");
        return ret.toString();
    }
%>
<%=pageHeader(ScriptController.Action.uploadAnalysis)%>
<form method="POST" action="<%=formAction(ScriptController.Action.uploadAnalysis)%>" enctype="multipart/form-data">
    <% if (form.workspaceObject != null)
    { %>
    <input type="hidden" name="workspaceObject" value="<%=PageFlowUtil.encodeObject(form.workspaceObject)%>">
    <%
        String[] analysisNames = this.getGroupAnalysisNames();
        if (analysisNames.length > 0)
        {
    %>
    <p>Which group do you want to use?<br>
        <select name="groupName">
            <option value=""></option>
            <% for (String group : analysisNames)
            { %>
            <option value="<%=h(group)%>"><%=h(group)%></option>
            <% } %>
        </select>
    </p>
    <% }
    else
    {
        Map<String, String> sampleAnalyses = this.getSampleAnalysisNames();
        if (sampleAnalyses.size() > 0)
        {
    %>
    <p>Which sample do you want to use?<br>
        <select name="sampleId">
            <option value=""/>
            <% for (Map.Entry<String, String> entry : sampleAnalyses.entrySet())
            { %>
            <option value="<%=h(entry.getKey())%>"><%=h(entry.getValue())%></option>
            <% } %>
        </select>
    </p>
    <% }
    }
    } %>

    <input type="hidden" name="ff_statisticOption" value=""/>
    <%-- Add this hidden field so that something gets posted for "ff_statisticOption", even when
    none of the checkboxes are checked --%>
    <% if (form.workspaceObject == null)
    { %>
    <p>Upload a new Flow Jo workspace XML file<br>
        <input type="file" name="workspaceFile">
    </p>

    <p>Which statistics should be calculated?<br>
        <%=statOption(StatisticSet.count)%> Count<br>
        <%=statOption(StatisticSet.frequency)%> Frequency of Total<br>
        <%=statOption(StatisticSet.frequencyOfParent)%> Frequency of Parent<br>
        <%=statOption(StatisticSet.frequencyOfGrandparent)%> Frequency of Grandparent<br>
        <%=statOption(StatisticSet.medianGated)%> Median value of parameters used in gates<br>
        <%=statOption(StatisticSet.medianAll)%> Median value of all parameters<br>
        <%=statOption(StatisticSet.meanAll)%> Mean value of all parameters<br>
        <%=statOption(StatisticSet.stdDevAll)%> Standard deviation of all parameters<br>
    </p>
    <% }
    else
    {

    %>

    <% } %>

    <input type="Submit" value="Submit"/>
</form>
