package org.labkey.flow.analysis.model;

import org.apache.commons.lang.StringUtils;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class MacWorkspace extends FlowJoWorkspace
{
    static Map<String, StatisticSpec.STAT> statMap = new HashMap<String, StatisticSpec.STAT>();
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
        readCompensationMatrices(elDoc);
        readAutoCompensationScripts(elDoc);
        readCalibrationTables(elDoc);
        readSamples(elDoc);
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

    public void readAutoCompensationScripts(Element elDoc)
    {
        for (Element elAutoCompScripts : getElementsByTagName(elDoc, "AutoCompensationScripts"))
        {
            for (Element elAutoCompScript : getElementsByTagName(elAutoCompScripts, "Script"))
            {
                AutoCompensationScript script = AutoCompensationScript.readAutoComp(elAutoCompScript);
                if (script != null)
                    _autoCompensationScripts.add(script);
            }
        }
    }

    public void readCalibrationTables(Element elDoc)
    {
        for (Element elCalibrationTables : getElementsByTagName(elDoc, "CalibrationTables"))
        {
            for (Element elCalibrationTable : getElementsByTagName(elCalibrationTables, "Table"))
            {
                _calibrationTables.add(FixedCalibrationTable.fromString(getInnerText(elCalibrationTable)));
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
            StatisticSpec count = new StatisticSpec(null, StatisticSpec.STAT.Count, null);
            // If the statistic "count" is unavailable, try to get it from the '$TOT" keyword.
            if (!results.getStatistics().containsKey(count))
            {
                SampleInfo sampleInfo = _sampleInfos.get(sampleId);
                if (sampleInfo != null)
                {
                    String strTot = sampleInfo.getKeywords().get("$TOT");
                    if (strTot != null)
                    {
                        results.setStatistic(count, Double.valueOf(strTot).doubleValue());
                    }
                }
            }
            // Fill in the Freq Of Parents that can be determined from the existing stats
            for (Map.Entry<StatisticSpec, Double> entry : results.getStatistics().entrySet())
            {
                if (entry.getKey().getStatistic() != StatisticSpec.STAT.Count)
                {
                    continue;
                }
                if (entry.getKey().getSubset() == null)
                {
                    continue;
                }
                StatisticSpec freqStat = new StatisticSpec(entry.getKey().getSubset(), StatisticSpec.STAT.Freq_Of_Parent, null);
                if (results.getStatistics().containsKey(freqStat))
                {
                    continue;
                }
                Double denominator = results.getStatistics().get(new StatisticSpec(entry.getKey().getSubset().getParent(), StatisticSpec.STAT.Count, null));
                if (denominator == null)
                {
                    continue;
                }
                if (entry.getValue().equals(0.0))
                {
                    results.setStatistic(freqStat, 0.0);
                }
                else if (!denominator.equals(0.0))
                {
                    results.setStatistic(freqStat, entry.getValue().doubleValue() / denominator.doubleValue() * 100);
                }
            }
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
        List<String> gatePaths = new ArrayList<String>();
        String specification = elBooleanGate.getAttribute("specification");
        Element elGatePaths = getElementsByTagName(elBooleanGate, "GatePaths").get(0);
        Element elStringArray = getElementsByTagName(elGatePaths, "StringArray").get(0);
        for (Element elString : getElementsByTagName(elStringArray, "String"))
        {
            String string = cleanName(getTextValue(elString));
            gatePaths.add(string);
        }
        StringTokenizer st = new StringTokenizer(specification, "&|", true);
        List<String> gateCodes = new ArrayList<String>();
        List<String> operators = new ArrayList<String>();
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
            results.setStatistic(statCount, Double.valueOf(strCount).doubleValue());
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
                value = Double.valueOf(strValue).doubleValue();
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

    protected PolygonGate readPolygon(Element el)
    {
        String xAxis = cleanName(el.getAttribute("xAxisName"));
        String yAxis = cleanName(el.getAttribute("yAxisName"));

        List<Double> lstX = new ArrayList<Double>();
        List<Double> lstY = new ArrayList<Double>();
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
        gate = interpolatePolygon(gate);
        return gate;
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
        Set<String> gatedParams = new LinkedHashSet<String>();

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
                    PolygonGate gate = readPolygon(el);
                    ret.addGate(gate);
                    analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis(), gate.getYAxis()));
                }
                else if ("Range".equals(el.getTagName()))
                {
                    String axis = cleanName(el.getAttribute("xAxisName"));
                    gatedParams.add(axis);
                    List<Double> lstValues = new ArrayList<Double>();
                    for (Element elPolygon : getElementsByTagName(el, "Polygon"))
                    {
                        for (Element elVertex : getElementsByTagName(elPolygon, "Vertex"))
                        {
                            lstValues.add(parseParamValue(axis, elVertex, "x"));
                        }
                    }
                    scaleValues(axis, lstValues);
                    IntervalGate gate = new IntervalGate(axis, lstValues.get(0).doubleValue(), lstValues.get(1).doubleValue());
                    ret.addGate(gate);
                    analysis.addGraph(new GraphSpec(parentSubset, gate.getXAxis()));
                }
                else if ("Ellipse".equals(el.getTagName()))
                {
                    PolygonGate polygon = readPolygon(el);
                    EllipseGate.Point[] vertices = new EllipseGate.Point[4];
                    for (int i = 0; i < vertices.length; i ++)
                    {
                        vertices[i] = new EllipseGate.Point(polygon.getPolygon().X[i], polygon.getPolygon().Y[i]);
                    }
                    EllipseGate gate = EllipseGate.fromVertices(polygon.getXAxis(), polygon.getYAxis(), vertices);
                    ret.addGate(gate);
                    analysis.addGraph(new GraphSpec(parentSubset, polygon.getXAxis(), polygon.getYAxis()));
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
                String calibrationIndex = elParameter.getAttribute("calibrationIndex");
                if (!StringUtils.isEmpty(calibrationIndex))
                {
                    int index = Integer.valueOf(calibrationIndex).intValue();
                    if (index > 0 && index <= _calibrationTables.size())
                    {
                        pi.calibrationTable = _calibrationTables.get(index - 1);
                    }
                }
                else
                {

                    pi.calibrationTable = new IdentityCalibrationTable(getRange(elParameter));
                }
                _parameters.put(name, pi);
            }
            String lowValue = elParameter.getAttribute("lowValue");
            if (lowValue != null)
            {
                _settings.getParameterInfo(name, true).setMinValue(Double.valueOf(lowValue).doubleValue());
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
