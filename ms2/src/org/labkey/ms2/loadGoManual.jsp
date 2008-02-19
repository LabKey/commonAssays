<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.GoLoader" %>
<%
    boolean loaded = GoLoader.isGoLoaded().booleanValue();
%>
<script type="text/javascript" language="javascript">
    LABKEY.requiresScript('util.js');
</script>
<br>
<table><tr><td>
You are about to <%=loaded ? "reload" : "load"%> Gene Ontology (GO) annotation files from your
computer into your LabKey database manually.  Use this method if the automatic FTP method doesn't work for
you (e.g., your LabKey server can't connect to ftp.geneontology.org) or you need to upload a specific
version of the GO annotations.<br><br>

Click the "Browse..." button to choose a file to upload.  The file must be a GZIP compressed archive
that includes all five GO annotation files (graph_path.txt, term.txt, term_definition.txt, term_synonym.txt,
and term2term.txt) in the proper format.  See ftp.geneontology.org for examples of these files.<br><br>

After choosing a file, click "Continue" and your LabKey Server will:

<ul>
    <li>Load GO annotation files from the file you choose</li><%

    if (loaded)
    {

    %>
    <li>Clear all existing GO annotation data from your database tables</li><%

    }

    %>
    <li><%=loaded ? "Reload" : "Load"%> the database tables with information from the specified GO file</li>
</ul>

The loading will take place in the background and you can continue to use your LabKey Server normally. If you want,
you can monitor the process by refreshing the status information on the next page.<br><br>

Click "Cancel" to return to the Protein Databases Admin page.<br><br><br>
<form action="loadGo.post?manual=1" enctype="multipart/form-data" method="post">
    <input type="file" name="gofile" size="60" onChange="showPathname(this, 'filename')">&nbsp;<label class="normal" id="filename"></label><br><br>
    <input type=image src="<%=PageFlowUtil.buttonSrc("Continue")%>">
    <%=PageFlowUtil.buttonLink("Cancel", MS2Controller.MS2UrlsImpl.get().getShowProteinAdminUrl())%>
</form>
</td></tr></table>
