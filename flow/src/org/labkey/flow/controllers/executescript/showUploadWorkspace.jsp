<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors />
<p>You can upload a FlowJo workspace that contains statistics that FlowJo has calculated.  To do this, use FlowJo to save
the workspace as XML.</p>
<form action="<%=urlFor(AnalysisScriptController.Action.showUploadWorkspace)%>" method="POST" enctype="multipart/form-data">
    <p>Which file do you want to upload?<br>
    <input type="file" name="workspace.file">
    </p>
    <labkey:button text="Upload" />
</form>
<% if (PipelineService.get().findPipelineRoot(getContainer()) != null) { %>
<hr>
<p>Alternatively, if your workspace has been saved as XML on the file server, you can browse the pipeline directories and
find your workspace.  If there are FCS files are in the same directory as the workspace, then the FlowJo results will be
linked to FCS files.  You will be able to use <%=FlowModule.getLongProductName()%> to see additional graphs, or
calculate additional statistics.<br>
    <labkey:button href="<%=urlFor(AnalysisScriptController.Action.browseForWorkspace)%>" text="Browse the pipeline" />
</p>
<% } %>