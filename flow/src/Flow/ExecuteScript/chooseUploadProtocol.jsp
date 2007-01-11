<%@ page import="Flow.ExecuteScript.ChooseRunsToUploadForm" %>
<%@ page import="org.fhcrc.cpas.flow.util.PFUtil" %>
<%@ page import="Flow.ExecuteScript.AnalysisScriptController" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowScript" %>
<%@ page import="java.util.List" %>
<%@ page import="org.fhcrc.cpas.flow.data.FlowObject" %>
<%@ page extends="org.fhcrc.cpas.jsp.FormPage" %>
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