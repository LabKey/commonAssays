/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ViewForm;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.gateeditor.client.model.GWTEditingMode;

import java.sql.SQLException;

public class GateEditorForm extends ViewForm
{
    int runId;
    int scriptId;
    int actionSequence;
    String editingMode;
    String subset;

    public int getActionSequence()
    {
        return actionSequence;
    }

    public void setActionSequence(int actionSequence)
    {
        this.actionSequence = actionSequence;
    }

    public int getRunId()
    {
        if (runId != 0)
            return runId;
        
        try
        {
            int savedId = FlowPreference.editScriptRunId.getIntValue(getRequest());
            if (savedId != 0)
            {
                FlowRun run = FlowRun.fromRunId(savedId);
                // ignore the saved run 'preference' if it is not in the current container
                if (null != run && getContainer().getId().equals(run.getContainerId()))
                    return savedId;
            }
            
            FlowRun[] runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            if (runs.length > 0)
                return runs[0].getRunId();
            return 0;
        }
        catch (SQLException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public void setRunId(int runId)
    {
        this.runId = runId;
    }

    public int getScriptId()
    {
        return scriptId;
    }

    public void setScriptId(int scriptId)
    {
        this.scriptId = scriptId;
    }

    public GWTEditingMode getEditingMode()
    {
        if (scriptId == 0)
        {
            return GWTEditingMode.run;
        }
        if (actionSequence == FlowProtocolStep.calculateCompensation.getDefaultActionSequence())
        {
            return GWTEditingMode.compensation;
        }
        if (actionSequence == FlowProtocolStep.analysis.getDefaultActionSequence())
        {
            return GWTEditingMode.analysis;
        }
        FlowScript script = FlowScript.fromScriptId(scriptId);
        if (script.hasStep(FlowProtocolStep.analysis))
        {
            return GWTEditingMode.analysis;
        }
        if (script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            return GWTEditingMode.compensation;
        }
        return null;
    }

    public FlowObject getFlowObject()
    {
        if (scriptId != 0)
        {
            return FlowScript.fromScriptId(scriptId);
        }
        if (runId != 0)
        {
            return FlowRun.fromRunId(runId);
        }
        return null;
    }


    public String getSubset()
    {
        return subset;
    }

    public void setSubset(String subset)
    {
        this.subset = subset;
    }
}
