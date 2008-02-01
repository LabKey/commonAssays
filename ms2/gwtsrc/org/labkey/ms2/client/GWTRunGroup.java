package org.labkey.ms2.client;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Jan 31, 2008
 */
public class GWTRunGroup implements Serializable
{
    /** @gwt.typeArgs <org.labkey.ms2.client.GWTExperimentRun> */
    private List _runs = new ArrayList();
    private int _rowId;
    private String _name;
    private String _url;

    public List getRuns()
    {
        return _runs;
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

    public String getURL()
    {
        return _url;
    }

    public void setURL(String url)
    {
        _url = url;
    }

    public void addRun(GWTExperimentRun gwtRun)
    {
        _runs.add(gwtRun);
    }
}
