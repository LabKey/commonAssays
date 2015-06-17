/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.elispot;

import org.labkey.api.exp.api.ExpProtocol;

import java.util.Map;

/**
 * Created by davebradlee on 3/20/15.
 */
public class RunDataRow
{
    private int _rowId;
    private int _runId;
    private String _specimenLsid;
    private String _antigenLsid;
    private Double _spotCount;
    private Double _spotSize;
    private String _wellgroupName;
    private String _wellgroupLocation;
    private Double _normalizedSpotCount;
    private String _antigenWellgroupName;
    private String _analyte;
    private String _cytokine;
    private Double _activity;
    private Double _intensity;
    private String _objectUri;
    private int _objectId;          // TODO: remove when we remove use of exp.Object

    private Map<String, Object> _antigenRow = null;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getSpecimenLsid()
    {
        return _specimenLsid;
    }

    public void setSpecimenLsid(String specimenLsid)
    {
        _specimenLsid = specimenLsid;
    }

    public Double getSpotCount()
    {
        return _spotCount;
    }

    public void setSpotCount(Double spotCount)
    {
        _spotCount = spotCount;
    }

    public String getWellgroupName()
    {
        return _wellgroupName;
    }

    public void setWellgroupName(String wellgroupName)
    {
        _wellgroupName = wellgroupName;
    }

    public String getWellgroupLocation()
    {
        return _wellgroupLocation;
    }

    public void setWellgroupLocation(String wellgroupLocation)
    {
        _wellgroupLocation = wellgroupLocation;
    }

    public Double getNormalizedSpotCount()
    {
        return _normalizedSpotCount;
    }

    public void setNormalizedSpotCount(Double normalizedSpotCount)
    {
        _normalizedSpotCount = normalizedSpotCount;
    }

    public String getCytokine()
    {
        return _cytokine;
    }

    public void setCytokine(String cytokine)
    {
        _cytokine = cytokine;
    }

    public Integer getAntigenId(ExpProtocol protocol)
    {
        if (null == _antigenRow)
            _antigenRow = ElispotManager.get().getAntigenRow(_antigenLsid, protocol);
        return null != _antigenRow ? (Integer)_antigenRow.get("AntigenId") : null;
    }

    public String getAntigenName(ExpProtocol protocol)
    {
        if (null == _antigenRow)
            _antigenRow = ElispotManager.get().getAntigenRow(_antigenLsid, protocol);
        return null != _antigenRow ? (String)_antigenRow.get("AntigenName") : null;
    }

    public String getAntigenWellgroupName()
    {
        return _antigenWellgroupName;
    }

    public void setAntigenWellgroupName(String antigenWellgroupName)
    {
        _antigenWellgroupName = antigenWellgroupName;
    }

    public Integer getCellWell(ExpProtocol protocol)
    {
        if (null == _antigenRow)
            _antigenRow = ElispotManager.get().getAntigenRow(_antigenLsid, protocol);
        return null != _antigenRow ? (Integer)_antigenRow.get("CellWell") : null;
    }

    public String getAnalyte()
    {
        return _analyte;
    }

    public void setAnalyte(String analyte)
    {
        _analyte = analyte;
    }

    public Double getActivity()
    {
        return _activity;
    }

    public void setActivity(Double activity)
    {
        _activity = activity;
    }

    public Double getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Double intensity)
    {
        _intensity = intensity;
    }

    public String getObjectUri()
    {
        return _objectUri;
    }

    public void setObjectUri(String objectUri)
    {
        _objectUri = objectUri;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public void setObjectId(int objectId)
    {
        _objectId = objectId;
    }

    public String getAntigenLsid()
    {
        return _antigenLsid;
    }

    public void setAntigenLsid(String antigenLsid)
    {
        _antigenLsid = antigenLsid;
    }

    public Double getSpotSize()
    {
        return _spotSize;
    }

    public void setSpotSize(Double spotSize)
    {
        _spotSize = spotSize;
    }
}
