/*
 * Copyright (c) 2005-2012 LabKey Corporation
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

import org.w3c.dom.Element;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.labkey.flow.analysis.data.NumberArray;

/**
 */
public class IntervalGate extends RegionGate
{
    static final double MAX_VALUE = Float.MAX_VALUE;    // using Double.MAX_VALUE is likely to cause errors if anytone tries arithmetic
    String _axis;
    double _min;
    double _max;

    public IntervalGate(String axis, double min, double max)
    {
        _axis = axis;
        _min = min;
        _max = max;
    }


    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        BitSet ret = new BitSet(data.getRowCount());
        NumberArray values = data.getColumn(_axis);
        for (int i = 0; i < data.getRowCount(); i ++)
        {
            if (values.getDouble(i) >= _min && values.getDouble(i) < _max)
            {
                ret.set(i, true);
            }
        }
        return ret;
    }


    public String getXAxis()
    {
        return _axis;
    }


    public String getYAxis()
    {
        return null;
    }

    public List<String> getAxes()
    {
        return Collections.singletonList(_axis);
    }


    public double getMin()
    {
        return _min;
    }


    public double getMax()
    {
        return _max;
    }


    static public IntervalGate readInterval(Element el)
    {
        IntervalGate ret = new IntervalGate(el.getAttribute("axis"), Double.parseDouble(el.getAttribute("min")), Double.parseDouble(el.getAttribute("max")));
        return ret;
    }


    public int hashCode()
    {
        return super.hashCode() ^ _axis.hashCode() ^ Double.valueOf(_max).hashCode() ^ Double.valueOf(_min).hashCode();
    }


    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        IntervalGate gate = (IntervalGate) other;
        if (!this._axis.equals(gate._axis))
            return false;
        return this._max == gate._max && this._min == gate._min;
    }


    public void getPolygons(List<Polygon> polys, String xAxis, String yAxis)
    {
        double[] X = null;
        double[] Y = null;
        if (null!=xAxis && xAxis.equals(_axis))
        {
            X = new double[]{_min, _min, _max, _max};
        }
        if (null!=yAxis && yAxis.equals(_axis))
        {
            Y = new double[]{_min, _max, _max, _min};
        }
        if (X == null && Y == null)
        {
            return;
        }
        if (X == null)
        {
            X = new double[]{-MAX_VALUE, -MAX_VALUE, MAX_VALUE, MAX_VALUE};
        }
        if (Y == null)
        {
            Y = new double[]{-MAX_VALUE, MAX_VALUE, MAX_VALUE, -MAX_VALUE};
        }
        polys.add(new Polygon(X, Y));
    }


    public boolean requiresCompensationMatrix()
    {
        return CompensationMatrix.isParamCompensated(_axis);
    }

    @Override
    public String toString()
    {
        return "[IntervalGate " + _name + " (axis:" + _axis + ",min:" + _min + ",max:" + _max + ")]";
    }
}
