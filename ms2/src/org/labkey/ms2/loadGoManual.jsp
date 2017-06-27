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
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.GoLoader" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    boolean loaded = GoLoader.isGoLoaded().booleanValue();
    ActionURL loadGo = urlFor(MS2Controller.LoadGoAction.class).addParameter("manual", 1);
%>
<table><tr><td>
You are about to <%=text(loaded ? "reload" : "load")%> Gene Ontology (GO) annotation files from your computer into your
LabKey database. Use this method if the automatic FTP method doesn't work for you (e.g., your LabKey server can't
connect to ftp.geneontology.org) or you need to upload a specific version of the GO annotations.<br><br>

Click the "Browse..." or "Choose File..." button below to choose a file to upload. The file must be a GZIP compressed
archive that includes all five GO annotation files (graph_path.txt, term.txt, term_definition.txt, term_synonym.txt,
and term2term.txt) in the proper format. See <a href="ftp://ftp.geneontology.org/godatabase/archive/latest-full" target="go">
ftp://ftp.geneontology.org/godatabase/archive/latest-full</a> for examples of these files. The GO archive to use is an
approximately 12MB file named go_monthly-termdb-tables.tar.gz.<br><br>

After choosing a file, click "Continue" and your LabKey Server will:

<ul>
    <li>Load GO annotation files from the file you choose</li><%

    if (loaded)
    {

    %>
    <li>Clear all existing GO annotation data from your database tables</li><%

    }

    %>
    <li><%=text(loaded ? "Reload" : "Load")%> the database tables with information from the specified GO file</li>
</ul>

Loading will take place in the background and you can continue to use your LabKey Server normally. If you want,
you can monitor the progress by refreshing the status information on the next page.<br><br>

Click "Cancel" to return to the Protein Databases Admin page.<br><br>
<labkey:form action="<%=h(loadGo)%>" enctype="multipart/form-data" method="post">
    <input type="file" name="gofile" size="60" onChange="showPathname(this, 'filename')">&nbsp;<label id="filename"></label><br><br>
    <%= button("Continue").submit(true) %>
    <%= button("Cancel").href(MS2Controller.MS2UrlsImpl.get().getShowProteinAdminUrl()) %>
</labkey:form>
</td></tr></table>
