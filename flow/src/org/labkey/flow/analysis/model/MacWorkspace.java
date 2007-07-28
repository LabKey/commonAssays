package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.net.URI;

import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;

public class MacWorkspace extends FlowJoWorkspace
{
    static Map<String, StatisticSpec.STAT> statMap = new HashMap();
    static
    {
        statMap.put("Count", StatisticSpec.STAT.Count);
        statMap.put("Percentile", StatisticSpec.STAT.Percentile);
        statMap.put("Mean", StatisticSpec.STAT.Mean);
        statMap.put("Median", StatisticSpec.STAT.Median);
        statMap.put("FrequencyOfGrandParent", StatisticSpec.STAT.Freq_Of_Grandparent);
        statMap.put("FrequencyOfParent", StatisticSpec.STAT.Freq_Of_Parent);
        statMap.put("FrequencyOfTotal", StatisticSpec.STAT.Frequency);
    }


    protected void readSamples(Element elDoc)
    {
        for (Element elSamples : getElementsByTagName(elDoc, "Samples"))
        {
            for (Element elSample : getElementsByTagName(elSamples, "Sample"))
            {
                readSample(elSample);
            }
        }
    }

    public MacWorkspace(Element elDoc) throws Exception
    {
        readSamples(elDoc);
        readCompensationMatrices(elDoc);
        readSampleAnalyses(elDoc);
        readGroupAnalyses(elDoc);
    }



    protected void readSampleAnalyses(Element elDoc)
    {
        for (Element elSampleAnalyses : getElementsByTagName(elDoc, "SampleAnalyses"))
        {
            for (Element elSampleAnalysis : getElementsByTagName(elSampleAnalyses, "Sample"))
            {
                readSampleAnalysis(elSampleAnalysis);
            }
        }
    }

    public void readCompensationMatrices(Element elDoc)
    {
        for (Element elCompensationMatrices : getElementsByTagName(elDoc, "CompensationMatrices"))
        {
            for (Element elCompensationMatrix : getElementsByTagName(elCompensationMatrices, "CompensationMatrix"))
            {
                _compensationMatrices.add(new CompensationMatrix(elCompensationMatrix));
            }
        }
    }

    protected Analysis readSampleAnalysis(Element elSampleAnalysis)
    {
        AttributeSet results = new AttributeSet(ObjectType.fcsAnalysis, null);
        Analysis ret = readAnalysis(elSampleAnalysis, results);
        ret.setName(elSampleAnalysis.getAttribute("name"));
        String sampleId = elSampleAnalysis.getAttribute("sampleID");
        _sampleAnalyses.put(sampleId, ret);
        if (results.getStatistics().size() > 0)
        {
            _sampleAnalysisResults.put(sampleId, results);
        }
        return ret;
    }

    protected String toBooleanExpression(Element elPopulation)
    {
        List<Element> booleanGates = getElementsByTagName(elPopulation, "BooleanGate");
        if (booleanGates.size() != 1)
            return null;
        Element elBooleanGate = booleanGates.get(0);
        List<String> gatePaths = new ArrayList();
        String specification = elBooleanGate.getAttribute("specification");
        Element elGatePaths = getElementsByTagName(elBooleanGate, "GatePaths").get(0);
        Element elStringArray = getElementsByTagName(elGatePaths, "StringArray").get(0);
        for (Element elString : getElementsByTagName(elStringArray, "String"))
        {
            String string = cleanName(getTextValue(elString));
            gatePaths.add(string);
        }
        StringTokenizer st = new StringTokenizer(specification, "&|", true);
        List<String> gateCodes = new ArrayList();
        List<String> operators = new ArrayList();
        while(st.hasMoreTokens())
        {
            gateCodes.add(StringUtils.trim(st.nextToken()));
            if (st.hasMoreTokens())
            {
                operators.add(st.nextToken());
            }
        }
        if (gateCodes.size() != gatePaths.size())
        {
            // Error ?
            return null;
        }

        StringBuilder strExpr = new StringBuilder();
        String strAnd = "";


        for (int i = 0; i < gatePaths.size(); i ++)
        {
            String gateName = gatePaths.get(i);
            SubsetSpec gateSubset = SubsetSpec.fromString(gateName);
            String lastSubset = gateSubset.getSubset();
            String strGate = lastSubset;
            if (gateCodes.get(i).startsWith("!"))
            {
                strGate = "!" + lastSubset;
            }
            strExpr.append(strAnd);
            if (i < operators.size())
            {
                strAnd = operators.get(i);
            }
            strExpr.append(strGate);
        }
        return strExpr.toString();
    }

