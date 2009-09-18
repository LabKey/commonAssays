/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.data.ObjectFactory;
import org.labkey.api.exp.PropertyDescriptor;

import java.util.*;

/**
 * User: kevink
 * Date: Sep 16, 2009
 */
public class ViabilityResult
{
    private int rowID;

    private int dataID;
    private int objectID;
    private String participantID;
    private Double visitID;
    private Date date;

    private String poolID;
    private int totalCells;
    private int viableCells;

    private List<String> specimenID;
    private Map<String, Object> properties;

    public ViabilityResult() { }

    public static ViabilityResult fromMap(Map<String, Object> map)
    {
        ObjectFactory<ViabilityResult> factory = ObjectFactory.Registry.getFactory(ViabilityResult.class);
        return factory.fromMap(map);
    }

    public int getRowID()
    {
        return rowID;
    }

    public void setRowID(int rowID)
    {
        this.rowID = rowID;
    }

    public int getDataID()
    {
        return dataID;
    }

    public void setDataID(int dataID)
    {
        this.dataID = dataID;
    }

    public int getObjectID()
    {
        return objectID;
    }

    public void setObjectID(int objectID)
    {
        this.objectID = objectID;
    }

    public String getParticipantID()
    {
        return participantID;
    }

    public void setParticipantID(String participantID)
    {
        this.participantID = participantID;
    }

    public Double getVisitID()
    {
        return visitID;
    }

    public void setVisitID(Double visitID)
    {
        this.visitID = visitID;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getPoolID()
    {
        return poolID;
    }

    public void setPoolID(String poolID)
    {
        this.poolID = poolID;
    }

    public int getTotalCells()
    {
        return totalCells;
    }

    public void setTotalCells(int totalCells)
    {
        this.totalCells = totalCells;
    }

    public int getViableCells()
    {
        return viableCells;
    }

    public void setViableCells(int viableCells)
    {
        this.viableCells = viableCells;
    }

    public double getViability()
    {
        if (totalCells > 0)
            return (double)viableCells / totalCells;
        return 0;
    }

    public List<String> getSpecimenIDs()
    {
        return specimenID;
    }

    public void setSpecimenIDs(List<String> specimenID)
    {
        this.specimenID = specimenID;
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }
    
}
