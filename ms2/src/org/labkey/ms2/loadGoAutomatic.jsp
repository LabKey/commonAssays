<%
/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
<%@ page import="org.apache.commons.lang3.SystemUtils" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.GoLoader" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    boolean loaded = GoLoader.isGoLoaded().booleanValue();
%>
<table><tr><td>
You are about to <%=h(loaded ? "reload" : "load")%> the latest Gene Ontology (GO) annotation files into your
LabKey database.  If you click "Continue" your LabKey Server will automatically:

<ul>
    <li>Download the latest GO annotation files from ftp.geneontology.org</li><%

    if (loaded)
    {

    %>
    <li>Clear all existing GO annotation data from your database tables</li><%

    }

    %>
    <li><%=h(loaded ? "Reload" : "Load")%> the database tables with information from the downloaded GO files</li>
</ul>

Your LabKey Server must be able to connect via the Internet to ftp.geneontology.org. Assuming a reasonably fast
Internet connection, this process should take less than five minutes to complete. The loading will take place
in the background and you can continue to use your LabKey Server normally. If you want, you can monitor the
process by refreshing the status information on the next page.<br><br>

<%if (SystemUtils.IS_OS_WINDOWS) { %>
<span class="labkey-error">Warning: Since this server is running Windows it may encounter firewall issues with FTP downloads.
If the server's firewall has not been properly configured then the automatic GO download may fail, in which case you should
switch to the "Manual" tab and follow the instructions there.</span> For more information,
<%=helpLink("annotations#go", "visit the documentation")%>.<br><br>
<% } %>

If you wish to proceed, click the "Continue" button. Otherwise click "Cancel".<br><br>
<labkey:form action="<%=h(buildURL(MS2Controller.LoadGoAction.class))%>" method="post">
    <%= button("Continue").submit(true) %>
    <%= button("Cancel").href(MS2Controller.MS2UrlsImpl.get().getShowProteinAdminUrl()) %>
</labkey:form>
</td></tr></table>