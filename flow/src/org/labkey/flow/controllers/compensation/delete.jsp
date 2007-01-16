<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController"%>
<%@ page import="org.labkey.flow.util.PFUtil"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getViewContext().getViewURLHelper(), request); %>
<form method="POST" action="<%=comp.urlFor(CompensationController.Action.delete)%>">
    <p>Are you sure you want to delete this compensation matrix?<br>
        <a href="<%=h(comp.urlShow())%>"><%=h(comp.getName())%></a>
    </p>
    <cpas:button text="OK" /> <cpas:button text="Cancel" href="<%=PFUtil.urlFor(CompensationController.Action.begin, getContainer())%>"/>
</form>