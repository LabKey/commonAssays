<%
/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.protein.go.GoLoader" %>
<%@ page import="org.labkey.protein.AnnotController" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    boolean loaded = GoLoader.isGoLoaded().booleanValue();
%>
<table><tr><td>
    This is a deprecated feature. It will be completely removed in a future version.<br><br>

You are about to <%=h(loaded ? "reload" : "load")%> the January 2017 Gene Ontology (GO) annotation files into your
LabKey database. If you click "Continue", this server will automatically:

<ul>
    <li>Download the GO annotation files from geneontology.org</li><%

    if (loaded)
    {

    %>
    <li>Clear all existing GO annotation data from your database tables</li><%

    }

    %>
    <li><%=h(loaded ? "Reload" : "Load")%> the database tables with information from the downloaded GO files</li>
</ul>

Your LabKey Server must be able to connect via the Internet to geneontology.org. This process should take less than
    a minute to complete. The loading will take place in the background and you can continue to use your server
    normally. You can monitor the process by refreshing the status information on the next page.<br><br>

If you wish to proceed, click the "Continue" button. Otherwise, click "Cancel".<br><br>
<labkey:form action="<%=urlFor(AnnotController.LoadGoAction.class)%>" method="post">
    <%= button("Continue").submit(true) %>
    <%= button("Cancel").href(AnnotController.getShowProteinAdminUrl()) %>
</labkey:form>
</td></tr></table>