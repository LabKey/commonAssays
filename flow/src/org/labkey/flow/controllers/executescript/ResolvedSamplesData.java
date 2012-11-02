package org.labkey.flow.controllers.executescript;

import org.apache.commons.collections15.FactoryUtils;
import org.apache.commons.collections15.MapUtils;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.data.FlowFCSFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/21/12
 */
public class ResolvedSamplesData
{
    // non-form posted values used to initialize the ResolvedConfirmGridView
    private Set<String> _keywords;
    private List<Workspace.SampleInfo> _samples;
    private Map<Workspace.SampleInfo, FlowFCSFile> _resolved;

    // form posted values
    // workspace sample id -> resolved info
    private Map<String, ResolvedSample> _rows = MapUtils.lazyMap(new HashMap<String, ResolvedSample>(), FactoryUtils.instantiateFactory(ResolvedSample.class));

    public static class ResolvedSample
    {
        private boolean _selected;
        // FlowFCSFile rowid
        private Integer _matchedFile;

        public ResolvedSample()
        {
        }

        public ResolvedSample(boolean selected, int matchedFile)
        {
            _selected = selected;
            _matchedFile = matchedFile;
        }

        public boolean isSelected()
        {
            return _selected;
        }

        public void setSelected(boolean selected)
        {
            _selected = selected;
        }

        public Integer getMatchedFile()
        {
            return _matchedFile;
        }

        public void setMatchedFile(Integer matchedFile)
        {
            _matchedFile = matchedFile;
        }

        public boolean hasMatchedFile()
        {
            return _matchedFile != null && _matchedFile > 0;
        }
    }

    public ResolvedSamplesData()
    {
    }

    public Set<String> getKeywords()
    {
        return _keywords;
    }

    public void setKeywords(Set<String> keywords)
    {
        _keywords = keywords;
    }

    public List<Workspace.SampleInfo> getSamples()
    {
        return _samples;
    }

    public void setSamples(List<Workspace.SampleInfo> samples)
    {
        _samples = samples;
    }

    public Map<Workspace.SampleInfo, FlowFCSFile> getResolved()
    {
        return _resolved;
    }

    public void setResolved(Map<Workspace.SampleInfo, FlowFCSFile> resolved)
    {
        _resolved = resolved;
    }

    public void setRows(Map<String, ResolvedSample> rows)
    {
        _rows = rows;
    }

    public Map<String, ResolvedSample> getRows()
    {
        return _rows;
    }

    public Map<String, String> getHiddenFields()
    {
        if (_rows.isEmpty())
            return Collections.emptyMap();

        Map<String, String> hidden = new HashMap<String, String>();
        for (Map.Entry<String, ResolvedSample> entry : _rows.entrySet())
        {
            String sampleId = entry.getKey();
            ResolvedSample resolvedSample = entry.getValue();
            hidden.put("rows[" + sampleId + "].selected", String.valueOf(resolvedSample.isSelected()));
            hidden.put("rows[" + sampleId + "].matchedFile", resolvedSample.hasMatchedFile() ? String.valueOf(resolvedSample.getMatchedFile()) : "");
        }
        return hidden;
    }
}
