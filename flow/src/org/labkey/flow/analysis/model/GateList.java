/*
 * Copyright (c) 2005-2007 LabKey Corporation
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
