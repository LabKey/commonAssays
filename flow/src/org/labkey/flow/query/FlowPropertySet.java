/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

package org.labkey.flow.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetExpression;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.util.KeywordUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowPropertySet
{
    static private final Logger _log = Logger.getLogger(FlowPropertySet.class);
    private Container _container;

    private Collection<AttributeCache.KeywordEntry> _keywords;
    private Collection<AttributeCache.StatisticEntry> _statistics;
    private Collection<AttributeCache.GraphEntry> _graphs;
    private Map<String, SubsetSpec> _subsetNameAncestorMap;

    public FlowPropertySet(ExpDataTable table)
    {
        _container = table.getContainer();
    }

    public FlowPropertySet(Container c)
    {
        _container = c;
    }

    static protected Map<String, SubsetSpec> getSubsetNameAncestorMap(Collection<SubsetSpec> subsets)
    {
        Map<String, SubsetSpec> ret = new HashMap<>();
        for (SubsetSpec spec : subsets)
        {
            if (spec != null)
            {
                String name = spec.getSubset().toString();
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

    protected SubsetExpression simplifySubsetExpr(SubsetExpression subsetExpression)
    {
        // UNDONE: operate on the expression nodes instead of the string
        //if (subsetExpression instanceof SubsetExpression.SubsetTerm &&
        //        !((SubsetExpression.SubsetTerm)subsetExpression).getSpec().isExpression())
        //    return subsetExpression;
        
        String expr = subsetExpression.toString();
        if (expr.contains("|"))
            return subsetExpression;
        if (!expr.startsWith("(") || !expr.endsWith(")"))
            return subsetExpression;
        expr = expr.substring(1, expr.length() - 1);
        String[] names = StringUtils.split(expr, "&");
        StringBuilder ret = new StringBuilder();
        for (String name : names)
        {
            if (!name.endsWith("+"))
                return SubsetExpression.expression(expr);
            if (name.startsWith("!"))
            {
                name = name.substring(1, name.length() - 1) + "-";
            }
            ret.append(name);
        }
        return SubsetExpression.expression(ret.toString());
    }

    public SubsetSpec simplifySubset(SubsetSpec subset)
    {
        initStatisticsAndGraphs();
        if (subset == null)
            return null;
        PopulationName name = null;
        SubsetExpression expr = null;
        if (subset.isExpression())
            expr = simplifySubsetExpr(subset.getExpression());
        else
            name = subset.getPopulationName();

        SubsetSpec commonAncestor = _subsetNameAncestorMap.get(subset.getSubset().toString());
        if (commonAncestor == null)
            if (expr != null)
                return new SubsetSpec(subset.getParent(), expr);
            else
                return new SubsetSpec(subset.getParent(), name);

        if (commonAncestor.equals(subset))
        {
            if (expr != null)
                return new SubsetSpec(null, expr);
            else
                return new SubsetSpec(null, name);
        }
        try
        {
            // UNDONE: use expression tree instead of reparsing string
            SubsetSpec ret = SubsetSpec.fromEscapedString(subset.toString().substring(commonAncestor.toString().length() + 1));
            if (ret.isExpression())
                return new SubsetSpec(ret.getParent(), simplifySubsetExpr(ret.getExpression()));
            else
                return new SubsetSpec(ret.getParent(), ret.getPopulationName());
        }
        catch (Exception e)
        {
            _log.error("Error with subset '" + subset + "' and ancestor '" + commonAncestor + "'", e);
            return subset;
        }
    }

    private void initStatisticsAndGraphs()
    {
        if (_subsetNameAncestorMap != null)
            return;
        _statistics = AttributeCache.STATS.byContainer(_container);
        _graphs = AttributeCache.GRAPHS.byContainer(_container);
        Set<SubsetSpec> subsets = new HashSet<>();
        for (AttributeCache.StatisticEntry stat : _statistics)
        {
            StatisticSpec spec = stat.getAttribute();
            subsets.add(spec.getSubset());
        }
        for (AttributeCache.GraphEntry graph : _graphs)
        {
            GraphSpec spec = graph.getAttribute();
            subsets.add(spec.getSubset());
        }
        _subsetNameAncestorMap = getSubsetNameAncestorMap(subsets);
    }

    public Collection<AttributeCache.StatisticEntry> getStatistics()
    {
        initStatisticsAndGraphs();
        return _statistics;
    }

    public Collection<AttributeCache.GraphEntry> getGraphProperties()
    {
        initStatisticsAndGraphs();
        return _graphs;
    }

    public Collection<AttributeCache.KeywordEntry> getKeywordProperties()
    {
        if (_keywords == null)
        {
            _keywords = AttributeCache.KEYWORDS.byContainer(_container);
        }
        return _keywords;
    }

    public Collection<String> getVisibleKeywords()
    {
        List<String> visible = new ArrayList<>();
        for (AttributeCache.KeywordEntry entry : getKeywordProperties())
        {
            if (!KeywordUtil.isHidden(entry.getAttribute()))
                visible.add(entry.getAttribute());
        }
        return visible;
    }
}
