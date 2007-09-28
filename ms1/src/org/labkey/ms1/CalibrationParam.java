package org.labkey.ms1;

/**
 * Represents a Calibration Parameter in a peaks file
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 26, 2007
 * Time: 3:50:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class CalibrationParam extends DbBean
{
    public CalibrationParam() {}

    public CalibrationParam(int peaksFileID, int scan)
    {
        _scan = scan;
        _peaksFileID = peaksFileID;
        setDirty(true);
    }

    public Object getRowID()
    {
        return new Object[]{new Integer(_peaksFileID),new Integer(_scan),_name};
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
        setDirty(true);
    }

    public Double getValue()
    {
        return _value;
    }

    public void setValue(Double value)
    {
        _value = value;
        setDirty(true);
    }

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
        setDirty(true);
    }

    public int getPeaksFileID()
    {
        return _peaksFileID;
    }

    public void setPeaksFileID(int peaksFileID)
    {
        _peaksFileID = peaksFileID;
        setDirty(true);
    }

    protected String _name = null;
    protected Double _value = null;
    protected int _scan = DbBean.ID_INVALID;
    protected int _peaksFileID = DbBean.ID_INVALID;
}
