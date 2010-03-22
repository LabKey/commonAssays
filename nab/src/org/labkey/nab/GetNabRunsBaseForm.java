package org.labkey.nab;

import org.labkey.api.action.HasViewContext;
import org.labkey.api.view.ViewContext;

/**
* Copyright (c) 2010 LabKey Corporation
* <p/>
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* <p/>
* http://www.apache.org/licenses/LICENSE-2.0
* <p/>
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* <p/>
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
