package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTSettings implements IsSerializable, Serializable
{
    GWTParameterInfo[] parameters;

    public GWTParameterInfo[] getParameters()
    {
        return parameters;
    }

    public void setParameters(GWTParameterInfo[] parameters)
    {
        this.parameters = parameters;
    }
}
