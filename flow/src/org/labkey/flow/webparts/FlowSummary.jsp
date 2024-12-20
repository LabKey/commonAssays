<%
/*
 * Copyright (c) 2009-2019 LabKey Corporation
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.exp.api.ExpMaterial" %>
<%@ page import="org.labkey.api.exp.api.ExpSampleType" %>
<%@ page import="org.labkey.api.exp.flag.FlagColumnRenderer" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipelineStatusUrls" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.security.permissions.AdminOperationsPermission" %>
<%@ page import="org.labkey.api.security.permissions.AdminPermission" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.persist.ObjectType" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>
<%
    Container c = getContainer();
    User user = getUser();

    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(c);
    boolean _hasPipelineRoot = pipeRoot != null;
    boolean _canSetPipelineRoot = c.hasPermission(user, AdminOperationsPermission.class);
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
    ExpSampleType _sampleType = _protocol != null ? _protocol.getSampleType(getUser()) : null;
    List<? extends ExpMaterial> _sampleTypeSamples = _sampleType == null ? null : _protocol.getSamples(_sampleType, user);
    ActionURL _sampleTypeDetailsUrl = _sampleType != null ? _protocol.getSampleTypeDetailsURL(_sampleType, getContainer()) : null;

    FlowScript[] _scripts = FlowScript.getAnalysisScripts(c);
    Arrays.sort(_scripts);

    FlowExperiment[] _experiments = FlowExperiment.getAnalysesAndWorkspace(c);
    Arrays.sort(_experiments);

    ActionURL fcsFileRunsURL = RunController.ShowRunsAction.getFcsFileRunsURL(c);
    ActionURL fcsAnalysisRunsURL = RunController.ShowRunsAction.getFCSAnalysisRunsURL(c);
    ActionURL compMatricesURL = FlowTableType.CompensationMatrices.urlFor(user, c, QueryAction.executeQuery);

    final int DISPLAY_MAX_ROWS = 15;
%>
<style type="text/css">
    .summary-div {
        padding-bottom:1.2em;
        line-height:140%;
        /* Keep the label and tooltip downarrow from being wrapped */
        white-space: nowrap;
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

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    function createRenderer(detailsURL, countProperty, countLabel)
    {
        const h = LABKEY.Utils.encodeHtml;

        return function (el, json) {
            var html = "<table border='0'>";
            for (var i = 0; i < <%=DISPLAY_MAX_ROWS%> && i < json.rows.length; i++) {
                var row = json.rows[i];
                var name = row.Name.value;
                var url = row.Name.url;
                var comment = row["Flag/Comment"].value ? " title='" + h(row["Flag/Comment"].value) + "'" : "";
                var iconCls = row["Flag/Comment"].value ? <%=q(FlagColumnRenderer.flagEnabledCls())%> : "";

                html += "<tr>" +
                        "<td>" + (iconCls ? "<a" + comment + " href='" + h(url) + "'><i class='" + h(iconCls) + "'/></a>" : "") + "</td>" +
                        "<td nowrap><a" + comment + " href='" + h(url) + "'>" + h(name) + "</a><td>";
                if (countProperty) {
                    html += "<td align='right' nowrap>(" + h(row[countProperty].value) + " " + h(countLabel) + ")</td>";
                }
                html += "</tr>";
            }
            if (json.rows.length > <%=DISPLAY_MAX_ROWS%>) {
                html += "<tr>";
                html += "<td colspan=2><a href='" + h(detailsURL) + "'>More...</a></td>";
                html += "</tr>";
            }
            html += "</table>";
            el.update(html);
        };
    }
</script>

