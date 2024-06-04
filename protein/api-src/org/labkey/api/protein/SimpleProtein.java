package org.labkey.api.protein;

public class SimpleProtein
{
    protected int _seqId;
    protected String _sequence;
    protected double _mass;
    protected String _description;
    protected String _bestName;
    protected String _bestGeneName;

    public int getSeqId()
    {
        return _seqId;
    }

    public void setSeqId(int seqId)
    {
        _seqId = seqId;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = (sequence == null ? "" : sequence);    // Sequence can be null if FASTA is not loaded
    }

    public double getMass()
    {
        return _mass;
    }

    public void setMass(double mass)
    {
        _mass = mass;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getBestName()
    {
        return _bestName;
    }

    public void setBestName(String bestName)
    {
        _bestName = bestName;
    }

    public String getBestGeneName()
    {
        return _bestGeneName;
    }

    public void setBestGeneName(String bestGeneName)
    {
        _bestGeneName = bestGeneName;
    }
}
