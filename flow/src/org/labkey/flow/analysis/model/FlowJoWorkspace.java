package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.apache.commons.lang.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.Serializable;
import java.io.File;
import java.util.*;
import java.net.URI;

import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.data.*;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;

abstract public class FlowJoWorkspace implements Serializable
{
    protected Map<String, Analysis> _groupAnalyses = new HashMap();
    protected Map<String, Analysis> _sampleAnalyses = new HashMap();
    protected Map<String, AttributeSet> _sampleAnalysisResults = new HashMap();
    protected Map<String, SampleInfo> _sampleInfos = new HashMap();
    protected Map<String, ParameterInfo> _parameters = new LinkedHashMap();
    protected ScriptSettings _settings = new ScriptSettings();
    protected List<String> _warnings;
    protected List<CompensationMatrix> _compensationMatrices = new ArrayList();

    public class SampleInfo implements Serializable
    {
        Map<String, String> _keywords = new HashMap();
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
                return CompensationMatrix.fromSpillKeyword(_keywords.get("SPILL"));
            }
            if (_compensationMatrices.size() == 0)
            {
                return null;
            }
            if (_compensationMatrices.size() == 1)
            {
                return _compensationMatrices.get(0);
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
    }

    static public FlowJoWorkspace readWorkspace(InputStream stream) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(stream);
        Element elDoc = doc.getDocumentElement();
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

    protected FlowJoWorkspace()
    {
    }


    public List<CompensationMatrix> getCompensationMatrices()
    {
        return _compensationMatrices;
    }

