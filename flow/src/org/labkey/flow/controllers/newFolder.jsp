<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.flow.controllers.FlowController" %>
<%@ page import="org.labkey.flow.controllers.NewFolderForm" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<% NewFolderForm form = (NewFolderForm) __form; %>
<labkey:errors/>
<form method="POST" action="<%=org.labkey.api.util.PageFlowUtil.urlFor(FlowController.Action.newFolder, getContainer())%>">
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