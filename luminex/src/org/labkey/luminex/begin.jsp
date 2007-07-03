<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexRun" %>

<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<%
    JspView<List<LuminexRun>> me = (JspView<List<LuminexRun>>) HttpView.currentView();
    List<LuminexRun> runs = me.getModelBean();
%>
<br/>
<table cellpadding="2" cellspacing="0" style="border-bottom: black solid 1px; border-left: black solid 1px;">
    <tr>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong>File Name</strong></td>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong>Lab</strong></td>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong>Uploaded On</strong></td>
        <td style="border-top: black solid 1px; border-right: black solid 1px;"><strong>Uploaded By</strong></td>
        <td style="border-top: black solid 1px; border-right: black solid 1px;">&nbsp;</td>
    </tr>
    <% if (runs.isEmpty()) { %><tr><td style="border-top: black solid 1px; border-right: black solid 1px;" colspan="5"><em>No data to show.</em></td></tr><% } %> 
    <% for (LuminexRun run : runs) { %>
        <tr>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><a href="showRun.view?fileName=<%= run.getFileName() %>"><%= run.getFileName() %></a></td>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><%= run.getLab() %></td>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><%= run.getCreatedOn() %></td>
            <td style="border-top: black solid 1px; border-right: black solid 1px;"><%= run.getCreatedBy() %></td>
            <td style="border-top: black solid 1px; border-right: black solid 1px;">[<a href="publish.view?fileName=<%= run.getFileName() %>">publish to study</a>]</td>
        </tr>
    <% } %>
</table>
<br/>
<br/>
<a href="upload.view"><labkey:button text="Upload new run" /></a>
