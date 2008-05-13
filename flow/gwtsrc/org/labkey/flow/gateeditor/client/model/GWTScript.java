/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTScript implements IsSerializable, Serializable
{
    int scriptId;

    public int getScriptId()
    {
        return scriptId;
    }

    public void setScriptId(int scriptId)
    {
        this.scriptId = scriptId;
    }

    String name;
    GWTSettings settings;
    GWTCompensationCalculation compensationCalculation;
    GWTAnalysis analysis;

    public GWTSettings getSettings()
    {
        return settings;
    }

    public void setSettings(GWTSettings settings)
    {
        this.settings = settings;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public GWTCompensationCalculation getCompensationCalculation()
    {
        return compensationCalculation;
    }

    public void setCompensationCalculation(GWTCompensationCalculation compensationCalculation)
    {
        this.compensationCalculation = compensationCalculation;
    }

    public GWTAnalysis getAnalysis()
    {
        return analysis;
    }

    public void setAnalysis(GWTAnalysis analysis)
    {
        this.analysis = analysis;
    }

    public int hashCode()
    {
        return scriptId;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GWTScript))
        {
            return false;
        }
        GWTScript that = (GWTScript) other;
        return this.getScriptId() == that.getScriptId();
    }

    public GWTScript duplicate()
    {
        GWTScript ret = new GWTScript();
        ret.scriptId = scriptId;
        ret.settings = settings;
        if (compensationCalculation != null)
        {
            ret.compensationCalculation = (GWTCompensationCalculation) compensationCalculation.duplicate();
        }
        if (analysis != null)
        {
            ret.analysis = (GWTAnalysis) analysis.duplicate();
        }
        return ret;
    }
}