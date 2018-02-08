/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.luminex.model.Analyte;
import org.springframework.validation.Errors;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuminexSingleExclusionCommand
{
    private String _command;
    private Integer _key;
    private Integer _dataId;
    private String _description;
    private Double _dilution;
    private String _type;
    private List<String> _analyteRowIds = new ArrayList<>();
    private List<String> _analyteNames = new ArrayList<>();
    private String _comment;
    private String _well;

    private Integer _createdBy;
    private Timestamp _created;

    public Timestamp getCreated()
    {
        return _created;
    }

    public void setCreated(Timestamp created)
    {
        _created = created;
    }

    public Integer getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(Integer createdBy)
    {
        _createdBy = createdBy;
    }

    public Integer getKey()
    {
        return _key;
    }

    public void setKey(Integer key)
    {
        _key = key;
    }

    public Integer getDataId()
    {
        return _dataId;
    }

    public void setDataId(Integer dataId)
    {
        _dataId = dataId;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Double getDilution()
    {
        return _dilution;
    }

    public void setDilution(Double dilution)
    {
        _dilution = dilution;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }

    public String getAnalyteRowIds()
    {
        return String.join(",", _analyteRowIds);
    }

    public void setAnalyteRowIds(String analyteRowIds)
    {
        _analyteRowIds = StringUtils.isNotBlank(analyteRowIds) ?
                Arrays.asList(analyteRowIds.split(",")) : new ArrayList<>();
    }
    public String getAnalyteNames()
    {
        return String.join(",",_analyteNames);
    }

    public void setAnalyteNames(String analyteNames)
    {
        _analyteNames = StringUtils.isNotBlank(analyteNames) ?
                Arrays.asList(analyteNames.split(",")) : new ArrayList<>();
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public String getWell()
    {
        return _well;
    }

    public void setWell(String well)
    {
        _well = well;
    }

    public String getCommand()
    {
        return _command;
    }

    public void setCommand(String command)
    {
        _command = command;
    }

    public Map<String, Object> getBaseRowMap()
    {
        // include those properties that are either the same for all exclusion types or ignored
        Map<String, Object> row = new HashMap<>();
        row.put("Description", getDescription());
        row.put("DataId", getDataId());
        row.put("Comment", getComment());
        row.put("AnalyteId/RowId", getAnalyteRowIds());
        row.put("CreatedBy", getCreatedBy());
        row.put("Created", getCreated());

        return row;
    }

    public void validate(Errors errors)
    {
        // verify that we have a valid insert/update/delete command
        if (!"insert".equals(getCommand()) && !"update".equals(getCommand()) && !"delete".equals(getCommand()))
        {
            errors.reject(null, "Invalid command provided for exclusion: " + getCommand());
        }
    }

    public void addAnalyte(Analyte analyte)
    {
        _analyteNames.add(analyte.getName());
        _analyteRowIds.add(String.valueOf(analyte.getRowId()));
    }

    public LuminexManager.ExclusionType getExclusionType()
    {
        if (getType() == null && getDilution() == null)
            return LuminexManager.ExclusionType.TitrationExclusion;
        else if (getDilution() != null)
            return LuminexManager.ExclusionType.SinglepointUnknownExclusion;
        else
            return LuminexManager.ExclusionType.WellExclusion;
    }
}
