<%@ page import="org.labkey.flow.controllers.FlowController"%>
<%@ page import="org.labkey.flow.util.PFUtil"%>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<% FlowController.NewFolderForm form = (FlowController.NewFolderForm) __form; %>

<form method="POST" action="<%=PFUtil.urlFor(FlowController.Action.newFolder, getContainer())%>">
<p>What do you want to call the new folder?<br>
    <input type="text" name="ff_folderName" value="<%=h(form.ff_folderName)%>">
</p>
    <cpas:button text="Create Folder" />
</form>