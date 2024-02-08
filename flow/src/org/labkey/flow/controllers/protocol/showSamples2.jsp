<%
/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.api.ExpSampleType"%>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.api.query.QueryAction" %>
<%@ page import="org.labkey.api.util.URLHelper" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.JoinSampleTypeAction" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolForm" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.FlowProtocol.FCSFilesGroupedBySample" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="static org.labkey.api.data.CompareType.IN" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ProtocolForm form = (ProtocolForm) __form;
    FlowProtocol protocol = form.getProtocol();
    ExpSampleType st = protocol.getSampleType(getUser());

    ExperimentUrls expUrls = urlProvider(ExperimentUrls.class);

    var nameFieldKey = FieldKey.fromParts("Name");

    var sampleTypeJoinFields = protocol.getSampleTypeJoinFields();

    FCSFilesGroupedBySample fcsFilesBySample = protocol.getFCSFilesGroupedBySample(getUser(), getContainer());
    var samples = fcsFilesBySample.samples;
    var fcsFiles = fcsFilesBySample.fcsFiles;
    var fcsFileRuns = fcsFilesBySample.fcsFileRuns;
    var linkedSampleIdToFcsFileIds = fcsFilesBySample.linkedSampleIdToFcsFileIds;
    var unlinkedSampleIds = fcsFilesBySample.unlinkedSampleIds;
    var unlinkedFcsFileIds = fcsFilesBySample.unlinkedFcsFileIds;
    var linkedFCSFileCount = fcsFilesBySample.linkedFcsFileCount;
    var fcsFileFields = fcsFilesBySample.fcsFileFields;
    var sampleFields = fcsFilesBySample.sampleFields;

    int sampleCount = samples.size();
    int colCount = sampleFields.size() + 1 + fcsFileFields.size() + 1;

    ActionURL urlFcsFilesWithSamples = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
            .addParameter("query.Sample/Name~isnonblank", "");

    ActionURL urlFcsFilesWithoutSamples = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
            .addParameter("query.Sample/Name~isblank", "");

    URLHelper urlUnlinkedSamples = null;
    if (st != null)
    {
        urlUnlinkedSamples = st.detailsURL();
        urlUnlinkedSamples.addFilter("Material", FieldKey.fromParts("RowId"), IN, unlinkedSampleIds);
    }
%>

<% if (st == null) { %>
    No samples have been imported in this folder.<br>
    <%=link("Create sample type").href(protocol.urlCreateSampleType())%><br>
<% } else { %>
<p>
There are <a id="all-samples" href="<%=h(protocol.getSampleTypeDetailsURL(st, getContainer()))%>"><%=sampleCount%> sample descriptions</a> in this folder.<br>

<% if (sampleTypeJoinFields.size() == 0) { %>
<p>
    <%=link("Join samples to FCS File Data").href(protocol.urlFor(JoinSampleTypeAction.class))%><br>
    No sample join fields have been defined yet.  The samples are linked to the FCS files using keywords.  When new samples are added or FCS files are loaded, new links will be created.
<% } else { %>
    Samples are joined to FCSFiles by the following properties (<a href="<%=h(protocol.urlFor(JoinSampleTypeAction.class))%>">edit</a>):
<ul>
    <% for (var entry : sampleTypeJoinFields.entrySet()) { %>
    <li>Sample <%=h(entry.getKey())%> => FCSFile <%=h(entry.getValue())%></li>
    <% } %>
</ul>
<br>

<style>
    #flow-sample-fcsfiles {
        border-collapse: separate;
    }
    #flow-sample-fcsfiles th {
        position: sticky;
        top: 0;
        box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.4);
        background-color: white;
        border-right: 1px solid #d3d3d3;
    }
