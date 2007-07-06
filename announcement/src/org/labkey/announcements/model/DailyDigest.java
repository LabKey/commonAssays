package org.labkey.announcements.model;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.labkey.announcements.AnnouncementsController;
import org.labkey.announcements.DailyDigestPage;
import org.labkey.announcements.model.AnnouncementManager.Settings;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.announcements.CommSchema;
import org.labkey.api.data.*;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.*;
import org.labkey.api.util.MailHelper.ViewMessage;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.WebPartView;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;

/**
 * User: adam
 * Date: Feb 16, 2007
 * Time: 9:17:12 AM
 */
public class DailyDigest
{
    private static final String SET_KEY = "DailyDigest";
    private static final String LAST_KEY = "LastSuccessfulSend";
    private static CommSchema _comm = CommSchema.getInstance();
    private static CoreSchema _core = CoreSchema.getInstance();
    private static Timer _timer = null;
    private static DailyDigestTask _timerTask = null;

    private static final Logger _log = Logger.getLogger(DailyDigest.class);

    // Used only for AnnouncementsController test action -- remove when test action is removed
    public static void sendDailyDigest() throws Exception
    {
        sendDailyDigest(AppProps.getInstance().createMockRequest());
    }


    private static void sendDailyDigest(HttpServletRequest request) throws Exception
    {
        Date min = getLastSuccessful();
        Date current = new Date();

        if (null == min)
            min = getMidnight(current, -1, 0);  // If nothing is set, start yesterday morning at midnight

        Date max = getMidnight(current, 0, 0);  // Until midnight this morning 

        List<Container> containers = getContainersWithNewMessages(min, max);

        for (Container c : containers)
            sendDailyDigest(request, c, min, max);

        setLastSuccessful(max);
    }


    // Calculate midnight of date entered
    private static Date getMidnight(Date date, int addDays, int addMinutes)
    {
        Calendar current = Calendar.getInstance();

        current.setTime(date);
        current.add(Calendar.DATE, addDays);
        current.set(Calendar.HOUR_OF_DAY, 0);  // Midnight
        current.set(Calendar.MINUTE, addMinutes);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);

