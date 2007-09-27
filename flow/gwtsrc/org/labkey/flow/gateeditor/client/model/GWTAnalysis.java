package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTAnalysis extends GWTScriptComponent implements IsSerializable, Serializable
{
    public GWTScriptComponent duplicate()
    {
        GWTScriptComponent ret = new GWTAnalysis();
        copyInto(ret);
        return ret;
    }
}
