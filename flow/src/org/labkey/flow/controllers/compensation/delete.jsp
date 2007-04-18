<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page import="org.labkey.flow.util.PFUtil"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getViewContext().getViewURLHelper(), request); %>
<form method="POST" action="<%=comp.urlFor(CompensationController.Action.delete)%>">
    <p>Are you sure you want to delete this compensation matrix?<br>
        <a href="<%=h(comp.urlShow())%>"><%=h(comp.getName())%></a>
    </p>
    <labkey:button text="OK" /> <labkey:button text="Cancel" href="<%=PFUtil.urlFor(CompensationController.Action.begin, getContainer())%>"/>
</form>