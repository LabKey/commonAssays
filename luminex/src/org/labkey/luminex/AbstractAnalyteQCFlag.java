package org.labkey.luminex;

import org.labkey.api.exp.ExpQCFlag;

public abstract class AbstractAnalyteQCFlag extends ExpQCFlag
{
    private int _analyte;

    public AbstractAnalyteQCFlag() {}

    public AbstractAnalyteQCFlag(int runId, String flagType, String description, int analyte)
    {
        super(runId, flagType, description);
        setAnalyte(analyte);
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

    public void setRun(int run)
    {
        setRunId(run);
    }
}