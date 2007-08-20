/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.announcements;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.labkey.announcements.EmailResponsePage.Reason;
import org.labkey.announcements.model.*;
import org.labkey.announcements.model.AnnouncementManager.EmailOption;
import org.labkey.announcements.model.AnnouncementManager.Settings;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.announcements.CommSchema;
import org.labkey.api.attachments.AttachmentForm;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.DownloadUrlHelper;
import org.labkey.api.data.*;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.query.UserIdRenderer;
import org.labkey.api.security.*;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.SecurityManager.PermissionSet;
import org.labkey.api.util.*;
import org.labkey.api.util.MailHelper.ViewMessage;
import org.labkey.api.view.*;
import org.labkey.api.wiki.WikiRenderer;
import org.labkey.api.wiki.WikiRendererType;
import org.labkey.api.wiki.WikiService;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Jpf.Controller(
        messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
/**
 * Shows a set of announcement or bulletin board items with replies.
 * Sends email to subscribers.
 * Properties are stored under the following keys:
 *   user=user,container,Object="Announcements"
 *              key="email":0 (no email), 1 (email all entries), 2 (email responses to messages I've created or replied to)
 */
public class AnnouncementsController extends ViewController
{
    private static CommSchema _comm = CommSchema.getInstance();

    private Permissions getPermissions() throws ServletException
    {
        return getPermissions(getContainer(), getUser(), getSettings(getContainer()));
    }


    public static Permissions getPermissions(Container c, User user, AnnouncementManager.Settings settings) throws ServletException
    {
        if (settings.isSecure())
            return new SecureMessageBoardPermissions(c, user, settings);
        else
            return new NormalMessageBoardPermissions(c, user);
    }


    public ViewForward getBeginForward() throws ServletException
    {
        return new ViewForward(getBeginUrl(getContainer()));
    }


    private static ViewURLHelper getBeginUrl(Container c)
    {
        return new ViewURLHelper("announcements", "begin", c);
    }


    @Jpf.Action
    /**
     * This method represents the point of entry into the pageflow
     */
    protected Forward begin() throws Exception
    {
        // Anyone with read permission can attempt to view the list.  AnnoucementWebPart will do further permission checking.  For example,
        //   in a secure message board, those without Editor permissions will only see messages when they are on the member list
        requiresPermission(ACL.PERM_READ);

        boolean displayAll = getViewURLHelper().getPageFlow().equalsIgnoreCase("announcements");
        WebPartView v = new AnnouncementWebPart(getContainer(), getViewURLHelper(), getUser(), displayAll);
        v.setFrame(WebPartView.FrameType.DIV);

        NavTrailConfig nav = new NavTrailConfig(getViewContext());
        nav.setTitle(getSettings().getBoardName());
        _renderInTemplate(v, getContainer(), nav, null, null);

        return null;
    }


    private static ViewURLHelper getListUrl(Container c)
    {
        return new ViewURLHelper("announcements", "list", c).addParameter(".lastFilter", "true");
    }


    @Jpf.Action
    protected Forward list() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        AnnouncementListView view = new AnnouncementListView(getViewContext());
        view.setFrame(WebPartView.FrameType.DIV);
        _renderInTemplate(view, getContainer(), getSettings().getBoardName() + " List", null, null);

        return null;
    }


    public static ViewURLHelper getAdminEmailUrl(Container c)
    {
        return new ViewURLHelper("announcements", "adminEmail", c);
    }


    @Jpf.Action
    protected Forward adminEmail() throws Exception
    {
        requiresAdmin();

        Container c = getContainer();

        DataRegion rgn = new DataRegion();
        rgn.setName("Email Preferences");
        rgn.setTable(_comm.getTableInfoEmailPrefs());
        rgn.setShowFilters(false);
        rgn.setSortable(false);
        rgn.setShowColumnSeparators(true);
        rgn.setShadeAlternatingRows(true);

        ButtonBar bb = new ButtonBar();
        bb.add(new ActionButton("showBulkEdit.view", "Bulk Edit"));
        rgn.setButtonBar(bb);

        GridView gridView = new GridView(rgn);

        ResultSet rs = null;
        try
        {
            rs = AnnouncementManager.getEmailPrefsResultset(c);
            gridView.setResultSet(rs);
            rgn.setColumns(DataRegion.colInfoFromMetaData(rs.getMetaData()));
        }
        finally
        {
            ResultSetUtil.close(rs);
        }

        DisplayColumn colGroupMembership = new GroupMembershipDisplayColumn(c);
        colGroupMembership.setCaption("Project&nbsp;Member?");
        rgn.addColumn(colGroupMembership);

        DisplayColumn colFirstName = rgn.getDisplayColumn("FirstName");
        if (colFirstName != null)
            colFirstName.setCaption("First Name");

        DisplayColumn colLastName = rgn.getDisplayColumn("LastName");
        if (colLastName != null)
            colLastName.setCaption("Last Name");

        DisplayColumn colDisplayName = rgn.getDisplayColumn("DisplayName");
        if (colDisplayName != null)
            colDisplayName.setCaption("Display Name");

        DisplayColumn colEmailOption = rgn.getDisplayColumn("EmailOption");
        if (colEmailOption != null)
            colEmailOption.setCaption("Email Option");

        DisplayColumn colLastModifiedByName = rgn.getDisplayColumn("LastModifiedByName");
        if (colLastModifiedByName != null)
            colLastModifiedByName.setCaption("Last Modified By");

        DisplayColumn colUserId = rgn.getDisplayColumn("UserId");
        if (colUserId != null)
            colUserId.setVisible(false);

        DisplayColumn colEmailOptionId = rgn.getDisplayColumn("EmailOptionId");
        if (colEmailOptionId != null)
            colEmailOptionId.setVisible(false);

        DisplayColumn colLastModifiedBy = rgn.getDisplayColumn("LastModifiedBy");
        if (colLastModifiedBy != null)
            colLastModifiedBy.setVisible(false);

        VBox vbox = new VBox();
        vbox.addView(new AnnouncementEmailDefaults(c));
        vbox.addView(gridView);

        _renderInTemplate(vbox, c, "Admin Email Preferences", null, null);

        return null;
    }

    @Jpf.Action
    protected Forward showBulkEdit() throws Exception
    {
        requiresAdmin();

        Container c = getContainer();

        ResultSet rs = null;
        List<AnnouncementManager.EmailPref> emailPrefList = new ArrayList<AnnouncementManager.EmailPref>();
        try
        {
            rs = AnnouncementManager.getEmailPrefsResultset(c);
            List<User> memberList = SecurityManager.getProjectMembers(c.getProject(), false);

            //get resultset data
            while(rs.next())
            {
                AnnouncementManager.EmailPref emailPref = new AnnouncementManager.EmailPref();

                int userId = rs.getInt("UserId");
                emailPref.setUserId(userId);
                emailPref.setEmail(rs.getString("Email"));
                emailPref.setFirstName(StringUtils.trimToEmpty(rs.getString("FirstName")));
                emailPref.setLastName(StringUtils.trimToEmpty(rs.getString("LastName")));
                emailPref.setDisplayName(StringUtils.trimToEmpty(rs.getString("DisplayName")));
                emailPref.setEmailOptionId((Integer) rs.getObject("EmailOptionId"));

                //specify whether user is member of a project group
                if (memberList.contains(UserManager.getUser(userId)))
                    emailPref.setProjectMember(true);

                emailPrefList.add(emailPref);
            }
        }
        finally
        {
            ResultSetUtil.close(rs);
        }

        HttpView v = new GroovyView("/org/labkey/announcements/bulkEdit.gm");
        v.addObject("title", "Admin Email Preferences");
        v.addObject("emailPrefList", emailPrefList);

        EmailOption[] emailOptions = AnnouncementManager.getEmailOptions();
        int defaultEmailOptionId = AnnouncementManager.getProjectEmailOption(c);
        
        for (EmailOption emailOption : emailOptions)
        {
            if (defaultEmailOptionId == emailOption.getEmailOptionId())
            {
                v.addObject("projectEmailOption", emailOption.getEmailOption());
                break;
            }
        }

        _renderInTemplate(v, c, "Admin Email Preferences", null, null);

        return null;
    }

    @Jpf.Action
    protected Forward bulkEdit(BulkEditEmailPrefsForm form) throws Exception
    {
        requiresAdmin();

        Container c = getContainer();

        int[] userId = form.getUserId();
        int[] emailOptionId = form.getEmailOptionId();

        for (int i = 0; i < userId.length; i++)
        {
            User projectUser = UserManager.getUser(userId[i]);
            Integer currentEmailOption = AnnouncementManager.getUserEmailOption(c, projectUser);

            //has this projectUser's option changed? if so, update
            //creating new record in EmailPrefs table if there isn't one, or deleting if set back to project default
            if (currentEmailOption != emailOptionId[i])
            {
                AnnouncementManager.saveEmailPreference(getUser(), c, projectUser, emailOptionId[i]);
            }
        }

        return new ViewForward(getAdminEmailUrl(c));
    }


    // Used for testing daily digest
    @Jpf.Action
    protected Forward sendDailyDigest() throws Exception
    {
        requiresAdmin();

        DailyDigest.sendDailyDigest();

        return _renderInTemplate(new HtmlView("Daily digest sent"), getContainer(), "Email", null, null);
    }


    @Jpf.Action
    protected Forward delete() throws Exception
    {
        if (!getPermissions().allowDeleteThread())
            HttpView.throwUnauthorized();

        Container c = getContainer();

        String[] deleteRows = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);

        if (deleteRows != null)
        {
            for (String deleteRow : deleteRows)
            {
                int rowId = Integer.parseInt(deleteRow);
                AnnouncementManager.deleteAnnouncement(c, rowId);
            }
        }

        return new ViewForward(getListUrl(c), true);
    }


    @Jpf.Action
    protected Forward confirmDeleteThread(AnnouncementDeleteForm form) throws Exception
    {
        return confirmDelete(form, "entire");
    }


    @Jpf.Action
    protected Forward confirmDeleteResponse(AnnouncementDeleteForm form) throws Exception
    {
        return confirmDelete(form, "response from the");
    }


    private Forward confirmDelete(AnnouncementDeleteForm form, String what) throws Exception
    {
        Permissions perm = getPermissions();

        Announcement message = null; 
        if (null != form.getEntityId())
            message = AnnouncementManager.getAnnouncement(getContainer(), form.getEntityId());
        if (null == message)
            message = AnnouncementManager.getAnnouncement(getContainer(), form.getRowId());

        if (null == message)
            HttpView.throwNotFound();
        if(!perm.allowDeleteMessage(message))
            HttpView.throwUnauthorized();

        GroovyView confirmDeleteView = new GroovyView("/org/labkey/announcements/confirmDelete.gm");
        confirmDeleteView.addObject("message", message);
        confirmDeleteView.addObject("what", what);
        confirmDeleteView.addObject("settings", getSettings());
        confirmDeleteView.addObject("redirect", form.getRedirect());
        HttpView template = new DialogTemplate(confirmDeleteView);
        includeView(template);

        return null;
    }


    @Jpf.Action
    protected Forward deleteSingleAnnouncement(AnnouncementDeleteForm form) throws Exception
    {
        Permissions perm = getPermissions();
        Container c = getContainer();

        Announcement message = null;
        if (null != form.getEntityId())
            message = AnnouncementManager.getAnnouncement(c, form.getEntityId());
        if (null == message)
            message = AnnouncementManager.getAnnouncement(c, form.getRowId());

        if (message == null)
            return throwResponseNotFound();
        if (!perm.allowDeleteMessage(message))
            HttpView.throwUnauthorized();

        AnnouncementManager.deleteAnnouncement(c, message.getRowId());

        ViewURLHelper url = null;

        String redirect = StringUtils.trimToNull(form.getRedirect());
        if (null != redirect)
        {
            url = new ViewURLHelper(redirect);
        }

        if (null == url)
        {
            //if this is a response, return to thread view
            url = cloneViewURLHelper();
            url.deleteParameters();
            if (message.getParent() != null)
            {
                Announcement parent = AnnouncementManager.getAnnouncement(c, message.getParent());
                url.setAction("thread");
                url.addParameter("rowId", Integer.toString(parent.getRowId()));
            }
            else
                url.setAction("begin");
        }

        return new ViewForward(url);
    }


    @Jpf.Action
    protected Forward confirmRemove(MemberListRemovalForm form) throws Exception
    {
        requiresLogin();

        User user = getUser();
        Settings settings = getSettings();
        Announcement thread = AnnouncementManager.getAnnouncement(getContainer(), form.getMessageId(), AnnouncementManager.INCLUDE_MEMBERLIST);
        String message = null;

        if (form.getUserId() != user.getUserId())
        {
            User removeUser = UserManager.getUser(form.getUserId());

            if (null == removeUser)
                message = "User could not be found.";
            else
                message = "You need to be logged in as " + removeUser.getEmail() + ".";
        }
        else if (null == thread)
        {
            message = settings.getConversationName().toLowerCase() + " not found.";
        }
        else if (!thread.getMemberList().contains(getUser()))
        {
            message = "You are not on the member list for this " + settings.getConversationName().toLowerCase() + ".";
        }

        String postUrl = cloneViewURLHelper().setAction("removeFromMemberList").getEncodedLocalURIString();
        GroovyView confirmRemoveUserView = new GroovyView("/org/labkey/announcements/confirmRemoveUser.gm");
        confirmRemoveUserView.addObject("message", message);
        confirmRemoveUserView.addObject("email", user.getEmail());
        confirmRemoveUserView.addObject("settings", getSettings());
        confirmRemoveUserView.addObject("thread", thread);
        confirmRemoveUserView.addObject("action", postUrl);
        HttpView template = new DialogTemplate(confirmRemoveUserView);
        includeView(template);

        return null;
    }


    @Jpf.Action
    protected Forward removeFromMemberList(MemberListRemovalForm form) throws Exception
    {
        requiresLogin();

        if (form.getUserId() != getUser().getUserId())
            HttpView.throwUnauthorized();

        // TODO: Could check that user is on member list...
        // TODO: Make this insert a new message to get history?
        AnnouncementManager.deleteUserFromMemberList(getUser(), form.getMessageId());

        return new ViewForward(cloneViewURLHelper().setPageFlow("Project").setAction("begin"), true);
    }


    private Announcement getAnnouncement(AttachmentForm form) throws SQLException, ServletException
    {
        Announcement ann = AnnouncementManager.getAnnouncement(getContainer(), form.getEntityId(), true);  // Force member list to be selected

        if (null == ann)
            throwThreadNotFound(getContainer());

        return ann;
    }


    @Jpf.Action
    protected Forward showAddAttachment(AttachmentForm form) throws Exception
    {
        Announcement ann = getAnnouncement(form);

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        return includeView(AttachmentService.get().getAddAttachmentView(ann, form));
    }


    @Jpf.Action
    protected Forward addAttachment(AttachmentForm form) throws Exception
    {
        Announcement ann = getAnnouncement(form);

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        return includeView(AttachmentService.get().add(ann, form));
    }


    @Jpf.Action
    protected Forward download(AttachmentForm form) throws IOException, ServletException, SQLException
    {
        Announcement ann = getAnnouncement(form);

        if (!getPermissions().allowRead(ann))
            HttpView.throwUnauthorized();

        AttachmentService.get().download(getResponse(), ann, form);

        return null;
    }


    @Jpf.Action
    protected Forward showConfirmDelete(AttachmentForm form) throws Exception
    {
        Announcement ann = getAnnouncement(form);

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        return includeView(AttachmentService.get().getConfirmDeleteView(ann, form));
    }


    @Jpf.Action
    protected Forward deleteAttachment(AttachmentForm form) throws Exception
    {
        Announcement ann = getAnnouncement(form);

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        return includeView(AttachmentService.get().delete(ann, form));
    }


    public static class MemberListRemovalForm extends FormData
    {
        private int _userId;
        private int _messageId;

        public int getMessageId()
        {
            return _messageId;
        }

        public void setMessageId(int messageId)
        {
            _messageId = messageId;
        }

        public int getUserId()
        {
            return _userId;
        }

        public void setUserId(int userId)
        {
            _userId = userId;
        }
    }


    private ViewURLHelper getReturnUrl()
    {
        String url = StringUtils.trimToNull((String)getViewContext().get("returnUrl"));
        if (null != url)
            return new ViewURLHelper(url);
        return null;
    }

    
    public static ViewURLHelper getShowCustomizeUrl(Container c, ViewURLHelper returnUrl)
    {
        ViewURLHelper url = new ViewURLHelper("announcements", "showCustomize", c);
        url.addParameter("returnUrl", returnUrl.getLocalURIString());
        return url;
    }


    @Jpf.Action
    protected Forward showCustomize() throws Exception
    {
        requiresAdmin();

        Settings settings = AnnouncementManager.getMessageBoardSettings(getContainer());
        JspView<Settings> view = new JspView<Settings>("/org/labkey/announcements/customize.jsp", settings);
        view.addObject("returnUrl", getReturnUrl());
        view.addObject("assignedToSelect", getAssignedToSelect(getContainer(), settings.getDefaultAssignedTo(), "defaultAssignedTo"));

        if (hasEditorPerm(Group.groupGuests))
            view.addObject("securityWarning", "Warning: guests have been granted editor permissions in this folder.  As a result, any anonymous user will be able to view, create, and respond to posts, regardless of the security setting below.  You may want to change permissions in this folder.");
        else if (hasEditorPerm(Group.groupUsers))
            view.addObject("securityWarning", "Warning: all site users have been granted editor permissions in this folder.  As a result, any logged in user will be able to view, create, and respond to posts, regardless of the security setting below.  You may want to change permissions in this folder.");

        _renderInTemplate(view, getContainer(), "Customize " + settings.getBoardName(), null, null);

        return null;
    }


    private boolean hasEditorPerm(int groupId) throws ServletException
    {
        ACL acl = getContainer().getAcl();
        int editorPerm = PermissionSet.EDITOR.getPermissions();
        int groupPerm = acl.getPermissions(groupId);

        return groupPerm == (groupPerm | editorPerm);
    }


    @Jpf.Action
    protected Forward customize(Settings form) throws Exception
    {
        requiresAdmin();

        AnnouncementManager.saveMessageBoardSettings(getContainer(), form);

        return new ViewForward(getReturnUrl());
    }


    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showResponse.do", name = "validate"))
    protected Forward respond(AnnouncementForm form) throws Exception
    {
        return _insert(form, false);
    }


    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showInsert.do", name = "validate"))
    protected Forward insert(AnnouncementForm form) throws Exception
    {
        return _insert(form, false);
    }


    // Different action ensures no validation in notes case

    @Jpf.Action
    protected Forward insertNote(AnnouncementForm form) throws Exception
    {
        return _insert(form, true);
    }


    private Forward _insert(AnnouncementForm form, boolean isNote) throws Exception
    {
        Permissions perm = getPermissions();

        if (!perm.allowInsert())
            HttpView.throwUnauthorized();

        User u = form.getUser();
        Container c = form.getContainer();

        FormFile[] formFiles = null;
        if (null != form.getMultipartRequestHandler())
        {
            //noinspection unchecked
            Map<String, FormFile> fileMap = form.getMultipartRequestHandler().getFileElements();
            formFiles = fileMap.values().toArray(new FormFile[fileMap.size()]);
        }

        Announcement insert = form.getBean();
        if (null == insert.getParent() || 0 == insert.getParent().length())
            insert.setParent(form.getParentId());

        if (!isNote && getSettings().hasMemberList() && null == form.getMemberList())
            insert.setMemberList(Collections.<User>emptyList());  // Force member list to get deleted, bug #2484
        else
            insert.setMemberList(form.getMemberList());  // TODO: Do this in validate()?

        AnnouncementManager.insertAnnouncement(c, u, insert, formFiles);

        // Don't send email for notes.  For messages, send email if there's body text or an attachment.
        if (!isNote && (null != insert.getBody() || AttachmentService.get().hasAttachment(formFiles)))
        {
            String rendererTypeName = (String) form.get("rendererType");
            WikiRendererType currentRendererType = (null == rendererTypeName ? WikiService.get().getDefaultMessageRendererType() : WikiRendererType.valueOf(rendererTypeName));
            sendNotificationEmails(insert, currentRendererType);
        }

        ViewURLHelper returnUrl = getReturnUrl();

        if (null == returnUrl)
        {
            // if this is a discussion, redirect back to originating page
            Announcement thread = insert;
            if (null != insert.getParent())
                thread = AnnouncementManager.getAnnouncement(getContainer(), insert.getParent(), true);

            if (null != thread.getDiscussionSrcURL())
            {
                returnUrl = DiscussionServiceImpl.fromSaved(thread.getDiscussionSrcURL());
                returnUrl.addParameter("discussion.id", "" + thread.getRowId());
                returnUrl.addParameter("_anchor", "discussionArea");
            }
            else
            {
                String threadId = thread.getEntityId();
                returnUrl = getThreadUrl(getRequest(), c, threadId, String.valueOf(insert.getRowId()));
            }
        }

        HttpView errorView = AttachmentService.get().getErrorView(formFiles, returnUrl);

        if (null == errorView)
            return new ViewForward(returnUrl);
        else
            return includeView(errorView);
    }


    private static String getStatusSelect(Settings settings, String currentValue)
    {
        List<String> options = Arrays.asList(settings.getStatusOptions().split(";"));

        StringBuilder sb = new StringBuilder(options.size() * 30);
        sb.append("    <select name=\"status\">\n");

        for (String word : options)
        {
            sb.append("      <option");

            if (word.equals(currentValue))
                sb.append(" selected");

            sb.append(">");
            sb.append(PageFlowUtil.filter(word));
            sb.append("</option>\n");
        }
        sb.append("    </select>");

        return sb.toString();
    }


    // AssignedTo == null => assigned to no one.
    private static String getAssignedToSelect(Container c, Integer assignedTo, String name)
    {
        List<User> possibleAssignedTo;

        try
        {
            possibleAssignedTo = SecurityManager.getUsersWithPermissions(c, ACL.PERM_INSERT);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        Collections.sort(possibleAssignedTo, new Comparator<User>()
        {
            public int compare(User u1, User u2)
            {
                return u1.getDisplayName().compareToIgnoreCase(u2.getDisplayName());
            }
        });

        // TODO: Should merge all this with IssuesManager.getAssignedToList()
        StringBuilder select = new StringBuilder("    <select name=\"" + name + "\">\n");
        select.append("      <option value=\"\"");
        select.append(null == assignedTo ? " selected" : "");
        select.append("></option>\n");

        for (User user : possibleAssignedTo)
        {
            select.append("      <option value=").append(user.getUserId());

            if (assignedTo != null && assignedTo == user.getUserId())
                select.append(" selected");

            select.append(">");
            select.append(user.getDisplayName());
            select.append("</option>\n");
        }

        select.append("    </select>");

        return select.toString();
    }


    private static ViewURLHelper getCompleteUserUrl(Container c)
    {
        return new ViewURLHelper("announcements", "completeUser", c);
    }


    @Jpf.Action
    protected Forward completeUser(CompletionForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        // Limit member list lookup to those with read permissions in this container.
        List<User> completionUsers = SecurityManager.getUsersWithPermissions(getContainer(), ACL.PERM_READ);
        List<AjaxCompletion> completions = UserManager.getAjaxCompletions(form.getPrefix(), completionUsers.toArray(new User[completionUsers.size()]));

        return sendAjaxCompletions(completions);
    }


    public static class CompletionForm extends FormData
    {
        String _prefix;

        public String getPrefix()
        {
            return _prefix;
        }

        public void setPrefix(String prefix)
        {
            _prefix = prefix;
        }
    }


    private static String getMemberListTextArea(User user, Container c, Announcement ann, String emailList)
    {
        String completeUserUrl = getCompleteUserUrl(c).getLocalURIString();

        StringBuilder sb = new StringBuilder();
        sb.append("<script type=\"text/javascript\">LABKEY.requiresScript('completion.js');</script>");
        sb.append("<textarea name=\"emailList\" id=\"emailList\" cols=\"30\" rows=\"5\"" );
        sb.append(" onKeyDown=\"return ctrlKeyCheck(event);\"");
        sb.append(" onBlur=\"hideCompletionDiv();\"");
        sb.append(" autocomplete=\"off\"");
        sb.append(" onKeyUp=\"return handleChange(this, event, '");
        sb.append(completeUserUrl);
        sb.append("prefix=');\"");
        sb.append(">");

        if (emailList != null)
        {
            sb.append(emailList);
        }
        else if (null != ann)
        {
            List<User> users = ann.getMemberList();
            sb.append(StringUtils.join(users.iterator(), "\n"));
        }
        else if (!user.isGuest())
        {
            sb.append(user.getEmail());
        }

        sb.append("</textarea>");

        return sb.toString();
    }


    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showUpdate.do", name = "validate"))
    protected Forward update(AnnouncementForm form) throws Exception
    {
        Announcement ann = form.selectAnnouncement();

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        Container c = getContainer();
        Announcement update = form.getBean();

        // TODO: What is this checking for?
        if (!c.getId().equals(update.getContainerId()))
            HttpView.throwUnauthorized();

        AnnouncementManager.updateAnnouncement(form.getUser(), update);

        String entityForward = update.getEntityId();
        if (update.getParent() != null)
        {
            Announcement parent = AnnouncementManager.getAnnouncement(c, update.getParent());
            entityForward = parent.getEntityId();
        }

        return new ViewForward(getThreadUrl(getRequest(), c, entityForward, String.valueOf(update.getRowId())), true);
    }


    private static ViewURLHelper getShowInsertUrl(Container c, ViewURLHelper returnUrl)
    {
        return new ViewURLHelper("announcements", "showInsert", c).addParameter("returnUrl", returnUrl.getLocalURIString());
    }


    @Jpf.Action
    protected Forward showInsert(AnnouncementForm form) throws Exception
    {
        Container c = getContainer();
        Settings settings = getSettings(c);
        Permissions perm = getPermissions(c, getUser(), settings);

        if (!perm.allowInsert())
            HttpView.throwUnauthorized();

        InsertMessageView insertView = new InsertMessageView(form, "New " + settings.getConversationName(), PageFlowUtil.getStrutsError(getRequest(), "main"));
        insertView.addObject("allowBroadcast", !settings.isSecure() && getUser().isAdministrator());
        insertView.addObject("returnUrl", getReturnUrl());

        return _renderInTemplate(insertView, c, "New " + settings.getConversationName(), "forms[0].title", null);
    }


    // UNDONE: convert to .jsp with MessageViewBean
    public static class InsertMessageView extends GroovyView<Object>
    {
        public InsertMessageView(AnnouncementForm form, String title, String errors)
        {
            super("/org/labkey/announcements/insert.gm", form);
            setTitle(title);
            addObject("allowBroadcast", false);
            addObject("returnUrl", null);
            boolean reshow = errors != null && errors.length() > 0;
            initView(this, form.getContainer(), form, null, reshow);
        }
    }


    private static void initView(HttpView view, Container c, AnnouncementForm form, Announcement mostRecent, boolean reshow)
    {
        // In reshow case we leave all form values as is so user can correct the errors.
        WikiRendererType currentRendererType;
        Integer assignedTo;

        Settings settings = getSettings(c);

        if (reshow)
        {
            String rendererTypeName = (String) form.get("rendererType");

            if (null == rendererTypeName)
                currentRendererType = WikiService.get().getDefaultMessageRendererType();
            else
                currentRendererType = WikiRendererType.valueOf(rendererTypeName);

            Announcement ann = form.getBean();
            assignedTo = ann.getAssignedTo();
        }
        else if (null == mostRecent)
        {
            // New thread... set base defaults
            Calendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, 1);

            String expires = DateUtil.formatDate(cal.getTime());
            form.set("expires", expires);
            currentRendererType = WikiService.get().getDefaultMessageRendererType();
            assignedTo = settings.getDefaultAssignedTo();
        }
        else
        {
            // Response... set values to match most recent properties on this thread
            assert null == form.get("title");
            assert null == form.get("expires");

            form.set("title", mostRecent.getTitle());
            form.set("status", mostRecent.getStatus());
            form.setTypedValue("expires", DateUtil.formatDate(mostRecent.getExpires()));

            assignedTo = mostRecent.getAssignedTo();
            currentRendererType = WikiRendererType.valueOf(mostRecent.getRendererType());
        }

        view.addObject("assignedToSelect", getAssignedToSelect(c, assignedTo, "assignedTo"));
        view.addObject("settings", settings);
        view.addObject("statusSelect", getStatusSelect(settings, (String)form.get("status")));
        view.addObject("memberList", getMemberListTextArea(form.getUser(), c, mostRecent, (String)(reshow ? form.get("emailList") : null)));
        view.addObject("currentRendererType", currentRendererType);
        view.addObject("renderers", WikiRendererType.values());

        view.addObject("form", form);
    }


    @Jpf.Action
    protected Forward showUpdate(AnnouncementForm form) throws Exception
    {
        Announcement ann = form.selectAnnouncement();
        if (null == ann)
            HttpView.throwNotFound();

        if (!getPermissions().allowUpdate(ann))
            HttpView.throwUnauthorized();

        AnnouncementUpdateView updateView = new AnnouncementUpdateView(form, ann);
        return _renderInTemplate(updateView, form.getContainer(), "Edit " + getSettings().getConversationName(), null, null);
    }


    public static ViewURLHelper getThreadUrl(HttpServletRequest request, Container c, String threadId, String anchor)
    {
        ViewURLHelper threadUrl = new ViewURLHelper(request, "announcements", "thread", c);
        threadUrl.addParameter("entityId", threadId);
        threadUrl.addParameter("_anchor", anchor);
        return threadUrl;
    }


    @Jpf.Action
    protected Forward thread(AnnouncementForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_READ); // TODO: Shouldn't need this check once ThreadView throws Unauthorized
        boolean print = "1".equals(getViewURLHelper().getParameter("print"));

        ThreadView threadView = new ThreadView(form, getContainer(), getViewURLHelper(), getPermissions(), print);
        threadView.setFrame(WebPartView.FrameType.DIV);

        Announcement ann = threadView.getAnnouncement();
        String title = ann != null ? ann.getTitle() : "Error";

        if (print)
        {
            threadView.setFrame(WebPartView.FrameType.DIV);
            PrintTemplate template = new PrintTemplate(threadView, title);
            return includeView(template);
        }
        else
        {
            String anchor = getViewURLHelper().getParameter("_anchor");
            return _renderInTemplate(threadView, c, title, null, (null != anchor ? "row:" + anchor : null));
        }
    }


    @Jpf.Action
    protected Forward showResponse(AnnouncementForm form) throws Exception
    {
        Permissions perm = getPermissions();
        Announcement parent = null;
        Container c = getContainer();

        if (null != form.getParentId())
            parent = AnnouncementManager.getAnnouncement(c, form.getParentId(), true);

        if (null == parent)
            return throwThreadNotFound(c);

        if (!perm.allowResponse(parent))
            HttpView.throwUnauthorized();

        ThreadView threadView = new ThreadView(c, getViewURLHelper(), parent, perm);
        threadView.setFrame(WebPartView.FrameType.DIV);

        boolean reshow = (PageFlowUtil.getStrutsError(getRequest(), "main").length() != 0);
        HttpView respondView = new RespondView(c, parent, form, reshow);

        NavTrailConfig trail = getDefaultNavTrail();
        if (parent != null)
            trail.add(new NavTree(parent.getTitle(), "thread.view?rowId=" + parent.getRowId()));
        trail.setTitle("Respond to " + getSettings().getConversationName());
        return _renderInTemplate(new VBox(threadView, respondView), c, trail, "forms[0].body", "response");
    }


    public static class RespondView extends GroovyView
    {
        public RespondView(Container c, Announcement parent, AnnouncementForm form, boolean reshow)
        {
            super("/org/labkey/announcements/respond.gm", "Response");
            Announcement latestPost = AnnouncementManager.getLatestPost(c, parent);
            initView(this, c, form, latestPost, reshow);
            addObject("parentAnnouncement", parent);
            addObject("returnUrl", null);
        }

        public RespondView(Container c, Announcement parent, ViewURLHelper returnUrl)
        {
            this(c, parent, new AnnouncementForm(), false);
            addObject("returnUrl", returnUrl);
        }
    }


    @Jpf.Action
    protected Forward rss() throws Exception
    {
        // First level of permission checking... must at least be able to read.
        Container c = getContainer(ACL.PERM_READ);
        HttpServletRequest request = AppProps.getInstance().createMockRequest();

        // getFilter performs further permission checking on secure board (e.g., non-Editors only see threads where they're on the member list)
        SimpleFilter filter = getFilter(getSettings(), getPermissions(), true);

        // TODO: This only grabs announcements... add responses too?
        Pair<Announcement[], Boolean> pair = AnnouncementManager.getAnnouncements(c, filter, getSettings().getSort(), 100);

        WebPartView v = new GroovyView("/org/labkey/announcements/rss.gm");
        v.setFrame(WebPartView.FrameType.NONE);
        v.addObject("announcements", pair.first);
        v.addObject("container", c);
        v.addObject("request", request);
        ViewURLHelper url = new ViewURLHelper(request, "announcements", "thread.view", c);
        v.addObject("url", url.getURIString() + "rowId=");
        v.addObject("homePageUrl", ViewURLHelper.getBaseServerURL(request));

        getResponse().setContentType("text/xml");
        includeView(v);
        return null;
    }


    @Jpf.Action
    protected Forward purge() throws ServletException, SQLException, IOException
    {
        if (!getUser().isAdministrator())
            HttpView.throwUnauthorized();
        int rows = ContainerUtil.purgeTable(_comm.getTableInfoAnnouncements(), null);
        getResponse().getWriter().println("deleted " + rows + " rows<br>");
        return null;
    }


    public static ViewURLHelper getShowEmailPreferencesUrl(Container c, ViewURLHelper srcUrl)
    {
        return new ViewURLHelper("announcements", "showEmailPreferences", c).addParameter("srcUrl", srcUrl.getLocalURIString());
    }


    // Used by daily digest & response emails, which uses mock request and returns to the home page
    private static ViewURLHelper getShowEmailPreferencesUrl(HttpServletRequest request, Container c)
    {
        ViewURLHelper url = new ViewURLHelper(request, "announcements", "showEmailPreferences", c);
        url.addParameter("srcUrl", new ViewURLHelper(request, "announcements", "begin", c.getPath()).getURIString());
        return url;
    }


    @Jpf.Action
    protected Forward showEmailPreferences(EmailOptionsForm form) throws Exception
    {
        requiresLogin();
        Container c = getContainer(ACL.PERM_READ);

        User user = getUser();
        List<User> projectMembers = SecurityManager.getProjectMembers(c, false);

        int emailOption = AnnouncementManager.getUserEmailOption(c, user);
        if (emailOption == AnnouncementManager.EMAIL_PREFERENCE_DEFAULT)
        {
            if (projectMembers.contains(user))
                emailOption = AnnouncementManager.getProjectEmailOption(c);
            else
                emailOption = AnnouncementManager.EMAIL_PREFERENCE_NONE;
        }

        form.setEmailOption(emailOption);

        showEmailPreferencesPage(form, null);
        return null;
    }


    @Jpf.Action
    protected Forward updateEmailPreferences(EmailOptionsForm form) throws Exception
    {
        requiresLogin();
        AnnouncementManager.saveEmailPreference(form.getUser(), form.getContainer(), form.getEmailOption());

        showEmailPreferencesPage(form, "Setting changed successfully.");
        return null;
    }

    @Jpf.Action
    protected Forward setDefaultMailOptions(EmailDefaultSettingsForm form) throws Exception
    {
        requiresAdmin();

        //save the default settings
        AnnouncementManager.saveProjectEmailSettings(getContainer(), form.getDefaultEmailOption());

        //redirect to admin email page
        return new ViewForward(getAdminEmailUrl(getContainer()));
    }


    private void sendNotificationEmails(Announcement a, WikiRendererType currentRendererType) throws Exception
    {
        Container c = getContainer();
        HttpServletRequest request = AppProps.getInstance().createMockRequest();
        Settings settings = getSettings();

        boolean isResponse = null != a.getParent();
        Announcement parent = a;
        if (isResponse)
            parent = AnnouncementManager.getAnnouncement(c, a.getParent());

        String messageId = "<" + a.getEntityId() + "@" + AppProps.getInstance().getDefaultDomain() + ">";
        String references = messageId + " <" + parent.getEntityId() + "@" + AppProps.getInstance().getDefaultDomain() + ">";

        // Email all copies of this message in a background thread
        MailHelper.BulkEmailer emailer = new MailHelper.BulkEmailer();

        if (a.isBroadcast())
        {
            // Allow broadcast only if message board is not secure and user is site administrator
            if (settings.isSecure() || !getUser().isAdministrator())
                HttpView.throwUnauthorized();

            // Get all site users' email addresses
            List<String> emails = UserManager.getUserEmailList();
            ViewMessage m = getMessage(c, settings, parent, a, isResponse, null, currentRendererType, Reason.broadcast);
            m.setHeader("References", references);
            emailer.addMessage(emails, m);
        }
        else
        {
            // Send a notification email to everyone on the member list.  This email will include a link that removes the user from the member list.
            IndividualEmailPrefsSelector sel = new IndividualEmailPrefsSelector(c);

            List<User> users = sel.getNotificationUsers(a);

            if (!users.isEmpty())
            {
                List<User> memberList;

                if (settings.hasMemberList() && null != a.getMemberList())
                    memberList = a.getMemberList();
                else
                    memberList = Collections.emptyList();

                List<String> noMemberListEmails = new ArrayList<String>(users.size());

                for (User user : users)
                {
                    if (memberList.contains(user))
                    {
                        ViewURLHelper removeMeURL = new ViewURLHelper(request, "announcements", "confirmRemove", c.getPath());
                        removeMeURL.addParameter("userId", String.valueOf(user.getUserId()));
                        removeMeURL.addParameter("messageId", String.valueOf(parent.getRowId()));

                        ViewMessage m = getMessage(c, settings, parent, a, isResponse, removeMeURL.getURIString(), currentRendererType, Reason.memberList);
                        m.setHeader("References", references);
                        m.setHeader("Message-ID", messageId);
                        emailer.addMessage(user.getEmail(), m);
                    }
                    else
                    {
                        noMemberListEmails.add(user.getEmail());
                    }
                }

                if (!noMemberListEmails.isEmpty())
                {
                    ViewURLHelper changeEmailURL = getShowEmailPreferencesUrl(request, c);
                    ViewMessage m = getMessage(c, settings, parent, a, isResponse, changeEmailURL.getURIString(), currentRendererType, Reason.signedUp);
                    m.setHeader("References", references);
                    m.setHeader("Message-ID", messageId);
                    emailer.addMessage(noMemberListEmails, m);
                }
            }
        }

        emailer.start();
    }


    private ViewMessage getMessage(Container c, Settings settings, Announcement parent, Announcement a, boolean isResponse, String removeUrl, WikiRendererType currentRendererType, Reason reason) throws Exception
    {
        ViewMessage m = MailHelper.createMultipartViewMessage(AppProps.getInstance().getSystemEmailAddress(), null);
        m.setSubject(StringUtils.trimToEmpty(isResponse ? "RE: " + parent.getTitle() : a.getTitle()));
        HttpServletRequest request = AppProps.getInstance().createMockRequest();

        EmailResponsePage page = createResponseTemplate("emailResponsePlain.jsp", false, c, settings, parent, a, removeUrl, currentRendererType, reason);
        JspView view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(request, view, "text/plain");

        page = createResponseTemplate("emailResponse.jsp", true, c, settings, parent, a, removeUrl, currentRendererType, reason);
        view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(request, view, "text/html");

        return m;
    }

    private EmailResponsePage createResponseTemplate(String templateName, boolean includeBody, Container c, Settings settings, Announcement parent,
            Announcement a, String removeUrl, WikiRendererType currentRendererType, Reason reason)
    {
        HttpServletRequest request = AppProps.getInstance().createMockRequest();

        EmailResponsePage page = (EmailResponsePage) JspLoader.createPage(request, AnnouncementsController.class, templateName);

        page.settings = settings;
        page.threadURL = getThreadUrl(request, c, parent.getEntityId(), String.valueOf(a.getRowId())).getURIString();
        page.boardPath = c.getPath();
        ViewURLHelper boardURL = new ViewURLHelper(request, "announcements", "begin", c.getPath());
        page.boardURL = boardURL.getURIString();

        URLHelper cssURL = new URLHelper(request);
        cssURL.setPath("/core/stylesheet.view");
        cssURL.setRawQuery(null);
        page.cssURL = cssURL.getURIString();

        page.removeUrl = removeUrl;
        page.siteURL = ViewURLHelper.getBaseServerURL(request);
        page.responseAnnouncement = a;
        page.reason = reason;

        // for plain text email messages, we don't want to include the body regardless of whether the msg board
        // is secure
        if (includeBody && !settings.isSecure())
        {
            //format email using same renderer chosen for message
            //note that we still send all messages, including plain text, as html-formatted messages; only the inserted body text differs between renderers.
            WikiRenderer w = WikiService.get().getRenderer(currentRendererType);
            page.responseBody = w.format(a.getBody()).getHtml();
        }
        return page;
    }

    private void showEmailPreferencesPage(EmailOptionsForm form, String message) throws Exception
    {
        JspView view = new JspView("/org/labkey/announcements/emailPreferences.jsp");
        view.setFrame(WebPartView.FrameType.DIV);
        EmailPreferencesPage page = (EmailPreferencesPage)view.getPage();
        view.setTitle("Email Preferences");

        Settings settings = getSettings();
        page.emailPreference = form.getEmailPreference();
        page.notificationType = form.getNotificationType();
        page.srcURL = form.getSrcUrl();
        page.message = message;
        page.hasMemberList = settings.hasMemberList();
        page.conversationName = settings.getConversationName().toLowerCase();

        _renderInTemplate(view, form.getContainer(), "Email Preferences", null, null);
    }

    private Settings getSettings() throws ServletException
    {
        return getSettings(getContainer());
    }

    public static Settings getSettings(Container c)
    {
        try
        {
            return AnnouncementManager.getMessageBoardSettings(c);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);  // Not great... but this method is called from all over (webpart constructors, etc.)
        }
    }


    private static Forward throwThreadNotFound(Container c)
    {
        return HttpView.throwNotFound("Could not find " + getSettings(c).getConversationName().toLowerCase());
    }


    private Forward throwResponseNotFound() throws ServletException
    {
        return HttpView.throwNotFound("Could not find response");
    }


    private NavTrailConfig getDefaultNavTrail() throws ServletException
    {
        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        trailConfig.add(new NavTree(getSettings().getBoardName().toLowerCase() + " home", getBeginForward()));
        return trailConfig;
    }


    private Forward _renderInTemplate(HttpView view, Container c, String title, String focus, String anchor) throws Exception
    {
        NavTrailConfig trailConfig = getDefaultNavTrail();
        if (null != title)
            trailConfig.setTitle(title);
        return _renderInTemplate(view, c, trailConfig, focus, anchor);
    }
    

    private Forward _renderInTemplate(HttpView view, Container c, NavTrailConfig trailConfig, String focus, String anchor) throws Exception
    {
        HomeTemplate template = new HomeTemplate(getViewContext(), c, view, trailConfig);

        if (null != focus)
            template.getModelBean().setFocus(focus);

        template.getModelBean().setAnchor(anchor);

        return includeView(template);
    }


    public static class BulkEditEmailPrefsForm extends FormData
    {
        private int[] _userId;
        private int[] _emailOptionId;

        public int[] getEmailOptionId()
        {
            return _emailOptionId;
        }

        public void setEmailOptionId(int[] emailOptionId)
        {
            _emailOptionId = emailOptionId;
        }

        public int[] getUserId()
        {
            return _userId;
        }

        public void setUserId(int[] userId)
        {
            _userId = userId;
        }
    }

    public static class AnnouncementDeleteForm extends FormData
    {
        private int _rowId;
        private String _entityId;
        private String _redirect = null;

        public String getEntityId()
        {
            return _entityId;
        }

        public void setEntityId(String entityId)
        {
            _entityId = entityId;
        }

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }

        public String getRedirect()
        {
            return _redirect;
        }

        public void setRedirect(String redirect)
        {
            this._redirect = redirect;
        }
    }

    public static class AnnouncementForm extends BeanViewForm<Announcement>
    {
        Announcement _selectedAnnouncement = null;
        List<User> _memberList = null;

        public AnnouncementForm()
        {
            super(Announcement.class, null, new String[]{"parentid"});
        }

        public String getParentId()
        {
            return _stringValues.get("parentid");
        }

        public List<User> getMemberList()
        {
            return _memberList;
        }

        public void setMemberList(List<User> memberList)
        {
            _memberList = memberList;
        }

        Announcement selectAnnouncement() throws SQLException
        {
            if (null == _selectedAnnouncement)
            {
                Announcement bean = getBean();
                if (null != bean.getEntityId())
                    _selectedAnnouncement = AnnouncementManager.getAnnouncement(getContainer(), bean.getEntityId(), true);  // Need member list
                if (null == _selectedAnnouncement)
                    _selectedAnnouncement = AnnouncementManager.getAnnouncement(getContainer(), bean.getRowId(), AnnouncementManager.INCLUDE_MEMBERLIST);
            }
            return _selectedAnnouncement;
        }

        @Override
        public ActionErrors validate(ActionMapping actionMapping, HttpServletRequest servletRequest)
        {
            Settings settings = getSettings(getContainer());
            Announcement bean = getBean();

            // Title can never be null.  If title is not editable, it will still be posted in a hidden field.
            if (StringUtils.trimToNull(bean.getTitle()) == null)
                addActionError("Title must not be blank.");

            try
            {
                String expires = StringUtils.trimToNull((String) get("expires"));
                if (null != expires)
                    DateUtil.parseDateTime(expires);
            }
            catch (ConversionException x)
            {
                addActionError("Expires must be blank or a valid date.");
            }

            String emailList = bean.getEmailList();
            List<User> memberList = Collections.emptyList();

            if (null != emailList)
            {
                String[] rawEmails = emailList.split("\n");
                List<String> invalidEmails = new ArrayList<String>();
                List<ValidEmail> emails = SecurityManager.normalizeEmails(rawEmails, invalidEmails);

                for (String rawEmail : invalidEmails)
                {
                    // Ignore lines of all whitespace, otherwise show an error.
                    if (!"".equals(rawEmail.trim()))
                        addActionError(rawEmail.trim() + ": Invalid email address");
                }

                memberList = new ArrayList<User>(emails.size());

                for (ValidEmail email : emails)
                {
                    User user = UserManager.getUser(email);

                    if (null == user)
                        addActionError(email.getEmailAddress() + ": Doesn't exist");
                    else if (!memberList.contains(user))
                        memberList.add(user);
                }

                // New up an announcement to check permissions for the member list
                Announcement ann = new Announcement();
                ann.setMemberList(memberList);

                for (User user : memberList)
                {
                    try
                    {
                        Permissions perm = getPermissions(getContainer(), user, settings);

                        if (!perm.allowRead(ann))
                            addActionError("Can't add " + user.getEmail() + " to the member list: This user doesn't have permission to read the thread.");
                    }
                    catch(ServletException e)
                    {
                        addActionError("Error retrieving settings for this message board.  " + e);
                    }
                }

                setMemberList(memberList);
            }

            Integer assignedTo = bean.getAssignedTo();

            if (null != assignedTo)
            {
                User assignedToUser = UserManager.getUser(assignedTo);

                if (null == assignedToUser)
                {
                    addActionError("Assigned to user " + assignedTo + ": Doesn't exist");
                }
                else
                {
                    try
                    {
                        Permissions perm = getPermissions(getContainer(), assignedToUser, settings);

                        // New up an announcement to check permissions for the assigned to user
                        Announcement ann = new Announcement();
                        ann.setMemberList(memberList);

                        if (!perm.allowRead(ann))
                            addActionError("Can't assign to " + assignedToUser.getEmail() + ": This user doesn't have permission to read the thread.");
                    }
                    catch(ServletException e)
                    {
                        addActionError("Error retrieving settings for this message board.  " + e);
                    }
                }
            }

            if ("HTML".equals(bean.getRendererType()))
            {
                Collection<String> validateErrors = new LinkedList<String>();
                PageFlowUtil.validateHtml(bean.getBody(),validateErrors, getUser().isAdministrator());
                for (String err : validateErrors)
                    addActionError(err);
            }

            return getActionErrors();
        }
    }


    public static class EmailDefaultSettingsForm extends ViewForm
    {
        int _defaultEmailOption;
        int _defaultEmailFormat;

        public int getDefaultEmailFormat()
        {
            return _defaultEmailFormat;
        }

        public void setDefaultEmailFormat(int defaultEmailFormat)
        {
            _defaultEmailFormat = defaultEmailFormat;
        }

        public int getDefaultEmailOption()
        {
            return _defaultEmailOption;
        }

        public void setDefaultEmailOption(int defaultEmailOption)
        {
            _defaultEmailOption = defaultEmailOption;
        }
    }

    public static class EmailOptionsForm extends ViewForm
    {
        private int _emailPreference = AnnouncementManager.EMAIL_PREFERENCE_NONE;
        private int _notificationType = 0;
        private String _srcUrl = null;

        // Email option is a single int that contains the conversation preference AND a bit for digest vs. individual
        // This method splits them apart
        public void setEmailOption(int emailOption)
        {
            _emailPreference = emailOption & AnnouncementManager.EMAIL_PREFERENCE_MASK;
            _notificationType = emailOption & AnnouncementManager.EMAIL_NOTIFICATION_TYPE_DIGEST;
        }

        public int getEmailOption()
        {
            // Form allows "no email" + "daily digest" -- change this to "no email" + "individual" since they are equivalent
            // and we don't want to deal with the former option in the database, with foreign keys, on the admin pages, etc. 
            if (_emailPreference == AnnouncementManager.EMAIL_PREFERENCE_NONE)
                _notificationType = 0;

            return _emailPreference | _notificationType;
        }

        public int getEmailPreference()
        {
            return _emailPreference;
        }

        public void setEmailPreference(int emailPreference)
        {
            _emailPreference = emailPreference;
        }

        public int getNotificationType()
        {
            return _notificationType;
        }

        public void setNotificationType(int notificationType)
        {
            _notificationType = notificationType;
        }

        public String getSrcUrl()
        {
            return _srcUrl;
        }

        public void setSrcUrl(String srcUrl)
        {
            _srcUrl = srcUrl;
        }
    }


    public static class AdminEmailsForm extends ViewForm
    {
        private String _addresses;

        public String getAddresses()
        {
            return _addresses;
        }

        public void setAddresses(String addresses)
        {
            _addresses = addresses;
        }
    }


    public static class AnnouncementEmailDefaults extends GroovyView
    {
        Container _c = null;

        public AnnouncementEmailDefaults(Container c)
        {
            super("/org/labkey/announcements/announcementEmailDefaults.gm");
            addObject("emailOptionsList", null);
            addObject("defaultEmailOption", null);
            setContainer(c);
        }

        @Override
        public void prepareWebPart(Object model) throws ServletException
        {
            try
            {
                List<AnnouncementManager.EmailOption> emailOptionsList = Arrays.asList(AnnouncementManager.getEmailOptions());
                addObject("emailOptionsList", emailOptionsList);
                addObject("defaultEmailOption", AnnouncementManager.getProjectEmailOption(getContainer()));
            }
            catch (SQLException e)
            {
                throw new ServletException(e);
            }
        }

        public Container getContainer()
        {
            return _c;
        }

        public void setContainer(Container c)
        {
            _c = c;
        }

    }

    public static class AnnouncementListLinkBar extends GroovyView
    {
        public AnnouncementListLinkBar(Container c, ViewURLHelper url, User user, Settings settings, Permissions perm, boolean displayAll)
        {
            super("/org/labkey/announcements/announcementListLinkBar.gm");

            boolean useLastFilter = ColumnInfo.booleanFromString(url.getParameter(DataRegion.LAST_FILTER_PARAM));
            if (useLastFilter)
            {
                HttpServletRequest request = HttpView.currentRequest();
                ViewURLHelper lastFilterUrl = (ViewURLHelper) request.getSession().getAttribute(request.getRequestURI() + DataRegion.LAST_FILTER_PARAM);

                if (null != lastFilterUrl)
                    url = lastFilterUrl;
            }

            SimpleFilter urlFilter = new SimpleFilter(url, "Threads");
            boolean isFiltered = !urlFilter.getWhereParamNames().isEmpty();
            String filterText = getFilterText(settings, displayAll, isFiltered, 0);

            addObject("settings", settings);
            addObject("insertURL", perm.allowInsert() ? getShowInsertUrl(c, url) : null);
            addObject("messagesURL", getBeginUrl(c));
            addObject("customizeURL", c.hasPermission(user, ACL.PERM_ADMIN) ? getShowCustomizeUrl(c, url) : null);
            addObject("emailPrefsURL", user.isGuest() ? null : getShowEmailPreferencesUrl(c, url));
            addObject("emailManageURL", c.hasPermission(user, ACL.PERM_ADMIN) ? getAdminEmailUrl(c) : null);
            addObject("filterText", filterText);
            addObject("urlFilterText", isFiltered ? urlFilter.getFilterText(
                new SimpleFilter.ColumnNameFormatter()
                {
                    @Override
                    public String format(String columnName)
                    {
                        return super.format(columnName).replaceFirst(".DisplayName", "");
                    }
                }) : null);
        }
    }


    public static class AnnouncementWebPart extends GroovyView
    {
        public AnnouncementWebPart(Container c, ViewURLHelper url, User user, boolean displayAll) throws SQLException, ServletException
        {
            super("/org/labkey/announcements/announcementWebPart.gm");

            Settings settings = getSettings(c);
            setTitle(settings.getBoardName());
            setTitleHref(getBeginUrl(c).getEncodedLocalURIString());

            Permissions perm = getPermissions(c, user, settings);
            SimpleFilter filter = getFilter(settings, perm, displayAll);
            Pair<Announcement[], Boolean> pair = AnnouncementManager.getAnnouncements(c, filter, settings.getSort(), 100);

            addObject("settings", settings);
            addObject("container", c);
            addObject("insertURL", perm.allowInsert() ? getShowInsertUrl(c, url) : null);
            addObject("listURL", getListUrl(c));
            addObject("customizeURL", c.hasPermission(user, ACL.PERM_ADMIN) ? getShowCustomizeUrl(c, url) : null);
            addObject("emailPrefsURL", user.isGuest() ? null : getShowEmailPreferencesUrl(c, url).getEncodedLocalURIString());
            addObject("emailManageURL", c.hasPermission(user, ACL.PERM_ADMIN) ? getAdminEmailUrl(c) : null);
            addObject("announcements", pair.first);
            addObject("filterText", getFilterText(settings, displayAll, false, pair.second ? 100 : 0));
        }

        public AnnouncementWebPart(ViewContext ctx) throws SQLException, ServletException
        {
            this(ctx.getContainer(), ctx.getViewURLHelper(), ctx.getUser(), false);
        }
    }


    private static SimpleFilter getFilter(Settings settings, Permissions perm, boolean displayAll)
    {
        // Filter out threads that this user can't read
        SimpleFilter filter = perm.getThreadFilter();

        if (!displayAll)
        {
            if (settings.hasExpires())
                filter.addWhereClause("Expires IS NULL OR Expires > ?", new Object[]{new Date(System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY)});

            if (settings.hasStatus())
                filter.addCondition("Status", "Closed", CompareType.NEQ_OR_NULL);
        }

        return filter;
    }


    private static String getFilterText(Settings settings, boolean displayAll, boolean isFiltered, int rowLimit)
    {
        StringBuilder sb = new StringBuilder();

        String separator = "";

        if (!displayAll)
        {
            if (settings.hasExpires())
            {
                sb.append("recent");
                separator = ", ";
            }

            if (settings.hasStatus())
            {
                sb.append(separator);
                sb.append("open");
                separator = ", ";
            }
        }

        if (isFiltered)
        {
            sb.append(separator);
            sb.append("filtered");
            separator = ", ";
        }

        if (rowLimit > 0)
        {
            sb.append(separator);
            sb.append("limited to ");
            sb.append(rowLimit);
        }

        if (sb.length() == 0)
            sb.append("all");

        sb.append(" ");
        sb.append(settings.getConversationName().toLowerCase());
        sb.append("s");

        return sb.toString();
    }


    public static class AnnouncementListWebPart extends WebPartView
    {
        private VBox _vbox;

        public AnnouncementListWebPart(ViewContext ctx) throws ServletException
        {
            this(ctx, false);
        }

        private AnnouncementListWebPart(ViewContext ctx, boolean displayAll) throws ServletException
        {
            Container c = ctx.getContainer();
            User user = ctx.getUser();
            ViewURLHelper url = ctx.getViewURLHelper();

            Settings settings = getSettings(c);
            Permissions perm = getPermissions(c, user, settings);
            DataRegion rgn = getDataRegion(perm, settings);

            setTitle(settings.getBoardName() + " List");
            setTitleHref(getListUrl(c).getEncodedLocalURIString());            

            TableInfo tinfo = _comm.getTableInfoThreads();
            DisplayColumn title = new DataColumn(tinfo.getColumn("Title"));
            title.setURL(url.relativeUrl("thread", "rowId=${RowId}", "announcements"));
            rgn.addColumn(title);

            if (settings.hasStatus())
                rgn.addColumn(tinfo.getColumn("Status"));

            if (settings.hasAssignedTo())
            {
                DisplayColumn dc = new UserIdRenderer(tinfo.getColumn("AssignedTo"));
                rgn.addColumn(dc);
            }

            if (settings.hasExpires())
                rgn.addColumn(tinfo.getColumn("Expires"));

            ColumnInfo colCreatedBy = tinfo.getColumn("CreatedBy"); // TODO: setRenderClass?
            DisplayColumn dc = new UserIdRenderer(colCreatedBy);
            rgn.addColumn(dc);

            rgn.addColumn(tinfo.getColumn("Created"));

            ColumnInfo colLastUpdatedBy = tinfo.getColumn("ResponseCreatedBy"); // TODO: setRenderClass?
            DisplayColumn lastDc = new UserIdRenderer(colLastUpdatedBy);
            rgn.addColumn(lastDc);

            rgn.addColumn(tinfo.getColumn("ResponseCreated"));

            GridView gridView = new GridView(rgn);
            gridView.setTitle(null);  // Prevent double title
            gridView.setContainer(c);
            gridView.setSort(settings.getSort());

            SimpleFilter filter = getFilter(settings, perm, displayAll);
            gridView.setFilter(filter);

            setTitle(settings.getBoardName() + " List");

            _vbox = new VBox(new AnnouncementListLinkBar(c, url, user, settings, perm, displayAll), gridView);
        }

        protected DataRegion getDataRegion(Permissions perm, Settings settings)
        {
            DataRegion rgn = new DataRegion();
            rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY);
            rgn.setShadeAlternatingRows(true);
            return rgn;
        }

        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            include(_vbox);
        }
    }


    public static class AnnouncementListView extends AnnouncementListWebPart
    {
        public AnnouncementListView(ViewContext ctx) throws ServletException
        {
            super(ctx, true);
        }

        @Override
        protected DataRegion getDataRegion(Permissions perm, Settings settings)
        {
            DataRegion rgn = super.getDataRegion(perm, settings);

            if (perm.allowDeleteThread())
            {
                ButtonBar bb = new ButtonBar();
                rgn.setShowRecordSelectors(true);

                bb.add(ActionButton.BUTTON_SELECT_ALL);
                bb.add(ActionButton.BUTTON_CLEAR_ALL);

                String conversations = settings.getConversationName().toLowerCase() + "s";
                ActionButton delete = new ActionButton("button", "Delete");
                delete.setScript("return verifySelected(this.form, \"delete.post\", \"post\", \"" + conversations + "\", \"Are you sure you want to delete these " + conversations + "?\")");
                delete.setActionType(ActionButton.Action.GET);
                delete.setDisplayPermission(ACL.PERM_DELETE);
                bb.add(delete);

                rgn.setButtonBar(bb);
            }
            else
                rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY);

            return rgn;
        }
    }


    public static class ThreadViewBean
    {
        public Announcement announcement;
        public String message = "";
        public Permissions perm = null;
        public boolean isResponse = false;
        public Settings settings;
        public String messagesURL;
        public ViewURLHelper listURL;
        public String printURL;
        public boolean print = false;
    }


    public static class ThreadView extends JspView<ThreadViewBean>
    {
        private ThreadView()
        {
            super("/org/labkey/announcements/announcementThread.jsp", new ThreadViewBean());
        }

        public ThreadView(Container c, User user, String rowId, String entityId) throws ServletException
        {
            this();
            init(c, findThread(c, rowId, entityId), null, getPermissions(c, user, getSettings(c)), false, false);
        }

        public ThreadView(Container c, ViewURLHelper url, Announcement ann, Permissions perm) throws ServletException
        {
            this();
            init(c, ann, url, perm, true, false);
        }
        
        public ThreadView(AnnouncementForm form, Container c, ViewURLHelper url, Permissions perm, boolean print)
                throws ServletException
        {
            this();
            Announcement ann = findThread(c, (String)form.get("rowId"), (String)form.get("entityId"));
            init(c, ann, url, perm, false, print);
        }

        protected void init(Container c, Announcement ann, ViewURLHelper url, Permissions perm, boolean isResponse, boolean print)
                throws ServletException
        {
            if (null == c || !perm.allowRead(ann))
                HttpView.throwUnauthorized();

            if (ann instanceof AnnouncementManager.BareAnnouncement)
                throw new IllegalArgumentException("can't use getBareAnnoucements() with this view");

            ThreadViewBean bean = getModelBean();
            bean.announcement = ann;
            bean.settings = getSettings(c);
            bean.message = null;
            bean.perm = perm;
            bean.isResponse = isResponse;
            bean.messagesURL = getBeginUrl(c).getEncodedLocalURIString();
            bean.listURL = getListUrl(c);
            bean.printURL = null == url ? null : url.clone().replaceParameter("print", "1").getEncodedLocalURIString();
            bean.print = print;

            setTitle("View " + bean.settings.getConversationName());
        }

        public Announcement getAnnouncement()
        {
            return getModelBean().announcement;
        }
    }


    private static Announcement findThread(Container c, String rowIdVal, String entityId)
    {
        int rowId = 0;
        if (rowIdVal != null)
        {
            try
            {
                rowId = Integer.parseInt(rowIdVal);
            }
            catch(NumberFormatException e)
            {
                throwThreadNotFound(c);
            }
        }

        try
        {
            if (0 != rowId)
                return AnnouncementManager.getAnnouncement(c, rowId, AnnouncementManager.INCLUDE_ATTACHMENTS + AnnouncementManager.INCLUDE_RESPONSES + AnnouncementManager.INCLUDE_MEMBERLIST);
            else if (null == entityId)
                throwThreadNotFound(c);

            return AnnouncementManager.getAnnouncement(c, entityId, true);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }

    


    public static class AnnouncementUpdateView extends GroovyView
    {
        private Announcement _announcement = null;

        public AnnouncementUpdateView(AnnouncementForm form, Announcement ann)
        {
            super("/org/labkey/announcements/announcementUpdate.gm");

            addObject("currentRendererType", WikiRendererType.valueOf(ann.getRendererType()));
            addObject("renderers", WikiRendererType.values());

            if (ann.getParent() == null)
            {
                setTitle("Edit Message");
            }
            else
            {
                setTitle("Edit Response");
            }
            _announcement = ann;
            addObject("announcement", _announcement);
            addObject("form", form);

            String reshowEmailList = (String)form.get("emailList");
            addObject("memberList", getMemberListTextArea(form.getUser(), form.getContainer(), _announcement, null != reshowEmailList ? reshowEmailList : null));

        }

        @Override
        public void prepareWebPart(Object model) throws ServletException
        {
            Container c = getViewContext().getContainer();
            Settings settings = getSettings(c);

            addObject("announcement", _announcement);
            DownloadUrlHelper attachmentURL = new DownloadUrlHelper(getViewContext().getRequest(), "announcements", c.getPath(), _announcement.getEntityId(), null);
            attachmentURL.setAction("showAddAttachment.view");
            addObject("addAttachmentURL", attachmentURL.getLocalURIString());
            attachmentURL.setAction("showConfirmDelete.view");
            addObject("deleteURLHelper", attachmentURL);
            addObject("settings", settings);
            addObject("statusSelect", getStatusSelect(settings, _announcement.getStatus()));
            addObject("assignedToSelect", getAssignedToSelect(c, _announcement.getAssignedTo(), "assignedTo"));
        }
    }

    //
    // Hibernate session handling
    //

//    private transient Object _s = null;

    Object openSession()
    {
//        if (null == _s)
//            _s = AnnouncementManager.openSession();
//        return _s;
        return null;
    }


    void closeSession()
    {
//        if (null != _s)
//            _s.close();
//        _s = null;
    }


    protected synchronized void afterAction() throws Exception
    {
        super.afterAction();
        closeSession();
    }

    public static class GroupMembershipDisplayColumn extends SimpleDisplayColumn
    {
        private List<User> _memberList;

        public GroupMembershipDisplayColumn(Container c)
        {
            super();
            _memberList = SecurityManager.getProjectMembers(c.getProject(), false);
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            Map row = ctx.getRow();

            Container c = (Container) ctx.get("container");
            //resultset query represents union between two queries, so not all rows include container value
            if (c == null)
                out.write("Yes");
            else
            {
                int userId = (Integer)row.get("UserId");

                if (_memberList.contains(UserManager.getUser(userId)))
                    out.write("Yes");
                else
                    out.write("No");
            }
        }
    }
}
