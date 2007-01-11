<%@ page import="org.labkey.api.announcements.AnnouncementManager" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<% HttpView<AnnouncementManager.Settings> me = (HttpView<AnnouncementManager.Settings>) HttpView.currentView();
    AnnouncementManager.Settings settings = me.getModel();
%><form action="customize.post" method="post">
<table>
    <tr>
        <td class="ms-searchform">Board name</td>
        <td class="ms-vb"><input type="text" size="30" value="<%=settings.getBoardName()%>" name="boardName"></td>
    </tr>
    <tr>
        <td class="ms-searchform">Conversation name</td>
        <td class="ms-vb"><input type="text" size="30" value="<%=settings.getConversationName()%>" name="conversationName"></td>
    </tr>
    <tr>
        <td class="ms-searchform" valign="top" style="padding-top:2;">Security</td>
        <td class="ms-vb">
            <table>
                <tr>
                    <td valign="top"><input type="radio" name="secure" value="0" <%=settings.isSecure() ? "" : "checked"%>></td>
                    <td class="ms-vb"><b>OFF</b> - Converstations are visible to anyone with read permissions, content can be modified after posting, content will be sent via email</td>
                </tr>
                <tr>
                    <td valign="top"><input type="radio" name="secure" value="1" <%=settings.isSecure() ? "checked" : ""%>></td>
                    <td class="ms-vb"><b>ON</b> - Only editors and those on the members list can view conversations, content can't be modified after posting, content is never sent via email</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Members List</td>
        <td class="ms-vb"><input type="checkbox" name="userList"<%=settings.hasUserList() ? " checked" : ""%>></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Status</td>
        <td class="ms-vb"><input type="checkbox" name="status"<%=settings.hasStatus() ? " checked" : ""%>></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Expires</td>
        <td class="ms-vb"><input type="checkbox" name="expires"<%=settings.hasExpires() ? " checked" : ""%>></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Assigned To</td>
        <td class="ms-vb"><input type="checkbox" name="assignedTo"<%=settings.hasAssignedTo() ? " checked" : ""%>></td>
    </tr>
    <tr>
        <td class="ms-searchform">Include Format Picker</td>
        <td class="ms-vb"><input type="checkbox" name="formatPicker"<%=settings.hasFormatPicker() ? " checked" : ""%>></td>
    </tr>
    <tr>
        <td><input type="image" src="<%=PageFlowUtil.buttonSrc("Save")%>"/></td>
    </tr>
</table>
</form>