    protected void readStats(SubsetSpec subset, Element elPopulation, AttributeSet results, Analysis analysis)
    {
        String strCount = elPopulation.getAttribute("count");
        if (!StringUtils.isEmpty(strCount))
        {
            StatisticSpec statCount = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
            results.setStatistic(statCount, Double.valueOf(strCount));
        }
        for (Element elStat : getElementsByTagName(elPopulation, "Statistic"))
        {
            String statistic = elStat.getAttribute("statistic");
            StatisticSpec.STAT stat = statMap.get(statistic);
            if (stat == null)
                continue;
            String parameter = StringUtils.trimToNull(elStat.getAttribute("parameter"));
            if (parameter != null)
            {
                parameter = cleanName(parameter);
            }
            String percentile = StringUtils.trimToNull(elStat.getAttribute("statisticVariable"));
            String strValue = elStat.getAttribute("value");
            if (strValue == null)
            {
                continue;
            }
            double value;
            try
            {
                value = Double.valueOf(strValue);
            }
            catch (NumberFormatException nfe)
            {
                continue;
            }
            StatisticSpec spec;
            if (percentile == null)
            {
                spec = new StatisticSpec(subset, stat, parameter);
            }
            else
            {
                spec = new StatisticSpec(subset, stat, parameter + ":" + percentile);
            }
            results.setStatistic(spec, value);
            analysis.addStatistic(spec);
        }
    }

    protected Population readPopulation(Element elPopulation, SubsetSpec parentSubset, Analysis analysis, AttributeSet results)
    {
        String booleanExpr = toBooleanExpression(elPopulation);
        if (booleanExpr != null)
        {
            SubsetSpec subset = new SubsetSpec(parentSubset, "(" + booleanExpr + ")");
            analysis.addSubset(subset);
            readStats(subset, elPopulation, results, analysis);
            return null;
        }

        Population ret = new Population();
        ret.setName(cleanName(elPopulation.getAttribute("name")));
        SubsetSpec subset = new SubsetSpec(parentSubset, ret.getName());
        Set<String> gatedParams = new LinkedHashSet();

        for (Element elPolygonGate : getElementsByTagName(elPopulation, "PolygonGate"))
        {
            NodeList nl = elPolygonGate.getChildNodes();
            for (int iNode = 0; iNode < nl.getLength(); iNode ++)
            {
                Node node = nl.item(iNode);
                if (!(node instanceof Element))
                    continue;
                Element el = (Element) node;
                if ("Polygon".equals(el.getTagName()) || "PolyRect".equals(el.getTagName()))
                {
                    String xAxis = cleanName(el.getAttribute("xAxisName"));
                    String yAxis = cleanName(el.getAttribute("yAxisName"));
                    gatedParams.add(xAxis);
                    gatedParams.add(yAxis);

                    List<Double> lstX = new ArrayList();
                    List<Double> lstY = new ArrayList();
                    for (Element elPolygon : getElementsByTagName(el, "Polygon"))
                    {
                        for (Element elVertex : getElementsByTagName(elPolygon, "Vertex"))
                        {
                            lstX.add(parseParamValue(xAxis, elVertex, "x"));
                            lstY.add(parseParamValue(yAxis, elVertex, "y"));
                        }
                    }
                    scaleValues(xAxis, lstX);
                    scaleValues(yAxis, lstY);
                    double[] X = toDoubleArray(lstX);
                    double[] Y = toDoubleArray(lstY);
                    PolygonGate gate = new PolygonGate(xAxis, yAxis, new Polygon(X, Y));
                    ret.addGate(gate);
                }
                else if ("Range".equals(el.getTagName()))
                {
                    String axis = cleanName(el.getAttribute("xAxisName"));
                    gatedParams.add(axis);
                    List<Double> lstValues = new ArrayList();
                    for (Element elPolygon : getElementsByTagName(el, "Polygon"))
                    {
                        for (Element elVertex : getElementsByTagName(elPolygon, "Vertex"))
                        {
                            lstValues.add(parseParamValue(axis, elVertex, "x"));
                        }
                    }
                    scaleValues(axis, lstValues);
                    IntervalGate gate = new IntervalGate(axis, lstValues.get(0), lstValues.get(1));
                    ret.addGate(gate);
                }
            }
        }

        readStats(subset, elPopulation, results, analysis);

        for (Element elChild: getElementsByTagName(elPopulation, "Population"))
        {
            Population child = readPopulation(elChild, subset, analysis, results);
            if (child != null)
            {
                ret.addPopulation(child);
            }
        }
        return ret;
    }


