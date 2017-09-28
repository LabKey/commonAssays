/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.VersionNumber;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


abstract public class FlowJoWorkspace extends Workspace
{
    static Map<String, StatisticSpec.STAT> STATS = new HashMap<>();
    static
    {
        STATS.put("Count", StatisticSpec.STAT.Count);

        STATS.put("%ile", StatisticSpec.STAT.Percentile);
        STATS.put("Percentile", StatisticSpec.STAT.Percentile);

        STATS.put("Mean", StatisticSpec.STAT.Mean);
        STATS.put("Median", StatisticSpec.STAT.Median);
        //statMap.put("Mode", StatisticSpec.STAT.Mode);

        STATS.put("Geom. Mean", StatisticSpec.STAT.Geometric_Mean);
        STATS.put("GeometricMean", StatisticSpec.STAT.Geometric_Mean);
        STATS.put("Geometric Mean", StatisticSpec.STAT.Geometric_Mean);

        STATS.put("CV", StatisticSpec.STAT.CV);

        STATS.put("SD", StatisticSpec.STAT.Std_Dev);
        STATS.put("StdDev", StatisticSpec.STAT.Std_Dev);

        STATS.put("MedianAbsDeviation", StatisticSpec.STAT.Median_Abs_Dev);
        STATS.put("Median Abs Dev", StatisticSpec.STAT.Median_Abs_Dev);

        STATS.put("MedianAbsDeviation%", StatisticSpec.STAT.Median_Abs_Dev_Percent);

        //STATS.put("RobustSD", StatisticSpec.STAT.Robust_SD);
        //STATS.put("Robust SD", StatisticSpec.STAT.Robust_SD);

        STATS.put("RobustCV", StatisticSpec.STAT.Robust_CV);
        STATS.put("Robust CV", StatisticSpec.STAT.Robust_CV);

        STATS.put("FrequencyOfGrandParent", StatisticSpec.STAT.Freq_Of_Grandparent);
        STATS.put("FreqGrandparent", StatisticSpec.STAT.Freq_Of_Grandparent);
        STATS.put("fj.stat.freqofgrandparent", StatisticSpec.STAT.Freq_Of_Grandparent);

        STATS.put("FrequencyOfParent", StatisticSpec.STAT.Freq_Of_Parent);
        STATS.put("FreqParent", StatisticSpec.STAT.Freq_Of_Parent);
        STATS.put("Freq. of Parent", StatisticSpec.STAT.Freq_Of_Parent);
        STATS.put("fj.stat.freqofparent", StatisticSpec.STAT.Freq_Of_Parent);

        STATS.put("FrequencyOfTotal", StatisticSpec.STAT.Frequency);
        STATS.put("Freq. of Total", StatisticSpec.STAT.Frequency);
        STATS.put("fj.stat.freqoftotal", StatisticSpec.STAT.Frequency);

        STATS.put("FrequencyOfAncestor", StatisticSpec.STAT.Freq_Of_Ancestor);
        STATS.put("FreqOf", StatisticSpec.STAT.Freq_Of_Ancestor);
        // FlowJo > 7.5 through < 10.0.5 uses the "Freq. of " prefix plus the ancestor population name
        //STATS.put("Freq. of <population name>", StatisticSpec.STAT.Freq_Of_Ancestor);
        STATS.put("fj.stat.freqof", StatisticSpec.STAT.Freq_Of_Ancestor);
    }

    private VersionNumber _version;
    private VersionNumber _flowJoVersion;

    protected FlowJoWorkspace()
    {
    }

    protected FlowJoWorkspace(String name, String path, Element elDoc)
    {
        _name = name;
        _path = path;
        readAll(elDoc);
    }

    protected void readAll(Element elDoc)
    {
        readVersion(elDoc);
        readCompensationMatrices(elDoc);
        readSamples(elDoc);
        readGroups(elDoc);
        postProcess();
    }

    protected void readVersion(Element elDoc)
    {
        if (elDoc.hasAttribute("version"))
            _version = new VersionNumber(elDoc.getAttribute("version"));

        if (elDoc.hasAttribute("flowJoVersion"))
        {
            String flowJoVersion = elDoc.getAttribute("flowJoVersion");
            if (flowJoVersion.startsWith("Version "))
                flowJoVersion = flowJoVersion.substring("Version ".length());
            _flowJoVersion = new VersionNumber(flowJoVersion);
        }
    }

    public VersionNumber getVersion() { return _version; }

    public VersionNumber getFlowJoVersion() { return _flowJoVersion; }

    abstract protected void readCompensationMatrices(Element elDoc);

    abstract protected void readSamples(Element elDoc);

    abstract protected void readGroups(Element elDoc);

    protected void postProcess()
    {
        createAliases();
    }

    private void createAliases()
    {
        Map<SubsetSpec, SubsetSpec> aliases = new HashMap<>();

        for (SampleInfo sampleInfo : getSamplesComplete())
        {
            Analysis analysis = getSampleAnalysis(sampleInfo);
            if (analysis == null)
                continue;

            AttributeSet attrs = getSampleAnalysisResults(sampleInfo);
            if (attrs == null)
                continue;

            for (StatisticSpec stat : attrs.getStatistics().keySet())
            {
                SubsetSpec alias;
                if (aliases.containsKey(stat.getSubset()))
                    alias = aliases.get(stat.getSubset());
                else
                {
                    alias = FCSAnalyzer.get().getSubsetAlias(analysis, stat.getSubset());
                    aliases.put(stat.getSubset(), alias);
                }

                if (alias != null)
                    attrs.addStatisticAlias(stat, new StatisticSpec(alias, stat.getStatistic(), stat.getParameter()));
            }

            for (GraphSpec graph : attrs.getGraphs().keySet())
            {
                SubsetSpec alias;
                if (aliases.containsKey(graph.getSubset()))
                    alias = aliases.get(graph.getSubset());
                else
                {
                    alias = FCSAnalyzer.get().getSubsetAlias(analysis, graph.getSubset());
                    aliases.put(graph.getSubset(), alias);
                }

                if (alias != null)
                    attrs.addGraphAlias(graph, new GraphSpec(alias, graph.getParameters()));
            }
        }
    }

