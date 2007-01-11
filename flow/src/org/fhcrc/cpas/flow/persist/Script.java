package org.fhcrc.cpas.flow.persist;

public class Script
{
    int _rowId;
    int _objectId;
    String _text;
    public void setRowId(int rowid)
    {
        _rowId = rowid;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setObjectId(int objectId)
    {
        _objectId = objectId;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public String getText()
    {
        return _text;
    }

    public void setText(String text)
    {
        _text = text;
    }
}
