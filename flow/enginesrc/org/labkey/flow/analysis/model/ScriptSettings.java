/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.FilterInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ScriptSettings implements Serializable
{
    Map<String, ParameterInfo> _parameters = new CaseInsensitiveHashMap<>();
    Map<String, FilterInfo> _filters = new HashMap<>();

    public static FilterInfo fromFilterDef(FilterDef filter)
    {
        return new FilterInfo(
                filter.getField(),
                filter.getOp() == null ? CompareType.NONBLANK.getPreferredUrlKey() : filter.getOp().toString(),
                filter.getValue());
    }

    public ParameterInfo getParameterInfo(DataFrame.Field field)
    {
        for (String alias : field.getAliases())
            if (_parameters.containsKey(alias))
                return _parameters.get(alias);

        return null;
    }

    public ParameterInfo getParameterInfo(String name, boolean create)
    {
        ParameterInfo ret = _parameters.get(name);
        if (ret != null)
            return ret;
        if (!create)
            return null;
        ret = new ParameterInfo(name);
        _parameters.put(name, ret);
        return ret;
    }

    public FilterInfo getFilter(String field)
    {
        return _filters.get(field);
    }

    public SimpleFilter getFilter()
    {
        SimpleFilter ret = new SimpleFilter();
        for (FilterInfo filter : _filters.values())
        {
            if (filter.getField() != null && filter.getValue() != null)
                ret.addCondition(filter.getField(), filter.getValue(), filter.getOp());
        }
        return ret;
    }

    public void merge(ScriptSettings that)
    {
        if (that == null)
            return;
        for (ParameterInfo paramInfo : that._parameters.values())
        {
            ParameterInfo mine = getParameterInfo(paramInfo.getName(), true);
            if (paramInfo.getMinValue() != null)
            {
                mine.setMinValue(paramInfo.getMinValue());
            }
        }

        for (FilterInfo filterInfo : that._filters.values())
        {
            FilterInfo mine = new FilterInfo(filterInfo.getField(), filterInfo.getOp(), filterInfo.getValue());
            _filters.put(mine.getField().toString(), mine);
        }
    }

    public void merge(SettingsDef settings)
    {
        if (settings == null)
            return;
        for (ParameterDef param : settings.getParameterArray())
        {
            ParameterInfo mine = getParameterInfo(param.getName(), true);
            if (param.isSetMinValue())
            {
                mine.setMinValue(param.getMinValue());
            }
        }

        FiltersDef filters = settings.getFilters();
        if (filters != null)
        {
            for (FilterDef filter : filters.getFilterArray())
            {
                FilterInfo mine = getFilter(filter.getField());
                if (mine == null)
                {
                    mine = fromFilterDef(filter);
                    _filters.put(filter.getField(), mine);
                }
            }
        }
    }

    public static ScriptSettings fromSettingsDef(SettingsDef settings)
    {
        ScriptSettings ret = new ScriptSettings();
        ret.merge(settings);
        return ret;
    }

    public SettingsDef toSettingsDef()
    {
        SettingsDef ret = SettingsDef.Factory.newInstance();
        for (ParameterInfo info : _parameters.values())
        {
            ParameterDef paramDef = ret.addNewParameter();
            paramDef.setName(info.getName());
            if (info.getMinValue() != null)
            {
                paramDef.setMinValue(info.getMinValue());
            }
        }

        if (_filters.size() > 0)
        {
            FiltersDef filtersDef = ret.addNewFilters();
            for (FilterInfo filter : _filters.values())
            {
                FilterDef filterDef = filtersDef.addNewFilter();
                filterDef.setField(filter.getField().toString());
                filterDef.setOp(OpDef.Enum.forString(filter.getOp().getPreferredUrlKey()));
                if (filter.getValue() != null)
                    filterDef.setValue(filter.getValue());
            }
        }
        return ret;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptSettings that = (ScriptSettings) o;

        if (!_parameters.equals(that._parameters)) return false;
        if (!_filters.equals(that._filters)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = _parameters.hashCode();
        result = 31 * result + _filters.hashCode();
        return result;
    }
}
