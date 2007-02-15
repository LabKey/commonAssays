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
package org.labkey.announcements.model;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.beehive.netui.pageflow.FormData;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.SecurityManager.PermissionSet;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.ContainerUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.TestContext;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.announcements.CommSchema;
import org.labkey.api.announcements.Announcement;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: mbellew
 * Date: Mar 11, 2005
 * Time: 10:08:26 AM
 */
public class AnnouncementManager
{
    private static CommSchema _comm = CommSchema.getInstance();
    private static CoreSchema _core = CoreSchema.getInstance();

    public static final int EMAIL_PREFERENCE_DEFAULT = -1;
    public static final int EMAIL_PREFERENCE_NONE = 0;
    public static final int EMAIL_PREFERENCE_ALL = 1;
    public static final int EMAIL_PREFERENCE_MINE = 2; //Only threads I've posted to

//    public static final int EMAIL_FORMAT_TEXT = 0;
    public static final int EMAIL_FORMAT_HTML = 1;

    public static final int PAGE_TYPE_MESSAGE = 0;
    public static final int PAGE_TYPE_WIKI = 1;

    private static Logger _log = Logger.getLogger(AnnouncementManager.class);

    private AnnouncementManager()
    {
    }

    protected static void attachAttachments(Announcement[] announcements) throws SQLException
    {
        for (Announcement announcement : announcements)
        {
            Attachment[] att = AttachmentService.get().getAttachments(announcement);
            announcement.setAttachments(Arrays.asList(att));
        }
    }


    protected static void attachResponses(Container c, Announcement[] announcements) throws SQLException
    {
        for (Announcement announcement : announcements)
        {
            Announcement[] res = getAnnouncements(c, announcement.getEntityId());
            announcement.setResponses(Arrays.asList(res));
        }
    }


    protected static void attachMemberLists(Announcement[] announcements) throws SQLException
    {
        for (Announcement announcement : announcements)
            announcement.setMemberList(getMemberList(announcement));
    }


