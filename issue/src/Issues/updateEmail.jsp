<%@ page import="Issues.model.Issue" %>
<%@ page import="java.util.Iterator" %>
<%@ page extends="Issues.UpdateEmailPage"%>
<%
    String changeComment = "(No change comment)";
    Iterator<Issue.Comment> it = issue.getComments().iterator();
    Issue.Comment lastComment = null;
    while(it.hasNext())
        lastComment = it.next();
    if (lastComment != null)
        changeComment = lastComment.getComment();
%>

<%
    if (isPlain)
    {
%>
You can review this issue from this URL: <%=url%>

<%
    } else {
%>
You can review this issue here: <a href="<%=url%>"><%=url%></a>
<%=changeComment%>
<%
    }
%>
