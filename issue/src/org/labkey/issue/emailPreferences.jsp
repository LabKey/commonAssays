<%@ page import="org.labkey.issue.model.IssueManager" %>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.springframework.validation.BindException" %>
<%@ page import="org.springframework.validation.ObjectError" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    int emailPrefs = IssueManager.getUserEmailPreferences(context.getContainer(), context.getUser().getUserId());
    BindException errors = (BindException) request.getAttribute("errors");
    String message = (String)request.getAttribute("message");

    if (message != null)
    {
        %><b><%=h(message)%></b><p/><%
    }

    if (null != errors && errors.getErrorCount() > 0)
    {
        for (ObjectError e : (List<ObjectError>) errors.getAllErrors())
        {
            %><span color=red><%=h(context.getMessage(e))%></span><br><%
        }
    }
%>
<form action="emailPrefs.post" method="post">
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