/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;
import org.labkey.flow.analysis.web.PlotInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class GraphCache
{
    static final private String key = GraphCache.class.getName();
    transient private GWTGraphOptions graphOptions;
    transient private GWTGraphInfo graphInfo;
    transient private PlotInfo plotInfo;

    static public GraphCache get(HttpServletRequest request)
    {
        HttpSession session = request.getSession(true);
        Object oCache = session.getAttribute(key);
        if (oCache instanceof GraphCache)
        {
            return (GraphCache) oCache;
        }
        GraphCache ret = new GraphCache();
        session.setAttribute(key, ret);
        return ret;
    }

    public GWTGraphInfo getGraphInfo(GWTGraphOptions options)
    {
        if (!options.equals(this.graphOptions))
        {
            return null;
        }
        return graphInfo;
    }

    public PlotInfo getPlotInfo(GWTGraphOptions options)
    {
        if (!options.equals(this.graphOptions))
        {
            return null;
        }
        return plotInfo;
    }

    public void setGraphInfo(GWTGraphOptions options, GWTGraphInfo info, PlotInfo plotInfo)
    {
        this.graphOptions = options;
        this.graphInfo = info;
        this.plotInfo = plotInfo;
    }
}
