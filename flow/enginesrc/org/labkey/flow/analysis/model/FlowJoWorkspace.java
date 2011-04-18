/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.util.SymbolTable;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;


abstract public class FlowJoWorkspace implements Serializable
{
    protected Map<String, Analysis> _groupAnalyses = new HashMap<String, Analysis>();
    protected Map<String, Analysis> _sampleAnalyses = new HashMap<String, Analysis>();
    protected Map<String, AttributeSet> _sampleAnalysisResults = new HashMap<String, AttributeSet>();
    protected Map<String, SampleInfo> _sampleInfos = new HashMap<String, SampleInfo>();
    protected Map<String, ParameterInfo> _parameters = new LinkedHashMap<String, ParameterInfo>();
    protected List<CalibrationTable> _calibrationTables = new ArrayList<CalibrationTable>();
    protected ScriptSettings _settings = new ScriptSettings();
    protected List<String> _warnings;
    protected List<CompensationMatrix> _compensationMatrices = new ArrayList<CompensationMatrix>();
    protected List<AutoCompensationScript> _autoCompensationScripts = new ArrayList<AutoCompensationScript>();

    public class SampleInfo implements Serializable
    {
        Map<String, String> _keywords = new HashMap<String, String>();
        Map<String, ParameterInfo> _parameters;
        String _sampleId;
        String _compensationId;

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

        public String getCompensationId()
        {
            return _compensationId;
        }

        public void setCompensationId(String id)
        {
            _compensationId = id;
        }

        public String getLabel()
        {
            String ret = getKeywords().get("$FIL");
            if (ret == null)
                return _sampleId;
            return ret;
        }

        public CompensationMatrix getCompensationMatrix()
        {
            if (_compensationId == null)
            {
                return null;
            }

            int id = Integer.parseInt(_compensationId);
            if (id < 0)
            {
                return CompensationMatrix.fromSpillKeyword(_keywords);
            }

            if (_compensationMatrices.size() == 0)
            {
                return null;
            }
            if (_compensationMatrices.size() == 1)
            {
                return _compensationMatrices.get(0);
            }
            if (_compensationMatrices.size() < id)
            {
                return null;
            }
            return _compensationMatrices.get(id - 1);
        }
    }

