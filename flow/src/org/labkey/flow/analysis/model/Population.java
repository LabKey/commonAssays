package org.labkey.flow.analysis.model;

import java.util.List;
import java.util.ArrayList;

/**
 */
public class Population extends PopulationSet
{
    List<Gate> _gates = new ArrayList<Gate>();

    public List<Gate> getGates()
    {
        return _gates;
    }

    public void addGate(Gate gate)
    {
        if (gate == null)
            return;
        _gates.add(gate);
    }

    public int hashCode()
    {
        return _gates.hashCode() ^ super.hashCode();
    }

    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        Population pop = (Population) other;
        return _gates.equals(pop._gates);
    }

    public boolean requiresCompensationMatrix()
    {
        if (super.requiresCompensationMatrix())
            return true;
        for (Gate gate : getGates())
        {
            if (gate.requiresCompensationMatrix())
                return true;
        }
        return false;
    }
}
