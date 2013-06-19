/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.flow.script.xml.AnalysisDef;
import org.fhcrc.cpas.flow.script.xml.ChannelDef;
import org.fhcrc.cpas.flow.script.xml.ChannelSubsetDef;
import org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef;
import org.fhcrc.cpas.flow.script.xml.CriteriaDef;
import org.fhcrc.cpas.flow.script.xml.EllipseDef;
import org.fhcrc.cpas.flow.script.xml.GateDef;
import org.fhcrc.cpas.flow.script.xml.GateListDef;
import org.fhcrc.cpas.flow.script.xml.GraphDef;
import org.fhcrc.cpas.flow.script.xml.IntervalDef;
import org.fhcrc.cpas.flow.script.xml.PointDef;
import org.fhcrc.cpas.flow.script.xml.PolygonDef;
import org.fhcrc.cpas.flow.script.xml.PopulationDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.fhcrc.cpas.flow.script.xml.StatisticDef;
import org.fhcrc.cpas.flow.script.xml.SubsetDef;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.AndGate;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.model.EllipseGate;
import org.labkey.flow.analysis.model.Gate;
import org.labkey.flow.analysis.model.GateList;
import org.labkey.flow.analysis.model.IntervalGate;
import org.labkey.flow.analysis.model.NotGate;
import org.labkey.flow.analysis.model.OrGate;
import org.labkey.flow.analysis.model.Polygon;
import org.labkey.flow.analysis.model.PolygonGate;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SubsetRef;
import org.labkey.flow.analysis.model.SampleCriteria;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.model.Workspace;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: kevink
 * Date: Apr 17, 2011
 *
 * Split from FlowAnalyzer.
 */
public class ScriptAnalyzer
{
    static public ScriptDocument parseScriptDocument(String script) throws XmlException
    {
        return ScriptDocument.Factory.parse(script);
    }

    static public ScriptDef parseScript(String script) throws XmlException
    {
        ScriptDef scriptElement = parseScriptDocument(script).getScript();
        return scriptElement;
    }

    static public Map<String, Gate> getGates(Node node)
    {
        NodeList children = node.getChildNodes();
        HashMap gates = new HashMap();
        for (int i = 0; i < children.getLength(); i ++)
        {
            Node child = children.item(i);
            if ("gate".equals(child.getNodeName()))
            {
                Gate gate = Gate.readGate((Element) child);
                gates.put(gate.getName(), gate);
            }
        }
        return gates;
    }

    static public Map<String, Gate> getParentGates(Node node)
    {
        Element parent = (Element) node.getParentNode();
        return getGates(parent);
    }

    public static Population makePopulation(Map<String, Gate> gates, PopulationDef populationElement)
    {
        assert !SubsetSpec.___isExpression(populationElement.getName());
        Population ret = new Population();
        PopulationName name = PopulationName.fromString(populationElement.getName());
        ret.setName(name);
        GateDef gateElement = populationElement.getGate();
        if (gateElement != null)
        {
            ret.addGate(Gate.readGate((Element) gateElement.getDomNode()));
            if (ret.getGates().size() == 1)
            {
                ret.getGates().get(0).setName(ret.getName());
            }
        }
        else
        {
            ret.addGate(gates.get(populationElement.getName()));
        }
        for (PopulationDef child : populationElement.getPopulationArray())
        {
            ret.addPopulation(makePopulation(gates, child));
        }

        return ret;
    }

    static public StatisticSpec makeStatisticSpec(StatisticDef statElement)
    {
        return new StatisticSpec(SubsetSpec.fromEscapedString(statElement.getSubset()), StatisticSpec.STAT.valueOf(statElement.getName()), statElement.getParameter());
    }

    static public GraphSpec makeGraphSpec(GraphDef graphElement)
    {
        SubsetSpec subset = SubsetSpec.fromEscapedString(graphElement.getSubset());
        if (graphElement.getYAxis() != null)
            return new GraphSpec(subset, graphElement.getXAxis(), graphElement.getYAxis());
        else
            return new GraphSpec(subset, graphElement.getXAxis());
    }

