package org.labkey.luminex;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public abstract class AbstractLuminexControl implements Serializable
{
    private int _rowId;
    private int _runId;
    private String _name;

    public AbstractLuminexControl() {}

    public AbstractLuminexControl(Titration titration)
    {
        setRunId(titration.getRunId());
        setName(titration.getName());
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        name = name == null || name.trim().isEmpty() ? "Standard" : name;

        _name = name;
    }


}