    static private class WorkspaceRecognizer extends DefaultHandler
    {
        boolean _isWorkspace = false;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if ("Workspace".equals(qName))
            {
                _isWorkspace = true;
            }
            else
            {
                _isWorkspace = false;
            }
            throw new SAXException("Stop parsing");
        }
        boolean isWorkspace()
        {
            return _isWorkspace;
        }
    }


    static public boolean isFlowJoWorkspace(File file)
    {
        if (file.getName().endsWith(".wsp"))
            return true;
        if (file.isDirectory())
            return false;
        WorkspaceRecognizer recognizer = new WorkspaceRecognizer();
        try
        {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(file, recognizer);
        }
        catch (Exception e)
        {
            // suppress
        }
        return recognizer.isWorkspace();
    }


    public class ParameterInfo implements Serializable
    {
        public String name;
        // For some parameters, FlowJo maps them as integers between 0 and 4095, even though
        // they actually range much higher.
        // This multiplier maps to the range that we actually use.
        public double multiplier;
        public double minValue;
        public CalibrationTable calibrationTable;
    }


	static class FJErrorHandler implements ErrorHandler
	{
		public void warning(SAXParseException exception) throws SAXException
		{
			// ignore
		}

		public void error(SAXParseException exception) throws SAXException
		{
			throw exception;
		}

		public void fatalError(SAXParseException exception) throws SAXException
		{
			String msg = exception.getLocalizedMessage();
			if (msg != null)
			{
				// ignore malformed XML in <OverlayGraphs> element
				if (msg.contains("OverlayGraphs") && (msg.contains("xParameter") || msg.contains("yParameter")))
					return;
			}
			throw exception;
		}
	}



    final static String[] parsedElements =
    {
        "AutoCompensationScripts",
        "Axis",
        "BooleanGate",
        "CalibrationTables",
        "Channel",
        "ChannelValue",
        "CompensationMatrices",
        "CompensationMatrix",
        "Ellipse",
        "FCSHeader",
        "GatePaths",
        "Group",
        "GroupAnalyses",
        "GroupNode",
        "Groups",
        "Keyword",
        "Keywords",
        "MatchingCriteria",
        "Parameter",
        "ParameterDefinition",
        "Point",
        "PolyRect",
        "Polygon",
        "PolygonGate",
        "Population",
        "Range",
        "RangeGate",
        "RectangleGate",
        "Sample",
        "SampleAnalyses",
        "SampleList",
        "SampleNode",
        "Samples",
        "Script",
        "Statistic",
        "String",
        "StringArray",
        "Subpopulations",
        "Table",
        "ValidateCompensation",
        "Vertex",
        "Workspace",
        "and",
        "ellipse",
        "focus",
        "interval",
        "not",
        "or",
        "point",
        "polygon"
    };


    final static String[] rejectElements =
    {
        "Layout",
        "LayoutEditor",
        "LayoutGraph",
        "OverlayGraphs",
        "TableEditor"
    };


    final static String[] knownElements =
    {
        "Annotation",
        "AnnotationTextTraits",
        "AutoCompensationScripts",
        "Axis",
        "AxisLabelText",
        "AxisText",
        "BooleanGate",
        "CalibrationTables",
        "Channel",
        "ChannelValue",
        "Column",
        "Columns",
        "CompensationMatrices",
        "CompensationMatrix",
        "Contents",
        "Criteria",
        "CriteriaFormula",
        "Criterion",
        "DT_32BitKeepAsLinear",
        "DataSet",
        "EventLimit",
        "GatePaths",
        "GateText",
        "Graph",
        "Group",
        "GroupNode",
        "Groups",
        "Keyword",
        "Keywords",
        "Layer",
        "Layout",
        "LayoutEditor",
        "LayoutGraph",
        "Legend",
        "LegendTextTraits",
        "OverlayGraphs",
        "PCPlotBlueControl",
        "PCPlotGreenControl",
        "PCPlotRedControl",
        "Parameter",
        "ParameterNames",
        "PolyChromaticPlot",
        "PolyRect",
        "Polygon",
        "PolygonGate",
        "Population",
        "Preferences",
        "Sample",
        "SampleList",
        "SampleNode",
        "SampleRef",
        "SampleRefs",
        "SampleSortCriteria",
        "SciBook",
        "StainChannelList",
        "StainCriterion",
        "String",
        "StringArray",
        "Table",
        "TableEditor",
        "Text",
        "TextTraits",
        "Vertex",
        "WindowPosition",
        "Workspace",
        "graphList",
        "subsetList"
    };


    static final short defaultFilter = LSParserFilter.FILTER_SKIP;
    
    final static HashMap<String,Short> elements = new HashMap<String, Short>(100);
    static
    {
        for (String s : knownElements)
            elements.put(s, defaultFilter);
        for (String s : rejectElements)
            elements.put(s, LSParserFilter.FILTER_REJECT);
        for (String s : parsedElements)
            elements.put(s, LSParserFilter.FILTER_ACCEPT);
    }

    static class FJParseFilter implements LSParserFilter
    {
        SymbolTable fSymbolTable = new SymbolTable();
        Set<String> rejected = new HashSet<String>();
        
        public short startElement(Element element)
        {
            Short s = elements.get(element.getNodeName());
            short filter = null == s ? defaultFilter : s.shortValue();
//            if (filter != FILTER_ACCEPT && rejected.add(element.getNodeName())) System.err.println((filter == FILTER_SKIP ? "SKIPPED:  " : "REJECTED: ") + element.getNodeName());
            return filter;
        }

        public short acceptNode(Node node)
        {
            if (node instanceof Text)
            {
                String data = ((Text)node).getData();
                if (data.length() < 10 && data.trim().length() == 0)
                    return FILTER_REJECT;
                else
                    return FILTER_ACCEPT;
            }
            if (node instanceof Element)
            {
                int len = node.getAttributes().getLength();
                for (int i=0 ; i<len ; i++)
                {
                    Attr a = (Attr)node.getAttributes().item(i);
                    a.setValue(fSymbolTable.addSymbol(a.getValue()));
                }
            }
            return FILTER_ACCEPT;
        }

        public int getWhatToShow()
        {
            return NodeFilter.SHOW_ALL;
        }
    }


