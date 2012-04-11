/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.flow.analysis.web.SubsetExpression;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 2/8/12
 */
public abstract class Workspace implements Serializable
{
    public static final String ALL_SAMPLES = "All Samples";
    
    protected String _name = null;
    // group name -> analysis
    protected Map<PopulationName, Analysis> _groupAnalyses = new LinkedHashMap<PopulationName, Analysis>();
    // sample id -> analysis
    protected Map<String, Analysis> _sampleAnalyses = new LinkedHashMap<String, Analysis>();
    protected Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<String, AttributeSet>();
    protected Map<String, GroupInfo> _groupInfos = new LinkedHashMap<String, GroupInfo>();
    protected Map<String, SampleInfo> _sampleInfos = new CaseInsensitiveMapWrapper<SampleInfo>(new LinkedHashMap<String, SampleInfo>());
    protected Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<ParameterInfo>(new LinkedHashMap<String, ParameterInfo>());
    protected List<CalibrationTable> _calibrationTables = new ArrayList<CalibrationTable>();
    protected ScriptSettings _settings = new ScriptSettings();
    protected List<String> _warnings = new LinkedList<String>();
    protected List<CompensationMatrix> _compensationMatrices = new ArrayList<CompensationMatrix>();
    protected List<AutoCompensationScript> _autoCompensationScripts = new ArrayList<AutoCompensationScript>();

    protected Workspace()
    {
    }

    static public Workspace readWorkspace(InputStream stream) throws Exception
    {
        return readWorkspace(null, stream);
    }

