/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.luminex.model;

import org.labkey.api.exp.ExpQCFlag;

/**
 * User: cnathe
 * Date: Jan 11, 2012
 */
public class CVQCFlag extends ExpQCFlag
{
    private int _analyte;
    private int _dataId;
    private String _wellType;
    private String _wellDescription;

    public CVQCFlag() {}

    public CVQCFlag(int runId, String flagType, String description, int analyte, int dataId, String wellType, String wellDescription)
    {
        super(runId, flagType, description);
        setAnalyte(analyte);
        setDataId(dataId);
        setWellType(wellType);
        setWellDescription(wellDescription);
        setEnabled(true);
    }

    public int getAnalyte()
    {
        return _analyte;
    }

    public void setAnalyte(int analyte)
    {
        _analyte = analyte;
        setIntKey1(analyte);
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
        setIntKey2(dataId);
    }

    public String getWellType()
    {
        return _wellType;
    }

    public void setWellType(String wellType)
    {
        _wellType = wellType;
        setKey1(wellType);
    }

    public String getWellDescription()
    {
        return _wellDescription;
    }

    public void setWellDescription(String wellDescription)
    {
        _wellDescription = wellDescription;
        setKey2(wellDescription);
    }

    public void setRun(int run)
    {
        setRunId(run);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CVQCFlag that = (CVQCFlag) o;

        if (getRunId() != that.getRunId()) return false;
        if (getFlagType() != null ? !getFlagType().equals(that.getFlagType()) : that.getFlagType() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) return false;
        if (_analyte != that._analyte) return false;
        if (_dataId != that._dataId) return false;
        if (_wellType != null ? !_wellType.equals(that._wellType) : that._wellType != null) return false;
        if (_wellDescription != null ? !_wellDescription.equals(that._wellDescription) : that._wellDescription != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = getRunId();
        result = 31 * result + (getFlagType() != null ? getFlagType().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + _analyte;
        result = 31 * result + _dataId;
        result = 31 * result + (_wellType != null ? _wellType.hashCode() : 0);
        result = 31 * result + (_wellDescription != null ? _wellDescription.hashCode() : 0);
        return result;
    }
}

