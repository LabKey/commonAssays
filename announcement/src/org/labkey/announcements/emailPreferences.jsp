<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.announcements.EmailPreferencesPage" %>
<%=message == null ? "" : message%>
<form action="updateEmailPreferences.post" method="post">
    <input type="radio" value="0" name="emailPreference" <%=emailPreference == 0 ? " checked" : ""%>>
    Don't send me any email for this message board<br>
    <input type="radio" value="2" name="emailPreference" <%=emailPreference == 2 ? " checked" : ""%>>
    Send me responses for threads I've posted to<br>
    <input type="radio" value="1" name="emailPreference" <%=emailPreference == 1 ? " checked" : ""%>>
    Send me email for all posts<br>
    <br>
    <input type=hidden name="srcUrl" value="<%=PageFlowUtil.filter(srcURL)%>"/>
    <input type=image src="<%=PageFlowUtil.buttonSrc("Update")%>"/>
    <a href="<%=srcURL%>"><img src="<%=PageFlowUtil.buttonSrc("Return to Messages")%>" border=0/></a>
</form>