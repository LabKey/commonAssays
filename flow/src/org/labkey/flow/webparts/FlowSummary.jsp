<%
/*
 * Copyright (c) 2008-2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.security.ACL" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.ObjectType" %>
<%@ page import="org.labkey.flow.webparts.FlowSummaryWebPart" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="org.labkey.api.exp.api.ExpSampleSet" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.api.pipeline.PipelineStatusUrls" %>
<%@ page import="org.labkey.flow.data.*" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.exp.api.ExpMaterial" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.data.*" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.security.permissions.AdminPermission" %>
<%@page extends="org.labkey.api.jsp.JspBase" %>
<%
    FlowSummaryWebPart me = (FlowSummaryWebPart) HttpView.currentModel();
    Container c = me.c;
    User user = getViewContext().getUser();

    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(c);
    boolean _hasPipelineRoot = pipeRoot != null && pipeRoot.getUri(c) != null;
    boolean _canSetPipelineRoot = user.isAdministrator();
    boolean _canInsert = c.hasPermission(user, InsertPermission.class);
    boolean _canUpdate = c.hasPermission(user, UpdatePermission.class);
    boolean _canCreateFolder = c.getParent() != null && !c.getParent().isRoot() &&
            c.getParent().hasPermission(user, AdminPermission.class);

    int _fcsFileCount = FlowManager.get().getObjectCount(c, ObjectType.fcsKeywords);
    int _fcsRunCount = FlowManager.get().getFCSFileOnlyRunsCount(user, c);
    int _fcsRealRunCount = FlowManager.get().getFCSRunCount(c);
    int _fcsAnalysisCount = FlowManager.get().getObjectCount(c, ObjectType.fcsAnalysis);
    int _fcsAnalysisRunCount = FlowManager.get().getRunCount(c, ObjectType.fcsAnalysis);
    int _compensationMatrixCount = FlowManager.get().getObjectCount(c, ObjectType.compensationMatrix);
//    int _compensationRunCount = FlowManager.get().getRunCount(c, ObjectType.compensationControl);
    //int _flaggedCount = FlowManager.get().getFlaggedCount(c);

    FlowProtocol _protocol = FlowProtocol.getForContainer(c);
    ExpSampleSet _sampleSet = _protocol != null ? _protocol.getSampleSet() : null;

    FlowScript[] _scripts = FlowScript.getAnalysisScripts(c);
    Arrays.sort(_scripts);

    FlowExperiment[] _experiments = FlowExperiment.getAnalysesAndWorkspace(c);
    Arrays.sort(_experiments);

    ActionURL fcsFileRunsURL = new ActionURL(RunController.ShowRunsAction.class, c)
            .addParameter("query.FCSFileCount~neq", 0)
            .addParameter("query.ProtocolStep~eq", "Keywords");
    ActionURL fcsAnalysisRunsURL = new ActionURL(RunController.ShowRunsAction.class, c)
            .addParameter("query.FCSAnalysisCount~neq", 0);
    ActionURL compMatricesURL = FlowTableType.CompensationMatrices.urlFor(user, c, QueryAction.executeQuery);

%>
<style type="text/css">
    .summary-div {
        padding-bottom:1.2em;
        line-height:140%;
    }
    .summary-header {
        border-bottom:1px solid lightgray;
        font-weight:bold;
        padding-top:0.2em;
        margin-top:0;
        margin-bottom:0.3em;
    }

    h3.summary-header {
        font-size: 1.15em;
    }
</style>

<script type="text/javascript">
    function createRenderer(urlFlagged, countProperty, countLabel)
    {
        return function (el, json) {
            var html = "<table border='0'>";
            for (var i = 0; i < json.rows.length; i++) {
                var row = json.rows[i];
                var name = row.Name.value;
                var url = row.Name.url;
                var comment = row["Flag/Comment"].value ? " title='" + row["Flag/Comment"].value + "'" : "";
                var src = row["Flag/Comment"].value ? urlFlagged : "<%=h(AppProps.getInstance().getContextPath())%>/_.gif";

                html += "<tr>" +
                        "<td><a" + comment + " href='" + url + "'><img src='" + src + "'></a></td>" +
                        "<td nowrap><a" + comment + " href='" + url + "'>" + name + "</a><td>";
                if (countProperty) {
                    html += "<td align='right' nowrap>(" + row[countProperty].value + " " + countLabel + ")</td>";
                }
                html += "</tr>";
            }
            html += "</table>";
            el.update(html);
        };
    }
</script>

<% if (_fcsRunCount > 0 || _fcsAnalysisRunCount > 0 || _compensationMatrixCount > 0 || _sampleSet != null) { %>
    <div class="summary-div">
        <h3 class="summary-header">Summary</h3>

    <% if (_fcsRunCount > 0) { %>
        <script type="text/javascript">
        Ext.onReady(function () {
            var tip = new LABKEY.ext.CalloutTip({
                target: "fcsFileRuns-div",
                autoLoad: {
                  url: LABKEY.ActionURL.buildURL("query", "getQuery", null, {
                      schemaName: "flow",
                      "query.queryName": "Runs",
                      "query.FCSFileCount~neq": 0,
                      "query.ProtocolStep~eq": "Keywords",
                      "query.columns": encodeURI("Name,Flag/Comment,FCSFileCount"),
                      "query.sort": "Name",
                      apiVersion: 9.1
                  })
                },
                renderer: createRenderer("<%=h(FlowDataType.FCSFile.urlFlag(true))%>", "FCSFileCount", "files")
            });
        });
        </script>
        <div id="fcsFileRuns-div">
            <a href="<%=fcsFileRunsURL%>">FCS Files (<%=_fcsRunCount%> <%=_fcsRunCount == 1 ? "run" : "runs"%>)</a>
        </div>
    <% } %><%-- end if (_fcsRunCount > 0) --%>

    <% if (_fcsAnalysisRunCount > 0) { %>
        <script type="text/javascript">
        Ext.onReady(function () {
            var tip = new LABKEY.ext.CalloutTip({
                target: "fcsAnalysisRuns-div",
                autoLoad: {
                  url: LABKEY.ActionURL.buildURL("query", "getQuery", null, {
                      schemaName: "flow",
                      "query.queryName": "Runs",
                      "query.FCSAnalysisCount~neq": 0,
                      "query.columns": encodeURI("Name,Flag/Comment,FCSAnalysisCount"),
                      "query.sort": "Name",
                      apiVersion: 9.1
                  })
                },
                renderer: createRenderer("<%=h(FlowDataType.FCSAnalysis.urlFlag(true))%>", "FCSAnalysisCount", "wells")
            });
        });
        </script>
        <div id="fcsAnalysisRuns-div">
            <a href="<%=fcsAnalysisRunsURL%>">FCS Analyses (<%=_fcsAnalysisRunCount%> <%=_fcsAnalysisRunCount == 1 ? "run" : "runs"%>)</a>
        </div>
    <% } %><%-- end if (_fcsAnalysisRunCount > 0) --%>

    <% if (_compensationMatrixCount > 0) { %>
        <script type="text/javascript">
        Ext.onReady(function () {
            var tip = new LABKEY.ext.CalloutTip({
                target: "compensationMatrices-div",
                autoLoad: {
                  url: LABKEY.ActionURL.buildURL("query", "getQuery", null, {
                      schemaName: "flow",
                      "query.queryName": "CompensationMatrices",
                      "query.columns": encodeURI("Name,Flag/Comment"),
                      "query.sort": "Name",
                      apiVersion: 9.1
                  })
                },
                renderer: createRenderer("<%=h(FlowDataType.CompensationMatrix.urlFlag(true))%>")
            });
        });
        </script>
        <div id="compensationMatrices-div">
            <a href="<%=compMatricesURL%>">Compensation (<%=_compensationMatrixCount%> <%=_compensationMatrixCount == 1 ? "matrix" : "matrices"%>)</a>
        </div>
    <% } %><%-- end if (_compensationMatrixCount > 0) --%>

    <% if (_sampleSet != null) { %>
        <script type="text/javascript">
        Ext.onReady(function () {
            var tip = new LABKEY.ext.CalloutTip({
                target: "samples-div",
                html: "<table border='0'>" +
                      <%
                        for (ExpMaterial sample : _sampleSet.getSamples())
                        {
                            String name = sample.getName();
                            String url = sample.detailsURL().getLocalURIString();
                            String comment = sample.getComment() != null ? " title='" + h(sample.getComment()) + "'" : "";
                            String src = StringUtils.isNotEmpty(sample.getComment()) ? sample.urlFlag(true) : AppProps.getInstance().getContextPath() + "/_.gif";
                      %>
                          "<tr>" +
                          "<td><a<%=comment%> href='<%=url%>'><img src='<%=src%>'></a>" +
                          "<td nowrap><a<%=comment%> href='<%=url%>'><%=name%></a>" +
                          "</tr>" +
                      <%
                         }
                      %>
                      "</table>"
            });
        });
        </script>
        <div id="samples-div">
            <a title="<%=_sampleSet.getDescription()%>" href="<%=_sampleSet.detailsURL()%>">Samples (<%=_sampleSet.getSamples().length%>)</a>
        </div>
    <% } %>

    <%--
    <% if (_flaggedCount > 0) { %>
        <div id="flagged-div">
            <a href="#">Flagged (<%=_flaggedCount%>)</a>
        </div>
    <% } %>
    --%>

    <% if (_scripts.length > 0) { %>
        <br/>
        <div>Analysis Scripts</div>
        <div class="labkey-indented">
        <%
        for (FlowScript script : _scripts)
        {
            int runCount = script.getRunCount();
            boolean canEditScript = runCount == 0 && _canUpdate;
            %>
            <script type="text/javascript">
            Ext.onReady(function () {
                var tip = new LABKEY.ext.CalloutTip({
                    target: "script-<%=script.getScriptId()%>-div",
                    autoLoad: {
                      url: LABKEY.ActionURL.buildURL("query", "getQuery", null, {
                          schemaName: "flow",
                          "query.queryName": "Runs",
                          "query.AnalysisScript/RowId~eq": <%=script.getScriptId()%>,
                          "query.columns": encodeURI("Name,Flag/Comment,WellCount"),
                          "query.sort": "Name",
                          apiVersion: 9.1
                      })
                    },
                    tpl: new Ext.XTemplate(
                        '<div style="white-space:nowrap">',
                        '<div class="summary-div">',
                            '<div class="summary-header">Execute Script</div>',
                            <% if (!script.hasStep(FlowProtocolStep.calculateCompensation) && !script.hasStep(FlowProtocolStep.analysis)) { %>
                                '<div>This blank script must be<br>',
                                '<a href="<%=script.urlShow()%>">edited</a> before it can be used.</div>',
                            <% } %>
                            <% if (script.hasStep(FlowProtocolStep.calculateCompensation)) { %>
                                '<div><a href="<%=script.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze, FlowProtocolStep.calculateCompensation)%>">Compensation</a></div>',
                            <% } %>
                            <% if (script.hasStep(FlowProtocolStep.analysis)) { %>
                                '<div><a href="<%=script.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze, FlowProtocolStep.analysis)%>">Statistics and Graphs</a></div>',
                            <% } %>
                        '</div>',
                        <% if (canEditScript || script.hasStep(FlowProtocolStep.calculateCompensation) || script.hasStep(FlowProtocolStep.analysis)) { %>
                            '<div class="summary-div">',
                                '<div class="summary-header">Analysis Definition</div>',
                                <% if (script.hasStep(FlowProtocolStep.calculateCompensation)) { %>
                                    '<div><a href="<%=script.urlFor(ScriptController.Action.editCompensationCalculation)%>">Show Compensation</a></div>',
                                <% } else if (canEditScript) { %>
                                    '<div><a href="<%=script.urlFor(ScriptController.Action.uploadCompensationCalculation)%>">Upload FlowJo Compensation</a></div>',
                                <% } %>
                                <% if (script.hasStep(FlowProtocolStep.analysis)) { %>
                                    '<div><a href="<%=script.urlFor(ScriptController.Action.gateEditor, FlowProtocolStep.analysis)%>"><%=canEditScript ? "Edit" : "View"%> Gate Definitions</a></div>',
                                    '<div><a href="<%=script.urlFor(ScriptController.Action.editAnalysis)%>"><%=canEditScript ? "Edit" : "View"%> Statistics and Graphs</a></div>',
                                    <% if (canEditScript) { %>
                                        '<div><a href="<%=script.urlFor(ScriptController.Action.editGateTree, FlowProtocolStep.analysis)%>">Rename Populations</a></div>',
                                    <% } %>
                                <% } else if (canEditScript) { %>
                                    '<div><a href="<%=script.urlFor(ScriptController.Action.uploadAnalysis)%>">Upload FlowJo Analysis</a></div>',
                                <% } %>
                            '</div>',
                        <% } %>
                        '<div class="summary-div">',
                            '<div class="summary-header">Manage</div>',
                            '<div><a href="<%=script.urlFor(ScriptController.Action.editSettings)%>">Settings</a></div>',
                            '<div><a href="<%=script.urlFor(ScriptController.Action.copy)%>">Copy</a></div>',
                            <% if (runCount == 0) { %>
                                '<div><a href="<%=script.urlFor(ScriptController.Action.delete)%>">Delete</a></div>',
                            <% } %>
                        '</div>',
                        <% if (runCount > 0) { %>
                            '<div class="summary-div">',
                                '<div class="summary-header">Runs</div>',
                                '<table boder=0>',
                                '<tpl for="rows">',
                                '  <tr>',
                                '    <td nowrap><a href="{[values.Name.url]}">{[values.Name.value]}</a>',
                                '    <td align="right" nowrap>({[values.WellCount.value]} wells)',
                                '  </tr>',
                                '</tpl>',
                                '</table>',
                            '</div>',
                        <% } %>
                        '</div>')
                });
            });
            </script>
            <div id="script-<%=script.getScriptId()%>-div" style="white-space:nowrap;">
                <a href="<%=script.urlShow()%>"><%=script.getName()%> (<%=runCount%> <%=runCount == 1 ? "run" : "runs"%>)</a>
            </div>
            <%
        }
        %></div><%-- end labkey-indented --%>
    <% } %><%-- end if (_scripts.length > 0) --%>

    <% if (_fcsAnalysisRunCount > 0) { %>
        <br/>
        <div>Analysis Folders</div>
        <div class="labkey-indented">
        <%
        for (FlowExperiment experiment : _experiments)
        {
            int runCount = experiment.getRunCount(null);
            if (runCount > 0)
            {
                %>
                <script type="text/javascript">
                Ext.onReady(function () {
                    var tip = new LABKEY.ext.CalloutTip({
                        target: "script-<%=experiment.getExperimentId()%>-div",
                        autoLoad: {
                          url: LABKEY.ActionURL.buildURL("query", "getQuery", null, {
                              schemaName: "flow",
                              "query.queryName": "Runs",
                              "experimentId": <%=experiment.getExperimentId()%>,
                              "query.columns": encodeURI("Name,Flag/Comment,WellCount"),
                              "query.sort": "Name,ProtocolStep",
                              apiVersion: 9.1
                          })
                        },
                        renderer: createRenderer("<%=h(FlowDataType.FCSAnalysis.urlFlag(true))%>", "WellCount", "wells")
                    });
                });
                </script>
                <%
            }
            %>
            <div id="script-<%=experiment.getExperimentId()%>-div">
                <a href="<%=experiment.urlShow()%>"><%=experiment.getName()%> (<%=runCount%> <%=runCount == 1 ? "run" : "runs"%>)</a>
            </div>
            <%
        }
        %></div><%-- end labkey-indented --%>
    <% } %><%-- end if (_fcsAnalysisRunCount > 0) --%>

</div><%-- end summary-div --%>
<% } %>


<% if (_canUpdate || _canSetPipelineRoot) { %>
<div class="summary-div">
    <h3 class="summary-header">Actions</h3>
    <%
        if (_canSetPipelineRoot)
        {
            ActionURL urlPipelineRoot = PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(c);
            if (!_hasPipelineRoot)
            {
                %><div><%=PageFlowUtil.textLink("Setup Pipeline", urlPipelineRoot)%></div><%
            }
            else if (_fcsFileCount == 0)
            {
                %><div><%=PageFlowUtil.textLink("Change Pipeline", urlPipelineRoot)%></div><%
            }
        }

        if (_canUpdate)
        {
            if (!_hasPipelineRoot)
            {
                %><div><%=PageFlowUtil.textLink("Import Workspace", new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, c))%></div><%
            }
            else
            {
                %><div><%=PageFlowUtil.textLink("Upload and Import", PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(c, getViewContext().getActionURL().getLocalURIString()))%></div><%
            }

            if (_fcsRealRunCount > 0)
            {
                %><div><%=PageFlowUtil.textLink("Create Analysis Script", new ActionURL(ScriptController.NewProtocolAction.class, c))%></div><%
            }
        }
    %>
</div>
<% } %>

<% if (_protocol != null && _canUpdate) { %>
    <div class="summary-div">
        <h3 class="summary-header">Manage</h3>
        <div><%= PageFlowUtil.textLink("Settings", _protocol.urlShow())%></div>

        <div><%=PageFlowUtil.textLink("Upload Samples", _protocol.urlUploadSamples(_sampleSet != null))%></div><%
        if (_sampleSet != null)
        {
            %><div><%=PageFlowUtil.textLink("Sample Join Fields", _protocol.urlFor(ProtocolController.Action.joinSampleSet))%></div><%
        }
        if (_fcsAnalysisCount > 0)
        {
            %><div><%=PageFlowUtil.textLink("Identify Background", new ActionURL(ProtocolController.EditICSMetadataAction.class, c))%></div><%
        }

        int jobCount = PipelineService.get().getQueuedStatusFiles(c).length;
        %><div><%=PageFlowUtil.textLink("Show Jobs" + (jobCount > 0 ? " (" + jobCount + " running)" : ""), PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlBegin(c))%></div><%

        if (_canCreateFolder && _hasPipelineRoot)
        {
            %><div><%=PageFlowUtil.textLink("Copy Folder", new ActionURL(FlowController.NewFolderAction.class, c))%></div><%
        }
        %>
    </div>
<% } %>


