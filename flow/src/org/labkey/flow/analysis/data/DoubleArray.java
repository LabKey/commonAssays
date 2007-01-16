package org.labkey.flow.analysis.data;

/**
 */
public class DoubleArray implements NumberArray
    {
    double[] _array;

    public Number get(int index)
        {
        return new Double(_array[index]);
        }

    public double getDouble(int index)
        {
        return _array[index];
        }

    public float getFloat(int index)
        {
        return (float) _array[index];
        }

    public int getInt(int index)
        {
        return (int) _array[index];
        }

    public void set(int index, double value)
        {
        _array[index] = value;
        }
    public void set(int index, float value)
        {
        _array[index] = value;
        }
    public void set(int index, int value)
        {
        _array[index] = value;
        }

    public int size()
        {
        return _array.length;
        }
    public int memSize()
        {
        return _array.length * 8;
        }
    }
