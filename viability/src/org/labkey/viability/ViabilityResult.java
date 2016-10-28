/*
 * Copyright (c) 2009-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
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
    private int runID;
    private int dataID;
    private int objectID;
    private String participantID;
    private Double visitID;
    private Date date;

    private int sampleNum;
    private String poolID;
    private int totalCells;
    private int viableCells;

    private List<String> specimenIDList;
    private String targetStudy;

    private Map<PropertyDescriptor, Object> properties;

    public ViabilityResult() { }

    public static ViabilityResult fromMap(Map<String, Object> base, Map<PropertyDescriptor, Object> extra)
    {
        ViabilityResult result = new ViabilityResult();

        if (base.get("rowid") != null)
            result.setRowID((int)base.get("rowId"));
        if (base.get("container") != null)
            result.setContainer((String) base.get("container"));
        if (base.get("protocolID") != null)
            result.setProtocolID((int) base.get("protocolID"));
        if (base.get("runID") != null)
            result.setRunID((int) base.get("runID"));
        if (base.get("dataID") != null)
            result.setDataID((int) base.get("dataID"));
        if (base.get("objectID") != null)
            result.setObjectID((int) base.get("objectID"));
        if (base.get("participantID") != null)
            result.setParticipantID(String.valueOf(base.get("participantID")));
        if (base.get("visitID") != null)
            result.setVisitID(((Number) base.get("visitID")).doubleValue());
        if (base.get("date") != null)
            result.setDate((Date) base.get("date"));

        if (base.get("sampleNum") != null)
            result.setSampleNum((int)base.get("sampleNum"));
        if (base.get("poolID") != null)
            result.setPoolID(String.valueOf(base.get("poolID")));
        if (base.get("totalCells") != null)
            result.setTotalCells(((Number) base.get("totalCells")).intValue());
        if (base.get("viableCells") != null)
            result.setViableCells(((Number) base.get("viableCells")).intValue());

        // NOTE: We use the 'string' version of 'specimenIDs' when binding to the database in TableSelector.
        // NOTE: The 'list' version of 'SpecimenIDs' is used when the guava file is parsed.
        if (base.get("specimenIDs") != null)
        {
            Object o = base.get("specimenIDs");
            if (o instanceof String)
                result.setSpecimenIDs((String) o);
            else if (o instanceof List)
                result.setSpecimenIDList((List<String>) o);
            else
                throw new IllegalArgumentException("Expected comma separated list or a collecting of specimen IDs");
        }

        if (base.get("targetStudy") instanceof String)
            result.setTargetStudy((String)base.get("targetStudy"));

        if (extra != null)
            result.setProperties(extra);
        else
            result.setProperties(Collections.emptyMap());
        return result;
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> ret = new CaseInsensitiveHashMap<>();

        ret.put("rowid", getRowID());
        ret.put("container", getContainer());
        ret.put("protocolID", getProtocolID());
        ret.put("runID", getRunID());
        ret.put("dataID", getDataID());
        ret.put("objectID", getObjectID());
        ret.put("participantID", getParticipantID());
        ret.put("visitID", getVisitID());
        ret.put("date", getDate());

        ret.put("sampleNum", getSampleNum());
        ret.put("poolID", getPoolID());
        ret.put("totalCells", getTotalCells());
        ret.put("viableCells", getViableCells());

        // NOTE: We use the 'string' version of 'specimenIDs' when converting to/from a map.
        // NOTE: The 'list' version of 'SpecimenIDs' is used when the guava file is parsed.
        ret.put("specimenIDs", getSpecimenIDList());

        ret.put("targetStudy", getTargetStudy());

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

    public int getRunID()
    {
        return runID;
    }

    public void setRunID(int runID)
    {
        this.runID = runID;
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

    // Gets the concatenated String of specimen IDs.
    // NOTE: Only used when inserting into viability.result table
    @Nullable
    public String getSpecimenIDs()
    {
        return specimenIDList == null ? null : StringUtils.join(specimenIDList, ",");
    }

    // NOTE: Used when instantiating ViabilityResult bean from viability.result table
    public void setSpecimenIDs(@Nullable String specimenIDs)
    {
        if (specimenIDs == null)
        {
            setSpecimenIDList(null);
        }
        else
        {
            setSpecimenIDList(Arrays.asList(StringUtils.split(specimenIDs, ",")));
        }
    }

    @Nullable
    public List<String> getSpecimenIDList()
    {
        return specimenIDList;
    }

    public void setSpecimenIDList(@Nullable List<String> specimenID)
    {
        if (specimenID == null || specimenID.isEmpty())
            this.specimenIDList = null;
        else
        {
            this.specimenIDList = new ArrayList<>(specimenID);
            Collections.sort(this.specimenIDList);
        }
    }

    // NOTE: Only used when inserting into viability.result table
    public int getSpecimenCount()
    {
        return specimenIDList == null ? 0 : specimenIDList.size();
    }

    public void setSpecimenCount(int specimenCount)
    {
        // no-op
    }

    public String getTargetStudy()
    {
        return targetStudy;
    }

    public void setTargetStudy(String targetStudy)
    {
        this.targetStudy = targetStudy;
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
