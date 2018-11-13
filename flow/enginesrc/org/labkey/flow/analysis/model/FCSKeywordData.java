/*
 * Copyright (c) 2005-2018 LabKey Corporation
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

import org.labkey.flow.analysis.web.FCSRef;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FCSKeywordData
{
    FCSRef _ref;
    FCSHeader _header;

    public FCSKeywordData(FCSRef ref, FCSHeader header)
    {
        _ref = ref;
        _header = header;
    }

    public Date getDateTime()
    {
        return _header.getDateTime();
    }

    public String getKeyword(String key)
    {
        String ret = _ref.getKeyword(key);
        if (ret != null)
            return ret;
        ret = _header.getKeywords().get(key);
        if (ret != null)
            return ret;
        for (Map.Entry<String,String> entry : _header.getKeywords().entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(key))
                return entry.getValue();
        }
        return null;
    }

    public String[] getKeywordNames()
    {
        return _header.getKeywords().keySet().toArray(new String[0]);
    }

    // NOTE: case-insensitive
    public Map<String,String> getAllKeywords()
    {
        HashMap<String,String> ret = new CaseInsensitiveHashMap<>();
        for (String override : _ref.getKeywordNames())
        {
            ret.put(override, _ref.getKeyword(override));
        }
        for (Map.Entry<String, String> entry : _header.getKeywords().entrySet())
        {
            if (ret.containsKey(entry.getKey()))
                continue;
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    public URI getURI()
    {
        return _ref.getURI();
    }
}