</style>
<table id="flow-sample-fcsfiles" class="labkey-data-region-legacy labkey-show-borders" width="100%" style="min-width: 300px">
    <thead>
    <tr>
        <th class="labkey-column-header"><b>Sample Name</b></th>
        <% for (var sampleField : sampleFields) { %>
        <th class="labkey-column-header"><%=h(sampleField)%></th>
        <% } %>
        <th class="labkey-column-header"><b>FCS Files (Run)</b></th>
        <% for (var fcsFileField : fcsFileFields) { %>
        <th class="labkey-column-header"><%=h(fcsFileField)%></th>
        <% } %>
    </tr>
    </thead>

    <tbody>
    <tr class="labkey-row">
        <td style="background-color:#ddd;padding:0.5em;" colspan="<%=colCount%>">
            <b>Linked Samples and FCSFiles</b>
            <small>
                &mdash; <a id="linked-fcsfiles" href="<%=h(urlFcsFilesWithSamples)%>"><%=linkedFCSFileCount%> FCS Files</a> are joined with samples<br>
            </small>
        </td>
    </tr>
    <% if (linkedSampleIdToFcsFileIds.isEmpty()) { %>
    <tr>
        <td colspan="<%=colCount%>">
            <em>No data to show.</em>
        </td>
    </tr>
    <% } %>
    <%
        int i = 0;
        for (Map.Entry<Integer, List<Integer>> entry : linkedSampleIdToFcsFileIds.entrySet())
        {
            i++;
            List<Integer> fcsFileIds = entry.getValue();
            var sampleId = entry.getKey();
            var sample = samples.get(entry.getKey());
            String sampleName = (String)sample.get(nameFieldKey);
    %>
    <tr class="<%=getShadeRowClass(i)%>">
        <td valign="top">
            <a href="<%=h(expUrls.getMaterialDetailsURL(getContainer(), sampleId))%>"><%=h(sampleName)%></a>
        </td>
        <% for (var sampleField : sampleFields) { %>
        <td valign="top"><%=h(sample.get(sampleField))%></td>
        <% } %>
        <td>
            <%
                for (Integer fcsFileId : fcsFileIds)
                {
                    var fcsFile = fcsFiles.get(fcsFileId);
                    String fcsFileName = (String)fcsFile.get(nameFieldKey);
                    var fcsFileRun = fcsFileRuns.get(fcsFileId);
            %><a href="<%=h(new ActionURL(WellController.ShowWellAction.class, getContainer()).addParameter(FlowParam.wellId, fcsFileId))%>"><%=h(fcsFileName)%></a>
            <% if (fcsFileRun != null) { %>
            (<a href="<%=h(new ActionURL(RunController.ShowRunAction.class, getContainer()).addParameter(FlowParam.runId, fcsFileRun.first))%>"><%=h(fcsFileRun.second)%></a>)
            <% } %>
            <br><%
                }
        %>
        </td>
        <% for (var fcsFileField : fcsFileFields) { %>
        <td valign="top">
            <%
                for (Integer fcsFileId : fcsFileIds)
                {
                    var fcsFile = fcsFiles.get(fcsFileId);
            %><%=h(fcsFile.get(fcsFileField))%><br><%
            }
        %>
        </td>
        <% } %>
    </tr>
    <%
        }
    %>
    </tbody>

    <tbody>
    <tr class="labkey-row">
        <td style="background-color:#ddd;padding:0.5em;" colspan="<%=colCount%>">
            <b>Unlinked Samples</b>
            <small>
                &mdash;
                <a id="unlinked-samples" href="<%=h(urlUnlinkedSamples)%>"><%=unlinkedSampleIds.size()%> samples</a> <%=unsafe(unlinkedSampleIds.size() == 1 ? "is" : "are")%> not joined to any FCS Files<br>
            </small>
        </td>
    </tr>
    <% if (unlinkedSampleIds.isEmpty()) { %>
    <tr>
        <td colspan="<%=colCount%>">
            <em>No data to show.</em>
        </td>
    </tr>
    <% } %>
    <%
        for (Integer sampleId : unlinkedSampleIds)
        {
            i++;
            var sample = samples.get(sampleId);
            String sampleName = (String) sample.get(nameFieldKey);
    %>
    <tr class="<%=getShadeRowClass(i)%>">
        <td valign="top">
            <a href="<%=h(expUrls.getMaterialDetailsURL(getContainer(), sampleId))%>"><%=h(sampleName)%></a>
        </td>
        <% for (var sampleField : sampleFields) { %>
        <td><%=h(sample.get(sampleField))%></td>
        <% } %>
        <td>&nbsp;</td>
        <% for (var fcsFileField : fcsFileFields) { %>
        <td valign="top">&nbsp;</td>
        <% } %>
    </tr>
    <% } %>
    </tbody>

    <tbody>
    <tr class="labkey-row">
        <td style="background-color:#ddd;padding:0.5em;" colspan="<%=colCount%>">
            <b>Unlinked FCSFiles</b>
            <small>
                &mdash;
                <a id="unlinked-fcsfiles" href="<%=h(urlFcsFilesWithoutSamples)%>"><%=unlinkedFcsFileIds.size()%> FCS Files</a> are not joined with any samples<br>
            </small>
        </td>
    </tr>
    <% if (unlinkedFcsFileIds.isEmpty()) { %>
    <tr>
        <td colspan="<%=colCount%>">
            <em>No data to show.</em>
        </td>
    </tr>
    <% } %>
    <%
        for (Integer fcsFileId : unlinkedFcsFileIds)
        {
            i++;
            Map<FieldKey, Object> fcsFile = fcsFiles.get(fcsFileId);
            String fcsFileName = (String)fcsFile.get(nameFieldKey);
    %>
    <tr class="<%=getShadeRowClass(i)%>">
        <td>&nbsp;</td>
        <% for (var sampleField : sampleFields) { %>
        <td valign="top">&nbsp;</td>
        <% } %>
        <td valign="top">
            <a href="<%=h(new ActionURL(WellController.ShowWellAction.class, getContainer()).addParameter("wellId", fcsFileId))%>"><%=h(fcsFileName)%></a>
        </td>
        <% for (var fcsFileField : fcsFileFields) { %>
        <td valign="top">
            <%=h(fcsFile.get(fcsFileField))%>
        </td>
        <% } %>
    </tr>
    <% } %>
    </tbody>

</table>

    <% } %>

<% } %>

