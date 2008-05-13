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

package org.labkey.flow;

import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.data.ContainerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.labkey.flow.controllers.FlowController;

public enum FlowPreference
{
    graphSize("300"),
    showGraphs("1"),
    editScriptRunId(null),
    editScriptCompId(null),
    editScriptWellId(null)
    ;

    FlowPreference(String defValue)
    {
        _defValue = defValue;
    }
    String _defValue;

    public String getValue(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        if (session == null)
            return null;
        String value = (String) session.getAttribute(getKey());
        if (value == null)
            return _defValue;
        return value;
    }

    public void setValue(HttpServletRequest request, String value)
    {
        HttpSession session = request.getSession(true);
        session.setAttribute(getKey(), value);
    }

    public int getIntValue(HttpServletRequest request)
    {
        try
        {
            return Integer.valueOf(getValue(request));
        }
        catch (Exception e)
        {
            if (_defValue == null)
                return 0;
            return Integer.valueOf(_defValue);
        }
    }

    public String urlUpdate()
    {
        ActionURL url = PageFlowUtil.urlFor(FlowController.Action.savePreferences, ContainerManager.getRoot());
        url.addParameter(name(), "");
        return url.toString();
    }

    private String getKey()
    {
        return FlowPreference.class.getName() + "." + name();
    }

    static public void update(HttpServletRequest request)
    {
        for (FlowPreference pref : values())
        {
            String value = request.getParameter(pref.name());
            if (value != null)
            {
                pref.setValue(request, value);
            }
        }
    }
}
