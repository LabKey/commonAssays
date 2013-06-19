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
        if (ret == null)
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
        String name = _sampleName;
        if (name == null || name.length() == 0)
            name = getFilename();
        if (name == null)
            return _sampleId;

        return name + " (" + _sampleId + ")";
    }
}