    protected void addSampleAnalysisResults(AttributeSet results, String sampleId)
    {
        final Map<StatisticSpec, Double> statistics = results.getStatistics();
        if (statistics.size() > 0)
        {
            // Issue 19117: FlowJo 10.0.6 saves total count as -1
            // If the statistic "count" is unavailable, try to get it from the '$TOT" keyword.
            StatisticSpec totalSpec = new StatisticSpec(null, StatisticSpec.STAT.Count, null);
            Double total = statistics.get(totalSpec);
            if (total == null || total == 0.0d || total == -1.0d)
            {
                throw new IllegalStateException("Reading the total event count should be covered by readStats method now");
            }

            // Fill in the Freq_Of_Parent and Frequency stats that can be determined from the existing stats
            for (Map.Entry<StatisticSpec, Double> entry : new HashMap<>(statistics).entrySet())
            {
                final StatisticSpec spec = entry.getKey();
                if (spec.getStatistic() != StatisticSpec.STAT.Count)
                    continue;
                if (spec.getSubset() == null)
                    continue;

                final Double count = entry.getValue();

                // Fill in Frequency stat if it is present and is 0.0
                StatisticSpec freqOfTotalSpec = new StatisticSpec(spec.getSubset(), StatisticSpec.STAT.Frequency, null);
                if (statistics.containsKey(freqOfTotalSpec))
                {
                    Double freqOfTotal = statistics.get(freqOfTotalSpec);
                    if (freqOfTotal.equals(0.0) && total != null)
                    {
                        if (count.equals(0.0))
                            freqOfTotal = 0.0;
                        else
                            freqOfTotal = count / total * 100;
                        results.setStatistic(freqOfTotalSpec, freqOfTotal);
                    }
                }

                // Fill in Freq_Of_Parent if it doesn't already exist
                StatisticSpec freqOfParentSpec = new StatisticSpec(spec.getSubset(), StatisticSpec.STAT.Freq_Of_Parent, null);
                if (statistics.containsKey(freqOfParentSpec))
                    continue;

                Double denominator = statistics.get(new StatisticSpec(spec.getSubset().getParent(), StatisticSpec.STAT.Count, null));
                if (denominator == null)
                    continue;

                if (count.equals(0.0))
                    results.setStatistic(freqOfParentSpec, 0.0);
                else if (!denominator.equals(0.0))
                    results.setStatistic(freqOfParentSpec, count / denominator * 100);

            }

            if (results.getStatistics().size() == 1)
            {
                StatisticSpec spec = statistics.keySet().iterator().next();
                warning(sampleId, null, null, "Analysis contains only " + spec + " statistic and no others");
            }

            _sampleAnalysisResults.put(sampleId, results);
        }
        else
        {
            warning(sampleId, null, null, "No statistics found, no results will be imported for this sample");
        }
    }

