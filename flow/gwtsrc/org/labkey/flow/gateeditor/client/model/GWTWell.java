package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTWell implements IsSerializable, Serializable
{
    private int wellId;
    private String name;
    private String label;
    private String[] parameters;
    private GWTScript script;
    private GWTRun run;
    private GWTCompensationMatrix compensationMatrix;

    public GWTRun getRun()
    {
        return run;
    }

    public void setRun(GWTRun run)
    {
        this.run = run;
    }

    public String[] getParameters()
    {
        return parameters;
    }

    public void setParameters(String[] parameters)
    {
        this.parameters = parameters;
    }

    public int getWellId()
    {
        return wellId;
    }

    public void setWellId(int wellId)
    {
        this.wellId = wellId;
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
        if (!(o instanceof GWTWell)) return false;

        GWTWell gwtWell = (GWTWell) o;

        if (wellId != gwtWell.wellId) return false;

        return true;
    }

    public int hashCode()
    {
        return wellId;
    }

    public GWTScript getScript()
    {
        return script;
    }

    public void setScript(GWTScript script)
    {
        this.script = script;
    }


    public GWTCompensationMatrix getCompensationMatrix()
    {
        return compensationMatrix;
    }

    public void setCompensationMatrix(GWTCompensationMatrix compensationMatrix)
    {
        this.compensationMatrix = compensationMatrix;
    }
}
