package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.labkey.flow.gateeditor.client.util.LogAxisFunction;

public class GWTRange extends LogAxisFunction implements IsSerializable
{
    public double min;
    public double max;
    public boolean log;

    public GWTRange()   // for GWT serializablity
    {
        this(1);
    }

    public GWTRange(double logLinSwitch)
    {
        super(logLinSwitch);
    }

    public double adjustedLog10(double val)
    {
        return compute(val);
    }

    double adjustedPow10(double val)
    {
        return invert(val);
    }

    public double toScreen(double val, double width)
    {
        if (val < min)
            return -1;
        if (!log)
        {
            return Math.round((val - min) * width / (max - min));
        }
        return Math.round((adjustedLog10(val) - adjustedLog10(min)) / (adjustedLog10(max) - adjustedLog10(min)) * width);
    }

    public double roundValue(double value)
    {
        if (Math.abs(value) > 10)
            return Math.round(value);
        return Math.round(value * 10) / 10;
    }

    public double toValue(double coor, double width)
    {
        if (coor < 0)
            return roundValue(min - 1);
        if (!log)
        {
            return roundValue(min + (max - min) * coor / width);
        }
        return roundValue(adjustedPow10(adjustedLog10(min) + coor / width * (adjustedLog10(max) - adjustedLog10(min))));
    }
}
