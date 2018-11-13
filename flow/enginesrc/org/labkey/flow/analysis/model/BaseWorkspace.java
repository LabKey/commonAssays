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

import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.api.collections.CaseInsensitiveTreeSet;
import org.labkey.flow.persist.AttributeSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseWorkspace implements IWorkspace, Serializable
{
    protected String _name = null;
    protected String _path = null;

    protected List<String> _warnings = new LinkedList<>();

    protected Set<String> _keywords = new CaseInsensitiveTreeSet();
    protected Map<String, ParameterInfo> _parameters = new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    // sample id -> analysis results
    protected Map<String, AttributeSet> _sampleAnalysisResults = new LinkedHashMap<>();


    @Override
    public String getName()
    {
        return _name;
    }

    public String getPath()
    {
        return _path;
    }

    public List<String> getWarnings()
    {
        return _warnings;
    }

    // NOTE: case-insensitive
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

}
