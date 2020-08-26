/*
 * Copyright (c) 2018 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.api.collections.CaseInsensitiveTreeSet;
import org.labkey.flow.persist.AttributeSet;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public abstract class BaseWorkspace<S extends ISampleInfo> implements IWorkspace, Serializable
{
    protected String _name = null;
    protected String _path = null;

    protected final List<String> _warnings = new LinkedList<>();

    protected final Set<String> _keywords = new CaseInsensitiveTreeSet();
    protected final Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    // sample id -> analysis results
    protected final Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<>();

    // sample id -> SampleInfo
    protected final Map<String, S> _sampleInfos = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    // sample id -> SampleInfo
    protected final Map<String, S> _deletedInfos = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    // sample label -> list of sample id
    protected final Map<String, List<String>> _sampleLabelToIds = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    // list of sample labels associated with different sample id
    protected final List<String> _duplicateSampleLabels = new ArrayList<>();


    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public String getPath()
    {
        return _path;
    }

    @Override
    public List<String> getWarnings()
    {
        return _warnings;
    }

    // NOTE: case-insensitive
    @Override
    public Set<String> getKeywords()
    {
        return Collections.unmodifiableSet(_keywords);
    }

    @Override
    public List<String> getParameterNames()
    {
        return new ArrayList<>(_parameters.keySet());
    }

    @Override
    public List<ParameterInfo> getParameters()
    {
        return new ArrayList<>(_parameters.values());
    }

    @Override
    public AttributeSet getSampleAnalysisResults(ISampleInfo sample)
    {
        return _sampleAnalysisResults.get(sample.getSampleId());
    }

    public int getSampleCount()
    {
        return _sampleInfos.size();
    }

    @Override
    public List<S> getSamples()
    {
        return List.copyOf(_sampleInfos.values());
    }

    /**
     * Get all samples in the workspace, including samples that are no longer referenced by any group.
     * Usually using .getSamples() is preferred.
     * After deleting samples from a FlowJo workspace, the workspace may retain the sample info and just
     * remove it from the "All Samples" group.
     */
    public List<S> getSamplesComplete()
    {
        return List.copyOf(_sampleInfos.values());
    }

    /**
     * Get all sample IDs in the workspace, including samples that are no longer referenced by any group.
     */
    protected List<String> getSampleIdsComplete()
    {
        return List.copyOf(_sampleInfos.keySet());
    }

    /**
     * Get SampleInfo by workspace sample ID.
     */
    @Override
    public S getSampleById(String sampleId)
    {
        S sample = _sampleInfos.get(sampleId);
        assert sample == null || !sample.isDeleted();
        return sample;
    }

    /**
     * Get SampleInfos by workspace label (typically the FCS filename, $FIL keyword)
     */
    @NotNull
    @Override
    public List<S> getSampleByLabel(String label)
    {
        List<String> ids = _sampleLabelToIds.get(label);
        if (ids == null)
            return Collections.emptyList();
        return ids.stream().map(id -> _sampleInfos.get(id)).collect(Collectors.toList());
    }

    public S getDeletedSampleById(String sampleId)
    {
        S sample = _deletedInfos.get(sampleId);
        assert sample == null || sample.isDeleted();
        return sample;
    }

    @NotNull
    @Override
    public List<String> getDuplicateSampleLabels()
    {
        return unmodifiableList(_duplicateSampleLabels);
    }

    // Add the sample and check for duplicates sample IDs
    protected void addSample(S sampleInfo)
    {
        S existing;
        if (sampleInfo.isDeleted())
            existing = _deletedInfos.put(sampleInfo.getSampleId(), sampleInfo);
        else
            existing = _sampleInfos.put(sampleInfo.getSampleId(), sampleInfo);
        if (existing != null)
            throw new FlowException("Sample '" + sampleInfo + "' and '" + existing + "' have the same sample id");

        if (!sampleInfo.isDeleted())
        {
            var sampleIds = _sampleLabelToIds.computeIfAbsent(sampleInfo.getLabel(), label -> new ArrayList<>());
            sampleIds.add(sampleInfo.getSampleId());
            if (sampleIds.size() == 2)
                _duplicateSampleLabels.add(sampleInfo.getLabel());
        }
    }

}
