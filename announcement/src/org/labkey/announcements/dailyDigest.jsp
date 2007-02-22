<%@ page import="org.labkey.announcements.AnnouncementsController" %>
<%@ page import="org.labkey.api.announcements.Announcement" %>
<%@ page import="org.labkey.api.util.DateUtil" %>
<%@ page extends="org.labkey.announcements.DailyDigestPage" %>
<html>
<head>
<link href="<%=cssURL%>" rel="stylesheet" type="text/css">
</head>

<body>
<table width="100%" border="0" cellspacing="0" cellpadding="4">
    <tr><td class="ms-vb"><b>The following new posts were made yesterday in folder: <%=h(c.getPath())%></b></td></tr><%

    String previousThread = null;
    String threadUrl = null;

    for (Announcement ann : announcements)
    {
        if (null == ann.getParent() || !ann.getParent().equals(previousThread))
        {
            if (null == ann.getParent())
                previousThread = ann.getEntityId();
            else
                previousThread = ann.getParent();

            if (null != threadUrl)
            {
                %><tr><td><a href="<%=threadUrl%>">Click here to view this <%=conversationName%></a></td></tr><%
            }

            threadUrl = h(AnnouncementsController.getThreadUrl(request, c, previousThread, String.valueOf(ann.getRowId())).getURIString());%>
            <tr><td>&nbsp;</td></tr><tr><td class="ms-vb" colspan="2" style="background-color: #dddddd"><%=ann.getTitle()%></td></tr><%
        } %>
            <tr><td><%=ann.getCreatedByName()%><% if (null == ann.getParent()) { %> created this <%=conversationName%><% } else { %> responded <% } %> at <%=DateUtil.formatDateTime(ann.getCreated())%></td></tr><%

        if (!settings.isSecure())
        { %>
            <tr><td style="padding-left:35px;"><%=ann.getBody()%></td></tr><%
        }
    }

    if (null != threadUrl)
    {
        %><tr><td><a href="<%=threadUrl%>">Click here to view this <%=conversationName%></a></td></tr><%
    }

    %>
</table>

<hr size="1">

<table width="100%" border="0" cellspacing="0" cellpadding="4">
    <tr><td class="ms-vb">You have received this email because you are signed up for a daily digest about new posts to <a href="<%=boardUrl%>"><%=boardPath%></a> at <a href="<%=siteUrl%>"><%=siteUrl%></a>.
  If you no longer wish to receive these notifications, please <a href="<%=removeUrl%>">change your email preferences</a>.</td></tr>
</table>    
</body>
</html>
