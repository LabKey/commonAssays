<%@ page import="Flow.Protocol.ProtocolForm"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowProtocol"%>
<%@ page import="Flow.Protocol.ProtocolController.Action" %>
<%@ page extends="org.fhcrc.cpas.jsp.FormPage" %>
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
