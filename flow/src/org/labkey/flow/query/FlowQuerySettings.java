/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.flow.query;

import org.labkey.api.query.QuerySettings;
import org.springframework.beans.PropertyValues;

public class FlowQuerySettings extends QuerySettings
{
    public enum ShowGraphs {
        None, Thumbnail, Inline
    }

    private ShowGraphs _showGraphs;
    private boolean _subtractBackground;

    protected FlowQuerySettings(String dataRegionName)
    {
        super(dataRegionName);
        setAllowChooseQuery(true);
    }

    public FlowQuerySettings(PropertyValues pvs, String dataRegionName)
    {
        super(pvs, dataRegionName);
        setAllowChooseQuery(true);
    }

    @Override
    public void init(PropertyValues params)
    {
        super.init(params);
        String showGraphsParam = _getParameter(param("showGraphs"));
        if (showGraphsParam != null)
        {
            if (showGraphsParam.equals("true"))
                _showGraphs = ShowGraphs.Inline;
            else if (showGraphsParam.equals("false"))
                _showGraphs = ShowGraphs.None;
            else
            {
                try
                {
                    _showGraphs = ShowGraphs.valueOf(showGraphsParam);
                }
                catch (IllegalArgumentException ex)
                {
                    _showGraphs = ShowGraphs.None;
                }
            }
        }
        _subtractBackground = _getParameter(param("subtractBackground")) != null;
    }

    public ShowGraphs getShowGraphs()
    {
        return _showGraphs;
    }
    public void setShowGraphs(ShowGraphs showGraphs)
    {
        _showGraphs = showGraphs;
    }

    public boolean getSubtractBackground()
    {
        return _subtractBackground;
    }

    public void setSubtractBackground(boolean subtractBackground)
    {
        _subtractBackground = subtractBackground;
    }
}
