package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.web.FCSRef;

import java.util.Map;
import java.util.HashMap;
import java.net.URI;

public class FCSKeywordData
{
    FCSRef _ref;
    FCSHeader _header;
    public FCSKeywordData(FCSRef ref, FCSHeader header)
    {
        _ref = ref;
        _header = header;
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
    public Map<String,String> getAllKeywords()
    {
        HashMap<String,String> ret = new HashMap();
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
