package org.labkey.ms2;

/**
 * User: jeckels
 * Date: May 5, 2006
 */
public class ProteinSummary
{
    private final String _name;
    private final int _seqId;
    private final String _description;
    private final String _bestName;
    private final String _bestGeneName;
    private final double _sequenceMass;

    public ProteinSummary(String name, int seqId, String description, String bestName, String bestGeneName, double sequenceMass)
    {
        _name = name;
        _seqId = seqId;
        _description = description;
        _bestName = bestName;
        _bestGeneName = bestGeneName;
        _sequenceMass = sequenceMass;
    }

    public String getName()
    {
        return _name;
    }

    public int getSeqId()
    {
        return _seqId;
    }

    public String getDescription()
    {
        return _description;
    }

    public String getBestGeneName()
    {
        return _bestGeneName;
    }

    public String getBestName()
    {
        return _bestName;
    }

    public double getSequenceMass()
    {
        return _sequenceMass;
    }
}
