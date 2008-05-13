/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.contour.ContourDataset;
import org.jfree.data.Range;

import java.util.Arrays;

/**
 * Contour dataset where the Z value is the number of points at that particular location.
 */
public class DensityDataset extends AbstractXYDataset implements ContourDataset
    {
    int[][] _values;
    double[] _xValues;
    double[] _yValues;
    int _zMax;

    /**
     *
     * @param values the location of the points in a scatterplot
     * @param xValues the possible X values.  These values will be used as the buckets for calculating density.
     * @param yValues the possible Y values.
     */
    public DensityDataset(XYDataset values, double[] xValues, double[] yValues)
        {
        init(values, xValues, yValues);
        }

    protected void init(XYDataset values, double[] xValues, double[] yValues)
        {
        _xValues = xValues;
        _yValues = yValues;
        _values = new int[xValues.length][yValues.length];
        _zMax = 0;
        int count = values.getItemCount(0);
        for (int item = 0; item < count; item ++)
            {
            Number xNum = values.getX(0, item);
            Number yNum = values.getY(0, item);
            int x = findBucket(_xValues, xNum.doubleValue());
            int y = findBucket(_yValues, yNum.doubleValue());
            _values[x][y] ++;
            if (_values[x][y] > _zMax)
                _zMax = _values[x][y];
            }
        }

    public int getSeriesCount()
        {
        return 1;
        }

    public String getSeriesKey(int series)
        {
        return "First series";
        }

    public int getItemCount(int series)
        {
        return _xValues.length * _yValues.length;
        }

    public Number getX(int series, int item)
        {
        return new Double(_xValues[item / _yValues.length]);
        }

    public Number getY(int series, int item)
        {
        return new Double(_yValues[item % _yValues.length]);
        }

    public double getZValue(int series, int item)
        {
        return _values[item / _yValues.length][item % _yValues.length];
        }

    public double getXValue(int series, int item)
        {
        return _xValues[item / _yValues.length];
        }

    public double getYValue(int series, int item)
        {
        return _yValues[item % _yValues.length];
        }

    public Number getZ(int series, int item)
        {
        double Z = getZValue(series, item);
        if (Z == 0)
            return null;
        return new Double(Z);
        }

    public double getMinZValue()
        {
        return (double) 0;
        }

    public double getMaxZValue()
        {
        return (double) _zMax;
        }

    public Number[] getYValues()
        {
        Number[] ret = new Number[_xValues.length * _yValues.length];
        for (int i = 0; i < ret.length; i ++)
            {
            ret[i] = getY(0, i);
            }
        return ret;
        }

    public Number[] getXValues()
        {
        Number[] ret = new Number[_xValues.length * _yValues.length];
        for (int i = 0; i < ret.length; i ++)
            {
            ret[i] = getX(0, i);
            }
        return ret;
        }

    public Number[] getZValues()
        {
        Number[] ret = new Number[_xValues.length * _yValues.length];
        for (int i = 0; i < ret.length; i ++)
            {
            ret[i] = getZ(0, i);
            }
        return ret;
        }

    public int[] getXIndices()
        {
        int[] ret = new int[_xValues.length];
        for (int i = 0; i < ret.length; i ++)
            {
            ret[i] = i * _yValues.length;
            }
        return ret;
        }

    public int[] indexX()
        {
        int[] ret = new int[_xValues.length * _yValues.length];
        for (int i = 0; i < ret.length; i ++)
            {
            ret[i] = i / _yValues.length;
            }
        return ret;
        }

    public boolean isDateAxis(int axisNumber)
        {
        return false;
        }

    public Range getZValueRange(Range xRange, Range yRange)
        {
        int xMin = findBucket(_xValues, xRange.getLowerBound());
        int xMax = findBucket(_xValues, xRange.getUpperBound());
        int yMin = findBucket(_yValues, yRange.getLowerBound());
        int yMax = findBucket(_yValues, yRange.getUpperBound());
        int zMin = Integer.MAX_VALUE;
        int zMax = Integer.MIN_VALUE;
        for (int x = xMin; x <= xMax; x ++)
            {
            for (int y = yMin; y <= yMax; y ++)
                {
                int z = _values[x][y];
                if (zMin > z)
                    zMin = z;
                if (zMax < z)
                    zMax = z;
                }
            }
        return new Range(zMin, zMax);
        }

    /**
     * Find the nearest value in the array.
     */
    static public int findBucket(double[] range, double value)
        {
        if (value <= range[0])
            return 0;
        if (value >= range[range.length - 1])
            return range.length - 1;
        int guess = (int) Math.round((range.length - 1) * (value - range[0]) / (range[range.length - 1] - range[0]));
        if (value == range[guess])
            return guess;
        guess = Arrays.binarySearch(range, value);
        if (guess >= 0)
            return guess;
        guess = guess * -1 - 1;
        assert range[guess] > value;
        if (guess == 0)
            return guess;
        if (value - range[guess - 1] < range[guess] - value)
            return guess - 1;
        return guess;
        }
    public double[] getPossibleXValues()
        {
        return _xValues;
        }
    public double[] getPossibleYValues()
        {
        return _yValues;
        }

    }
