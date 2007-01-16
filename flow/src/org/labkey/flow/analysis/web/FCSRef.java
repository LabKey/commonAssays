package org.labkey.flow.analysis.web;

import java.util.Map;
import java.net.URI;

public class FCSRef
{
    URI _uri;
    Map<String,String> _keywordOverrides;
    public FCSRef(URI uri, Map<String, String> keywordOverrides)
    {
        _uri = uri;
        _keywordOverrides = keywordOverrides;
    }

    public URI getURI()
    {
        return _uri;
    }

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
