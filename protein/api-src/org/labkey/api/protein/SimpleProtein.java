/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
