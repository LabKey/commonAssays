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

import org.apache.log4j.Logger;
import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.api.collections.CaseInsensitiveTreeSet;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.api.writer.VirtualFile;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 11/16/12
 */
public class ExternalAnalysis implements IWorkspace, Serializable
{
    public static final Logger LOG = Logger.getLogger(ExternalAnalysis.class);

    protected String _name;
    protected String _path;
    protected Set<String> _keywords = new CaseInsensitiveTreeSet();
    protected Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<String, ParameterInfo>());
    protected Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<>();
    protected Map<String, SampleInfo> _sampleInfos = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<String, SampleInfo>());
    protected List<String> _warnings = new LinkedList<>();
    protected Set<CompensationMatrix> _compensatrionMatrices = new LinkedHashSet<>();
    protected Map<String, CompensationMatrix> _sampleCompensationMatrices = new HashMap<>();

    public ExternalAnalysis()
    {
    }

    public ExternalAnalysis(String path, SampleIdMap<AttributeSet> keywords, SampleIdMap<AttributeSet> results, SampleIdMap<CompensationMatrix> matrices)
    {
        _path = path;
        _name = new File(path).getName();

        for (String id : keywords.idSet())
        {
            String name = keywords.getNameForId(id);
            AttributeSet attrs = keywords.getById(id);
            addSampleKeywords(id, name, attrs);
        }

        for (String id : results.idSet())
        {
            String name = results.getNameForId(id);
            AttributeSet attrs = results.getById(id);
            addSampleAnalysisResults(id, name, attrs);
        }

        for (String id : matrices.idSet())
        {
            String name = matrices.getNameForId(id);
            CompensationMatrix matrix = matrices.getById(id);
            addSampleCompMatrix(id, name, matrix);
        }
    }

    @Override
    public String getKindName()
    {
        return "Analysis Archive";
    }

    public static ExternalAnalysis readAnalysis(File file) throws IOException
    {
        VirtualFile vf = new FileSystemFile(file);
        AnalysisSerializer as = new AnalysisSerializer(LOG, vf);
        return as.readAnalysis();
    }

    private SampleInfo ensureSample(String id, String name)
    {
        SampleInfo sample = _sampleInfos.get(id);
        if (sample == null)
        {
            sample = new SampleInfo();
            sample._sampleId = id;
            sample._sampleName = name;
            _sampleInfos.put(id, sample);
        }
        return sample;
    }

    private void addSampleKeywords(String id, String name, AttributeSet keywords)
    {
        SampleInfo sample = ensureSample(id, name);
        sample._keywords.putAll(keywords.getKeywords());
        _keywords.addAll(keywords.getKeywords().keySet());

        for (int i = 0; i < 100; i++)
        {
            String paramName = FCSHeader.getParameterName(sample._keywords, i);
            if (paramName == null)
                break;
            if (_parameters.containsKey(paramName))
                continue;

            ParameterInfo paramInfo = ParameterInfo.fromKeywords(sample._keywords, i);
            _parameters.put(paramInfo._name, paramInfo);
        }
    }

    private void addSampleAnalysisResults(String id, String name, AttributeSet results)
    {
        SampleInfo sample = ensureSample(id, name);
        _sampleAnalysisResults.put(sample._sampleId, results);
    }

    private void addSampleCompMatrix(String id, String name, CompensationMatrix matrix)
    {
        SampleInfo sample = ensureSample(id, name);
        _compensatrionMatrices.add(matrix);
        _sampleCompensationMatrices.put(sample._sampleId, matrix);
    }

    @Override
    public String getName()
    {
        return _name;
    }

    public String getPath()
    {
        return _path;
    }

    @Override
    public List<String> getWarnings()
    {
        return _warnings;
    }

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
    public List<String> getSampleIds()
    {
        List<String> ret = new ArrayList<>(_sampleInfos.size());
        for (ISampleInfo sample : _sampleInfos.values())
            ret.add(sample.getSampleId());

        return ret;
    }

    @Override
    public List<String> getSampleLabels()
    {
        List<String> ret = new ArrayList<>(_sampleInfos.size());
        for (ISampleInfo sample : _sampleInfos.values())
            ret.add(sample.getLabel());

        return ret;
    }

    @Override
    public List<? extends ISampleInfo> getSamples()
    {
        return new ArrayList<>(_sampleInfos.values());
    }

    @Override
    public ISampleInfo getSample(String sampleIdOrLabel)
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

    @Override
    public boolean hasAnalysis()
    {
        // ExternalAnalysis doesn't store the analysis definition.
        return false;
    }

    @Override
    public Analysis getSampleAnalysis(ISampleInfo sample)
    {
        // ExternalAnalysis doesn't store the analysis definition.
        return null;
    }

    @Override
    public AttributeSet getSampleAnalysisResults(ISampleInfo sample)
    {
        return _sampleAnalysisResults.get(sample.getSampleId());
    }

    @Override
    public CompensationMatrix getSampleCompensationMatrix(ISampleInfo sample)
    {
        return _sampleCompensationMatrices.get(sample.getSampleId());
    }

    @Override
    public List<CompensationMatrix> getCompensationMatrices()
    {
        return new ArrayList<>(_compensatrionMatrices);
    }

    private class SampleInfo extends SampleInfoBase
    {
        @Override
        public Analysis getAnalysis()
        {
            return getSampleAnalysis(this);
        }

        @Override
        public AttributeSet getAnalysisResults()
        {
            return getSampleAnalysisResults(this);
        }

        @Override
        public CompensationMatrix getCompensationMatrix()
        {
            return null;
        }
    }
}
