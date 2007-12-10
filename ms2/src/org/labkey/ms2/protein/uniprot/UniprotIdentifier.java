package org.labkey.ms2.protein.uniprot;

/**
 * User: jeckels
 * Date: Nov 30, 2007
 */
public class UniprotIdentifier
{
    private String _identType;
    private String _identifier;
    private UniprotSequence _sequence;

    public UniprotIdentifier(String identType, String identifier, UniprotSequence sequence)
    {
        _identType = identType;
        _identifier = identifier;
        _sequence = sequence;
    }

    public String getIdentType()
    {
        return _identType;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public UniprotSequence getSequence()
    {
        return _sequence;
    }
}
