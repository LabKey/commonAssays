/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.flow.controllers.executescript;

import org.apache.commons.collections4.FactoryUtils;
import org.apache.commons.collections4.MapUtils;
import org.labkey.flow.analysis.model.ISampleInfo;
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
public class SelectedSamples
{
    // non-form posted values used to initialize the SamplesConfirmGridView
    private Set<String> _keywords;
    private List<? extends ISampleInfo> _samples;

    // form posted values
    // workspace sample id -> resolved info
    private Map<String, ResolvedSample> _rows = MapUtils.lazyMap(new HashMap<>(), FactoryUtils.instantiateFactory(ResolvedSample.class));

    public static class ResolvedSample
    {
        private boolean _selected;
        // FlowFCSFile rowid (may be 0 or null if there is no match)
        private Integer _matchedFile;

        // FlowFCSFile rowid (may be null if there are no candidates)
        private int[] _candidateFile;
        private List<FlowFCSFile> _candidateFCSFiles;

        public ResolvedSample()
        {
        }

        public ResolvedSample(boolean selected, int matchedFile, List<FlowFCSFile> candidateFCSFiles)
        {
            _selected = selected;
            _matchedFile = matchedFile;
            if (candidateFCSFiles == null)
            {
                _candidateFCSFiles = null;
                _candidateFile = null;
            }
            else
            {
                _candidateFCSFiles = candidateFCSFiles;
                _candidateFile = new int[candidateFCSFiles.size()];
                for (int i = 0, len = candidateFCSFiles.size(); i < len; i++)
                    _candidateFile[i] = candidateFCSFiles.get(i).getRowId();
            }
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

        public int[] getCandidateFile()
        {
            return _candidateFile;
        }

        public List<FlowFCSFile> getCandidateFCSFiles()
        {
            if (_candidateFCSFiles == null && _candidateFile != null)
            {
                _candidateFCSFiles = FlowFCSFile.fromWellIds(_candidateFile);
            }
            return _candidateFCSFiles;
        }

        public void setCandidateFile(int[] candidateFile)
        {
            _candidateFile = candidateFile;
        }
    }

    public SelectedSamples()
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

    public List<? extends ISampleInfo> getSamples()
    {
        return _samples;
    }

    public void setSamples(List<? extends ISampleInfo> samples)
    {
        _samples = samples;
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

        Map<String, String> hidden = new HashMap<>();
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
