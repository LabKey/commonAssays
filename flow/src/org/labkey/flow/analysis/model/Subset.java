package org.labkey.flow.analysis.model;

import java.util.BitSet;

/**
 */
public class Subset
{
    String _name;
    FCS _fcs;
    DataFrame _data;
    Subset _parent;

    public Subset(Subset parent, String name, FCS fcs, DataFrame data)
    {
        _parent = parent;
        _name = name;
        _fcs = fcs;
        _data = data;
    }

    public Subset(FCS fcs)
    {
        this(null, null, fcs, fcs.getScaledData());
    }

    public Subset apply(CompensationMatrix matrix)
    {
        DataFrame data = matrix.getCompensatedData(_data);
        return new Subset(_parent, _name, _fcs, data);
    }

    public Subset apply(Gate gate)
    {
        return apply(gate._name, new Gate[]{gate});
    }

    public Subset apply(String name, BitSet bits)
    {
        String newName;
        if (_name == null)
            newName = name;
        else
            newName = _name + "/" + name;
        assert bits.length() <= getDataFrame().getRowCount();
        DataFrame data = _data.Filter(bits);
        return new Subset(this, newName, _fcs, data);
    }

    public Subset apply(String name, Gate[] gates)
    {
        if (gates.length == 0)
            return new Subset(this, name, _fcs, _data);
        BitSet bits = gates[0].apply(_data);
        for (int i = 1; i < gates.length; i ++)
        {
            bits.or(gates[i].apply(_data));
        }
        return apply(name, bits);
    }

    public FCS getFCS()
    {
        return _fcs;
    }

    public DataFrame getDataFrame()
    {
        return _data;
    }

    public String getName()
    {
        if (_name == null)
            return "Ungated";
        return _name;
    }

    public Subset getParent()
    {
        return _parent;
    }

    public String getRawName()
    {
        return _name;
    }
}