    public Set<CompensationMatrix> getUsedCompensationMatrices()
    {
        Set<CompensationMatrix> ret = new LinkedHashSet();
        for (SampleInfo sample : getSamples())
        {
            CompensationMatrix comp = sample.getCompensationMatrix();
            if (comp == null)
                continue;
            ret.add(comp);
        }
        return ret;
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
    public AttributeSet getSampleAnalysisResults(SampleInfo sample)
    {
        return _sampleAnalysisResults.get(sample._sampleId);
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

    public CompensationCalculation makeCompensationCalculation(Map<String, CompensationChannelData> channelDataMap, List<String> errors)
    {
        CompensationCalculation ret = new CompensationCalculation();
        ret.setSettings(_settings);
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
        return Double.valueOf(el.getAttribute(attribute));
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
        for (int i = 0; i < values.size(); i++)
        {
            if (values.get(i) > 4096)
                return;
        }
        for (int i = 0; i < values.size(); i ++)
        {
            values.set(i, values.get(i) * multiplier);
        }
    }

    public FlowRun createExperimentRun(User user, Container container, FlowExperiment experiment, File workspaceFile, File runFilePathRoot) throws Exception
    {
        URI dataFileURI = new File(workspaceFile.getParent(), "attributes.flowdata.xml").toURI();
        ExperimentService.Interface svc = ExperimentService.get();
        Map<SampleInfo, AttributeSet> keywordsMap = new LinkedHashMap();
        Map<CompensationMatrix, AttributeSet> compMatrixMap = new LinkedHashMap();
        Map<SampleInfo, AttributeSet> analysisMap = new LinkedHashMap();
        Map<Analysis, FlowScript> scripts = new HashMap();
        for (FlowJoWorkspace.SampleInfo sample : getSamples())
        {
            AttributeSet attrs = new AttributeSet(ObjectType.fcsKeywords, null);
            if (runFilePathRoot != null)
            {
                attrs.setURI(new File(runFilePathRoot, sample.getLabel()).toURI());
            }
            attrs.setKeywords(sample.getKeywords());
            attrs.prepareForSave();
            keywordsMap.put(sample, attrs);
            AttributeSet results = getSampleAnalysisResults(sample);
            if (results != null)
            {
                results.prepareForSave();
                analysisMap.put(sample, results);
            }
        }
        Set<CompensationMatrix> compMatrices = getUsedCompensationMatrices();
        for (CompensationMatrix compMatrix : compMatrices)
        {
            AttributeSet attrs = new AttributeSet(compMatrix);
            attrs.prepareForSave();
            compMatrixMap.put(compMatrix, attrs);
        }

        boolean transaction = false;
        try
        {
            svc.beginTransaction();
            transaction = true;
            ExpRun run = svc.createExperimentRun(container, workspaceFile.getName());
            FlowProtocol flowProtocol = FlowProtocol.ensureForContainer(user, container);
            ExpProtocol protocol = flowProtocol.getProtocol();
            run.setProtocol(protocol);
            if (runFilePathRoot != null)
            {
                run.setFilePathRoot(runFilePathRoot);
            }
            run.save(user);

            ExpData workspaceData = svc.createData(container, new DataType("Flow-Workspace"));
            workspaceData.setDataFileURI(workspaceFile.toURI());
            workspaceData.setName(workspaceFile.getName());
            workspaceData.save(user);

            ExpProtocolApplication startingInputs = run.addProtocolApplication(user, null, ExpProtocol.ApplicationType.ExperimentRun);
            startingInputs.addDataInput(user, workspaceData, InputRole.Workspace.toString(), null);
            Map<FlowJoWorkspace.SampleInfo, FlowFCSFile> fcsFiles = new HashMap();
            for (FlowJoWorkspace.SampleInfo sample : getSamples())
            {
                ExpProtocolApplication paSample = run.addProtocolApplication(user, FlowProtocolStep.keywords.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication);
                paSample.addDataInput(user, workspaceData, InputRole.Workspace.toString(), InputRole.Workspace.getPropertyDescriptor(container));
                ExpData fcsFile = svc.createData(container, FlowDataType.FCSFile);
                fcsFile.setName(sample.getLabel());
                fcsFile.setDataFileURI(dataFileURI);

                fcsFile.setSourceApplication(paSample);
                fcsFile.save(user);
                fcsFiles.put(sample, new FlowFCSFile(fcsFile));
                AttributeSet attrs = keywordsMap.get(sample);
                attrs.doSave(user, fcsFile);
            }
            Map<CompensationMatrix, FlowCompensationMatrix> flowCompMatrices = new HashMap();
            for (CompensationMatrix compMatrix : compMatrices)
            {
                FlowCompensationMatrix flowComp = FlowCompensationMatrix.create(user, container, null, compMatrixMap.get(compMatrix));
                ExpProtocolApplication paComp = run.addProtocolApplication(user, FlowProtocolStep.calculateCompensation.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication);
                paComp.addDataInput(user, workspaceData, InputRole.Workspace.toString(), null);
                flowComp.getData().setSourceApplication(paComp);
                flowComp.getData().setName(compMatrix.getName() + " " + workspaceFile.getName());
                flowComp.getData().save(user);
                flowCompMatrices.put(compMatrix, flowComp);
            }
            for (Map.Entry<FlowJoWorkspace.SampleInfo, FlowFCSFile> entry : fcsFiles.entrySet())
            {
                AttributeSet results = analysisMap.get(entry.getKey());
                if (results != null)
                {
                    ExpProtocolApplication paAnalysis = run.addProtocolApplication(user,
                            FlowProtocolStep.analysis.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication);
                    FlowFCSFile fcsFile = entry.getValue();
                    results.setURI(fcsFile.getFCSURI());
                    paAnalysis.addDataInput(user, fcsFile.getData(), InputRole.FCSFile.toString(), InputRole.FCSFile.getPropertyDescriptor(container));
                    ExpData fcsAnalysis = svc.createData(container, FlowDataType.FCSAnalysis);
                    fcsAnalysis.setName(flowProtocol.getFCSAnalysisName(fcsFile));
                    fcsAnalysis.setSourceApplication(paAnalysis);
                    fcsAnalysis.setDataFileURI(dataFileURI);
                    fcsAnalysis.save(user);
                    results.doSave(user, fcsAnalysis);
                    Analysis analysis = getSampleAnalysis(entry.getKey());
                    if (analysis != null)
                    {
                        ScriptDocument scriptDoc = ScriptDocument.Factory.newInstance();
                        ScriptDef scriptDef = scriptDoc.addNewScript();
                        FlowScript script = scripts.get(analysis);
                        FlowWell well = new FlowFCSAnalysis(fcsAnalysis);
                        if (script == null)
                        {
                            FlowAnalyzer.makeAnalysisDef(scriptDef, analysis, EnumSet.of(StatisticSet.workspace, StatisticSet.count, StatisticSet.frequencyOfParent));
                            well = FlowScript.createScriptForWell(user, well, "workspaceScript" + (scripts.size() + 1), scriptDoc, workspaceData, InputRole.Workspace);
                            scripts.put(analysis, well.getScript());
                        }
                        else
                        {
                            well.getProtocolApplication().addDataInput(user, script.getData(), InputRole.AnalysisScript.toString(), null);
                        }
                    }
                    CompensationMatrix comp = entry.getKey().getCompensationMatrix();
                    if (comp != null)
                    {
                        FlowCompensationMatrix flowComp = flowCompMatrices.get(comp);
                        paAnalysis.addDataInput(user, flowComp.getData(), InputRole.CompensationMatrix.toString(), null);
                    }
                }
            }
            if (experiment != null)
            {
                experiment.getExperiment().addRun(user, run);
            }

            svc.commitTransaction();
            transaction = false;
            return new FlowRun(run);
        }
        finally
        {
            if (transaction)
            {
                svc.rollbackTransaction();
            }
        }



    }
}
