package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.query.api.QuerySettings;
import org.fhcrc.cpas.view.ViewContext;
import org.fhcrc.cpas.view.Portal;
import org.fhcrc.cpas.view.ViewURLHelper;
import Flow.FlowParam;

public class FlowQuerySettings extends QuerySettings
{
    private boolean _showGraphs;
    private int _graphSize = 300;
    public FlowQuerySettings(ViewURLHelper url, String dataRegionName)
    {
        super(url, dataRegionName);
    }

    protected void init(ViewURLHelper url)
    {
        super.init(url);
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

    public FlowQuerySettings(Portal.WebPart webPart, ViewURLHelper url)
    {
        super(webPart, url);
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
