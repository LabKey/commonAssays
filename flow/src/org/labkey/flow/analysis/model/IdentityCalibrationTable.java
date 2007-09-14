package org.labkey.flow.analysis.model;

public class IdentityCalibrationTable implements CalibrationTable
{
    double range;
    public IdentityCalibrationTable(double range)
    {
        this.range = range;
    }


    public double fromIndex(double index)
    {
        return index;
    }

    public double getRange()
    {
        return range;
    }

    public double indexOf(double value)
    {
        return value;
    }

    public boolean isLinear()
    {
        return true;
    }
}
