package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTPopulationSet implements IsSerializable, Serializable
{
    static private final GWTPopulation[] EMPTY_LIST = new GWTPopulation[0];
    GWTPopulation[] populations;

    public GWTPopulationSet()
    {
        populations = EMPTY_LIST;
    }

    public GWTPopulation[] getPopulations()
    {
        return populations;
    }

    public void setPopulations(GWTPopulation[] populations)
    {
        this.populations = populations;
    }

    public GWTPopulation getPopulation(String name)
    {
        for (int i = 0; i < populations.length; i ++)
        {
            if (name.equals(populations[i]))
            {
                return populations[i];
            }
        }
        return null;
    }

    public GWTPopulation findPopulation(String fullName)
    {
        for (int i = 0; i < populations.length; i ++)
        {
            GWTPopulation ret = populations[i].findPopulation(fullName);
            if (ret != null)
                return ret;
        }
        return null;
    }

    public GWTPopulation[] clonePopulations()
    {
        GWTPopulation[] ret = new GWTPopulation[populations.length];
        for (int i = 0; i < populations.length; i ++)
        {
            ret[i] = populations[i].duplicate();
        }
        return ret;
    }
}
