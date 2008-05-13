/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.protein.uniprot;

import java.sql.Timestamp;

/**
 * User: jeckels
 * Date: Nov 30, 2007
 */
public class UniprotSequence
{
    private String _protSequence;
    private String _genus;
    private String _species;
    private String _hash;
    private String _bestName;
    private String _bestGeneName;
    private String _description;
    private Timestamp _sourceChangeDate;
    private Float _mass;
    private Integer _length;
    private Timestamp _sourceInsertDate;
    private String _source;

    public String getGenus()
    {
        return _genus;
    }

    public String getSpecies()
    {
        return _species;
    }

    public String getHash()
    {
        return _hash;
    }

    public void setBestName(String bestName)
    {
        _bestName = bestName;
    }

    public void setBestGeneName(String bestGeneName)
    {
        _bestGeneName = bestGeneName;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setHash(String hash)
    {
        _hash = hash;
    }

    public void setProtSequence(String sequence)
    {
        _protSequence = sequence;
    }

    public void setSourceChangeDate(Timestamp sourceChangeDate)
    {
        _sourceChangeDate = sourceChangeDate;
    }

    public void setMass(Float mass)
    {
        _mass = mass;
    }

    public void setLength(Integer length)
    {
        _length = length;
    }

    public String getProtSequence()
    {
        return _protSequence;
    }

    public String getBestName()
    {
        return _bestName;
    }

    public String getBestGeneName()
    {
        return _bestGeneName;
    }

    public Timestamp getSourceChangeDate()
    {
        return _sourceChangeDate;
    }

    public Float getMass()
    {
        return _mass;
    }

    public Integer getLength()
    {
        return _length;
    }

    public Timestamp getSourceInsertDate()
    {
        return _sourceInsertDate;
    }

    public void setSourceInsertDate(Timestamp sourceInsertDate)
    {
        _sourceInsertDate = sourceInsertDate;
    }

    public void setSource(String source)
    {
        _source = source;
    }

    public String getSource()
    {
        return _source;
    }

    public void setGenus(String genus)
    {
        _genus = genus;
    }

    public void setSpecies(String species)
    {
        _species = species;
    }
}
