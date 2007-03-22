<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.query.CustomView"%>
<%@ page import="org.labkey.api.security.ACL"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.issue.IssuesController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<IssuesController.ListForm> me = (JspView<IssuesController.ListForm>) HttpView.currentView();
    IssuesController.ListForm bean = me.getModel();
    ViewContext context = HttpView.getRootContext();

    String viewName = me.getModel().getQuerySettings().getViewName();
    boolean isHidden = bean.getViews().get(viewName) != null ? bean.getViews().get(viewName).isHidden() : false;

    final String OPEN_FILTER = "Issues.Status~eq=open&Issues.sort=Milestone%2CAssignedTo/DisplayName";
    final String RESOLVED_FILTER = "Issues.Status~eq=resolved&Issues.sort=Milestone%2CAssignedTo/DisplayName";
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
	%><td><a href="insert.view"><%=PageFlowUtil.buttonImg("New Issue")%></a></td><%
	}
    %>
    <td>&nbsp;views:</td>
    <td><select onchange="document.location.href=this.options[this.selectedIndex].value">
        <option value="#"></option>
        <option value="list.view" <%=isFilterSelected("", context)%>>all</option>
        <option value="<%='?' + h(OPEN_FILTER)%>" <%=isFilterSelected(OPEN_FILTER, context)%>>open</option>
        <option value="<%='?' + h(RESOLVED_FILTER)%>" <%=isFilterSelected(RESOLVED_FILTER, context)%>>resolved</option>
    <%
        if (!context.getUser().isGuest())
        {
            final String mineFilter = "Issues.AssignedTo/DisplayName~eq=" +  h(context.getUser().getDisplayName()) + "&Issues.Status~neqornull=closed&Issues.sort=-Milestone";
    %>
        <option value="<%='?' + h(mineFilter)%>" <%=isFilterSelected(mineFilter, context)%>>mine</option>
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

<%!
    String isFilterSelected(String filter, ViewContext context) {
        String qs = context.getViewURLHelper().getQueryString();
        if (StringUtils.isEmpty(filter) && StringUtils.isEmpty(qs))
            return "selected";

        if (!StringUtils.isEmpty(qs))
            qs = PageFlowUtil.decode(qs);

        if (PageFlowUtil.decode(filter).equals(qs))
            return "selected";

        return "";
    }
%>