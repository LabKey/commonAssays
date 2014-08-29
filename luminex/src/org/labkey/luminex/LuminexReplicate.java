/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.luminex.model.LuminexWell;

/**
 * User: jeckels
 * Date: Sep 1, 2011
 */
public class LuminexReplicate
{
    private String _description;
    private Double _dilution;
    private int _dataId;
    private Double _expConc;
    private String _type;

    public LuminexReplicate(String description, Double dilution, int dataId)
    {
        _description = description;
        _dilution = dilution;
        _dataId = dataId;
    }

    public LuminexReplicate(LuminexWell well)
    {
        _description = well._dataRow.getDescription();
        _dilution = well._dataRow.getDilution();
        _dataId = well._dataRow.getData();
        _expConc = well._dataRow.getExpConc();
        _type = well._dataRow.getType();
    }

    public String getDescription()
    {
        return _description;
    }

    public Double getDilution()
    {
        return _dilution;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public Double getExpConc()
    {
        return _expConc;
    }

    public String getType()
    {
        return _type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LuminexReplicate that = (LuminexReplicate) o;

        if (_dataId != that._dataId) return false;
        if (_description != null ? !_description.equals(that._description) : that._description != null) return false;
        if (_dilution != null ? !_dilution.equals(that._dilution) : that._dilution != null) return false;
        if (_expConc != null ? !_expConc.equals(that._expConc) : that._expConc != null) return false;
        if (_type != null ? !_type.equals(that._type) : that._type != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _description != null ? _description.hashCode() : 0;
        result = 31 * result + (_dilution != null ? _dilution.hashCode() : 0);
        result = 31 * result + _dataId;
        result = 31 * result + (_expConc != null ? _expConc.hashCode() : 0);
        result = 31 * result + (_type != null ? _type.hashCode() : 0);
        return result;
    }
}
