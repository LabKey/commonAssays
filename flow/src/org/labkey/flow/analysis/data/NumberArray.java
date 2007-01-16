package org.labkey.flow.analysis.data;

/**
 */
public interface NumberArray
    {
    Number get(int index);
    double getDouble(int index);
    float getFloat(int index);
    int getInt(int index);

    void set(int index, double value);
    void set(int index, float value);
    void set(int index, int value);

    int size();
    }
