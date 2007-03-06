<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.announcements.model.AnnouncementManager" %>
<%@ page extends="org.labkey.announcements.EmailPreferencesPage" %>
<%=message == null ? "" : message%>
<form action="updateEmailPreferences.post" method="post">
    <br>Send email notifications for these <%=conversationName%>s<br>
    <input type="radio" value="<%=AnnouncementManager.EMAIL_PREFERENCE_NONE%>" name="emailPreference" <%=emailPreference == AnnouncementManager.EMAIL_PREFERENCE_NONE ? " checked" : ""%>>
    <b>None</b> - Don't send me any email for this message board<br>
    <input type="radio" value="<%=AnnouncementManager.EMAIL_PREFERENCE_MINE%>" name="emailPreference" <%=emailPreference == AnnouncementManager.EMAIL_PREFERENCE_MINE ? " checked" : ""%>>
    <b>Mine</b> - Send me email for posts to my <%=conversationName%>s (I've posted to the <%=conversationName%><% if (hasMemberList) { %> or I'm on its member list<% } %>)<br>
    <input type="radio" value="<%=AnnouncementManager.EMAIL_PREFERENCE_ALL%>" name="emailPreference" <%=emailPreference == AnnouncementManager.EMAIL_PREFERENCE_ALL ? " checked" : ""%>>
    <b>All</b> - Send me email for all posts<br>

    <br>Notification type<br>
    <input type="radio" value="0" name="notificationType" <%= notificationType == 0 ? " checked" : ""%>>
    <b>Individual</b> - send a separate email after each post<br>
    <input type="radio" value="<%=AnnouncementManager.EMAIL_NOTIFICATION_TYPE_DIGEST%>" name="notificationType" <%=notificationType == AnnouncementManager.EMAIL_NOTIFICATION_TYPE_DIGEST ? " checked" : "" %>>
    <b>Daily Digest</b>-send one email each day that summarizes all posts<br>

    <br><input type=hidden name="srcUrl"value="<%=PageFlowUtil.filter(srcURL)%>"/>
    <input type=image src="<%=PageFlowUtil.buttonSrc("Update")%>"/>
    <a href="<%=srcURL%>"><img src="<%=PageFlowUtil.buttonSrc("Return to Messages")%>" border=0/></a>
</form>