package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.BitSet;
import java.util.List;

import org.labkey.flow.analysis.data.NumberArray;

/**
 */
public class IntervalGate extends Gate
{
    String _axis;
    double _min;
    double _max;

    public IntervalGate(String axis, double min, double max)
    {
        _axis = axis;
        _min = min;
        _max = max;
    }

    public BitSet apply(DataFrame data)
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

    public String getAxis()
    {
        return _axis;
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
        if (xAxis.equals(_axis))
        {
            X = new double[]{_min, _min, _max, _max};
        }
        if (yAxis.equals(_axis))
        {
            Y = new double[]{_min, _max, _max, _min};
        }
        if (X == null && Y == null)
        {
            return;
        }
        if (X == null)
        {
            X = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        }
        if (Y == null)
        {
            Y = new double[]{-Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
        }
        polys.add(new Polygon(X, Y));
    }

    public boolean requiresCompensationMatrix()
    {
        return CompensationMatrix.isParamCompensated(_axis);
    }

}
