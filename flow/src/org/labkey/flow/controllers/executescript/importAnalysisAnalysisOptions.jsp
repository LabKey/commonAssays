<%
/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.api.action.SpringActionController" %>
<%@ page import="org.labkey.api.admin.AdminUrls" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.FlowSettings" %>
<%@ page import="org.labkey.flow.analysis.model.Analysis" %>
<%@ page import="org.labkey.flow.analysis.model.CompensationMatrix" %>
<%@ page import="org.labkey.flow.analysis.model.ISampleInfo" %>
<%@ page import="org.labkey.flow.analysis.model.IWorkspace" %>
<%@ page import="org.labkey.flow.analysis.model.Population" %>
<%@ page import="org.labkey.flow.analysis.model.SubsetExpressionGate" %>
<%@ page import="org.labkey.flow.analysis.web.SubsetSpec" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisEngine" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.SelectedSamples" %>
<%@ page import="org.labkey.flow.util.KeywordUtil" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    boolean normalizationEnabled = FlowSettings.isNormalizationEnabled();

    IWorkspace workspace = form.getWorkspace().getWorkspaceObject();

    SelectedSamples selectedSamples = form.getSelectedSamples();
    List<String> sampleIds = new ArrayList<>(selectedSamples.getRows().size());
    for (Map.Entry<String, SelectedSamples.ResolvedSample> entry : selectedSamples.getRows().entrySet())
    {
        if (entry.getValue().isSelected())
            sampleIds.add(entry.getKey());
    }
%>
<script>
    var sampleIds = <%=new JSONArray(sampleIds)%>;
</script>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
<input type="hidden" name="resolving" value="<%=form.isResolving()%>">
<input type="hidden" name="selectAnalysisEngine" id="selectAnalysisEngine" value="<%=h(form.getSelectAnalysisEngine())%>">

<p>Analysis engine options.
</p>
<hr/>

