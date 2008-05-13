/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.flow.analysis.web.StatisticSpec.STAT;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/*
    public enum StatisticSet
    {
        count,
        frequency,
        frequencyOfParent,
        frequencyOfGrandparent,
        medianGated,
        medianAll,
        meanAll,
        stdDevAll,
    }

 */

public enum StatisticSet
{
    existing("Existing", null),
    workspace("Workspace", null),
    count("Count", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Count, null)),
    frequency("Frequency", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Frequency, null)),
    frequencyOfParent("Frequency Of Parent", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Freq_Of_Parent, null)),
    frequencyOfGrandparent("FrequencyOfGrandparent", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Freq_Of_Grandparent, null)),
    medianAll("Median values of all parameters", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Median, "*")),
    meanAll("Mean values of all parameters", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Mean, "*")),
    stdDevAll("Standard deviation of all parameters", new StatisticSpec(new SubsetSpec(null, "*"), STAT.Std_Dev, "*"))
    ;

    final StatisticSpec _spec;
    final String _label;
    final static Map<StatisticSpec, StatisticSet> statSpecMap = new HashMap();
    static
    {
        for (StatisticSet set : values())
        {
            if (set.getStat() != null)
            {
                statSpecMap.put(set.getStat(), set);
            }
        }
    }
    StatisticSet(String label, StatisticSpec statistic)
    {
        _label = label;
        _spec = statistic;
    }

    public String getLabel()
    {
        return _label;
    }

    public StatisticSpec getStat()
    {
        return _spec;
    }

    static public StatisticSet fromStatisticSpec(StatisticSpec spec)
    {
        return statSpecMap.get(spec);
    }

    static public boolean isRedundant(Set<StatisticSet> statSets, StatisticSpec spec)
    {
        switch (spec.getStatistic())
        {
            case Count:
                return statSets.contains(StatisticSet.count);
            case Frequency:
                return statSets.contains(StatisticSet.frequency);
            case Freq_Of_Parent:
                return statSets.contains(StatisticSet.frequencyOfParent);
            case Freq_Of_Grandparent:
                return statSets.contains(StatisticSet.frequencyOfGrandparent);
            case Median:
                return statSets.contains(StatisticSet.medianAll);
            case Mean:
                return statSets.contains(StatisticSet.meanAll);
            case Std_Dev:
                return statSets.contains(StatisticSet.stdDevAll);
        }
        return false;
    }
}
