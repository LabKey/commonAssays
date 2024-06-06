package org.labkey.api.protein;

public class SimpleProtein
{
    private int _seqId;
    private double _mass;
    private String _description;
    private String _bestName;
    private String _bestGeneName;

    protected String _sequence;

    public SimpleProtein()
    {
    }

    public SimpleProtein(SimpleProtein protein)
    {
        _seqId = protein._seqId;
        _sequence = protein._sequence;
        _mass = protein._mass;
        _description = protein._description;
        _bestName = protein._bestName;
        _bestGeneName = protein._bestGeneName;
    }

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
