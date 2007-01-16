package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.apache.commons.lang.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

import org.labkey.flow.analysis.web.SubsetSpec;

abstract public class FlowJoWorkspace implements Serializable
{
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

    protected Map<String, Analysis> _groupAnalyses = new HashMap();
    protected Map<String, Analysis> _sampleAnalyses = new HashMap();
    protected Map<String, SampleInfo> _sampleInfos = new HashMap();
    protected Map<String, ParameterInfo> _parameters = new LinkedHashMap();
    protected Set<StatisticSet> _statisticSets;
    protected List<String> _warnings;

    public static class SampleInfo implements Serializable
    {
        Map<String, String> _keywords = new HashMap();
        String _sampleId;

        public void setSampleId(String id)
        {
            _sampleId = id;
        }
        public Map<String,String> getKeywords()
        {
            return _keywords;
        }
        public String getSampleId()
        {
            return _sampleId;
        }

        public String getLabel()
        {
            String ret = getKeywords().get("$FIL");
            if (ret == null)
                return _sampleId;
            return ret;
        }
    }

    public class ParameterInfo implements Serializable
    {
        public String name;
        // For some parameters, FlowJo maps them as integers between 0 and 4095, even though
        // they actually range much higher.
        // This multiplier maps to the range that we actually use.
        public double multiplier;
    }

