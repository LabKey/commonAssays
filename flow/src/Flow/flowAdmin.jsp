<%@ page import="org.fhcrc.cpas.flow.util.PFUtil"%>
<%@ page import="Flow.FlowController"%>
<%@ page import="org.labkey.api.data.ContainerManager"%>
<%@ page import="Flow.FlowAdminForm"%>
<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>

<% FlowAdminForm form = (FlowAdminForm) __form; %>
<cpas:errors />
<form method="POST" action="<%=h(PFUtil.urlFor(FlowController.Action.flowAdmin, ContainerManager.getRoot()))%>">
    <p>
        Which directory should the flow module use to do work in?  By default, it will use the system temporary directory.<br>
        <input type="text" name="ff_workingDirectory" value="<%=h(form.ff_workingDirectory)%>">
    </p>
    <cpas:button text="update" />
    <cpas:button text="cancel" href="<%=new ViewURLHelper("admin", "begin", "")%>" />
</form>