<%@ page import="org.labkey.flow.controllers.protocol.ProtocolForm"%>
<%@ page import="org.labkey.flow.data.FlowProtocol"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.Action" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% ProtocolForm form = (ProtocolForm) __form;
   FlowProtocol protocol = form.getProtocol();
%>
<p>
    The Flow Protocol describes some things about the experiment.
</p>
<p><b>Samples</b><br>
    The Flow Protocol describes how to match samples from a Sample Set with FCSFiles.<br>
    <% if (protocol.getSampleSet() == null) { %>
        No samples have been uploaded in this folder.<br>
        <labkey:link href="<%=protocol.urlUploadSamples()%>" text="Upload samples from a spreadsheet" /><br>
    <% } else { %>
        <labkey:link href="<%=protocol.getSampleSet().detailsURL()%>" text="Show samples" /><br>
        <labkey:link href="<%=protocol.urlUploadSamples()%>" text="Upload more samples from a spreadsheet" /><br>
        <labkey:link href="<%=protocol.urlFor(Action.joinSampleSet)%>" text="Join samples to FCS File Data" /><br>
    <% } %>
</p>
<p><b>FCS Analysis Display Names</b><br>
    When you analyze an FCS file, the FCS analysis can be given a name composed from keyword values from the FCS file.<br>
    <labkey:link href="<%=protocol.urlFor(ProtocolController.Action.editFCSAnalysisName)%>" text="Change FCS Analyses Names" />
</p>
<p><b>FCS Analysis Filter</b><br>
    You can choose to only analyze FCS files where the keywords match certain criteria.<br>
    <labkey:link href="<%=protocol.urlFor(ProtocolController.Action.editFCSAnalysisFilter)%>" text="Edit FCS Analysis Filter" />
</p>
