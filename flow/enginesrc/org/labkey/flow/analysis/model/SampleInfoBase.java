/*
 * Copyright (c) 2012-2018 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveTreeMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * User: kevink
 * Date: 11/16/12
 */
public abstract class SampleInfoBase implements ISampleInfo, Serializable
{
    protected final Map<String, String> _keywords = new CaseInsensitiveTreeMap<>();
    protected final @NotNull String _sampleId;
    protected final @Nullable String _sampleName;
    protected final boolean _deleted;

    protected SampleInfoBase(@NotNull String sampleId, @Nullable String sampleName)
    {
        this(sampleId, sampleName, false);
    }

    protected SampleInfoBase(@NotNull String sampleId, @Nullable String sampleName, boolean deleted)
    {
        this._sampleId = sampleId;
        this._sampleName = sampleName;
        this._deleted = deleted;
    }

    @Override
    public boolean isDeleted()
    {
        return _deleted;
    }

    @Override
    public String getSampleId()
    {
        return _sampleId;
    }

    @Override
    public @Nullable String getSampleName()
    {
        return _sampleName;
    }

    @Override
    public @Nullable String getFilename()
    {
        return getKeywords().get("$FIL");
    }

    @Override
    public @NotNull String getLabel()
    {
        String ret = _sampleName;
        if (ret == null || ret.length() == 0)
            ret = getFilename();
        if (ret == null || ret.length() == 0)
            return _sampleId;
        return ret;
    }

    // NOTE: case-insensitive
    @Override
    public Map<String, String> getKeywords()
    {
        return Collections.unmodifiableMap(_keywords);
    }

    public void putKeyword(String keywordName, String keywordValue)
    {
        _keywords.put(keywordName, keywordValue);
    }

    public void putAllKeywords(Map<String, String> keywords)
    {
        _keywords.putAll(keywords);
    }

    public String toString()
    {
        String label = getLabel();
        if (label.equals(_sampleId))
            return label;
        else
            return label + " (" + _sampleId + ")";
    }
}
