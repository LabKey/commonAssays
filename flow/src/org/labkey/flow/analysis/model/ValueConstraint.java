package org.labkey.flow.analysis.model;

public class ValueConstraint
{
    private double _minValue;
    private double _maxValue;

    public ValueConstraint(double minValue, double maxValue)
    {
        _minValue = minValue;
        _maxValue = maxValue;
    }

    public double constrain(double value)
    {
        if (value < _minValue)
        {
            return _minValue;
        }
        if (value > _maxValue)
        {
            return _maxValue;
        }
        return value;
    }


}
