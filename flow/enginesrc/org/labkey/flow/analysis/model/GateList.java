/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

import org.labkey.flow.analysis.web.SubsetExpression;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

abstract public class GateList extends Gate
{
    protected List<Gate> _gates = new ArrayList<>();
    protected SubsetExpression _originalExpression = null;

    protected GateList(Gate... gates)
    {
        _gates.addAll(Arrays.asList(gates));
    }

    protected GateList(List<Gate> gates)
    {
        _gates.addAll(gates);
    }

    public List<Gate> getGates()
    {
        return _gates;
    }

    public void addGate(Gate gate)
    {
        _gates.add(gate);
    }

    @Override
    public boolean equals(Object other)
    {
        return super.equals(other) && _gates.equals(((GateList)other)._gates);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode() ^ _gates.hashCode();
    }

    @Override
    public boolean isSimilar(Gate other)
    {
        if (equals(other))
            return true;

        if (other.getClass() != this.getClass())
            return false;

        GateList otherGateList = (GateList)other;
        if (_gates.size() != otherGateList._gates.size())
            return false;

        for (int i = 0; i < _gates.size(); i++)
        {
            if (!_gates.get(i).isSimilar(otherGateList._gates.get(i)))
                return false;
        }

        return true;
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

    public SubsetExpression getOriginalExpression()
    {
        return _originalExpression;
    }

    public void setOriginalExpression(SubsetExpression expr)
    {
        _originalExpression = expr;
    }

}
