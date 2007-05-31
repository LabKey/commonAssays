package org.labkey.flow.analysis.data;

public class ConstrainedNumberArray implements NumberArray
{
    NumberArray _array;
    double _minValue;
    double _maxValue;
    public ConstrainedNumberArray(NumberArray array, double minValue, double maxValue)
    {
        _array = array;
        _minValue = minValue;
        _maxValue = maxValue;
    }


    public Number get(int index)
    {
        return getDouble(index);
    }

    public double getDouble(int index)
    {
        double ret = _array.getDouble(index);
        if (ret < _minValue)
            return _minValue;
        if (ret > _maxValue)
            return _maxValue;
        return ret;
    }

    public float getFloat(int index)
    {
        return (float) getDouble(index);
    }

    public int getInt(int index)
    {
        return (int) getDouble(index);
    }

    public void set(int index, double value)
    {
        _array.set(index, value);
    }

    public void set(int index, float value)
    {
        _array.set(index, value);
    }

    public void set(int index, int value)
    {
        _array.set(index, value);
    }

    public int size()
    {
        return _array.size();
    }
}
