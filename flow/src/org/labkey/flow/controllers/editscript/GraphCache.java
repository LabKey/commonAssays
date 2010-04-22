/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.labkey.api.util.Pair;
import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;
import org.labkey.flow.analysis.web.PlotInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.ref.SoftReference;

public class GraphCache
{
    static final private String key = GraphCache.class.getName();
    transient private GWTGraphOptions graphOptions;
    transient private SoftReference<Pair<GWTGraphInfo,PlotInfo>> ref;

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

    public synchronized Pair<GWTGraphInfo,PlotInfo> getInfo(GWTGraphOptions options)
    {
        if (!options.equals(this.graphOptions))
        {
            ref = null;
            return null;
        }
        return null==ref ? null : ref.get();
    }

    public synchronized void setGraphInfo(GWTGraphOptions options, GWTGraphInfo info, PlotInfo plotInfo)
    {
        this.graphOptions = options;
        this.ref = new SoftReference<Pair<GWTGraphInfo,PlotInfo>>(new Pair<GWTGraphInfo,PlotInfo>(info,plotInfo));
    }
}