    static List<Element> getElements(Element parent)
    {
        List<Element> ret = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i ++)
        {
            Node node = nl.item(i);
            if (!(node instanceof Element))
                continue;
            ret.add((Element)node);
        }
        return ret;
    }

    static List<Element> getElementsByTagName(Element parent, String tagName)
    {
        List<Element> ret = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i ++)
        {
            Node node = nl.item(i);
            if (!(node instanceof Element))
                continue;
            Element child = (Element) node;
            if (child.getTagName().equals(tagName))
                ret.add(child);
        }
        return ret;
    }

    static Element getElementByTagName(Element parent, String tagName)
    {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i ++)
        {
            Node node = nl.item(i);
            if (!(node instanceof Element))
                continue;
            Element child = (Element) node;
            if (child.getTagName().equals(tagName))
                return child;
        }
        return null;
    }

    static String getInnerText(Element el)
    {
        NodeList nl = el.getChildNodes();
        int len = nl.getLength();
        if (len == 0)
            return "";
        if (len == 1)
            return nl.item(0).getNodeValue();
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < nl.getLength(); i ++)
            ret.append(nl.item(i).getNodeValue());
        return ret.toString();
    }

    /** Returns attribute value using the given name or alternate names. */
    static String getAttribute(Element el, String name, String... alternateNames)
    {
        if (el.hasAttribute(name))
            return el.getAttribute(name);

        for (String attrName : alternateNames)
            if (el.hasAttribute(attrName))
                return el.getAttribute(attrName);

        return null;
    }

    /** Returns attribute value using the given name or alternate names. */
    static String getAttribute(Element el, QName name, QName... alternateNames)
    {
        if (el.hasAttributeNS(name.getNamespaceURI(), name.getLocalPart()))
            return el.getAttributeNS(name.getNamespaceURI(), name.getLocalPart());

        for (QName attrName : alternateNames)
            if (el.hasAttributeNS(attrName.getNamespaceURI(), attrName.getLocalPart()))
                return el.getAttributeNS(attrName.getNamespaceURI(), attrName.getLocalPart());

        return null;
    }

    // For some parameters, the actual range is 262144, but FlowJo coerces
    // the value to something between 0 and 4096.  This code is a bit of a
    // hack to try to detect that case.
    protected double findMultiplier(Element elParameter)
    {
        if ("1".equals(elParameter.getAttribute("log")))
            return 1;
        // Only check for FSC-A, FSC-H, and SSC-A
        String name = elParameter.getAttribute("name");
        if (",FSC-A,FSC-H,SSC-A,".indexOf("," + name + ",") < 0)
            return 1;
        if ("4096".equals(elParameter.getAttribute("range")) && "4096".equals(elParameter.getAttribute("highValue")))
            return 64;
        return 1;
    }

    protected double getRange(Element elParameter)
    {
        if (StringUtils.isEmpty(elParameter.getAttribute("highValue")))
        {
            return 4096;
        }
        return Double.valueOf(elParameter.getAttribute("highValue")).doubleValue() * findMultiplier(elParameter);
    }

    static public String ___cleanName(String name)
    {
        name = StringUtils.replace(name, "<", CompensationMatrix.PREFIX);
        name = StringUtils.replace(name, ">", CompensationMatrix.SUFFIX);
        name = StringUtils.replaceChars(name, ',', ';');
        // replace MacRoman em-dash and en-dash
        name = StringUtils.replaceChars(name, PopulationName.MAC_EMDASH, '-');
        name = StringUtils.replaceChars(name, PopulationName.MAC_ENDASH, '-');
        return name;
    }

    protected double getMultiplier(String name)
    {
        ParameterInfo info = _parameters.get(name);
        if (info == null)
            return 1;
        return info._multiplier;
    }

    protected void readKeywords(SampleInfo sample, Element el)
    {
        for (Element elKeyword : getElementsByTagName(el, "Keyword"))
        {
            String name = StringUtils.trimToNull(elKeyword.getAttribute("name"));
            if (null == name)
                continue;
            // FCS format encodes empty keyword values as a single space character -- convert it to null.
            String value = StringUtils.trimToNull(elKeyword.getAttribute("value"));
            sample.putKeyword(name, value);
        }
    }

    protected Analysis readSampleAnalysis(Element elSampleNode)
    {
        String sampleId = elSampleNode.getAttribute("sampleID");
        SampleInfo sample = getSample(sampleId);
        if (sample == null)
        {
            // Don't read analysis if sample has been marked as 'deleted'
            sample = getDeletedSample(sampleId);
            if (sample != null)
                warning(sample, null, null, "Ignoring deleted sample");
            else
                warning(sampleId, null, null, "No sample info found");
            return null;
        }

        // Don't read analysis if sampleID isn't in "All Samples" group.
        GroupInfo allSamplesGroup = getAllSamplesGroup();
        if (allSamplesGroup != null && !allSamplesGroup.getSampleIds().contains(sampleId))
        {
            warning(sampleId, null, null, "orphaned from the 'All Samples' group -- it may have been deleted");
            return null;
        }

        AttributeSet results = new AttributeSet(ObjectType.fcsAnalysis, null);
        Analysis ret = readAnalysis(elSampleNode, results, sampleId, true);
        _sampleAnalyses.put(sampleId, ret);
        addSampleAnalysisResults(results, sampleId);
        return ret;
    }

    protected abstract Analysis readAnalysis(Element elAnalysis, @Nullable AttributeSet results, String sampleId, boolean warnOnMissingStats);

    protected boolean readSampleDeletedFlag(Element elSample)
    {
        for (Element elSampleNode : getElementsByTagName(elSample, "SampleNode"))
        {
            boolean deleted = "1".equals(elSampleNode.getAttribute("deleted"));
            if (deleted)
                return true;
        }
        return false;
    }

    protected void readStats(SubsetSpec subset, Element elPopulation, @Nullable AttributeSet results, Analysis analysis, String sampleId, boolean warnOnMissingStats)
    {
        readStatsCount(subset, elPopulation, results, analysis, sampleId, warnOnMissingStats);
        readStatsOther(subset, elPopulation, results, analysis, sampleId, warnOnMissingStats);
    }

    protected void readStatsCount(SubsetSpec subset, Element elPopulation, @Nullable AttributeSet results, Analysis analysis, String sampleId, boolean warnOnMissingStats)
    {
        String strCount = elPopulation.getAttribute("count");
        if (results != null)
        {
            StatisticSpec statCount = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
            Double count = null;
            if (!StringUtils.isEmpty(strCount))
                count = Double.valueOf(strCount);

            // If we are at the root and "count" is unavailable, try to get it from the "$TOT" keyword
            if (subset == null && (count == null || count == 0.0d || count == -1.0d))
            {
                SampleInfo sampleInfo = getSample(sampleId);
                if (sampleInfo != null)
                {
                    String strTot = sampleInfo.getKeywords().get("$TOT");
                    if (strTot != null)
                        count = Double.valueOf(strTot);
                }
            }

            if (count != null)
                results.setStatistic(statCount, count);
            else if (warnOnMissingStats)
                warning(sampleId, analysis.getName(), subset, "Count statistic missing");
        }
    }

    protected abstract void readStatsOther(SubsetSpec subset, Element elPopulation, @Nullable AttributeSet results, Analysis analysis, String sampleId, boolean warnOnMissingStats);

    protected void readStat(Element elStat, SubsetSpec subset, @Nullable AttributeSet results, Analysis analysis, String sampleId, boolean warnOnMissingStats,
                            String statisticAttr, String parameterAttr, String percentileAttr, String ancestorAttr)
    {
        // FlowJo v9.7 prefixes the statistic attribute name, e.g. 'statNode_statistic'
        String statistic = getAttribute(elStat, statisticAttr, "statNode_" + statisticAttr);

        // FlowJo v9.7 prefixes the statistic values, e.g. 'statNode_FrequencyOfParent'
        if (statistic.startsWith("statNode_"))
            statistic = statistic.substring("statNode_".length());

        StatisticSpec.STAT stat = STATS.get(statistic);

        // FlowJo 7.2.5 used "FreqOf" name with ancestor of "Total": consider it a Frequency of total statistic
        if (stat == StatisticSpec.STAT.Freq_Of_Ancestor && "Total".equals(elStat.getAttribute(ancestorAttr)))
        {
            stat = StatisticSpec.STAT.Frequency;
        }

        // FlowJo > 7.5 through < 10.0.5 uses the "Freq. of " prefix plus the ancestor population name
        if (stat == null && statistic.startsWith("Freq. of ") && !statistic.equals("Freq. of Total") && elStat.hasAttribute(ancestorAttr))
        {
            stat = StatisticSpec.STAT.Freq_Of_Ancestor;
        }

        if (stat == null)
        {
            warnOnce(sampleId, analysis.getName(), subset, statistic + " statistic not yet supported.");
            return;
        }

        String parameter = null;
        if (stat.isParameterRequired())
        {
            if (stat == StatisticSpec.STAT.Freq_Of_Ancestor)
            {
                // Parameter is ancestor population name
                parameter = StringUtils.trimToNull(elStat.getAttribute(ancestorAttr));
                if (parameter == null)
                {
                    warnOnce(sampleId, analysis.getName(), subset, "Frequency of ancestor stat requires '" + ancestorAttr + "' attribute.");
                    return;
                }
            }
            else
            {
                // Parameter is channel name
                parameter = StringUtils.trimToNull(elStat.getAttribute(parameterAttr));
                if (parameter != null)
                    parameter = ___cleanName(parameter);

                if (stat == StatisticSpec.STAT.Percentile)
                {
                    String percentile = StringUtils.trimToNull(elStat.getAttribute(percentileAttr));
                    if (percentile == null)
                    {
                        warnOnce(sampleId, analysis.getName(), subset, "Percentile stat requires '" + percentileAttr + "' attribute.");
                        return;
                    }
                    else
                    {
                        parameter = parameter + ":" + percentile;
                    }
                }
            }
        }

        StatisticSpec spec = new StatisticSpec(subset, stat, parameter);
        analysis.addStatistic(spec);

        if (results != null)
        {
            // FlowJo v9.7 changed attribute 'value' to 'statValue'
            String strValue = StringUtils.trimToNull(getAttribute(elStat, "value", "statValue"));
            if (strValue != null && !strValue.equals("\ufffd") && !strValue.equals("¥"))
            {
                double value;
                try
                {
                    value = Double.valueOf(strValue).doubleValue();
                }
                catch (NumberFormatException nfe)
                {
                    warning(sampleId, analysis.getName(), subset, stat.getLongName() + " statistic value invalid double value: " + strValue);
                    return;
                }

                // PC FlowJo version doesn't return frequencies in 0-100 range
                if (this instanceof PCWorkspace && value > 0 && value < 1)
                {
                    if (stat == StatisticSpec.STAT.Frequency || stat == StatisticSpec.STAT.Freq_Of_Parent || stat == StatisticSpec.STAT.Freq_Of_Grandparent || stat == StatisticSpec.STAT.Freq_Of_Ancestor)
                        value = 100.0d * value;
                }

                results.setStatistic(spec, value);
            }
            else
            {
                if (warnOnMissingStats)
                {
                    // Issue 22348: ignore missing statistic values for populations that have 0 cells
                    Map<StatisticSpec, Double> stats = results.getStatistics();
                    Double count = stats.get(new StatisticSpec(subset, StatisticSpec.STAT.Count, null));
                    if (count != null && count > 0)
                        warning(sampleId, analysis.getName(), subset, stat.getLongName() + " statistic value missing");
                }
            }
        }
    }


    // Get case-normalized axis name
    protected String getNormalizedParameterName(String param)
    {
        param = ___cleanName(param);
        if (_parameters.containsKey(param))
            return _parameters.get(param)._name;

        return param;
    }

    protected double[] toDoubleArray(List<Double> lst)
    {
        double[] ret = new double[lst.size()];
        for (int i = 0; i < lst.size(); i ++)
        {
            ret[i] = lst.get(i).doubleValue();
        }
        return ret;
    }

    protected double parseParamValue(String param, Element el, String attribute)
    {
        return Double.valueOf(el.getAttribute(attribute)).doubleValue();
    }


    protected void warnOnce(String msg)
    {
        warnOnce(null, null, null, msg);
    }

    protected void warnOnce(String sampleId, PopulationName name, SubsetSpec subset, String msg)
    {
        for (String warning : getWarnings())
        {
            if (warning.endsWith(msg))
                return;
        }

        warning(sampleId, name, subset, msg);
    }

    protected void warning(String sampleId, PopulationName name, SubsetSpec subset, String msg)
    {
        SampleInfo sampleInfo = null;
        if (sampleId != null)
        {
            sampleInfo = getSample(sampleId);
            if (sampleInfo == null)
                sampleInfo = getDeletedSample(sampleId);

            if (sampleInfo == null)
            {
                // Just create a dummy sample for error reporting
                sampleInfo = new SampleInfo();
                sampleInfo._sampleId = sampleId;
            }
        }

        warning(sampleInfo, name, subset, msg);
    }

    protected void warning(SampleInfo sample, PopulationName name, SubsetSpec subset, String msg)
    {
        StringBuilder sb = new StringBuilder();
        if (sample != null)
            sb.append("Sample ").append(sample.toString()).append(": ");

        if (name != null)
            sb.append(name.toString()).append(": ");

        if (subset != null)
            sb.append(subset.toString()).append(": ");

        sb.append(msg);
        warning(sb.toString());
    }

    protected void warning(String str)
    {
        _warnings.add(str);
    }

    /**
     * There are some
     * @param axis
     * @param values
     */
    protected void scaleValues(String axis, List<Double> values)
    {
        double multiplier = getMultiplier(axis);
        if (multiplier == 1)
        {
            return;
        }
        assert multiplier == 64;
        for (Double value : values)
        {
            if (value.doubleValue() > 4096 * 1.05) // small fudge factor for gates nudged above scale
                return;
        }
        for (int i = 0; i < values.size(); i ++)
        {
            values.set(i, values.get(i) * multiplier);
        }
    }

    private CalibrationTable getCalibrationTable(String param)
    {
        if (param.startsWith(CompensationMatrix.PREFIX) && param.endsWith(CompensationMatrix.SUFFIX))
        {
            param = param.substring(CompensationMatrix.PREFIX.length(), param.length() - CompensationMatrix.SUFFIX.length());
        }
        ParameterInfo info = _parameters.get(param);
        if (info == null)
            return null;
        return info._calibrationTable;
    }

    private double interpolate(double v1, double v2, CalibrationTable ct, int index, int count)
    {
        double i1 = ct.indexOf(v1);
        double i2 = ct.indexOf(v2);
        return ct.fromIndex(i2 * index / count + i1 * (count - index) / count);
    }

    /**
     * Decide the number of points that it will be necessary to add to a line in a polygon so that LabKey's representation
     * of the polygon will closely match FlowJo's interpretation.
     * FlowJo makes their polygons have straight lines in the scaled (logarithmic) space.  In order to not have this
     * introduce differences, LabKey interpolates the polygon points.
     * We decide here that the number of points necessary to interpolate a diagonal line is the lesser of the following:
     * a) 10
     * b) The number of 64ths of the graph range that the line travels in the x and y directions
     */

    private int decideInterpCount(double v1, double v2, CalibrationTable ct)
    {
        double dScale = Math.abs(ct.indexOf(v1) - ct.indexOf(v2)) * 64 / ct.getRange();
        if (dScale <= 1)
            return 1;
        return Math.min(10, (int) dScale);
    }

    protected void interpolateLine(List<Double> lstX, List<Double> lstY, double x1, double y1, double x2, double y2, CalibrationTable ctX, CalibrationTable ctY)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 || dy == 0)
        {
            lstX.add(x2);
            lstY.add(y2);
            return;
        }

        int interpCount = Math.min(decideInterpCount(x1, x2, ctX), decideInterpCount(y1, y2, ctY));
        for (int i = 1; i <= interpCount; i ++)
        {
            lstX.add(interpolate(x1, x2, ctX, i, interpCount));
            lstY.add(interpolate(y1, y2, ctY, i, interpCount));
        }
    }

    /**
     * FlowJo computes the polygon in transformed space.  LabKey applies the polygon to untransformed values.
     * In order to ensure that the results we get are comparable, LabKey fills in the points along some of the diagonal
     * lines of the polygon with extra points so as not to have an error.
     */
    protected PolygonGate interpolatePolygon(PolygonGate polygonGate)
    {
        CalibrationTable ctX = getCalibrationTable(polygonGate.getXAxis());
        CalibrationTable ctY = getCalibrationTable(polygonGate.getYAxis());
        if (ctX.isLinear() && ctY.isLinear())
            return polygonGate;

        List<Double> lstX = new ArrayList();
        List<Double> lstY = new ArrayList();
        Polygon polygon = polygonGate.getPolygon();
        double x1 = polygon.X[polygon.len - 1];
        double y1 = polygon.Y[polygon.len - 1];
        for (int i = 0; i < polygon.len; i ++)
        {
            double x2 = polygon.X[i];
            double y2 = polygon.Y[i];
            interpolateLine(lstX, lstY, x1, y1, x2, y2, ctX, ctY);
            x1 = x2;
            y1 = y2;
        }
        polygon = new Polygon(lstX, lstY);
        return new PolygonGate(polygonGate.getXAxis(), polygonGate.getYAxis(), polygon);
    }


    public static class LoadTests extends Assert
    {
        private File projectRoot()
        {
            String projectRootPath =  AppProps.getInstance().getProjectRoot();
            if (projectRootPath == null)
                projectRootPath = System.getProperty("user.dir");
            return new File(projectRootPath);
        }

        private Workspace loadWorkspace(String path) throws Exception
        {
            File file = new File(projectRoot(), "sampledata/" + path);
            return Workspace.readWorkspace(file.getName(), path, new FileInputStream(file));
        }

        @Test
        public void loadOldMac() throws Exception
        {
            loadWorkspace("flow/8color/workspace.xml");
        }

        @Test
        public void loadPC_5_7_2() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/versions/v5.7.2.xml");
            assertPC(workspace, "5.7.2");
        }

        @Test
        public void loadPC_7_2_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/versions/v7.2.5.wsp");
            assertPC(workspace, "7.2.5");
        }

        @Test
        public void loadPC_7_6_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/versions/v7.6.5.wsp");
            assertPC(workspace, "7.6.5");
        }

        @Test
        public void loadPC_10_0_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/versions/v10.0.5.wsp");
            assertPC(workspace, "10.0.5");
        }

        private void assertPC(Workspace workspace, String version)
        {
            assertEquals(72, workspace.getSampleCount());
            assertEquals(72, workspace._sampleAnalyses.size());
            assertEquals(72, workspace._sampleAnalysisResults.size());
            assertEquals(2, workspace.getGroups().size());
            assertEquals("panel 1", workspace.getGroups().get(1).getGroupName().toString());
            assertEquals(72, workspace.getGroups().get(1).getSampleIds().size());
            assertEquals(2, workspace.getGroupAnalyses().size());
            assertEquals(11, workspace.getParameterNames().size());
            assertEquals(0, workspace.getWarnings().size());

            SampleInfo sampleInfo = workspace.getSample("2");
            assertEquals("Specimen_001_stain.fcs", sampleInfo.getLabel());

            Analysis analysis = workspace.getSampleAnalysis(sampleInfo);
            Population cd3cd4 = workspace.findPopulation(analysis, SubsetSpec.fromUnescapedString("Viable/Lymphocytes/CD3+CD4+"));
            assertEquals(1, cd3cd4.getGates().size());
            PolygonGate cd3cd4gate = (PolygonGate)cd3cd4.getGates().get(0);
            assertEquals("PE-A", cd3cd4gate.getXAxis());
            assertEquals("APC-A", cd3cd4gate.getYAxis());
            assertEquals(8, cd3cd4gate.getPolygon().len);

            AttributeSet attrs = workspace.getSampleAnalysisResults(sampleInfo);
            Map<StatisticSpec, Double> stats = attrs.getStatistics();
            if (version.equals("5.7.2") || version.equals("7.2.5"))
            {
                assertEquals(3821, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Count")).intValue());
                assertEquals(33.465, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Freq_Of_Parent")), 0.001d);
            }
            else if (version.equals("10.0.5"))
            {
                assertEquals(3811, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Count")).intValue());
                assertEquals(33.194, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Freq_Of_Parent")), 0.001d);
            }
            else
            {
                assertEquals(3832, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Count")).intValue());
                assertEquals(33.316, stats.get(new StatisticSpec("Viable/Lymphocytes/CD3+CD4+:Freq_Of_Parent")), 0.001d);
            }

        }

        @Test
        public void loadMacAdvanced_8_5_3() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v8.5.3.xml");
            assertAdvanced(workspace, "8.5.3", true);
        }

        @Test
        public void loadMacAdvanced_9_6_4() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v9.6.4.xml");
            assertAdvanced(workspace, "9.6.4", true);
        }

        @Test
        public void loadMacAdvanced_9_7_2() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v9.7.2.xml");
            assertAdvanced(workspace, "9.7.2", true);
        }

        @Test
        public void loadPCAdvanced_7_2_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v7.2.5.wsp");
            assertAdvanced(workspace, "7.2.5", false);
        }

        @Test
        public void loadPCAdvanced_7_5_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v7.5.5.wsp");
            assertAdvanced(workspace, "7.5.5", false);
        }

        @Test
        public void loadPCAdvanced_7_6_3() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v7.6.3.wsp");
            assertAdvanced(workspace, "7.6.3", false);
        }

        @Test
        public void loadPCAdvanced_7_6_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v7.6.5.wsp");
            assertAdvanced(workspace, "7.6.5", false);
        }

        @Test
        public void loadPCAdvanced_10_0_5() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v10.0.5.wsp");
            assertAdvanced(workspace, "10.0.5", false);
        }

        @Test
        public void loadPCAdvanced_10_0_6() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v10.0.6.wsp");
            assertAdvanced(workspace, "10.0.6", false);
        }

        @Test
        public void loadPCAdvanced_10_0_7() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v10.0.7.wsp");
            assertAdvanced(workspace, "10.0.7", false);
        }

        @Test
        public void loadPCAdvanced_10_0_8() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v10.0.8.wsp");
            assertAdvanced(workspace, "10.0.8", false);
        }

        @Test
        public void loadPCAdvanced_10_2() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/advanced/advanced-v10.2.wsp");
            assertAdvanced(workspace, "10.2", false);
        }

        private boolean isVersionGreaterThan10_0_7(String version)
        {
            int[] version10_0_7 = new int[]{10, 0, 7};

            String[] testVersion = version.split("\\.");

            for (int i = 0; i < version10_0_7.length; i++)
            {
                if (testVersion[i] == null)
                {
                    return false;
                }

                int testVersionPart = Integer.parseInt(testVersion[i]);
                int version_10_0_7_Part = version10_0_7[i];

                if (testVersionPart != version_10_0_7_Part)
                {
                    return testVersionPart > version_10_0_7_Part;
                }
            }

            return false;
        }

        @Test
        public void testVersionMethod()
        {
            assertEquals(true, isVersionGreaterThan10_0_7("10.2"));
            assertEquals(true, isVersionGreaterThan10_0_7("10.0.8"));
            assertEquals(false, isVersionGreaterThan10_0_7("10.0.7"));
            assertEquals(false, isVersionGreaterThan10_0_7("7.2.5"));
        }

        private void assertAdvanced(Workspace workspace, String version, boolean mac)
        {
            assertEquals(16, workspace.getSampleCount());
            assertEquals(16, workspace._sampleAnalyses.size());
            assertEquals(16, workspace._sampleAnalysisResults.size());
            if (isVersionGreaterThan10_0_7(version))
            {
                // 10.0.8 automagically added a "Compensation" group
                assertTrue(workspace._groupInfos.containsKey("Compensation"));
                assertEquals(8, workspace._groupInfos.size());
                assertEquals(8, workspace._groupAnalyses.size());
            }
            else
            {
                assertEquals(7, workspace._groupInfos.size());
                assertEquals(7, workspace._groupAnalyses.size());
            }
            //assertEquals(1, workspace.getCompensationMatrices().size());
            assertEquals(5, workspace.getParameterNames().size());

            // warnings
            if ("10.0.5".equals(version))
            {
                assertEquals(5, workspace.getWarnings().size());
                assertTrue(workspace.getWarnings().get(0).contains("Coefficient of Variation statistic value missing"));
                assertTrue(workspace.getWarnings().get(1).contains("Mode statistic not yet supported"));
            }
            else if ("10.2".equals(version))
            {
                assertEquals(4, workspace.getWarnings().size());
                assertTrue(workspace.getWarnings().get(0).contains("Lymphocytes/CD45+: Median statistic value missing"));
                assertTrue(workspace.getWarnings().get(1).contains("Lymphocytes/T cells/CD4 T: Coefficient of Variation statistic value missing"));
            }
            else
            {
                assertEquals(StringUtils.join(workspace.getWarnings(), "\n"), 1, workspace.getWarnings().size());
                assertTrue(workspace.getWarnings().get(0).contains("Mode statistic not yet supported"));
            }

            String windowsSampleId = "2";
            String sampleFileName = "931115-B02- Sample 01.fcs";
            if ("10.2".equals(version))
            {
                windowsSampleId = "6";
                sampleFileName = "931115-C02- Sample 02.fcs";
            }
            String sampleId = mac ? "268435458" : windowsSampleId;
            SampleInfo sample = workspace.getSample(sampleId);
            assertEquals(sampleFileName, sample.getLabel());

            Analysis analysis = workspace.getSampleAnalysis(sample);
            assertEquals(20, analysis.getPopulations().size());
            Population A = workspace.findPopulation(analysis, SubsetSpec.fromParts("A"));
            PolygonGate Agate = (PolygonGate)A.getGates().get(0);
            assertEquals("Fluor", Agate.getXAxis());
            assertEquals("PhyEry", Agate.getYAxis());
            if (mac)
            {
                assertEquals(new Polygon(
                        new double[] { 1.0496, 19.7711, 19.7711, 1.0496 },
                        new double[] { 4.7977, 4.7977, 0.3244, 0.3244 }),
                        Agate.getPolygon());
            }
            else
            {
                assertEquals(new Polygon(
                        new double[] { 1.1073, 1.1073, 17.2391, 17.2391 },
                        new double[] { 0.3010, 4.5324, 4.5324, 0.3010 }),
                        Agate.getPolygon());
            }

            Population AandnotB = workspace.findPopulation(analysis, SubsetSpec.fromParts("A and not B"));
            AndGate AandnotBgate = (AndGate)AandnotB.getGates().get(0);
            assertEquals("A", ((SubsetRef)AandnotBgate.getGates().get(0)).getRef().toString());
            if (mac)
            {
                // Foolishly, I created the gate as "A & ! B" when setting up the mac workspace instead of "A & 'not B'"
                NotGate notB = (NotGate)AandnotBgate.getGates().get(1);
                assertEquals("B", ((SubsetRef)notB.getGate()).getRef().toString());
            }
            else
            {
                assertEquals("not B", ((SubsetRef) AandnotBgate.getGates().get(1)).getRef().toString());
            }

            Population bifurcateCD8plus = workspace.findPopulation(analysis, SubsetSpec.fromParts("bifurcate CD8+"));
            IntervalGate bifurcateCD8plusGate = (IntervalGate)bifurcateCD8plus.getGates().get(0);
            assertEquals("PhyEry", bifurcateCD8plusGate.getXAxis());
            if (mac)
            {
                assertEquals(18.451, bifurcateCD8plusGate.getMin(), 0.001);
                assertEquals(127652.657, bifurcateCD8plusGate.getMax(), 0.001);
            }
            else
            {
                assertEquals(16.635, bifurcateCD8plusGate.getMin(), 0.001);
                if ("7.2.5".equals(version))
                    assertEquals(1384.662, bifurcateCD8plusGate.getMax(), 0.001d);
                else
                    assertEquals(Double.MAX_VALUE, bifurcateCD8plusGate.getMax(), DELTA);
            }

            Population CD4CD8ellipse = workspace.findPopulation(analysis, SubsetSpec.fromParts("CD4, CD8 ellipse"));
            assertNotNull(CD4CD8ellipse);
            //EllipseGate CD4CD8ellipseGate = (EllipseGate)CD4CD8ellipse.getGates().get(0);
            //assertEquals(...);
            //assertEquals("ForSc", CD4CD8ellipse.getXAxis());
            //assertEquals("OrthSc", CD4CD8ellipse.getYAxis());

            // Not gate that references an ellipse gate
            Population notCD4CD8ellipse = workspace.findPopulation(analysis, SubsetSpec.fromParts("not CD4, CD8 ellipse"));
            NotGate notCD4CD8ellipseGate = (NotGate)notCD4CD8ellipse.getGates().get(0);
            SubsetRef CD4CD8ellipseRef = (SubsetRef)notCD4CD8ellipseGate.getGate();
            assertEquals("CD4, CD8 ellipse", CD4CD8ellipseRef.getRef().toString());

            // ellipse gate with eventsInside="0"
            Population invertedEllipse = workspace.findPopulation(analysis, SubsetSpec.fromParts("inverted ellipse"));
            assertNotNull(invertedEllipse);
            //EllipseGate invertedEllipseGate = (EllipseGate)invertedEllipse.getGates().get(0);
            //assertEquals("ForSc", invertedEllipseGate.getXAxis());
            //assertEquals("OrthSc", invertedEllipseGate.getYAxis());

            // I labeled the mac quadrant gates backwards
            String Q1name = mac ? "Q1: CD8-, CD4+" : "Q1: CD4- , CD8+";
            Population Q1 = workspace.findPopulation(analysis, SubsetSpec.fromParts(Q1name));
            PolygonGate Q1gate = (PolygonGate)Q1.getGates().get(0);
            //assertEquals(...);

            AttributeSet results = workspace.getSampleAnalysisResults(sample);
            Map<StatisticSpec, Double> stats = results.getStatistics();
            assertEquals(10000, stats.get(new StatisticSpec("Count")).intValue());
            if (mac)
            {
                // CONSIDER: Casing of 'T Cells' different from PC workspaces.  Make StatisticSpec case-insensitive
                assertEquals(7542,    stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                assertEquals(75.420d, stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                assertEquals(62.198d, stats.get(new StatisticSpec("Lymphocytes/T Cells:Freq_Of_Parent")), 0.001d);
                assertEquals(1391,    stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Count")).intValue());
                assertEquals(72.000d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                assertEquals(39.000d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:CV(Fluor)")), 0.001d);
                assertEquals(42.900d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                assertEquals(46.700d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Mean(Fluor)")), 0.001d);
                assertEquals(45.600d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Median(Fluor)")), 0.001d);
                //assertEquals(40.500d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:RobustCV(Fluor)")), 0.001d);
                assertEquals(18.400d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Freq_Of_Grandparent")), 0.001d);
                assertEquals(29.700d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Freq_Of_Parent")), 0.001d);
                assertEquals(13.900d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Frequency")), 0.001d);
                assertEquals(18.400d, stats.get(new StatisticSpec("Lymphocytes/T Cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                assertEquals(3049.0d, stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
            }
            else
            {
                // PC statistics seem to change quite often
                if ("7.2.5".equals(version))
                {
                    assertEquals(7554,    stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                    assertEquals(75.540d, stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                    assertEquals(61.702d, stats.get(new StatisticSpec("Lymphocytes/T cells:Freq_Of_Parent")), 0.001d);
                    assertEquals(1381d,   stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")), 0.001d);
                    assertEquals(72.191d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                    assertEquals(38.258d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:CV(Fluor)")), 0.001d);
                    assertEquals(43.602d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                    assertEquals(47.223d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mean(Fluor)")), 0.001d);
                    assertEquals(45.803d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Median(Fluor)")), 0.001d);
                    //assertEquals(39.947d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:RobustCV(Fluor)")), 0.001d);
                    assertEquals(18.281d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Grandparent")), 0.001d);
                    assertEquals(18.281d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                    assertEquals(29.628d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Parent")), 0.001d);
                    assertEquals(13.810d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Frequency")), 0.001d);
                    assertEquals(2867.0d, stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
                }
                else if ("10.0.5".equals(version))
                {
                    assertEquals(27,      stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                    assertEquals(0.27d,   stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                    assertEquals(48.148d, stats.get(new StatisticSpec("Lymphocytes/T cells:Freq_Of_Parent")), 0.001d);
                    assertEquals(5d,      stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")), 0.001d);
                    assertEquals(61.806d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                    assertEquals(44.101d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:CV(Fluor)")), 0.001d);
                    assertEquals(40.694d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                    assertEquals(46.791d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mean(Fluor)")), 0.001d);
                    assertEquals(50.673d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Median(Fluor)")), 0.001d);
                    //assertEquals(0d,      stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:RobustCV(Fluor)")), 0.001d);
                    // XXX: FreqOfParent wasn't migrated properly by FlowJo from v7.6.5 to v10.0.5
                    //assertEquals(18.519d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Grandparent")), 0.001d);
                    assertEquals(18.519d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                    assertEquals(38.462d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Parent")), 0.001d);
                    assertEquals(0.0500d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Frequency")), 0.001d);
                    assertEquals(2867.0d, stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
                }
                else if ("10.2".equals(version))
                {
                    assertEquals(105,      stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                    assertEquals(1.05d,    stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                    assertEquals(35.238d,  stats.get(new StatisticSpec("Lymphocytes/T cells:Freq_Of_Parent")), 0.001d);
                    assertEquals(4d,       stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")), 0.001d);
                    assertEquals(4.0d,     stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")), 0.001d);
                    assertEquals(26.979d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                    assertEquals(36.198d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:CV(Fluor)")), 0.001d);
                    assertEquals(16.998d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                    assertEquals(17.776d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mean(Fluor)")), 0.001d);
                    assertEquals(15.795d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Median(Fluor)")), 0.001d);
                    assertEquals(3.809d,   stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                    assertEquals(10.810d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Parent")), 0.001d);
                    assertEquals(0.0400d,  stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Frequency")), 0.001d);
                    assertEquals(2616.0d,  stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
                }
                else if ("10.0.6".equals(version) || "10.0.7".equals(version) || "10.0.8".equals(version) || "10.2".equals(version))
                {
                    // It's tiresome caring about stat values
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells:Freq_Of_Parent")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")).intValue());
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:CV(Fluor)")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mean(Fluor)")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Median(Fluor)")), 0.001d);
                    //assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:RobustCV(Fluor)")), 0.001d);
                    // XXX: FreqOfParent wasn't migrated properly by FlowJo from v7.6.5 to v10.0.5
                    //assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Grandparent")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Parent")), 0.001d);
                    assertNotEquals(0,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Frequency")), 0.001d);
                    assertEquals(2867.0d, stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
                }
                else
                {
                    assertEquals(7548,    stats.get(new StatisticSpec("Lymphocytes:Count")).intValue());
                    assertEquals(75.480d, stats.get(new StatisticSpec("Lymphocytes:Freq_Of_Parent")), 0.001d);
                    assertEquals(61.711d, stats.get(new StatisticSpec("Lymphocytes/T cells:Freq_Of_Parent")), 0.001d);
                    assertEquals(1379,    stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Count")).intValue());
                    assertEquals(72.207d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Percentile(Fluor:90)")), 0.001d);
                    assertEquals(38.137d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:CV(Fluor)")), 0.001d);
                    assertEquals(43.686d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Geometric_Mean(Fluor)")), 0.001d);
                    assertEquals(47.283d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mean(Fluor)")), 0.001d);
                    assertEquals(45.861d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Median(Fluor)")), 0.001d);
                    //assertEquals(39.386d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:RobustCV(Fluor)")), 0.001d);
                    assertEquals(18.270d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Grandparent")), 0.001d);
                    assertEquals(18.270d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Ancestor(Lymphocytes)")), 0.001d);
                    assertEquals(29.605d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Freq_Of_Parent")), 0.001d);
                    assertEquals(13.790d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Frequency")), 0.001d);
                    assertEquals(2867.0d, stats.get(new StatisticSpec("A and not B:Count")).intValue(), 0.001d);
                }
            }

            // ... when we support Mode
            //if ("7.2.5.".equals(version) || "7.5.5".equals(version))
            //    assertEquals(57.681d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mode(Fluor)")), 0.001d);
            //else
            //    assertEquals(37.457d, stats.get(new StatisticSpec("Lymphocytes/T cells/CD4 T:Mode(Fluor)")), 0.001d);
        }

        @Test
        public void loadPV1() throws Exception
        {
            loadWorkspace("flow/flowjoquery/Workspaces/PV1-public.xml");
        }

        @Test
        public void loadMiniFCS() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/flowjoquery/miniFCS/mini-fcs.xml");
            GroupInfo group = workspace.getGroup("3");
            Analysis analysis = workspace.getGroupAnalysis(group);

            SubsetSpec allCytSpec = SubsetSpec.fromUnescapedString("S/Lv/L/3+/4+/All Cyt");
            SubsetSpec aliasSpec = FCSAnalyzer.get().getSubsetAlias(analysis, allCytSpec);
            assertEquals("S/Lv/L/3+/4+/(IFNg+|IL2+|IL4+|TNFa+)", aliasSpec.toString());
        }

        @Test
        public void loadSubsets() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/flowjoquery/Workspaces/subset-parsing.xml");
            SampleInfo sampleInfo = workspace.getSample("2");
            assertEquals("118795.fcs", sampleInfo.getLabel());

            AttributeSet attrs = workspace.getSampleAnalysisResults(sampleInfo);
            Set<StatisticSpec> stats = attrs.getStatisticNames();

            // Check boolean gates are in the correct order and illegal characters in subset expression alias are not escaped.
            // NODE: FlowJo writes the boolean gates in seemingly random order so resaving subset-parsing.xml could break this test.
            {
                SubsetSpec subset = SubsetSpec.fromParts("A and not (B or C)");
                StatisticSpec stat = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
                assertTrue("Expected statistic '" + stat + "' in analysis results.", stats.contains(stat));

                List<String> aliases = new ArrayList<>();
                for (Object alias : attrs.getStatisticAliases(stat))
                    aliases.add(alias.toString());

                assertEquals(1, aliases.size());
                String alias = aliases.get(0);
                assertEquals("({A & co: fun}&!(B|{C (awesome)})):Count", alias);
            }

            // Check subset population names are cleaned:
            //   Name as it appears in FJ:  Z,|;<z>!
            //   LabKey name in database:   Z,|;<z>!     -- not escaped, no illegal characters
            //   Alias for <11.1 compat:    Z;|;<z>!     -- comma is replaced with semicolon
            {
                SubsetSpec subset = SubsetSpec.fromParts("B", "Z,|;<z>!");
                StatisticSpec stat = new StatisticSpec(subset, StatisticSpec.STAT.Freq_Of_Parent, null);
                assertTrue("Expected statistic '" + stat + "' in analysis results.", stats.contains(stat));

                // Name as it will appear in the database
                assertEquals("B/Z,|;<z>!:Freq_Of_Parent", stat.toString());

                // Name as it will appear in the UI
                assertEquals("B/Z,|;<z>!:%P", stat.toShortString());

                List<String> aliases = new ArrayList<>();
                for (Object alias : attrs.getStatisticAliases(stat))
                    aliases.add(alias.toString());

                assertEquals(1, aliases.size());
                String alias = aliases.get(0);

                // Alias as it will appear in the database
                assertEquals("B/Z;|;<z>!:Freq_Of_Parent", alias);
            }

            // Check subset names that are a part of a boolean expression are cleaned
            {
                SubsetSpec subset = SubsetSpec.fromParts("B", "Y and (B/Z or not X-1)");
                StatisticSpec stat = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
                assertTrue("Expected statistic '" + stat + "' in analysis results.", stats.contains(stat));

                List<String> aliases = new ArrayList<>();
                for (Object alias : attrs.getStatisticAliases(stat))
                    aliases.add(alias.toString());

                assertEquals(1, aliases.size());
                String alias = aliases.get(0);
                // Alias is escaped because it contains illegal characters in the expression.
                assertEquals("B/({Y{foo\\}}&({Z;|;<z>!}|!{X (x&x)})):Count", alias);
            }
        }

        private static final double DELTA = 1E-8;

        @Test
        public void loadBooleanSubPopulations() throws Exception
        {
            Workspace workspace = loadWorkspace("flow/flowjoquery/Workspaces/boolean-sub-populations.xml");
            SampleInfo sampleInfo = workspace.getSample("1");
            assertEquals("118795.fcs", sampleInfo.getLabel());

            Analysis analysis = workspace.getSampleAnalysis(sampleInfo);
            assertEquals(3, analysis.getPopulations().size());

            // And gate named "A&B"
            Population AandB = analysis.getPopulation(PopulationName.fromString("A&B"));
            assertEquals(1, AandB.getGates().size());
            assertTrue(AandB.getGates().get(0) instanceof AndGate);

            AndGate AandBgate = (AndGate)AandB.getGates().get(0);
            assertEquals(SubsetSpec.fromParts("A"), ((SubsetRef)AandBgate.getGates().get(0)).getRef());
            assertEquals(SubsetSpec.fromParts("B"), ((SubsetRef)AandBgate.getGates().get(1)).getRef());

            // Or gate named "C|D"
            Population CorD = AandB.getPopulation(PopulationName.fromString("C|D"));
            assertEquals(1, CorD.getGates().size());
            assertTrue(CorD.getGates().get(0) instanceof OrGate);

            OrGate CorDgate = (OrGate)CorD.getGates().get(0);
            assertEquals(SubsetSpec.fromParts("A&B", "C"), ((SubsetRef)CorDgate.getGates().get(0)).getRef());
            assertEquals(SubsetSpec.fromParts("A&B", "D"), ((SubsetRef)CorDgate.getGates().get(1)).getRef());


            // Check count stats of the boolean populations.
            AttributeSet results = workspace.getSampleAnalysisResults(sampleInfo);
            Map<StatisticSpec, Double> stats = results.getStatistics();
            assertEquals(10000d, stats.get(new StatisticSpec(null, StatisticSpec.STAT.Count, null)), DELTA);
            assertEquals(2983d, stats.get(new StatisticSpec(SubsetSpec.fromParts("A&B"), StatisticSpec.STAT.Count, null)), DELTA);
            assertEquals(1256d, stats.get(new StatisticSpec(SubsetSpec.fromParts("A&B", "C|D"), StatisticSpec.STAT.Count, null)), DELTA);


            // Check for backwards-compatibility aliases
            checkAlias(results, new StatisticSpec("{A&B}:Count"), new StatisticSpec("(A&B):Count"));
            checkAlias(results, new StatisticSpec("{A&B}:Freq_Of_Parent"), new StatisticSpec("(A&B):Freq_Of_Parent"));
            checkAlias(results, new StatisticSpec("{A&B}/C:Count"), new StatisticSpec("(A&B)/C:Count"));
            checkAlias(results, new StatisticSpec("{A&B}/C:Freq_Of_Parent"), new StatisticSpec("(A&B)/C:Freq_Of_Parent"));
            checkAlias(results, new StatisticSpec("{A&B}/D:Count"), new StatisticSpec("(A&B)/D:Count"));
            checkAlias(results, new StatisticSpec("{A&B}/D:Freq_Of_Parent"), new StatisticSpec("(A&B)/D:Freq_Of_Parent"));
            checkAlias(results, new StatisticSpec("{A&B}/{C|D}:Count"), new StatisticSpec("(A&B)/(C|D):Count"));
            checkAlias(results, new StatisticSpec("{A&B}/{C|D}:Freq_Of_Parent"), new StatisticSpec("(A&B)/(C|D):Freq_Of_Parent"));
        }

        private void checkAlias(AttributeSet attrs, StatisticSpec spec, StatisticSpec expectedAlias)
        {
            Iterable<StatisticSpec> alises = attrs.getStatisticAliases(spec);
            StatisticSpec actualAlias = alises.iterator().next();
            assertEquals(expectedAlias.toString(), actualAlias.toString());
        }

        private void assertEquals(Polygon expected, Polygon actual)
        {
            assertEquals("Polygon length", expected.len, actual.len);
            assertEquals("Polygon X", expected.X, actual.X);
            assertEquals("Polygon Y", expected.Y, actual.Y);
        }

        private void assertEquals(String msg, double[] expected, double[] actual)
        {
            assertEquals(msg + ": lengths not equal", expected.length, actual.length);
            for (int i = 0; i < expected.length; i++)
                assertEquals(": values[" + i + "] not equal", expected[i], actual[i], 0.001);
        }

    }
}