    static public Analysis makeAnalysis(SettingsDef settingsElement, AnalysisDef analysisElement)
    {
        Analysis ret = new Analysis();
        ret.setSettings(ScriptSettings.fromSettingsDef(settingsElement));
        if (analysisElement == null)
            return ret;
        Map<String, Gate> gates = getParentGates(analysisElement.getDomNode());
        for (PopulationDef child : analysisElement.getPopulationArray())
        {
            ret.addPopulation(makePopulation(gates, child));
        }
        for (SubsetDef subset : analysisElement.getSubsetArray())
        {
            ret.addSubset(SubsetSpec.fromEscapedString(subset.getSubset()));
        }
        for (StatisticDef statElement : analysisElement.getStatisticArray())
        {
            ret.addStatistic(makeStatisticSpec(statElement));
        }
        for (GraphDef graphElement : analysisElement.getGraphArray())
        {
            ret.addGraph(makeGraphSpec(graphElement));
        }
        return ret;
    }

    static public StatisticDef addStatistic(AnalysisDef analysis, StatisticSpec stat)
    {
        StatisticDef statElement = analysis.addNewStatistic();
        statElement.setName(stat.getStatistic().toString());
        if (stat.getSubset() != null)
            statElement.setSubset(stat.getSubset().toString());
        if (stat.getParameter() != null)
            statElement.setParameter(stat.getParameter());
        return statElement;
    }

    static public GraphDef addGraph(AnalysisDef analysis, GraphSpec graph)
    {
        GraphDef graphElement = analysis.addNewGraph();
        if (graph.getSubset() != null)
            graphElement.setSubset(graph.getSubset().toString());
        graphElement.setXAxis(graph.getParameters()[0]);
        if (graph.getParameters().length > 1)
        {
            graphElement.setYAxis(graph.getParameters()[1]);
        }
        return graphElement;
    }

    public static CompensationCalculation.ChannelSubset readSubsetDef(ChannelSubsetDef subsetDef)
    {
        SampleCriteria criteria = SampleCriteria.readChildCriteria((Element) subsetDef.getDomNode());
        SubsetSpec subset = null;
        if (subsetDef.getSubset() != null)
        {
            subset = SubsetSpec.fromEscapedString(subsetDef.getSubset());
        }
        return new CompensationCalculation.ChannelSubset(criteria, subset);
    }

    public static void fillSubsetDef(ChannelSubsetDef subsetDef, CompensationCalculation.ChannelSubset subset)
    {
        if (subset.getSubset() != null)
        {
            subsetDef.setSubset(subset.getSubset().toString());
        }
        CriteriaDef criteriaDef = subsetDef.addNewCriteria();
        criteriaDef.setKeyword(subset.getCriteria().getKeyword());
        criteriaDef.setPattern(subset.getCriteria().getPattern());
    }

    static public CompensationCalculation makeCompensationCalculation(SettingsDef settingsElement, CompensationCalculationDef compensationCalculationElement)
    {
        CompensationCalculation ret = new CompensationCalculation();
        ret.setSettings(ScriptSettings.fromSettingsDef(settingsElement));
        if (compensationCalculationElement == null)
            return ret;
        Map<String, Gate> gates = getParentGates(compensationCalculationElement.getDomNode());
        for (PopulationDef child : compensationCalculationElement.getPopulationArray())
        {
            ret.addPopulation(makePopulation(gates, child));
        }
        for (ChannelDef channel : compensationCalculationElement.getChannelArray())
        {
            CompensationCalculation.ChannelSubset positive = readSubsetDef(channel.getPositive());
            CompensationCalculation.ChannelSubset negative = readSubsetDef(channel.getNegative());
            ret.addChannel(channel.getName(), positive, negative);
        }
        return ret;
    }

    static public void fillPolygonGate(PolygonDef polygonDef, PolygonGate polygonGate)
    {
        polygonDef.setXAxis(polygonGate.getXAxis());
        polygonDef.setYAxis(polygonGate.getYAxis());
        Polygon polygon = polygonGate.getPolygon();
        for (int i = 0; i < polygon.len; i ++)
        {
            PointDef point = polygonDef.addNewPoint();
            point.setX(polygon.X[i]);
            point.setY(polygon.Y[i]);
        }
    }

