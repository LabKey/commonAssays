<%@ page extends="announcements.EmailResponsePage" %>
<html>
<head>
<link href="<%=cssURL%>" rel="stylesheet" type="text/css">
</head>

<body>
<table width="100%" border="0" cellspacing="0" cellpadding="4">
    <tr><td class="ms-vb" colspan="2" style="background-color: #dddddd">
    <%=responseAnnouncement.getCreatedByName() + (responseAnnouncement.getParent() != null ? " responded" : " created a new " + settings.getConversationName().toLowerCase()) %></td>
    <td class="ms-vb" align="right" style="background-color: #dddddd"><%=formatDateTime(responseAnnouncement.getCreated())%></td></tr><%

    if (null != responseBody)
    {  %>
    <tr><td colspan="3" class="ms-vb"><%=responseBody%></td></tr>
    <tr><td colspan="3" class="ms-vb">&nbsp;</td></tr><%
    }  %>
    <tr><td colspan="3" class="ms-vb"><a href="<%=threadURL%>">View this <%=settings.getConversationName().toLowerCase()%></a></td></tr>
</table>

    <hr size="1">

<table width="100%" border="0" cellspacing="0" cellpadding="4">
    <tr><td class="ms-vb">You have received this email because <%
        switch(reason)
        {
            case broadcast:
    %>a site administrator sent this notification to all users of <a href="<%=siteURL%>"><%=siteURL%></a>.<%
            break;

            case signedUp:
    %>you are signed up to receive notifications about new posts to <a href="<%=boardURL%>"><%=boardPath%></a> at <a href="<%=siteURL%>"><%=siteURL%></a>.
  If you no longer wish to receive these notifications, please <a href="<%=removeUrl%>">change your email preferences</a>.<%
            break;

            case userList:
    %>you are on the Members list for this <%=settings.getConversationName().toLowerCase()%>.  If you no longer wish to receive these notifications,
  please <a href="<%=removeUrl%>">click here to remove yourself</a> from this <%=settings.getConversationName().toLowerCase()%>.<%
            break;
        }
    %></td></tr>
</table>    
</body>
</html>
