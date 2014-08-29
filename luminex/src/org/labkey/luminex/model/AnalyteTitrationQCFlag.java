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

/**
 * User: cnathe
 * Date: Jan 11, 2012
 */
public class AnalyteTitrationQCFlag extends AbstractAnalyteQCFlag
{
    private int _titration;

    public AnalyteTitrationQCFlag() {}

    public AnalyteTitrationQCFlag(int runId, String flagType, String description, int analyte, int titration)
    {
        super(runId, flagType, description, analyte);
        setTitration(titration);
    }

    public int getTitration()
    {
        return _titration;
    }

    public void setTitration(int titration)
    {
        _titration = titration;
        setIntKey2(titration);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyteTitrationQCFlag that = (AnalyteTitrationQCFlag) o;

        if (getRunId() != that.getRunId()) return false;
        if (getFlagType() != null ? !getFlagType().equals(that.getFlagType()) : that.getFlagType() != null) return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) return false;
        if (getAnalyte() != that.getAnalyte()) return false;
        if (_titration != that._titration) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = getRunId();
        result = 31 * result + (getFlagType() != null ? getFlagType().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + getAnalyte();
        result = 31 * result + _titration;
        return result;
    }
}

