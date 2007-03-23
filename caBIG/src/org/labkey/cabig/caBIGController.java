package org.labkey.cabig;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.labkey.api.view.*;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.data.Container;

import javax.servlet.ServletException;
import java.io.PrintWriter;


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
    protected Forward publishContainer() throws Exception
    {
        requiresAdmin();

        caBIGManager.get().publishContainer(getContainer());

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


    public static class caBIGPermissionsView extends JspView
    {
        public caBIGPermissionsView()
        {
            super("/org/labkey/cabig/view/hello.jsp");
            this.setTitle("caBIG Permissions");
        }
    }
}