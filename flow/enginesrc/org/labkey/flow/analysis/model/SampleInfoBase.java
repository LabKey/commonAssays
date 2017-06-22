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
    protected Map<String, String> _keywords = new CaseInsensitiveTreeMap<>();
    protected String _sampleId;
    protected String _sampleName;
    protected boolean _deleted = false;

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
    public String getSampleName()
    {
        return _sampleName;
    }

    @Override
    public String getFilename()
    {
        return getKeywords().get("$FIL");
    }

    @Override
    public String getLabel()
    {
        String ret = _sampleName;
        if (ret == null || ret.length() == 0)
            ret = getFilename();
        if (ret == null || ret.length() == 0)
            return _sampleId;
        return ret;
    }

    @Override
    public Map<String, String> getKeywords()
    {
        return Collections.unmodifiableMap(_keywords);
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
