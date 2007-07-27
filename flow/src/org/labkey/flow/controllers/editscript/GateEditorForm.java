package org.labkey.flow.controllers.editscript;

import org.labkey.api.view.ViewForm;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.FlowPreference;
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
        int ret = runId;
        if (ret == 0)
        {
            ret = FlowPreference.editScriptRunId.getIntValue(getRequest());
            try
            {
                if (ret == 0)
                {
                    FlowRun[] runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
                    if (runs.length > 0)
                    {
                        ret = runs[0].getRunId();
                    }

                }
            }
            catch (SQLException e)
            {
                throw UnexpectedException.wrap(e);
            }
        }
        return ret;
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
