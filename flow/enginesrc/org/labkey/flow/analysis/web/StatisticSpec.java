/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.Subset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticSpec implements Serializable, Comparable<StatisticSpec>
{
    final SubsetSpec _subset;
    final STAT _statistic;
    final String _parameter;

    private static final Pattern _statPattern;

    public enum STAT
    {
        // Well statistics
        Count("Count", "Count", false),
        Frequency("Frequency", "%", false),
        Freq_Of_Parent("Frequency of Parent", "%P", false),
        Freq_Of_Grandparent("Frequency of Grandparent", "%G", false),
        Freq_Of_Ancestor("Frequency of Ancestor", "%of", true),
        Min("Min", "Min", true),
        Max("Max", "Max", true),
        Median("Median", "Median", true),
        Mean("Mean", "Mean", true),
        //Mode("Mode", "Mode", true),
        Geometric_Mean("Geometric Mean", "GeomMean", true),
        Std_Dev("Standard_Deviation", "StdDev", true),
        CV("Coefficient of Variation", "CV", true),
        Median_Abs_Dev("Median Absolute Deviation", "MAD", true),
        Median_Abs_Dev_Percent("Median Absolute Deviation (%)", "MAD%", true),
        Robust_CV("Robust Coefficient of Variation", "rCV", true),
        Percentile("Percentile", "%ile", true),
        // Run statistics
        Spill("Spill", "Spill", true); // Used for compensation calculations

        private final String _longName;
        private final String _shortName;
        private final boolean _parameterRequired;

        STAT(String longName, String shortName, boolean parameterRequired)
        {
            _longName = longName;
            _shortName = shortName;
            _parameterRequired = parameterRequired;
        }

        public String getShortName()
        {
            return _shortName;
        }

        public String getLongName()
        {
            return _longName;
        }

        public boolean isParameterRequired()
        {
            return _parameterRequired;
        }

        /**
         * Get a STAT based upon either the STAT enum name or the short name.
         */
        public static STAT fromString(String str)
        {
            try
            {
                return STAT.valueOf(str);
            }
            catch (IllegalArgumentException e)
            {
                // ok
            }

            for (STAT stat : STAT.values())
            {
                if (stat.getShortName().equals(str))
                    return stat;
            }

            throw new IllegalArgumentException(str);
        }
    }

    static
    {
        List<String> stats = new ArrayList<>();
        for (STAT stat : STAT.values())
            stats.add(stat.name());

        // Create regular expression to find the STAT in a statistic spec
        //   start-of-line-or-colon ( stat-names ) open-paren-or-end-of-line
        StringBuilder sb = new StringBuilder();
        sb.append("(^|:)");
        sb.append("(").append(StringUtils.join(stats, "|")).append(")");
        sb.append("(\\(|$)");

        _statPattern = Pattern.compile(sb.toString());
    }

    public StatisticSpec(SubsetSpec subset, STAT statistic, String parameter)
    {
        _subset = subset;
        _statistic = statistic;
        _parameter = parameter;
    }

    // UNDONE: need parser
    public StatisticSpec(String stat)
    {
        Matcher m = _statPattern.matcher(stat);
        if (!m.find())
            throw new FlowException("Invalid statistic spec: " + stat);

        String subset = stat.substring(0, m.start());
        _subset = SubsetSpec.fromEscapedString(subset);

        String str = stat.substring(m.start());
        int ichLParen = str.indexOf("(");
        if (ichLParen >= 0)
        {
            if (!str.endsWith(")"))
                throw new FlowException("Expected matching parens in '" + str + "' from statistic spec: " + stat);

            _parameter = str.substring(ichLParen+1, str.length()-1);
            str = str.substring(0, ichLParen);
        }
        else
        {
            _parameter = null;
        }

        if (str.startsWith(":"))
            str = str.substring(1);

        try
        {
            _statistic = STAT.fromString(str);
        }
        catch (Exception e)
        {
            throw new FlowException("Invalid statistic '" + str + "' from statistic spec: " + stat, e);
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

	private transient String _toString = null;

    // print in escaped form
    public String toString()
    {
		if (null == _toString)
			_toString = toString(_statistic.toString(), true);
		return _toString;
    }

    private String toString(String strStat, boolean escaped)
    {
        StringBuilder ret = new StringBuilder();
        if (_subset != null)
        {
            ret.append(_subset.toString(escaped, false));
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
        return toString(_statistic.getShortName(), false);
    }

    public String toShortString(boolean escaped)
    {
        return toString(_statistic.getShortName(), escaped);
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

    static public double calculate(Subset subset, StatisticSpec stat, Map<String, MathStat> stats)
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
            case Freq_Of_Ancestor:
            {
                // parameter is the name of the ancestor population
                String name = stat.getParameter();
                if (name == null)
                    throw new IllegalArgumentException("ancestor name required");
                Subset ancestor = subset;
                while (ancestor.getParent() != null || !name.equals(ancestor.getName()))
                {
                    ancestor = ancestor.getParent();
                }
                return getFrequency(ancestor, subset);
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
        MathStat doubleStats = stats.get(param);
        if (doubleStats == null)
        {
            StatsService service = ServiceRegistry.get().getService(StatsService.class);
            doubleStats = service.getStats(subset.getDataFrame().getDoubleArray(param));
            stats.put(param, doubleStats);
        }
        switch (stat.getStatistic())
        {
            case Min:
                return doubleStats.getMinimum();
            case Max:
                return doubleStats.getMaximum();
            case Mean:
                return doubleStats.getMean();
            case Median:
                return doubleStats.getMedian();
            //case Mode:
            //    //return doubleStats.getMode();
            case Geometric_Mean:
                return doubleStats.getGeometricMean();
            case Std_Dev:
                return doubleStats.getStdDev();
            case CV:
                double mean = doubleStats.getMean();
                if (mean == 0)
                    return 0;
                return 100.0 * doubleStats.getStdDev() / mean;
            case Median_Abs_Dev:
                return doubleStats.getMedianAbsoluteDeviation();
            case Median_Abs_Dev_Percent:
                double median = doubleStats.getMedian();
                if (median == 0)
                    return 0;
                return 100.0 * doubleStats.getMedianAbsoluteDeviation() / median;
            case Robust_CV:
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

                // FlowJo's definition: 100 * 1/2 ( 84.13%ile - 15.87%ile ) / Median
                double q1 = doubleStats.getPercentile(.1587);
                double q3 = doubleStats.getPercentile(.8413);
                return 100.0 * 0.5 * (q3 - q1) / doubleStats.getMedian();
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

    @Override
    public int compareTo(@NotNull StatisticSpec spec)
    {
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


        System.out.println("Name\tMin\t1st Qu.\tMean\tMedian\t3rd Qu.\tMax\tCount\tStd_Dev\tMedian_Abs_Dev\tGeometric_Mean\tCV\tRobust_CV");

        DataFrame frame = fcs.getScaledData(null);
        for (int p = 0; p < frame.getColCount(); p++)
        {
            DataFrame.Field field = frame.getField(p);
            System.out.print(field.getName());

            java.util.Map<String, MathStat> map = new java.util.HashMap<>();
            Subset subset = new Subset(null, null, fcs, frame);
            StatisticSpec[] stats = new StatisticSpec[] {
                new StatisticSpec(null, STAT.Min, field.getName()),
                new StatisticSpec(null, STAT.Percentile, field.getName() + ":25"),
                new StatisticSpec(null, STAT.Mean, field.getName()),
                new StatisticSpec(null, STAT.Median, field.getName()),
                //new StatisticSpec(null, STAT.Mode, field.getName()),
                new StatisticSpec(null, STAT.Percentile, field.getName() + ":75"),
                new StatisticSpec(null, STAT.Max, field.getName()),
                new StatisticSpec(null, STAT.Count, field.getName()),
                new StatisticSpec(null, STAT.Std_Dev, field.getName()),
                new StatisticSpec(null, STAT.Median_Abs_Dev, field.getName()),
                new StatisticSpec(null, STAT.Geometric_Mean, field.getName()),
                new StatisticSpec(null, STAT.CV, field.getName()),
                new StatisticSpec(null, STAT.Robust_CV, field.getName()),
            };

            for (StatisticSpec stat : stats)
            {
                double d = calculate(subset, stat, map);
                System.out.printf("\t%.2f", d);
            }

            System.out.println();
        }

    }



    public static class TestCase extends Assert
    {
        @Test
        public void testParseSave11_1() throws Exception
        {
            for (String s : statitics11_1)
            {
                try
                {
                    StatisticSpec spec = new StatisticSpec(s);
                }
                catch (Exception x)
                {
                    System.err.println(s);
                    throw x;
                }
            }
        }
    }

final static String[] statitics11_1 = {
    "-:Count",
    "-:Freq_Of_Parent",
    "Beads/-:Count",
    "Beads/-:Freq_Of_Parent",
    "Beads/MIP-1B PerCP Cy55+:Count",
    "Beads/MIP-1B PerCP Cy55+:Freq_Of_Parent",
    "Beads:Count",
    "Beads:Freq_Of_Parent",
    "Count",
    "FSC-A; SSC-A subset:Count",
    "FSC-A; SSC-A subset:Freq_Of_Parent",
    "HLA-DR Pac Blue:Count",
    "HLA-DR Pac Blue:Freq_Of_Parent",
    "L/-:Count",
    "L/-:Freq_Of_Parent",
    "L/APC CD3+:Count",
    "L/APC CD3+:Freq_Of_Parent",
    "S/-:Count",
    "S/-:Freq_Of_Parent",
    "S/HLA-DR+ Pac Blue:Count",
    "S/HLA-DR+ Pac Blue:Freq_Of_Parent",
    "S/L/+:Count",
    "S/L/+:Freq_Of_Parent",
    "S/L/-:Count",
    "S/L/-:Freq_Of_Parent",
    "S/L/3+/4+/4+ Live:Count",
    "S/L/3+:Count",
    "S/L/3+:Freq_Of_Parent",
    "S/L/3-/3- Live:Count",
    "S/L/3-/3- Live:Freq_Of_Parent",
    "S/L/3-:Count",
    "S/L/3-:Freq_Of_Parent",
    "S/L/<Alexa 680-A>; <APC-A> subset:Count",
    "S/L/<Alexa 680-A>; <APC-A> subset:Freq_Of_Parent",
    "S/L/<PerCP Cy55 Blue-A>; <PE Cy5-A> subset:Count",
    "S/L/<PerCP Cy55 Blue-A>; <PE Cy5-A> subset:Freq_Of_Parent",
    "S/L/APC-Cy7 HLA-DR+:Freq_Of_Parent",
    "S/L/APC-H7 CD4+:Count",
    "S/L/APC-H7 CD4+:Freq_Of_Parent",
    "S/L/AViD (new)+:Count",
    "S/L/AViD (new)+:Freq_Of_Parent",
    "S/L/AViD+:Count",
    "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&!CD57+&D103+):Count",
    "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&!CD57+&D103+):Freq_Of_Parent",
    "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&CD57+&!D103+):Count",
    "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&CD57+&!D103+):Freq_Of_Parent",
    "S/Lv/L/CD3+/CD4+/CD38+:Freq_Of_Parent",
    "S/Lv/L/CD3+/CD4+/CD57+:Count",
    "S/Lv/L/CD3+/CD4+/CD57+:Freq_Of_Parent",
    "S/Lv/L/CD3+/CD8+/(!45RO+&CCR5+&!CCR7+&CD27+&CD28+&!CD57+&D103+):Count",
    "S/Lv/L/CD3+/CD8+/(!45RO+&CCR5+&!CCR7+&CD27+&CD28+&!CD57+&D103+):Freq_Of_Parent",
    "S/Lv/L:Freq_Of_Parent",
    "S/Lv/L:Mean(Time)",
    "S/Lv/L:Median(<APC-A>)",
    "S/Lv/L:Median(Pacific Blue-A)",
    "S/Lv/L:Median(SSC-A)",
    "S/Lv/L:Median(Time)",
    "S/Lv:Count",
    "S/Lv:Freq_Of_Parent",
    "S/Lv:Mean(Time)",
    "S/Lv:Median(<Pacific Blue-A>)",
    "S/Lv:Median(APC-A)",
    "S/S/Lv/L/3+/4+/(!IFNg+&IL2+&Perforin+&!TNFa+):Count",
    "S/S/Lv/L/3+/4+/(!IFNg+&IL2+&Perforin+&!TNFa+):Freq_Of_Parent",
    "S/S/Lv/L/3+/8+/(!IFNg+&!IL2+&TNFa+&!Granzyme B+&57+):Freq_Of_Parent",
    "S/S/Lv/L/3+/8+/(!IFNg+&!IL2+&TNFa+&Granzyme B+&!57+):Count",
    "S:Median(Pacific Blue-A)",
    "S:Median(SSC-A)",
    "S:Median(Time)",
    "SSC-A; FSC-A subset:Count",
    "SSC-A; FSC-A subset:Freq_Of_Parent",
    "Singlets/L/Live/3+/4+/(!IFNg+&!IL2+&!IL4+&!TNF+):Count",
    "Singlets/L/Live/3+/4+/(!IFNg+&!IL2+&!IL4+&!TNF+):Freq_Of_Parent",
    "Singlets1:Median(Pacific Blue-A)",
    "Singlets1:Std_Dev(SSC-A)",
    "Singlets1:Std_Dev(Time)",
    "Singlets:Count",
    "Singlets:Freq_Of_Parent",
    "Singlets:Median(SSC-A)",
    "Singlets:Median(Time)",
    "Spill(APC Cy7-A:APC Cy7-A)",
    "Spill(APC-Cy7-A:Indo-1 (Blue)-A)",
    "Spill(Foo (Violet)-H:Indo-1 (Blue)-A)",
    "S/L:Count(Indo-1 (Blue)-A)",
    "Std_Dev(APC Cy7-A)",
    "Std_Dev(APC-A)",
    "Std_Dev(Time)",
    "comp:Count",
    "comp:Median(PE-A)",
    "comp:Median(Pacific Blue-A)",
    "comp:Median(PerCP Cy55 Blue-A)",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q10: CD159a (NKG2a)+; HLA Dr+:Count",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q11: CD159a (NKG2a)+; HLA Dr-:Count",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q12: CD159a (NKG2a)-; HLA Dr-:Count",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q5: CD16-; CD335 (NKp46)+:Count",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q6: CD16+; CD335 (NKp46)+:Count",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q7: CD16+; CD335 (NKp46)-:Freq_Of_Parent",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q8: CD16-; CD335 (NKp46)-:Freq_Of_Parent",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q9: CD159a (NKG2a)-; HLA Dr+:Freq_Of_Parent",
    "CD45+/LYMPHS/CD3-20-/14-DR-/CD8+159a+/Q9: CD159a; HLA Dr+:Freq_Of_Parent"
    };
}
