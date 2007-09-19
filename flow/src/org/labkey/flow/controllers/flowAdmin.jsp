<%@ page import="org.labkey.flow.controllers.FlowController"%>
<%@ page import="org.labkey.api.data.ContainerManager"%>
<%@ page import="org.labkey.flow.controllers.FlowAdminForm"%>
<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<% FlowAdminForm form = (FlowAdminForm) __form; %>
<labkey:errors />
<form method="POST" action="<%=h(org.labkey.api.util.PageFlowUtil.urlFor(FlowController.Action.flowAdmin, ContainerManager.getRoot()))%>">
    <p>
        Which directory should the flow module use to do work in?  By default, it will use the system temporary directory.<br>
        <input type="text" name="ff_workingDirectory" value="<%=h(form.ff_workingDirectory)%>">
    </p>
    <labkey:button text="update" />
    <labkey:button text="cancel" href="<%=new ViewURLHelper("admin", "begin", "")%>" />
</form>