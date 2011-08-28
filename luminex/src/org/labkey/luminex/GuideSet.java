package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSet
{
    private int _rowId;
    private int _protocolId;
    private String _analyteName;
    private String _conjugate;
    private String _isotype;
    private boolean _currentGuideSet;
    private Double _maxFIAverage;
    private Double _maxFIStdDev;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(int protocolId)
    {
        _protocolId = protocolId;
    }

    public String getAnalyteName()
    {
        return _analyteName;
    }

    public void setAnalyteName(String analyteName)
    {
        _analyteName = analyteName;
    }

    public String getConjugate()
    {
        return _conjugate;
    }

    public void setConjugate(String conjugate)
    {
        _conjugate = conjugate;
    }

    public String getIsotype()
    {
        return _isotype;
    }

    public void setIsotype(String isotype)
    {
        _isotype = isotype;
    }

    public Double getMaxFIAverage()
    {
        return _maxFIAverage;
    }

    public void setMaxFIAverage(Double maxFIAverage)
    {
        _maxFIAverage = maxFIAverage;
    }

    public Double getMaxFIStdDev()
    {
        return _maxFIStdDev;
    }

    public void setMaxFIStdDev(Double maxFIStdDev)
    {
        _maxFIStdDev = maxFIStdDev;
    }

    public boolean isCurrentGuideSet()
    {
        return _currentGuideSet;
    }

    public void setCurrentGuideSet(boolean currentGuideSet)
    {
        _currentGuideSet = currentGuideSet;
    }
}
