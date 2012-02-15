/*
 * Copyright (c) 2006-2012 LabKey Corporation
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
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PCWorkspace extends FlowJoWorkspace
{
    public PCWorkspace(String name, Element elDoc)
    {
        super(name, elDoc);
    }

    protected void readSample(Element elSample)
    {
        SampleInfo sampleInfo = new SampleInfo();
        for (Element elKeywords : getElementsByTagName(elSample, "Keywords"))
        {
            readKeywords(sampleInfo, elKeywords);
        }
        readParameterInfo(sampleInfo);

        Element elSampleNode = getElementByTagName(elSample, "SampleNode");
        sampleInfo._sampleId = elSampleNode.getAttribute("sampleID");
        if (elSampleNode.hasAttribute("compensationID"))
        {
            sampleInfo._compensationId = elSampleNode.getAttribute("compensationID");
        }

        _sampleInfos.put(sampleInfo.getSampleId(), sampleInfo);
        readSampleAnalysis(elSampleNode);
    }

    protected void readParameterInfo(SampleInfo sampleInfo)
    {
        for (int i = 1; i < 100; i ++)
        {
            String paramName = FCSHeader.getParameterName(sampleInfo._keywords, i);
            if (paramName == null)
                break;
            if (_parameters.containsKey(paramName))
                continue;

            ParameterInfo paramInfo = new ParameterInfo();
            paramInfo.name = paramName;
            paramInfo.multiplier = 1;
            String paramDisplay = sampleInfo._keywords.get("P" + i + "DISPLAY");
            String paramRange = sampleInfo._keywords.get("$P" + i + "R");
            if ("LIN".equals(paramDisplay))
            {
                double range = Double.valueOf(paramRange).doubleValue();
                if (range > 4096)
                {
                    paramInfo.multiplier = range/4096;
                }
            }
            _parameters.put(paramInfo.name, paramInfo);
        }
    }

    protected void readStats(SubsetSpec subset, Element elPopulation, @Nullable AttributeSet results, Analysis analysis, boolean warnOnMissingStats)
    {
        String strCount = elPopulation.getAttribute("count");
        if (results != null)
        {
            if (!StringUtils.isEmpty(strCount))
            {
                StatisticSpec statCount = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
                results.setStatistic(statCount, Double.valueOf(strCount).doubleValue());
            }
            else
            {
                if (warnOnMissingStats)
                    warning(analysis.getName(), subset, "Count statistic missing");
            }
        }
        for (Element elSubpopulations : getElementsByTagName(elPopulation, "Subpopulations"))
        {
            for (Element elStat : getElementsByTagName(elSubpopulations, "Statistic"))
            {
                readStat(elStat, subset, results, analysis, warnOnMissingStats,
                        "name", "id", "percent");
            }
        }
    }

    protected PolygonGate readPolygonGate(Element elPolygonGate)
    {
        String xAxis = null;
        String yAxis = null;
        List<Double> lstX = new ArrayList<Double>();
        List<Double> lstY = new ArrayList<Double>();
        for (Element elAxis : getElementsByTagName(elPolygonGate, "Axis"))
        {
            String axis = elAxis.getAttribute("dimension");
            String axisName = ___cleanName(elAxis.getAttribute("name"));
            if ("x".equals(axis))
            {
                xAxis = axisName;
            }
            else
            {
                yAxis = axisName;
            }
        }
        for (Element elPoint : getElementsByTagName(elPolygonGate, "Point"))
        {
            double x = parseParamValue(xAxis, elPoint, "x");
            double y = parseParamValue(yAxis, elPoint, "y");
            lstX.add(x);
            lstY.add(y);
        }
        scaleValues(xAxis, lstX);
        scaleValues(yAxis, lstY);
        double[] X = toDoubleArray(lstX);
        double[] Y = toDoubleArray(lstY);
        Polygon poly = new Polygon(X, Y);
        return new PolygonGate(xAxis, yAxis, poly);
    }

    protected IntervalGate readRangeGate(Element elRangeGate)
    {
        Element elAxis = getElementByTagName(elRangeGate, "Axis");
        String axisName = ___cleanName(elAxis.getAttribute("name"));
        // UNDONE: support open ranges
        double min = parseParamValue(axisName, elRangeGate, "min");
        double max = parseParamValue(axisName, elRangeGate, "max");

        return new IntervalGate(axisName, min, max);
    }

    protected PolygonGate readRectangleGate(Element elRectangleGate)
    {
        List<String> axes = new ArrayList<String>();
        List<Double> lstMin = new ArrayList<Double>();
        List<Double> lstMax = new ArrayList<Double>();
        for (Element elRangeGate : getElementsByTagName(elRectangleGate, "RangeGate"))
        {
            IntervalGate rangeGate = readRangeGate(elRangeGate);
            axes.add(rangeGate.getXAxis());
            lstMin.add(rangeGate.getMin());
            lstMax.add(rangeGate.getMax());
        }
        double[] X = new double[] { lstMin.get(0).doubleValue(), lstMin.get(0).doubleValue(), lstMax.get(0).doubleValue(), lstMax.get(0).doubleValue() };
        double[] Y = new double[] { lstMin.get(1).doubleValue(), lstMax.get(1).doubleValue(), lstMax.get(1).doubleValue(), lstMin.get(1).doubleValue() };
        return new PolygonGate(axes.get(0), axes.get(1), new Polygon(X, Y));
    }

    protected Population readBoolean(Element elBoolNode, SubsetSpec parentSubset)
    {
        Population ret = new Population();
        PopulationName name = PopulationName.fromString(elBoolNode.getAttribute("name"));
        ret.setName(name);

        List<Gate> gates = new ArrayList<Gate>();
        Element elDependents = getElementByTagName(elBoolNode, "Dependents");
        if (elDependents != null)
        {
            for (Element elDependent : getElementsByTagName(elDependents, "Dependent"))
            {
                // NOTE: we only support subset refs relative to the current parent.
                String dependentName = StringUtils.trimToNull(elDependent.getAttribute("name"));
                if (dependentName != null)
                    gates.add(new SubsetRef(new SubsetSpec(parentSubset, PopulationName.fromString(dependentName))));
            }
        }

        if (gates.size() > 0)
        {
            Gate gate = null;
            String tagName = elBoolNode.getTagName();
            if ("AndNode".equals(tagName))
                gate = new AndGate(gates);
            else if ("OrNode".equals(tagName))
                gate = new OrGate(gates);
            else if ("NotNode".equals(tagName))
                gate = new NotGate(gates.get(0));

            if (gate != null)
                ret.addGate(gate);
        }
        else
        {
            warning(name, parentSubset, "No dependent gates found for boolean gate");
        }

        return ret;
    }

    protected void readGates(Element elPopulation, SubsetSpec parentSubset, Population ret, Analysis analysis)
    {
        for (Element elPolygonGate : getElementsByTagName(elPopulation, "PolygonGate"))
        {
            boolean invert = inverted(elPolygonGate);
            PolygonGate gate = readPolygonGate(elPolygonGate);
            ret.addGate(invert ? new NotGate(gate) : gate);
            analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis(), gate.getYAxis()));
        }
        for (Element elRectangleGate : getElementsByTagName(elPopulation, "RectangleGate"))
        {
            boolean invert = inverted(elRectangleGate);
            PolygonGate gate = readRectangleGate(elRectangleGate);
            ret.addGate(invert ? new NotGate(gate) : gate);
            analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis(), gate.getYAxis()));
        }
        for (Element elRangeGate : getElementsByTagName(elPopulation, "RangeGate"))
        {
            boolean invert = inverted(elRangeGate);
            IntervalGate gate = readRangeGate(elRangeGate);
            ret.addGate(invert ? new NotGate(gate) : gate);
            analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis()));
        }
        for (Element elEllipseGate : getElementsByTagName(elPopulation, "EllipseGate"))
        {
            //boolean invert = inverted(elEllipseGate);
            //PolygonGate gate = readRectangleGate(elEllipseGate);
            //ret.addGate(invert ? new NotGate(gate) : gate);
            //analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis()));
        }
    }

    protected boolean inverted(Element elGate)
    {
        return "1".equals(elGate.getAttribute("negated")) || "0".equals(elGate.getAttribute("eventsInside"));
    }

    protected Population readPopulation(Element elPopulation, SubsetSpec parentSubset, Analysis analysis, @Nullable AttributeSet results, boolean warnOnMissingStats)
    {
        Population ret = new Population();
        PopulationName name = PopulationName.fromString(elPopulation.getAttribute("name"));
        ret.setName(name);
        SubsetSpec subset = new SubsetSpec(parentSubset, name);

        readGates(elPopulation, parentSubset, ret, analysis);

        readStats(subset, elPopulation, results, analysis, warnOnMissingStats);

        for (Element elSubpopulations : getElementsByTagName(elPopulation, "Subpopulations"))
        {
            List<Population> subpops = readSubpopulations(elSubpopulations, subset, analysis, results, warnOnMissingStats);
            for (Population pop : subpops)
                ret.addPopulation(pop);
        }

        return ret;
    }

    protected List<Population> readSubpopulations(Element elSubpopulations, SubsetSpec parentSubset, Analysis analysis, @Nullable AttributeSet results, boolean warnOnMissingStats)
    {
        List<Population> subpops = new LinkedList<Population>();
        for (Element elPopulation : getElements(elSubpopulations))
        {
            String tagName = elPopulation.getTagName();
            if ("Population".equals(tagName))
                subpops.add(readPopulation(elPopulation, parentSubset, analysis, results, warnOnMissingStats));
            else if ("AndNode".equals(tagName) || "OrNode".equals(tagName) || "NotNode".equals(tagName))
                subpops.add(readBoolean(elPopulation, parentSubset));
        }
        return subpops;
    }

    protected Analysis readAnalysis(Element elAnalysis, @Nullable AttributeSet results, boolean warnOnMissingStats)
    {
        Analysis ret = new Analysis();
        PopulationName name = PopulationName.fromString(elAnalysis.getAttribute("name"));
        ret.setName(name);
        ret.setSettings(_settings);
        ret.getStatistics().add(new StatisticSpec(null, StatisticSpec.STAT.Count, null));

        readStats(null, elAnalysis, results, ret, warnOnMissingStats);

        for (Element elSubpopulations : getElementsByTagName(elAnalysis, "Subpopulations"))
        {
            List<Population> subpops = readSubpopulations(elSubpopulations, null, ret, results, warnOnMissingStats);
            for (Population pop : subpops)
                ret.addPopulation(pop);
        }

        if (results != null)
        {
            for (StatisticSpec stat : results.getStatistics().keySet())
            {
                ret.addStatistic(stat);
            }
        }

        return ret;
    }

    protected Analysis readSampleAnalysis(Element elSampleNode)
    {
        AttributeSet results = new AttributeSet(ObjectType.fcsAnalysis, null);
        Analysis ret = readAnalysis(elSampleNode, results, true);
        String sampleId = elSampleNode.getAttribute("sampleID");
        _sampleAnalyses.put(sampleId, ret);
        addSampleAnalysisResults(results, sampleId);
        return ret;
    }

    protected void readSamples(Element elDoc)
    {
        for (Element elSampleList : getElementsByTagName(elDoc, "SampleList"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "Sample"))
            {
                readSample(elSample);
            }
        }
    }

    protected GroupInfo readGroup(Element elGroupNode)
    {
        GroupInfo ret = new GroupInfo();
        ret._groupId = elGroupNode.getAttribute("name");
        ret._groupName = PopulationName.fromString(elGroupNode.getAttribute("name"));

        for (Element elSampleList : getElementsByTagName(elGroupNode, "SampleRefs"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "SampleRef"))
            {
                String sampleID = elSample.getAttribute("sampleID");
                if (sampleID != null)
                    ret._sampleIds.add(sampleID);
            }
        }

        _groupInfos.put(ret._groupId, ret);
        return ret;
    }

    protected Analysis readGroupAnalysis(Element elGroup)
    {
        Analysis analysis = readAnalysis(elGroup, null, false);
        _groupAnalyses.put(analysis.getName(), analysis);

        return analysis;
    }

    protected void readGroups(Element elDoc)
    {
        for (Element elGroups : getElementsByTagName(elDoc, "Groups"))
        {
            for (Element elGroupNode : getElementsByTagName(elGroups, "GroupNode"))
            {
                for (Element elGroup : getElementsByTagName(elGroupNode, "Group"))
                {
                    readGroup(elGroup);
                }

                readGroupAnalysis(elGroupNode);
            }
        }
    }

    protected void readCompensationMatrices(Element elDoc)
    {
        // UNDONE
    }
}
