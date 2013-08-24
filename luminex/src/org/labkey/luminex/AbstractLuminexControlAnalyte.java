package org.labkey.luminex;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public abstract class AbstractLuminexControlAnalyte implements Serializable
{
    private int _analyteId;
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
