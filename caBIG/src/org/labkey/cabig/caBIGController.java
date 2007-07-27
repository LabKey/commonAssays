package org.labkey.cabig;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.ContainerTree;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.*;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.net.URISyntaxException;


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
    protected Forward publishAll() throws Exception
    {
        return setPublishState(State.Publish);
    }


    @Jpf.Action
    protected Forward unpublishAll() throws Exception
    {
        return setPublishState(State.Unpublish);
    }


    private enum State
    {
        Publish
        {
            void setState(Container c) throws SQLException
            {
                caBIGManager.get().publish(c);
            }
        },
        Unpublish
        {
            void setState(Container c) throws SQLException
            {
                caBIGManager.get().unpublish(c);
            }
        };

        abstract void setState(Container c) throws SQLException;
    }


    private Forward setPublishState(State state) throws Exception
    {
        requiresAdmin();

        List<String> containerIds = getViewContext().getList("containerIds");

        for (String id : containerIds)
        {
            Container c = ContainerManager.getForId(id);
            if (!c.hasPermission(getUser(), ACL.PERM_ADMIN))
                throw new UnauthorizedException();

            state.setState(c);
        }

        return new ViewForward("cabig", "admin.view", getContainer());
    }


    @Jpf.Action
    protected Forward blank() throws Exception
    {
        requiresLogin();

        return renderInTemplate(new EmptyView(), getContainer(), "I am empty");
    }


    @Jpf.Action
    protected Forward admin() throws Exception
    {
        requiresAdmin();

        caBIGHierarchyTree tree = new caBIGHierarchyTree(getContainer().getPath(), getUser(), ACL.PERM_ADMIN, getViewURLHelper());

        StringBuilder html = new StringBuilder();
        html.append("<script type=\"text/javascript\">\n").append("LABKEY.requiresScript('filter.js');\n").append("</script>");
        html.append("Click the buttons below to publish or unpublish folders to the caBIG&trade; interface.  If your caBIG&trade; web application ");
        html.append("is running then all experiment data in published folders is publicly visible via the caBIG&trade; interface.<br><br>");
        html.append("For more information about publishing to caBIG&trade;, ");
        html.append("<a href=\"").append(PageFlowUtil.filter(new HelpTopic("cabig", HelpTopic.Area.CPAS).getHelpTopicLink())).append("\">click here</a>.<br><br>\n");
        html.append("<form method=post action=''>");
        html.append("<table class=\"dataRegion\" cellspacing=\"0\" cellpadding=\"1\">");
        tree.render(html);
        html.append("</table><br>");
        renderHierarchyButtonBar(html);
        html.append("</form>");

        return renderInTemplate(new HtmlView(html.toString()), getContainer(), "caBIG Admin");
    }


    private void renderHierarchyButtonBar(StringBuilder html) throws IOException, ServletException
    {
        ButtonBar bb = new ButtonBar();

        ActionButton publishAll = new ActionButton("publishAll.post", "Publish All");
        publishAll.setActionType(ActionButton.Action.POST);
        bb.add(publishAll);

        ActionButton unpublishAll = new ActionButton("unpublishAll.post", "Unpublish All");
        unpublishAll.setActionType(ActionButton.Action.POST);
        bb.add(unpublishAll);

        ActionButton done = new ActionButton("Done", new ViewURLHelper("Security", "begin.view", getContainer()));
        bb.add(done);

        StringWriter s = new StringWriter();
        bb.render(new RenderContext(getViewContext()), s);
        html.append(s);
    }


    // Always forward to permissions page
    private ViewForward getForward() throws ServletException
    {
        String returnUrl = getViewURLHelper().getParameter("returnUrl");

        if (null != returnUrl)
        {
            try
            {
                return new ViewForward(returnUrl);
            }
            catch (URISyntaxException e)
            {
                // Ignore... just do the default redirect
            }
        }

        Container c = getContainer();
        return new ViewForward(SecurityManager.getPermissionsUrl(c));
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
        public HttpView createView(ViewContext context)
        {
            if (AppProps.getInstance().isCaBIGEnabled())
                return new caBIGPermissionsView();
            else
                return null;
        }
    }


    private static class caBIGHierarchyTree extends ContainerTree
    {
        private static caBIGManager _caBIG = caBIGManager.get();
        private static String _unauthorizedButton = PageFlowUtil.buttonImg("Not Authorized", "disabled");
        private String _currentUrl;

        private caBIGHierarchyTree(String rootPath, User user, int perm, ViewURLHelper currentUrl)
        {
            super(rootPath, user, perm, currentUrl);
            _currentUrl = currentUrl.getEncodedLocalURIString();
        }


        @Override
        protected void renderNodeStart(StringBuilder html, Container c, ViewURLHelper url, boolean isAuthorized, int level)
        {
            html.append("<tr><td>");
            appendButton(html, c, isAuthorized, _currentUrl);
            appendContainerId(html, c, isAuthorized);
            html.append("</td><td style=\"padding-left:");
            html.append(10 * level);
            html.append("\">");
        }


        private static void appendButton(StringBuilder html, Container c, boolean isAuthorized, String returnUrl)
        {
            if (!isAuthorized)
            {
                html.append(_unauthorizedButton);
            }
            else
            {
                boolean isPublished = isPublished(c);
                ViewURLHelper publishUrl = new ViewURLHelper("cabig", isPublished ? "unpublish" : "publish", c).addParameter("returnUrl", returnUrl);
                html.append(PageFlowUtil.buttonLink(isPublished ? "Unpublish" : "Publish", publishUrl));
            }
        }


        private static void appendContainerId(StringBuilder html, Container c, boolean isAuthorized)
        {
            if (isAuthorized)
                html.append("<input type=\"hidden\" name=\"containerIds\" value=\"").append(c.getId()).append("\">");
        }


        private static boolean isPublished(Container c)
        {
            try
            {
                return _caBIG.isPublished(c);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
    }
}