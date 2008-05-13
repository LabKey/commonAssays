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

package org.labkey.flow.controllers;

import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewController;

abstract public class BaseController<A extends Enum, P extends Enum> extends ViewController
{
    protected int getIntParam(P param)
    {
        String value = getParam(param);
        if (value == null)
            return 0;
        return Integer.valueOf(value);
    }

    protected String getParam(P param)
    {
        return getRequest().getParameter(param.toString());
    }

    protected void putParam(ActionURL helper, Enum param, String value)
    {
        helper.replaceParameter(param.toString(), value);
    }

    protected void putParam(ActionURL helper, Enum param, int value)
    {
        putParam(helper, param, Integer.toString(value));
    }

    protected boolean hasParameter(String name)
    {
        if (getRequest().getParameter(name) != null)
            return true;
        if (getRequest().getParameter(name + ".x") != null)
            return true;
        return false;
    }

    public String getContainerPath()
    {
        return getActionURL().getExtraPath();
    }
}
