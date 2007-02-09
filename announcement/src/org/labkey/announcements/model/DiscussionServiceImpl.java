package org.labkey.announcements.model;

import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.view.*;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.User;
import static org.labkey.api.util.PageFlowUtil.filter;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.PageFlowUtil;
import static org.labkey.api.util.PageFlowUtil.jsString;
import static org.labkey.api.util.PageFlowUtil.filter;
import org.labkey.announcements.AnnouncementsController;

import javax.servlet.ServletException;
import java.net.URISyntaxException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Feb 6, 2007
 * Time: 12:37:45 PM
 */
public class DiscussionServiceImpl implements DiscussionService.Service
{
    public WebPartView startDiscussion(Container c, User user, String identifier, ViewURLHelper url, String title, String summary)
    {
        String viewTitle = "New Discussion";
        AnnouncementsController.AnnouncementForm form = new AnnouncementsController.AnnouncementForm();
        form.setUser(user);
        form.setContainer(c);
        form.set("title", title);
        form.set("discussionSrcIdentifier", identifier);
        form.set("discussionSrcURL", toSaved(url));
        WebPartView view = new AnnouncementsController.InsertMessageView(form, viewTitle, null);
        return view;
    }


    public static String toSaved(ViewURLHelper url)
    {
        Container c = ContainerManager.getForPath(url.getExtraPath());
        ViewURLHelper saveUrl = url.clone();
        if (null != c)
            saveUrl.setExtraPath(c.getId());
        String saved=saveUrl.getLocalURIString();

        String contextPath = AppProps.getInstance().getContextPath();
        if (saved.startsWith(contextPath))
            saved = "~" + saved.substring(contextPath.length());
        return saved;
    }


    public static ViewURLHelper fromSaved(String saved) throws URISyntaxException
    {
        if (saved.startsWith("~/"))
            saved = AppProps.getInstance().getContextPath() + saved.substring(1);
        ViewURLHelper url = new ViewURLHelper(saved);
        Container c = ContainerManager.getForId(url.getExtraPath());
        if (null != c)
            url.setExtraPath(c.getPath());
        return url;
    }


    public Announcement[] getDiscussions(Container c, String identifier, User user)
    {
        SimpleFilter filter = new SimpleFilter("discussionSrcIdentifier", identifier);
        Announcement[] announcements = AnnouncementManager.getBareAnnouncements(c, filter, new Sort("Created"));
        return announcements;
    }


    public HttpView getDiscussion(Container c, Announcement ann, User user)
    {
        try
        {
            // NOTE: don't pass in Announcement, it came from getBareAnnouncements()
            AnnouncementsController.ThreadView threadView = new AnnouncementsController.ThreadView(c, user, null, ann.getEntityId());
            return threadView;
        }
        catch (ServletException x)
        {
            return new HtmlView(x.toString());
        }
    }


    public ViewURLHelper getDiscussionUrl(Container c, Announcement ann)
    {
        return null;
    }


    public HttpView getDisussionArea(ViewContext context, Container c, User user, String objectId, ViewURLHelper pageURL, String title)
    {
        if (true)
            return null;
        
        if (context.get("startDiscussion") != null)
        {
            return startDiscussion(c, user, objectId, pageURL, title, "");
        }
        else
        {
            Announcement[] announcements = getDiscussions(c, objectId, user);

            HttpView discussionView = null;
            if (context.get("discussionId") != null)
            {
                int discussionId = 0;
                try {discussionId = Integer.parseInt((String)context.get("discussionId"));} catch (Exception x) {/* */}
                for (Announcement ann : announcements)
                {
                    if (ann.getRowId() == discussionId)
                    {
                        discussionView = getDiscussion(c, ann, user);
                        break;
                    }
                }
            }

            HttpView pickerView = new PickerView(pageURL, announcements);

            return new VBox(pickerView, discussionView);
        }
    }


    public static class PickerView extends GroovyView
    {
        public ViewURLHelper pageURL;
        public Announcement[] announcements;

        PickerView(ViewURLHelper pageURL, Announcement[] announcements)
        {
            super("/org/labkey/announcements/discussionMenu.gm");
            setFrame(FrameType.NONE);
            this.pageURL = pageURL.clone();
            this.announcements = announcements;
        }
    }


    /* for conversion of NotesView in MouseModel module */
    @Deprecated
    public Announcement[] getAnnouncements(Container container, String parentId)
    {
        return AnnouncementManager.getAnnouncements(container, parentId);
    }
}