    // Get all threads in this container, filtered using filter
    public static Announcement[] getAnnouncements(Container c, SimpleFilter filter, Sort sort)
    {
        filter.addCondition("Container", c.getId());

        try
        {
            Announcement[] recent = Table.select(_comm.getTableInfoThreads(), Table.ALL_COLUMNS, filter, sort, Announcement.class);
            attachAttachments(recent);
            attachResponses(c, recent);
            return recent;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    // marker for non fully loaded announcement
    public static class BareAnnouncement extends Announcement
    {
    }


    // Get all threads in this container, filtered using filter, no attachments, no responses
    public static Announcement[] getBareAnnouncements(Container c, SimpleFilter filter, Sort sort)
    {
        filter.addCondition("Container", c.getId());

        try
        {
            Announcement[] recent = Table.select(_comm.getTableInfoThreads(), Table.ALL_COLUMNS, filter, sort, BareAnnouncement.class);
            return recent;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    public static Announcement[] getAnnouncements(Container c, String parent)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("container", c.getId());
        if (null == parent)
            filter.addCondition("parent", null, CompareType.ISBLANK);
        else
            filter.addCondition("parent", parent);

        Sort sort = new Sort("Created");

        try
        {
            Announcement[] ann = Table.select(_comm.getTableInfoAnnouncements(),
                    Table.ALL_COLUMNS, filter, sort, Announcement.class);

            attachAttachments(ann);
            return ann;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    public static Announcement getAnnouncement(Container c, String entityId) throws SQLException
    {
        return getAnnouncement(c, entityId, false);
    }


    public static Announcement getAnnouncement(Container c, String entityId, boolean eager) throws SQLException
    {
        Announcement[] ann = Table.select(_comm.getTableInfoAnnouncements(), Table.ALL_COLUMNS,
                new SimpleFilter("container", c.getId()).addCondition("entityId", entityId),
                null, Announcement.class);
        if (ann.length < 1)
            return null;
        attachAttachments(ann);
        if (eager)
        {
            attachResponses(c, ann);
            attachMemberLists(ann);
        }
        return ann[0];
    }


    public static Announcement getAnnouncement(Container c, int rowId) throws SQLException
    {
        return getAnnouncement(c, rowId, INCLUDE_ATTACHMENTS);
    }

    public static void saveEmailPreference(User user, Container c, int emailPreference) throws SQLException
    {
        saveEmailPreference(user, c, user, emailPreference);
    }

    public static synchronized void saveEmailPreference(User currentUser, Container c, User projectUser, int emailPreference) throws SQLException
    {
        //determine whether user has record in EmailPrefs table.
        EmailPref emailPref = getUserEmailPrefRecord(c, projectUser);

        //insert new if user preference record does not yet exist
        if(null == emailPref && emailPreference != EMAIL_PREFERENCE_DEFAULT)
        {
            emailPref = new EmailPref();
            emailPref.setContainer(c.getId());
            emailPref.setUserId(projectUser.getUserId());
            emailPref.setEmailFormatId(EMAIL_FORMAT_HTML);
            emailPref.setEmailOptionId(emailPreference);
            emailPref.setPageTypeId(PAGE_TYPE_MESSAGE);
            emailPref.setLastModifiedBy(currentUser.getUserId());
            Table.insert(currentUser, _comm.getTableInfoEmailPrefs(), emailPref);
        }
        else
        {
            if (emailPreference == EMAIL_PREFERENCE_DEFAULT)
            {
                //if preference has been set back to default, delete user's email pref record
                SimpleFilter filter = new SimpleFilter();
                filter.addCondition("UserId", projectUser.getUserId());
                Table.delete(_comm.getTableInfoEmailPrefs(), filter);
            }
            else
            {
                //otherwise update if it already exists
                emailPref.setEmailOptionId(emailPreference);
                emailPref.setLastModifiedBy(currentUser.getUserId());
                Table.update(currentUser, _comm.getTableInfoEmailPrefs(), emailPref,
                        new Object[]{c.getId(), projectUser.getUserId()}, null);
            }
        }
    }


    public static final int INCLUDE_ATTACHMENTS = 1;
    public static final int INCLUDE_RESPONSES = 2;
    public static final int INCLUDE_MEMBERLIST = 4;

    public static Announcement getAnnouncement(Container c, int rowId, int mask) throws SQLException
    {
        Announcement[] ann = Table.select(_comm.getTableInfoAnnouncements(), Table.ALL_COLUMNS,
                new SimpleFilter("container", c.getId()).addCondition("rowId", new Integer(rowId)),
                null, Announcement.class);
        if (ann.length < 1)
            return null;

        if ((mask & INCLUDE_ATTACHMENTS) != 0)
            attachAttachments(ann);
        if ((mask & INCLUDE_RESPONSES) != 0)
            attachResponses(c, ann);
        if ((mask & INCLUDE_MEMBERLIST) != 0)
            attachMemberLists(ann);
        return ann[0];
    }


    public static Announcement getLatestPost(Container c, Announcement parent)
    {
        try
        {
            Integer postId = Table.executeSingleton(_comm.getSchema(), "SELECT LatestId FROM " + _comm.getTableInfoThreads() + " WHERE RowId=?", new Object[]{parent.getRowId()}, Integer.class);

            if (null == postId)
                throw new NotFoundException("Can't find most recent post");

            return getAnnouncement(c, postId, INCLUDE_MEMBERLIST);
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    public static void insertAnnouncement(Container c, User user, Announcement insert, FormFile[] files) throws SQLException, IOException
    {
        insert.beforeInsert(user, c.getId());
        Announcement ann = Table.insert(user, _comm.getTableInfoAnnouncements(), insert);

        List<User> users = ann.getMemberList();
        
        if (users != null)
        {
            // Always attach member list to initial message
            int rowId = (null == ann.getParent() ? ann.getRowId() : getParentRowId(ann));
            insertMemberList(user, ann.getMemberList(), rowId);
        }

        if (null != files && 0 < files.length)
            AttachmentService.get().addAttachments(user, insert, files);
    }


    private static synchronized void insertMemberList(User user, List<User> users, int messageId) throws SQLException
    {
        // TODO: Should delete/insert only on diff
        if (null != users)
        {
            Table.delete(_comm.getTableInfoMemberList(), new SimpleFilter("MessageId", messageId));

            for (User u : users)
                Table.insert(user, _comm.getTableInfoMemberList(), PageFlowUtil.map("MessageId", messageId, "UserId", u.getUserId()));
        }
    }


    private static int getParentRowId(Announcement ann) throws SQLException
    {
        return Table.executeSingleton(_comm.getSchema(), "SELECT RowId FROM " + _comm.getTableInfoAnnouncements() + " WHERE EntityId=?", new Object[]{ann.getParent()}, Integer.class);
    }


    private static List<User> getMemberList(Announcement ann) throws SQLException
    {
        Integer[] userIds;

        if (null == ann.getParent())
            userIds = Table.executeArray(_comm.getSchema(), "SELECT UserId FROM " + _comm.getTableInfoMemberList() + " WHERE MessageId = ?", new Object[]{ann.getRowId()}, Integer.class);
        else
            userIds = Table.executeArray(_comm.getSchema(), "SELECT UserId FROM " + _comm.getTableInfoMemberList() + " WHERE MessageId = (SELECT RowId FROM " + _comm.getTableInfoAnnouncements() + " WHERE EntityId=?)", new Object[]{ann.getParent()}, Integer.class);

        List<User> users = new ArrayList<User>(userIds.length);

        for (int userId : userIds)
            users.add(UserManager.getUser(userId));

        return users;
    }


    public static void updateAnnouncement(User user, Announcement update) throws SQLException
    {
        update.beforeUpdate(user);
        Table.update(user, _comm.getTableInfoAnnouncements(), update, new Integer(update.getRowId()), null);
    }


    private static void deleteAnnouncement(Announcement ann) throws SQLException
    {
        Table.delete(_comm.getTableInfoAnnouncements(), ann.getRowId(), null);
        AttachmentService.get().deleteAttachments(ann);
    }


    public static void deleteAnnouncement(Container c, int rowId) throws SQLException
    {
        DbSchema schema = _comm.getSchema();

        try
        {
            schema.getScope().beginTransaction();
            Announcement ann = getAnnouncement(c, rowId, INCLUDE_RESPONSES);
            if (ann != null)
            {
                deleteAnnouncement(ann);

                // Delete the member list associated witth this thread
                Table.delete(_comm.getTableInfoMemberList(), new SimpleFilter("MessageId", ann.getRowId()));

                Collection<Announcement> responses = ann.getResponses();
                if (null == responses)
                    return;
                for (Announcement response : responses)
                {
                    deleteAnnouncement(response);
                }
            }
            schema.getScope().commitTransaction();
        }
        finally
        {
            if (schema.getScope().isTransactionActive())
                schema.getScope().rollbackTransaction();
        }
    }

    public static void deleteUserFromAllMemberLists(User user) throws SQLException
    {
        Table.delete(_comm.getTableInfoMemberList(), new SimpleFilter("UserId", user.getUserId()));
    }

    public static void deleteUserFromMemberList(User user, int messageId) throws SQLException
    {
        Table.delete(_comm.getTableInfoMemberList(), new SimpleFilter("UserId", user.getUserId()).addCondition("MessageId", messageId));
    }

    // Return email address for those who signed up for notifications
    public static Set<String> getUserEmailSet(Container c, Announcement a, Settings settings)
            throws SQLException
    {
        Set<String> emailSet = new HashSet<String>();

        // In a non-secure message board, those with read permissions can elect to see all messages or those to which they've posted (or neither)
        // In a secure message board, only those with editor permissions can do this.
        int requiredPerm = settings.isSecure() ? PermissionSet.EDITOR.getPermissions() : ACL.PERM_READ;

        //get set of responders on thread, if this is a response
        boolean isResponse = null != a.getParent();
        Set<User> responderSet = new HashSet<User>();
        if (isResponse)
            responderSet = getResponderSet(c, a);

        //get email preferences for all project users (explicit and implicit)
        EmailPref[] emailPrefs = getEmailPrefs(c, null);
        for (EmailPref emailPref : emailPrefs)
        {
            User user = emailPref.getUser();
            Integer emailOption = emailPref.getEmailOptionId();
            emailOption = emailOption == null ? getProjectEmailOption(c) : emailOption;

            if (emailOption == AnnouncementManager.EMAIL_PREFERENCE_NONE)
                continue;

            // Ensure user has appropriate permissions on container (secure == editor, !secure = read)
            if (!c.hasPermission(user, requiredPerm))
                continue;

            if (emailOption == EMAIL_PREFERENCE_MINE && isResponse)
            {
                if (responderSet.contains(user))
                    emailSet.add(user.getEmail());
            }
            else
                emailSet.add(user.getEmail());
        }

        return emailSet;
    }

    private static Set<User> getResponderSet(Container c, Announcement a) throws SQLException
    {
        Set<User> responderSet = new HashSet<User>();
        boolean isResponse = null != a.getParent();

        //get parent if this is a response.
        if (isResponse)
        {
            Announcement parent = AnnouncementManager.getAnnouncement(c, a.getParent());
            //add creator of parent to responder set
            responderSet.add(UserManager.getUser(parent.getCreatedBy()));
            Collection<Announcement> responses = parent.getResponses();

            //add creator of each response to responder set
            for (Announcement response : responses)
            {
                //do we need to handle case where responder is not in a project group?
                User user = UserManager.getUser(response.getCreatedBy());
                //add to responder set, so we know who responders are
                responderSet.add(user);
            }
        }
        return responderSet;
    }

    public static int getUserEmailOption(Container c, User user) throws SQLException
    {
        EmailPref emailPref = getUserEmailPrefRecord(c, user);

        //user has not yet defined email preference; return project default
        if (emailPref == null)
            return EMAIL_PREFERENCE_DEFAULT;
        else
            return emailPref.getEmailOptionId();
    }

    //returns explicit record from EmailPrefs table for this user if there is one.
    private static EmailPref getUserEmailPrefRecord(Container c, User user) throws SQLException
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("Container", c.getId());
        filter.addCondition("UserId", user.getUserId());

        //return records only for those users who have explicitly set a preference for this container.
        EmailPref[] emailPrefs = Table.select(
                _comm.getTableInfoEmailPrefs(),
                Table.ALL_COLUMNS,
                filter,
                null,
                EmailPref.class
                );

        if (emailPrefs.length == 0)
            return null;
        else
            return emailPrefs[0];
    }

    private static EmailPref[] getEmailPrefs(Container c, User user) throws SQLException
    {
        EmailPref[] emailPrefs;

        //return records for all project users, including those who have not explicitly set preference
        Object[] param;
        String sql = _emailPrefsSql;
        if (user != null)
        {
            //if looking for single user, add in user criteria
            sql += "AND core.Members.UserId = ?";
            param =  new Object[]{c.getId(), c.getProject().getId(), c.getId(), user.getUserId()};
        }
        else
            param = new Object[]{c.getId(), c.getProject().getId(), c.getId()};

        emailPrefs = Table.executeQuery(
                _comm.getSchema(),
                sql,
                param,
                EmailPref.class
                );

        return emailPrefs;
    }

    public static ResultSet getEmailPrefsResultset(Container c) throws SQLException
    {
        Container cProject = c.getProject();
        Object[] param = new Object[]{c.getId(), cProject.getId(), c.getId()};
        return Table.executeQuery(CommSchema.getInstance().getSchema(), _emailPrefsSql, param);
    }

    private static final String _emailPrefsSql;

    static
    {
        StringBuilder sql = new StringBuilder();

        sql.append("(SELECT DISTINCT core.Members.UserId, core.Principals.Name AS Email, core.UsersData.FirstName, core.UsersData.LastName, core.UsersData.DisplayName, ");
        sql.append("\nCASE WHEN comm.EmailPrefs.EmailOptionId IS NULL THEN '<project default>' ELSE comm.EmailOptions.EmailOption END AS EmailOption, comm.EmailOptions.EmailOptionId, ");
        sql.append("\nP1.UserId AS LastModifiedBy, P1.Name AS LastModifiedByName ");
        sql.append("\nFROM core.Members ");
        sql.append("\nLEFT JOIN core.UsersData ON core.Members.UserId = core.UsersData.UserId ");
        sql.append("\nLEFT JOIN comm.EmailPrefs ON core.Members.UserId = comm.EmailPrefs.UserId AND comm.EmailPrefs.Container = ? ");
        sql.append("\nLEFT JOIN comm.EmailOptions ON comm.EmailPrefs.EmailOptionId = comm.EmailOptions.EmailOptionId ");
        sql.append("\nLEFT JOIN core.Principals ON core.Members.UserId = core.Principals.UserId ");
        sql.append("\nLEFT JOIN core.Principals AS P1 ON P1.UserId = comm.EmailPrefs.LastModifiedBy ");
        sql.append("\nWHERE (core.Members.GroupId IN (SELECT UserId FROM core.Principals WHERE Type = 'g' AND Container = ?))) ");
        sql.append("\nUNION ");
        sql.append("\n(SELECT comm.EmailPrefs.UserId, core.Principals.Name AS Email, core.UsersData.FirstName, ");
        sql.append("\ncore.UsersData.LastName, core.UsersData.DisplayName, comm.EmailOptions.EmailOption, comm.EmailPrefs.EmailOptionId, ");
        sql.append("\nP1.UserId AS LastModifiedBy, P1.Name AS LastModifiedByName ");
        sql.append("\nFROM comm.EmailPrefs ");
        sql.append("\nLEFT JOIN core.Principals ON comm.EmailPrefs.UserId = core.Principals.UserId ");
        sql.append("\nLEFT JOIN core.UsersData ON core.Principals.UserId = core.UsersData.UserId ");
        sql.append("\nLEFT JOIN comm.EmailOptions ON comm.EmailPrefs.EmailOptionId = comm.EmailOptions.EmailOptionId ");
        sql.append("\nLEFT JOIN core.Principals AS P1 ON P1.UserId = comm.EmailPrefs.LastModifiedBy ");
        sql.append("\nWHERE comm.EmailPrefs.Container = ?) ");
        sql.append("\nORDER BY Email");

        _emailPrefsSql = sql.toString();
    }


    public static long getMessageCount(Container c)
            throws SQLException
    {
        return Table.executeSingleton(_comm.getSchema(), "SELECT COUNT(*) FROM " + _comm.getTableInfoAnnouncements() + " WHERE Container = ?", new Object[]{c.getId()}, Long.class);
    }

    public static EmailOption[] getEmailOptions() throws SQLException
    {
        return Table.select(_comm.getTableInfoEmailOptions(),
                Table.ALL_COLUMNS,
                null,
                new Sort("EmailOptionId"),
                EmailOption.class
                );
    }

    public static void saveProjectEmailSettings(Container c, int emailOption) throws SQLException
    {
        Map<String, Object> props = PropertyManager.getWritableProperties(0, c.getId(), "defaultEmailSettings", true);
        props.put("defaultEmailOption", emailOption);
        PropertyManager.saveProperties(props);
    }

    public static int getProjectEmailOption(Container c) throws SQLException
    {
        Map<String, Object> props = PropertyManager.getWritableProperties(0, c.getId(), "defaultEmailSettings", false);
        if (props != null && props.size() > 0)
        {
            String option = (String) props.get("defaultEmailOption");
            if (option != null)
                return new Integer(option);
            else
                throw new IllegalStateException("Invalid stored property value.");
        }
        else
            return EMAIL_PREFERENCE_NONE;
    }

    //delete all user records regardless of container
    public static void deleteUserEmailPref(User user, List<Container> containerList) throws SQLException
    {
        if (containerList == null)
        {
            Table.delete(_comm.getTableInfoEmailPrefs(),
                    new SimpleFilter("UserId", user.getUserId()));
        }
        else
        {
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition("UserId", user.getUserId());
            StringBuilder whereClause = new StringBuilder("Container IN (");
            for (int i = 0; i < containerList.size(); i++)
            {
                Container c = containerList.get(i);
                whereClause.append("'");
                whereClause.append(c.getId());
                whereClause.append("'");
                if (i < containerList.size() - 1)
                    whereClause.append(", ");
            }
            whereClause.append(")");
            filter.addWhereClause(whereClause.toString(), null);

            Table.delete(_comm.getTableInfoEmailPrefs(), filter);
        }
    }

    public static class Settings extends FormData
    {
        String _boardName = "Messages";
        String _conversationName = "Message";
        boolean _secure = false;
        boolean _status = false;
        boolean _expires = false;
        boolean _assignedTo = false;
        Integer _defaultAssignedTo = null;
        boolean _formatPicker = false;
        boolean _memberList = false;
        boolean _titleEditable = false;
        SortOrder _sortOrder = SortOrder.getDefaultSortOrder();

        String _statusOptions = "Active;Closed";

        public enum SortOrder
        {
            CreationDate(0, "-Created"), LatestResponseDate(1, "-ResponseCreated");

            private int _index;
            private String _sortString;

            SortOrder(int index, String sortString)
            {
                _index = index;
                _sortString = sortString;
            }

            public int getIndex()
            {
                return _index;
            }

            public Sort getSort()
            {
                return new Sort(_sortString);
            }

            public static SortOrder getByIndex(int index)
            {
                for (SortOrder so : values())
                {
                    if (index == so.getIndex())
                        return so;
                }
                return getDefaultSortOrder();  // Bad index -- just return default
            }

            public static SortOrder getDefaultSortOrder()
            {
                return CreationDate;
            }


            // For convenience, used in customize.jsp
            @Override
            public String toString()
            {
                return String.valueOf(_index);
            }
        }

        // Set the defaults that will be used for un-customized message boards.  We must set them all to false above to
        // workaround the "checkbox doesn't post if false" problem.
        public void setDefaults()
        {
            _expires = true;
            _formatPicker = true;
            _titleEditable = true;
        }

        public String getBoardName()
        {
            return _boardName;
        }

        public void setBoardName(String boardName)
        {
            _boardName = boardName;
        }

        public String getConversationName()
        {
            return _conversationName;
        }

        public void setConversationName(String itemName)
        {
            _conversationName = itemName;
        }

        public boolean isSecure()
        {
            return _secure;
        }

        public void setSecure(boolean secure)
        {
            _secure = secure;
        }

        public boolean hasExpires()
        {
            return _expires;
        }

        public void setExpires(boolean expires)
        {
            _expires = expires;
        }

        public boolean hasFormatPicker()
        {
            return _formatPicker;
        }

        public void setFormatPicker(boolean formatPicker)
        {
            _formatPicker = formatPicker;
        }

        public boolean hasAssignedTo()
        {
            return _assignedTo;
        }

        public void setAssignedTo(boolean assignedTo)
        {
            _assignedTo = assignedTo;
        }

        public Integer getDefaultAssignedTo()
        {
            return _defaultAssignedTo;
        }

        public void setDefaultAssignedTo(Integer defaultAssignedTo)
        {
            _defaultAssignedTo = defaultAssignedTo;
        }

        public boolean hasStatus()
        {
            return _status;
        }

        public void setStatus(boolean status)
        {
            _status = status;
        }

        public boolean hasMemberList()
        {
            return _memberList;
        }

        public void setMemberList(boolean memberList)
        {
            _memberList = memberList;
        }

        // Keep this for backward compatibility with message boards that saved a "userList" setting.  These settings are loaded by reflection.
        @Deprecated
        public boolean hasUserList()
        {
            return hasMemberList();
        }

        // Keep this for backward compatibility with message boards that saved a "userList" setting.  These settings are loaded by reflection.
        @Deprecated
        public void setUserList(boolean memberList)
        {
            setMemberList(memberList);
        }

        public int getSortOrderIndex()
        {
            return _sortOrder.getIndex();
        }

        public void setSortOrderIndex(int index)
        {
            _sortOrder = SortOrder.getByIndex(index);
        }

        public Sort getSort()
        {
            return _sortOrder.getSort();
        }

        public boolean isTitleEditable()
        {
            return _titleEditable;
        }

        public void setTitleEditable(boolean titleEditable)
        {
            _titleEditable = titleEditable;
        }

        public String getStatusOptions()
        {
            return _statusOptions;
        }

        public void setStatusOptions(String options)
        {
            _statusOptions = options;
        }
    }


    private static final String MESSAGE_BOARD_SETTINGS = "messageBoardSettings";

    public static void saveMessageBoardSettings(Container c, Settings settings) throws SQLException, IllegalAccessException, InvocationTargetException
    {
        Map<String, Object> props = PropertyManager.getWritableProperties(0, c.getId(), MESSAGE_BOARD_SETTINGS, true);
        props.put("boardName", settings.getBoardName());
        props.put("conversationName", settings.getConversationName());
        props.put("secure", settings.isSecure());
        props.put("status", settings.hasStatus());
        props.put("expires", settings.hasExpires());
        props.put("assignedTo", settings.hasAssignedTo());
        props.put("formatPicker", settings.hasFormatPicker());
        props.put("memberList", settings.hasMemberList());
        props.put("sortOrderIndex", settings.getSortOrderIndex());
        props.put("defaultAssignedTo", settings.getDefaultAssignedTo());
        props.put("titleEditable", settings.isTitleEditable());
        PropertyManager.saveProperties(props);
    }

    public static Settings getMessageBoardSettings(Container c) throws SQLException, IllegalAccessException, InvocationTargetException
    {
        Map<String, Object> props = PropertyManager.getProperties(0, c.getId(), MESSAGE_BOARD_SETTINGS, false);
        Settings settings = new Settings();
        settings.setDefaults();
        BeanUtils.populate(settings, props);
        return settings;
    }


    public static void purgeContainer(Container c)
    {
        try
        {
            // Attachments are handled by AttachmentServiceImpl
            ContainerUtil.purgeTable(_comm.getTableInfoEmailPrefs(), c, null);
            ContainerUtil.purgeTable(_comm.getTableInfoAnnouncements(), c, null);
        }
        catch (SQLException x)
        {
            _log.error("purgeContainer", x);
        }
    }

    private static String _searchSql;

    static
    {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT child.Container, CASE WHEN parent.Title IS NULL THEN child.Title ELSE parent.Title END AS Title, CASE WHEN parent.RowId IS NULL THEN child.RowId ELSE parent.RowId END AS RowId FROM ");
        sql.append(_comm.getTableInfoAnnouncements());
        sql.append(" child\n    LEFT OUTER JOIN ");
        sql.append(_comm.getTableInfoAnnouncements());
        sql.append(" parent ON child.Parent = parent.EntityId\n    WHERE (child.Body ");
        sql.append(_comm.getSqlDialect().getCaseInsensitiveLikeOperator());
        sql.append(" ? OR (child.Parent IS NULL AND child.Title ");        // Search title only on original messages, not responses -- TODO: Change this since title can change?
        sql.append(_comm.getSqlDialect().getCaseInsensitiveLikeOperator());
        sql.append(" ?)) AND child.Container IN ");

        _searchSql = sql.toString();
    }


    public static MultiMap search(Set<Container> containers, String csvContainerIds, String searchTerm)
    {
        MultiMap map = new MultiValueMap();
        String findTerm = "%" + searchTerm + "%";

        try
        {
            ResultSet rs = Table.executeQuery(_comm.getSchema(), _searchSql + csvContainerIds, new Object[] {findTerm, findTerm});

            while(rs.next())
            {
                String containerId = rs.getString(1);
                Container c = ContainerManager.getForId(containerId);

                StringBuilder link = new StringBuilder("<a href=\"");
                link.append(ViewURLHelper.toPathString("announcements", "thread", c.getPath()));
                link.append("?rowId=");
                link.append(rs.getString(3));
                link.append("\">");
                link.append(PageFlowUtil.filter(rs.getString(2)));
                link.append("</a>");

                map.put(rs.getString(1), link.toString());
            }

            rs.close();
        }
        catch(SQLException e)
        {
            _log.error("announcement search", e);
        }

    return map;
    }

    public static class EmailPref
    {
        String _container;
        int _userId;
        String _email;
        String _firstName;
        String _lastName;
        String _displayName;
        Integer _emailOptionId;
        Integer _emailFormatId;
        String _emailOption;
        Integer _lastModifiedBy;
        String _lastModifiedByName;

        boolean _projectMember;

        int _pageTypeId;

        public String getLastModifiedByName()
        {
            return _lastModifiedByName;
        }

        public void setLastModifiedByName(String lastModifiedByName)
        {
            _lastModifiedByName = lastModifiedByName;
        }

        public Integer getLastModifiedBy()
        {
            return _lastModifiedBy;
        }

        public void setLastModifiedBy(Integer lastModifiedBy)
        {
            _lastModifiedBy = lastModifiedBy;
        }

        public String getEmail()
        {
            return _email;
        }

        public void setEmail(String email)
        {
            _email = email;
        }

        public String getContainer()
        {
            return _container;
        }

        public void setContainer(String container)
        {
            _container = container;
        }

        public String getEmailOption()
        {
            return _emailOption;
        }

        public void setEmailOption(String emailOption)
        {
            _emailOption = emailOption;
        }

        public String getFirstName()
        {
            return _firstName;
        }

        public void setFirstName(String firstName)
        {
            _firstName = firstName;
        }

        public String getLastName()
        {
            return _lastName;
        }

        public void setLastName(String lastName)
        {
            _lastName = lastName;
        }

        public String getDisplayName()
        {
            return _displayName;
        }

        public void setDisplayName(String displayName)
        {
            _displayName = displayName;
        }

        public Integer getEmailFormatId()
        {
            return _emailFormatId;
        }

        public void setEmailFormatId(Integer emailFormatId)
        {
            _emailFormatId = emailFormatId;
        }

        public Integer getEmailOptionId()
        {
            return _emailOptionId;
        }

        public void setEmailOptionId(Integer emailOptionId)
        {
            _emailOptionId = emailOptionId;
        }

        public int getUserId()
        {
            return _userId;
        }

        public void setUserId(int userId)
        {
            _userId = userId;
        }

        public int getPageTypeId()
        {
            return _pageTypeId;
        }

        public void setPageTypeId(int pageTypeId)
        {
            _pageTypeId = pageTypeId;
        }

        public boolean isProjectMember()
        {
            return _projectMember;
        }

        public void setProjectMember(boolean projectMember)
        {
            _projectMember = projectMember;
        }

        public User getUser()
        {
            return UserManager.getUser(_userId);
        }
    }

    public static class EmailOption
    {
        int _emailOptionId;
        String _emailOption;

        public String getEmailOption()
        {
            return _emailOption;
        }

        public void setEmailOption(String emailOption)
        {
            _emailOption = emailOption;
        }

        public int getEmailOptionId()
        {
            return _emailOptionId;
        }

        public void setEmailOptionId(int emailOptionId)
        {
            _emailOptionId = emailOptionId;
        }
    }

    public static class TestCase extends junit.framework.TestCase
    {
        public TestCase()
        {
            super();
        }


        public TestCase(String name)
        {
            super(name);
        }


        private void purgeAnnouncements(Container c, boolean verifyEmpty) throws SQLException
        {
            String deleteDocuments = "DELETE FROM " + _core.getTableInfoDocuments() + " WHERE Container = ? AND Parent IN (SELECT EntityId FROM " + _comm.getTableInfoAnnouncements() + " WHERE Container = ?)";
            int docs = Table.execute(_comm.getSchema(), deleteDocuments, new Object[]{c.getId(), c.getId()});
            String deleteAnnouncements = "DELETE FROM " + _comm.getTableInfoAnnouncements() + " WHERE Container = ?";
            int pages = Table.execute(_comm.getSchema(), deleteAnnouncements, new Object[]{c.getId()});
            if (verifyEmpty)
            {
                assertEquals(0, docs);
                assertEquals(0, pages);
            }
        }


        public void testAnnouncements()
                throws SQLException, ServletException, IOException
        {
            TestContext context = TestContext.get();

            User user = context.getUser();
            assertTrue("login before running this test", null != user);
            assertFalse("login before running this test", user.isGuest());

            Container c = JunitUtil.getTestContainer();

            purgeAnnouncements(c, false);

            int rowA;
            int rowResponse;
            {
                Announcement a = new Announcement();
                a.setTitle("new announcement");
                a.setBody("look at this");
                AnnouncementManager.insertAnnouncement(c, user, a, null);
                rowA = a.getRowId();
                assertTrue(0 != rowA);

                Announcement response = new Announcement();
                response.setParent(a.getEntityId());
                response.setTitle("response");
                response.setBody("bah");
                AnnouncementManager.insertAnnouncement(c, user, response, null);
                rowResponse = response.getRowId();
                assertTrue(0 != rowResponse);
            }

            {
                Announcement a = AnnouncementManager.getAnnouncement(c, rowA, INCLUDE_ATTACHMENTS + INCLUDE_RESPONSES);
                assertNotNull(a);
                assertEquals("new announcement", a.getTitle());
                Announcement[] responses = a.getResponses().toArray(new Announcement[0]);
                assertEquals(1, responses.length);
                Announcement response = responses[0];
                assertEquals(a.getEntityId(), response.getParent());
                assertEquals("response", response.getTitle());
                assertEquals("bah", response.getBody());
            }

            {
                // this test makes more sense if/when getParent() return an Announcement
                Announcement response = AnnouncementManager.getAnnouncement(c, rowResponse);
                assertNotNull(response.getParent());
            }

            {
                AnnouncementManager.deleteAnnouncement(c, rowA);
                assertNull(AnnouncementManager.getAnnouncement(c, rowA));
            }

            // UNDONE: attachments, update, responses, ....

            purgeAnnouncements(c, true);
        }


        public static Test suite()
        {
            return new TestSuite(TestCase.class);
        }
    }
}
