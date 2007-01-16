package org.labkey.flow.analysis.data;

/**
 */
public class FloatArray implements NumberArray
    {
    float[] _array;
    public FloatArray(float[] array)
        {
        _array = array;
        }
    private FloatArray(NumberArray array)
        {
        _array = new float[array.size()];
        for (int i = 0; i < _array.length; i ++)
            {
            _array[i] = array.getFloat(i);
            }
        }

    public Number get(int index)
        {
        return new Float(_array[index]);
        }

    public double getDouble(int index)
        {
        return _array[index];
        }

    public float getFloat(int index)
        {
        return _array[index];
        }

    public int getInt(int index)
        {
        return (int) _array[index];
        }
    public void set(int index, double value)
        {
        _array[index] = (float) value;
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
        return _array.length * 4;
        }
    }
