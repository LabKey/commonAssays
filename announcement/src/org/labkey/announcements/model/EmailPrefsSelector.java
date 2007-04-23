package org.labkey.announcements.model;

import org.labkey.announcements.AnnouncementsController;
import org.labkey.announcements.model.AnnouncementManager.EmailPref;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: adam
 * Date: Mar 4, 2007
 * Time: 9:14:51 PM
 */
public abstract class EmailPrefsSelector
{
    // All project users' preferences plus anyone else who's signed up for notifications from this board.
    // Default option is set if this user has not indicated a preference.  Prefs with NONE have been removed.
    protected List<EmailPref> _emailPrefs;
    protected Container _c;

    protected EmailPrefsSelector(Container c) throws SQLException
    {
        initEmailPrefs(c);
        _c = c;
    }


    // Initialize list of email preferences: get all settings from the database, add the default values, and remove NONE.
    private void initEmailPrefs(Container c) throws SQLException
    {
        int defaultOption = AnnouncementManager.getProjectEmailOption(c);
        EmailPref[] epArray = AnnouncementManager.getEmailPrefs(c, null);
        _emailPrefs = new ArrayList<EmailPref>(epArray.length);

        for (EmailPref ep : epArray)
        {
            if (null == ep.getEmailOptionId())
                ep.setEmailOptionId(defaultOption);

            if (includeEmailPref(ep))
                _emailPrefs.add(ep);
        }
    }


    // Override this to filter out other prefs
    protected boolean includeEmailPref(EmailPref ep)
    {
        return AnnouncementManager.EMAIL_PREFERENCE_NONE != ep.getEmailOptionId();
    }


    // All users with an email preference -- they have not been authorized!
    // TODO: I don't like exposing the email prefs to callers -- should probably return a list of Users and create a User -> EmailPref map for shouldSend() to use
    public List<EmailPref> getEmailPrefs()
    {
        return _emailPrefs;
    }


    // All users with an email preference plus project users who get the default email preference -- they have not been authorized!
    public Collection<User> getUsers()
    {
        Set<User> users = new HashSet<User>(_emailPrefs.size());

        for (EmailPref ep : _emailPrefs)
            users.add(ep.getUser());

        return users;
    }


    protected boolean shouldSend(Announcement ann, EmailPref ep) throws ServletException, SQLException
    {
        int emailPreference = ep.getEmailOptionId() & AnnouncementManager.EMAIL_PREFERENCE_MASK;

        User user = ep.getUser();
        AnnouncementManager.Settings settings = AnnouncementsController.getSettings(_c);

        if (AnnouncementManager.EMAIL_PREFERENCE_MINE == emailPreference)
        {
            Set<User> responderSet = AnnouncementManager.getResponderSet(_c, ann);

            if (!responderSet.contains(user))
                if (!settings.hasMemberList() || !ann.getMemberList().contains(user))
                    return false;
        }
        else
            assert AnnouncementManager.EMAIL_PREFERENCE_ALL == emailPreference;

        Permissions perm = AnnouncementsController.getPermissions(_c, user, settings);

        return perm.allowRead(ann);
    }
}
