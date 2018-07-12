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
