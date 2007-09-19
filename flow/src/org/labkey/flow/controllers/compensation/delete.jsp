<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getViewContext().getViewURLHelper(), request); %>
<p>Are you sure you want to delete this compensation matrix?<br>
    <a href="<%=h(comp.urlShow())%>"><%=h(comp.getName())%></a>
</p>