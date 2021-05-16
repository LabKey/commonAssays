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
 *
 * Add 1 to log10(val) to display values < 1.
 */
public class SimpleLogRangeFunction implements RangeFunction, Serializable
{
    @Override
    public double getMin()
    {
        return 0;
    }

    @Override
    public double getMax()
    {
        return 0;
    }

    @Override
    public boolean isLogarithmic()
    {
        return true;
    }

    @Override
    public double compute(double val)
    {
        double sign = signum(val);
        val = Math.abs(val);
        if (val == 0)
            return 0;
        double l = log10(val) + 1;
        return l < 0 ? 0 : sign * l;
    }

    @Override
    public double invert(double val)
    {
        if (val == 0)
            return 0;
        double sign = signum(val);
        val = Math.abs(val);
        return sign * Math.pow(10, val-1);
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
