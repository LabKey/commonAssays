package org.labkey.mousemodel;

import org.labkey.api.data.Container;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.GroovyView;
import org.labkey.api.announcements.Announcement;
import org.labkey.api.announcements.DiscussionService;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Nov 18, 2005
 */
@Deprecated
public class NotesView extends GroovyView
{
    private Container _container;
    private String _parentId;
    private static String INSERT_MODE_PARAM = "NotesView.insertMode";

    /**
     * @param c
     * @param parentId EntityID of the parent of these notes
     */
    public NotesView(Container c, String parentId)
    {
        super("/org/labkey/announcements/notes.gm", "Notes");
        _container = c;
        _parentId = parentId;
    }


    @Override
    protected void prepareWebPart(Object model) throws ServletException
    {
        super.prepareWebPart(model);

        try
        {
            Announcement[] notes = DiscussionService.get().getAnnouncements(_container, _parentId);
            addObject("notes", notes);
        }
        catch (Exception x)
        {
            throw new ServletException(x);
        }
        boolean insertMode = getViewContext().getRequest().getParameter(INSERT_MODE_PARAM) != null;
        ViewURLHelper urlhelp = getViewContext().cloneViewURLHelper();
        if (insertMode)
        {
            addObject("parentId", _parentId);
            addObject("insertMode", Boolean.TRUE);
            addObject("container", _container);
            urlhelp = urlhelp.clone();
            urlhelp.deleteParameter(INSERT_MODE_PARAM);
        }
        else
        {
            addObject("insertMode", Boolean.FALSE);
        }
        addObject("clientURL", urlhelp.getLocalURIString());
    }
}
