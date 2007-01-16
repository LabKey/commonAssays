package org.labkey.flow.analysis.model;

import java.util.List;
import java.util.ArrayList;

abstract public class GateList extends Gate
{
    protected List<Gate> _gates = new ArrayList();
    public List<Gate> getGates()
    {
        return _gates;
    }
    public void addGate(Gate gate)
    {
        _gates.add(gate);
    }

    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        for (Gate gate : _gates)
        {
            gate.getPolygons(list, xAxis, yAxis);
        }
    }
    public boolean requiresCompensationMatrix()
    {
        for (Gate gate : _gates)
        {
            if (gate.requiresCompensationMatrix())
                return true;
        }
        return false;
    }
}
