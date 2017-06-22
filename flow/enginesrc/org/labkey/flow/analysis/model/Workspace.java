/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.api.collections.CaseInsensitiveTreeSet;
import org.labkey.flow.analysis.web.SubsetExpression;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AttributeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public abstract class Workspace implements IWorkspace, Serializable
{
    public static final String ALL_SAMPLES = "All Samples";
    
    protected String _name = null;
    protected String _path = null;
    
    // group name -> analysis
    protected Map<PopulationName, Analysis> _groupAnalyses = new LinkedHashMap<>();
    // sample id -> analysis
    protected Map<String, Analysis> _sampleAnalyses = new LinkedHashMap<>();
    protected Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<>();
    protected Map<String, GroupInfo> _groupInfos = new LinkedHashMap<>();
    protected Map<String, SampleInfo> _sampleInfos = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<String, SampleInfo>());
    protected Map<String, SampleInfo> _deletedInfos = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<String, SampleInfo>());
    protected Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<String, ParameterInfo>());
    protected List<CalibrationTable> _calibrationTables = new ArrayList<>();
    protected ScriptSettings _settings = new ScriptSettings();
    protected List<String> _warnings = new LinkedList<>();
    protected List<CompensationMatrix> _compensationMatrices = new ArrayList<>();
    protected List<AutoCompensationScript> _autoCompensationScripts = new ArrayList<>();
    protected Set<String> _keywords = new CaseInsensitiveTreeSet();

    protected Workspace()
    {
    }

    static public Workspace readWorkspace(InputStream stream) throws Exception
    {
        return readWorkspace(null, null, stream);
    }

    static public Workspace readWorkspace(File file)
    {
        InputStream is = null;
        try
        {
            is = new FileInputStream(file);
            return readWorkspace(file.getName(), file.getPath(), is);
        }
        catch (FlowException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (is != null) try { is.close(); } catch (IOException ioe) { }
        }
    }

    static public Workspace readWorkspace(String name, String path, InputStream stream) throws Exception
    {
        if (name != null && name.toLowerCase().endsWith(".fcs"))
            throw new FlowException("Please import a FlowJo workspace xml or wsp file");

        if (name != null && name.toLowerCase().endsWith(".jo"))
            throw new FlowException("Mac FlowJo .jo workspaces not supported; save the workspace as xml before importing");

        Document doc;
        try
        {
            doc = WorkspaceParser.parseXml(stream);
        }
        catch (org.xml.sax.SAXParseException e)
        {
            boolean isNotXmlFile = e.getColumnNumber() == 1 && e.getLineNumber() == 1;
            if (isNotXmlFile)
            {
                throw new FlowException("Please import a FlowJo workspace xml or wsp file");
            }
            else
            {
                throw new FlowException("The selected file is invalid at line " +
                        e.getLineNumber() + ", column " + e.getColumnNumber() +
                        ". " + e.getMessage());
            }
        }
        Element elDoc = doc.getDocumentElement();
//        System.err.println("DOCUMENT SIZE: " + debugComputeSize(elDoc));

        // Issue 20074: provide better error message when attempting to import a diva xml file
        String tag = elDoc.getTagName();
        if (tag != null && tag.equals("bdfacs"))
            throw new FlowException("BD FACSDiva XML files not yet supported; please contact support@labkey.com for assistance.");

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
                return new PCWorkspace(name, path, elDoc);
            }
            else if (version >= 1.6 && version < 1.8)
            {
                // Version 1.6 (FlowJo 7.5.5) changes:
                // - GatingML 1.5
                return new PC75Workspace(name, path, elDoc);
            }
            else if ((version >= 1.8 && version < 2.0) || version == 20.0)
            {
                // Version 1.8 (FlowJo 10.0.6) changes:
                // - GatingML 2.0

                // Version 20.0 (FlowJo 10.0.7) changes:
                // - Adds plate editor, moves per-sample Cytometer settings to top-level
                return new FlowJo10_0_6Workspace(name, path, elDoc);
            }

            if (version == 2.0)
            {
                return new Mac2Workspace(name, path, elDoc);
            }
            else if (version == 3.0)
            {
                // Version 3.0 introduced in FlowJo v9.7
                return new Mac3Workspace(name, path, elDoc);
            }
        }

        if (name != null && (name.endsWith(".wsp") || name.endsWith(".WSP")))
        {
            return new PCWorkspace(name, path, elDoc);
        }

        return new MacWorkspace(name, path, elDoc);
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

    public String getName()
    {
        return _name;
    }

    public String getPath()
    {
        return _path;
    }

    public ScriptSettings getSettings()
    {
        return _settings;
    }

    public CompensationMatrix getSampleCompensationMatrix(ISampleInfo sample)
    {
        return sample.getCompensationMatrix();
    }

    public List<CompensationMatrix> getCompensationMatrices()
    {
        return _compensationMatrices;
    }

    public Set<CompensationMatrix> getUsedCompensationMatrices()
    {
        Set<CompensationMatrix> ret = new LinkedHashSet<>();
        for (SampleInfo sample : getSamplesComplete())
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
        return new ArrayList<>(_groupInfos.values());
    }

    public GroupInfo getGroup(String groupId)
    {
        return _groupInfos.get(groupId);
    }

    public GroupInfo getAllSamplesGroup()
    {
        GroupInfo allSamplesGroup = getGroup("0");
        if (allSamplesGroup == null || !allSamplesGroup.isAllSamples())
        {
            for (GroupInfo groupInfo : getGroups())
            {
                if (groupInfo.isAllSamples())
                {
                    allSamplesGroup = groupInfo;
                    break;
                }
            }
        }

        return allSamplesGroup;
    }

    public Analysis getGroupAnalysis(GroupInfo group)
    {
        return _groupAnalyses.get(group.getGroupName());
    }

    public Map<PopulationName, Analysis> getGroupAnalyses()
    {
        return _groupAnalyses;
    }

    /**
     * Get all samples in the workspace, including samples that are no longer referenced by any group.
     * Usually using .getSamples() is preferred.
     * After deleting samples from a FlowJo workspace, the workspace may retain the sample info and just
     * remove it from the "All Samples" group.
     */
    public List<SampleInfo> getSamplesComplete()
    {
        return new ArrayList<>(_sampleInfos.values());
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
            return new LinkedHashSet<>(getSamplesComplete());

        Set<Workspace.SampleInfo> sampleInfos = new LinkedHashSet<>();
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
            for (Workspace.SampleInfo sampleInfo : getSamplesComplete())
            {
                if (sampleNames.contains(sampleInfo.getSampleId()) || sampleNames.contains(sampleInfo.getLabel()))
                    sampleInfos.add(sampleInfo);
            }
        }

        return sampleInfos;
    }

    /** Get the sample list from the "All Samples" group or get all the samples in the workspace. */
    public List<SampleInfo> getSamples()
    {
        GroupInfo allSamplesGroup = getAllSamplesGroup();

        List<SampleInfo> allSamples = null;
        if (allSamplesGroup != null)
            allSamples = allSamplesGroup.getSampleInfos();

        // No "All Samples" group found or it was empty. Return all sample IDs in the workspace.
        if (allSamples == null || allSamples.size() == 0)
            allSamples = getSamplesComplete();

        return allSamples;
    }

    /** Get the sample ID list from the "All Samples" group or get all the samples in the workspace. */
    public List<String> getSampleIds()
    {
        List<SampleInfo> allSamples = getSamples();
        if (allSamples == null)
            return Collections.emptyList();

        List<String> allSampleIds = new ArrayList<>(allSamples.size());
        for (SampleInfo sample : allSamples)
            allSampleIds.add(sample.getSampleId());

        return allSampleIds;
    }

    /** Get the sample label list from the "All Samples" group or get all the samples in the workspace. */
    public List<String> getSampleLabels()
    {
        List<SampleInfo> allSamples = getSamples();
        if (allSamples == null || allSamples.size() == 0)
            return Collections.emptyList();

        List<String> allSampleLabels = new ArrayList<>(allSamples.size());
        for (SampleInfo sample : allSamples)
            allSampleLabels.add(sample.getLabel());

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
        SampleInfo sample = findSample(_sampleInfos, sampleIdOrLabel);
        assert sample == null || !sample.isDeleted();
        return sample;
    }

    public SampleInfo getDeletedSample(String sampleIdOrLabel)
    {
        SampleInfo sample = findSample(_deletedInfos, sampleIdOrLabel);
        assert sample == null || sample.isDeleted();
        return sample;
    }

    private SampleInfo findSample(Map<String, SampleInfo> samples, String sampleIdOrLabel)
    {
        SampleInfo sample = samples.get(sampleIdOrLabel);
        if (sample != null)
            return sample;

        for (SampleInfo sampleInfo : samples.values())
        {
            if (sampleIdOrLabel.equals(sampleInfo.getLabel()))
                return sampleInfo;
        }

        return null;
    }

    public boolean hasAnalysis()
    {
        return true;
    }

    public Analysis getSampleAnalysis(ISampleInfo sample)
    {
        return _sampleAnalyses.get(sample.getSampleId());
    }

    public AttributeSet getSampleAnalysisResults(ISampleInfo sample)
    {
        return _sampleAnalysisResults.get(sample.getSampleId());
    }

    public List<String> getParameterNames()
    {
        return new ArrayList<>(_parameters.keySet());
    }

    public List<ParameterInfo> getParameters()
    {
        return new ArrayList<>(_parameters.values());
    }

    public SampleInfo findSampleWithKeywordValue(String keyword, String value)
    {
        for (SampleInfo sample : getSamplesComplete())
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

    public Set<String> getKeywords()
    {
        return Collections.unmodifiableSet(_keywords);
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

    public class SampleInfo extends SampleInfoBase
    {
        String _compensationId;

        public void setDeleted(boolean deleted)
        {
            _deleted = deleted;
        }

        public void setSampleId(String id)
        {
            _sampleId = id;
        }
        public void setSampleName(String name)
        {
            _sampleName = name;
        }
        public void putKeyword(String keyword, String value)
        {
            // FCS format encodes empty keyword values as a single space character -- convert it to null.
            value = StringUtils.trimToNull(value);
            _keywords.put(keyword, value);
            Workspace.this._keywords.add(keyword);
        }
        public void putAllKeywords(Map<String, String> keywords)
        {
            _keywords.putAll(keywords);
            Workspace.this._keywords.addAll(keywords.keySet());
        }

        public String getCompensationId()
        {
            return _compensationId;
        }

        public void setCompensationId(String id)
        {
            _compensationId = id;
        }

        public Analysis getAnalysis()
        {
            return getSampleAnalysis(this);
        }

        public AttributeSet getAnalysisResults()
        {
            return getSampleAnalysisResults(this);
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

        public List<PopulationName> getGroupNames()
        {
            List<GroupInfo> groups = getGroups();
            List<PopulationName> groupNames = new ArrayList<>(groups.size());
            for (GroupInfo group : groups)
                groupNames.add(group.getGroupName());
            return groupNames;
        }

        public List<GroupInfo> getGroups()
        {
            List<GroupInfo> groups = new ArrayList<>(4);
            for (GroupInfo group : _groupInfos.values())
            {
                if (group.getSampleIds().contains(getSampleId()))
                    groups.add(group);
            }
            return groups;
        }

    }

    public class GroupInfo implements Serializable
    {
        String _groupId;
        PopulationName _groupName;
        List<String> _sampleIds = new ArrayList<>();

        public boolean isAllSamples()
        {
            return ALL_SAMPLES.equalsIgnoreCase(_groupName.toString());
        }

        public List<String> getSampleIds()
        {
            if (_sampleIds.size() == 0 && isAllSamples())
                return new ArrayList<>(Workspace.this._sampleInfos.keySet());
            return _sampleIds;
        }

        public List<SampleInfo> getSampleInfos()
        {
            List<String> sampleIds = getSampleIds();
            ArrayList<SampleInfo> sampleInfos = new ArrayList<>(sampleIds.size());
            for (String sampleId : sampleIds)
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

    @Override
    public String toString()
    {
        return "[" + getKindName() + ": " +
                "name: " + getName() + ", " +
                "path: " + getPath() + ", " +
                "samples: " + getSampleCount() +
                "]"
                ;
    }
}
