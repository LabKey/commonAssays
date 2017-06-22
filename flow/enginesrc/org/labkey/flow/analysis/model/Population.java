/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 */
public class Population extends PopulationSet
{
    List<Gate> _gates = new ArrayList<>();

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

    @Override
    public boolean isSimilar(PopulationSet other)
    {
        if (!(other instanceof Population))
            return false;

        return isSimilar((Population)other);
    }

    private boolean isSimilar(Population other)
    {
        if (equals(other))
            return true;

        if (getGates().size() != other.getGates().size())
            return false;

        ArrayList<Gate> thisSortedGates = new ArrayList<>(getGates());
        thisSortedGates.sort(Gate.NAME_COMPARATOR);

        ArrayList<Gate> otherSortedGates = new ArrayList<>(other.getGates());
        otherSortedGates.sort(Gate.NAME_COMPARATOR);

        for (int i = 0; i < thisSortedGates.size(); i++)
        {
            Gate thisGate = thisSortedGates.get(i);
            Gate otherGate = otherSortedGates.get(i);
            if (!thisGate.isSimilar(otherGate))
                return false;
        }

        return super.isSimilar(other);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[Population ");
        sb.append(super.toString());
        sb.setLength(sb.length()-1);
        sb.append(",gates:").append(_gates.size());
        sb.append(")]");

        return sb.toString();
    }
}
