package org.labkey.luminex;

import org.apache.commons.lang3.EnumUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.action.NullSafeBindException;
import org.labkey.api.action.SimpleApiJsonForm;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Cory on 10/13/14.
 */
public class LuminexSaveExclusionsForm extends SimpleApiJsonForm
{
    private String _assayName;
    private String _tableName;
    private Integer _runId;
    private List<LuminexSingleExclusionCommand> _commands = new ArrayList<>();
    private ExpProtocol _protocol;

    @Override
    public void bindProperties(Map<String, Object> properties)
    {
        super.bindProperties(properties);

        JSONObject json = getJsonObject();
        if (json == null)
            throw new IllegalArgumentException("Empty request");

        _assayName = getStringPropIfExists(json, "assayName");
        _tableName = getStringPropIfExists(json, "tableName");
        _runId = getIntPropIfExists(json, "runId");

        JSONArray commands = (JSONArray)json.get("commands");
        for (int i = 0; i < commands.length(); i++)
        {
            JSONObject commandJSON = commands.getJSONObject(i);
            LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
            command.setCommand(getStringPropIfExists(commandJSON, "command"));
            command.setKey(getIntPropIfExists(commandJSON, "key"));
            command.setDataId(getIntPropIfExists(commandJSON, "dataId"));
            command.setDescription(getStringPropIfExists(commandJSON, "description"));
            command.setType(getStringPropIfExists(commandJSON, "type"));
            command.setAnalyteRowIds(getStringPropIfExists(commandJSON, "analyteRowIds"));
            command.setAnalyteNames(getStringPropIfExists(commandJSON, "analyteNames"));
            command.setComment(getStringPropIfExists(commandJSON, "comment"));
            addCommand(command);
        }
    }

    public String getAssayName()
    {
        return _assayName;
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

    private String getStringPropIfExists(JSONObject json, String propName)
    {
        return json.containsKey(propName) ? json.getString(propName) : null;
    }

    private Integer getIntPropIfExists(JSONObject json, String propName)
    {
        return json.containsKey(propName) ? json.getInt(propName) : null;
    }

    public ExpProtocol getProtocol(Container c)
    {
        if (_protocol == null)
        {
            List<ExpProtocol> protocols =  AssayService.get().getAssayProtocols(c, AssayService.get().getProvider(LuminexAssayProvider.NAME));
            for (ExpProtocol possibleMatch : protocols)
            {
                if (possibleMatch.getName().equalsIgnoreCase(getAssayName()))
                {
                    if (_protocol != null)
                    {
                        throw new NotFoundException("More than one assay definition with the name '" + getAssayName() + "' is in scope");
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
        if (getTableName() == null || !EnumUtils.isValidEnum(LuminexExclusionPipelineJob.ExclusionType.class, getTableName()))
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
            assertTrue("No validation errors expected", !errors.hasErrors());
        }
    }
}