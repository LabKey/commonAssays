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
import org.labkey.api.view.*;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.io.IOException;
import java.io.StringWriter;


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
    protected Forward publishSelected() throws Exception
    {
        requiresAdmin();

        return renderInTemplate(new HtmlView("Not yet implemented"), getContainer(), "NYI");
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

        caBIGHierarchyTree tree = new caBIGHierarchyTree(getContainer().getPath(), getUser(), ACL.PERM_ADMIN);

        StringBuilder html = new StringBuilder();
        html.append("<script type=\"text/javascript\">\n");
        html.append("LABKEY.requiresScript('filter.js');\n");
        html.append("</script>");
        html.append("<form method=post action=''>");
        html.append("<table class=\"dataRegion\" cellspacing=\"0\" cellpadding=\"1\">");
        tree.render(html);
        html.append("</table>");
        renderHierarchyButtonBar(html);
        html.append("</form>");

        return renderInTemplate(new HtmlView(html.toString()), getContainer(), "caBIG Admin");
    }


    private void renderHierarchyButtonBar(StringBuilder html) throws IOException
    {
        ButtonBar bb = new ButtonBar();

        bb.add(ActionButton.BUTTON_SELECT_ALL);
        bb.add(ActionButton.BUTTON_CLEAR_ALL);

        ActionButton compareRuns = new ActionButton("button", "Publish Selected Folders");
        compareRuns.setScript("return verifySelected(this.form, \"publishSelected.view\", \"post\", \"folders\")");
        compareRuns.setActionType(ActionButton.Action.GET);
        compareRuns.setDisplayPermission(ACL.PERM_READ);
        bb.add(compareRuns);

        StringWriter s = new StringWriter();

        bb.render(new RenderContext(getViewContext()), s);
        html.append(s);
    }


    // Always forward to permissions page
    private ViewForward getForward() throws ServletException
    {
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
        private static caBIGManager _caBIG = caBIGManager.get();

        private caBIGHierarchyTree(String rootPath, User user, int perm)
        {
            super(rootPath, user, perm);
        }


        @Override
        protected void renderNodeStart(StringBuilder html, Container c, ViewURLHelper url, boolean isAuthorized, int level)
        {
            html.append("<tr><td>");
            html.append("<input type=checkbox name='container' value='");
            html.append(c.getId());
            html.append("'");

            if (isAuthorized)
            {
                if (isPublished(c))
                    html.append(" checked");
            }
            else
                html.append(" disabled");

            html.append("></td><td style=\"padding-left:");
            html.append(10 * level);
            html.append("\">");
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