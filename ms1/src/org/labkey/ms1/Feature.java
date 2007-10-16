package org.labkey.ms1;

import org.labkey.api.data.Table;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.common.util.Pair;

import java.sql.SQLException;

/**
 * Represents an MS1 Feature.
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 12, 2007
 * Time: 10:17:09 AM
 */
public class Feature
{
    public int getFeatureId()
    {
        return _featureId;
    }

    public void setFeatureId(int featureId)
    {
        _featureId = featureId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public Integer getScan()
    {
        return _scan;
    }

    public void setScan(Integer scan)
    {
        _scan = scan;
    }

    public Double getTime()
    {
        return _time;
    }

    public void setTime(Double time)
    {
        _time = time;
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
    }

    public Boolean getAccurateMz()
    {
        return _accurateMz;
    }

    public void setAccurateMz(Boolean accurateMz)
    {
        _accurateMz = accurateMz;
    }

    public Double getMass()
    {
        return _mass;
    }

    public void setMass(Double mass)
    {
        _mass = mass;
    }

    public Double getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Double intensity)
    {
        _intensity = intensity;
    }

    public Short getCharge()
    {
        return _charge;
    }

    public void setCharge(Short charge)
    {
        _charge = charge;
    }

    public Short getChargeStates()
    {
        return _chargeStates;
    }

    public void setChargeStates(Short chargeStates)
    {
        _chargeStates = chargeStates;
    }

    public Double getKl()
    {
        return _kl;
    }

    public void setKl(Double kl)
    {
        _kl = kl;
    }

    public Double getBackground()
    {
        return _background;
    }

    public void setBackground(Double background)
    {
        _background = background;
    }

    public Double getMedian()
    {
        return _median;
    }

    public void setMedian(Double median)
    {
        _median = median;
    }

    public Integer getPeaks()
    {
        return _peaks;
    }

    public void setPeaks(Integer peaks)
    {
        _peaks = peaks;
    }

    public Integer getScanFirst()
    {
        return _scanFirst;
    }

    public void setScanFirst(Integer scanFirst)
    {
        _scanFirst = scanFirst;
    }

    public Integer getScanLast()
    {
        return _scanLast;
    }

    public void setScanLast(Integer scanLast)
    {
        _scanLast = scanLast;
    }

    public Integer getScanCount()
    {
        return _scanCount;
    }

    public void setScanCount(Integer scanCount)
    {
        _scanCount = scanCount;
    }

    public Double getTotalIntensity()
    {
        return _totalIntensity;
    }

    public void setTotalIntensity(Double totalIntensity)
    {
        _totalIntensity = totalIntensity;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Integer getMs2Scan()
    {
        return _ms2Scan;
    }

    public void setMs2Scan(Integer ms2Scan)
    {
        _ms2Scan = ms2Scan;
    }

    public Double getMs2ConnectivityProbability()
    {
        return _ms2ConnectivityProbability;
    }

    public void setMs2ConnectivityProbability(Double ms2ConnectivityProbability)
    {
        _ms2ConnectivityProbability = ms2ConnectivityProbability;
    }

    public Integer getRunId() throws SQLException
    {
        if(null == _runId)
            _runId = MS1Manager.get().getRunIdFromFeature(_featureId);
        return _runId;
    }

    public static class PrevNextScans
    {
        public PrevNextScans(Integer prev, Integer next)
        {
            _prev = prev;
            _next = next;
        }

        public Integer getPrev()
        {
            return _prev;
        }

        public Integer getNext()
        {
            return _next;
        }

        private Integer _prev = null;
        private Integer _next = null;
    }

    public PrevNextScans getPrevNextScan(int scan, double mzLow, double mzHigh) throws SQLException
    {
        Table.TableResultSet rs = null;
        Integer prevScan = null;
        Integer nextScan = null;

        try
        {
            rs = MS1Manager.get().getScanList(getRunId().intValue(), mzLow, mzHigh,
                                            _scanFirst.intValue(), _scanLast.intValue());
            int curScan = 0;
            while(null != rs && rs.next())
            {
                curScan = rs.getInt("Scan");
                if(!rs.wasNull())
                {
                    if(curScan < scan)
                        prevScan = curScan;
                    if(curScan > scan && null == nextScan)
                    {
                        nextScan = curScan;
                        break;
                    }
                }
            }
        }
        finally
        {
            ResultSetUtil.close(rs);
        }

        return new PrevNextScans(prevScan, nextScan);
    }

    private int _featureId;
    private int _fileId;
    private Integer _scan;
    private Double _time;
    private Double _mz;
    private Boolean _accurateMz;
    private Double _mass;
    private Double _intensity;
    private Short _charge;
    private Short _chargeStates;
    private Double _kl;
    private Double _background;
    private Double _median;
    private Integer _peaks;
    private Integer _scanFirst;
    private Integer _scanLast;
    private Integer _scanCount;
    private Double _totalIntensity;
    private String _description;
    private Integer _ms2Scan;
    private Double _ms2ConnectivityProbability;
    private Integer _runId = null;
}
