<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.util.DateUtil" %>
<%@ page import="org.labkey.announcements.AnnouncementsController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.announcements.model.AnnouncementManager" %>
<%@ page import="org.labkey.api.announcements.Announcement" %>
<%@ page import="org.labkey.api.attachments.Attachment" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.announcements.model.DiscussionServiceImpl" %>
<%@ page import="java.net.URISyntaxException" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<!--ANNOUNCEMENTS-->
<%
    AnnouncementsController.ThreadView me = (AnnouncementsController.ThreadView) HttpView.currentView();
    ViewContext context = me.getViewContext();
    AnnouncementsController.ThreadViewBean bean = me.getModel();

    Announcement announcement = bean.announcement;
    AnnouncementManager.Settings settings = bean.settings;

if (null == announcement)
{
    %><span class=ms-vb><%=h(settings.getConversationName())%> not found</span><%
    return;
}

if (null != bean.message)
{
    %><span class=ms-vb><%=h(bean.message)%></span><%
}

// is this an embdedded discussion?
boolean embedded = (null != announcement.getDiscussionSrcURL() && !context.getViewURLHelper().getPageFlow().equals("announcements"));
ViewURLHelper discussionSrc = null;

if (!embedded && null != announcement.getDiscussionSrcURL())
{
    try
    {
        discussionSrc = DiscussionServiceImpl.fromSaved(announcement.getDiscussionSrcURL());
        discussionSrc.replaceParameter("discussion.id", "" + announcement.getRowId());
    } catch (URISyntaxException x){}
}

if (!bean.print && !embedded)
{
    %><table width="100%">
    <tr>
    <td align="left" class="ms-vb"><%
    if (null != bean.messagesURL)
    {
        %>[<a href="<%=bean.messagesURL%>"><%=settings.getBoardName()%> Home</a>]&nbsp;<%
    }
    if (null != bean.listURL)
    {
        %>[<a href="<%=bean.listURL%>">View List</a>]&nbsp;<%
    }
    if (!bean.isResponse)
    {
        %>[<a href="<%=bean.printURL%>" target="printAnn">Print</a>]&nbsp;<%
    }
    %></td>
    </tr>
    </table><%
}
if (null != discussionSrc)
{
    %><p></p>This thread is a discussion. [<a href='<%=discussionSrc.getLocalURIString()%>'>view original page</a>]<%
}

%><table width="100%" cellpadding=0>
<tr>
    <td class="ms-vb" style="padding-top:14px; padding-bottom:2px; width:33%;" align=left><span class="ms-announcementtitle"><%=h(announcement.getTitle())%></span></td>
    <td class="ms-vb" style="padding-top:14px; padding-bottom:2px; width:33%;" align=center><%=h(announcement.getCreatedByName())%></td>
    <td class="ms-vb" style="padding-top:14px; padding-bottom:2px; width:33%;" align="right" nowrap><%

if (bean.perm.allowUpdate(announcement) && !bean.print)
{
    ViewURLHelper showUpdate = announcementURL(context, "showUpdate", "entityId", announcement.getEntityId());
    %>[<a href="<%=showUpdate.getLocalURIString()%>">Edit</a>]<%
}

%>&nbsp;<%=DateUtil.formatDateTime(announcement.getCreated())%></td>
</tr>
<tr style="height:1px;"><td colspan=3 class=ms-titlearealine><img alt="" height=1 width=1 src="<%=request.getContextPath()%>/_.gif"></td></tr><%

if (settings.hasMemberList() && null != announcement.getEmailList())
{
    %><tr><td colspan="3">Members: <%=announcement.getEmailList()%></td></tr><%
}

if (settings.hasStatus() && null != announcement.getStatus())
{
    %><tr><td>Status: <%=announcement.getStatus()%></td></tr><%
}

if (settings.hasExpires() && null != announcement.getExpires())
{
    %><tr><td align=left>Expires: <%=DateUtil.formatDate(announcement.getExpires())%>&nbsp;</td></tr><%
}

if (settings.hasAssignedTo() && null != announcement.getAssignedTo())
{
    %><tr><td>Assigned&nbsp;To: <%=h(announcement.getAssignedToName())%></td></tr><%
}

if (null != announcement.getBody())
{
    %><tr><td colspan="3">&nbsp;</td></tr><%
}