    protected Analysis readAnalysis(Element elAnalysis, AttributeSet results)
    {
        Analysis ret = new Analysis();
        ret.setSettings(_settings);
        ret.getStatistics().add(new StatisticSpec(null, StatisticSpec.STAT.Count, null));
        for (Element elPopulation : getElementsByTagName(elAnalysis, "Population"))
        {
            Population child = readPopulation(elPopulation, null, ret, results);
            ret.addPopulation(child);
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

    protected Analysis readGroupAnalysis(Element elGroupAnalysis)
    {
        Analysis ret = readAnalysis(elGroupAnalysis, null);
        ret.setName(elGroupAnalysis.getAttribute("name"));
        _groupAnalyses.put(ret.getName(), ret);
        return ret;
    }

    protected void readGroupAnalyses(Element elDoc)
    {
        for (Element elGroupAnalyses : getElementsByTagName(elDoc, "GroupAnalyses"))
        {
            for (Element elGroupAnalysis : getElementsByTagName(elGroupAnalyses, "Group"))
            {
                readGroupAnalysis(elGroupAnalysis);
            }
        }
    }

    protected void readKeywords(SampleInfo sample, Element el)
    {
        for (Element elKeyword : getElementsByTagName(el, "Keyword"))
        {
            sample._keywords.put(elKeyword.getAttribute("name"), elKeyword.getAttribute("value"));
        }
    }

    protected void readParameterInfo(Element el)
    {
        for (Element elParameter : getElementsByTagName(el, "Parameter"))
        {
            String name = elParameter.getAttribute("name");
            if (!_parameters.containsKey(name))
            {
                ParameterInfo pi = new ParameterInfo();
                pi.name = name;
                pi.multiplier = findMultiplier(elParameter);
                _parameters.put(name, pi);
            }
            String lowValue = elParameter.getAttribute("lowValue");
            if (lowValue != null)
            {
                _settings.getParameterInfo(name, true).setMinValue(Double.valueOf(lowValue));
            }
        }
    }

    protected SampleInfo readSample(Element elSample)
    {
        SampleInfo ret = new SampleInfo();
        ret._sampleId = elSample.getAttribute("sampleID");
        if (elSample.hasAttribute("compensationID"))
        {
            ret._compensationId = elSample.getAttribute("compensationID");
        }
        for (Element elFCSHeader : getElementsByTagName(elSample, "FCSHeader"))
        {
            readKeywords(ret, elFCSHeader);
        }
        readParameterInfo(elSample);
        _sampleInfos.put(ret._sampleId, ret);
        return ret;
    }

}
