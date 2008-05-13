/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
