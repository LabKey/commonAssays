<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page import="org.labkey.flow.controllers.NewFolderForm" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<% NewFolderForm form = (NewFolderForm) __form; %>
<labkey:errors/>
<form method="POST" action="<%=PFUtil.urlFor(FlowController.Action.newFolder, getContainer())%>">
    <p>A new folder will be created that is a sibling of this one.
        What do you want to call the new folder?<br>
        <input type="text" name="ff_folderName" value="<%=h(form.ff_folderName)%>">
    </p>

    <p>
        You can choose to copy some items from this folder into the new one.
    </p>
    <% FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        if (protocol != null)
        {
            String description = protocol.getProtocolSettingsDescription();
            if (description != null)
            {
    %>
    <p>
        <input type="checkbox" name="ff_copyProtocol"
               value="true"<%= form.ff_copyProtocol ? " checked" : ""%>> <%=h(description)%>
    </p>

    <% }
    }%>

    <p>
        Analysis Scripts:<br>

        <% FlowScript[] scripts = FlowScript.getScripts(getContainer());
            if (scripts.length != 0) for (FlowScript script : scripts)
            { %>
        <input type="checkbox" name="ff_copyAnalysisScript"
               value="<%=h(script.getName())%>"<%=form.ff_copyAnalysisScript.contains(script.getName()) ? " checked" : ""%>> <%=h(script.getName())%>
        <br>
        <%
            }
        else
        { %>
        There are no analysis scripts in this folder.
        <% } %>
    </p>
    <labkey:button text="Create Folder"/>
</form>