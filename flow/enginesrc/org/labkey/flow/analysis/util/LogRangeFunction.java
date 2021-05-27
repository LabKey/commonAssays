/*
 * Copyright (c) 2011-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.analysis.util;

import java.io.Serializable;

/**
 * User: matthewb
 * Date: Sep 13, 2007
 */
public class LogRangeFunction implements RangeFunction, Serializable
{
    static public final int LOG_LIN_SWITCH = 50;

    private final double logLinSwitch;
    private final double min;
    private final double max;

    // precomputed constants
    private final double intersect;
    private final double scale;
    private final double adjust;

    public LogRangeFunction(double logLinSwitch, double min, double max)
    {
        if (logLinSwitch < 0)
            throw new IllegalStateException("logLinSwitch not set");
        this.logLinSwitch = logLinSwitch;
        // This scale makes sure that the linear slope is tangent to the log function
        intersect = 1/Math.log(10);                 // == log10(E) == .4343
        scale = intersect/logLinSwitch;             // magic slope to make 1st derivative continuous
        adjust = intersect - log10(logLinSwitch);

        this.min = min;
        this.max = max;
    }

    @Override
    public boolean isLogarithmic()
    {
        return true;
    }

    @Override
    public double getMin()
    {
        return min;
    }

    @Override
    public double getMax()
    {
        return max;
    }

    @Override
    public double compute(double val)
    {
        double sign = signum(val);
        val = Math.abs(val);
        if (val < logLinSwitch)
            return sign * val * scale;      // (0:logLinSwitch)->(0:1/log(10))
        else
            return sign * (log10(val) + adjust);   // (logLinSwitch:x)->(log(10):...
    }

    @Override
    public double invert(double val)
    {
        double sign = signum(val);
        val = Math.abs(val);
        if (val < intersect)
            return sign * val / scale;
        else
            return sign * Math.pow(10, val - adjust);
    }

    private double log10(double x)
    {
        return Math.log(x) * LOG_10_FACTOR;
    }
    private double signum(double x)
    {
        return x < 0 ? -1  : 1; // zero is a don't care
    }
}
