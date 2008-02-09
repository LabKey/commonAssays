package org.labkey.microarray.sampleset.client.model;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class GWTMaterial implements Serializable
{
    private String _lsid;
    private int _rowId;
    private String _name;

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
}
