package announcements;

import org.labkey.api.jsp.JspBase;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.announcements.AnnouncementManager;


abstract public class EmailResponsePage extends JspBase
    {
    public String threadURL;
    public String boardPath;
    public String boardURL;
    public String srcURL;
    public String siteURL;
    public String cssURL;
    public Announcement responseAnnouncement;
    public String responseBody;
    public AnnouncementManager.Settings settings;
    public String removeUrl;
    public Reason reason;

    public static enum Reason { broadcast, signedUp, userList }
    }
