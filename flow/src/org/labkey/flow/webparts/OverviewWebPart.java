package org.labkey.flow.webparts;

import org.labkey.api.view.*;

public class OverviewWebPart extends HtmlView
{
    static public final WebPartFactory FACTORY = new Factory();

    static class Factory extends WebPartFactory
    {
        Factory()
        {
            super("Flow Experiment Management");
            addLegacyNames("Flow Overview");
        }

        @Override
        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws Exception
        {
            return new OverviewWebPart(portalCtx);
        }
    }

    public OverviewWebPart(ViewContext portalCtx) throws Exception
    {
        super(new FlowOverview(portalCtx.getUser(), portalCtx.getContainer()).toString());
        setTitle("Flow Experiment Management");
    }
}
