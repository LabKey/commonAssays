package org.labkey.flow.analysis.data;

/**
 */
public class SubsetNumberArray implements NumberArray
    {
    NumberArray _array;
    IntArray _subset;
    public SubsetNumberArray(NumberArray array, IntArray subset)
        {
        _array = array;
        _subset = subset;
        }

    public Number get(int index)
        {
        return _array.get(_subset.getInt(index));
        }

    public double getDouble(int index)
        {
        return _array.getDouble(_subset.getInt(index));
        }

    public float getFloat(int index)
        {
        return _array.getFloat(_subset.getInt(index));
        }

    public int getInt(int index)
        {
        return _array.getInt(_subset.getInt(index));
        }

    public void set(int index, double value)
        {
        throw new UnsupportedOperationException();
        }

    public void set(int index, float value)
        {
        throw new UnsupportedOperationException();
        }

    public void set(int index, int value)
        {
        throw new UnsupportedOperationException();
        }

    public int size()
        {
        return _subset.size();
        }

    public NumberArray getArray()
        {
        return _array;
        }
    public IntArray getSubset()
        {
        return _subset;
        }
    }