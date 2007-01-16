package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.BitSet;

public class AndGate extends GateList
{
    public BitSet apply(DataFrame data)
    {
        BitSet bits = null;
        for (Gate gate : _gates)
        {
            BitSet nextBits = gate.apply(data);
            if (bits == null)
                bits = nextBits;
            else
                bits.and(nextBits);
        }
        return bits;
    }

    static public AndGate readAnd(Element el)
    {
        AndGate ret = new AndGate();
        ret._gates = Gate.readGateList(el);
        return ret;
    }
}
