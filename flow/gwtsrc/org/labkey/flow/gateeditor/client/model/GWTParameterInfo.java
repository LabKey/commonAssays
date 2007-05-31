package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTParameterInfo implements IsSerializable
{
    String name;
    Double minValue;


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Double getMinValue()
    {
        return minValue;
    }

    public void setMinValue(Double minValue)
    {
        this.minValue = minValue;
    }

    public GWTParameterInfo duplicate()
    {
        GWTParameterInfo that = new GWTParameterInfo();
        that.name = this.name;
        that.minValue = this.minValue;
        return that;
    }
}
