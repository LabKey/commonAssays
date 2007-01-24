package org.labkey.flow.webparts;

import org.labkey.api.view.*;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.jsp.ContextPage;

import java.io.PrintWriter;

public class OverviewWebPart extends JspView
{
    static public final WebPartFactory FACTORY = new WebPartFactory("Flow Overview") 
    {
        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws Exception
        {
            return new OverviewWebPart(portalCtx);
        }
    };

    public OverviewWebPart(ViewContext portalCtx)
    {
        super(JspLoader.createPage(portalCtx.getRequest(), OverviewWebPart.class, "overview.jsp"));
        setTitle("Flow Overview");
        ((ContextPage) _page).setViewContext(new ViewContext(portalCtx));
    }
}
