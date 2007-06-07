<%@ page import="org.labkey.issue.model.Issue" %>
<%@ page import="java.util.Iterator" %>
<%@ page extends="org.labkey.issue.UpdateEmailPage"%>
<%
    String changeComment = "(No change comment)";
    String modifiedBy = "(unknown)";
    Iterator<Issue.Comment> it = issue.getComments().iterator();
    Issue.Comment lastComment = null;
    while(it.hasNext())
        lastComment = it.next();
    if (lastComment != null)
    {
        modifiedBy = lastComment.getCreatedByName();
        changeComment = lastComment.getComment();
    }
%>

<%
    if (isPlain)
    {
%>
You can review this issue from this URL: <%=url%>

<%
    } else {
%>
You can review this issue here: <a href="<%=url%>"><%=url%></a><br/>
Modified by: <%=modifiedBy%><br/>
<%=changeComment%>
<%
    }
%>
