package org.labkey.flow.query;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ActionURL;

import javax.servlet.http.HttpServletRequest;

public class FlowQuerySettings extends QuerySettings
{
    private boolean _showGraphs;
    public FlowQuerySettings(ActionURL url, HttpServletRequest request, String dataRegionName)
    {
        super(url, request, dataRegionName);
    }

    protected void init(ActionURL url)
    {
        super.init(url);
        _showGraphs = url.getParameter(param("showGraphs")) != null;
    }

    public FlowQuerySettings(Portal.WebPart webPart, ViewContext context)
    {
        super(webPart, context);
    }

    public boolean getShowGraphs()
    {
        return _showGraphs;
    }
    public void setShowGraphs(boolean b)
    {
        _showGraphs = b;
    }
}
