package org.labkey.ms1;

/**
 * Represents a Peak Family read form the peaks xml file
 *
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 27, 2007
 * Time: 2:13:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeakFamily extends DbBean
{
    public PeakFamily() {}
    public PeakFamily(Integer peaksFileID, Integer scan)
    {
        _peaksFileID = peaksFileID;
        _scan = scan;
        setDirty(true);
    }

    public Object getRowID()
    {
        return _peakFamilyID;
    }

    public int getPeakFamilyID()
    {
        return _peakFamilyID;
    }

    public void setPeakFamilyID(int peakFamilyID)
    {
        _peakFamilyID = peakFamilyID;
        setDirty(true);
    }

    public Integer getPeaksFileID()
    {
        return _peaksFileID;
    }

    public void setPeaksFileID(Integer peaksFileID)
    {
        _peaksFileID = peaksFileID;
    }

    public Integer getScan()
    {
        return _scan;
    }

    public void setScan(Integer scan)
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

    public Byte getCharge()
    {
        return _charge;
    }

    public void setCharge(Byte charge)
    {
        _charge = charge;
        setDirty(true);
    }

    protected int _peakFamilyID = DbBean.ID_INVALID;
    protected Integer _peaksFileID = null;
    protected Integer _scan = null;
    protected Double _mz = null;
    protected Byte _charge = null;
} //class PeakFamily
