<%@ page import="org.labkey.flow.controllers.protocol.UpdateSamplesForm"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.Action"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% UpdateSamplesForm form = (UpdateSamplesForm) __form;%>
<labkey:errors />
<p>
    <%= form.fileCount%> FCS files were linked to samples in this sample set.
</p>
<p><a href="<%=h(form.getProtocol().getSampleSet().detailsURL())%>">Show Samples</a><br>
<a href="<%=h(form.getProtocol().urlFor(Action.joinSampleSet))%>">Edit join properties</a></p>