package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Jun 6, 2011
 */
public class Titration
{
    private int _rowId;
    private int _runId;
    private String _name;
    private boolean _standard;
    private boolean _qcControl;

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
        _name = name;
    }

    public boolean isStandard()
    {
        return _standard;
    }

    public void setStandard(boolean standard)
    {
        _standard = standard;
    }

    public boolean isQcControl()
    {
        return _qcControl;
    }

    public void setQcControl(boolean qcControl)
    {
        _qcControl = qcControl;
    }
}
