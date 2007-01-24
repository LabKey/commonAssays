<%@ page import="org.labkey.flow.controllers.compensation.CompensationController" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.query.FlowSchema" %>
<%@ page import="org.labkey.flow.script.FlowPipelineProvider" %>
<%@ page import="org.labkey.flow.util.PFUtil"%>
<%@ page import="org.labkey.api.pipeline.PipelineService"%>
<%@ page import="org.labkey.api.security.ACL"%>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.ObjectType" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.flow.controllers.BaseFlowController.FlowPage" %>
<style>
    .tablemainheader {
        background-color: gray;
    }
</style>


<%!
    private String strRunCount(FlowExperiment runset, FlowProtocolStep step)
    {
        int count = runset.getRunCount(step);
        if (count == 0)
            return "0";
        return Integer.toString(count);
    }

    private String strUser(User user)
    {
        if (user == null)
            return "Unknown";
        return user.toString();
    }
%>

<% PipelineService service = PipelineService.get();
    boolean hasPipelineRoot = service.getPipelineRoot(getContainer()) != null;
    boolean isAdmin = getContainer().hasPermission(getUser(), ACL.PERM_ADMIN);
    boolean canInsert = getContainer().hasPermission(getUser(), ACL.PERM_INSERT);
    boolean canUpdate = getContainer().hasPermission(getUser(), ACL.PERM_UPDATE);
%>
<p>Welcome to <%=FlowModule.getLongProductName()%>.</p>
<% if (canInsert)
{ %>
<% if (hasPipelineRoot)
{ %>
<p>
    <cpas:link
            href="<%=PipelineService.get().getViewUrlHelper(getPageFlow().getViewURLHelper(), FlowPipelineProvider.NAME, "upload", null)%>"
            text="Upload some flow runs"/>
</p>
<% }
else
{ %>
<p>
    There is no pipeline root specified.<br>
    <% if (isAdmin)
    {
    %>
    Click <cpas:link href="<%=new ViewURLHelper("Pipeline", "setup", getContainerPath())%>" text="here"/> to set the
    pipeline root
    for this folder.<br>
    <% }
    else
    { %>
    Contact your administrator to set the pipeline root for this folder.<br>
    <% } %>
</p>
<% } %>
<% } %>
<p>
    <% FlowManager mgr = FlowManager.get();
    int fcsFileCount = mgr.getObjectCount(getContainer(), ObjectType.fcsKeywords);
    int fcsRunCount = mgr.getRunCount(getContainer(), ObjectType.fcsKeywords);
    if (fcsFileCount == 0)
    { %>
There are no flow experiment runs in this folder.
<% }
else
{         ViewURLHelper urlShowRuns = FlowTableType.Runs.urlFor(getContainer(), "ProtocolStep", FlowProtocolStep.keywords.getName());
    %>There are <a
        href="<%=h(FlowTableType.FCSFiles.urlFor(getContainer(), QueryAction.executeQuery))%>"><%=fcsFileCount%> FCS
    files</a> in <a href="<%=h(urlShowRuns)%>"><%=fcsRunCount%> runs</a>.<br>
<% } %>
</p>


<% FlowExperiment[] analyses = FlowExperiment.getAnalyses(getContainer());
    if (analyses.length != 0)
    {%>
<table class="normal" style="border:solid black 1px;">
    <tr><th colspan="5" class="tablemainheader">Analyses in this folder:<th></tr>
    <tr><th rowspan="2">Analysis Name</th><th rowspan="2">Created</th><th rowspan="2">Created By</th><th colspan="2">
        Number of runs that have been</th>
    </tr>
    <tr><th>Compensated</th><th>Analyzed</th></tr>
    <% for (FlowExperiment experiment : analyses)
    { %>
    <tr><td><a href="<%=h(experiment.urlShow())%>"><%=h(experiment.getLabel())%></a></td>
        <td><%=formatDateTime(experiment.getExperiment().getCreated())%></td>
        <td><%=h(strUser(experiment.getExperiment().getCreatedBy()))%></td>
        <td><%=strRunCount(experiment, FlowProtocolStep.calculateCompensation)%></td>
        <td><%=strRunCount(experiment, FlowProtocolStep.analysis)%></td>
    </tr>
    <% } %>
</table>
<% } %>

<% FlowScript[] analysisScripts = FlowScript.getAnalysisScripts(getContainer());
    if (analysisScripts.length > 0)
    {
%>
<table class="normal" style="border:solid black 1px;">
    <tr><th colspan="5" class="tablemainheader">Analysis scripts:</th></tr>
    <tr><th>Name</th><th>Created</th><th>Description</th><th>Type</th></tr>
    <% for (FlowScript analysisScript : analysisScripts)
    { %>
    <tr><td><a href="<%=analysisScript.urlShow()%>"><%=h(analysisScript.getLabel())%></a></td>
        <td><%=formatDateTime(analysisScript.getExpObject().getCreated())%></td>
        <td><%=h(analysisScript.getExpObject().getComment())%></td>
        <td><%=h(analysisScript.getProtocolType())%></td>
    </tr>
    <% } %>
</table>
<% } %>
<% if (canUpdate)
{ %>
<cpas:link text="Create a new analysis script"
             href="<%=PFUtil.urlFor(ScriptController.Action.newProtocol, getContainerPath())%>"/>
<br>
<% } %>

<p class="normal">
    <cpas:link href="<%=new FlowSchema(null, getContainer()).urlSchemaDesigner()%>" text="Queries"/><br>
    <% if (canUpdate) { %>
    <cpas:link href="<%=PFUtil.urlFor(FlowController.Action.showJobs, getContainer())%>" text="Pipeline Jobs"/><br>
    <% } %>
</p>

<% FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
    if (protocol != null)
    {
%>
<p><cpas:link href="<%=protocol.urlShow()%>" text="Protocol Settings (and Samples)"/></p>
<% } %>
<p><cpas:link href="<%=PFUtil.urlFor(CompensationController.Action.begin, getContainer())%>" text="Compensation Matrices" /></p>

<% if (canUpdate)
{ %>
<p><cpas:link text="Create a new folder"
                href="<%=PFUtil.urlFor(FlowController.Action.newFolder, getContainer())%>"/></p>
<% } %>
