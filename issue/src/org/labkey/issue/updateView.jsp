<%@ page import="org.labkey.issue.IssuesController"%>
<%@ page import="org.labkey.issue.model.Issue"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.data.Container"%>
<%@ page import="org.labkey.api.data.DataRegion"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.ButtonServlet"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page extends="org.labkey.issue.IssuePage" %>
<%
    ViewContext context = HttpView.getRootContext();
    final Issue issue = getIssue();
    final Container c = context.getContainer();
    final String focusId = (0 == issue.getIssueId() ? "title" : "comment");
%>
<form style="margin:0" method="POST" action="<%=ViewURLHelper.toPathString("Issues", "doUpdate.post", context.getContainer().getPath())%>">

    <table border=0 cellspacing=2 cellpadding=0>
    <%
    if (null != getError() && 0 != getError().length())
    {
    %>
        <tr>
            <td colspan=3><font color="red" class="error"><%=h(getError())%></font></td>
        </tr>
    <%
    }
    if (!StringUtils.isEmpty(getRequiredFields()))
        out.print("<tr><td>Fields marked with an asterisk <span class=\"labkey-error\">*</span> are required.</td></tr>");
    %>
    </table>
    <table border=0 cellspacing=2 cellpadding=0><tr>
        <td><input name="<%=getAction()%>" type="image" value="Submit" src="<%=ButtonServlet.buttonSrc("Submit")%>"></td>
        <td><%= buttonLink("View Grid", new ViewURLHelper("Issues", "list", context.getContainer()).addParameter(DataRegion.LAST_FILTER_PARAM, "true"))%></td>
    </tr></table>

    <script language="javascript" type="text/javascript" src="<%=context.getContextPath()%>/select.js"></script>
    <script language="javascript" type="text/javascript" src="<%=context.getContextPath()%>/completion.js"></script>
    <table width=640>
        <tr><td colspan=3><table><tr>
<%
            if (0 == issue.getIssueId())
            {
%>
                <td class="ms-searchform" width="69"><%=getLabel("Title")%></td>
<%
            } else {
%>
                <td class="ms-WPTitle"><%=issue.getIssueId()%></td>
<%
            }
%>
                <td class="ms-vb" width="571">
                <%=writeInput("title", issue.getTitle(), "id=title style=\"width:100%;\"")%>
                </td></tr>
            </table></td></tr>
        <tr>
            <td valign="top" width="34%"><table>
                <tr><td class="ms-searchform"><%=getLabel("Status")%></td><td class="ms-vb"><%=h(issue.getStatus())%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("AssignedTo")%></td><td class="ms-vb"><%=writeSelect("assignedTo", "" + issue.getAssignedTo(), issue.getAssignedToName(), getUserOptions(c, issue))%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Type")%></td><td class="ms-vb"><%=writeSelect("type", issue.getType(), getTypeOptions(c.getId()))%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Area")%></td><td class="ms-vb"><%=writeSelect("area", issue.getArea(), getAreaOptions(c.getId()))%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Priority")%></td><td class="ms-vb"><%=writeSelect("priority", "" + issue.getPriority(), getPriorityOptions(c))%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Milestone")%></td><td class="ms-vb"><%=writeSelect("milestone", issue.getMilestone(), getMilestoneOptions(c.getId()))%></td></tr>
            </table></td>
            <td valign="top" width="33%"><table>
                <tr><td class="ms-searchform"><%=getLabel("Opened&nbsp;By")%></td><td class="ms-vb"><%=h(issue.getCreatedByName())%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Opened")%></td><td class="ms-vb"><%=writeDate(issue.getCreated())%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("ResolvedBy")%></td><td class="ms-vb"><%=h(issue.getResolvedByName())%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Resolved")%></td><td class="ms-vb"><%=writeDate(issue.getResolved())%></td></tr>
                <tr><td class="ms-searchform"><%=getLabel("Resolution")%></td><td class="ms-vb"><%=writeSelect("resolution", issue.getResolution(), getResolutionOptions(c))%></td></tr>
<%
            if (isEditable("resolution") || !"open".equals(issue.getStatus()) && null != issue.getDuplicate())
            {
%>
                <tr><td class="ms-searchform">Duplicate</td><td class="ms-vb">
                <%=writeInput("duplicate", null == issue.getDuplicate() ? null : issue.getDuplicate().toString())%>
                </td></tr>
<%
            }
%>
                <%=writeCustomColumn(c.getId(), "int1", _toString(issue.getInt1()), IssuesController.ISSUE_NONE)%>
                <%=writeCustomColumn(c.getId(), "int2", _toString(issue.getInt2()), IssuesController.ISSUE_NONE)%>
            </table></td>
            <td valign="top" width="33%"><table>
                <tr><td class="ms-searchform">Changed&nbsp;By</td><td class="ms-vb"><%=h(issue.getModifiedByName())%></td></tr>
                <tr><td class="ms-searchform">Changed</td><td class="ms-vb"><%=writeDate(issue.getModified())%></td></tr>
                <tr><td class="ms-searchform">Closed&nbsp;By</td><td class="ms-vb"><%=h(issue.getClosedByName())%></td></tr>
                <tr><td class="ms-searchform">Closed</td><td class="ms-vb"><%=writeDate(issue.getClosed())%></td></tr>
<%
            if (this.isEditable("notifyList"))
            {
%>
                <tr><td class="ms-searchform"><%=getLabel("NotifyList")%><br/>(one email address on each line)</td><td class="ms-vb"><%=getNotifyList(c, issue)%></td></tr>
<%
            } else {
%>
                <tr><td class="ms-searchform">Notify</td><td class="ms-vb"><%=getNotifyList(c, issue)%></td></tr>
<%
            }
%>
                <%=writeCustomColumn(c.getId(), "string1", issue.getString1(), IssuesController.ISSUE_STRING1)%>
                <%=writeCustomColumn(c.getId(), "string2", issue.getString2(), IssuesController.ISSUE_STRING2)%>
            </table></td>
        </tr>
    </table>
<%
    if (getBody() != null)
    {
%>
    <textarea id="comment" name="comment" cols="150" rows="20" style="width:100%"><%=PageFlowUtil.filter(getBody())%></textarea>
<%
    } else {
%>
    <textarea id="comment" name="comment" cols="150" rows="20" style="width:100%"></textarea>
<%
    }

    if (getCallbackURL() != null)
    {
%>
    <input type="hidden" name="callbackURL" value="<%=getCallbackURL()%>"/>
<%
    }

    for (Issue.Comment comment : issue.getComments())
    {
%>
        <hr><table width="100%"><tr><td align="left" class="ms-vb"><b>
        <%=writeDate(comment.getCreated())%>
        </b></td><td align="right" class="ms-vb"><b>
        <%=h(comment.getCreatedByName())%>
        </b></td></tr></table>
        <%=comment.getComment()%>
<%
    }
%>
    <input type="hidden" name=".oldValues" value="<%=PageFlowUtil.encodeObject(issue)%>">
    <input type="hidden" name="action" value="<%=getAction()%>">
</form>
<script type="text/javascript" for="window" event="onload">try {document.getElementById("<%=focusId%>").focus();} catch (x) {}</script>