    static public void fillIntervalGate(IntervalDef intervalDef, IntervalGate intervalGate)
    {
        intervalDef.setAxis(intervalGate.getXAxis());
        intervalDef.setMin(intervalGate.getMin());
        intervalDef.setMax(intervalGate.getMax());
    }

    static public void fillEllipseGate(EllipseDef ellipseDef, EllipseGate ellipseGate)
    {
        ellipseDef.setXAxis(ellipseGate.getXAxis());
        ellipseDef.setYAxis(ellipseGate.getYAxis());
        ellipseDef.setDistance(ellipseGate.getDistance());
        for (EllipseGate.Point point : ellipseGate.getFoci())
        {
            PointDef pointDef = ellipseDef.addNewFocus();
            pointDef.setX(point.x);
            pointDef.setY(point.y);
        }
    }

    static public void fillPopulationRefGate(SubsetDef subsetDef, SubsetRef populationRefGate)
    {
        assert !populationRefGate.getRef().isExpression();
        subsetDef.setSubset(populationRefGate.getRef().toString());
    }

    static public void fillGateList(GateListDef gateListDef, GateList gateList)
    {
        for (Gate gate : gateList.getGates())
        {
            if (gate instanceof PolygonGate)
            {
                fillPolygonGate(gateListDef.addNewPolygon(), (PolygonGate) gate);
            }
            else if (gate instanceof IntervalGate)
            {
                fillIntervalGate(gateListDef.addNewInterval(), (IntervalGate) gate);
            }
            else if (gate instanceof EllipseGate)
            {
                fillEllipseGate(gateListDef.addNewEllipse(), (EllipseGate) gate);
            }
            else if (gate instanceof NotGate)
            {
                fillGate(gateListDef.addNewNot(), ((NotGate) gate).getGate());
            }
            else if (gate instanceof AndGate)
            {
                fillGateList(gateListDef.addNewAnd(), (AndGate) gate);
            }
            else if (gate instanceof OrGate)
            {
                fillGateList(gateListDef.addNewOr(), (OrGate) gate);
            }
            else if (gate instanceof SubsetRef)
            {
                fillPopulationRefGate(gateListDef.addNewSubset(), (SubsetRef)gate);
            }
        }
    }

    static public void fillGate(GateDef gateDef, Gate gate)
    {
        if (gate instanceof PolygonGate)
        {
            PolygonGate polygonGate = (PolygonGate) gate;
            PolygonDef polygonDef = gateDef.addNewPolygon();
            fillPolygonGate(polygonDef, polygonGate);
        }
        else if (gate instanceof IntervalGate)
        {
            IntervalGate intervalGate = (IntervalGate) gate;
            IntervalDef intervalDef = gateDef.addNewInterval();
            fillIntervalGate(intervalDef, intervalGate);
        }
        else if (gate instanceof EllipseGate)
        {
            EllipseGate ellipseGate = (EllipseGate) gate;
            EllipseDef ellipseDef = gateDef.addNewEllipse();
            fillEllipseGate(ellipseDef, ellipseGate);
        }
        else if (gate instanceof NotGate)
        {
            fillGate(gateDef.addNewNot(), ((NotGate) gate).getGate());
        }
        else if (gate instanceof AndGate)
        {
            fillGateList(gateDef.addNewAnd(), (AndGate) gate);
        }
        else if (gate instanceof OrGate)
        {
            fillGateList(gateDef.addNewOr(), (OrGate) gate);
        }
        else if (gate instanceof SubsetRef)
        {
            fillPopulationRefGate(gateDef.addNewSubset(), ((SubsetRef)gate));
        }

    }

    static public void fillPopulation(PopulationDef populationDef, Population population)
    {
        // Save population name in escaped format
        populationDef.setName(population.getName().getName());
        for (Gate gate : population.getGates())
        {
            GateDef gateDef = populationDef.addNewGate();
            fillGate(gateDef, gate);
        }
        for (Population child : population.getPopulations())
        {
            PopulationDef childDef = populationDef.addNewPopulation();
            fillPopulation(childDef, child);
        }
    }

