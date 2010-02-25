<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%
    HttpView<String> me = (HttpView<String>) HttpView.currentView();
    String url = me.getModelBean();
%>
<form method="POST">
    <table>
        <tr>
            <td class="labkey-form-label">Base&nbsp;URL<%= PageFlowUtil.helpPopup("Base URL", "The sequence will be appended to the end of the URL to create links to the BLAST server")%></td>
            <td><input size="100" type="text" name="blastServerBaseURL" value="<%= PageFlowUtil.filter(url) %>"/></td>
            <td><%= PageFlowUtil.generateSubmitButton("Save")%></td>
        </tr>
    </table>
</form>
