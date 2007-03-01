<%@ page import="org.labkey.announcements.AnnouncementsController" %><%@ page import="org.labkey.api.announcements.Announcement" %><%@ page import="org.labkey.api.util.DateUtil" %><%@ page extends="org.labkey.announcements.DailyDigestPage" %>The following new posts were made yesterday in folder: <%=c.getPath()%>

<%
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
                %>
View this <%=conversationName%> at this URL: <%=threadUrl%>

<%
            }

            threadUrl = AnnouncementsController.getThreadUrl(request, c, previousThread, String.valueOf(ann.getRowId())).getURIString();
            %><%=ann.getTitle()%>

<%
        }
%><%=ann.getCreatedByName()%><% if (null == ann.getParent()) { %> created this <%=conversationName%><% } else { %> responded <% } %> at <%=DateUtil.formatDateTime(ann.getCreated())%>
<%
        if (!settings.isSecure())
        {
%><%=ann.getBody()%>
<%
        }
    }

    if (null != threadUrl)
    {
        %>
View this <%=conversationName%> at this URL: <%=threadUrl%>

<% } %>


You have received this email because you are signed up for a daily digest about new posts to <%=boardPath%> at <%=siteUrl%>.
If you no longer wish to receive these notifications, please change your email preferences here: <%=removeUrl%>