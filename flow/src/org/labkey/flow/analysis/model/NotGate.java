package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.BitSet;
import java.util.List;


public class NotGate extends Gate
{
    Gate _gate;

    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        NotGate otherGate = (NotGate) other;
        return _gate.equals(otherGate._gate);
    }

    public void setGate(Gate gate)
    {
        _gate = gate;
    }
    public Gate getGate()
    {
        return _gate;
    }

    public int hashCode()
    {
        return super.hashCode() ^ _gate.hashCode();
    }

    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        _gate.getPolygons(list, xAxis, yAxis);
    }

    public BitSet apply(DataFrame data)
    {
        BitSet bits = _gate.apply(data);
        bits.flip(0, data.getRowCount());
        return bits;
    }

    public boolean requiresCompensationMatrix()
    {
        return _gate.requiresCompensationMatrix();
    }

    static public NotGate readNot(Element el)
    {
        NotGate ret = new NotGate();
        ret._gate = Gate.readGate(el);
        return ret;
    }
}
