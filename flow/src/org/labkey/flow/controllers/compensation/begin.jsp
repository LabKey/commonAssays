<%@ page import="org.labkey.flow.controllers.compensation.CompensationController.Action"%>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="java.util.List"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

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
<labkey:link href="<%=PageFlowUtil.urlFor(Action.upload, getContainer())%>" text="Upload a new compensation matrix" />

