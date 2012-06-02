package org.labkey.ms2.protein;

/**
 * User: jeckels
 * Date: May 3, 2012
 */
public class Organism
{
    private int _orgId;
    private String _commonName;
    private String _genus;
    private String _species;
    private String _comments;
    private Integer _identId;

    public int getOrgId()
    {
        return _orgId;
    }

    public void setOrgId(int orgId)
    {
        _orgId = orgId;
    }

    public String getCommonName()
    {
        return _commonName;
    }

    public void setCommonName(String commonName)
    {
        _commonName = commonName;
    }

    public String getGenus()
    {
        return _genus;
    }

    public void setGenus(String genus)
    {
        _genus = genus;
    }

    public String getSpecies()
    {
        return _species;
    }

    public void setSpecies(String species)
    {
        _species = species;
    }

    public String getComments()
    {
        return _comments;
    }

    public void setComments(String comments)
    {
        _comments = comments;
    }

    public Integer getIdentId()
    {
        return _identId;
    }

    public void setIdentId(Integer identId)
    {
        _identId = identId;
    }
}
