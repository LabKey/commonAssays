/*
 * Copyright (c) 2007-2026 LabKey Corporation
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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.test.TestWhen;
import org.labkey.api.util.JunitUtil;
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
    static private final Logger _log = LogManager.getLogger(FlowPropertySet.class);
    private final Container _container;

    private Collection<AttributeCache.KeywordEntry> _keywords;
    private Collection<AttributeCache.StatisticEntry> _statistics;
    private Collection<AttributeCache.GraphEntry> _graphs;
    private Map<String, SubsetSpec> _subsetNameAncestorMap;
    // subsets processed and added to _subsetNameAncestorMap
    private Set<SubsetSpec> _subsets;

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
                    ret.compute(name, (_, spec2) -> SubsetSpec.commonAncestor(spec, spec2));
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

    /**
     * Returns a shortened form of {@code subset} for display, relative to the shortest ancestor path that is
     * common to every other subset in this container sharing the same leaf population name.
     * <p>
     * For example, if every occurrence of the leaf population "CD8+" in this container falls under
     * "Time/Singlets/Lymphocytes/CD3+", a subset "Time/Singlets/Lymphocytes/CD3+/CD8+" simplifies to "CD8+",
     * since the shared prefix carries no disambiguating information. If two subsets with the same leaf name
     * diverge under different ancestors, the common ancestor is correspondingly shorter (or nonexistent).
     * <p>
     * Because the ancestor lookup in {@link #_subsetNameAncestorMap} is keyed on leaf population name alone
     * (see {@link #getSubsetNameAncestorMap}), it is possible for two subsets with the same leaf name to come
     * from otherwise-unrelated gating trees (e.g. one rooted at "Time", another rooted directly at "Singlets").
     * In that case the ancestor recorded for the leaf name may not actually be an ancestor of {@code subset};
     * this method detects that (via {@link SubsetSpec#hasAncestor}) and returns {@code subset} unchanged
     * rather than simplifying against an ancestor that doesn't apply.
     *
     * @param subset the subset to simplify; returned unchanged if null or not one of the subsets processed by
     *               {@link #initStatisticsAndGraphs()}
     * @return a SubsetSpec equivalent to {@code subset} but with any common leading ancestor path removed,
     *         or {@code subset} itself if no (applicable) common ancestor was found
     */
    public SubsetSpec simplifySubset(SubsetSpec subset)
    {
        initStatisticsAndGraphs();

        // do not simplify if this subset was not processed in initStatisticsAndGraphs()
        if (subset == null || !_subsets.contains(subset))
            return subset;

        PopulationName name = null;
        SubsetExpression expr = null;
        if (subset.isExpression())
            expr = simplifySubsetExpr(subset.getExpression());
        else
            name = subset.getPopulationName();

        // The ancestor map is keyed by leaf population name only, so two unrelated gating trees that happen to
        // share a leaf name (e.g. one rooted at "Time", another rooted directly at "Singlets") can collide on
        // the same key. Guard against treating the stored ancestor as applicable when it isn't actually an
        // ancestor of this subset.
        SubsetSpec commonAncestor = _subsetNameAncestorMap.get(subset.getSubset().toString());
        if (commonAncestor == null || !subset.hasAncestor(commonAncestor))
        {
            if (expr != null)
                return new SubsetSpec(subset.getParent(), expr);
            else
                return new SubsetSpec(subset.getParent(), name);
        }

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
            assert false : "Error with subset '" + subset + "' and ancestor '" + commonAncestor + "'";
            _log.error("Error with subset '{}' and ancestor '{}'", subset, commonAncestor, e);
            return subset;
        }
    }

    private void initStatisticsAndGraphs()
    {
        if (_subsetNameAncestorMap != null)
            return;
        _statistics = AttributeCache.STATS.byContainer(_container);
        _graphs = AttributeCache.GRAPHS.byContainer(_container);
        _subsets = new HashSet<>();
        for (AttributeCache.StatisticEntry stat : _statistics)
        {
            StatisticSpec spec = stat.getAttribute();
            _subsets.add(spec.getSubset());
        }
        for (AttributeCache.GraphEntry graph : _graphs)
        {
            GraphSpec spec = graph.getAttribute();
            _subsets.add(spec.getSubset());
        }
        _subsetNameAncestorMap = getSubsetNameAncestorMap(_subsets);
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


    @TestWhen(TestWhen.When.DAILY)
    public static class TestCase
    {
        @Test
        public void testSimplifySubset()
        {
            var subset1 = SubsetSpec.fromParts(StringUtils.split("Trucount beads-/CD45+, less debris/Singlets/CD45+/CD14-/CD3+ T/CD4+ T/CD127low-,CD25+ (Treg)",'/'));
            var subset2 = SubsetSpec.fromParts(StringUtils.split("trucount beads-/CD45+, less debris/Singlets/CD45+/CD14-/CD3+ T/CD4+ T/CD127low-,CD25+ (Treg)",'/'));

            // this tests that we do not hit the assert in the catch block in simplifySubset()
            FlowPropertySet fps = new FlowPropertySet(JunitUtil.getTestContainer());
            fps._subsets = Set.of(subset1);
            fps._subsetNameAncestorMap = FlowPropertySet.getSubsetNameAncestorMap(fps._subsets);
            fps.simplifySubset(subset1);
            fps.simplifySubset(subset2);

            fps._subsets = Set.of(subset1, subset2);
            fps._subsetNameAncestorMap = FlowPropertySet.getSubsetNameAncestorMap(fps._subsets);
            fps.simplifySubset(subset1);
            fps.simplifySubset(subset2);
        }

        @Test
        public void testSimplifySubsetUnrelatedRoots()
        {
            // Two gating trees rooted differently ("Time" vs "Foo") that happen to share a leaf population
            // name ("CD8+"). getSubsetNameAncestorMap() keys purely on the leaf name, so these collide.
            var timeRooted = SubsetSpec.fromParts(StringUtils.split("Time/Singlets/CD3+/CD8+", '/'));
            var fooRooted = SubsetSpec.fromParts(StringUtils.split("Foo/Singlets/CD3+/CD8+", '/'));

            FlowPropertySet fps = new FlowPropertySet(JunitUtil.getTestContainer());
            fps._subsets = Set.of(timeRooted, fooRooted);

            // Force the ancestor map into the state that used to trigger the bug: process the unrelated
            // "fooRooted" entry so the reduction for "CD8+" collapses to null (no common ancestor) and is
            // removed from the map, then re-seed the map with a fresh, unreduced "timeRooted" entry -- which
            // is NOT actually an ancestor of fooRooted. This mirrors what a HashSet iteration order can
            // produce in production, without depending on real hash ordering.
            fps._subsetNameAncestorMap = FlowPropertySet.getSubsetNameAncestorMap(List.of(timeRooted, fooRooted, timeRooted));

            // Previously this hit the catch block in simplifySubset() (subset.toString().substring(...) on an
            // ancestor that wasn't actually an ancestor of subset) and fell back to returning subset unchanged
            // only via the assertion/logging path. It should now take the same "no applicable ancestor" path
            // as a null lookup, cleanly and without logging an error.
            Assert.assertEquals(fooRooted, fps.simplifySubset(fooRooted));
        }
    }
}
