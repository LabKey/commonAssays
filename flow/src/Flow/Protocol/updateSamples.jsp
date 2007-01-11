<%@ page import="Flow.Protocol.UpdateSamplesForm"%>
<%@ page import="Flow.Protocol.ProtocolController.Action"%>
<%@ page extends="org.fhcrc.cpas.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% UpdateSamplesForm form = (UpdateSamplesForm) __form;%>
<cpas:errors />
<p>
    <%= form.fileCount%> FCS files were linked to samples in this sample set.
</p>
<p><a href="<%=h(form.getProtocol().getSampleSet().detailsURL())%>">Show Samples</a><br>
<a href="<%=h(form.getProtocol().urlFor(Action.joinSampleSet))%>">Edit join properties</a></p>