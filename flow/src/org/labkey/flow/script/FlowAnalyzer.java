package org.labkey.flow.script;

import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.flow.data.*;
import org.labkey.flow.FlowSettings;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.xmlbeans.XmlException;

import java.net.URI;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.awt.*;

import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.*;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.model.Polygon;

public class FlowAnalyzer
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

    static public List<FCSRef> getFCSRefs(FlowRun run) throws Exception
    {
        FlowWell[] wells = run.getWells();
        List<FCSRef> refs = new ArrayList();
        for (FlowWell well : wells)
        {
            refs.add(getFCSRef(well));
        }
        return refs;
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

    static Population makePopulation(Map<String, Gate> gates, PopulationDef populationElement)
    {
        Population ret = new Population();
        ret.setName(populationElement.getName());
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
        return new StatisticSpec(SubsetSpec.fromString(statElement.getSubset()), StatisticSpec.STAT.valueOf(statElement.getName()), statElement.getParameter());
    }

    static public GraphSpec makeGraphSpec(GraphDef graphElement)
    {
        return new GraphSpec(SubsetSpec.fromString(graphElement.getSubset()), graphElement.getXAxis(), graphElement.getYAxis());
    }

    static public Analysis makeAnalysis(AnalysisDef analysisElement)
    {
        Analysis ret = new Analysis();
        if (analysisElement == null)
            return ret;
        Map<String, Gate> gates = getParentGates(analysisElement.getDomNode());
        for (PopulationDef child : analysisElement.getPopulationArray())
        {
            ret.addPopulation(makePopulation(gates, child));
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

    static private CompensationCalculation.ChannelSubset readSubsetDef(ChannelSubsetDef subsetDef)
    {
        SampleCriteria criteria = SampleCriteria.readChildCriteria((Element) subsetDef.getDomNode());
        SubsetSpec subset = null;
        if (subsetDef.getSubset() != null)
        {
            subset = SubsetSpec.fromString(subsetDef.getSubset());
        }
        return new org.labkey.flow.analysis.model.CompensationCalculation.ChannelSubset(criteria, subset);
    }

    static private void fillSubsetDef(ChannelSubsetDef subsetDef, org.labkey.flow.analysis.model.CompensationCalculation.ChannelSubset subset)
    {
        if (subset.getSubset() != null)
        {
            subsetDef.setSubset(subset.getSubset().toString());
        }
        CriteriaDef criteriaDef = subsetDef.addNewCriteria();
        criteriaDef.setKeyword(subset.getCriteria().getKeyword());
        criteriaDef.setPattern(subset.getCriteria().getPattern());
    }


    static public CompensationCalculation makeCompensationCalculation(CompensationCalculationDef compensationCalculationElement)
    {
        org.labkey.flow.analysis.model.CompensationCalculation ret = new org.labkey.flow.analysis.model.CompensationCalculation();
        if (compensationCalculationElement == null)
            return ret;
        Map<String, Gate> gates = getParentGates(compensationCalculationElement.getDomNode());
        for (PopulationDef child : compensationCalculationElement.getPopulationArray())
        {
            ret.addPopulation(makePopulation(gates, child));
        }
        for (ChannelDef channel : compensationCalculationElement.getChannelArray())
        {
            org.labkey.flow.analysis.model.CompensationCalculation.ChannelSubset positive = readSubsetDef(channel.getPositive());
            org.labkey.flow.analysis.model.CompensationCalculation.ChannelSubset negative = readSubsetDef(channel.getNegative());
            ret.addChannel(channel.getName(), positive, negative);
        }
        return ret;
    }

    static public void fillPolygonGate(PolygonDef polygonDef, PolygonGate polygonGate)
    {
        polygonDef.setXAxis(polygonGate.getX());
        polygonDef.setYAxis(polygonGate.getY());
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
        intervalDef.setAxis(intervalGate.getAxis());
        intervalDef.setMin(intervalGate.getMin());
        intervalDef.setMax(intervalGate.getMax());
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
            else if (gate instanceof NotGate)
            {
                fillGate(gateListDef.addNewNot(), ((NotGate) gate).getGate());
            }
            else if (gate instanceof AndGate)
            {
                fillGateList(gateListDef.addNewAnd(), (AndGate) gate);
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
        else if (gate instanceof AndGate)
        {
            fillGateList(gateDef.addNewAnd(), (AndGate) gate);
        }
        else if (gate instanceof NotGate)
        {
            fillGate(gateDef.addNewNot(), ((NotGate) gate).getGate());
        }
    }

    static public void fillPopulation(PopulationDef populationDef, Population population)
    {
        populationDef.setName(population.getName());
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

    static public void makeAnalysis(ScriptDef script, CompensationCalculation compensationCalculation, PopulationSet analysis)
    {
        while (script.getGateArray().length > 0)
        {
            script.removeGate(0);
        }

        if (compensationCalculation != null)
        {
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
            for (org.labkey.flow.analysis.model.CompensationCalculation.ChannelInfo info : compensationCalculation.getChannels())
            {
                ChannelDef def = compensationCalculationElement.addNewChannel();
                def.setName(info.getName());
                fillSubsetDef(def.addNewPositive(), info.getPositive());
                fillSubsetDef(def.addNewNegative(), info.getNegative());
            }
        }
        if (analysis != null)
        {
            AnalysisDef analysisElement = script.getAnalysis();
            if (analysisElement == null)
            {
                analysisElement = script.addNewAnalysis();
            }
            while (analysisElement.getPopulationArray().length > 0)
            {
                analysisElement.removePopulation(0);
            }
            for (Population population : analysis.getPopulations())
            {
                PopulationDef populationDef = analysisElement.addNewPopulation();
                fillPopulation(populationDef, population);
            }
        }
    }

    static public URI getFCSUri(FlowWell well) throws Exception
    {
        return well.getFCSURI();
    }

    static public FCSRef getFCSRef(FlowWell well) throws Exception
    {
        Map<String, String> overrides = new HashMap();
        overrides.putAll(well.getKeywords());
        return new FCSRef(well.getFCSURI(), overrides);
    }

    static public FCSAnalyzer.GraphResult generateGraph(FlowWell well, FlowScript script, FlowProtocolStep step, FlowCompensationMatrix comp, GraphSpec graph) throws Exception
    {
        PopulationSet group = null;
        if (script != null)
        {
            ScriptDef scriptElement= script.getAnalysisScriptDocument().getScript();
            if (step == FlowProtocolStep.calculateCompensation)
            {
                group = makeCompensationCalculation(scriptElement.getCompensationCalculation());
            }
            if (step == FlowProtocolStep.analysis)
            {
                group = makeAnalysis(scriptElement.getAnalysis());
            }
        }
        else
        {
            group = new Analysis();
        }
        CompensationMatrix matrix = comp == null ? null : comp.getCompensationMatrix();
        return FCSAnalyzer.get().generateGraphs(getFCSUri(well), matrix, group, Collections.singletonList(graph)).get(0);
    }

    static public Map<String, String> getParameters(FlowWell well) throws Exception
    {
        FlowRun run = well.getRun();
        FlowCompensationMatrix flowComp = well.getCompensationMatrix();
        CompensationMatrix comp = null;
        if (flowComp != null)
        {
            comp = flowComp.getCompensationMatrix();
        }
        return FCSAnalyzer.get().getParameterNames(getFCSUri(well), comp);
    }

    static public Map<String, String> getParameters(FlowWell well, CompensationMatrix comp) throws Exception
    {
        return FCSAnalyzer.get().getParameterNames(getFCSUri(well), comp);
    }

    static public List<String> getKeywords(ScriptDef scriptElement) throws Exception
    {
        KeywordDef[] el = scriptElement.getRun().getWell().getKeywordArray();
        List<String> ret = new ArrayList();
        for (KeywordDef keyword : el)
        {
            ret.add(keyword.getName());
        }
        return ret;
    }

    static private void addSubsets(List<String> list, String cur, PopulationDef pop)
    {
        list.add(cur + pop.getName());
        cur = cur + pop.getName() + "/";
        for (PopulationDef childPop : pop.getPopulationArray())
        {
            addSubsets(list, cur, childPop);
        }
    }

    static public List<String> getSubsets(FlowScript script) throws Exception
    {
        if (script == null)
        {
            return Collections.EMPTY_LIST;
        }
        return getSubsets(script.getAnalysisScript(), FlowProtocolStep.analysis);
    }

    static public List<String> getSubsets(String script, FlowProtocolStep step)
    {
        try
        {
            ScriptDef scriptElement = parseScript(script);
            List<String> ret = new ArrayList();
            PopulationDef[] pops = null;
            if (step == FlowProtocolStep.calculateCompensation)
            {
                if (scriptElement.getCompensationCalculation() != null)
                {
                    pops = scriptElement.getCompensationCalculation().getPopulationArray();
                }
            }
            if (step == FlowProtocolStep.analysis)
            {
                if (scriptElement.getAnalysis() != null)
                {
                    pops = scriptElement.getAnalysis().getPopulationArray();
                }
            }
            if (pops != null)
            {
                for (PopulationDef pop : pops)
                {
                    addSubsets(ret, "", pop);
                }
            }
            return ret;
        }
        catch (XmlException e)
        {
            return Collections.EMPTY_LIST;
        }
    }

    static public CompensationMatrix getCompensationMatrix(FlowRun run) throws SQLException
    {
        FlowCompensationMatrix comp = run.getCompensationMatrix();
        if (comp == null)
            return null;
        return comp.getCompensationMatrix();
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

    synchronized static public File getAnalysisDirectory() throws Exception
    {
        return FlowSettings.getWorkingDirectory();
    }
}
