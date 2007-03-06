<%@ page extends="org.labkey.announcements.EmailResponsePage" %>

<%=responseAnnouncement.getCreatedByName() + (responseAnnouncement.getParent() != null ? " responded" : " created a new " + settings.getConversationName().toLowerCase()) %> <%=formatDateTime(responseAnnouncement.getCreated())%>
<%

if (null != responseBody)
{  %>
    <%=responseBody%><%
}  %>
View this <%=settings.getConversationName().toLowerCase()%> here: <%=threadURL%>



You have received this email because <%
    switch(reason)
    {
        case broadcast:
%>a site administrator sent this notification to all users of <%=siteURL%>.<%
        break;

        case signedUp:
%>you are signed up to receive notifications about new posts to <%=boardPath%> at <%=siteURL%>.
If you no longer wish to receive these notifications you can change your email preferences by
navigating here: <%=removeUrl%>.<%
        break;

        case memberList:
%>you are on the member list for this <%=settings.getConversationName().toLowerCase()%>.
If you no longer wish to receive these notifications you can remove yourself from
the member list by navigating here: <%=removeUrl%><%
        break;
    }
%>
