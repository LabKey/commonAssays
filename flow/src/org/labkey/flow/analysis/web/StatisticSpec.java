/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.analysis.web;

import org.labkey.api.view.Stats;
import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.Subset;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StatisticSpec implements Serializable, Comparable
{
    final SubsetSpec _subset;
    final STAT _statistic;
    final String _parameter;

    public enum STAT
    {
        // Well statistics
        Count("Count", "Count"),
        Frequency("Frequency", "%"),
        Freq_Of_Parent("Frequency of Parent", "%P"),
        Freq_Of_Grandparent("Frequency of Grandparent", "%G"),
        Min("Min", "Min"),
        Max("Max", "Max"),
        Median("Median", "Median"),
        Mean("Mean", "Mean"),
        Std_Dev("Standard_Deviation", "StdDev"),
        CV("Coefficient of Variation", "CV"),
        Median_Abs_Dev("Median Absolute Deviation", "MAD"),
        //Robust_CV("Robust Coefficient of Variation", "rCV"),
        Percentile("Percentile", "%ile"),
        // Run statistics
        Spill("Spill", "Spill"); // Used for compensation calculations

        private String _longName;
        private String _shortName;
        STAT(String longName, String shortName)
        {
            _longName = longName;
            _shortName = shortName;
        }
        public String getShortName()
        {
            return _shortName;
        }
        public String getLongName()
        {
            return _longName;
        }
    }

    public StatisticSpec(SubsetSpec subset, STAT statistic, String parameter)
    {
        _subset = subset;
        _statistic = statistic;
        _parameter = parameter;
    }

    public StatisticSpec(String stat)
    {
        String str = stat;
        if (str.endsWith(")"))
        {
            int ichLParen = str.lastIndexOf("(");
            if (ichLParen < 0)
            {
                throw new FlowException("Missing '('");
            }
            _parameter = str.substring(ichLParen + 1, str.length() - 1);
            str = str.substring(0, ichLParen);
        }
        else
        {
            _parameter = null;
        }

        int ichColon = str.lastIndexOf(":");
        if (ichColon < 0)
        {
            _subset = null;
        }
        else
        {
            _subset = SubsetSpec.fromString(str.substring(0, ichColon));
            str = str.substring(ichColon + 1);
        }
        try
        {
            _statistic = STAT.valueOf(str);
        }
        catch (Exception e)
        {
            throw new FlowException("'" + stat + "' is not a valid statistic.", e);
        }
    }

    public SubsetSpec getSubset()
    {
        return _subset;
    }

    public STAT getStatistic()
    {
        return _statistic;
    }

    public String getParameter()
    {
        return _parameter;
    }

    public String toString()
    {
        return toString(_statistic.toString());
    }

    private String toString(String strStat)
    {
        StringBuilder ret = new StringBuilder();
        if (_subset != null)
        {
            ret.append(_subset.toString());
            ret.append(":");
        }
        ret.append(strStat);
        if (_parameter != null)
        {
            ret.append("(" + _parameter + ")");
        }
        return ret.toString();
    }

    public String toShortString()
    {
        return toString(_statistic.getShortName());
    }



    static double getFrequency(Subset parent, Subset child)
    {
        double total = parent.getDataFrame().getRowCount();
        double count = child.getDataFrame().getRowCount();
        if (count == 0)
        {
            return 0;
        }
        return count * 100 / total;
    }

    static public double calculate(Subset subset, StatisticSpec stat, Map<String, Stats.DoubleStats> stats)
    {
        String param;
        double percentile = 0;
        switch(stat.getStatistic())
        {
            case Count:
                return subset.getDataFrame().getRowCount();
            case Frequency:
            {
                Subset root = subset;
                while (root.getParent() != null)
                {
                    root = root.getParent();
                }
                return getFrequency(root, subset);
            }
            case Freq_Of_Parent:
                return getFrequency(subset.getParent(), subset);
            case Freq_Of_Grandparent:
                return getFrequency(subset.getParent().getParent(), subset);
            case Percentile:
                param = stat.getParameter();
                int ichColon = param.indexOf(":");
                percentile = new Double(param.substring(ichColon + 1));
                param = param.substring(0, ichColon);
                break;
            default:
                param = stat.getParameter();
                break;
        }
        Stats.DoubleStats doubleStats = stats.get(param);
        if (doubleStats == null)
        {
            doubleStats = new Stats.DoubleStats(subset.getDataFrame().getDoubleArray(param));
            stats.put(param, doubleStats);
        }
        switch (stat.getStatistic())
        {
            case Min:
                return doubleStats.getMin();
            case Max:
                return doubleStats.getMax();
            case Mean:
                return doubleStats.getMean();
            case Median:
                return doubleStats.getMedian();
            case Std_Dev:
                return doubleStats.getStdDev();
            case CV:
                double median = doubleStats.getMedian();
                if (median == 0)
                    return 0;
                return 100.0 * doubleStats.getStdDev() / median;
            case Median_Abs_Dev:
                return doubleStats.getMedianAbsoluteDeviation();
//            case Robust_CV:
//                // BD's definition
//                return 100.0 * doubleStats.getMedianAbsoluteDeviation() / doubleStats.getMedian();

//                // interquartile range, 50% from the middle
//                double q1 = doubleStats.getFirstQuartile();
//                double q3 = doubleStats.getThirdQuartile();
//                return 100.0 * 0.7515 * (q3 - q1) / doubleStats.getMedian();

//                // 63% from the middle, roughly 1 stddev
//                double q1 = doubleStats.getPercentile(.315);
//                double q3 = doubleStats.getPercentile(.815);
//                return 100.0 * 0.8413 * (q3 - q1) / doubleStats.getMedian();
            case Percentile:
                return doubleStats.getPercentile(percentile / 100);
            default:
                throw new IllegalArgumentException("Unknown statistic " + stat);
        }
    }

    static public double calculate(Subset subset, StatisticSpec stat)
    {
        return calculate(subset, stat, new HashMap());
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StatisticSpec))
            return false;
        return toString().equals(other.toString());
    }
    public int hashCode()
    {
        return toString().hashCode();
    }

    public int compareTo(Object o)
    {
        if (!(o instanceof StatisticSpec))
            return 0;
        StatisticSpec spec = (StatisticSpec) o;
        int ret = SubsetSpec.compare(getSubset(), spec.getSubset());
        if (ret != 0)
            return ret;
        return this.toString().compareTo(spec.toString());
    }

    public static void main(String[] args)
    {
        String fcsFile = args[0];
        FCS fcs = null;
        try
        {
            fcs = new FCS(new java.io.File(fcsFile));
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace(System.err);
        }
        if (fcs == null)
            return;


        System.out.println("Name\tMin\t1st Qu.\tMean\tMedian\t3rd Qu.\tMax\tCount\tStd_Dev\tMedian_Abs_Dev\tCV");

        DataFrame frame = fcs.getScaledData(null);
        for (int p = 0; p < frame.getColCount(); p++)
        {
            DataFrame.Field field = frame.getField(p);
            System.out.print(field.getName());

            java.util.Map<String, Stats.DoubleStats> map = new java.util.HashMap<String, Stats.DoubleStats>();
            Subset subset = new Subset(null, null, fcs, frame);
            StatisticSpec[] stats = new StatisticSpec[] {
                new StatisticSpec(null, STAT.Min, field.getName()),
                new StatisticSpec(null, STAT.Percentile, field.getName() + ":25"),
                new StatisticSpec(null, STAT.Mean, field.getName()),
                new StatisticSpec(null, STAT.Median, field.getName()),
                new StatisticSpec(null, STAT.Percentile, field.getName() + ":75"),
                new StatisticSpec(null, STAT.Max, field.getName()),
                new StatisticSpec(null, STAT.Count, field.getName()),
                new StatisticSpec(null, STAT.Std_Dev, field.getName()),
                new StatisticSpec(null, STAT.Median_Abs_Dev, field.getName()),
                new StatisticSpec(null, STAT.CV, field.getName()),
            };

            for (StatisticSpec stat : stats)
            {
                double d = calculate(subset, stat, map);
                System.out.printf("\t%.2f", d);
            }

            System.out.println();
        }

    }
}
