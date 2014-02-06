<%
/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.JoinSampleSetAction"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolForm" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.AttributeType" %>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% ProtocolForm form = (ProtocolForm) __form;
   FlowProtocol protocol = form.getProtocol();
%>
<p>
    The Flow Protocol describes sample information and metadata about the experiment.
</p>
<p><b>Samples</b><br>
    Upload sample information and match samples with FCSFiles.<br>
    <% if (protocol.getSampleSet() == null) { %>
        No samples have been uploaded in this folder.<br>
        <labkey:link href="<%=protocol.urlUploadSamples(false)%>" text="Upload samples from a spreadsheet" /><br>
    <% } else { %>
        <labkey:link href="<%=protocol.getSampleSet().detailsURL()%>" text="Show sample set"/><br>
        <labkey:link href="<%=protocol.urlShowSamples(false)%>" text="Show samples joined to FCS Files" /><br>
        <labkey:link href="<%=protocol.urlUploadSamples(true)%>" text="Upload more samples from a spreadsheet" /><br>
        <% if (protocol.getSampleSetJoinFields().size() != 0) { %>
            <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Modify sample join fields" /><br>
        <% } else { %>
            <labkey:link href="<%=protocol.urlFor(JoinSampleSetAction.class)%>" text="Join samples to FCS File Data" /><br>
        <% } %>
    <% } %>
</p>
<p><b>FCS Analysis Display Names</b><br>
    When you analyze an FCS file, the FCS analysis can be given a name composed from keyword values from the FCS file.<br>
    <labkey:link href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisNameAction.class)%>" text="Change FCS Analyses Names" />
</p>
<p><b>FCS Analysis Filter</b><br>
    You can choose to only analyze FCS files where the keywords match certain criteria.<br>
    <labkey:link href="<%=protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" text="Edit FCS Analysis Filter" />
</p>
<p><b>Metadata</b><br>
    Identify participant visit/date columns and
    columns used to subtract background from stimulated wells.<br>
    <labkey:link href="<%=protocol.urlFor(ProtocolController.EditICSMetadataAction.class)%>" text="Edit ICS Metadata" />
</p>
<p><b>Manage Names and Aliases</b><br>
    Create and remove names and aliases for Keywords, Statistics, and Graphs.<br>
    <labkey:link href='<%=protocol.urlFor(AttributeController.DeleteUnusedAction.class).addReturnURL(getActionURL())%>' text="Delete Unused"/><br/>
    <labkey:link href='<%=protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.keyword.name())%>' text="Manage Keywords"/><br/>
    <labkey:link href='<%=protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.statistic.name())%>' text="Manage Statistics"/><br/>
    <labkey:link href='<%=protocol.urlFor(AttributeController.SummaryAction.class).addParameter(AttributeController.Param.type, AttributeType.graph.name())%>' text="Manage Graphs"/><br/>
</p>