    static public Workspace readWorkspace(String name, InputStream stream) throws Exception
    {
        Document doc = WorkspaceParser.parseXml(stream);
        Element elDoc = doc.getDocumentElement();
//        System.err.println("DOCUMENT SIZE: " + debugComputeSize(elDoc));
        String versionString = elDoc.getAttribute("version");
        double version = 0;
        try
        {
            if (versionString != null && versionString.length() > 0)
                version = Double.parseDouble(versionString);
        }
        catch (NumberFormatException nfe)
        {
            // ignore
        }

        if (version > 0)
        {
            if (version >= 1.4 && version < 1.6)
            {
                return new PCWorkspace(name, elDoc);
            }
            else if (version < 2.0)
            {
                // GatingML appears in version >= 1.6 (FlowJo version 7.5.5)
                return new PC75Workspace(name, elDoc);
            }

            if (version == 2.0)
            {
                return new FJ8Workspace(name, elDoc);
            }
        }

        if (name != null && (name.endsWith(".wsp") || name.endsWith(".WSP")))
        {
            return new PCWorkspace(name, elDoc);
        }

        return new MacWorkspace(name, elDoc);
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

    public ScriptSettings getSettings()
    {
        return _settings;
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

    public List<GroupInfo> getGroups()
    {
        return new ArrayList<GroupInfo>(_groupInfos.values());
    }

    public GroupInfo getGroup(String groupId)
    {
        return _groupInfos.get(groupId);
    }

    public Analysis getGroupAnalysis(GroupInfo group)
    {
        return _groupAnalyses.get(group.getGroupName());
    }

    public Map<PopulationName, Analysis> getGroupAnalyses()
    {
        return _groupAnalyses;
    }

    public List<SampleInfo> getSamples()
    {
        return new ArrayList<SampleInfo>(_sampleInfos.values());
    }

    /**
     * Get a Set of SampleInfos from any group names or sample IDs or names that match.
     *
     * @param groupNames A set of group IDs or group names.
     * @param sampleNames A set of sample IDs or sample names.
     * @return Set of SampleInfo.
     */
    public Set<SampleInfo> getSamples(Collection<PopulationName> groupNames, Collection<String> sampleNames)
    {
        if (groupNames.isEmpty() && sampleNames.isEmpty())
            return new LinkedHashSet<SampleInfo>(getSamples());

        Set<Workspace.SampleInfo> sampleInfos = new LinkedHashSet<Workspace.SampleInfo>();
        if (!groupNames.isEmpty())
        {
            for (Workspace.GroupInfo group : getGroups())
            {
                // TODO: refactor GroupInfo to just use Strings as names
                PopulationName groupName = group.getGroupName();
                PopulationName groupId = PopulationName.fromString(group.getGroupId());
                if (groupNames.contains(groupId) || groupNames.contains(groupName))
                {
                    for (String sampleID : group.getSampleIds())
                        sampleInfos.add(getSample(sampleID));
                }
            }
        }

        if (!sampleNames.isEmpty())
        {
            for (Workspace.SampleInfo sampleInfo : getSamples())
            {
                if (sampleNames.contains(sampleInfo.getSampleId()) || sampleNames.contains(sampleInfo.getLabel()))
                    sampleInfos.add(sampleInfo);
            }
        }

        return sampleInfos;
    }

    /** Get the sample ID list from the "All Samples" group or get all the samples in the workspace. */
    public List<String> getAllSampleIDs()
    {
        GroupInfo allSamplesGroup = getGroup("0");
        if (allSamplesGroup == null || !allSamplesGroup.getGroupName().toString().equalsIgnoreCase(ALL_SAMPLES))
        {
            for (GroupInfo groupInfo : getGroups())
            {
                if (groupInfo.getGroupName().toString().equalsIgnoreCase(ALL_SAMPLES))
                {
                    allSamplesGroup = groupInfo;
                    break;
                }
            }
        }

        List<String> allSampleIDs = null;
        if (allSamplesGroup != null)
            allSampleIDs = allSamplesGroup.getSampleIds();

        // No "All Samples" group found or it was empty. Return all sample IDs in the workspace.
        if (allSampleIDs == null || allSampleIDs.size() == 0)
            allSampleIDs = new ArrayList<String>(_sampleInfos.keySet());

        return allSampleIDs;
    }

    /** Get the sample label list from the "All Samples" group or get all the samples in the workspace. */
    public List<String> getAllSampleLabels()
    {
        List<String> allSampleIDs = getAllSampleIDs();
        if (allSampleIDs == null || allSampleIDs.size() == 0)
            return null;

        List<String> allSampleLabels = new ArrayList<String>(allSampleIDs.size());
        for (String sampleID : allSampleIDs)
        {
            SampleInfo sampleInfo = getSample(sampleID);
            if (sampleInfo != null)
                allSampleLabels.add(sampleInfo.getLabel());
        }
        return allSampleLabels;
    }

    public int getSampleCount()
    {
        return _sampleInfos.size();
    }

    /**
     * Get SampleInfo by either workspace sample ID or by FCS filename ($FIL keyword.)
     * @param sampleIdOrLabel Sample ID or FCS filename.
     * @return SampleInfo
     */
    public SampleInfo getSample(String sampleIdOrLabel)
    {
        SampleInfo sample = _sampleInfos.get(sampleIdOrLabel);
        if (sample != null)
            return sample;

        for (SampleInfo sampleInfo : _sampleInfos.values())
        {
            if (sampleIdOrLabel.equals(sampleInfo.getLabel()))
                return sampleInfo;
        }

        return null;
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

    public SampleInfo findSampleWithKeywordValue(String keyword, String value)
    {
        for (SampleInfo sample : getSamples())
        {
            if (value.equals(sample._keywords.get(keyword)))
                return sample;
        }
        return null;
    }

    protected Analysis findAnalysisWithKeywordValue(String keyword, String value, List<String> errors)
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

    protected Population findPopulation(PopulationSet calc, SubsetSpec spec)
    {
        PopulationSet cur = calc;
        for (SubsetPart term : spec.getSubsets())
        {
            if (cur == null)
                return null;
            if (term instanceof PopulationName)
                cur = cur.getPopulation((PopulationName)term);
            else if (term instanceof SubsetExpression)
                assert false;
        }
        return (Population) cur;
    }

    public List<String> getWarnings()
    {
        return _warnings;
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

    public class SampleInfo implements Serializable
    {
        Map<String, String> _keywords = new HashMap<String, String>();
        Map<String, Workspace.ParameterInfo> _parameters;
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

        /** Returns true if the sample has already been compensated by the flow cytometer. */
        public boolean isPrecompensated()
        {
            return getSpill() != null;
        }

        /** Returns the spill matrix. */
        public CompensationMatrix getSpill()
        {
            if (_compensationId == null)
                return null;

            int id = Integer.parseInt(_compensationId);
            if (id < 0)
                return CompensationMatrix.fromSpillKeyword(_keywords);

            return null;
        }

        /** Returns the spill matrix or FlowJo applied comp matrix. */
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

        public String toString()
        {
            String $FIL = getKeywords().get("$FIL");
            if ($FIL == null)
                return _sampleId;

            return $FIL + " (" + _sampleId + ")";
        }
    }

    public class GroupInfo implements Serializable
    {
        String _groupId;
        PopulationName _groupName;
        List<String> _sampleIds = new ArrayList<String>();

        public List<String> getSampleIds()
        {
            return _sampleIds;
        }

        public List<SampleInfo> getSampleInfos()
        {
            ArrayList<SampleInfo> sampleInfos = new ArrayList<SampleInfo>(_sampleIds.size());
            for (String sampleId : _sampleIds)
            {
                SampleInfo sampleInfo = getSample(sampleId);
                if (sampleInfo != null)
                    sampleInfos.add(sampleInfo);
            }
            return sampleInfos;
        }

        public String getGroupId()
        {
            return _groupId;
        }

        public void setGroupId(String groupId)
        {
            _groupId = groupId;
        }

        public PopulationName getGroupName()
        {
            return _groupName;
        }

        public void setGroupName(PopulationName groupName)
        {
            _groupName = groupName;
        }
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
}
