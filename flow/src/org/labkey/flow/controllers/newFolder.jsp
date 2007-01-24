<%@ page import="org.labkey.flow.controllers.FlowController"%>
<%@ page import="org.labkey.flow.util.PFUtil"%>
<%@ page import="org.labkey.flow.controllers.NewFolderForm" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<% NewFolderForm form = (NewFolderForm) __form; %>
<cpas:errors />
<form method="POST" action="<%=PFUtil.urlFor(FlowController.Action.newFolder, getContainer())%>">
<p>A new folder will be created that is a sibling of this one.
    What do you want to call the new folder?<br>
    <input type="text" name="ff_folderName" value="<%=h(form.ff_folderName)%>">
</p>

<% FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
    if (protocol != null) {

    %>
<p>
    Which items do you want to copy into the new folder?<br>
    <input type="checkbox" name="ff_copyProtocol" value="true"<%= form.ff_copyProtocol ? " checked" : ""%>> Protocol settings <br>
    <% FlowScript[] scripts = FlowScript.getScripts(getContainer());
    if (scripts.length != 0) {
    %>
    Analysis Scripts:<br>
    <% for (FlowScript script : scripts) { %>
    <input type="checkbox" name="ff_copyAnalysisScript" value="<%=h(script.getName())%>"<%=form.ff_copyAnalysisScript.contains(script.getName()) ? " checked" : ""%>> <%=h(script.getName())%><br>
    <% }
    }%>
</p>
<%  }%>
    <cpas:button text = "Create Folder" />
</form>