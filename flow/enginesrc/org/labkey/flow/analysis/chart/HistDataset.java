/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.flow.analysis.chart;

import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.labkey.flow.analysis.data.NumberArray;

public class HistDataset extends AbstractIntervalXYDataset
{
    double[] _xValues;
    int[] _yValues;

    public HistDataset(double[] bins, NumberArray numbers)
    {
        _xValues = bins;
        _yValues = new int[bins.length];
        for (int i = 0; i < numbers.size(); i ++)
        {
            double value = numbers.getDouble(i);
            int bucket = DensityDataset.findBucket(bins, value);
            _yValues[bucket] ++;
        }
    }

    public int getSeriesCount()
    {
        return 1;
    }

    public Comparable getSeriesKey(int series)
    {
        return "Series";
    }

    public int getItemCount(int series)
    {
        return _xValues.length;
    }

    public Double getX(int series, int item)
    {
        return _xValues[item];
    }

    public Integer getY(int series, int item)
    {
        return _yValues[item];
    }

    public Double getStartX(int series, int item)
    {
        if (item > 0)
        {
            return (_xValues[item] + _xValues[item - 1]) / 2;
        }
        return _xValues[0];
    }

    public Double getEndX(int series, int item)
    {
        if (item < _xValues.length - 1)
        {
            return (_xValues[item] + _xValues[item + 1]) / 2;
        }
        return _xValues[item];
    }

    public Number getEndY(int series, int item)
    {
        return getY(series, item) + 1;
    }

    public Number getStartY(int series, int item)
    {
        return getY(series, item) - 1;
    }
}
