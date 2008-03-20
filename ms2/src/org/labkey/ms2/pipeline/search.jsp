<%@ page import="org.labkey.api.exp.api.ExpRun"%>
<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.api.util.HelpTopic"%>
<%@ page import="org.labkey.api.view.ThemeFont"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.ms2.pipeline.FileStatus" %>
<%@ page import="org.labkey.ms2.pipeline.MS2SearchForm" %>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.ms2.pipeline.SearchPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%
MS2SearchForm form = getForm();
PipelineUrls up = urlProvider(PipelineUrls.class);

boolean hasWork = false;
%>

<labkey:errors />

<span style="font-size:<%=ThemeFont.getThemeFont().getHeader_1Size()%>">An MS2 search protocol is defined by a set of options for the search engine and a protein sequence file to search.</span>
<br><br>
<%
    if ("".equals(form.getProtocol()) && getSequenceDBs().size () == 0)
    {
        if ("mascot".equalsIgnoreCase(form.getSearchEngine()))
        {
            out.print ("In order to perform a search using Mascot, LabKey Server must be able to contact your Mascot server and at least one protein database must have already been uploaded to it.<br>");
        }
        else if("sequest".equalsIgnoreCase(form.getSearchEngine()))
        {
            out.print ("In order to perform a search using Sequest, LabKey Server must be able to contact your Sequest server and at least one protein database must have already been uploaded to it.<br>");
        }
        else
        {
            out.print ("In order to perform a search, you must first upload a protein database.<br>");
            %><labkey:button text="Upload Database" href="<%=urlFor(PipelineController.AddSequenceDBAction.class)%>"/><%
        }
    }
    else
    {
        if(getProtocolNames().length > 0)
            out.print("Choose an existing protocol or define a new one.<br>");
        else
            out.print("Define a new protocol using the following fields and run analysis.<br>");
%>
    <br>

<form id="search_form" method="post" action="<%=urlFor(PipelineController.SearchAction.class)%>">
    <input type="hidden" name="runSearch" value="true">
    <input type="hidden" name="path" value="<%=h(form.getPath())%>">
    <input type="hidden" name="searchEngine" value="<%=h(form.getSearchEngine())%>">

<table border="0">
    <tr><td class='ms-searchform'>Analysis Protocol:</td>
        <td class='ms-vb'><select name="protocol"
                            onchange="changeProtocol(this)">
            <option>&lt;New Protocol&gt;</option>
<%
    for (String protocol : getProtocolNames())
    {
        if (protocol.equals(form.getProtocol()))
            out.print("<option selected>");
        else
            out.print("<option>");
        out.print(h(protocol));
        out.print("</option>");
    }
%>
        </select></td></tr>

<%  if ("".equals(form.getProtocol()))
    { %>
    <tr><td class='ms-searchform'>Protocol Name:</td>
        <td class='ms-vb'><input type="text" name="protocolName" size="40" value="<%=h(form.getProtocolName())%>"></td></tr>
    <tr><td class='ms-searchform'>Protocol Description:</td>
        <td class='ms-vb'><textarea style="width: 100%;" name="protocolDescription" cols="150" rows="4"><%=h(form.getProtocolDescription())%></textarea></td></tr>
<%  } %>

<%  if (getMzXmlFileStatus().size() != 1)
    { %>
    <tr><td class='ms-searchform'>Analyze Files:</td>
<%  }
    else
    { %>
    <tr><td class='ms-searchform'>Analyze File:</td>
<%  } %>
        <td class='ms-vb'>
<%
    if (getMzXmlFileStatus().size() == 0)
        out.print("No files found");
    else
    { %>
        <table border="0" cellpadding="0" cellspacing="3">
<%
        for (Map.Entry<File, FileStatus> entry : getMzXmlFileStatus().entrySet())
        {
            FileStatus status = entry.getValue();
            out.print("<tr><td class='ms-vb'>");
            out.print(h(entry.getKey().getName()));
            out.print("</td><td class='ms-vb'>");
            if (status == FileStatus.ANNOTATED || status == FileStatus.UNKNOWN)
            {
                hasWork = true;
                out.print("&nbsp;");
            }
            else
            {
                out.print("<b>(");
                out.print(status);
                out.print(")</b>");
            }
            out.print("</td></tr>\n");
        } %>
        </table><%
    }
%>
        </td>
    </tr>
    <tr><td class='ms-searchform'>Search Engine:</td>
        <td class='ms-vb'><table border="0" cellpadding="0" cellspacing="3">
<tr><td class='ms-vb'><%=h(form.getSearchEngine())%></td></tr>
        </table>
        </td>
    </tr>
    <tr><td class='ms-searchform'>Sequence Databases:</td>
        <td class='ms-vb'>
<%  if ("".equals(form.getProtocol()))
    {
        String dbPath = "";
        if (getSequenceDBs().size() > 1)
        {
            out.print("<select name=\"sequenceDBPath\" onchange=\"changeDBPath(this)\">\n");
            for (String path : getSequenceDBs().keySet())
            {
                out.print("<option");
                if (path.equals(form.getSequenceDBPath()))
                {
                    dbPath = path;
                    out.print(" selected");
                }
                out.print(">");
                out.print(h(path));
                out.print("</option>\n");
            }
            out.print("</select><br>\n");
        }
        out.print("<select id=\"sequenceDBSel\" name=\"sequenceDB\" size=\"6\">\n");
        for (String db : getSequenceDBs().get(dbPath))
        {
            out.print("<option");
            if (db.equals(form.getSequenceDB()))
                out.print(" selected");
            out.print(">");
            out.print(h(db));
            out.print("</option>\n");
        }
        out.print("</select>\n");
    }
    else
    {
        out.print(h(form.getSequenceDB()));
        out.print("<br>\n");
    }
%>
        </td>
    </tr>
    <tr><td class='ms-searchform'><%=form.getSearchEngine()%> XML:</td>
        <td class='ms-vb'>
<%  if ("".equals(form.getProtocol()))
    { %>
            <textarea style="width: 100%" name="configureXml" cols="150" rows="20"><%=form.getConfigureXml()%></textarea><br>
<%  }
    else
    { %>
<pre>
<%=h(form.getConfigureXml())%>
</pre>
<%  } %>
            For detailed explanations of all available input parameters, see the
<%
    if("mascot".equalsIgnoreCase(form.getSearchEngine()))
    {
        %><a href="<%=(new HelpTopic("pipelineMascot", HelpTopic.Area.CPAS)).getHelpTopicLink()%>" target="_api">Mascot API Documentation</a> <%
    }
    else if("sequest".equalsIgnoreCase(form.getSearchEngine()))
    {
        %><a href="http://fields.scripps.edu/sequest/index.html">Sequest Documentation</a><%
    }
    else
    {
        %><a href="<%=(new HelpTopic("pipelineXTandem", HelpTopic.Area.CPAS)).getHelpTopicLink()%>" target="_api">X!Tandem API Documentation</a> or <a href="http://www.thegpm.org/TANDEM/api/index.html" target="_api">X!Tandem site</a><%
    }
%>
 on-line.</td></tr>
<%
    if ("".equals(form.getProtocol()))
    {
        %><tr><td></td><td class='ms-vb'><input type="checkbox" name="saveProtocol" <% if (form.isSaveProtocol()) { %>checked<% } %>/> Save protocol for future use</td></tr><%
    }
    if (hasWork)
    {
        %><tr><td colspan="2"><labkey:button text="Search"/>&nbsp;<labkey:button text="Cancel" href="<%=up.urlReferer(getContainer())%>"/></td></tr><%
    }
%>
</table>
</form>
<script>
    function changeProtocol(sel)
    {
        document.getElementsByName("runSearch")[0].value = false;
        document.getElementById("search_form").submit();
    }
    <% if (form.getProtocol() == "" && getSequenceDBs().size() > 1)
    {
        out.print("var fastaFiles = new Object();\n");
        for (String path : getSequenceDBs().keySet())
        {
            out.print("fastaFiles['");
            out.print(h(path));
            out.print("'] = [");
            for (String file : getSequenceDBs().get(path))
            {
                out.print("'");
                out.print(h(file));
                out.print("',");
            }
            out.print("];\n");
        }
    %>
    function changeDBPath(sel)
    {
        var fastaSel = document.getElementById("sequenceDBSel");
        fastaSel.options.length = 0;
        var files = fastaFiles[sel.options[sel.selectedIndex].text];
        for (var i = 0; i < files.length; i++)
        {
            if (files[i] == null || files[i].length == 0)
                continue;
            fastaSel.options[fastaSel.options.length] = new Option(files[i]);
        }
    }
<%  } %>
</script>
<script for=window event=onload>
    try {document.getElementByName("protocol").focus();} catch(x){}
</script><%
}%>
