/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.flow.analysis.web;

import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.net.URI;

public class FCSRef
{
    URI _uri;

    CaseInsensitiveHashMap<String> _keywordOverrides;

    public FCSRef(URI uri, CaseInsensitiveHashMap<String> keywordOverrides)
    {
        _uri = uri;
        _keywordOverrides = keywordOverrides;
    }

    public URI getURI()
    {
        return _uri;
    }

    // NOTE: case-insensitive
    public String getKeyword(String key)
    {
        if (_keywordOverrides == null)
            return null;
        return _keywordOverrides.get(key);
    }

    public String[] getKeywordNames()
    {
        return _keywordOverrides.keySet().toArray(new String[0]);
    }
}
