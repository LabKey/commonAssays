package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class PCWorkspace extends FlowJoWorkspace
{
    public PCWorkspace(Element elDoc, Set<StatisticSet> stats)
    {
        super(stats);
        for (Element elSampleList : getElementsByTagName(elDoc, "SampleList"))
        {
            readSamples(elSampleList);
        }
        for (Element elGroups : getElementsByTagName(elDoc, "Groups"))
        {
            readGroups(elGroups);
        }
    }

    protected void readSample(Element elSample)
    {
        SampleInfo sampleInfo = new SampleInfo();
        for (Element elKeywords : getElementsByTagName(elSample, "Keywords"))
        {
            for (Element elKeyword : getElementsByTagName(elKeywords, "Keyword"))
            {
                sampleInfo._keywords.put(elKeyword.getAttribute("name"), elKeyword.getAttribute("value"));
            }
        }
        for (int i = 1; i < 100; i ++)
        {
            String paramName = sampleInfo._keywords.get("$P" + i + "N");
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
                double range = Double.valueOf(paramRange);
                if (range > 4096)
                {
                    paramInfo.multiplier = range/4096;
                }
            }
            _parameters.put(paramInfo.name, paramInfo);
        }
        for (Element elSampleNode : getElementsByTagName(elSample, "SampleNode"))
        {
            sampleInfo._sampleId = elSampleNode.getAttribute("sampleID");
            readSampleAnalysis(elSampleNode);
        }
        _sampleInfos.put(sampleInfo.getSampleId(), sampleInfo);
    }

    protected PolygonGate readPolygonGate(Element elPolygonGate)
    {
        String xAxis = null;
        String yAxis = null;
        List<Double> lstX = new ArrayList();
        List<Double> lstY = new ArrayList();
        for (Element elAxis : getElementsByTagName(elPolygonGate, "Axis"))
        {
            String axis = elAxis.getAttribute("dimension");
            String axisName = elAxis.getAttribute("name");
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
        double[] X = toDoubleArray(lstX);
        double[] Y = toDoubleArray(lstY);
        Polygon poly = new Polygon(X, Y);
        return new PolygonGate(xAxis, yAxis, poly);
    }

    protected Population readPopulation(Element elPopulation)
    {
        Population ret = new Population();
        ret.setName(elPopulation.getAttribute("name"));
        for (Element elPolygonGate : getElementsByTagName(elPopulation, "PolygonGate"))
        {
            ret.addGate(readPolygonGate(elPolygonGate));
        }
        for (Element elRectangleGate : getElementsByTagName(elPopulation, "RectangleGate"))
        {
            ret.addGate(readRectangleGate(elRectangleGate));
        }
        for (Element elSubpopulations : getElementsByTagName(elPopulation, "Subpopulations"))
        {
            readSubpopulations(ret, elSubpopulations);
        }
        return ret;
    }

    protected PolygonGate readRectangleGate(Element elRectangleGate)
    {
        List<String> axes = new ArrayList();
        List<Double> lstMin = new ArrayList();
        List<Double> lstMax = new ArrayList();
        for (Element elRangeGate : getElementsByTagName(elRectangleGate, "RangeGate"))
        {
            Element elAxis = getElementByTagName(elRangeGate, "Axis");
            String axisName = elAxis.getAttribute("name");
            axes.add(axisName);
            lstMin.add(parseParamValue(axisName, elRangeGate, "min"));
            lstMax.add(parseParamValue(axisName, elRangeGate, "max"));
        }
        double[] X = new double[] { lstMin.get(0), lstMin.get(0), lstMax.get(0), lstMax.get(0) };
        double[] Y = new double[] { lstMin.get(1), lstMax.get(1), lstMax.get(1), lstMin.get(1) };
        return new PolygonGate(axes.get(0), axes.get(1), new Polygon(X, Y));
    }

    protected void readSubpopulations(PopulationSet pops, Element elSubpopulations)
    {
        for (Element elPopulation : getElementsByTagName(elSubpopulations, "Population"))
        {
            pops.addPopulation(readPopulation(elPopulation));
        }
    }

    protected Analysis readSampleAnalysis(Element elSampleNode)
    {
        Analysis ret = new Analysis();
        ret.setName(elSampleNode.getAttribute("name"));
        _sampleAnalyses.put(elSampleNode.getAttribute("sampleID"), ret);

        for (Element elSubpopulations : getElementsByTagName(elSampleNode, "Subpopulations"))
        {
            readSubpopulations(ret, elSubpopulations);
        }
        return ret;
    }

    protected void readSamples(Element elSampleList)
    {
        for (Element elSample : getElementsByTagName(elSampleList, "Sample"))
        {
            readSample(elSample);
        }
    }

    protected void readGroups(Element elGroups)
    {
        for (Element elGroup: getElementsByTagName(elGroups, "GroupNode"))
        {
            Analysis analysis = new Analysis();
            analysis.setName(elGroup.getAttribute("name"));
            for (Element elSubpopulations : getElementsByTagName(elGroup, "Subpopulations"))
            {
                readSubpopulations(analysis, elSubpopulations);
            }
            _groupAnalyses.put(analysis.getName(), analysis);
        }
    }
}
