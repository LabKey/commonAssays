package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTScript implements IsSerializable
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