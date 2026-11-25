/*
 * Copyright (c) 2014-2026 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LuminexSaveExclusionsForm implements ApiJsonForm
{
    public static final int MAX_RUNS = 10;

    private Integer _assayId;
    private String _tableName;
    private Long _runId;
    private final List<LuminexSingleExclusionCommand> _commands = new ArrayList<>();

    // One form per run, from the runs array or the single runId and commands
    private final List<LuminexSaveExclusionsForm> _runForms = new ArrayList<>();

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

        JSONArray runs = json.optJSONArray("runs");
        if (runs != null)
        {
            for (int i = 0; i < runs.length(); i++)
                addRunForm(runs.getJSONObject(i));
        }
        else
        {
            addRunForm(json);
        }
    }

    private void addRunForm(JSONObject runJSON)
    {
        LuminexSaveExclusionsForm runForm = new LuminexSaveExclusionsForm();
        runForm._assayId = _assayId;
        runForm._tableName = _tableName;
        runForm.bindRun(runJSON);
        _runForms.add(runForm);
    }

    private void bindRun(JSONObject json)
    {
        _runId = getLongPropIfExists(json, "runId");

        JSONArray commands = json.optJSONArray("commands");
        for (int i = 0; commands != null && i < commands.length(); i++)
        {
            JSONObject commandJSON = commands.getJSONObject(i);
            LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
            command.setCommand(commandJSON.optString("command", null));
            command.setKey(getIntPropIfExists(commandJSON, "key"));
            command.setDataId(getLongPropIfExists(commandJSON, "dataId"));
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

    public Long getRunId()
    {
        return _runId;
    }

    public List<LuminexSingleExclusionCommand> getCommands()
    {
        return _commands;
    }

    /** One form per run to save, each queued as its own job */
    public List<LuminexSaveExclusionsForm> getRunForms()
    {
        return _runForms;
    }

    private void addCommand(LuminexSingleExclusionCommand command)
    {
        _commands.add(command);
    }

    private Integer getIntPropIfExists(JSONObject json, String propName)
    {
        return json.has(propName) ? json.getInt(propName) : null;
    }

    private Long getLongPropIfExists(JSONObject json, String propName)
    {
        return json.has(propName) ? json.getLong(propName) : null;
    }

    private Double getDoublePropIfExists(JSONObject json, String propName)
    {
        return json.has(propName) ? json.getDouble(propName) : null;
    }

    public ExpProtocol getProtocol(Container c)
    {
        if (_protocol == null)
            _protocol = getProtocol(c, getAssayId());

        return _protocol;
    }

    /** Finds the in-scope Luminex assay protocol with the given RowId. */
    public static @Nullable ExpProtocol getProtocol(Container c, @Nullable Integer assayId)
    {
        if (assayId == null)
            return null;

        ExpProtocol protocol = null;
        List<ExpProtocol> protocols =  AssayService.get().getAssayProtocols(c, AssayService.get().getProvider(LuminexAssayProvider.NAME));
        for (ExpProtocol possibleMatch : protocols)
        {
            if (possibleMatch.getRowId() == assayId)
            {
                if (protocol != null)
                {
                    throw new NotFoundException("More than one assay definition with the id \"" + assayId + "\" is in scope");
                }
                protocol = possibleMatch;
            }
        }

        return protocol;
    }

    public void validate(Errors errors)
    {
        // verify that the tableName is a valid exclusion type enum
        if (getTableName() == null || !EnumUtils.isValidEnum(LuminexManager.ExclusionType.class, getTableName()))
        {
            errors.reject(null, "Invalid tableName provided for exclusion: " + getTableName());
        }

        if (_runForms.isEmpty())
            errors.reject(null, "No runs provided for exclusion.");
        else if (_runForms.size() > MAX_RUNS)
            errors.reject(null, "Exclusions can be saved for at most " + MAX_RUNS + " runs at a time.");

        Set<Long> runIds = new HashSet<>();
        for (LuminexSaveExclusionsForm runForm : _runForms)
        {
            if (runForm.getRunId() != null && !runIds.add(runForm.getRunId()))
                errors.reject(null, "Run " + runForm.getRunId() + " is listed more than once.");
            runForm.validateCommands(errors);
        }
    }

    private void validateCommands(Errors errors)
    {
        // verify that we have at least one commend
        if (getCommands().isEmpty())
        {
            errors.reject(null, "No commands provided for exclusion" + (_runId != null ? " for run " + _runId : ""));
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
            form.bindJson(new JSONObject().put("tableName", "InvalidTableName"));
            assertEquals(List.of("Invalid tableName provided for exclusion: InvalidTableName", "No commands provided for exclusion"), messages(form));

            form = new LuminexSaveExclusionsForm();
            form.bindJson(new JSONObject().put("tableName", "TitrationExclusion")
                    .put("commands", new JSONArray().put(new JSONObject().put("command", "InvalidCommand"))));
            assertEquals(List.of("Invalid command provided for exclusion: InvalidCommand"), messages(form));

            form = new LuminexSaveExclusionsForm();
            form.bindJson(new JSONObject().put("tableName", "TitrationExclusion")
                    .put("commands", new JSONArray().put(new JSONObject().put("command", "update")).put(new JSONObject().put("command", "delete"))));
            assertEquals(List.of("A key is required to update an exclusion.", "A key is required to delete an exclusion."), messages(form));

            form = new LuminexSaveExclusionsForm();
            form.bindJson(new JSONObject().put("tableName", "TitrationExclusion")
                    .put("commands", new JSONArray().put(new JSONObject().put("command", "insert"))
                            .put(new JSONObject().put("command", "update").put("key", 1))
                            .put(new JSONObject().put("command", "delete").put("key", 2))));
            assertEquals(List.of(), messages(form));
        }

        @Test
        public void testRunForms()
        {
            LuminexSaveExclusionsForm single = bind(new JSONObject()
                    .put("runId", 10)
                    .put("commands", new JSONArray().put(command(100))));
            assertEquals(1, single.getRunForms().size());
            LuminexSaveExclusionsForm singleRunForm = single.getRunForms().getFirst();
            assertEquals(Integer.valueOf(5), singleRunForm.getAssayId());
            assertEquals("WellExclusion", singleRunForm.getTableName());
            assertEquals(Long.valueOf(10), singleRunForm.getRunId());
            assertEquals(List.of(100L), singleRunForm.getCommands().stream().map(LuminexSingleExclusionCommand::getDataId).toList());

            LuminexSaveExclusionsForm multi = bind(new JSONObject().put("runs", new JSONArray()
                    .put(new JSONObject().put("runId", 10).put("commands", new JSONArray().put(command(100)).put(command(101))))
                    .put(new JSONObject().put("runId", 20).put("commands", new JSONArray().put(command(200))))));
            List<LuminexSaveExclusionsForm> runForms = multi.getRunForms();
            assertEquals(2, runForms.size());
            for (LuminexSaveExclusionsForm runForm : runForms)
            {
                assertEquals(Integer.valueOf(5), runForm.getAssayId());
                assertEquals("WellExclusion", runForm.getTableName());
            }
            assertEquals(Long.valueOf(10), runForms.get(0).getRunId());
            assertEquals(List.of(100L, 101L), runForms.get(0).getCommands().stream().map(LuminexSingleExclusionCommand::getDataId).toList());
            assertEquals(Long.valueOf(20), runForms.get(1).getRunId());
            assertEquals(List.of(200L), runForms.get(1).getCommands().stream().map(LuminexSingleExclusionCommand::getDataId).toList());
            assertFalse("No validation errors expected", validate(multi).hasErrors());
        }

        @Test
        public void testValidateRuns()
        {
            assertEquals(List.of("No runs provided for exclusion."), messages(bind(new JSONObject().put("runs", new JSONArray()))));

            LuminexSaveExclusionsForm form = bind(new JSONObject().put("runs", new JSONArray()
                    .put(new JSONObject().put("runId", 10).put("commands", new JSONArray().put(command(100))))
                    .put(new JSONObject().put("runId", 10).put("commands", new JSONArray().put(command(101))))
                    .put(new JSONObject().put("runId", 20))));
            assertEquals(List.of("Run 10 is listed more than once.", "No commands provided for exclusion for run 20"), messages(form));

            JSONArray tooManyRuns = new JSONArray();
            for (int runId = 1; runId <= MAX_RUNS + 1; runId++)
                tooManyRuns.put(new JSONObject().put("runId", runId).put("commands", new JSONArray().put(command(runId * 100L))));
            assertEquals(List.of("Exclusions can be saved for at most " + MAX_RUNS + " runs at a time."), messages(bind(new JSONObject().put("runs", tooManyRuns))));
        }

        private static JSONObject command(long dataId)
        {
            return new JSONObject().put("command", "insert").put("dataId", dataId).put("analyteRowIds", "1");
        }

        private static LuminexSaveExclusionsForm bind(JSONObject json)
        {
            LuminexSaveExclusionsForm form = new LuminexSaveExclusionsForm();
            form.bindJson(json.put("assayId", 5).put("tableName", "WellExclusion"));
            return form;
        }

        private static BindException validate(LuminexSaveExclusionsForm form)
        {
            BindException errors = new NullSafeBindException(form, "form");
            form.validate(errors);
            return errors;
        }

        private static List<String> messages(LuminexSaveExclusionsForm form)
        {
            return validate(form).getAllErrors().stream().map(ObjectError::getDefaultMessage).toList();
        }
    }
}