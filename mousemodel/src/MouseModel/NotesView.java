package MouseModel;

import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.view.ViewURLHelper;
import org.fhcrc.cpas.view.GroovyView;
import org.fhcrc.cpas.announcements.Announcement;
import org.fhcrc.cpas.announcements.AnnouncementManager;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Nov 18, 2005
 */
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
        super("/announcements/notes.gm", "Notes");
        _container = c;
        _parentId = parentId;
    }


    @Override
    protected void prepareWebPart(Object model) throws ServletException
    {
        super.prepareWebPart(model);

        try
        {
            //Announcement parent = getAnnouncement(null, _parentId);
            Announcement[] notes = AnnouncementManager.getAnnouncements(_container, _parentId);
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
