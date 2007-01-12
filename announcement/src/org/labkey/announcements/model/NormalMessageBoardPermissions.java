package org.labkey.announcements.model;

import org.labkey.api.announcements.Announcement;
import org.labkey.api.announcements.CommSchema;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;

/**
 * User: adam
 * Date: Nov 16, 2006
 * Time: 9:54:48 AM
 */

public class NormalMessageBoardPermissions implements Permissions
{
    protected Container _c;
    protected User _user;
    protected static CommSchema _comm = CommSchema.getInstance();

    public NormalMessageBoardPermissions(Container c, User user)
    {
        _c = c;
        _user = user;
    }

    public boolean allowRead(Announcement ann)
    {
        return hasPermission(ACL.PERM_READ);
    }

    public boolean allowInsert()
    {
        return hasPermission(ACL.PERM_INSERT);
    }

    public boolean allowResponse(Announcement ann)
    {
        return hasPermission(ACL.PERM_INSERT);
    }

    public boolean allowUpdate(Announcement ann)
    {
        // Either current user has update permissions on this container OR
        //   current user: is not a guest, has "update own" permissions, and created this message
        return hasPermission(ACL.PERM_UPDATE) ||
               (!_user.isGuest() && hasPermission(ACL.PERM_UPDATEOWN) && ann.getCreatedBy() == _user.getUserId());
    }

    public boolean allowDeleteMessage(Announcement ann)
    {
        // Either current user has delete permissions on this container OR
        //   current user: is not a guest, has "delete own" permissions, and created this message
        return hasPermission(ACL.PERM_DELETE) ||
               (!_user.isGuest() && hasPermission(ACL.PERM_DELETEOWN) && ann.getCreatedBy() == _user.getUserId());
    }

    public boolean allowDeleteThread()
    {
        return hasPermission(ACL.PERM_DELETE);
    }


    public SimpleFilter getThreadFilter()
    {
        return new SimpleFilter();
    }

    protected boolean hasPermission(int perm)
    {
        return _c.hasPermission(_user, perm);
    }
}