<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToUploadForm" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.data.FlowObject" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib uri="http://cpas.fhcrc.org/taglib/cpas" prefix="cpas" %>
<% ChooseRunsToUploadForm form = (ChooseRunsToUploadForm) __form; %>
<form method="POST" action="<%=PFUtil.urlFor(AnalysisScriptController.Action.uploadRuns, getContainer())%>">
    <% for (String ff_path : form.ff_path) { %>
    <input type="hidden" name="ff_path" value="<%=h(ff_path)%>">
    <%}%>

    <% List<FlowScript> protocols = form.getProtocols();
        if (protocols.size() > 1)
        {
    %>
    <p>Which upload protocol do you want to use?<br>
        <select name="ff_protocolId">
            <cpas:options value="<%=form.ff_protocolId%>" map="<%=FlowObject.idLabelsFor(protocols)%>"/>
        </select>
    </p>
    <% } %>
    <cpas:button text="Upload Runs" />
</form>