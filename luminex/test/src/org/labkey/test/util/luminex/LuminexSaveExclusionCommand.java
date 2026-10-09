/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.util.luminex;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

/**
 * Wrapper for luminex-saveExclusion.api, which queues a pipeline job to apply the exclusion commands.
 */
public class LuminexSaveExclusionCommand extends PostCommand<CommandResponse>
{
    private final int _assayId;
    private final long _runId;
    private final JSONArray _commands = new JSONArray();

    public LuminexSaveExclusionCommand(int assayId, long runId)
    {
        super("luminex", "saveExclusion");
        _assayId = assayId;
        _runId = runId;
    }

    public LuminexSaveExclusionCommand addWellExclusion(long dataId, String description, String type, String well, long analyteRowId, String comment)
    {
        JSONObject command = new JSONObject();
        command.put("command", "insert");
        command.put("dataId", dataId);
        command.put("description", description);
        command.put("type", type);
        command.put("well", well);
        command.put("analyteRowIds", String.valueOf(analyteRowId));
        command.put("comment", comment);
        _commands.put(command);
        return this;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("assayId", _assayId);
        json.put("tableName", "WellExclusion");
        json.put("runId", _runId);
        json.put("commands", _commands);
        return json;
    }
}
