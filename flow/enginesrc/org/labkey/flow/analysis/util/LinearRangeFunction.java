package org.labkey.flow.analysis.util;

// NOTE: Unlike the other range functions, this adjusts for the min/max range
public class LinearRangeFunction extends AbstractRangeFunction
{
    private final double _gain;

    public LinearRangeFunction(double min, double max, double gain)
    {
        super(min, max);
        this._gain = gain;
    }

    @Override
    public boolean isLogarithmic()
    {
        return false;
    }

    @Override
    public double compute(double range)
    {
        if (range == 0d)
            return 0d;

        return ((range * getWidth()) + _min) / _gain;
    }

    @Override
    public double invert(double domain)
    {
        if (domain == 0d)
            return 0d;

        return ((domain * _gain) - _min) / getWidth();
    }
}
