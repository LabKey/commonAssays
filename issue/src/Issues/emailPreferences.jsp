<%@ page import="Issues.model.IssueManager" %>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.ViewContext"%>
<%@ page extends="Issues.EmailPreferencesPage" %>

<%
    ViewContext context = HttpView.currentContext();
    int emailPrefs = IssueManager.getUserEmailPreferences(context.getContainer(), context.getUser().getUserId());
%>

<%=_message == null ? "" : "<b>" + _message + "</b><p/>"%>

<form action="updateEmailPrefs.post" method="post">
    <input type="checkbox" value="1" name="emailPreference" <%=(emailPrefs & IssueManager.NOTIFY_ASSIGNEDTO_OPEN) != 0 ? " checked" : ""%>>
    Send me email when an issue is opened and assigned to me<br>
    <input type="checkbox" value="2" name="emailPreference" <%=(emailPrefs & IssueManager.NOTIFY_ASSIGNEDTO_UPDATE) != 0 ? " checked" : ""%>>
    Send me email when an issue that's assigned to me is modified<br>
    <input type="checkbox" value="4" name="emailPreference" <%=(emailPrefs & IssueManager.NOTIFY_CREATED_UPDATE) != 0 ? " checked" : ""%>>
    Send me email when an issue I opened is modified<br>
    <hr/>
    <input type="checkbox" value="8" name="emailPreference" <%=(emailPrefs & IssueManager.NOTIFY_SELF_SPAM) != 0 ? " checked" : ""%>>
    Send me email notifications when I enter/edit an issue<br>
    <br>
    <input type=image src="<%=PageFlowUtil.buttonSrc("Update")%>"/>
    <a href="list.view?.lastFilter=true"><img border=0 src="<%=PageFlowUtil.buttonSrc("Back to Issues")%>"></a>
</form>