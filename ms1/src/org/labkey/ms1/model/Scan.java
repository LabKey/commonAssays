package org.labkey.ms1.model;

/**
 * Represents an MS1 Scan
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 16, 2007
 * Time: 3:01:26 PM
 */
public class Scan
{
    public int getScanId()
    {
        return _scanId;
    }

    public void setScanId(int scanId)
    {
        _scanId = scanId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getObservationDuration()
    {
        return _observationDuration;
    }

    public void setObservationDuration(Double observationDuration)
    {
        _observationDuration = observationDuration;
    }

    public String toString()
    {
        return "Scan (id=" + _scanId + ", scan=" + _scan + ")";
    }

    private int _scanId = -1;
    private int _fileId = -1;
    private int _scan = -1;
    private Double _retentionTime;
    private Double _observationDuration;
}
