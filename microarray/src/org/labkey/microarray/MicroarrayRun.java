package org.labkey.microarray;

import org.labkey.api.data.Container;

/**
 * User: jeckels
 * Date: Jan 4, 2008
 */
public class MicroarrayRun
{
    private int _rowId;
    private String _status;
    private int _dataId;
    private Container _container;
    private int _protocolId;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getStatus()
    {
        return _status;
    }

    public void setStatus(String status)
    {
        _status = status;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(int protocolId)
    {
        _protocolId = protocolId;
    }
}
