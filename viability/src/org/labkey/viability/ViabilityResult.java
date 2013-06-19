/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.*;
import java.sql.SQLException;

/**
 * User: kevink
 * Date: Sep 16, 2009
 */
public class ViabilityResult
{
    private int rowID;

    private String containerID;
    private int protocolID;
    private int dataID;
    private int objectID;
    private String participantID;
    private Double visitID;
    private Date date;

    private int sampleNum;
    private String poolID;
    private int totalCells;
    private int viableCells;

    private List<String> specimenID;
    private Map<PropertyDescriptor, Object> properties;

    public ViabilityResult() { }

    public static ViabilityResult fromMap(Map<String, Object> base, Map<PropertyDescriptor, Object> extra)
    {
        ObjectFactory<ViabilityResult> factory = ObjectFactory.Registry.getFactory(ViabilityResult.class);
        ViabilityResult result = factory.fromMap(base);

        if (result.getSpecimenIDs() == null)
            result.setSpecimenIDs(Collections.<String>emptyList());

        if (extra != null)
            result.setProperties(extra);
        else
            result.setProperties(Collections.<PropertyDescriptor, Object>emptyMap());
        return result;
    }

    public Map<String, Object> toMap()
    {
        ObjectFactory<ViabilityResult> factory = ObjectFactory.Registry.getFactory(ViabilityResult.class);
        Map<String, Object> ret = factory.toMap(this, null);
        ret.putAll(getStringProperties());
        return ret;
    }

    public int getRowID()
    {
        return rowID;
    }

    public void setRowID(int rowID)
    {
        this.rowID = rowID;
    }

    public String getContainer()
    {
        return containerID;
    }

    public void setContainer(String containerID)
    {
        this.containerID = containerID;
    }

    public int getProtocolID()
    {
        return protocolID;
    }

    public void setProtocolID(int protocolID)
    {
        this.protocolID = protocolID;
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

    public int getSampleNum()
    {
        return sampleNum;
    }

    public void setSampleNum(int sampleNum)
    {
        this.sampleNum = sampleNum;
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
        if (specimenID == null)
        {
            try
            {
                specimenID = Arrays.asList(ViabilityManager.getSpecimens(getRowID()));
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return specimenID;
    }

    public void setSpecimenIDs(List<String> specimenID)
    {
        this.specimenID = specimenID;
    }

    public Map<PropertyDescriptor, Object> getProperties()
    {
        if (properties == null)
        {
            try
            {
                properties = ViabilityManager.getProperties(getObjectID());
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return properties;
    }

    private Map<String, Object> getStringProperties()
    {
        Map<String, Object> ret = new CaseInsensitiveHashMap<>();
        Map<PropertyDescriptor, Object> properties = getProperties();
        for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
            ret.put(entry.getKey().getName(), entry.getValue());
        return ret;
    }

    public void setProperties(Map<PropertyDescriptor, Object> properties)
    {
        this.properties = properties;
    }
    
}
