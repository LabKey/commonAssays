<%@ page import="Issues.IssuesController"%>
<%@ page import="org.labkey.api.data.ColumnInfo"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="Issues.model.IssueManager" %>
<%@ page import="java.sql.SQLException" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<IssuesController.IssuesPreference> me = (JspView<IssuesController.IssuesPreference>) HttpView.currentView();
    IssuesController.IssuesPreference bean = me.getModel();
%>
<form action="updateRequiredFields.view" method="post" name="requiredFieldsForm">
    <table class="normal">
        <tr><td class=ms-vb colspan=2 align=center><div class="ms-searchform"><b>Required Fields for Issues</b></div></td></tr>
        <tr><td class=ms-vb colspan=2>Select fields to be required when entering or updating an issue:</td></tr>
    <%
        for (ColumnInfo info : bean.getColumns())
        {
    %>
        <tr><td><input type="checkbox" name="requiredFields" <%=isRequired(info.getName(), bean.getRequiredFields()) ? "checked " : ""%> value="<%=info.getName()%>"><%=getCaption(info)%></td></tr>
    <%
        }
    %>
        <tr><td></td></tr>
        <tr>
            <td><input type="image" src="<%=PageFlowUtil.buttonSrc("Update")%>"></td>
        </tr>
    </table><br>
</form>

<%!
    public boolean isRequired(String name, String requiredFields) {
        if (requiredFields != null) {
            return requiredFields.indexOf(name.toLowerCase()) != -1;
        }
        return false;
    }

    public String getCaption(ColumnInfo col) throws SQLException
    {
        final IssueManager.CustomColumnConfiguration ccc = IssueManager.getCustomColumnConfiguration(HttpView.getRootContext().getContainer());
        if (ccc.getColumnCaptions().containsKey(col.getName()))
        {
            return ccc.getColumnCaptions().get(col.getName());
        }
        return col.getCaption();
    }
%>