package org.labkey.flow.analysis.data;

import java.util.BitSet;

/**
 */
public class IntArray implements NumberArray
    {
    int[] _array;
    public IntArray(int[] array)
        {
        _array = array;
        }
    public IntArray(BitSet bits)
        {
        _array = new int[bits.cardinality()];
        int bitIndex;
        int index = 0;
        for (bitIndex = bits.nextSetBit(0); bitIndex != -1;
                index ++, bitIndex = bits.nextSetBit(bitIndex + 1))
            {
            _array[index] = bitIndex;
            }
        }
    public IntArray(IntArray other, BitSet bits)
        {
        _array = new int[bits.cardinality()];
        int bitIndex;
        int index = 0;
        for (bitIndex = bits.nextSetBit(0); bitIndex != -1;
                index ++, bitIndex = bits.nextSetBit(bitIndex + 1))
            {
            _array[index] = other.getInt(bitIndex);
            }
        }
    public Number get(int index)
        {
        return new Integer(_array[index]);
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
        return _array[index];
        }

    public void set(int index, double value)
        {
        _array[index] = (int) value;
        }

    public void set(int index, float value)
        {
        _array[index] = (int) value;
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