    static public void makeCompensationCalculationDef(ScriptDocument doc, CompensationCalculation compensationCalculation)
    {
        ScriptDef script = doc.getScript();
        ScriptSettings settings = new ScriptSettings();
        settings.merge(script.getSettings());
        if (compensationCalculation != null)
        {
            settings.merge(compensationCalculation.getSettings());
            CompensationCalculationDef compensationCalculationElement = script.getCompensationCalculation();
            if (compensationCalculationElement == null)
            {
                compensationCalculationElement = script.addNewCompensationCalculation();
            }
            while (compensationCalculationElement.getPopulationArray().length > 0)
            {
                compensationCalculationElement.removePopulation(0);
            }
            for (Population population : compensationCalculation.getPopulations())
            {
                PopulationDef populationDef = compensationCalculationElement.addNewPopulation();
                fillPopulation(populationDef, population);
            }
            while (compensationCalculationElement.getChannelArray().length > 0)
            {
                compensationCalculationElement.removeChannel(0);
            }
            for (CompensationCalculation.ChannelInfo info : compensationCalculation.getChannels())
            {
                ChannelDef def = compensationCalculationElement.addNewChannel();
                def.setName(info.getName());
                fillSubsetDef(def.addNewPositive(), info.getPositive());
                fillSubsetDef(def.addNewNegative(), info.getNegative());
            }
        }
    }

    static public void makeAnalysisDef(ScriptDef script, Workspace workspace, PopulationName groupName, String sampleId, Set<StatisticSet> statisticSets)
    {
        Analysis analysis = null;
        if (groupName != null)
        {
            analysis = workspace.getGroupAnalyses().get(groupName);
            if (analysis == null)
                throw new RuntimeException("No group analysis with the name '" + groupName + "' could be found.");
        }
        else if (sampleId != null)
        {
            Workspace.SampleInfo sample = workspace.getSample(sampleId);
            if (sample == null)
                throw new RuntimeException("Cannot find sample '" + sampleId + "'");

            analysis = workspace.getSampleAnalysis(sample);
            if (analysis == null)
                throw new RuntimeException("Cannot find analysis for sample " + sampleId);
        }
        else
        {
            for (Analysis analysisTry : workspace.getGroupAnalyses().values())
            {
                if (analysisTry.getPopulations().size() > 0)
                {
                    if (analysis != null)
                        throw new RuntimeException("There is more than one group analysis in this workspace.  Please specify which one to use.");
                    analysis = analysisTry;
                }
            }
            if (analysis == null)
            {
                for (Workspace.SampleInfo sample : workspace.getSamplesComplete())
                {
                    Analysis analysisTry = workspace.getSampleAnalysis(sample);
                    if (analysisTry == null)
                        continue;
                    if (analysisTry.getPopulations().size() > 0)
                    {
                        if (analysis != null)
                            throw new RuntimeException("There is more than one sample analysis in this workspace.  Please specify which one to use.");
                        analysis = analysisTry;
                    }
                }
            }
        }

        if (analysis == null)
            throw new RuntimeException("No analyses were found in this workspace.");

        makeAnalysisDef(script, analysis, statisticSets);
    }

