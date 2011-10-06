<%
    /*
    * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.query.QueryParam" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.analysis.model.FlowJoWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowProtocolStep" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.io.File" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="org.labkey.flow.util.KeywordUtil" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.api.flow.api.FlowService" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.api.data.SimpleFilter" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);
    FlowProtocol protocol = FlowProtocol.getForContainer(container);

    FlowJoWorkspace workspace = form.getWorkspace().getWorkspaceObject();

    Set<String> keywordOptions = new LinkedHashSet<String>();
    keywordOptions.add("");
    for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
    {
        for (String keyword : sampleInfo.getKeywords().keySet())
        {
            if (!KeywordUtil.isHidden(keyword))
                keywordOptions.add(keyword);
        }
    }

    Map<String, String> opOptions = new LinkedHashMap<String, String>();
    opOptions.put(CompareType.EQUAL.getPreferredUrlKey(), CompareType.EQUAL.getDisplayValue());
    opOptions.put(CompareType.NEQ_OR_NULL.getPreferredUrlKey(), CompareType.NEQ_OR_NULL.getDisplayValue());
    opOptions.put(CompareType.CONTAINS.getPreferredUrlKey(), CompareType.CONTAINS.getDisplayValue());
    opOptions.put(CompareType.DOES_NOT_CONTAIN.getPreferredUrlKey(), CompareType.DOES_NOT_CONTAIN.getDisplayValue());
    opOptions.put(CompareType.STARTS_WITH.getPreferredUrlKey(), CompareType.STARTS_WITH.getDisplayValue());
    opOptions.put(CompareType.DOES_NOT_START_WITH.getPreferredUrlKey(), CompareType.DOES_NOT_START_WITH.getDisplayValue());
    opOptions.put(CompareType.IN.getPreferredUrlKey(), CompareType.IN.getDisplayValue());

    String importGroupNames = form.getImportGroupNames();

    JSONArray jsonGroupNames = new JSONArray();
    for (FlowJoWorkspace.GroupInfo group : workspace.getGroups())
    {
        jsonGroupNames.put(new String[] { group.getGroupName().toString(), group.getGroupName().toString() });
    }

%>

<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<input type="hidden" name="runFilePathRoot" id="runFilePathRoot" value="<%=h(form.getRunFilePathRoot())%>">
<input type="hidden" name="selectAnalysisEngine" id="selectAnalysisEngine" value="<%=h(form.getSelectAnalysisEngine())%>">

<p>Analysis engine options.
</p>
<hr/>

<p>Which samples should be imported?</p>
<div style="padding-left: 2em; padding-bottom: 1em;">
<% if (protocol != null) { %>
    <% if (protocol.getFCSAnalysisFilterString() != null) { %>
        Samples will be filtered by the current protocol <a href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" target="_blank">FCS analysis filter</a>:
        <br>
        <div style="padding-left: 2em;">
            <%=protocol.getFCSAnalysisFilter().getFilterText()%>
        </div>
    <% } else { %>
        No protocol <a href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" target="_blank">FCS analysis filter</a> has been defined in this folder.
        <%System.out.println("qq");%>
    <% } %>
<% } %>
    <p>
    Select the FlowJo groups to import from the workspace. Leave blank to import all samples.
    <div id="importGroupNamesDiv"></div>
    <script>
        LABKEY.requiresScript('Ext.ux.form.LovCombo.js');
        LABKEY.requiresCss('Ext.ux.form.LovCombo.css');
    </script>
    <script>
        Ext.onReady(function () {
            var combo = new Ext.ux.form.LovCombo({
                id: "importGroupNames",
                renderTo: "importGroupNamesDiv",
                width: 275,
                value: <%=PageFlowUtil.jsString(importGroupNames)%>,
                triggerAction: "all",
                mode: "local",
                valueField: "myId",
                displayField: "displayText",
                store: new Ext.data.ArrayStore({
                    fields: ["myId", "displayText"],
                    data: <%=jsonGroupNames%>
                })
            });
        });
    </script>
</div>

<%
    if ("rEngine".equals(form.getSelectAnalysisEngine()))
    {
        Set<String> sampleLabels = new LinkedHashSet<String>(workspace.getAllSampleLabels());

        JSONArray jsonParams = new JSONArray();
        for (String param : workspace.getParameters())
        {
            jsonParams.put(new String[]{param, param});
        }
%>
<p><b>Normalization Options</b></p>
<script>
    function onNormalizationChange()
    {
        var disable = !document.getElementById("rEngineNormalization").checked;
        document.getElementById("rEngineNormalizationReference").disabled = disable;
        Ext.getCmp("rEngineNormalizationParameters").setDisabled(disable);
    }
</script>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <input type="checkbox" name="rEngineNormalization" id="rEngineNormalization" <%=form.isrEngineNormalization() ? "checked" : ""%> onchange="onNormalizationChange();">
    <label for="rEngineNormalization">Perform normalization?</label>
</div>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <label for="rEngineNormalizationReference">Select sample to be use as normalization reference.</label>
    <br>
    <select name="rEngineNormalizationReference" id="rEngineNormalizationReference">
        <labkey:options value="<%=form.getrEngineNormalizationReference()%>" set="<%=sampleLabels%>" />
    </select>
</div>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <label for="rEngineNormalizationParameters">Select parameters to be normalized.  Leave blank to normalize all parameters.</label>
    <div id="rEngineNormalizationParametersDiv"></div>
    <script>
        Ext.onReady(function () {
            var combo = new Ext.ux.form.LovCombo({
                id: "rEngineNormalizationParameters",
                renderTo: "rEngineNormalizationParametersDiv",
                value: <%=PageFlowUtil.jsString(form.getrEngineNormalizationParameters())%>,
                width: 275,
                triggerAction: "all",
                mode: "local",
                valueField: "myId",
                displayField: "displayText",
                store: new Ext.data.ArrayStore({
                    fields: ["myId", "displayText"],
                    data: <%=jsonParams%>
                })
            });
        });
    </script>
</div>

<p></p>
<%
    }
%>