<% if (_fcsRunCount > 0 || _fcsAnalysisRunCount > 0 || _compensationMatrixCount > 0 || _sampleType != null) { %>
    <div class="summary-div">
        <h3 class="summary-header">Summary</h3>

    <% if (_fcsRunCount > 0) { %>
        <script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                renderer: createRenderer("<%=h(fcsFileRunsURL)%>", "FCSFileCount", "files")
            });
        });
        </script>
        <div id="fcsFileRuns-div">
            <a href="<%=h(fcsFileRunsURL)%>">FCS Files (<%=_fcsRunCount%> <%=unsafe(_fcsRunCount == 1 ? "run" : "runs")%>)</a>
        </div>
    <% } %><%-- end if (_fcsRunCount > 0) --%>

    <% if (_fcsAnalysisRunCount > 0) { %>
        <script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                renderer: createRenderer("<%=h(fcsAnalysisRunsURL)%>", "FCSAnalysisCount", "wells")
            });
        });
        </script>
        <div id="fcsAnalysisRuns-div">
            <a href="<%=h(fcsAnalysisRunsURL)%>">FCS Analyses (<%=_fcsAnalysisRunCount%> <%=unsafe(_fcsAnalysisRunCount == 1 ? "run" : "runs")%>)</a>
        </div>
    <% } %><%-- end if (_fcsAnalysisRunCount > 0) --%>

    <% if (_compensationMatrixCount > 0) { %>
        <script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                renderer: createRenderer("<%=h(compMatricesURL)%>")
            });
        });
        </script>
        <div id="compensationMatrices-div">
            <a href="<%=h(compMatricesURL)%>">Compensation (<%=_compensationMatrixCount%> <%=unsafe(_compensationMatrixCount == 1 ? "matrix" : "matrices")%>)</a>
        </div>
    <% } %><%-- end if (_compensationMatrixCount > 0) --%>

    <% if (_sampleTypeSamples != null && _sampleTypeSamples.size() > 0) { %>
        <script type="text/javascript" nonce="<%=getScriptNonce()%>">
        Ext.onReady(function () {
            var tip = new LABKEY.ext.CalloutTip({
                target: "samples-div",
                html: "<table border='0'>" +
                      <%
                        for (int sampleIndex = 0; sampleIndex < DISPLAY_MAX_ROWS && sampleIndex < _sampleTypeSamples.size(); sampleIndex++)
                        {
                            ExpMaterial sample = _sampleTypeSamples.get(sampleIndex);
                            String name = sample.getName();
                            ActionURL url = sample.detailsURL();
                            HtmlString comment = sample.getComment() != null ? unsafe(" title='" + h(sample.getComment()) + "'") : HtmlString.EMPTY_STRING;
                            String iconCls = StringUtils.isNotEmpty(sample.getComment()) ? FlagColumnRenderer.flagEnabledCls() : "";
                      %>
                          "<tr>" +
                          "<td><a<%=comment%> href='<%=h(url)%>'><i class='<%=h(iconCls)%>'/></a>" +
                          "<td nowrap><a<%=comment%> href='<%=h(url)%>'><%=h(name)%></a>" +
                          "</tr>" +
                      <%
                         }

                         if (_sampleTypeSamples.size() > DISPLAY_MAX_ROWS)
                         {
                      %>
                            "<tr>" +
                            "<td colspan=2><a href='<%=h(_sampleTypeDetailsUrl)%>'>More...</a></td>" +
                            "</tr>" +
                      <%
                         }
                      %>
                      "</table>"
            });
        });
        </script>
        <div id="samples-div">
            <a title="<%=h(_sampleType.getDescription())%>" href="<%=h(_sampleTypeDetailsUrl)%>">Samples (<%=_sampleTypeSamples.size()%>)</a>
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
            <script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                                '<a href="<%=h(script.urlShow())%>">edited</a> before it can be used.</div>',
                            <% } %>
                            <% if (script.hasStep(FlowProtocolStep.calculateCompensation)) { %>
                                '<div><a href="<%=h(script.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, FlowProtocolStep.calculateCompensation))%>">Compensation</a></div>',
                            <% } %>
                            <% if (script.hasStep(FlowProtocolStep.analysis)) { %>
                                '<div><a href="<%=h(script.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, FlowProtocolStep.analysis))%>">Statistics and Graphs</a></div>',
                            <% } %>
                        '</div>',
                        <% if (canEditScript || script.hasStep(FlowProtocolStep.calculateCompensation) || script.hasStep(FlowProtocolStep.analysis)) { %>
                            '<div class="summary-div">',
                                '<div class="summary-header">Analysis Definition</div>',
                                <% if (script.hasStep(FlowProtocolStep.calculateCompensation)) { %>
                                    '<div><a href="<%=h(script.urlFor(ScriptController.EditCompensationCalculationAction.class))%>">Show Compensation</a></div>',
                                <% } else if (canEditScript) { %>
                                    '<div><a href="<%=h(script.urlFor(ScriptController.UploadCompensationCalculationAction.class))%>">Upload FlowJo Compensation</a></div>',
                                <% } %>
                                <% if (script.hasStep(FlowProtocolStep.analysis)) { %>
                                    '<div><a href="<%=h(script.urlFor(ScriptController.EditAnalysisAction.class))%>"><%=unsafe(canEditScript ? "Edit" : "View")%> Statistics and Graphs</a></div>',
                                    <% if (canEditScript) { %>
                                        '<div><a href="<%=h(script.urlFor(ScriptController.EditGateTreeAction.class, FlowProtocolStep.analysis))%>">Rename Populations</a></div>',
                                    <% } %>
                                <% } else if (canEditScript) { %>
                                    '<div><a href="<%=h(script.urlFor(ScriptController.UploadAnalysisAction.class))%>">Upload FlowJo Analysis</a></div>',
                                <% } %>
                            '</div>',
                        <% } %>
                        '<div class="summary-div">',
                            '<div class="summary-header">Manage</div>',
                            '<div><a href="<%=h(script.urlFor(ScriptController.EditSettingsAction.class))%>">Settings</a></div>',
                            '<div><a href="<%=h(script.urlFor(ScriptController.CopyAction.class))%>">Copy</a></div>',
                            <% if (runCount == 0) { %>
                                '<div><a href="<%=h(script.urlFor(ScriptController.DeleteAction.class))%>">Delete</a></div>',
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
                <a href="<%=h(script.urlShow())%>"><%=h(script.getName())%> (<%=runCount%> <%=unsafe(runCount == 1 ? "run" : "runs")%>)</a>
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
                <script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                        renderer: createRenderer("<%=h(experiment.urlShow())%>", "WellCount", "wells")
                    });
                });
                </script>
                <%
            }
            %>
            <div id="script-<%=experiment.getExperimentId()%>-div">
                <a href="<%=h(experiment.urlShow())%>"><%=h(experiment.getName())%> (<%=runCount%> <%=unsafe(runCount == 1 ? "run" : "runs")%>)</a>
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
            ActionURL urlPipelineRoot = urlProvider(PipelineUrls.class).urlSetup(c);
            if (!_hasPipelineRoot)
            {
                %><div><%=link("Setup Pipeline", urlPipelineRoot)%></div><%
            }
            else if (_fcsFileCount == 0)
            {
                %><div><%=link("Change Pipeline", urlPipelineRoot)%></div><%
            }
        }

        if (_canUpdate)
        {
            if (!_hasPipelineRoot)
            {
                %><div><%=link("Import Workspace", new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, c))%></div><%
            }
            else
            {
                %><div><%=link("Upload and Import", urlProvider(PipelineUrls.class).urlBrowse(c, getActionURL()))%></div><%
            }

            if (_fcsRealRunCount > 0)
            {
                %><div><%=link("Create Analysis Script", new ActionURL(ScriptController.NewProtocolAction.class, c))%></div><%
            }
        }
    %>
