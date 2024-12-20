/*
 * Copyright (c) 2007-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.api.protein;

import org.labkey.api.protein.fasta.FastaProtein;
import org.labkey.api.util.HashHelpers;

public class ProteinPlus
{
    protected String _hash;
    protected String _fullOrg;
    protected String _species;
    protected String _genus;
    protected FastaProtein _protein;

    public ProteinPlus(FastaProtein p)
    {
        setProtein(p);
        genHash();
    }

    public FastaProtein getProtein()
    {
        return _protein;
    }

    public void setProtein(FastaProtein p)
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

    public String getDescription()
    {
        int firstBlankIndex = getProtein().getHeader().indexOf(" ");
        if (firstBlankIndex != -1)
        {
            return getProtein().getHeader().substring(firstBlankIndex + 1).replaceAll("\\01", " ").trim();
        }
        return null;
    }

    public String getBestName()
    {
        int firstBlankIndex = getProtein().getHeader().indexOf(" ");

        String result;
        if (firstBlankIndex != -1)
        {
            result = getProtein().getHeader().substring(0, firstBlankIndex).trim();
        }
        else
        {
            result = getProtein().getHeader();
        }
        if (result.length() > 500) result = result.substring(0, 499);
        return result;
    }
}
