package org.labkey.flow.query;

import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.api.data.ColumnInfo;
import org.apache.commons.lang.StringUtils;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import java.util.*;

public class FlowPropertySet
{
    Map<String, Integer> _keywords;
    Map<StatisticSpec, Integer> _statistics;
    Map<GraphSpec, Integer> _graphs;
    Map<String, SubsetSpec> _subsetNameAncestorMap;

    public FlowPropertySet(ExpDataTable table)
    {
        ColumnInfo colDataId = table.getColumn("RowId");
        if (colDataId == null)
        {
            throw new IllegalArgumentException("Table must have rowid column");
        }
        _keywords = AttributeCache.KEYWORDS.getAttrValues(colDataId);
        _statistics = AttributeCache.STATS.getAttrValues(colDataId);
        _graphs = AttributeCache.GRAPHS.getAttrValues(colDataId);
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

    public Map<StatisticSpec, Integer> getStatistics()
    {
        return _statistics;
    }

    public Map<GraphSpec, Integer> getGraphProperties()
    {
        return _graphs;
    }

    public Map<String, Integer> getKeywordProperties()
    {
        return _keywords;
    }
}
