package announcements.model;

import org.fhcrc.cpas.announcements.Announcement;
import org.fhcrc.cpas.announcements.AnnouncementManager;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.Filter;
import org.fhcrc.cpas.data.SimpleFilter;
import org.fhcrc.cpas.security.ACL;
import org.fhcrc.cpas.security.User;

/**
 * User: adam
 * Date: Nov 16, 2006
 * Time: 9:56:01 AM
 */
public class SecureMessageBoardPermissions extends NormalMessageBoardPermissions
{
    protected final static int EDITOR_PERM = org.fhcrc.cpas.security.SecurityManager.PermissionSet.EDITOR.getPermissions();
    protected AnnouncementManager.Settings _settings;

    public SecureMessageBoardPermissions(Container c, User user, AnnouncementManager.Settings settings)
    {
        super(c, user);
        _settings = settings;
    }

    public boolean allowRead(Announcement ann)
    {
        // Editors can read all messages
        if (hasPermission(EDITOR_PERM))
            return true;

        // If not an editor, message board must have a user list, user must be on it, and user must have read permissions
        return _settings.hasUserList() && hasPermission(ACL.PERM_READ) && ann.getUserList().contains(_user);
    }

    public boolean allowDeleteMessage(Announcement ann)
    {
        return false;
    }

    public boolean allowDeleteThread()
    {
        return false;
    }

    public boolean allowResponse(Announcement ann)
    {
        // Editors can respond to any message
        if (hasPermission(EDITOR_PERM))
            return true;

        // If not an editor, message board must have a user list, user must be on it, and user must have insert permissions
        return _settings.hasUserList() && hasPermission(ACL.PERM_INSERT) && ann.getUserList().contains(_user);
    }

    public boolean allowUpdate(Announcement ann)
    {
        return false;
    }

    public SimpleFilter getThreadFilter()
    {
        SimpleFilter filter = new SimpleFilter();

        if (!hasPermission(EDITOR_PERM))
            filter.addWhereClause("RowId IN (SELECT MessageId FROM " + _comm.getTableInfoUserList() + " WHERE UserId = ?)", new Object[]{_user.getUserId()});

        return filter;
    }
}