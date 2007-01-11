package announcements.model;

import org.fhcrc.cpas.announcements.Announcement;
import org.fhcrc.cpas.data.SimpleFilter;

/**
 * User: adam
 * Date: Nov 1, 2006
 * Time: 4:45:34 PM
 */
public interface Permissions
{
    public boolean allowResponse(Announcement ann);
    public boolean allowRead(Announcement ann);
    public boolean allowInsert();
    public boolean allowUpdate(Announcement ann);
    public boolean allowDeleteMessage(Announcement ann);
    public boolean allowDeleteThread();
    public SimpleFilter getThreadFilter();
}
