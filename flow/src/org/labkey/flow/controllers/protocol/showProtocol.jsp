<%
/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.JoinSampleTypeAction" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolForm" %>
<%@ page import="org.labkey.flow.data.AttributeType" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.api.exp.api.ExpSampleType" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ProtocolForm form = (ProtocolForm) __form;
    FlowProtocol protocol = form.getProtocol();
    ExpSampleType sampleType = protocol.getSampleType(getUser());
%>
<p>
    The Flow Protocol describes sample information and metadata about the experiment.
</p>
<p><b>Samples</b><br>
    Upload sample information and match samples with FCSFiles.<br>
    <% if (sampleType == null) { %>
        No samples have been uploaded in this folder.<br>
        <%=link("Create new sample type").href(protocol.urlCreateSampleType())%><br>
    <% } else { %>
        <%=link("Show sample type").href(protocol.getSampleTypeDetailsURL(sampleType, getContainer()))%><br>
        <%=link("Show samples joined to FCS Files").href(protocol.urlShowSamples())%><br>
        <%=link("Upload more samples from a spreadsheet").href(protocol.urlUploadSamples())%><br>
        <% if (protocol.getSampleTypeJoinFields().size() != 0) { %>
            <%=link("Modify sample join fields").href(protocol.urlFor(JoinSampleTypeAction.class))%><br>
        <% } else { %>
            <%=link("Join samples to FCS File Data").href(protocol.urlFor(JoinSampleTypeAction.class))%><br>
        <% } %>
    <% } %>
</p>
<p><b>FCS Analysis Display Names</b><br>
    When you analyze an FCS file, the FCS analysis can be given a name composed from keyword values from the FCS file.<br>
    <%=link("Change FCS Analyses Names").href(protocol.urlFor(ProtocolController.EditFCSAnalysisNameAction.class))%>
</p>
<p><b>FCS Analysis Filter</b><br>
    You can choose to only analyze FCS files where the keywords match certain criteria.<br>
    <%=link("Edit FCS Analysis Filter").href(protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class))%>
</p>
<p><b>Metadata</b><br>
    Identify participant visit/date columns and
    columns used to subtract background from stimulated wells.<br>
    <%=link("Edit Metadata").href(protocol.urlFor(ProtocolController.EditICSMetadataAction.class))%>
</p>
<p><b>Manage Names and Aliases</b><br>
    Create and remove names and aliases for Keywords, Statistics, and Graphs.<br>
    <%=link("Case sensitivity").href(protocol.urlFor(AttributeController.CaseSensitivityAction.class).addReturnURL(getActionURL()))%><br/>
    <%=link("Delete Unused").href(protocol.urlFor(AttributeController.DeleteUnusedAction.class).addReturnURL(getActionURL()))%><br/>
    <%=link("Manage Keywords").href(protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.keyword.name()))%><br/>
    <%=link("Manage Statistics").href(protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.statistic.name()))%><br/>
    <%=link("Manage Graphs").href(protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.graph.name()))%><br/>
</p>
