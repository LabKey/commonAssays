package org.labkey.elisa.query;

import org.labkey.api.data.Entity;

public class CurveFitDb extends Entity
{
    private Integer _rowId;
    private Integer _runId;
    private Integer _protocolId;
    private String _plateName;
    private Integer _spot;
    private Double _rSquared;
    private String _fitParameters;

    public boolean isNew()
    {
        return _rowId == null;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    public Integer getRunId()
    {
        return _runId;
    }

    public void setRunId(Integer runId)
    {
        _runId = runId;
    }

    public Integer getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(Integer protocolId)
    {
        _protocolId = protocolId;
    }

    public String getPlateName()
    {
        return _plateName;
    }

    public void setPlateName(String plateName)
    {
        _plateName = plateName;
    }

    public Integer getSpot()
    {
        return _spot;
    }

    public void setSpot(Integer spot)
    {
        _spot = spot;
    }

    public Double getrSquared()
    {
        return _rSquared;
    }

    public void setrSquared(Double rSquared)
    {
        _rSquared = rSquared;
    }

    public String getFitParameters()
    {
        return _fitParameters;
    }

    public void setFitParameters(String fitParameters)
    {
        _fitParameters = fitParameters;
    }
}
