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
import org.labkey.api.util.MailHelper.*;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;

import javax.servlet.ServletException;
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
    private static Timer _timer = null;
    private static DailyDigestTask _timerTask = null;

    private static final Logger _log = Logger.getLogger(DailyDigest.class);

    public static void sendDailyDigest(ViewContext ctx) throws Exception
    {
        Date min = getLastSuccessful();
        Date current = new Date();

        if (null == min)
            min = getMidnight(current, 0, 0);

        Date max = getMidnight(current, 1, 0);

        List<Container> containers = getContainersWithNewMessages(min, max);

        for (Container c : containers)
            sendDailyDigest(ctx, c, min, max);

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
        Map<String, Object> props = PropertyManager.getProperties(ContainerManager.getRoot().getId(), SET_KEY, true);
        String value = (String)props.get(LAST_KEY);
        return null != value ? new Date(Long.parseLong(value)) : null;
    }


    private static void setLastSuccessful(Date last)
    {
        Map<String, Object> props = PropertyManager.getWritableProperties(0, ContainerManager.getRoot().getId(), SET_KEY, true);
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


    private static void sendDailyDigest(ViewContext ctx, Container c, Date min, Date max) throws Exception
    {
        Settings settings = AnnouncementManager.getMessageBoardSettings(c);
        Announcement[] announcements = getRecentAnnouncementsInContainer(c, min, max);
        List<User> users = new ArrayList<User>();
        users.add(ctx.getUser());

        for (User user : users)
        {
            List<Announcement> announcementList = new ArrayList<Announcement>(announcements.length);
            Permissions perm = AnnouncementsController.getPermissions(c, user, settings);

            for (Announcement ann : announcements)
                if (perm.allowRead(ann))
                    announcementList.add(ann);

            if (!announcementList.isEmpty())
            {
                ViewMessage m = getDailyDigestMessage(ctx, c, settings, announcementList, user);
                MailHelper.send(m);
            }
        }
    }


    private static MailHelper.ViewMessage getDailyDigestMessage(ViewContext ctx, Container c, Settings settings, List<Announcement> announcements, User user) throws Exception
    {
        MailHelper.ViewMessage m = MailHelper.createMultipartViewMessage(AppProps.getInstance().getSystemEmailAddress(), user.getEmail());
        m.setSubject("New posts to " + c.getPath());

        DailyDigestPage page = createPage("dailyDigestPlain.jsp", ctx, c, settings, announcements);
        JspView view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(ctx, view, "text/plain");

        page = createPage("dailyDigest.jsp", ctx, c, settings, announcements);
        view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        m.setTemplateContent(ctx, view, "text/html");

        return m;
    }


    private static DailyDigestPage createPage(String templateName, ViewContext ctx, Container c, Settings settings, List<Announcement> announcements) throws ServletException
    {
        DailyDigestPage page = (DailyDigestPage) JspLoader.createPage(ctx.getRequest(), AnnouncementsController.class, templateName);

        page.ctx = ctx;
        page.conversationName = settings.getConversationName().toLowerCase();
        page.settings = settings;
        page.c = c;
        page.announcements = announcements;

        URLHelper cssURL = new URLHelper(ctx.getRequest());
        cssURL.setPath("/core/stylesheet.view");
        cssURL.setRawQuery(null);
        page.cssURL = cssURL.getURIString();

        return page;
    }


    // Retrieve all messages in this container with a body posted during the given timespan
    // Messages are grouped by thread and threads are sorted by earliest post within each thread
    private static final String RECENT_ANN_SQL = "SELECT Ann.* FROM\n" +
            "\t(\n" +
            "\tSELECT Thread, MIN(Created) AS Earliest FROM\n" +
            "\t\t(SELECT Created, CASE WHEN Parent IS NULL THEN EntityId ELSE Parent END AS Thread FROM comm.Announcements WHERE Container = ? AND Created >= ? AND Created < ? AND Body IS NOT NULL) x\n" +
            "\tGROUP BY Thread\n" +
            "\t) X LEFT OUTER JOIN comm.Announcements Ann ON Parent = Thread OR EntityId = Thread\n" +
            "WHERE Created >= ? and Created < ? AND Body IS NOT NULL\n" +
            "ORDER BY Earliest, Thread, Created";


    private static Announcement[] getRecentAnnouncementsInContainer(Container c, Date min, Date max) throws SQLException
    {
        Announcement[] announcements = Table.executeQuery(_comm.getSchema(), RECENT_ANN_SQL, new Object[]{c, min, max, min, max}, AnnouncementManager.BareAnnouncement.class);
        AnnouncementManager.attachMemberLists(announcements);
        return announcements;
    }


    public static void setTimer()
    {
        _timer = new Timer("DailyDigest", true);
        _timerTask = new DailyDigestTask();
        ContextListener.addShutdownListener(_timerTask);
        _timer.scheduleAtFixedRate(_timerTask, getMidnight(new Date(), 1, 5), DateUtils.MILLIS_PER_DAY);
    }


    private static class DailyDigestTask extends TimerTask implements ShutdownListener
    {
        public void run()
        {
            _log.debug("Sending daily digest");
            // sendDailyDigest();
        }


        public void shutdownStarted()
        {
            ContextListener.removeShutdownListener(_timerTask);
            _timer.cancel();
        }
    }
}
