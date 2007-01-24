<%@ page import="org.labkey.api.exp.api.ExpRunTable" %>
<%@ page import="org.labkey.api.exp.api.ExpSampleSet" %>
<%@ page import="org.labkey.api.pipeline.PipelineQueue" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.flow.controllers.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.ObjectType" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="org.labkey.flow.script.FlowPipelineProvider" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.api.jsp.ContextPage" %>
<% FlowManager mgr = FlowManager.get();
    PipelineService service = PipelineService.get();
    boolean hasPipelineRoot = service.getPipelineRoot(getContainer()) != null;
    boolean isAdmin = getContainer().hasPermission(getUser(), ACL.PERM_ADMIN);
    boolean canInsert = getContainer().hasPermission(getUser(), ACL.PERM_INSERT);
    boolean canUpdate = getContainer().hasPermission(getUser(), ACL.PERM_UPDATE);
    int fcsFileCount = mgr.getObjectCount(getContainer(), ObjectType.fcsKeywords);
    int fcsRunCount = mgr.getRunCount(getContainer(), ObjectType.fcsKeywords);
    int compensationRunCount = mgr.getRunCount(getContainer(), ObjectType.compensationControl);
    int fcsAnalysisCount = mgr.getObjectCount(getContainer(), ObjectType.fcsAnalysis);
    int analysisRunCount = mgr.getRunCount(getContainer(), ObjectType.fcsAnalysis);
    FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
    List<FlowCompensationMatrix> compensationMatrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
    int compensationMatrixCount = compensationMatrices.size();

    String strParamProtocolStep = QueryView.DATAREGIONNAME_DEFAULT + "." + ExpRunTable.Column.ProtocolStep + "~eq";

    FlowScript[] scripts = FlowScript.getAnalysisScripts(getContainer());
    FlowScript scriptCompensation = null;
    FlowScript scriptAnalysis = null;
    boolean requiresCompensation = false;
    for (FlowScript script : scripts) {
        if (script.requiresCompensationMatrix(FlowProtocolStep.analysis)) {
            requiresCompensation = true;
        }
        if (scriptAnalysis == null && script.hasStep(FlowProtocolStep.analysis)) {
            scriptAnalysis = script;
        }
        if (scriptCompensation == null && script.hasStep(FlowProtocolStep.calculateCompensation) && !script.hasStep(FlowProtocolStep.calculateCompensation)) {
            scriptCompensation = script;
        }
    }
    String strPipelineRootDescription = "<i>The pipeline root tells " + FlowModule.getLongProductName() +
            " where in the file system on the server FCS files are permitted to be loaded from.</i>";
%>
<p><i>In this folder</i></p>
<ul>
<% if (canInsert && !hasPipelineRoot)
{ %>
<li>
    <b>Pipeline</b><br>
    <span class="labkey-error">There is no pipeline root specified.</span><br>
    <% if (isAdmin)
    {
    %><%=strPipelineRootDescription%>You must set the pipeline root for this folder before any FCS files can be
    loaded.<br>
    <cpas:link href="<%=new ViewURLHelper("Pipeline", "setup", getContainer())%>" text="Set Pipeline Root"/>
    <br>
    <% }
    else
    { %>Contact your administrator to set the pipeline root for this folder.<br>
    <% } %>
</li>
<% }
else if (isAdmin && fcsFileCount == 0)
{ %>
<li>
    <%=strPipelineRootDescription%><br>
    <cpas:link href="<%=new ViewURLHelper("Pipeline", "setup", getContainer())%>" text="Change Pipeline Root"/>
    <br>
</li>
<% } %>
<li>
    <b>FCS files</b>
    <% if (fcsFileCount != 0)
    {
        ViewURLHelper urlShowRuns = FlowTableType.Runs.urlFor(getContainer(), "ProtocolStep", FlowProtocolStep.keywords.getName());
    %>There are <a
        href="<%=h(FlowTableType.FCSFiles.urlFor(getContainer(), QueryAction.executeQuery))%>"><%=fcsFileCount%> FCS
    files</a> in <a href="<%=h(urlShowRuns)%>"><%=fcsRunCount%> runs</a>.<br>
    <%
    }
    else
    {
    %>There are no FCS files.<br>
    <% }
    %><% if (canInsert && hasPipelineRoot)
{
    String strLinkText = fcsFileCount == 0 ? "Browse for FCS files to be loaded" : "Browse for more FCS files to be loaded";
%>
    <cpas:link
            href="<%=PipelineService.get().getViewUrlHelper(getViewURLHelper(), FlowPipelineProvider.NAME, "upload", null)%>"
            text="<%=strLinkText%>"/>
    <% } %>
</li>
<% if (fcsFileCount != 0 || scripts.length != 0)
{ %>
<li>
    <b>Analysis Scripts</b><br>
    <% if (scripts.length == 0)
    { %>There are no analysis scripts.<br>
    <% }
    else if (scripts.length == 1)
    {
        FlowScript script = scripts[0];
    %>There is an analysis script named <a href="<%=h(script.urlShow())%>">'<%=h(script.getName())%>'</a><br>
    <% }
    else
    { %>There are <a
        href="<%=h(FlowTableType.AnalysisScripts.urlFor(getContainer(), QueryAction.executeQuery))%>"><%=scripts.length%>
    analysis scripts</a><br>
    <% } %><% if (canUpdate)
{ %>
    <i>An analysis script tells <%=FlowModule.getLongProductName()%> how to calculate the compensation matrix, which
        gates to apply, statistics to calculate, and graphs to draw.</i><br>
    <cpas:link text="Create a new analysis script"
               href="<%=PFUtil.urlFor(ScriptController.Action.newProtocol, getContainer())%>"/>
    <br>
    <% } %>
</li>
<% } %>