%><tr><td colspan="3" class="ms-vb"><%=announcement.translateBody(request,context.getContainer())%></td></tr>
<%
if(0 < announcement.getAttachments().size())
{%>
    <tr><td colspan="3"><div class="ms-vb">
    <%
        for (Attachment d : announcement.getAttachments())
        {%>
        <a href="<%=h(d.getDownloadUrl(request, "announcements"))%>"><img alt="" border=0 src="<%=request.getContextPath() + d.getFileIcon()%>">&nbsp;<%=d.getName()%></a>&nbsp;
    <%}%>
    </div></td></tr>
<%}%>
<tr><td colspan="3">&nbsp;</td></tr>
<%
if(0 < announcement.getResponses().size())
{
    Announcement prev = announcement;
    %>
<tr><td colspan="3"><table width="100%" border="0" cellspacing="0" cellpadding="2">
    <tr>
    <td width="2%">&nbsp;</td>
    <td colspan="2" width="100%">
        <table width="100%" border="0" cellspacing="0" cellpadding="2"><%

        for (Announcement r : announcement.getResponses())
        {%>
            <tr>
                <td class="ms-vb" colspan="2" style="background-color: #dddddd"><a name="row:<%=r.getRowId()%>"></a><%=r.getCreatedByName() + " responded:"%></td>
                <td class="ms-vb" align="right" style="background-color: #dddddd"><%
                if (bean.perm.allowUpdate(r) && !bean.print)
                {
                    ViewURLHelper showUpdate = announcementURL(context, "showUpdate", "entityId", r.getEntityId());
                    %>[<a href="<%=showUpdate.getLocalURIString()%>">Edit</a>]<%
                }
                if (bean.perm.allowDeleteMessage(r) && !bean.print)
                {
                    ViewURLHelper deleteSingleAnnouncement = announcementURL(context, "deleteSingleAnnouncement", "entityId", r.getEntityId());
                    %>[<a href="<%=deleteSingleAnnouncement.getLocalURIString()%>">Delete</a>]<%
                }
                %><%=DateUtil.formatDateTime(r.getCreated())%></td>
            </tr><%
            if (settings.hasMemberList() && !StringUtils.equals(r.getEmailList(),prev.getEmailList()))
            {
        %><tr><td>Members: <%=h(r.getEmailList())%></td></tr><%
            }
            if (settings.hasStatus() && !StringUtils.equals(r.getStatus(),prev.getStatus()))
            {
                %><tr><td>Status: <%=r.getStatus()%></td></tr><%
            }
            if (settings.hasExpires() && r.getExpires() != prev.getExpires())
            {
                %><tr><td>Expires: <%=DateUtil.formatDate(r.getExpires())%></td></tr><%
            }
            if (settings.hasAssignedTo() && r.getAssignedTo() != prev.getAssignedTo()) { %>
            <tr><td>Assigned&nbsp;To: <%=h(r.getAssignedToName())%></td></tr><% }
            if (null != r.getTitle() && !StringUtils.equals(r.getTitle(), prev.getTitle()))
            {
                %><tr><td>Title: <%=h(r.getTitle())%></td></tr><%
            }
            %><tr><td colspan="3" class="ms-vb"><%=r.translateBody(request,context.getContainer())%></td></tr><%
            if(0 < r.getAttachments().size())
            {
                %><tr><td colspan="3"><div class="ms-vb"><%
                for (Attachment rd : r.getAttachments())
                {
                    %><a href="<%=h(rd.getDownloadUrl(request, "announcements"))%>"><img alt="" border=0 src="<%=request.getContextPath()+ rd.getFileIcon()%>">&nbsp;<%=rd.getName()%></a>&nbsp;<%
                }
                %></div></td></tr><%
            }
            prev = r;
            %><tr><td colspan="2">&nbsp;</td></tr><%
        }%>
        </table>
    </td>
    </tr>
    </table></td></tr><%
}
%><tr><td colspan="3" class="ms-vb"><%
if (!bean.isResponse && !bean.print)
{
    if (bean.perm.allowResponse(announcement))
    {
        // There are two cases here.... I'm in the wiki controller or I'm not (e.g. I'm a discussion)
        if (embedded)
        {
            // UNDONE: respond in place
            ViewURLHelper url = context.cloneViewURLHelper();
            url.replaceParameter("discussion.id",""+announcement.getRowId());
            url.replaceParameter("discussion.reply","1");
            %><a href="<%=h(url.getLocalURIString())%>"><img src='<%=PageFlowUtil.buttonSrc("Post Response")%>' border="0" alt="[post response]"></a>&nbsp;<%
        }
        else
        {
            ViewURLHelper showResponse = announcementURL(context, "showResponse", "parentId", announcement.getEntityId());
            %><a href="<%=showResponse.getLocalURIString()%>"><img src='<%=PageFlowUtil.buttonSrc("Post Response")%>' border="0" alt="[post response]"></a>&nbsp;<%
        }
    }
    if (bean.perm.allowDeleteThread())
    {
        ViewURLHelper confirmDelete = announcementURL(context, "confirmDelete", "entityId", ""+announcement.getEntityId());
        if (embedded)
        {
            ViewURLHelper redirect = context.cloneViewURLHelper().deleteScopeParameters("discussion");
            confirmDelete.addParameter("redirect", redirect.getLocalURIString());
        }
        %><a href="<%=confirmDelete.getLocalURIString()%>"><img src='<%=PageFlowUtil.buttonSrc("Delete " + settings.getConversationName())%>' border="0" alt="[delete <%=settings.getConversationName().toLowerCase()%>]"></a>&nbsp;<%
    }
    if (null != discussionSrc)
    {
        %><a href="<%=discussionSrc.getLocalURIString()%>"><img src='<%=PageFlowUtil.buttonSrc("View original page")%>' border="0" alt="[original page]"></a>&nbsp;<%
    }
}
%></td></tr></table><%
if (bean.isResponse)
{
    %><a name="response"/><%
}

%>

<%!
    ViewURLHelper announcementURL(ViewContext context, String action, String... params)
    {
        ViewURLHelper url = new ViewURLHelper("announcements", action, context.getContainer());
        for (int i=0 ; i<params.length ; i+=2)
            url.addParameter(params[i], params[i+1]);
        return url;
    }
%>