/*
    static class FJSymbolTable extends SymbolTable
    {
        int sizeIn = 0;
        int sizeOut = 0;
        
        @Override
        public String addSymbol(String symbol)
        {
            assert (sizeIn += symbol.length()) > -1;
            assert (sizeOut += (containsSymbol(symbol) ? 0 : symbol.length())) > -1;
            return super.addSymbol(symbol);
        }

        @Override
        public String addSymbol(char[] buffer, int offset, int length)
        {
            assert (sizeIn += length) > -1;
            assert (sizeOut += (containsSymbol(buffer, offset, length) ? 0 : length)) > -1;
            return super.addSymbol(buffer, offset, length);
        }
    }
*/

    static class FJDOMParser extends DOMParser
    {
        SymbolTable fSymbolTable;

        static FJDOMParser create()
        {
            SymbolTable fj = new SymbolTable();
            return new FJDOMParser(fj);
        }

        FJDOMParser(SymbolTable st)
        {
            super(st);
            fSymbolTable = st;
            fSkippedElemStack = new Stack();
            fDOMFilter = new FJParseFilter();
            try
            {
                setFeature(Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE, false);
                setFeature(DEFER_NODE_EXPANSION, false);
                setFeature(NAMESPACES, false);
                setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE, true);
                setErrorHandler(new FJErrorHandler());
            }
            catch (SAXNotSupportedException x)
            {
                throw new RuntimeException(x);
            }
            catch (SAXNotRecognizedException x)
            {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void parse(InputSource inputSource) throws SAXException, IOException
        {
            try
            {
                super.parse(inputSource);
            }
            catch (RuntimeException x)
            {
                Logger.getLogger(FlowJoWorkspace.class).error("Unexpected error", x);
                throw x;
            }
        }
    }

    static public FlowJoWorkspace readWorkspace(InputStream stream) throws Exception
    {
        DOMParser p = FJDOMParser.create();
        p.parse(new InputSource(stream));
        Document doc = p.getDocument();
        Element elDoc = doc.getDocumentElement();
//        System.err.println("DOCUMENT SIZE: " + debugComputeSize(elDoc));
        if ("1.4".equals(elDoc.getAttribute("version")))
        {
            return new PCWorkspace(elDoc);
        }
        if ("2.0".equals(elDoc.getAttribute("version")))
        {
            return new FJ8Workspace(elDoc);
        }
        return new MacWorkspace(elDoc);
    }


    static long debugComputeSize(Object doc)
    {
        try
        {
            final long[] len = new long[1];
            OutputStream counterStream = new OutputStream()
            {
                public void write(int i) throws IOException
                {
                    len[0] += 4;
                }

                @Override
                public void write(byte[] bytes) throws IOException
                {
                    len[0] += bytes.length;
                }

                @Override
                public void write(byte[] bytes, int off, int l) throws IOException
                {
                    len[0] += l;
                }
            };
            ObjectOutputStream os = new ObjectOutputStream(counterStream);
            os.writeObject(doc);
            os.close();
            return len[0];
        }
        catch (IOException x)
        {
            return -1;
        }
    }


    protected FlowJoWorkspace()
    {
    }


    public List<CompensationMatrix> getCompensationMatrices()
    {
        return _compensationMatrices;
    }

    public Set<CompensationMatrix> getUsedCompensationMatrices()
    {
        Set<CompensationMatrix> ret = new LinkedHashSet<CompensationMatrix>();
        for (SampleInfo sample : getSamples())
        {
            CompensationMatrix comp = sample.getCompensationMatrix();
            if (comp == null)
                continue;
            ret.add(comp);
        }
        return ret;
    }

    public List<? extends AutoCompensationScript> getAutoCompensationScripts()
    {
        return _autoCompensationScripts;
    }

    static List<Element> getElementsByTagName(Element parent, String tagName)
    {
        List<Element> ret = new ArrayList<Element>();
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

    protected double getRange(Element elParameter)
    {
        if (StringUtils.isEmpty(elParameter.getAttribute("highValue")))
        {
            return 4096;
        }
        return Double.valueOf(elParameter.getAttribute("highValue")).doubleValue() * findMultiplier(elParameter);
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
        return new ArrayList<SampleInfo>(_sampleInfos.values());
    }

    public SampleInfo getSample(String sampleId)
    {
        return _sampleInfos.get(sampleId);
    }
    public Analysis getSampleAnalysis(SampleInfo sample)
    {
        return _sampleAnalyses.get(sample._sampleId);
    }
    public AttributeSet getSampleAnalysisResults(SampleInfo sample)
    {
        return _sampleAnalysisResults.get(sample._sampleId);
    }
    public String[] getParameters()
    {
        return _parameters.keySet().toArray(new String[_parameters.keySet().size()]);
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

    private Analysis findAnalysisWithKeywordValue(String keyword, String value, List<String> errors)
    {
        SampleInfo sample = findSampleWithKeywordValue(keyword, value);
        if (sample == null)
        {
            errors.add("Could not find sample for " + keyword + "=" + value);
            return null;
        }

        Analysis analysis = getSampleAnalysis(sample);
        if (analysis == null)
        {
            errors.add("Could not find sample analysis for " + keyword + "=" + value);
            return null;
        }

        return analysis;
    }

    private SubsetSpec makeSubsetKeyAndAddAnalysis(CompensationCalculation calc, String name, Analysis analysis, String subset, List<String> errors)
    {
        if (subset == null || analysis == null)
            return null;
        String rootSubset = cleanPopName(name);
        SubsetSpec ret = SubsetSpec.fromString(rootSubset + "/" + subset);

        Population pop = calc.getPopulation(rootSubset);
        if (pop == null)
        {
            pop = new Population();
            pop.setName(rootSubset);
            for (Population child : analysis.getPopulations())
            {
                pop.addPopulation(child);
            }
            calc.addPopulation(pop);
        }

        if (!"Ungated".equals(subset) && findPopulation(pop, SubsetSpec.fromString(subset)) == null)
        {
            String analysisName = analysis.getName() == null ? "" : " '" + analysis.getName() + "'";
            errors.add("Channel '" + name + "' subset '" + subset + "' not found in analysis" + analysisName);
        }

        return ret;
    }

    private CompensationCalculation.ChannelSubset makeChannelSubset(
            CompensationCalculation calc, String name, Analysis analysis, String keyword, String value, String subset, List<String> errors)
    {
        if (analysis == null)
        {
            analysis = findAnalysisWithKeywordValue(keyword, value, errors);
        }

        SubsetSpec subsetSpec = makeSubsetKeyAndAddAnalysis(calc, name, analysis, subset, errors);
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

    private void addPopulationMap(Map<SubsetSpec, Population> map, SubsetSpec parent, Population pop)
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
        for (Map<SubsetSpec, Population> aLstMap : lstMap)
        {
            Population pop = aLstMap.get(subset);
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
    private void simplifySubsetNames(Map<SubsetSpec, SubsetSpec> subsetMap, List<Map<SubsetSpec,Population>> lstPopulationMap, SubsetSpec oldParent, Population population)
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

    private Population findPopulation(PopulationSet calc, SubsetSpec spec)
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
        Map<SubsetSpec, SubsetSpec> subsetMap = new LinkedHashMap<SubsetSpec, SubsetSpec>();
        List<Map<SubsetSpec,Population>> lstPopulationMap = new ArrayList<Map<SubsetSpec,Population>>();
        for (Population pop : calc.getPopulations())
        {
            Map<SubsetSpec,Population> map = new HashMap<SubsetSpec,Population>();
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
        ret.setSettings(calc.getSettings());
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

    public CompensationCalculation makeCompensationCalculation(Map<String, CompensationChannelData> channelDataMap, String groupName, List<String> errors)
    {
        CompensationCalculation ret = new CompensationCalculation();
        ret.setSettings(_settings);
        boolean isUniversalNegative = isUniversalNegative(channelDataMap);

        Analysis analysis = null;
        if (StringUtils.isNotEmpty(groupName))
        {
            analysis = getGroupAnalyses().get(groupName);
            if (analysis == null)
            {
                errors.add("Group '" + groupName + "' not found in workspace");
                return ret;
            }
        }

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
            CompensationCalculation.ChannelSubset positiveSubset = makeChannelSubset(ret, positiveName, analysis,
                    data.positiveKeywordName, data.positiveKeywordValue, data.positiveSubset, errors);
            CompensationCalculation.ChannelSubset negativeSubset = makeChannelSubset(ret, negativeName, analysis,
                    data.negativeKeywordName, data.negativeKeywordValue, data.negativeSubset, errors);
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
            ret[i] = lst.get(i).doubleValue();
        }
        return ret;
    }

    protected double parseParamValue(String param, Element el, String attribute)
    {
        return Double.valueOf(el.getAttribute(attribute)).doubleValue();
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
        return info.calibrationTable;
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

}
