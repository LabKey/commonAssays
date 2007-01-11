<%@ page import="Flow.Compensation.CompensationController.Action"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.fhcrc.cpas.flow.util.PFUtil"%>
<%@ page import="java.util.List"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib uri="http://cpas.fhcrc.org/taglib/cpas" prefix="cpas" %>

<% List<FlowCompensationMatrix> matrices = FlowCompensationMatrix.getUploadedCompensationMatrices(getContainer()); %>
<% if (matrices.size() == 0) { %>
<p>No compensation matrices have been uploaded to this folder.</p>
<% } else { %>
<table class="normal">
    <tr><th>Name</th><th>Created</th></tr>
    <% for (FlowCompensationMatrix matrix : matrices) { %>
        <tr><td><a href="<%=h(matrix.urlShow())%>"><%=matrix.getName()%></a></td><td><%=formatDateTime(matrix.getExpObject().getCreated())%></td></tr>
    <% } %>
</table>
<% } %>
<cpas:link href="<%=PFUtil.urlFor(Action.upload, getContainer())%>" text="Upload a new compensation matrix" />

