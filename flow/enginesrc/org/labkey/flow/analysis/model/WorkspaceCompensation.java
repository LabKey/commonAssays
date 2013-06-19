/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkspaceCompensation implements Serializable
{
    private final Workspace _workspace;

    public WorkspaceCompensation(Workspace workspace)
    {
        this._workspace = workspace;
    }

    private boolean isUniversalNegative(Map<String, Workspace.CompensationChannelData> channelDataMap)
    {
        String keyword = null;
        String value = null;
        for (Map.Entry<String, Workspace.CompensationChannelData> entry : channelDataMap.entrySet())
        {
            if (keyword == null)
            {
                keyword = entry.getValue().negativeKeywordName;
            }
            else if (!keyword.equals(entry.getValue().negativeKeywordName))
            {
                return false;
            }
            if (value == null)
            {
                value = entry.getValue().negativeKeywordValue;
            }
            else if (!value.equals(entry.getValue().negativeKeywordValue))
            {
                return false;
            }
        }
        return true;
    }

    private boolean gatesEqual(Population pop1, Population pop2)
    {
        return pop1.getGates().equals(pop2.getGates());
    }

    private boolean isUniversal(SubsetSpec subset, List<Map<SubsetSpec, Population>> lstMap)
    {
        Population popCompare = null;
        for (Map<SubsetSpec, Population> aLstMap : lstMap)
        {
            Population pop = aLstMap.get(subset);
            if (pop == null)
                continue;
            if (popCompare == null)
            {
                popCompare = pop;
            }
            else
            {
                if (!gatesEqual(popCompare, pop))
                    return false;
            }
        }
        return true;
    }

    private void mapSubsetNames(Map<SubsetSpec, SubsetSpec> map, SubsetSpec oldParent, SubsetSpec newParent, Population pop)
    {
        SubsetSpec oldSubset = new SubsetSpec(oldParent, pop.getName());
        SubsetSpec newSubset = new SubsetSpec(newParent, pop.getName());
        map.put(oldSubset, newSubset);
        for (Population child : pop.getPopulations())
        {
            mapSubsetNames(map, oldSubset, newSubset, child);
        }
    }

    private SubsetSpec makeSubsetKeyAndAddAnalysis(CompensationCalculation calc, String name, Analysis analysis, String subset, List<String> errors)
    {
        if (subset == null || analysis == null)
            return null;
        assert !SubsetSpec.___isExpression(name);
        assert !SubsetSpec.___isExpression(subset);
        PopulationName rootName = PopulationName.fromString(name);
        SubsetSpec subsetName = SubsetSpec.fromEscapedString(subset);
        SubsetSpec ret = new SubsetSpec(null, rootName).createChild(subsetName);

        Population pop = calc.getPopulation(rootName);
        if (pop == null)
        {
            pop = new Population();
            pop.setName(rootName);
            for (Population child : analysis.getPopulations())
            {
                pop.addPopulation(child);
            }
            calc.addPopulation(pop);
        }

        if (!"Ungated".equals(subset) && pop.getPopulation(subsetName.getRoot().getPopulationName()) == null)
        {
            String analysisName = analysis.getName() == null ? "" : " '" + analysis.getName() + "'";
            errors.add("Channel '" + name + "' subset '" + subset + "' not found in analysis" + analysisName);
        }

        return ret;
    }

    private CompensationCalculation.ChannelSubset makeChannelSubset(
            CompensationCalculation calc, String name, Analysis analysis, String keyword, String value, String subset, List<String> errors)
    {
        if (analysis == null)
        {
            analysis = _workspace.findAnalysisWithKeywordValue(keyword, value, errors);
        }

        SubsetSpec subsetSpec = makeSubsetKeyAndAddAnalysis(calc, name, analysis, subset, errors);
        SampleCriteria criteria = new SampleCriteria();
        criteria.setKeyword(keyword);
        criteria.setPattern(value);
        return new CompensationCalculation.ChannelSubset(criteria, subsetSpec);
    }

    private void addPopulationMap(Map<SubsetSpec, Population> map, SubsetSpec parent, Population pop)
    {
        SubsetSpec subset = new SubsetSpec(parent, pop.getName());
        map.put(subset, pop);
        for (Population child : pop.getPopulations())
        {
            addPopulationMap(map, subset, child);
        }
    }

    /**
     * Initially, each channel has a unique gating tree with a root population with a name like "FITC+", or something.
     * This walks through one of these trees, and figures out if the gates within them (e.g. "FITC+/L") is the same
     * for each other tree.
     * If it is, then the "FITC+/L" gate is changed to "L".
     * If it is not, then the "FITC+/L" gate is changed to "FITC+L"
     */
    private void simplifySubsetNames(Map<SubsetSpec, SubsetSpec> subsetMap, List<Map<SubsetSpec, Population>> lstPopulationMap, SubsetSpec oldParent, Population population)
    {
        SubsetSpec newParent = subsetMap.get(oldParent);
        SubsetSpec oldSubset = new SubsetSpec(oldParent, population.getName());
        SubsetSpec subsetTry = new SubsetSpec(newParent, population.getName());
        SubsetSpec newSubset;
        if (!isUniversal(subsetTry, lstPopulationMap))
        {
            SubsetSpec root = oldParent.getRoot();
            assert !root.isExpression();
            assert root.getParent() == null;
            assert root.getPopulationName() != null;
            newSubset = new SubsetSpec(newParent, root.getPopulationName().compose(population.getName()));
            subsetMap.put(oldSubset, newSubset);
            for (Population child : population.getPopulations())
            {
                mapSubsetNames(subsetMap, oldSubset, newSubset, child);
            }
            return;
        }
        newSubset = subsetTry;
        subsetMap.put(oldSubset, newSubset);
        for (Population child : population.getPopulations())
        {
            simplifySubsetNames(subsetMap, lstPopulationMap, oldSubset, child);
        }
    }

    private CompensationCalculation simplify(CompensationCalculation calc)
    {
        Map<SubsetSpec, SubsetSpec> subsetMap = new LinkedHashMap<>();
        List<Map<SubsetSpec, Population>> lstPopulationMap = new ArrayList<>();
        for (Population pop : calc.getPopulations())
        {
            Map<SubsetSpec, Population> map = new HashMap<>();
            for (Population child : pop.getPopulations())
            {
                addPopulationMap(map, null, child);
            }
            lstPopulationMap.add(map);
        }
        for (Population pop : calc.getPopulations())
        {
            for (Population child : pop.getPopulations())
            {
                simplifySubsetNames(subsetMap, lstPopulationMap, new SubsetSpec(null, pop.getName()), child);
            }
        }
        CompensationCalculation ret = new CompensationCalculation();
        ret.setSettings(calc.getSettings());
        for (Map.Entry<SubsetSpec, SubsetSpec> entry : subsetMap.entrySet())
        {
            SubsetSpec oldSubset = entry.getKey();
            SubsetSpec newSubset = entry.getValue();
            if (_workspace.findPopulation(ret, newSubset) != null)
                continue;
            Population oldPop = _workspace.findPopulation(calc, oldSubset);

            SubsetSpec newParentSubset = newSubset.getParent();
            PopulationSet newParent;
            if (newParentSubset == null)
            {
                newParent = ret;
            }
            else
            {
                newParent = _workspace.findPopulation(ret, newParentSubset);
            }
            Population newPop = new Population();
            assert !newSubset.isExpression();
            assert newSubset.getPopulationName() != null;
            PopulationName name = newSubset.getPopulationName();
            newPop.setName(name);
            newPop.getGates().addAll(oldPop.getGates());
            assert newParent.getPopulation(newPop.getName()) == null;
            newParent.addPopulation(newPop);
        }
        for (CompensationCalculation.ChannelInfo oldChannel : calc.getChannels())
        {
            CompensationCalculation.ChannelSubset oldPositive = oldChannel.getPositive();
            CompensationCalculation.ChannelSubset oldNegative = oldChannel.getNegative();
            SubsetSpec newPositiveSubset = subsetMap.get(oldPositive.getSubset());
            SubsetSpec newNegativeSubset = subsetMap.get(oldNegative.getSubset());
            ret.addChannel(oldChannel.getName(),
                    new CompensationCalculation.ChannelSubset(oldPositive.getCriteria(), newPositiveSubset),
                    new CompensationCalculation.ChannelSubset(oldNegative.getCriteria(), newNegativeSubset));
        }
        return ret;
    }

    public CompensationCalculation makeCompensationCalculation(Map<String, Workspace.CompensationChannelData> channelDataMap, PopulationName groupName, List<String> errors)
    {
        CompensationCalculation ret = new CompensationCalculation();
        ret.setSettings(_workspace.getSettings());
        boolean isUniversalNegative = isUniversalNegative(channelDataMap);

        Analysis analysis = null;
        if (groupName != null)
        {
            analysis = _workspace.getGroupAnalyses().get(groupName);
            if (analysis == null)
            {
                errors.add("Group '" + groupName + "' not found in workspace");
                return ret;
            }
        }

        for (Map.Entry<String, Workspace.CompensationChannelData> entry : channelDataMap.entrySet())
        {
            String parameter = entry.getKey();
            Workspace.CompensationChannelData data = entry.getValue();
            if (data.positiveKeywordName == null || data.positiveKeywordValue == null ||
                    data.negativeKeywordName == null || data.negativeKeywordValue == null)
            {
                errors.add("Missing data for parameter '" + parameter + "'");
                continue;
            }
            String positiveName = parameter + "+";
            String negativeName = isUniversalNegative ? "-" : parameter + "-";
            CompensationCalculation.ChannelSubset positiveSubset = makeChannelSubset(ret, positiveName, analysis,
                    data.positiveKeywordName, data.positiveKeywordValue, data.positiveSubset, errors);
            CompensationCalculation.ChannelSubset negativeSubset = makeChannelSubset(ret, negativeName, analysis,
                    data.negativeKeywordName, data.negativeKeywordValue, data.negativeSubset, errors);
            ret.addChannel(parameter, positiveSubset, negativeSubset);
        }
        ret = simplify(ret);
        return ret;
    }
}
