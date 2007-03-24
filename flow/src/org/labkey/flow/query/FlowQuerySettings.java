package org.labkey.flow.query;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;
import org.labkey.api.data.PropertyManager;
import org.labkey.flow.controllers.FlowParam;

import javax.servlet.http.HttpServletRequest;

public class FlowQuerySettings extends QuerySettings
{
    private boolean _showGraphs;
    public FlowQuerySettings(ViewURLHelper url, HttpServletRequest request, String dataRegionName)
    {
        super(url, request, dataRegionName);
    }

    protected void init(ViewURLHelper url)
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
