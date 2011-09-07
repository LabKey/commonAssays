package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Sep 6, 2011
 */
public class AnalyteTitration
{
    private int _analyteId;
    private int _titrationId;
    private double _maxFI;
    private Integer _guideSetId;
    private boolean _includeInGuideSetCalculation;

    public int getAnalyteId()
    {
        return _analyteId;
    }

    public void setAnalyteId(int analyteId)
    {
        _analyteId = analyteId;
    }

    public int getTitrationId()
    {
        return _titrationId;
    }

    public void setTitrationId(int titrationId)
    {
        _titrationId = titrationId;
    }

    public double getMaxFI()
    {
        return _maxFI;
    }

    public void setMaxFI(double maxFI)
    {
        _maxFI = maxFI;
    }

    public void setGuideSetId(Integer guideSetId)
    {
        _guideSetId = guideSetId;
    }

    public Integer getGuideSetId()
    {
        return _guideSetId;
    }

    public boolean isIncludeInGuideSetCalculation()
    {
        return _includeInGuideSetCalculation;
    }

    public void setIncludeInGuideSetCalculation(boolean includeInGuideSetCalculation)
    {
        _includeInGuideSetCalculation = includeInGuideSetCalculation;
    }

}
