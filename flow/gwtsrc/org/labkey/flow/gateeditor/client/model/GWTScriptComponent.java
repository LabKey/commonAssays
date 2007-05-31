package org.labkey.flow.gateeditor.client.model;

abstract public class GWTScriptComponent extends GWTPopulationSet
{
    protected void copyInto(GWTScriptComponent that)
    {
        that.populations = clonePopulations();
    }

    abstract public GWTScriptComponent duplicate();
}
