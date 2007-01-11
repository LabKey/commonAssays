<%@ page import="Issues.IssuesController"%>
<%@ page import="org.fhcrc.cpas.query.api.CustomView"%>
<%@ page import="org.fhcrc.cpas.security.ACL"%>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.JspView"%>
<%@ page import="org.fhcrc.cpas.view.ViewContext" %>
<%@ page extends="org.fhcrc.cpas.jsp.JspBase" %>

<%
    JspView<IssuesController.ListForm> me = (JspView<IssuesController.ListForm>) HttpView.currentView();
    IssuesController.ListForm bean = me.getModel();
    ViewContext context = HttpView.getRootContext();

    String viewName = request.getParameter("Issues.viewName");
    boolean isHidden = bean.getViews().get(viewName) != null ? bean.getViews().get(viewName).isHidden() : false;
%>

<%
    if (request.getParameter("error") != null)
    {
%>
        <font color="#FF0000"><%=request.getParameter("error")%></font><br/>
<%
    }
%>

<table><tr>
    <td>&nbsp;</td>
    <%
    if (context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT))
	{
	%><td><a href="insert.view"><%=PageFlowUtil.buttonImg("New Issue")%></td><%
	}
    %>
    <td>&nbsp;views:</td>
    <td><select onchange="document.location.href=this.options[this.selectedIndex].value">
        <option value="#"></option>
        <option value="?Issues.sort=-Milestone%2CAssignedTo/DisplayName">all</option>
        <option value="?Issues.Status~eq=open&amp;Issues.sort=-Milestone%2CAssignedTo/DisplayName">open</option>
        <option value="?Issues.Status~eq=resolved&amp;Issues.sort=-Milestone%2CAssignedTo/DisplayName">resolved</option>
    <%
        if (!context.getUser().isGuest())
        {
    %>
        <option value="?Issues.AssignedTo/DisplayName~eq=<%=h(context.getUser().getDisplayName())%>&amp;Issues.Status~neqornull=closed&amp;Issues.sort=-Milestone">mine</option>
    <%
        }

        for (CustomView cv : bean.getViews().values())
        {
            String customViewName = cv.getName() != null ? cv.getName() : "";
    %>
        <option value="?Issues.viewName=<%=h(customViewName)%>" <%=customViewName.equals(viewName) ? "selected" : ""%>><%=cv.getName()%></option>
    <%
        }
    %>
    </select></td>
    <%
    if ((!isHidden && !context.getUser().isGuest()) || (context.hasPermission(ACL.PERM_ADMIN)))
    {
    %>
    <td class=ms-vb>[<a href="<%=bean.getCustomizeURL()%>">customize&nbsp;view</a>]</td>
    <%
    }
    %>
    <td width=100%>&nbsp;</td>
    <td nowrap><form action="jumpToIssue.view" method="get">Jump&nbsp;to&nbsp;issue:<input type="text" size="5" name="issueId"/></form></td>
    <td align="right" nowrap><form action="search.view" method="get"><input type="image" align="top" vspace="2" src="<%=PageFlowUtil.buttonSrc("Search")%>">&nbsp;&nbsp;<input type="text" size="30" name="search" value="">&nbsp;&nbsp;&nbsp;</form></td>
</tr></table>
