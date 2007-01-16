<%@ page import="org.labkey.flow.controllers.protocol.ProtocolForm"%>
<%@ page import="org.labkey.flow.data.FlowProtocol"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.Action" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% ProtocolForm form = (ProtocolForm) __form;
   FlowProtocol protocol = form.getProtocol();
%>
<p>
    The Flow Protocol describes some things about the experiment.
</p>
<p><b>Samples</b><br>
    The Flow Protocol describes how to match samples form a Sample Set with FCSFiles.<br>
    <% if (protocol.getSampleSet() == null) { %>
        No samples have been uploaded in this folder.<br>
        <cpas:link href="<%=protocol.urlUploadSamples()%>" text="Upload samples from a spreadsheet" /><br>
    <% } else { %>
        <cpas:link href="<%=protocol.getSampleSet().detailsURL()%>" text="Show samples" /><br>
        <cpas:link href="<%=protocol.urlFor(Action.joinSampleSet)%>" text="Join samples to FCS File Data" /><br>
    <% } %>
</p>
<p><b>FCS Analysis Display Names</b><br>
    When you analyze an FCS file, the FCS analysis can be given a name composed from keyword values from the FCS file.<br>
    <cpas:link href="<%=protocol.urlFor(ProtocolController.Action.editFCSAnalysisName)%>" text="Change FCS Analyses Names" />
</p>
