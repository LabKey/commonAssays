<%
/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.analysis.model.ISampleInfo" %>
<%@ page import="org.labkey.flow.analysis.model.IWorkspace" %>
<%@ page import="org.labkey.flow.analysis.model.PopulationName" %>
<%@ page import="org.labkey.flow.analysis.model.Workspace" %>
<%@ page import="org.labkey.flow.controllers.WorkspaceData" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.SamplesConfirmGridView" %>
<%@ page import="org.labkey.flow.controllers.executescript.SelectedSamples" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    Container container = getContainer();
    FlowProtocol protocol = FlowProtocol.getForContainer(container);

    WorkspaceData workspaceData = form.getWorkspace();
    IWorkspace workspace = workspaceData.getWorkspaceObject();

    Map<String, String> groupOptions = new TreeMap<>();
    Map<String, Collection<String[]>> groups = new TreeMap<>();
    if (workspace instanceof Workspace)
    {
        Workspace w = (Workspace)workspace;
        for (Workspace.GroupInfo group : w.getGroups())
        {
            Map<String, String[]> groupSamples = new TreeMap<>();
            for (String sampleID : group.getSampleIds())
            {
                Workspace.SampleInfo sampleInfo = w.getSample(sampleID);
                if (sampleInfo != null)
                    groupSamples.put(sampleInfo.getLabel(), new String[] { sampleInfo.getSampleId(), sampleInfo.getLabel() });
            }
            if (group.isAllSamples() || groupSamples.size() > 0)
            {
                String groupName = group.getGroupName().toString();
                groups.put(groupName, groupSamples.values());
                groupOptions.put(groupName, groupName + " (" + groupSamples.size() + " samples)");
            }
        }
    }

    List<? extends ISampleInfo> sampleInfos = workspace.getSamples();
    List<Map<String, Object>> samples = new ArrayList<>(sampleInfos.size());
    for (ISampleInfo sample : sampleInfos)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("sampleId", sample.getSampleId());
        map.put("label", sample.getLabel());
        List<String> sampleGroups = new ArrayList<>(10);
        if (sample instanceof Workspace.SampleInfo)
            for (PopulationName pop : ((Workspace.SampleInfo)sample).getGroupNames())
                sampleGroups.add(pop.toString());
        map.put("groups", sampleGroups);
        samples.add(map);
    }

    SelectedSamples selectedSamples = form.getSelectedSamples();
    SamplesConfirmGridView resolveView = new SamplesConfirmGridView(selectedSamples, form.isResolving(), null);
%>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
<input type="hidden" name="resolving" value="<%=form.isResolving()%>">

<p>Please choose which samples from the analysis should be imported. Only <b>selected rows</b> will be imported.
</p>
<% if (form.isResolving()) { %>
<p>If a sample from the analysis couldn't be resolved or was incorrectly resolved, you may correct it by
    selecting the appropriate FCS file from the dropdown.  All selected rows must have a match to be imported.
</p>
<% } %>
<hr/>
<script>
    var samples = <%=new JSONArray(samples)%>;
    var groups = <%=new JSONObject(groups)%>;
    var importedGroup = <%=PageFlowUtil.jsString(form.getImportGroupNames().length() > 0 ? form.getImportGroupNameList().get(0) : "All Samples")%>;
</script>

<%
if (protocol != null)
{
    if (protocol.getFCSAnalysisFilterString() != null)
    {
        %>
        Samples will be filtered by the current protocol <a href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" target="_blank">FCS analysis filter</a>:
        <br>
        <div style="padding-left: 2em;">
            <%=h(protocol.getFCSAnalysisFilter().getFilterText())%>
        </div>
        <%
    }
    else
    {
        %>No protocol <a href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" target="_blank">FCS analysis filter</a> has been defined in this folder.<%
    }
}

if (groups.size() > 1)
{
    %>
    <p>
    <script type="application/javascript">
        function onGroupChanged(selectedGroup) {
            importedGroup = selectedGroup || "All Samples";

            var dr = LABKEY.DataRegions[<%= PageFlowUtil.jsString(SamplesConfirmGridView.DATAREGION_NAME) %>];
            if (dr) {
                var group = groups[importedGroup];
                if (group) {
                    for (var i = 0; i < samples.length; i++) {
                        var sampleId = samples[i].sampleId;
                        // escape quotes
                        sampleId = sampleId.replace(/('|"|\\)/g, "\\$1");

                        var selected = samples[i].groups.indexOf(importedGroup) > -1;
                        var elts = document.getElementsByName("selectedSamples.rows[" + sampleId + "].selected");
                        if (elts.length > 0) {
                            var inputEl = elts[0];
                            inputEl.checked = selected;
                        }
                    }
                }
            }
        }
    </script>
    Select a group to import from the <%=h(workspace.getKindName())%>.
    <select id="importGroupNames" name="importGroupNames" onchange="onGroupChanged(this.value);">
        <labkey:options value="<%=form.getImportGroupNameList()%>" map="<%=groupOptions%>" />
    </select>
    <%
}
%>

<p></p>
<%
    include(resolveView, out);
%>
<br/>

