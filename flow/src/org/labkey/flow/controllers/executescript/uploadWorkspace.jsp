<%@ page import="org.labkey.flow.controllers.executescript.UploadWorkspaceResultsForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.FormPage"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% UploadWorkspaceResultsForm form = (UploadWorkspaceResultsForm) __form;
    Map<Integer, String> analyses = new LinkedHashMap();
    for (FlowExperiment analysis : FlowExperiment.getAnalyses(getContainer()))
    {
        analyses.put(analysis.getExperimentId(), analysis.getName());
    }

%>
<labkey:errors />
<form action="<%=h(getContainer().urlFor(AnalysisScriptController.Action.uploadWorkspace))%>" method="POST">
    <input type="hidden" name="path" value="<%=h(form.getPath())%>">
    <p>The statistics in this workspace that have been calculated will be uploaded to <%=FlowModule.getLongProductName()%>.
        If the workspace is in the same directory as the FCS files in it, then <%=FlowModule.getLongProductName()%> will
    be able to find them, and you will be able to perform additional analysis on these files.  If the FCS files are somewhere
    else, <%=FlowModule.getLongProductName()%> will not be able to find them, but you will still be able to browse and
    query the results that have been uploaded.</p>
    <% if (analyses.size() == 0) { %>
    <p><%=FlowModule.getLongProductName()%> organizes results into different "analyses".  The same FCS file should only
    be analyzed once in a given analysis.  If you want to analyze the same FCS file in two different ways, those results
    should be put into different analyses.<br>

    What do you want to call the new Analysis?  You will be able to use this name for multiple uploaded workspaces.<br>
    <input type="text" name="ff_newAnalysisName" value="<%=h(form.ff_newAnalysisName)%>">
    </p>
    <% } else { %>
    <p>Which analysis do you want to put the results into?<br>
        <select name="ff_existingAnalysisId">
            <labkey:options value="<%=form.ff_existingAnalysisId%>" map="<%=analyses%>" />
        </select><br>
        or create a new analysis named:<br>
        <input type="text" name="ff_newAnalysisName" value="<%=h(form.ff_newAnalysisName)%>">
    </p>
    <% } %>
    <labkey:button text="Upload Results" />
</form>