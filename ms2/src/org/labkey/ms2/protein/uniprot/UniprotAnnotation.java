package org.labkey.ms2.protein.uniprot;

/**
 * User: jeckels
 * Date: Nov 30, 2007
 */
public class UniprotAnnotation
{
    private final String _annotVal;
    private final String _annotType;
    private final UniprotSequence _sequence;
    private Integer _startPos;
    private Integer _endPos;
    private UniprotIdentifier _identifier;

    public UniprotAnnotation(String annotVal, String annotType, UniprotSequence sequence)
    {
        _annotVal = annotVal;
        _annotType = annotType;
        _sequence = sequence;
    }

    public UniprotAnnotation(String annotVal, String annotType, UniprotIdentifier identifier)
    {
        this(annotVal, annotType, identifier.getSequence());
        _identifier = identifier;
    }

    public String getAnnotVal()
    {
        return _annotVal;
    }

    public String getAnnotType()
    {
        return _annotType;
    }

    public Integer getStartPos()
    {
        return _startPos;
    }

    public Integer getEndPos()
    {
        return _endPos;
    }

    public UniprotIdentifier getIdentifier()
    {
        return _identifier;
    }

    public void setStartPos(Integer startPos)
    {
        _startPos = startPos;
    }

    public void setEndPos(Integer endPos)
    {
        _endPos = endPos;
    }

    public UniprotSequence getSequence()
    {
        return _sequence;
    }
}
