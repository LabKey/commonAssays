package org.labkey.ms2.protein.uniprot;

/**
 * User: jeckels
 * Date: Dec 4, 2007
 */
public class UniprotOrganism
{
    private String _species;
    private String _genus;
    private String _commonName;
    private String _comments;
    private String _identID;

    public String getSpecies()
    {
        return _species;
    }

    public void setSpecies(String species)
    {
        _species = species;
    }

    public String getGenus()
    {
        return _genus;
    }

    public void setCommonName(String commonName)
    {
        _commonName = commonName;
    }

    public void setGenus(String genus)
    {
        _genus = genus;
    }

    public void setComments(String comments)
    {
        _comments = comments;
    }

    public String getCommonName()
    {
        return _commonName;
    }

    public String getComments()
    {
        return _comments;
    }

    public void setIdentID(String identID)
    {
        _identID = identID;
    }

    public String getIdentID()
    {
        return _identID;
    }
}