<%
    if (AnalysisEngine.R == form.getSelectAnalysisEngine())
    {
%>
<h3>Normalization Options</h3>
<% if (!normalizationEnabled) { %>
<p>
    <em>NOTE:</em> Normalization is current disabled.  Administrators can enable normalization in the Flow Cytometry settings of the <a href="<%=PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL()%>">Admin Console</a>.
</p>
<% } %>
<script>
    var normalizationReference = <%=PageFlowUtil.jsString(form.getrEngineNormalizationReference())%>;

    function onNormalizationChange()
    {
        var disable = !document.getElementById("rEngineNormalization").checked;
        document.getElementById("rEngineNormalizationReference").disabled = disable;
        Ext.getCmp("rEngineNormalizationSubsets").setDisabled(disable);
        Ext.getCmp("rEngineNormalizationParameters").setDisabled(disable);
    }

    function onNormalizationReferenceChanged(selectedReference)
    {
        if (selectedReference != normalizationReference)
        {
            normalizationReference = selectedReference;

            var rEngineNormalizationSubsets = Ext.getCmp('rEngineNormalizationSubsets');
            if (rEngineNormalizationSubsets)
            {
                var value = rEngineNormalizationSubsets.getValue();

                if (normalizationReference)
                {
                    rEngineNormalizationSubsets.getStore().loadData(jsonSubsetMap[normalizationReference]);
                    rEngineNormalizationSubsets.setValue(value);
                }
                else
                {
                    rEngineNormalizationSubsets.getStore().clearData();
                    rEngineNormalizationSubsets.clearValue();
                }
            }
        }
    }
</script>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <input type="checkbox" name="rEngineNormalization" id="rEngineNormalization" onchange="onNormalizationChange();"
        <%=checked(normalizationEnabled && form.isrEngineNormalization())%>
        <%=disabled(!normalizationEnabled)%> >
    <input type="hidden" name="<%=text(SpringActionController.FIELD_MARKER)%>rEngineNormalization"/>
    Perform normalization using flowWorkspace R library? (experimental)
</div>

<div style="padding-left: 2em; padding-bottom: 1em;">
    Select sample to be use as normalization reference.<br>
    <em>NOTE:</em> The list of available samples is restricted to those selected in a previous step.<br>
    <select name="rEngineNormalizationReference" id="rEngineNormalizationReference"
        <%=disabled(!normalizationEnabled)%> onchange="onNormalizationReferenceChanged(this.value);" >
        <option value="">&lt;Select sample&gt;</option>
        <%
            String rEngineNormalizationReference = form.getrEngineNormalizationReference();
            for (String sampleId : sampleIds)
            {
                ISampleInfo sampleInfo = workspace.getSample(sampleId);
                if (sampleInfo != null)
                {
                    boolean selected = sampleInfo.getSampleId().equals(rEngineNormalizationReference);
        %><option value=<%=h(sampleInfo.getSampleId())%><%=selected(selected)%>><%=h(sampleInfo.getLabel())%></option><%
                }
            }
        %>
    </select>
</div>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <%!
        public void addPopulation(JSONArray jsonSubsets, SubsetSpec parent, Population pop)
        {
            // Can't apply normalization to populations created from boolean gates.  Ignore.
            if (pop.getGates().size() == 1 && pop.getGates().get(0) instanceof SubsetExpressionGate)
                return;

            SubsetSpec subset = new SubsetSpec(parent, pop.getName());
            jsonSubsets.put(new String[] {subset.toString(), subset.toString()});

            for (Population child : pop.getPopulations())
            {
                addPopulation(jsonSubsets, subset, child);
            }
        }
    %>
    <%
        JSONObject jsonSubsetMap = new JSONObject();
        for (String sampleId : sampleIds)
        {
            ISampleInfo sampleInfo = workspace.getSample(sampleId);
            Analysis analysis = sampleInfo != null ? workspace.getSampleAnalysis(sampleInfo) : null;
            if (analysis != null)
            {
                JSONArray jsonSubsets = new JSONArray();

                for (Population child : analysis.getPopulations())
                {
                    addPopulation(jsonSubsets, null, child);
                }

                jsonSubsetMap.put(sampleId, jsonSubsets);
            }
        }
    %>
    Select subsets to be normalized.  At least one subset must be selected.<br>
    <em>NOTE:</em> The list of available subsets is restricted to those in the reference sample and excludes boolean subsets.<br>
    <div id="rEngineNormalizationSubsetsDiv"></div>
    <script>
        LABKEY.requiresScript('Ext.ux.form.LovCombo.js');
        LABKEY.requiresCss('Ext.ux.form.LovCombo.css');
    </script>
    <script>
        var jsonSubsetMap = <%=text(jsonSubsetMap.toString())%>;

        Ext.onReady(function () {
            var combo = new Ext.ux.form.LovCombo({
                id: "rEngineNormalizationSubsets",
                renderTo: "rEngineNormalizationSubsetsDiv",
                value: <%=PageFlowUtil.jsString(form.getrEngineNormalizationSubsets())%>,
                disabled: <%=text(normalizationEnabled ? "false" : "true")%>,
                width: 475,
                triggerAction: "all",
                mode: "local",
                valueField: "myId",
                displayField: "displayText",
                allowBlank: false,
                separator: "<%=text(ImportAnalysisForm.PARAMETER_SEPARATOR)%>",
                store: new Ext.data.ArrayStore({
                    fields: ["myId", "displayText"],
                    data: normalizationReference && jsonSubsetMap[normalizationReference] || []
                })
            });
        });
    </script>
</div>

<div style="padding-left: 2em; padding-bottom: 1em;">
    <%
        JSONArray jsonParams = new JSONArray();
        for (String param : workspace.getParameterNames())
        {
            if (KeywordUtil.isColorChannel(param))
            {
                String compensated = CompensationMatrix.isParamCompensated(param) ? param :
                        (CompensationMatrix.PREFIX + param + CompensationMatrix.SUFFIX);
                jsonParams.put(new String[]{compensated, compensated});
            }
        }
    %>
    Select the compensated parameters to be normalized.  At least one parameter must be selected.
    <div id="rEngineNormalizationParametersDiv"></div>
    <script>
        LABKEY.requiresScript('Ext.ux.form.LovCombo.js');
        LABKEY.requiresCss('Ext.ux.form.LovCombo.css');
    </script>
    <script>
        Ext.onReady(function () {
            var combo = new Ext.ux.form.LovCombo({
                id: "rEngineNormalizationParameters",
                renderTo: "rEngineNormalizationParametersDiv",
                value: <%=PageFlowUtil.jsString(form.getrEngineNormalizationParameters())%>,
                disabled: <%=text(normalizationEnabled ? "false" : "true")%>,
                width: 275,
                triggerAction: "all",
                mode: "local",
                valueField: "myId",
                displayField: "displayText",
                allowBlank: false,
                separator: "<%=text(ImportAnalysisForm.PARAMETER_SEPARATOR)%>",
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


