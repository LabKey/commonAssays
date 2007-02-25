package org.labkey.flow.query;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;
import org.labkey.api.data.PropertyManager;
import org.labkey.flow.controllers.FlowParam;

public class FlowQuerySettings extends QuerySettings
{
    private boolean _showGraphs;
    private int _graphSize = 300;
    public FlowQuerySettings(ViewURLHelper url, String dataRegionName)
    {
        super(url, dataRegionName);
    }

    protected void init(ViewURLHelper url, User user)
    {
        super.init(url, user);
        PropertyManager.PropertyMap propertyMap = getPropertyMap(user, url);
        _showGraphs = url.getParameter(param("showGraphs")) != null;
        String strGraphSize = url.getParameter(param("graphSize"));
        if (strGraphSize != null)
        {
            try
            {
                _graphSize = Integer.parseInt(strGraphSize);
            }
            catch (Exception e)
            {
                
            }
        }
    }

    public FlowQuerySettings(Portal.WebPart webPart, ViewURLHelper url, User user)
    {
        super(webPart, url, user);
    }

    public boolean getShowGraphs()
    {
        return _showGraphs;
    }
    public void setShowGraphs(boolean b)
    {
        _showGraphs = b;
    }

    public int getGraphSize()
    {
        return _graphSize;
    }

    public void setGraphSize(int size)
    {
        _graphSize = size;
    }
}
