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

import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Class corresponding to a <groupanalysis> element in a FlowJo workspace.
 */
public class PopulationSet implements Serializable, Cloneable
{
    List<Population> _populations = new ArrayList<Population>();
    String _name;

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public List<Population> getPopulations()
    {
        return _populations;
    }

    public Population getPopulation(String name)
    {
        for (Population pop : _populations)
        {
            if (pop._name.equals(name))
                return pop;
        }
        return null;
    }

    public void addPopulation(Population population)
    {
        _populations.add(population);
    }

    public boolean equals(Object other)
    {
        if (other.getClass() != this.getClass())
            return false;
        PopulationSet group = (PopulationSet) other;
        if (!_populations.equals(group._populations))
            return false;
        return true;
    }

    public int hashCode()
    {
        return _populations.hashCode();
    }

    public boolean requiresCompensationMatrix()
    {
        for (Population child : getPopulations())
        {
            if (child.requiresCompensationMatrix())
                return true;
        }
        return false;
    }
}