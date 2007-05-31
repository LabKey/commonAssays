package org.labkey.flow.gateeditor.client.model;

public class GWTChannel
{
    GWTChannelSubset positive;
    GWTChannelSubset negative;


    public GWTChannelSubset getPositive()
    {
        return positive;
    }

    public void setPositive(GWTChannelSubset positive)
    {
        this.positive = positive;
    }

    public GWTChannelSubset getNegative()
    {
        return negative;
    }

    public void setNegative(GWTChannelSubset negative)
    {
        this.negative = negative;
    }
}
