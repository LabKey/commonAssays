package announcements;

import org.fhcrc.cpas.jsp.JspBase;
import org.fhcrc.cpas.announcements.Announcement;
import org.fhcrc.cpas.announcements.AnnouncementManager;


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
