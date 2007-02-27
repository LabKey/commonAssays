package org.labkey.announcements.model;

import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.security.User;
import org.labkey.api.util.AppProps;
import org.labkey.announcements.AnnouncementsController;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URISyntaxException;
import java.io.PrintWriter;
import java.util.Map;
import java.sql.SQLException;

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
        String id = StringUtils.strip(url.getExtraPath(),"/");
        Container c = ContainerManager.getForId(id);
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


    public WebPartView getDiscussion(Container c, Announcement ann, User user)
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
        int discussionId = 0;
        try {discussionId = Integer.parseInt((String)context.get("discussion.id"));} catch (Exception x) {/* */}
        pageURL = pageURL.clone();
        // clean up discussion parameters (in case caller didn't)
        pageURL.deleteScopeParameters("discussion");

        // often, but not necessarily the same as pageURL, assume we want to return to current page
        ViewURLHelper returnUrl = context.cloneViewURLHelper().deleteScopeParameters("discussion");
        if (0 != discussionId)
            returnUrl.addParameter("discussion.id", "" + discussionId);
        
        if (context.get("discussion.start") != null)
        {
            WebPartView start = startDiscussion(c, user, objectId, pageURL, title, "");
            start.setFrame(WebPartView.FrameType.NONE);
            return new ThreadWrapper("Start a new discussion", start);
        }
        else
        {
            Announcement[] announcements = getDiscussions(c, objectId, user);
            Announcement selected = null;

            WebPartView discussionView = null;
            HttpView respondView = null;

            if (discussionId != 0)
            {
                for (Announcement ann : announcements)
                {
                    if (ann.getRowId() == discussionId)
                    {
                        selected = ann;
                        break;
                    }
                }

                if (selected != null)
                {
                    discussionView = getDiscussion(c, selected, user);
                    discussionView.setFrame(WebPartView.FrameType.NONE);
                    if (context.get("discussion.reply") != null)
                    {
                        ((AnnouncementsController.ThreadView)discussionView).getModel().isResponse = true;
                        respondView = new AnnouncementsController.RespondView(c, selected, returnUrl);
                    }
                }
            }

            HttpView pickerView = new PickerView(pageURL, announcements);
            if (discussionView == null)
                return pickerView;
            return new VBox(pickerView, new ThreadWrapper("Discussion", discussionView, respondView));
        }
    }


    public void deleteDiscussions(Container c, String identifier, User user)
    {
        Announcement[] anns = getDiscussions(c, identifier, user);
        for (Announcement ann : anns)
        {
            try
            {
                AnnouncementManager.deleteAnnouncement(c, ann.getRowId());
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
        }
    }


    public void unlinkDiscussions(Container c, String identifier, User user)
    {
        Announcement[] anns = getDiscussions(c, identifier, user);
        for (Announcement ann : anns)
        {
            try
            {
                ann.setDiscussionSrcURL(null);
                AnnouncementManager.updateAnnouncement(user, ann);
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
        }
    }


    public static class ThreadWrapper extends WebPartView
    {
        WebPartView _vbox;

        ThreadWrapper(String caption, HttpView... views)
        {
            _vbox = new VBox(views);
            _vbox.setTitle(caption);
            _vbox.setFrame(WebPartView.FrameType.DIALOG);
        }

        public ThreadWrapper()
        {
            super();
        }

        public void doStartTag(Map context, PrintWriter out)
        {
            out.write("<table><tr><th valign=top width=50px><img src='" + getViewContext().getContextPath() + "/_.gif' width=50 height=1></th><td class=normal>");
        }

        protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
        {
            _vbox.render(request, response);
        }

        public void doEndTag(Map context, PrintWriter out)
        {
            out.write("</td></tr></table>");
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
