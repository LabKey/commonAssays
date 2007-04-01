package org.labkey.cabig;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.labkey.api.view.*;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.data.Container;

import javax.servlet.ServletException;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class caBIGController extends ViewController
{
    static Logger _log = Logger.getLogger(caBIGController.class);

    /**
     * This method represents the point of entry into the pageflow
     */
    @Jpf.Action
    protected Forward begin() throws Exception
    {
        return getForward();
    }


    @Jpf.Action
    protected Forward publish() throws Exception
    {
        requiresAdmin();

        caBIGManager.get().publish(getContainer());

        return getForward();
    }


    @Jpf.Action
    protected Forward unpublish() throws Exception
    {
        requiresAdmin();

        caBIGManager.get().unpublish(getContainer());

        return getForward();
    }


    // Always forward to permissions page
    private ViewForward getForward() throws ServletException
    {
        Container c = getContainer();
        return new ViewForward(SecurityManager.getPermissionsUrl(c));
    }


    private Forward renderInTemplate(HttpView view) throws Exception
    {
        HttpView template = new HomeTemplate(getViewContext(), getContainer(), view);
        includeView(template);
        return null;
    }


    private static class caBIGPermissionsView extends JspView
    {
        private caBIGPermissionsView()
        {
            super("/org/labkey/cabig/view/publish.jsp");
            this.setTitle("Publish to caBIG");
        }
    }


    public static class caBIGPermissionsViewFactory implements SecurityManager.ViewFactory
    {
        public HttpView createView()
        {
            return new caBIGPermissionsView();
        }
    }
}