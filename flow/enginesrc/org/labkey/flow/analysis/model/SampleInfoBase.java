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
    protected Map<String, String> _keywords = new CaseInsensitiveTreeMap<String>();
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
