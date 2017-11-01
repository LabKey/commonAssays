<%
/*
 * Copyright (c) 2010-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.api.ExpMaterial"%>
<%@ page import="org.labkey.api.exp.api.ExpSampleSet"%>
<%@ page import="org.labkey.api.query.QueryAction"%>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.JoinSampleSetAction" %>
<%@ page import="org.labkey.flow.data.FlowFCSFile" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.persist.FlowManager" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ProtocolController.ShowSamplesForm form = (ProtocolController.ShowSamplesForm) __form;
    FlowProtocol protocol = form.getProtocol();
    ExpSampleSet ss = protocol.getSampleSet();
    List<? extends ExpMaterial> samples = ss == null ? null : ss.getSamples();
    boolean unlinkedOnly = form.isUnlinkedOnly();
%>
<% if (ss == null || samples == null || samples.size() == 0) { %>
    No samples have been imported in this folder.<br>
    <labkey:link href="<%=protocol.urlUploadSamples(ss != null)%>" text="Import samples from a spreadsheet" /><br>
<% } else { %>
<p>
There are <a href="<%=h(ss.detailsURL())%>"><%=samples.size()%> sample descriptions</a> in this folder.

<% if (protocol.getSampleSetJoinFields().size() == 0) { %>
    <p>
    <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Join samples to FCS File Data" /><br>
    No sample join fields have been defined yet.  The samples are linked to the FCS files using keywords.  When new samples are added or FCS files are loaded, new links will be created.
<% } else { %>
    <%
        int unlinkedCount = FlowProtocol.getUnlinkedSampleCount(samples);
        int fcsFilesWithSamplesCount = FlowManager.get().getFCSFileSamplesCount(getUser(), getContainer(), true);
        int fcsFilesWithoutSamplesCount = FlowManager.get().getFCSFileSamplesCount(getUser(), getContainer(), false);

        ActionURL urlFcsFilesWithSamples = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
                .addParameter("query.Sample/Name~isnonblank", "");

        ActionURL urlFcsFilesWithoutSamples = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
                .addParameter("query.Sample/Name~isblank", "");
    %>

    <% if (unlinkedCount > 0) { %>
        <a href="<%=h(protocol.urlShowSamples(true))%>"><%=unlinkedCount%> <%=text(unlinkedCount == 1 ? "sample is" : "samples are")%> not joined</a> to any FCS Files.
    <% } %>
    </p>
    <p>
    <a href="<%=h(urlFcsFilesWithSamples)%>"><%=fcsFilesWithSamplesCount%> FCS Files</a> have been joined with a sample and
    <a href="<%=h(urlFcsFilesWithoutSamples)%>"><%=fcsFilesWithoutSamplesCount%> FCS Files</a> are not joined with any samples.
    </p>

    <p><% if (unlinkedOnly) { %><b>Showing Unlinked Samples</b><% } %>
    <table class="labkey-data-region-legacy labkey-show-borders">
        <thead>
        <tr>
            <td class="labkey-column-header">Sample Name</td>
            <td class="labkey-column-header">FCS Files</td>
        </tr>
        </thead>
        <%
            for (int i = 0; i < samples.size(); i++)
            {
                ExpMaterial sample = samples.get(i);
                List<FlowFCSFile> fcsFiles = FlowProtocol.getFCSFiles(sample);
                if (unlinkedOnly && fcsFiles.size() > 0)
                    continue;

                %>
                <tr class="<%=getShadeRowClass(i%2==0)%>">
                <td valign="top"><a href="<%=h(sample.detailsURL())%>"><%= h(sample.getName())%></a></td>
                <td>
                    <% if (fcsFiles.size() > 0) { %>
                        <% for (FlowFCSFile fcsFile : fcsFiles) { %>
                            <a href="<%=h(fcsFile.urlShow())%>"><%=h(fcsFile.getName())%></a><br>
                        <% } %>
                    <% } else { %>
                        <em>unlinked</em>
                    <% } %>
                </td>
                </tr>
                <%
            }
        %>
    </table>
    </p>

    <p>
        <labkey:link href="<%=ss.detailsURL()%>" text="Show sample set"/><br>
        <labkey:link href="<%=protocol.urlUploadSamples(true)%>" text="Upload more samples from a spreadsheet" /><br>
        <% if (protocol.getSampleSetJoinFields().size() != 0) { %>
            <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Modify sample join fields" /><br>
        <% } else { %>
            <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Join samples to FCS File Data" /><br>
        <% } %>
    </p>
    <% } %>

<% } %>
