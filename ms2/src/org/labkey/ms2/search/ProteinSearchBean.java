package org.labkey.ms2.search;

/**
 * User: jeckels
 * Date: Feb 26, 2007
 */
public class ProteinSearchBean
{
    private boolean _includeSubfolders = true;
    private Float _minProbability;
    private Float _maxErrorRate;
    private String _identifier = "";
    private boolean _horizontal;

    public ProteinSearchBean(boolean horizontal)
    {
        _horizontal = horizontal;
    }

    public boolean isHorizontal()
    {
        return _horizontal;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public void setIdentifier(String identifier)
    {
        _identifier = identifier;
    }

    public boolean isIncludeSubfolders()
    {
        return _includeSubfolders;
    }

    public void setIncludeSubfolders(boolean includeSubfolders)
    {
        _includeSubfolders = includeSubfolders;
    }

    public Float getMaxErrorRate()
    {
        return _maxErrorRate;
    }

    public void setMaxErrorRate(Float maxErrorRate)
    {
        _maxErrorRate = maxErrorRate;
    }

    public Float getMinProbability()
    {
        return _minProbability;
    }

    public void setMinProbability(Float minProbability)
    {
        _minProbability = minProbability;
    }
}
