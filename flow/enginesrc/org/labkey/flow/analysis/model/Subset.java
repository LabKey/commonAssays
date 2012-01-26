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

import java.util.BitSet;

/**
 */
public class Subset
{
    /** A full name path from the root. */
    String _name;
    FCSHeader _fcs;
    DataFrame _data;
    Subset _parent;

    public Subset(Subset parent, String name, FCSHeader fcs, DataFrame data)
    {
        _parent = parent;
        _name = name;
        _fcs = fcs;
        _data = data;
    }

    public Subset(FCS fcs, ScriptSettings settings)
    {
        this(null, null, fcs, fcs.getScaledData(settings));
    }

    public Subset apply(CompensationMatrix matrix) throws FlowException
    {
        assert (_fcs.isPrecompensated() && !_fcs.getSpill().equals(matrix)) || !_fcs.isPrecompensated(): "Applying identical compensation to machine compensated FCS file";
        DataFrame data = matrix.getCompensatedData(_data);
        return new Subset(_parent, _name, _fcs, data);
    }

    public Subset apply(PopulationSet populations, Gate gate)
    {
        return apply(populations, gate._name == null ? null : gate._name.getName(), new Gate[]{gate});
    }

    public Subset apply(String name, BitSet bits)
    {
        String newName;
        if (_name == null)
            newName = name;
        else
            newName = _name + "/" + name;
        assert bits.length() <= getDataFrame().getRowCount();
        DataFrame data = _data.filter(bits);
        return new Subset(this, newName, _fcs, data);
    }

    public Subset apply(PopulationSet populations, String name, Gate[] gates)
    {
        if (gates.length == 0)
            return new Subset(this, name, _fcs, _data);
        BitSet bits = gates[0].apply(populations, _data);
        for (int i = 1; i < gates.length; i ++)
        {
            bits.or(gates[i].apply(populations, _data));
        }
        return apply(name, bits);
    }

    public FCSHeader getFCSHeader()
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
