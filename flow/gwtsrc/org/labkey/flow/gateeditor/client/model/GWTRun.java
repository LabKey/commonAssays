package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTRun implements IsSerializable
{
    int runId;
    String name;


    public int getRunId()
    {
        return runId;
    }

    public void setRunId(int runId)
    {
        this.runId = runId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GWTRun))
            return false;
        return runId == ((GWTRun) other).runId;
    }

    public int hashCode()
    {
        return runId;
    }
}
