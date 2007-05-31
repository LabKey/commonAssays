package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTCompensationMatrix implements IsSerializable
{
    public static String PREFIX = "<";
    public static String SUFFIX = ">";

    int compId;
    String name;
    String label;
    String[] parameterNames;


    public String[] getParameterNames()
    {
        return parameterNames;
    }

    public void setParameterNames(String[] parameterNames)
    {
        this.parameterNames = parameterNames;
    }

    public int getCompId()
    {
        return compId;
    }

    public void setCompId(int compId)
    {
        this.compId = compId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTCompensationMatrix)) return false;

        GWTCompensationMatrix that = (GWTCompensationMatrix) o;

        if (compId != that.compId) return false;

        return true;
    }

    public int hashCode()
    {
        return compId;
    }


}