<% if (requiresCompensation || compensationMatrixCount != 0)
{ %>
<li>
    <b>Compensation Matrices</b><br>
    <% if (compensationMatrixCount == 0)
    { %>There are no compensation matrices.<br>
    <% }
    else if (compensationMatrixCount == 1)
    {
        FlowCompensationMatrix comp = compensationMatrices.get(0);
    %>There is <a href="<%=h(comp.urlShow())%>">one compensation matrix</a>.<br>
    <% }
    else
    { %>There are <a
        href="<%=h(FlowTableType.CompensationMatrices.urlFor(getContainer(), QueryAction.executeQuery))%>"><%=compensationMatrixCount%>
    compensation matrices.</a><br>
    <% if (compensationRunCount != 0)
    {
        ViewURLHelper urlCompensationRuns = FlowTableType.Runs.urlFor(getContainer(), QueryAction.executeQuery);
        urlCompensationRuns.addParameter(strParamProtocolStep, FlowProtocolStep.calculateCompensation.getName());
    %>These have been calculated in <a href="<%=h(urlCompensationRuns)%>"><%=compensationRunCount%>
    runs</a>.<br><% } %><% } %>
    <cpas:link text="Upload a compensation matrix"
               href="<%=PFUtil.urlFor(CompensationController.Action.begin, getContainer())%>"/>
    <br>
    <% if (scriptCompensation != null)
    { %>
    <cpas:link text="Calculate compensation matrices"
               href="<%=scriptCompensation.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze)%>"/>
    <% } %>
</li>
<% } %><% if (scriptAnalysis != null || fcsAnalysisCount != 0)
{ %>
<li>
    <b>Analyses</b><br>
    <% if (fcsAnalysisCount == 0)
    { %>No FCS files have been analyzed.<br>
    <% }
    else
    {
        ViewURLHelper urlAnalysisRuns = FlowTableType.Runs.urlFor(getContainer(), QueryAction.executeQuery);
        urlAnalysisRuns.addParameter(strParamProtocolStep, FlowProtocolStep.analysis.getName());
    %>
    <a href="<%=h(FlowTableType.FCSAnalyses.urlFor(getContainer(), QueryAction.executeQuery))%>"><%=fcsAnalysisCount%>
        FCS files have been analyzed</a> in <a href="<%=h(urlAnalysisRuns)%>"><%=analysisRunCount%> runs</a>.<br>
    <% } %><% if (scriptAnalysis != null)
{ %>
    <cpas:link text="Analyze some flow runs"
               href="<%=scriptAnalysis.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze)%>"/>
    <% } %>
</li>
<% } %>
<% if (protocol != null && fcsFileCount > 0)
{ %>
<li>
    <b>Samples</b><br>
    <%
        ExpSampleSet ss = protocol.getSampleSet();

        if (ss != null)
        { %>There are <a href="<%=ss.detailsURL()%>"><%=ss.getSamples().length%> samples</a>.<br>
    <% if (canUpdate)
    { %><% if (protocol.getSampleSetJoinFields().size() != 0)
{ %>
    <i>The samples are linked to the FCS files using some keywords. When new samples are added, or FCS files are
        uploaded, new links will in general be created.</i><br>
    <cpas:link text="Modify sample set join fields"
               href="<%=protocol.urlFor(ProtocolController.Action.joinSampleSet)%>"/>
    <br>
    <% }
    else
    { %>You can specify how these samples should be linked to the FCS files.
    <cpas:link text="Define sample set join fields"
               href="<%=protocol.urlFor(ProtocolController.Action.joinSampleSet)%>"/>
    <br>
    <% } %><% } %><% }
else if (canUpdate)
{ %>
    <i>Additional information about groups of FCS files can be uploaded in a spreadsheet, and associated with the FCS
        files using keywords.</i><br>
    <cpas:link text="Upload Sample Set" href="<%=protocol.urlUploadSamples()%>"/>
    <br>
    <% } %>
    <cpas:link text="Miscellaneous settings" href="<%=protocol.urlShow()%>"/>
    <br>
</li>
<% } %>
</ul>

<% if (canUpdate) {
    PipelineQueue.JobData jobData = PipelineService.get().getPipelineQueue().getJobData(getContainer());
    int jobCount = jobData.getPendingJobs().size() + jobData.getRunningJobs().size();
    if (jobCount != 0)
    {
%>
    <p>
        <b>Pipeline Jobs</b><br>
        <a href="<%=PFUtil.urlFor(FlowController.Action.showJobs, getContainer())%>"><%=jobCount%>
    jobs</a> are not completed.</p>
<% } %>
<% } %>
<% if (hasPipelineRoot && canUpdate && protocol != null) { %>
<p>
    <i>If you want to analyze a new set of experiment runs with a slightly different protocol, you should create a new
        folder to do this work in. You can copy some of the settings from this folder.</i><br>
    <cpas:link href="<%=PFUtil.urlFor(FlowController.Action.newFolder, getContainer())%>" text="Create new folder"/>
</p>
<% } %>

