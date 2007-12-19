<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms2.OldMS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.GoLoader" %>
<%
    boolean loaded = GoLoader.isGoLoaded().booleanValue();
%>
You are about to <%=loaded ? "reload" : "load"%> the latest Gene Ontology (GO) annotation files into your
LabKey database.  If you click Continue your LabKey Server will automatically:

<ul>
    <li>Download the latest GO annotation files from ftp.geneontology.org</li><%

    if (loaded)
    {

    %>
    <li>Clear all existing GO annotation data from your database tables</li><%

    }

    %>
    <li><%=loaded ? "Reload" : "Load"%> the database tables with information from the downloaded GO files</li>
</ul>

Your LabKey Server must be able to connect via the Internet to ftp.geneontology.org. Assuming a reasonably fast
Internet connection, this process should take less than five minutes to complete. The loading will take place
in the background and you can continue to use your LabKey Server normally. If you want, you can monitor the
process by refreshing the status information on the next page.<br><br>

If you wish to proceed, click the Continue button. Otherwise click Cancel.<br><br>
<form action="loadGo.post" method="post">
    <input type=image src="<%=PageFlowUtil.buttonSrc("Continue")%>">
    <%=PageFlowUtil.buttonLink("Cancel", OldMS2Controller.getShowProteinAdminUrl())%>
</form>
