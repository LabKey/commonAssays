package org.labkey.flow.query;

import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.apache.commons.lang.StringUtils;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import java.util.*;

public class FlowPropertySet
{
    private Container _container;
    private ColumnInfo _colDataId;
    private Map<String, Integer> _keywords;
    private Map<StatisticSpec, Integer> _statistics;
    private Map<GraphSpec, Integer> _graphs;
    private Map<String, SubsetSpec> _subsetNameAncestorMap;

    public FlowPropertySet(ExpDataTable table)
    {
        _colDataId = table.getColumn("RowId");
        _container = table.getContainer();
    }

    static protected Map<String, SubsetSpec> getSubsetNameAncestorMap(Collection<SubsetSpec> subsets)
    {
        Map<String, SubsetSpec> ret = new HashMap();
        for (SubsetSpec spec : subsets)
        {
            if (spec != null)
            {
                String name = spec.getSubset();
                if (ret.containsKey(name))
                {
                    SubsetSpec spec2 = ret.get(name);
                    ret.put(name, SubsetSpec.commonAncestor(spec, spec2));
                }
                else
                {
                    ret.put(name, spec);
                }
            }
        }
        return ret;
    }

    protected String simplifySubsetExpr(String expr)
    {
        if (expr.indexOf("|") >= 0)
            return expr;
        if (!expr.startsWith("(") || !expr.endsWith(")"))
            return expr;
        expr = expr.substring(1, expr.length() - 1);
        String[] names = StringUtils.split(expr, "&");
        StringBuilder ret = new StringBuilder();
        for (String name : names)
        {
            if (!name.endsWith("+"))
                return expr;
            if (name.startsWith("!"))
            {
                name = name.substring(1, name.length() - 1) + "-";
            }
            ret.append(name);
        }
        return ret.toString();
    }

    public SubsetSpec simplifySubset(SubsetSpec subset)
    {
        initStatisticsAndGraphs();
        if (subset == null)
            return null;
        String name = simplifySubsetExpr(subset.getSubset());

        SubsetSpec commonAncestor = _subsetNameAncestorMap.get(subset.getSubset());
        if (commonAncestor == null)
            return new SubsetSpec(subset.getParent(), name);

        if (commonAncestor.equals(subset))
        {
            return new SubsetSpec(null, name);
        }
        SubsetSpec ret = SubsetSpec.fromString(subset.toString().substring(commonAncestor.toString().length() + 1));
        return new SubsetSpec(ret.getParent(), simplifySubsetExpr(ret.getSubset()));
    }

    private void initStatisticsAndGraphs()
    {
        if (_subsetNameAncestorMap != null)
            return;
        _statistics = AttributeCache.STATS.getAttrValues(_container, _colDataId);
        _graphs = AttributeCache.GRAPHS.getAttrValues(_container, _colDataId);
        Set<SubsetSpec> subsets = new HashSet();
        for (StatisticSpec stat : _statistics.keySet())
        {
            subsets.add(stat.getSubset());
        }
        for (GraphSpec graph : _graphs.keySet())
        {
            subsets.add(graph.getSubset());
        }
        _subsetNameAncestorMap = getSubsetNameAncestorMap(subsets);
    }

    public Map<StatisticSpec, Integer> getStatistics()
    {
        initStatisticsAndGraphs();
        return _statistics;
    }

    public Map<GraphSpec, Integer> getGraphProperties()
    {
        initStatisticsAndGraphs();
        return _graphs;
    }

    public Map<String, Integer> getKeywordProperties()
    {
        if (_keywords == null)
        {
            _keywords = AttributeCache.KEYWORDS.getAttrValues(_container, _colDataId); 
        }
        return _keywords;
    }
}
