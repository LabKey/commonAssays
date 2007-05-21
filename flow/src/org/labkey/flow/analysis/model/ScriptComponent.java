package org.labkey.flow.analysis.model;

public class ScriptComponent extends PopulationSet
{
    ScriptSettings _settings;

    public ScriptSettings getSettings()
    {
        return _settings;
    }

    public void setSettings(ScriptSettings settings)
    {
        _settings = settings;
    }
}
