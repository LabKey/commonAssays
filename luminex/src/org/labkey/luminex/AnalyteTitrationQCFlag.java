package org.labkey.luminex;

import org.labkey.api.exp.ExpQCFlag;

/**
 * User: cnathe
 * Date: Jan 11, 2012
 */
public class AnalyteTitrationQCFlag extends ExpQCFlag
{
    private int _analyte;
    private int _titration;

    public AnalyteTitrationQCFlag() {}

    public AnalyteTitrationQCFlag(int runId, String flagType, String description, int analyte, int titration)
    {
        super(runId, flagType, description);
        setAnalyte(analyte);
        setTitration(titration);
        setEnabled(true);
    }

    public int getAnalyte()
    {
        return _analyte;
    }

    public void setAnalyte(int analyte)
    {
        _analyte = analyte;
        setIntKey1(analyte);
    }

    public int getTitration()
    {
        return _titration;
    }

    public void setTitration(int titration)
    {
        _titration = titration;
        setIntKey2(titration);
    }

    public void setRun(int run)
    {
        setRunId(run);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyteTitrationQCFlag that = (AnalyteTitrationQCFlag) o;

        if (getRunId() != that.getRunId()) return false;
        if (getFlagType() != null ? !getFlagType().equals(that.getFlagType()) : that.getFlagType() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) return false;
        if (_analyte != that._analyte) return false;
        if (_titration != that._titration) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = getRunId();
        result = 31 * result + (getFlagType() != null ? getFlagType().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + _analyte;
        result = 31 * result + _titration;
        return result;
    }
}

