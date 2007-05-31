package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTRange implements IsSerializable
{
    public double min;
    public double max;
    public boolean log;
    public double logLinSwitch;

    static private double log10(double value)
    {
        return Math.log(value) / Math.log(10);
    }

    public double adjustedLog10(double val)
    {
        boolean neg = false;
        if (val < 0)
        {
            neg = true;
            val = -val;
        }

        double ret;
        if (val < logLinSwitch)
        {
            ret = val / logLinSwitch / Math.log(10);
        }
        else
        {
            ret = log10(val) + (1 - Math.log(logLinSwitch)) / Math.log(10);

        }
        return neg ? -ret : ret;
    }

    double adjustedPow10(double val)
    {
        boolean neg = false;
        if (val < 0)
        {
            neg = true;
            val = - val;
        }

        double ret;
        if (val < 1 / Math.log(10))
        {
            ret = val * logLinSwitch * Math.log(10);
        }
        else
        {
            ret = Math.pow(10, val - (1 - Math.log(logLinSwitch)) / Math.log(10));
        }

        return neg ? -ret : ret;
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
