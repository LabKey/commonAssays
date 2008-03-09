package org.labkey.flow.persist;

import org.labkey.api.data.Container;

public class AttrObject
{
    Container _container;
    int _rowId;
    int _dataId;
    int _typeId;
    String _uri;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowid)
    {
        _rowId = rowid;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public int getTypeId()
    {
        return _typeId;
    }

    public void setTypeId(int typeId)
    {
        _typeId = typeId;
    }

    public String getUri()
    {
        return _uri;
    }

    public void setUri(String uri)
    {
        _uri = uri;
    }

    public void setContainer(Container c)
    {
        _container = c;
    }

    public Container getContainer()
    {
        return _container;
    }
}
