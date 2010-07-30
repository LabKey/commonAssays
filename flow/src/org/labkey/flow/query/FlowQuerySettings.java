/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
    private boolean _showGraphs;
    private boolean _subtractBackground;

    protected FlowQuerySettings(String dataRegionName)
    {
        super(dataRegionName);
    }

    public FlowQuerySettings(PropertyValues pvs, String dataRegionName)
    {
        super(pvs, dataRegionName);
    }

    @Override
    public void init(PropertyValues params)
    {
        super.init(params);
        _showGraphs = _getParameter(param("showGraphs")) != null;
        _subtractBackground = _getParameter(param("subtractBackground")) != null;
    }

    public boolean getShowGraphs()
    {
        return _showGraphs;
    }
    public void setShowGraphs(boolean b)
    {
        _showGraphs = b;
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
