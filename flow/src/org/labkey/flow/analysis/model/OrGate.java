package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.BitSet;

public class OrGate extends GateList
{
    public BitSet apply(DataFrame data)
    {
        BitSet bits = null;
        for (Gate gate : _gates)
        {
            BitSet next = gate.apply(data);
            if (bits == null)
                bits = next;
            else
                bits.or(next);
        }
        return bits;
    }

    static public OrGate readOr(Element el)
    {
        OrGate ret = new OrGate();
        ret._gates = Gate.readGateList(el);
        return ret;
    }
}
