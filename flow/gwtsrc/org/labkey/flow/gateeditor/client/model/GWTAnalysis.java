package org.labkey.flow.gateeditor.client.model;

public class GWTAnalysis extends GWTScriptComponent
{
    public GWTScriptComponent duplicate()
    {
        GWTScriptComponent ret = new GWTAnalysis();
        copyInto(ret);
        return ret;
    }
}