</div>
<% } %>

<% if (_protocol != null && _canUpdate) { %>
    <div class="summary-div">
        <h3 class="summary-header">Manage</h3>
        <div><%= link("Settings", _protocol.urlShow())%></div>

        <%
        if (_sampleType != null)
        {
        %>
            <div><%=link("Show Samples", _protocol.urlShowSamples())%></div>
            <div><%=link("Upload Samples", _protocol.urlUploadSamples())%></div>
        <%
        }
        else
        {
        %>
            <div><%=link("Upload Samples", _protocol.urlCreateSampleType())%></div>
        <%
        }
        if (_fcsAnalysisCount > 0)
        {
        %>
            <div><%=link("Edit Metadata", new ActionURL(ProtocolController.EditICSMetadataAction.class, c))%></div>
        <%
        }

        int jobCount = PipelineService.get().getQueuedStatusFiles(c).size();
        %><div><%=link("Show Jobs" + (jobCount > 0 ? " (" + jobCount + " running)" : ""), urlProvider(PipelineStatusUrls.class).urlBegin(c))%></div><%

        if (_canCreateFolder && _hasPipelineRoot)
        {
            %><div><%=link("Copy Folder", new ActionURL(FlowController.NewFolderAction.class, c))%></div><%
        }
        %>
    </div>
<% } %>


