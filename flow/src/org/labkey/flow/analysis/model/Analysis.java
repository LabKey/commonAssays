package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.*;
import java.io.Serializable;

public class Analysis extends ScriptComponent
{
    List<SubsetSpec> _subsets = new ArrayList();
    List<StatisticSpec> _statistics = new ArrayList();
    List<GraphSpec> _graphs = new ArrayList();

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
            if (spec.getSubset() != null && spec.getSubset().getParent() == null && spec.getSubset().getSubset().equals("*"))
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
                GraphSpec graph = new GraphSpec(parent, poly.getX(), poly.getY());
                graphs.add(graph);
            }
            else if (gate instanceof IntervalGate)
            {
                IntervalGate interval = (IntervalGate) gate;
                GraphSpec graph = new GraphSpec(parent, interval.getAxis());
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
}