        return current.getTime();
    }


    private static Date getLastSuccessful()
    {
        Map<String, String> props = PropertyManager.getProperties(ContainerManager.getRoot().getId(), SET_KEY, true);
        String value = props.get(LAST_KEY);
        return null != value ? new Date(Long.parseLong(value)) : null;
    }


    private static void setLastSuccessful(Date last)
    {
        Map<String, String> props = PropertyManager.getWritableProperties(0, ContainerManager.getRoot().getId(), SET_KEY, true);
        props.put(LAST_KEY, String.valueOf(last.getTime()));
        PropertyManager.saveProperties(props);
    }


    private static List<Container> getContainersWithNewMessages(Date min, Date max) throws SQLException
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT(Container) FROM " + _comm.getTableInfoAnnouncements() + " WHERE Created >= ? and Created < ?", min, max);
        String[] containerIds = Table.executeArray(_comm.getSchema(), sql, String.class);

        List<Container> containers = new ArrayList<Container>(containerIds.length);

        for (String id : containerIds)
            containers.add(ContainerManager.getForId(id));

        return containers;
    }


    private static void sendDailyDigest(HttpServletRequest request, Container c, Date min, Date max) throws Exception
    {
        Settings settings = AnnouncementManager.getMessageBoardSettings(c);
        Announcement[] announcements = getRecentAnnouncementsInContainer(c, min, max);

        DailyDigestEmailPrefsSelector sel = new DailyDigestEmailPrefsSelector(c);

        for (User user : sel.getUsers())
        {
            List<Announcement> announcementList = new ArrayList<Announcement>(announcements.length);

            for (Announcement ann : announcements)
                if (sel.shouldSend(ann, user))
                    announcementList.add(ann);

            if (!announcementList.isEmpty())
            {
                ViewMessage m = getDailyDigestMessage(request, c, settings, announcementList, user);

                try
                {
                    MailHelper.send(m);
                }
                catch (MessagingException e)
                {
                    // Just record these exceptions to the local log (don't send to mothership)
                    _log.error(e.getMessage());
                }
            }
        }
    }


    private static MailHelper.ViewMessage getDailyDigestMessage(HttpServletRequest request, Container c, Settings settings, List<Announcement> announcements, User user) throws Exception
    {
        MailHelper.ViewMessage m = MailHelper.createMultipartViewMessage(AppProps.getInstance().getSystemEmailAddress(), user.getEmail());
        m.setSubject("New posts to " + c.getPath());

        DailyDigestPage page = createPage("dailyDigestPlain.jsp", request, c, settings, announcements);
        JspView view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(request, view, "text/plain");

        page = createPage("dailyDigest.jsp", request, c, settings, announcements);
        view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(request, view, "text/html");

        return m;
    }


    private static DailyDigestPage createPage(String templateName, HttpServletRequest request, Container c, Settings settings, List<Announcement> announcements) throws ServletException
    {
        DailyDigestPage page = (DailyDigestPage) JspLoader.createPage(request, AnnouncementsController.class, templateName);

        page.conversationName = settings.getConversationName().toLowerCase();
        page.settings = settings;
        page.c = c;
        page.announcements = announcements;
        page.boardPath = c.getPath();
        ViewURLHelper boardUrl = new ViewURLHelper(request, "announcements", "begin", c.getPath());
        page.boardUrl = boardUrl.getURIString();
        page.siteUrl = ViewURLHelper.getBaseServerURL(request);
        page.removeUrl = new ViewURLHelper(request, "announcements", "showEmailPreferences", c.getPath()).getURIString();

        URLHelper cssURL = new URLHelper(request);
        cssURL.setPath("/core/stylesheet.view");
        cssURL.setRawQuery(null);
        page.cssURL = cssURL.getURIString();

        return page;
    }


    // Retrieve from this container all messages with a body or attachments posted during the given timespan
    // Messages are grouped by thread and threads are sorted by earliest post within each thread
    private static final String RECENT_ANN_SQL = "SELECT ann.* FROM\n" +
            "\t(\n" +
            "\tSELECT Thread, MIN(Created) AS Earliest FROM\n" +
            "\t\t(SELECT Created, CASE WHEN Parent IS NULL THEN EntityId ELSE Parent END AS Thread FROM " + _comm.getTableInfoAnnouncements() + " ann LEFT OUTER JOIN\n" +
            "\t\t\t(SELECT DISTINCT(Parent) AS DocParent FROM " + _core.getTableInfoDocuments() + ") doc ON ann.EntityId = DocParent\n" +
            "\t\t\tWHERE Container = ? AND Created >= ? AND Created < ? AND (Body IS NOT NULL OR DocParent IS NOT NULL)) x\n" +
            "\tGROUP BY Thread\n" +
            "\t) X LEFT OUTER JOIN " + _comm.getTableInfoAnnouncements() + " ann ON Parent = Thread OR EntityId = Thread LEFT OUTER JOIN\n" +
            "\t\t(SELECT DISTINCT(Parent) AS DocParent FROM " + _core.getTableInfoDocuments() + ") doc ON ann.EntityId = DocParent\n" + 
            "WHERE Container = ? AND Created >= ? AND Created < ? AND (Body IS NOT NULL OR DocParent IS NOT NULL)\n" +
            "ORDER BY Earliest, Thread, Created";


    private static Announcement[] getRecentAnnouncementsInContainer(Container c, Date min, Date max) throws SQLException
    {
        Announcement[] announcements = Table.executeQuery(_comm.getSchema(), RECENT_ANN_SQL, new Object[]{c, min, max, c, min, max}, AnnouncementManager.BareAnnouncement.class);
        AnnouncementManager.attachMemberLists(announcements);
        return announcements;
    }


    public static void setTimer()
    {
        _timer = new Timer("DailyDigest", true);
        _timerTask = new DailyDigestTask();
        ContextListener.addShutdownListener(_timerTask);
        _timer.scheduleAtFixedRate(_timerTask, getMidnight(new Date(), 1, 5), DateUtils.MILLIS_PER_DAY);  // 12:05AM tomorrow morning
    }


    private static class DailyDigestTask extends TimerTask implements ShutdownListener
    {
        public void run()
        {
            _log.debug("Sending daily digest");

            HttpServletRequest request;

            try
            {
                request = AppProps.getInstance().createMockRequest();
            }
            catch(IllegalStateException e)
            {
                // Exception could occur if this code is run before the first request.  But that should never happen
                // since timer is created after the first request is handled.  Just log and return... can't send to
                // mothership without a request.
                _log.error("DailyDigestTask couldn't create mock request", e);
                return;
            }

            try
            {
                sendDailyDigest(request);
            }
            catch(Exception e)
            {
                ExceptionUtil.logExceptionToMothership(request, e);
            }
        }


        public void shutdownStarted()
        {
            ContextListener.removeShutdownListener(_timerTask);
            _timer.cancel();
        }
    }
}
