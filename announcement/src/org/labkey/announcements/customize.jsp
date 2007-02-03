<%@ page import="org.labkey.api.announcements.AnnouncementManager" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.announcements.AnnouncementManager.Settings.SortOrder" %>
<% HttpView<AnnouncementManager.Settings> me = (HttpView<AnnouncementManager.Settings>) HttpView.currentView();
    AnnouncementManager.Settings settings = me.getModel();
    ViewURLHelper returnUrl = (ViewURLHelper)me.getViewContext().get("returnUrl");
    String assignedToSelect = (String)me.getViewContext().get("assignedToSelect");
%><form action="customize.post" method="post">
<input type="hidden" name="returnUrl" value="<%=returnUrl.getEncodedLocalURIString()%>">
<table>
    <tr>
        <td class="ms-searchform">Board name</td>
        <td><table><tr><td class="ms-vb"><input type="text" size="30" value="<%=settings.getBoardName()%>" name="boardName"></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Conversation name</td>
        <td><table><tr><td class="ms-vb"><input type="text" size="30" value="<%=settings.getConversationName()%>" name="conversationName"></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Conversation sorting</td>
        <td class="ms-vb">
            <table>
                <tr>
                    <td valign="top"><input type="radio" name="sortOrderIndex" value="<%=SortOrder.CreationDate%>" <%=settings.getSortOrderIndex() == SortOrder.CreationDate.getIndex() ? "checked" : ""%>></td>
                    <td class="ms-vb"><b>Initial Post</b> - Sort by date of the first posting.  This is appropriate for announcements and blogs.</td>
                </tr>
                <tr>
                    <td valign="top"><input type="radio" name="sortOrderIndex" value="<%=SortOrder.LatestResponseDate%>" <%=settings.getSortOrderIndex() == SortOrder.LatestResponseDate.getIndex() ? "checked" : ""%>></td>
                    <td class="ms-vb"><b>Most Recent Post</b> - Sort by date of the most recent post.  This is often preferred for discussion boards.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
    <tr><td colspan=2>&nbsp;</td></tr>
    <tr>
        <td class="ms-searchform" valign="middle" style="padding-top:2;">Security</td>
        <td class="ms-vb">
            <table>
                <tr>
                    <td valign="top"><input type="radio" name="secure" value="0" <%=settings.isSecure() ? "" : "checked"%>></td>
                    <td class="ms-vb"><b>OFF</b> - Converstations are visible to anyone with read permissions, content can be modified after posting, content will be sent via email</td>
                </tr>
                <tr>
                    <td valign="top"><input type="radio" name="secure" value="1" <%=settings.isSecure() ? "checked" : ""%>></td>
                    <td class="ms-vb"><b>ON</b> - Only editors and those on the member list can view conversations, content can't be modified after posting, content is never sent via email</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td class="ms-searchform">Allow editing Title</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="titleEditable"<%=settings.isTitleEditable() ? " checked" : ""%>></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Member List</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="memberList"<%=settings.hasMemberList() ? " checked" : ""%>></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Status</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="status"<%=settings.hasStatus() ? " checked" : ""%>></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Expires</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="expires"<%=settings.hasExpires() ? " checked" : ""%>></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Assigned To</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="assignedTo"<%=settings.hasAssignedTo() ? " checked" : ""%>><td class="ms-searchform">Default Assigned To</td><td><%=assignedToSelect%></td></tr></table></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Format Picker</td>
        <td><table><tr><td class="ms-vb"><input type="checkbox" name="formatPicker"<%=settings.hasFormatPicker() ? " checked" : ""%>></td></tr></table></td>
    </tr>
        <td colspan=2><input type="image" src="<%=PageFlowUtil.buttonSrc("Save")%>"/>
        <%=PageFlowUtil.buttonLink("Cancel", returnUrl)%></td>
    </tr>
</table>
</form>