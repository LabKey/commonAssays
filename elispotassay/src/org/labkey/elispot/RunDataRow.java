package org.labkey.elispot;

/**
 * Created by davebradlee on 3/20/15.
 */
public class RunDataRow
{
    private int _rowId;
    private int _runId;
    private String _specimenLsid;
    private Double _spotCount;
    private String _wellgroupName;
    private String _wellgroupLocation;
    private Double _normalizedSpotCount;
    private Integer _antigenId;
    private String _antigenName;
    private String _antigenWellgroupName;
    private Integer _cellWell;
    private String _analyte;
    private Double _activity;
    private Double _intensity;
    private String _objectUri;
    private int _objectId;          // TODO: remove when we remove use of exp.Object

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

    public String getSpecimenLsid()
    {
        return _specimenLsid;
    }

    public void setSpecimenLsid(String specimenLsid)
    {
        _specimenLsid = specimenLsid;
    }

    public Double getSpotCount()
    {
        return _spotCount;
    }

    public void setSpotCount(Double spotCount)
    {
        _spotCount = spotCount;
    }

    public String getWellgroupName()
    {
        return _wellgroupName;
    }

    public void setWellgroupName(String wellgroupName)
    {
        _wellgroupName = wellgroupName;
    }

    public String getWellgroupLocation()
    {
        return _wellgroupLocation;
    }

    public void setWellgroupLocation(String wellgroupLocation)
    {
        _wellgroupLocation = wellgroupLocation;
    }

    public Double getNormalizedSpotCount()
    {
        return _normalizedSpotCount;
    }

    public void setNormalizedSpotCount(Double normalizedSpotCount)
    {
        _normalizedSpotCount = normalizedSpotCount;
    }

    public Integer getAntigenId()
    {
        return _antigenId;
    }

    public void setAntigenId(Integer antigenId)
    {
        _antigenId = antigenId;
    }

    public String getAntigenName()
    {
        return _antigenName;
    }

    public void setAntigenName(String antigenName)
    {
        _antigenName = antigenName;
    }

    public String getAntigenWellgroupName()
    {
        return _antigenWellgroupName;
    }

    public void setAntigenWellgroupName(String antigenWellgroupName)
    {
        _antigenWellgroupName = antigenWellgroupName;
    }

    public Integer getCellWell()
    {
        return _cellWell;
    }

    public void setCellWell(Integer cellWell)
    {
        _cellWell = cellWell;
    }

    public String getAnalyte()
    {
        return _analyte;
    }

    public void setAnalyte(String analyte)
    {
        _analyte = analyte;
    }

    public Double getActivity()
    {
        return _activity;
    }

    public void setActivity(Double activity)
    {
        _activity = activity;
    }

    public Double getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Double intensity)
    {
        _intensity = intensity;
    }

    public String getObjectUri()
    {
        return _objectUri;
    }

    public void setObjectUri(String objectUri)
    {
        _objectUri = objectUri;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public void setObjectId(int objectId)
    {
        _objectId = objectId;
    }
}
