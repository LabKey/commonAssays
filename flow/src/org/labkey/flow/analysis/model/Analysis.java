package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class Analysis extends PopulationSet
{
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
}
