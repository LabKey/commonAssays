package org.labkey.microarray.sampleset.client.model;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class GWTSampleSet implements Serializable
{
    private String _lsid;
    private String _name;
    private int _rowId;

    public GWTSampleSet() {}

    public GWTSampleSet(String name, String lsid)
    {
        _name = name;
        _lsid = lsid;
    }

    public String getLsid()
    {
        return _lsid;
    }

    public void setLsid(String lsid)
    {
        _lsid = lsid;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTSampleSet)) return false;

        GWTSampleSet that = (GWTSampleSet) o;

        return !(_lsid != null ? !_lsid.equals(that._lsid) : that._lsid != null);
    }

    public int hashCode()
    {
        return (_lsid != null ? _lsid.hashCode() : 0);
    }
}
