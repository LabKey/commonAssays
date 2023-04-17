/*
 * Copyright (c) 2014-2018 LabKey Corporation
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

import org.apache.commons.lang3.EnumUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.action.ApiJsonForm;
import org.labkey.api.action.NullSafeBindException;
import org.labkey.api.assay.AssayService;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;

public class LuminexSaveExclusionsForm implements ApiJsonForm
{
    private Integer _assayId;
    private String _tableName;
    private Integer _runId;

    private final List<LuminexSingleExclusionCommand> _commands = new ArrayList<>();

    private transient ExpProtocol _protocol;

    // For serialization
    protected LuminexSaveExclusionsForm()
    {}

    @Override
    public void bindJson(JSONObject json)
    {
        if (json == null)
            throw new IllegalArgumentException("Empty request");

        _assayId = getIntPropIfExists(json, "assayId");
        _tableName = json.optString("tableName", null);
        _runId = getIntPropIfExists(json, "runId");

        JSONArray commands = json.getJSONArray("commands");
        for (int i = 0; i < commands.length(); i++)
        {
            JSONObject commandJSON = commands.getJSONObject(i);
            LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
            command.setCommand(commandJSON.optString("command", null));
            command.setKey(getIntPropIfExists(commandJSON, "key"));
            command.setDataId(getIntPropIfExists(commandJSON, "dataId"));
            command.setDescription(commandJSON.optString("description", null));
            command.setType(commandJSON.optString("type", null));
            command.setDilution(getDoublePropIfExists(commandJSON, "dilution"));
            command.setAnalyteRowIds(commandJSON.optString("analyteRowIds", null));
            command.setAnalyteNames(commandJSON.optString("analyteNames", null));
            command.setComment(commandJSON.optString("comment", null));
            command.setWell(commandJSON.optString("well", null));
            addCommand(command);
        }
    }

    public Integer getAssayId()
    {
        return _assayId;
    }

    public String getTableName()
    {
        return _tableName;
    }

    public Integer getRunId()
    {
        return _runId;
    }

    public List<LuminexSingleExclusionCommand> getCommands()
    {
        return _commands;
    }

    private void setTableName(String tableName)
    {
        _tableName = tableName;
    }

    private void addCommand(LuminexSingleExclusionCommand command)
    {
        _commands.add(command);
    }

    private Integer getIntPropIfExists(JSONObject json, String propName)
    {
        return json.has(propName) ? json.getInt(propName) : null;
    }

    private Double getDoublePropIfExists(JSONObject json, String propName)
    {
        return json.has(propName) ? json.getDouble(propName) : null;
    }

    public ExpProtocol getProtocol(Container c)
    {
        if (_protocol == null)
        {
            List<ExpProtocol> protocols =  AssayService.get().getAssayProtocols(c, AssayService.get().getProvider(LuminexAssayProvider.NAME));
            for (ExpProtocol possibleMatch : protocols)
            {
                if (possibleMatch.getRowId() == getAssayId())
                {
                    if (_protocol != null)
                    {
                        throw new NotFoundException("More than one assay definition with the id \"" + getAssayId() + "\" is in scope");
                    }
                    _protocol = possibleMatch;
                }
            }
        }

        return _protocol;
    }

    public void validate(Errors errors)
    {
        // verify that the tableName is a valid exclusion type enum
        if (getTableName() == null || !EnumUtils.isValidEnum(LuminexManager.ExclusionType.class, getTableName()))
        {
            errors.reject(null, "Invalid tableName provided for exclusion: " + getTableName());
        }

        // verify that we have at least one commend
        if (getCommands().size() == 0)
        {
            errors.reject(null, "No commands provided for exclusion");
        }

        for (LuminexSingleExclusionCommand command : getCommands())
        {
            command.validate(errors);
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testValidation()
        {
            LuminexSaveExclusionsForm form = new LuminexSaveExclusionsForm();
            form.setTableName("InvalidTableName");
            BindException errors = new NullSafeBindException(form, "form");
            form.validate(errors);
            List<ObjectError> allErrors = errors.getAllErrors();
            assertEquals("Expected 2 form validation errors", 2, allErrors.size());
            assertEquals("Invalid tableName provided for exclusion: InvalidTableName", allErrors.get(0).getDefaultMessage());
            assertEquals("No commands provided for exclusion", allErrors.get(1).getDefaultMessage());

            form.setTableName("TitrationExclusion");
            LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
            command.setCommand("InvalidCommand");
            form.addCommand(command);
            errors = new NullSafeBindException(form, "form");
            form.validate(errors);
            allErrors = errors.getAllErrors();
            assertEquals("Expected 1 form validation error", 1, allErrors.size());
            assertEquals("Invalid command provided for exclusion: InvalidCommand", allErrors.get(0).getDefaultMessage());

            command.setCommand("insert");
            errors = new NullSafeBindException(form, "form");
            form.validate(errors);
            assertFalse("No validation errors expected", errors.hasErrors());
        }
    }
}