package org.labkey.ms2.client;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: Jan 31, 2008
 */
public class GWTExperimentRun implements Serializable
{
    private String _lsid;
    private int _rowId;
    private String _name;
    private String _url;

    public String getLsid()
    {
        return _lsid;
    }

    public void setLsid(String lsid)
    {
        _lsid = lsid;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getUrl()
    {
        return _url;
    }

    public void setUrl(String url)
    {
        _url = url;
    }
}
