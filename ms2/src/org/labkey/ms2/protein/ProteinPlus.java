package org.labkey.ms2.protein;

import org.labkey.common.tools.Protein;
import org.labkey.api.util.HashHelpers;

/**
 * User: brittp
 * Date: Jan 2, 2006
 * Time: 3:47:22 PM
 */
public class ProteinPlus
{
    protected String _hash;
    protected String _fullOrg;
    protected String _species;
    protected String _genus;
    protected Protein _protein;


    public ProteinPlus(Protein p)
    {
        setProtein(p);
        genHash();
    }

    public Protein getProtein()
    {
        return _protein;
    }

    public void setProtein(Protein p)
    {
        _protein = p;
    }

    public String getGenus()
    {
        return _genus;
    }

    public void setGenus(String g)
    {
        _genus = g;
    }

    public String getSpecies()
    {
        return _species;
    }

    public void setSpecies(String s)
    {
        _species = s;
    }

    public String getFullOrg()
    {
        return _fullOrg;
    }

    public void setFullOrg(String fo)
    {
        _fullOrg = fo;
    }

    public String getHash()
    {
        return _hash;
    }

    public void setHash(String h)
    {
        _hash = h;
    }

    public void genHash()
    {
        setHash(HashHelpers.hash(getProtein().getBytes()));
    }
}
