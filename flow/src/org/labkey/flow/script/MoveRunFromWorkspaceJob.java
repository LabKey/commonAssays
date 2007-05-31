package org.labkey.flow.script;

import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;

public class MoveRunFromWorkspaceJob extends ScriptJob
{
    FlowRun _run;
    public MoveRunFromWorkspaceJob(ViewBackgroundInfo info, FlowExperiment experiment, FlowRun run) throws Exception
    {
        super(info, experiment.getName(), experiment.getLSID(), new FlowProtocol(run.getExperimentRun().getProtocol()), run.getScript(), FlowProtocolStep.analysis);
        _analysisHandler._getScriptFromWells = true;
        _run = run;
    }


    protected void doRun() throws Throwable
    {
        executeHandler(_run, _analysisHandler);
        if (!_errors)
        {
            ExperimentService.get().deleteExperimentRun(_run.getRunId(), getContainer());
        }
    }
}
