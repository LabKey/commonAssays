/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
