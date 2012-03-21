package org.labkey.nab;

import org.labkey.api.action.HasViewContext;
import org.labkey.api.view.ViewContext;

/**
 * User: brittp
 * Date: Mar 12, 2010 9:47:58 AM
 */
public abstract class GetNabRunsBaseForm implements HasViewContext
{
    private ViewContext _viewContext;
    private boolean _includeStats = true;
    private boolean _includeWells = true;
    private boolean _includeFitParameters = true;
    private boolean _calculateNeut = true;

    public ViewContext getViewContext()
    {
        return _viewContext;
    }

    public void setViewContext(ViewContext viewContext)
    {
        _viewContext = viewContext;
    }

    public boolean isIncludeStats()
    {
        return _includeStats;
    }

    public void setIncludeStats(boolean includeStats)
    {
        _includeStats = includeStats;
    }

    public boolean isIncludeWells()
    {
        return _includeWells;
    }

    public void setIncludeWells(boolean includeWells)
    {
        _includeWells = includeWells;
    }

    public boolean isCalculateNeut()
    {
        return _calculateNeut;
    }

    public void setCalculateNeut(boolean calculateNeut)
    {
        _calculateNeut = calculateNeut;
    }

    public boolean isIncludeFitParameters()
    {
        return _includeFitParameters;
    }

    public void setIncludeFitParameters(boolean includeFitParameters)
    {
        _includeFitParameters = includeFitParameters;
    }
}
