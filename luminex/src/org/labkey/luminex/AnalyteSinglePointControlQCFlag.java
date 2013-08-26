package org.labkey.luminex;

/**
 * User: jeckels
 * Date: 8/26/13
 */
public class AnalyteSinglePointControlQCFlag extends AbstractAnalyteQCFlag
{
    private int _singlePointControl;
    public AnalyteSinglePointControlQCFlag()
    {
        super();
    }

    public AnalyteSinglePointControlQCFlag(int runId, String description, int analyte, int singlePointControl)
    {
        super(runId, LuminexDataHandler.QC_FLAG_FI_FLAG_TYPE, description, analyte);
        setSinglePointControl(singlePointControl);
    }


    public int getSinglePointControl()
    {
        return _singlePointControl;
    }

    public void setSinglePointControl(int singlePointControl)
    {
        _singlePointControl = singlePointControl;
        setIntKey2(singlePointControl);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyteSinglePointControlQCFlag that = (AnalyteSinglePointControlQCFlag) o;

        if (getRunId() != that.getRunId()) return false;
        if (getFlagType() != null ? !getFlagType().equals(that.getFlagType()) : that.getFlagType() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) return false;
        if (getAnalyte() != that.getAnalyte()) return false;
        if (_singlePointControl != that._singlePointControl) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = getRunId();
        result = 31 * result + (getFlagType() != null ? getFlagType().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + getAnalyte();
        result = 31 * result + _singlePointControl;
        return result;
    }

}
