package org.labkey.flow.analysis.util;

abstract class AbstractRangeFunction implements RangeFunction
{
    final double _min;
    final double _max;

    protected AbstractRangeFunction(double min, double max)
    {
        _min = min;
        _max = max;
    }

    @Override
    public double getMin()
    {
        return _min;
    }

    @Override
    public double getMax()
    {
        return _max;
    }

    protected double getWidth()
    {
        return _max - _min;
    }

}
