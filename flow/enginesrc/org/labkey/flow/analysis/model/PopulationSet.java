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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class corresponding to a <groupanalysis> element in a FlowJo workspace.
 */
public class PopulationSet implements Serializable, Cloneable
{
    private final static Comparator<PopulationSet> NAME_COMPARATOR = Comparator.comparing(PopulationSet::getName);

    private final List<Population> _populations = new ArrayList<>();

    PopulationName _name;

    public PopulationName getName()
    {
        return _name;
    }

    public void setName(PopulationName name)
    {
        _name = name;
    }

    public List<Population> getPopulations()
    {
        return _populations;
    }

    public Population getPopulation(PopulationName name)
    {
        if (name == null)
            return null;
        
        for (Population pop : _populations)
        {
            if (pop._name.equals(name))
                return pop;
        }
        return null;
    }

    public void addPopulation(Population population)
    {
        assert population != null;
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

    @Override
    public String toString()
    {
        return _name.toString() + " (sub:" + _populations.size() + ")";
    }

    /**
     * Compares the populations of this PopulationSet to another.
     * If gating hierarchy is similar, the PopulationSets are similar.
     *
     * @param other PopulationSet to compare to.
     * @return true if similar.
     */
    public boolean isSimilar(PopulationSet other)
    {
        if (equals(other))
            return true;

        if (getPopulations().size() != other.getPopulations().size())
            return false;

        ArrayList<Population> thisSortedPopulation = new ArrayList<>(getPopulations());
        thisSortedPopulation.sort(NAME_COMPARATOR);

        ArrayList<Population> otherSortedPopulation = new ArrayList<>(other.getPopulations());
        otherSortedPopulation.sort(NAME_COMPARATOR);

        for (int i = 0; i < thisSortedPopulation.size(); i++)
        {
            Population thisPopulation = thisSortedPopulation.get(i);
            Population otherPopulation = otherSortedPopulation.get(i);
            if (!thisPopulation.isSimilar(otherPopulation))
                return false;
        }

        return true;
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
