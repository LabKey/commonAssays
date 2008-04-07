<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToUploadForm" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%
    ChooseRunsToUploadForm form = (ChooseRunsToUploadForm) __form;
    Map<String, String> paths = form.getNewPaths();
    PipeRoot root = form.getPipeRoot();

    %><labkey:errors/><%

    if (paths.size() != 0)
    {
        assert root != null;

        %><form method="POST" action="<%=org.labkey.api.util.PageFlowUtil.urlFor(AnalysisScriptController.Action.chooseRunsToUpload, getContainer())%>"
          class="normal">
        <input type="hidden" name="path" value="<%=h(form.path)%>">
        <p>
            Choose which directories in '<%=h(PageFlowUtil.decode(form.path))%>' contain the FCS files for your experiment runs.
            <%=FlowModule.getLongProductName()%> will read the keywords from these FCS files into the database.  The FCS files
            themselves will not be modified, and will remain in the file system.
        </p>
        <table><%

        for (Map.Entry<String, String> entry : paths.entrySet())
        {
            %><tr><td><input type="checkbox" name="ff_path" value="<%=h(entry.getKey())%>"></td>
            <td><%=h(entry.getValue())%></td></tr><%
        }
        %></table>
        <labkey:selectAll/>
        <labkey:clearAll/>
        <labkey:button text="Upload Selected Runs" action="<%=AnalysisScriptController.Action.uploadRuns%>"/>
        </form><%
    }
    else
    {
        %><labkey:button text="Browse for more runs" href="<%=form.srcURL%>"/><% 
    }

%>