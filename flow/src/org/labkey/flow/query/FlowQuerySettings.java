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
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ActionURL;

import javax.servlet.http.HttpServletRequest;

public class FlowQuerySettings extends QuerySettings
{
    private boolean _showGraphs;

    public FlowQuerySettings(ActionURL url, String dataRegionName)
    {
        super(url, dataRegionName);
    }

    public FlowQuerySettings(Portal.WebPart webPart, ViewContext context)
    {
        super(webPart, context);
    }

    protected void init(ActionURL url)
    {
        super.init(url);
        _showGraphs = url.getParameter(param("showGraphs")) != null;
    }

    public boolean getShowGraphs()
    {
        return _showGraphs;
    }
    public void setShowGraphs(boolean b)
    {
        _showGraphs = b;
    }
}