    static public void makeAnalysisDef(ScriptDef script, Analysis analysis, Set<StatisticSet> statisticSets)
    {
        if (statisticSets == null)
        {
            statisticSets = EnumSet.of(StatisticSet.existing);
        }
        else
        {
            statisticSets = EnumSet.copyOf(statisticSets);
        }
        ScriptSettings settings = new ScriptSettings();
        settings.merge(script.getSettings());
        if (analysis != null)
        {
            settings.merge(analysis.getSettings());
            AnalysisDef analysisElement = script.getAnalysis();
            if (analysisElement == null)
            {
                analysisElement = script.addNewAnalysis();
            }
            while (analysisElement.getPopulationArray().length > 0)
            {
                analysisElement.removePopulation(0);
            }
            while (analysisElement.getSubsetArray().length > 0)
            {
                analysisElement.removeSubset(0);
            }
            for (Population population : analysis.getPopulations())
            {
                PopulationDef populationDef = analysisElement.addNewPopulation();
                fillPopulation(populationDef, population);
            }
            for (SubsetSpec subset : analysis.getSubsets())
            {
                analysisElement.addNewSubset().setSubset(subset.toString());
            }

            if (!statisticSets.equals(EnumSet.of(StatisticSet.existing)))
            {
                Set<StatisticSpec> statisticSpecs = new TreeSet();
                if (statisticSets.contains(StatisticSet.existing))
                {
                    for (StatisticDef stat : analysisElement.getStatisticArray())
                    {
                        StatisticSpec spec = makeStatisticSpec(stat);
                        StatisticSet set = StatisticSet.fromStatisticSpec(spec);
                        if (set != null)
                        {
                            statisticSets.add(set);
                        }
                        statisticSpecs.add(spec);
                    }
                }
                if (statisticSets.contains(StatisticSet.workspace))
                {
                    for (StatisticSpec spec : analysis.getStatistics())
                    {
                        if (StatisticSet.isRedundant(statisticSets, spec))
                        {
                            continue;
                        }
                        statisticSpecs.add(spec);
                    }
                }
                for (StatisticSet statisticSet : statisticSets)
                {
                    StatisticSpec spec = statisticSet.getStat();
                    if (spec != null)
                    {
                        statisticSpecs.add(spec);
                    }
                }
                while (analysisElement.getStatisticArray().length != 0)
                {
                    analysisElement.removeStatistic(0);
                }
                for (StatisticSpec spec : statisticSpecs)
                {
                    addStatistic(analysisElement, spec);
                }
            }
            while (analysisElement.getGraphArray().length != 0)
            {
                analysisElement.removeGraph(0);
            }
            for (GraphSpec graph : new TreeSet<>(analysis.getGraphs()))
            {
                addGraph(analysisElement, graph);
            }
        }
        script.setSettings(settings.toSettingsDef());
    }

    public static void addSubsets(Collection<SubsetSpec> list, SubsetSpec parent, PopulationDef pop)
    {
        assert !SubsetSpec.___isExpression(pop.getName());
        PopulationName name = PopulationName.fromString(pop.getName());
        SubsetSpec cur = new SubsetSpec(parent, name);
        list.add(cur);
        for (PopulationDef childPop : pop.getPopulationArray())
        {
            addSubsets(list, cur, childPop);
        }
    }

    static public Collection<SubsetSpec> getSubsets(String script, boolean includeAnalysis, boolean includeCompensation, boolean includeBooleans)
    {
        if (script == null || script.length() == 0)
            return Collections.emptyList();
        try
        {
            ScriptDef scriptElement = parseScript(script);
            Set<SubsetSpec> ret = new TreeSet<>(SubsetSpec.COMPARATOR);
            PopulationDef[] pops = null;
            if (includeCompensation)
            {
                if (scriptElement.getCompensationCalculation() != null)
                {
                    pops = scriptElement.getCompensationCalculation().getPopulationArray();
                }
            }
            if (includeAnalysis)
            {
                if (scriptElement.getAnalysis() != null)
                {
                    if (includeBooleans)
                    {
                        for (SubsetDef subsetDef : scriptElement.getAnalysis().getSubsetArray())
                        {
                            ret.add(SubsetSpec.fromEscapedString(subsetDef.getSubset()));
                        }
                    }
                    pops = scriptElement.getAnalysis().getPopulationArray();
                }
            }
            if (pops != null)
            {
                for (PopulationDef pop : pops)
                {
                    addSubsets(ret, null, pop);
                }
            }

            return ret;
        }
        catch (XmlException e)
        {
            return Collections.emptyList();
        }
    }

    static public boolean isFCSDirectory(File directory)
    {
        return FCSAnalyzer.get().containsFCSFiles(directory);
    }

    static public List<File> listFCSDirectories(File rootDirectory)
    {
        List<File> ret = new ArrayList();
        for (File file : rootDirectory.listFiles())
        {
            if (!file.isDirectory())
                continue;
            if (FCSAnalyzer.get().containsFCSFiles(file))
            {
                ret.add(file);
            }
        }
        return ret;
    }

    static public int countFCSFiles(File directory)
    {
        int ret = 0;
        for (File file : directory.listFiles())
        {
            if (FCSAnalyzer.get().isFCSFile(file))
                ret ++;
        }
        return ret;
    }

    static public Dimension getGraphSize()
    {
        return FCSAnalyzer.get().getGraphSize();
    }
}
