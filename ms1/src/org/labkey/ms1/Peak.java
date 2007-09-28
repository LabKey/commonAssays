package org.labkey.ms1;

/**
 * Represents a peak loaded from the peaks XML file
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 27, 2007
 * Time: 2:32:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class Peak extends DbBean
{
    public Peak() {}
    public Peak(int peaksFileID, int scan)
    {
        _scan = scan;
        _peaksFileID = peaksFileID;
        setDirty(true);
    }

    public Object getRowID()
    {
        return _peakID;
    }

    public int getPeakID()
    {
        return _peakID;
    }

    public void setPeakID(int peakID)
    {
        _peakID = peakID;
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

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
        setDirty(true);
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
        setDirty(true);
    }

    public Double getFrequency()
    {
        return _frequency;
    }

    public void setFrequency(Double frequency)
    {
        _frequency = frequency;
        setDirty(true);
    }

    public Double getAmplitude()
    {
        return _amplitude;
    }

    public void setAmplitude(Double amplitude)
    {
        _amplitude = amplitude;
        setDirty(true);
    }

    public Double getPhase()
    {
        return _phase;
    }

    public void setPhase(Double phase)
    {
        _phase = phase;
        setDirty(true);
    }

    public Double getDecay()
    {
        return _decay;
    }

    public void setDecay(Double decay)
    {
        _decay = decay;
        setDirty(true);
    }

    public Double getError()
    {
        return _error;
    }

    public void setError(Double error)
    {
        _error = error;
        setDirty(true);
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        _area = area;
        setDirty(true);
    }

    protected int _peakID = DbBean.ID_INVALID;
    protected int _scan = DbBean.ID_INVALID;
    protected int _peaksFileID = DbBean.ID_INVALID;
    protected Double _mz = null;
    protected Double _frequency = null;
    protected Double _amplitude = null;
    protected Double _phase = null;
    protected Double _decay = null;
    protected Double _error = null;
    protected Double _area = null;
} //class Peak
