<%
/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.api.ExpSampleSet"%>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.JoinSampleSetAction" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="java.util.ArrayList" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ProtocolController.ShowSamplesForm form = (ProtocolController.ShowSamplesForm) __form;
    FlowProtocol protocol = form.getProtocol();
    ExpSampleSet ss = protocol.getSampleSet();
    boolean unlinkedOnly = form.isUnlinkedOnly();

    ExperimentUrls expUrls = PageFlowUtil.urlProvider(ExperimentUrls.class);

    Map<Pair<Integer, String>, List<Pair<Integer, String>>> fcsFilesBySample = protocol.getFCSFilesGroupedBySample(getUser(), getContainer());
    List<Pair<Integer,String>> unlinkedSamples = new ArrayList<>();
    List<Pair<Integer,String>> unlinkedFCSFiles = new ArrayList<>();

%>
<% if (ss == null) { %>
    No samples have been imported in this folder.<br>
    <labkey:link href="<%=protocol.urlUploadSamples(ss != null)%>" text="Import samples" /><br>
<% } else { %>
<p>
There are <a href="<%=h(ss.detailsURL())%>"><%=fcsFilesBySample.size()%> sample descriptions</a> in this folder.

<% if (protocol.getSampleSetJoinFields().size() == 0) { %>
<p>
    <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Join samples to FCS File Data" /><br>
    No sample join fields have been defined yet.  The samples are linked to the FCS files using keywords.  When new samples are added or FCS files are loaded, new links will be created.
<% } else { %>

    <table cellpadding="10">
        <tr>
            <td valign="top">
                <h3>Linked Samples</h3>

    <table class="labkey-data-region labkey-show-borders" width="100%" style="min-width: 300px">
        <thead>
        <tr>
            <td class="labkey-column-header">Sample Name</td>
            <td class="labkey-column-header">FCS Files</td>
        </tr>
        </thead>
    <%
        int i = 0;
        for (Pair<Integer, String> sample : fcsFilesBySample.keySet())
        {
            List<Pair<Integer, String>> fcsFiles = fcsFilesBySample.get(sample);
            if (sample.first == null)
            {
                unlinkedFCSFiles.addAll(fcsFiles); continue;
            }

            i++;

            int fcsFileCount = 0;
            for (Pair<Integer, String> fcsFile : fcsFiles)
            {
                if (fcsFile.first != null)
                    fcsFileCount++;
            }

            if (fcsFileCount == 0)
            {
                unlinkedSamples.add(sample);
                continue;
            }

            %>
        <tr class="<%=getShadeRowClass(i%2 == 0)%>">
            <td valign="top">
                <a href="<%=expUrls.getMaterialDetailsURL(getContainer(), sample.first)%>"><%=h(sample.second)%></a>
                (<%=fcsFileCount%>)
            </td>
            <td>
            <%
                for (Pair<Integer, String> fcsFile : fcsFiles)
                {
                    if (fcsFile.first != null)
                    {
                        %><a href="<%=new ActionURL(WellController.ShowWellAction.class, getContainer()).addParameter("wellId", fcsFile.first)%>"><%=h(fcsFile.second)%></a><br><%
                    }
                }
            %>
            </td>
        </tr>
            <%
        }
    %>
    </table>

            </td>

            <td valign="top">
                <h3>Unlinked Samples</h3>
                <table class="labkey-data-region labkey-show-borders" width="100%" style="min-width: 200px">
                    <thead>
                    <tr>
                        <td class="labkey-column-header">Sample Name</td>
                    </tr>
                    <%
                        int sampleIdx = 0;
                        for (Pair<Integer,String> sample : unlinkedSamples)
                        {
                            sampleIdx++;
                    %>
                    <tr class="<%=getShadeRowClass(sampleIdx%2 == 0)%>">
                        <td valign="top">
                            <a href="<%=expUrls.getMaterialDetailsURL(getContainer(), sample.first)%>"><%=h(sample.second)%></a>
                        </td>
                    </tr>
                    <% } %>
                    </thead>
                </table>
            </td>

            <td valign="top">
                <h3>Unlinked FCSFiles</h3>
                <table class="labkey-data-region labkey-show-borders" width="100%" style="min-width: 200px">
                    <thead>
                    <tr>
                        <td class="labkey-column-header">FCSFile</td>
                    </tr>
                    <%
                        int fcsFileIdx = 0;
                        for (Pair<Integer,String> fcsFile : unlinkedFCSFiles)
                        {
                            fcsFileIdx++;
                    %>
                    <tr class="<%=getShadeRowClass(fcsFileIdx%2 == 0)%>">
                        <td valign="top">
                            <a href="<%=new ActionURL(WellController.ShowWellAction.class, getContainer()).addParameter("wellId", fcsFile.first)%>"><%=h(fcsFile.second)%></a>
                        </td>
                    </tr>
                    <% } %>
                    </thead>
                </table>
            </td>
        </tr>
    </table>

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

