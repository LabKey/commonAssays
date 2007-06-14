
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.data.DataRegion"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page import="org.labkey.issue.IssuesController"%>
<%@ page import="org.labkey.issue.model.Issue"%>
<%@ page extends="org.labkey.issue.IssuePage" %>
<%
    ViewContext context = HttpView.currentContext();
    final Issue issue = getIssue();
    final Container c = context.getContainer();
    final String issueId = Integer.toString(issue.getIssueId());
    final boolean hasUpdatePerms = getHasUpdatePermissions();

    if (!isPrint())
    {
%>

<form name="jumpToIssue" action="jumpToIssue.view" method="get">
<table border=0 cellspacing=2 cellpadding=2><tr>
<td><%= textLink("new issue", new ViewURLHelper("Issues", "insert", context.getContainer()).addParameter(DataRegion.LAST_FILTER_PARAM, "true"))%></td>
    <td><%= textLink("view grid", new ViewURLHelper("Issues", "list", context.getContainer()).addParameter(DataRegion.LAST_FILTER_PARAM, "true"))%></td>
    <td><%= textLink("update", new ViewURLHelper("Issues", "update", context.getContainer()).addParameter("issueId", issueId))%></td>
<%
    if (issue.getStatus().equals(Issue.statusOPEN))
    {
%>
    <td><%= textLink("resolve", new ViewURLHelper("Issues", "resolve", context.getContainer()).addParameter("issueId", issueId))%></td>
<%
    }
    else if (issue.getStatus().equals(Issue.statusRESOLVED))
    {
%>
    <td><%= textLink("close", new ViewURLHelper("Issues", "close", context.getContainer()).addParameter("issueId", issueId))%></td>
    <td><%= textLink("reopen", new ViewURLHelper("Issues", "reopen", context.getContainer()).addParameter("issueId", issueId))%></td>
<%
    }
    else if (issue.getStatus().equals(Issue.statusCLOSED))
    {
%>
    <td><%= textLink("reopen", new ViewURLHelper("Issues", "reopen", context.getContainer()).addParameter("issueId", issueId))%></td>
<%
    }
%>
    <td><%= textLink("print", context.cloneViewURLHelper().replaceParameter("_print", "1"))%></td>
    <td>&nbsp;&nbsp;&nbsp;Jump to issue: <input type="text" size="5" name="issueId"/></td>
</tr></table>
</form>

<%
    }
%>
<table width=640>
    <tr><td class="wpTitle" colspan="3"><%=issueId + " : " + h(issue.getTitle())%></td></tr>
    <tr>
        <td valign="top" width="34%"><table>
            <tr><td class="ms-searchform">Status</td><td class="normal"><%=h(issue.getStatus())%></td></tr>
            <tr><td class="ms-searchform">Assigned&nbsp;To</td><td class="normal"><%=h(issue.getAssignedToName())%></td></tr>
            <tr><td class="ms-searchform">Type</td><td class="normal"><%=h(issue.getType())%></td></tr>
            <tr><td class="ms-searchform">Area</td><td class="normal"><%=h(issue.getArea())%></td></tr>
            <tr><td class="ms-searchform">Priority</td><td class="normal"><%=issue.getPriority()%></td></tr>
            <tr><td class="ms-searchform">Milestone</td><td class="normal"><%=h(issue.getMilestone())%></td></tr>
        </table></td>
        <td valign="top" width="33%"><table>
            <tr><td class="ms-searchform">Opened&nbsp;By</td><td class="normal"><%=h(issue.getCreatedByName())%></td></tr>
            <tr><td class="ms-searchform">Opened</td><td class="normal"><%=writeDate(issue.getCreated())%></td></tr>
            <tr><td class="ms-searchform">Resolved By</td><td class="normal"><%=h(issue.getResolvedByName())%></td></tr>
            <tr><td class="ms-searchform">Resolved</td><td class="normal"><%=writeDate(issue.getResolved())%></td></tr>
            <tr><td class="ms-searchform">Resolution</td><td class="normal"><%=h(issue.getResolution())%></td></tr>
<%
            if (isEditable("resolution") || !"open".equals(issue.getStatus()) && null != issue.getDuplicate())
            {
%>
                <tr><td class="ms-searchform">Duplicate</td><td class="normal">
                <%=writeInput("duplicate", null == issue.getDuplicate() ? null : issue.getDuplicate().toString())%>
                </td></tr>
<%
            }
%>
            <%=writeCustomColumn(c.getId(), "int1", _toString(issue.getInt1()), IssuesController.ISSUE_NONE)%>
            <%=writeCustomColumn(c.getId(), "int2", _toString(issue.getInt2()), IssuesController.ISSUE_NONE)%>
        </table></td>
        <td valign="top" width="33%"><table>
            <tr><td class="ms-searchform">Changed&nbsp;By</td><td class="normal"><%=h(issue.getModifiedByName())%></td></tr>
            <tr><td class="ms-searchform">Changed</td><td class="normal"><%=writeDate(issue.getModified())%></td></tr>
            <tr><td class="ms-searchform">Closed&nbsp;By</td><td class="normal"><%=h(issue.getClosedByName())%></td></tr>
            <tr><td class="ms-searchform">Closed</td><td class="normal"><%=writeDate(issue.getClosed())%></td></tr>

            <%
                if (hasUpdatePerms)
                {%>
                    <tr><td class="ms-searchform">Notify</td><td class="normal"><%=getNotifyList(c, issue)%></td></tr>
                <%}
            %>
            <%=writeCustomColumn(c.getId(), "string1", issue.getString1(), IssuesController.ISSUE_STRING1)%>
            <%=writeCustomColumn(c.getId(), "string2", issue.getString2(), IssuesController.ISSUE_STRING2)%>
        </table></td>
    </tr>
</table>


<%
    if (getCallbackURL() != null)
    {
%>
    <input type="hidden" name="callbackURL" value="<%=getCallbackURL()%>"/>
<%
    }
%>


<%
    for (Issue.Comment comment : issue.getComments())
    {
%>
        <hr><table width="100%"><tr><td align="left" class="normal"><b>
        <%=writeDate(comment.getCreated())%>
        </b></td><td align="right" class="normal"><b>
        <%=h(comment.getCreatedByName())%>
        </b></td></tr></table>
        <%=comment.getComment()%>
<%
    }
%>