/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.flow.analysis.model;

import org.labkey.api.arrays.DoubleArray;

import java.util.List;
import java.util.Arrays;
import java.util.StringTokenizer;

public class FixedCalibrationTable implements CalibrationTable
{
    double[] values;

    public FixedCalibrationTable(double[] values)
    {
        this.values = values;
    }

    public FixedCalibrationTable(DoubleArray values)
    {
        this.values = values.toArray(new double[values.size()]);
    }

    public FixedCalibrationTable(List<Double> values)
    {
        this.values = new double[values.size()];
        for (int i = 0; i < this.values.length; i ++)
        {
            this.values[i] = values.get(i);
        }
    }

    public double indexOf(double value)
    {
        if (value < values[0])
        {
            return value - values[0];
        }
        if (value > values[values.length - 1])
        {
            return values.length - 1 + value - values[values.length - 1];
        }


        int index = Arrays.binarySearch(values, value);
        if (index >= 0)
            return index;
        index = -index - 1;
        if (index == 0)
        {
            return value - values[0];
        }
        if (index >= values.length - 1)
        {
            return index;
        }
        double prev = values[index - 1];
        double next = values[index + 1];
        return index - 1 + (value - prev) / (next - prev);
    }

    public double fromIndex(double index)
    {
        if (index <= 0)
            return values[0] + index;
        if (index >= values.length - 1)
        {
            return values[values.length - 1] + index - (values.length - 1);
        }
        int prevIndex = (int) Math.floor(index);
        int nextIndex = (int) Math.ceil(index);
        if (prevIndex == nextIndex)
        {
            return values[prevIndex];
        }
        return (nextIndex - index) * values[prevIndex] + (index - prevIndex) * values[nextIndex];
    }

    public double getRange()
    {
        return values.length;
    }

    public boolean isLinear()
    {
        return false;
    }

    static public FixedCalibrationTable fromString(String str)
    {
        StringTokenizer tok = new StringTokenizer(str, ",");
        DoubleArray values = new DoubleArray();
        while (tok.hasMoreElements())
        {
            tok.nextElement();
            if (!tok.hasMoreElements())
                break;
            values.add(Double.parseDouble(tok.nextToken()));
        }
        return new FixedCalibrationTable(values);
    }
}
