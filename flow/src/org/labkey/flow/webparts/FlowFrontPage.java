package org.labkey.flow.webparts;

import org.labkey.api.view.*;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Nov 10, 2008
 * Time: 11:07:10 AM
 */
public class FlowFrontPage extends JspView<FlowFrontPage>
{
    // web parts shouldn't assume the current container
    public Container c;

    public FlowFrontPage(ViewContext c) throws Exception
    {
        this(c.getContainer());
    }

    public FlowFrontPage(Container c) throws Exception
    {
        super(FlowFrontPage.class, "frontpage.jsp", null);
        setTitle("Flow Front Page");
        setFrame(WebPartView.FrameType.PORTAL);
        setModelBean(this);
        this.c = c;
    }

    public static WebPartFactory FACTORY = new SimpleWebPartFactory("Flow Front Page", FlowFrontPage.class);
}

