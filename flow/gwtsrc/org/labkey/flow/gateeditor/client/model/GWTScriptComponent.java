package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

abstract public class GWTScriptComponent extends GWTPopulationSet implements IsSerializable, Serializable
{
    protected void copyInto(GWTScriptComponent that)
    {
        that.populations = clonePopulations();
    }

    abstract public GWTScriptComponent duplicate();
}
