/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cnathe on 10/9/14.
 */
public class LuminexSingleExclusionCommand
{
    private String _command;
    private Integer _key;
    private Integer _dataId;
    private String _description;
    private String _type;
    private String _analyteRowIds;
    private String _analyteNames;
    private String _comment;

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
        return _analyteRowIds;
    }

    public void setAnalyteRowIds(String analyteRowIds)
    {
        _analyteRowIds = analyteRowIds;
    }

    public String getAnalyteNames()
    {
        return _analyteNames;
    }

    public void setAnalyteNames(String analyteNames)
    {
        _analyteNames = analyteNames;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
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
}
