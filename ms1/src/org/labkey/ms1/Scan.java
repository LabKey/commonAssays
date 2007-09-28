package org.labkey.ms1;

/**
 * Represents a Scan loaded from a Peaks file
 *
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 26, 2007
 * Time: 2:09:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class Scan extends DbBean
{
    public Scan() {}
    public Scan(int peaksFileID)
    {
        _peaksFileID = peaksFileID;
        setDirty(true);
    }

    public Object getRowID()
    {
        return new Object[]{new Integer(_peaksFileID),new Integer(_scan)};
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

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
        setDirty(true);
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
        setDirty(true);
    }

    public Double getObservedDuration()
    {
        return _observedDuration;
    }

    public void setObservedDuration(Double observationDuration)
    {
        _observedDuration = observationDuration;
        setDirty(true);
    }

    protected int _scan = DbBean.ID_INVALID;
    protected int _peaksFileID = DbBean.ID_INVALID;
    protected Double _retentionTime = null;     //can be null
    protected Double _observedDuration = null;  //can be null
} //class Scan
