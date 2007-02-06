package org.labkey.announcements.model;

import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;

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
        return null;
    }

    public Announcement[] getDiscussions(Container c, String identifier, User user)
    {
        return new Announcement[0];
    }

    public HttpView getDiscussion(Announcement ann, User user)
    {
        return null;
    }

    public ViewURLHelper getDiscussionUrl(Announcement ann)
    {
        return null;
    }

    public String disussionWidget(Announcement[] ann, boolean startDiscussion)
    {
        return null;
    }


    /* for conversion of NotesView */
    @Deprecated
    public Announcement[] getAnnouncements(Container container, String parentId)
    {
        return AnnouncementManager.getAnnouncements(container, parentId);
    }
}
