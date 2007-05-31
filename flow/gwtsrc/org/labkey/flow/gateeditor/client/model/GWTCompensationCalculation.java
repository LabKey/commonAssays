package org.labkey.flow.gateeditor.client.model;

public class GWTCompensationCalculation extends GWTScriptComponent
{
    public GWTScriptComponent duplicate()
    {
        GWTCompensationCalculation ret = new GWTCompensationCalculation();
        copyInto(ret);
        return ret;
    }
}
