package org.labkey.cabig;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.labkey.api.view.*;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.data.Container;
import org.labkey.api.util.ContainerTree;
import org.labkey.api.util.AppProps;

import javax.servlet.ServletException;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class caBIGController extends ViewController
{
    static Logger _log = Logger.getLogger(caBIGController.class);

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


    @Jpf.Action
    protected Forward showHierarchy() throws Exception
    {
        requiresLogin();

        caBIGHierarchyTree tree = new caBIGHierarchyTree(getContainer().getPath(), getUser(), ACL.PERM_ADMIN);

        //StringBuilder html = new StringBuilder();
        //tree.render(html);

        return renderInTemplate(new HtmlView("hello world"));
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
            setTitle("Publish to caBIG");
        }
    }


    public static class caBIGPermissionsViewFactory implements SecurityManager.ViewFactory
    {
        public HttpView createView()
        {
            if (AppProps.getInstance().isCaBIGEnabled())
                return new caBIGPermissionsView();
            else
                return null;
        }
    }


    private static class caBIGHierarchyTree extends ContainerTree
    {
        private caBIGHierarchyTree(String rootPath, User user, int perm)
        {
            super(rootPath, user, perm);
        }
    }
}