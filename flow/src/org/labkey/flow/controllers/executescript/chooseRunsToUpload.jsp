<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToUploadForm" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%
    ChooseRunsToUploadForm form = (ChooseRunsToUploadForm) __form;
%>
<labkey:errors/>
<form method="POST" action="<%=PFUtil.urlFor(AnalysisScriptController.Action.chooseRunsToUpload, getContainer())%>"
      class="normal">
    <input type="hidden" name="path" value="<%=h(form.path)%>">
    <%
        Map<String, String> paths = form.getNewPaths();
        if (paths.size() == 0)
        {%>
    <labkey:button text="Browse for more runs" href="<%=form.srcURL%>"/>
    <% }
    else
    {%>
    <p>
        Choose which directories in '<%=h(form.path)%>' contain the FCS files for your experiment runs.
        <%=FlowModule.getLongProductName()%> will read the keywords from these FCS files into the database.  The FCS files
        themselves will not be modified, and will remain in the file system. 
    </p>
    <table>
        <% for (Map.Entry<String, String> entry : paths.entrySet())
        { %>
        <tr><td><input type="checkbox" name="ff_path" value="<%=h(entry.getKey())%>"></td>
            <td><%=h(entry.getValue())%></td></tr>
        <%} %>
    </table>
            <labkey:selectAll/>
            <labkey:clearAll/>
            <labkey:button text="Upload Selected Runs" action="<%=AnalysisScriptController.Action.uploadRuns%>"/>
    <% } %>
</form>
