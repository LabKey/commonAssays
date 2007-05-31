package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTSettings implements IsSerializable
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
