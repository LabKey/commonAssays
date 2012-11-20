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
    protected Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<ParameterInfo>(new LinkedHashMap<String, ParameterInfo>());
    protected Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<String, AttributeSet>();
    protected Map<String, SampleInfo> _sampleInfos = new CaseInsensitiveMapWrapper<SampleInfo>(new LinkedHashMap<String, SampleInfo>());
    protected List<String> _warnings = new LinkedList<String>();
    protected Set<CompensationMatrix> _compensatrionMatrices = new LinkedHashSet<CompensationMatrix>();
    protected Map<String, CompensationMatrix> _sampleCompensationMatrices = new HashMap<String, CompensationMatrix>();

    public ExternalAnalysis()
    {
    }

    public ExternalAnalysis(String path, Map<String, AttributeSet> keywords, Map<String, AttributeSet> results, Map<String, CompensationMatrix> matrices)
    {
        _path = path;
        _name = new File(path).getName();

        for (Map.Entry<String, AttributeSet> entry : keywords.entrySet())
        {
            addSampleKeywords(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, AttributeSet> entry : results.entrySet())
        {
            addSampleAnalysisResults(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, CompensationMatrix> entry : matrices.entrySet())
        {
            addSampleCompMatrix(entry.getKey(), entry.getValue());
        }
    }

    public static ExternalAnalysis readAnalysis(File file) throws IOException
    {
        VirtualFile vf = new FileSystemFile(file);
        AnalysisSerializer as = new AnalysisSerializer(LOG, vf);
        return as.readAnalysis();
    }

    private SampleInfo ensureSample(String sampleId)
    {
        SampleInfo sample = _sampleInfos.get(sampleId);
        if (sample == null)
        {
            sample = new SampleInfo();
            sample._sampleId = sampleId;
            sample._sampleName = sampleId;
            _sampleInfos.put(sampleId, sample);
        }
        return sample;
    }

    private void addSampleKeywords(String sampleId, AttributeSet keywords)
    {
        SampleInfo sample = ensureSample(sampleId);
        sample._keywords.putAll(keywords.getKeywords());
        _keywords.addAll(keywords.getKeywords().keySet());

        for (int i = 1; i < 100; i++)
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

    private void addSampleAnalysisResults(String sampleId, AttributeSet results)
    {
        SampleInfo sample = ensureSample(sampleId);
        _sampleAnalysisResults.put(sampleId, results);
    }

    private void addSampleCompMatrix(String sampleId, CompensationMatrix matrix)
    {
        SampleInfo sample = ensureSample(sampleId);
        _compensatrionMatrices.add(matrix);
        _sampleCompensationMatrices.put(sampleId, matrix);
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
        return new ArrayList<String>(_parameters.keySet());
    }

    @Override
    public List<ParameterInfo> getParameters()
    {
        return new ArrayList<ParameterInfo>(_parameters.values());
    }

    @Override
    public List<String> getSampleIds()
    {
        List<String> ret = new ArrayList<String>(_sampleInfos.size());
        for (ISampleInfo sample : _sampleInfos.values())
            ret.add(sample.getSampleId());

        return ret;
    }

    @Override
    public List<String> getSampleLabels()
    {
        List<String> ret = new ArrayList<String>(_sampleInfos.size());
        for (ISampleInfo sample : _sampleInfos.values())
            ret.add(sample.getLabel());

        return ret;
    }

    @Override
    public List<? extends ISampleInfo> getSamples()
    {
        return new ArrayList<SampleInfo>(_sampleInfos.values());
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
        return new ArrayList<CompensationMatrix>(_compensatrionMatrices);
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
