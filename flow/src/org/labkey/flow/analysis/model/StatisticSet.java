package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.StatisticSpec.STAT;
import org.labkey.flow.analysis.web.SubsetSpec;

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
}
