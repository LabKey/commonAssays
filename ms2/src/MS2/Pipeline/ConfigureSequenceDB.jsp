<%@ page extends="MS2.Pipeline.ConfigureSequenceDB" %>
<form method="POST" name="updateClusterSequenceDB" action="updateClusterSequenceDB.post">
<% if (getError() != null)
    {
%>
    <font class="cpas-error"><%= getError() %></font><br>
<%
    }
%>
    <table class="normal">
        <tr>
            <td>Local Path to Sequence Database Files:</td>
        </tr>
        <tr>
            <td><input type="text" name="localPathRoot" size="40" value="<%= h(getLocalPathRoot()) %>"></td>
        </tr>
        <tr>
            <td><input type="checkbox" name="allowUpload" <%= isAllowUpload() ? "checked" : "" %>> Allow Upload</td>
        </tr>
        <tr>
            <td><%= buttonImg("Done")%></td>
        </tr>
    </table>
</form>
