package org.labkey.luminex;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public class AnalyteSinglePointControl extends AbstractLuminexControlAnalyte
{
    private int _rowId;
    private int _singlePointControlId;

    public AnalyteSinglePointControl() {}

    public AnalyteSinglePointControl(Analyte analyte, SinglePointControl control)
    {
        setAnalyteId(analyte.getRowId());
        _singlePointControlId = control.getRowId();
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getSinglePointControlId()
    {
        return _singlePointControlId;
    }

    public void setSinglePointControlId(int singlePointControlId)
    {
        _singlePointControlId = singlePointControlId;
    }
}
