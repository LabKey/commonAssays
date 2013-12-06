/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.flow.analysis.model;

import java.io.Serializable;
import java.util.Map;

/**
 * UNDONE: Replace CalibrationTable with ScalingFunction transform and display transform.
 * CONSIDER: Merge with DataFrame.Field ?
 */
public class ParameterInfo implements Serializable
{
    protected String _name;
    protected Double _minValue;
    // For some parameters, FlowJo maps them as integers between 0 and 4095, even though
    // they actually range much higher.
    // This multiplier maps to the range that we actually use.
    protected Double _multiplier;
    protected CalibrationTable _calibrationTable;

    public ParameterInfo(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public Double getMinValue()
    {
        return _minValue;
    }

    public void setMinValue(double value)
    {
        _minValue = value;
    }

    public void setMultiplier(double multiplier)
    {
        _multiplier = multiplier;
    }

    public static ParameterInfo fromKeywords(Map<String, String> keywords, int index)
    {
        String paramName = FCSHeader.getParameterName(keywords, index);
        if (paramName == null)
            return null;

        ParameterInfo paramInfo = new ParameterInfo(paramName);
        paramInfo._multiplier = 1d;
        String paramDisplay = keywords.get("P" + (index+1) + "DISPLAY");
        String paramRange = keywords.get("$P" + (index+1) + "R");
        if ("LIN".equals(paramDisplay))
        {
            double range = Double.valueOf(paramRange).doubleValue();
            if (range > 4096)
            {
                paramInfo._multiplier = range/4096;
            }
        }

        return paramInfo;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterInfo that = (ParameterInfo) o;

        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        if (_minValue != null ? !_minValue.equals(that._minValue) : that._minValue != null) return false;
        if (_multiplier != null ? !_multiplier.equals(that._multiplier) : that._multiplier != null) return false;
        if (_calibrationTable != null ? !_calibrationTable.equals(that._calibrationTable) : that._calibrationTable != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _name != null ? _name.hashCode() : 0;
        result = 31 * result + (_minValue != null ? _minValue.hashCode() : 0);
        result = 31 * result + (_multiplier != null ? _multiplier.hashCode() : 0);
        result = 31 * result + (_calibrationTable != null ? _calibrationTable.hashCode() : 0);
        return result;
    }
}
