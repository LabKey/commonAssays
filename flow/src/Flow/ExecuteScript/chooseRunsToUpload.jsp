<%@ page import="Flow.ExecuteScript.ChooseRunsToUploadForm" %>
<%@ taglib uri="http://cpas.fhcrc.org/taglib/cpas" prefix="cpas" %>
<%@ page import="org.fhcrc.cpas.flow.util.PFUtil" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.util.List" %>
<%@ page import="Flow.ExecuteScript.AnalysisScriptController" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.fhcrc.cpas.flow.script.FlowPipelineProvider" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%
    ChooseRunsToUploadForm form = (ChooseRunsToUploadForm) __form;
%>
<cpas:errors/>

<form method="POST" action="<%=PFUtil.urlFor(AnalysisScriptController.Action.chooseRunsToUpload, getContainer())%>"
      class="normal">
    <input type="hidden" name="path" value="<%=h(form.path)%>">
    <%
        List<URI> paths = form.getNewPaths();
        if (paths.size() == 0)
        {
            ViewURLHelper urlBrowse =
                    PipelineService.get().getViewUrlHelper(form.getContext().getViewURLHelper(), FlowPipelineProvider.NAME, "upload", form.path); %>
    <cpas:button text="Browse for more runs" href="<%=urlBrowse%>"/>
    <% }
    else
    {%>
    <table>
        <% for (URI path : paths)
        { %>
        <tr><td><input type="checkbox" name="ff_path" value="<%=h(path.toString())%>"></td>
            <td><%=h(path.toString())%></td></tr>
        <%} %>
        <tr><td colspan="2">
            <cpas:selectAll/>
            <cpas:clearAll/>
            <cpas:button text="Upload Selected Runs" action="<%=AnalysisScriptController.Action.uploadRuns%>"/>
        </td></tr> `
    </table>
    <% } %>
</form>
