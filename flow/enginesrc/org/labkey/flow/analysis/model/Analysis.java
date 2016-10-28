/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.*;

public class Analysis extends ScriptComponent
{
    List<SubsetSpec> _subsets = new ArrayList<>();
    List<StatisticSpec> _statistics = new ArrayList<>();
    List<GraphSpec> _graphs = new ArrayList<>();

    public List<StatisticSpec> getStatistics()
    {
        return _statistics;
    }

    public void addStatistic(StatisticSpec stat)
    {
        _statistics.add(stat);
    }

    public void addSubset(SubsetSpec subset)
    {
        _subsets.add(subset);
    }

    public List<SubsetSpec> getSubsets()
    {
        return _subsets;
    }

    public void addGraph(GraphSpec graph)
    {
        _graphs.add(graph);
    }

    public List<GraphSpec> getGraphs()
    {
        return _graphs;
    }

    public boolean requiresCompensationMatrix()
    {
        if (super.requiresCompensationMatrix())
            return true;
        for (StatisticSpec stat : _statistics)
        {
            if (stat.getParameter() == null)
                continue;
            if (CompensationMatrix.isParamCompensated(stat.getParameter()))
                return true;
        }
        for (GraphSpec graph : _graphs)
        {
            for (String param : graph.getParameters())
            {
                if (CompensationMatrix.isParamCompensated(param))
                    return true;
            }
        }
        return false;
    }

    private void materializeSubsets(Set<SubsetSpec> subsets, PopulationSet popset, SubsetSpec parent)
    {
        for (PopulationSet child : popset.getPopulations())
        {
            SubsetSpec cur = new SubsetSpec(parent, child.getName());
            subsets.add(cur);
            materializeSubsets(subsets, child, cur);
        }
    }

    private Set<SubsetSpec> materializeSubsets()
    {
        Set<SubsetSpec> ret = new HashSet();
        ret.add(null);
        ret.addAll(_subsets);
        materializeSubsets(ret, this, null);
        return ret;
    }

    public Set<StatisticSpec> materializeStatistics(Subset subset)
    {
        Set<StatisticSpec> ret = new HashSet();
        Set<SubsetSpec> allSubsets = materializeSubsets();
        for (StatisticSpec spec : _statistics)
        {
            Collection<SubsetSpec> subsets;
            if (spec.getSubset() != null && spec.getSubset().getParent() == null && PopulationName.ALL.equals(spec.getSubset().getSubset()))
            {
                subsets = allSubsets;
            }
            else
            {
                subsets = Collections.singleton(spec.getSubset());
            }
            for (SubsetSpec sSpec : subsets)
            {
                switch (spec.getStatistic())
                {
                    case Frequency:
                        if (sSpec == null)
                            continue;
                        break;
                    case Freq_Of_Parent:
                        if (sSpec == null)
                            continue;
                        break;
                    case Freq_Of_Grandparent:
                        if (sSpec == null || sSpec.getParent() == null)
                            continue;
                        break;
                }
                if (spec.getParameter() != null && ("*".equals(spec.getParameter()) || spec.getParameter().startsWith("*:")))
                {
                    String suffix = spec.getParameter().substring(1);
                    for (int i = 0; i < subset.getDataFrame().getColCount(); i++)
                    {
                        DataFrame.Field field = subset.getDataFrame().getField(i);
                        if (field.getName().startsWith(CompensationMatrix.DITHERED_PREFIX))
                            continue;
                        ret.add(new StatisticSpec(sSpec, spec.getStatistic(), field.getName() + suffix));
                    }
                }
                else
                {
                    ret.add(new StatisticSpec(sSpec, spec.getStatistic(), spec.getParameter()));
                }
            }
        }
        return ret;
    }

    private void materializeGraphs(Set<GraphSpec> graphs, Population population, SubsetSpec parent)
    {
        if (population.getGates().size() == 1)
        {
            Gate gate = population.getGates().get(0);
            if (gate instanceof PolygonGate)
            {
                PolygonGate poly = (PolygonGate) gate;
                GraphSpec graph = new GraphSpec(parent, poly.getXAxis(), poly.getYAxis());
                graphs.add(graph);
            }
            else if (gate instanceof IntervalGate)
            {
                IntervalGate interval = (IntervalGate) gate;
                GraphSpec graph = new GraphSpec(parent, interval.getXAxis());
                graphs.add(graph);
            }
        }
        SubsetSpec subset = new SubsetSpec(parent, population.getName());
        for (Population child : population.getPopulations())
        {
            materializeGraphs(graphs, child, subset);
        }
    }

    public Set<GraphSpec> materializeGraphs()
    {
        Set<GraphSpec> ret = new HashSet();
        ret.addAll(_graphs);
        for (Population child : getPopulations())
        {
            materializeGraphs(ret, child, null);
        }
        return ret;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Analysis analysis = (Analysis) o;

        if (!_graphs.equals(analysis._graphs)) return false;
        if (!_statistics.equals(analysis._statistics)) return false;
        if (!_subsets.equals(analysis._subsets)) return false;

        return true;
    }

    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + _subsets.hashCode();
        result = 31 * result + _statistics.hashCode();
        result = 31 * result + _graphs.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[Analysis ");
        sb.append(super.toString());
        sb.setLength(sb.length()-1);
        sb.append(",subsets:").append(_subsets.size());
        sb.append(",stats:").append(_statistics.size());
        sb.append(",graphs:").append(_graphs.size());
        sb.append(")]");

        return sb.toString();
    }
}
