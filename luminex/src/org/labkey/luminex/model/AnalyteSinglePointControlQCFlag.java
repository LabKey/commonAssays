/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.labkey.luminex.LuminexDataHandler;

/**
 * User: jeckels
 * Date: 8/26/13
 */
public class AnalyteSinglePointControlQCFlag extends AbstractAnalyteQCFlag
{
    private int _singlePointControl;
    public AnalyteSinglePointControlQCFlag()
    {
        super();
    }

    public AnalyteSinglePointControlQCFlag(int runId, String description, int analyte, int singlePointControl)
    {
        super(runId, LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE, description, analyte);
        setSinglePointControl(singlePointControl);
    }


    public int getSinglePointControl()
    {
        return _singlePointControl;
    }

    public void setSinglePointControl(int singlePointControl)
    {
        _singlePointControl = singlePointControl;
        setIntKey2(singlePointControl);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyteSinglePointControlQCFlag that = (AnalyteSinglePointControlQCFlag) o;

        if (getRunId() != that.getRunId()) return false;
        if (getFlagType() != null ? !getFlagType().equals(that.getFlagType()) : that.getFlagType() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) return false;
        if (getAnalyte() != that.getAnalyte()) return false;
        if (_singlePointControl != that._singlePointControl) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = getRunId();
        result = 31 * result + (getFlagType() != null ? getFlagType().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + getAnalyte();
        result = 31 * result + _singlePointControl;
        return result;
    }

}