    static public FlowJoWorkspace readWorkspace(InputStream stream, Set<StatisticSet> statisticSets) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(stream);
        Element elDoc = doc.getDocumentElement();
        if ("1.4".equals(elDoc.getAttribute("version")))
        {
            return new PCWorkspace(elDoc, statisticSets);
        }
        if ("2.0".equals(elDoc.getAttribute("version")))
        {
            return new FJ8Workspace(elDoc, statisticSets);
        }
        return new MacWorkspace(elDoc, statisticSets);
    }

    protected FlowJoWorkspace(Set<StatisticSet> statisiticSets)
    {
        _statisticSets = statisiticSets;
    }


    static List<Element> getElementsByTagName(Element parent, String tagName)
    {
        List<Element> ret = new ArrayList();
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

    // For some parameters, the actual range is 262144, but FlowJo coerces
    // the value to something between 0 and 4096.  This code is a bit of a
    // hack to try to detect that case.
    protected double findMultiplier(Element elParameter)
    {
        if ("1".equals(elParameter.getAttribute("log")))
            return 1;
        // Only check for FSC-A, FSC-H, and SSC-A
        if (",FSC-A,FSC-H,SSC-A,".indexOf("," + elParameter.getAttribute("name") + ",") < 0)
            return 1;
        if ("4096".equals(elParameter.getAttribute("range")) && "4096".equals(elParameter.getAttribute("highValue")))
            return 64;
        return 1;
    }
    static public String cleanName(String name)
    {
        name = StringUtils.replace(name, "<", CompensationMatrix.PREFIX);
        name = StringUtils.replace(name, ">", CompensationMatrix.SUFFIX);
        name = StringUtils.replaceChars(name, ',', ';');
        name = StringUtils.replaceChars(name, (char) 209, '-');
        return name;
    }

    static public String cleanPopName(String name)
    {
        name = cleanName(name);
        name = StringUtils.replaceChars(name, '/', '_');
        return name;
    }

    protected double getMultiplier(String name)
    {
        ParameterInfo info = _parameters.get(name);
        if (info == null)
            return 1;
        return info.multiplier;
    }
    protected String getTextValue(Element el)
    {
        String ret = "";
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i ++)
        {
            ret += nl.item(i).getNodeValue();
        }
        return ret;
    }

    public Map<String, Analysis> getGroupAnalyses()
    {
        return _groupAnalyses;
    }

    public List<SampleInfo> getSamples()
    {
        return new ArrayList(_sampleInfos.values());
    }

    public SampleInfo getSample(String sampleId)
    {
        return _sampleInfos.get(sampleId);
    }
    public Analysis getSampleAnalysis(SampleInfo sample)
    {
        return _sampleAnalyses.get(sample._sampleId);
    }
    public String[] getParameters()
    {
        return _parameters.keySet().toArray(new String[0]);
    }

    static public class CompensationChannelData
    {
        public String positiveKeywordName;
        public String positiveKeywordValue;
        public String positiveSubset;
        public String negativeKeywordName;
        public String negativeKeywordValue;
        public String negativeSubset;
    }

    public SampleInfo findSampleWithKeywordValue(String keyword, String value)
    {
        for (SampleInfo sample : getSamples())
        {
            if (value.equals(sample._keywords.get(keyword)))
                return sample;
        }
        return null;
    }

    private SubsetSpec makeSubsetKeyAndAddAnalysis(CompensationCalculation calc, String name, String keyword, String value, String subset, List<String> errors)
    {
        if (subset == null)
            return null;
        String rootSubset = cleanPopName(name);
        SubsetSpec ret = SubsetSpec.fromString(rootSubset + "/" + subset);
        if (calc.getPopulation(rootSubset) != null)
        {
            return ret;
        }
        SampleInfo sample = findSampleWithKeywordValue(keyword, value);
        if (sample == null)
        {
            errors.add("Could not find sample with " + keyword + "=" + value);
            return ret;
        }
        Analysis analysis = getSampleAnalysis(sample);
        if (analysis == null)
        {
            errors.add("Could not find sample analysis for " + keyword + "=" + value);
            return ret;
        }
        Population pop = new Population();
        pop.setName(rootSubset);
        for (Population child : analysis.getPopulations())
        {
            pop.addPopulation(child);
        }
        calc.addPopulation(pop);
        return ret;
    }

    private CompensationCalculation.ChannelSubset makeChannelSubset(CompensationCalculation calc, String name, String keyword, String value, String subset, List<String> errors)
    {
        SubsetSpec subsetSpec = makeSubsetKeyAndAddAnalysis(calc, name, keyword, value, subset, errors);
        SampleCriteria criteria = new SampleCriteria();
        criteria.setKeyword(keyword);
        criteria.setPattern(value);
        return new CompensationCalculation.ChannelSubset(criteria, subsetSpec);
    }

    private boolean isUniversalNegative(Map<String, CompensationChannelData> channelDataMap)
    {
        String keyword = null;
        String value = null;
        for (Map.Entry<String, CompensationChannelData> entry : channelDataMap.entrySet())
        {
            if (keyword == null)
            {
                keyword = entry.getValue().negativeKeywordName;
            }
            else if (!keyword.equals(entry.getValue().negativeKeywordName))
            {
                return false;
            }
            if (value == null)
            {
                value = entry.getValue().negativeKeywordValue;
            }
            else if (!value.equals(entry.getValue().negativeKeywordValue))
            {
                return false;
            }
        }
        return true;
    }

    private void addPopulationMap(HashMap<SubsetSpec, Population> map, SubsetSpec parent, Population pop)
    {
        SubsetSpec subset = new SubsetSpec(parent, pop.getName());
        map.put(subset, pop);
        for (Population child : pop.getPopulations())
        {
            addPopulationMap(map, subset, child);
        }
    }

    private boolean gatesEqual(Population pop1, Population pop2)
    {
        return pop1.getGates().equals(pop2.getGates());
    }

    private boolean isUniversal(SubsetSpec subset, List<Map<SubsetSpec,Population>> lstMap)
    {
        Population popCompare = null;
        for (int i = 0; i < lstMap.size(); i ++)
        {
            Population pop = lstMap.get(i).get(subset);
            if (pop == null)
                continue;
            if (popCompare == null)
            {
                popCompare = pop;
            }
            else
            {
                if (!gatesEqual(popCompare, pop))
                    return false;
            }
        }
        return true;
    }

    private void mapSubsetNames(Map<SubsetSpec, SubsetSpec> map, SubsetSpec oldParent, SubsetSpec newParent, Population pop)
    {
        SubsetSpec oldSubset = new SubsetSpec(oldParent, pop.getName());
        SubsetSpec newSubset = new SubsetSpec(newParent, pop.getName());
        map.put(oldSubset, newSubset);
        for (Population child : pop.getPopulations())
        {
            mapSubsetNames(map, oldSubset, newSubset, child);
        }
    }

    private String compose(String prefix, String suffix)
    {
        if (prefix.endsWith("+") && suffix.startsWith("+") ||
            prefix.endsWith("-") && suffix.startsWith("-"))
        {
            return prefix + suffix.substring(1);
        }
        return prefix + suffix;
    }
    /**
     * Initially, each channel has a unique gating tree with a root population with a name like "FITC+", or something.
     * This walks through one of these trees, and figures out if the gates within them (e.g. "FITC+/L") is the same
     * for each other tree.
     * If it is, then the "FITC+/L" gate is changed to "L".
     * If it is not, then the "FITC+/L" gate is changed to "FITC+L"
     */
    private void simplifySubsetNames(LinkedHashMap<SubsetSpec, SubsetSpec> subsetMap, List<Map<SubsetSpec,Population>> lstPopulationMap, SubsetSpec oldParent, Population population)
    {
        SubsetSpec newParent = subsetMap.get(oldParent);
        SubsetSpec oldSubset = new SubsetSpec(oldParent, population.getName());
        SubsetSpec subsetTry = new SubsetSpec(newParent, population.getName());
        SubsetSpec newSubset;
        if (!isUniversal(subsetTry, lstPopulationMap))
        {
            String root = oldParent.getRoot().toString();
            newSubset = new SubsetSpec(newParent, compose(root, population.getName()));
            subsetMap.put(oldSubset, newSubset);
            for (Population child : population.getPopulations())
            {
                mapSubsetNames(subsetMap, oldSubset, newSubset, child);
            }
            return;
        }
        newSubset = subsetTry;
        subsetMap.put(oldSubset, newSubset);
        for (Population child : population.getPopulations())
        {
            simplifySubsetNames(subsetMap, lstPopulationMap, oldSubset, child);
        }
    }

    private Population findPopulation(CompensationCalculation calc, SubsetSpec spec)
    {
        PopulationSet cur = calc;
        for (String name : spec.getSubsets())
        {
            if (cur == null)
                return null;
            cur = cur.getPopulation(name);
        }
        return (Population) cur;
    }

    private CompensationCalculation simplify(CompensationCalculation calc)
    {
        LinkedHashMap<SubsetSpec, SubsetSpec> subsetMap = new LinkedHashMap();
        List<Map<SubsetSpec,Population>> lstPopulationMap = new ArrayList();
        for (Population pop : calc.getPopulations())
        {
            HashMap<SubsetSpec,Population> map = new HashMap();
            for (Population child : pop.getPopulations())
            {
                addPopulationMap(map, null, child);
            }
            lstPopulationMap.add(map);
        }
        for (Population pop : calc.getPopulations())
        {
            for (Population child : pop.getPopulations())
            {
                simplifySubsetNames(subsetMap, lstPopulationMap, new SubsetSpec(null, pop.getName()), child);
            }
        }
        CompensationCalculation ret = new CompensationCalculation();
        for (Map.Entry<SubsetSpec, SubsetSpec> entry : subsetMap.entrySet())
        {
            SubsetSpec oldSubset = entry.getKey();
            SubsetSpec newSubset = entry.getValue();
            if (findPopulation(ret, newSubset) != null)
                continue;
            Population oldPop = findPopulation(calc, oldSubset);

            SubsetSpec newParentSubset = newSubset.getParent();
            PopulationSet newParent;
            if (newParentSubset == null)
            {
                newParent = ret;
            }
            else
            {
                newParent = findPopulation(ret, newParentSubset);
            }
            Population newPop = new Population();
            newPop.setName(cleanName(newSubset.getSubset()));
            newPop.getGates().addAll(oldPop.getGates());
            assert newParent.getPopulation(newPop.getName()) == null;
            newParent.addPopulation(newPop);
        }
        for (CompensationCalculation.ChannelInfo oldChannel : calc.getChannels())
        {
            CompensationCalculation.ChannelSubset oldPositive = oldChannel.getPositive();
            CompensationCalculation.ChannelSubset oldNegative = oldChannel.getNegative();
            SubsetSpec newPositiveSubset = subsetMap.get(oldPositive.getSubset());
            SubsetSpec newNegativeSubset = subsetMap.get(oldNegative.getSubset());
            ret.addChannel(oldChannel.getName(),
                    new CompensationCalculation.ChannelSubset(oldPositive.getCriteria(), newPositiveSubset),
                    new CompensationCalculation.ChannelSubset(oldNegative.getCriteria(), newNegativeSubset));
        }
        return ret;
    }

    public CompensationCalculation makeCompensationCalculation(Map<String, CompensationChannelData> channelDataMap, List<String> errors)
    {
        CompensationCalculation ret = new CompensationCalculation();
        boolean isUniversalNegative = isUniversalNegative(channelDataMap);

        for (Map.Entry<String, CompensationChannelData> entry : channelDataMap.entrySet())
        {
            String parameter = entry.getKey();
            CompensationChannelData data = entry.getValue();
            if (data.positiveKeywordName == null || data.positiveKeywordValue == null ||
                    data.negativeKeywordName == null || data.negativeKeywordValue == null)
            {
                errors.add("Missing data for parameter '" + parameter +"'");
                continue;
            }
            String positiveName = parameter + "+";
            String negativeName = isUniversalNegative ? "-" : parameter + "-";
            CompensationCalculation.ChannelSubset positiveSubset = makeChannelSubset(ret, positiveName, data.positiveKeywordName, data.positiveKeywordValue, data.positiveSubset, errors);
            CompensationCalculation.ChannelSubset negativeSubset = makeChannelSubset(ret, negativeName, data.negativeKeywordName, data.negativeKeywordValue, data.negativeSubset, errors);
            ret.addChannel(parameter, positiveSubset, negativeSubset);
        }
        ret = simplify(ret);
        return ret;
    }

    protected double[] toDoubleArray(List<Double> lst)
    {
        double[] ret = new double[lst.size()];
        for (int i = 0; i < lst.size(); i ++)
        {
            ret[i] = lst.get(i);
        }
        return ret;
    }

    protected double parseParamValue(String param, Element el, String attribute)
    {
        double ret = Double.valueOf(el.getAttribute(attribute));
        ret *= getMultiplier(param);
        return ret;
    }

    protected void warning(String str)
    {
        _warnings.add(str);
    }
}
