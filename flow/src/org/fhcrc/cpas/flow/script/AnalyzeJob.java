package org.fhcrc.cpas.flow.script;

import org.fhcrc.cpas.view.ViewBackgroundInfo;
import org.fhcrc.cpas.flow.data.FlowProtocolStep;
import org.fhcrc.cpas.flow.data.FlowRun;
import org.fhcrc.cpas.flow.data.FlowScript;
import org.fhcrc.cpas.flow.data.FlowProtocol;
import org.apache.log4j.Logger;

import java.io.File;

public class AnalyzeJob extends ScriptJob
{
    private static Logger _log = Logger.getLogger(AnalyzeJob.class);

    int[] _runIds;
    String _compensationExperimentLSID;

    public AnalyzeJob(ViewBackgroundInfo info, String experimentName, String experimentLSID, FlowProtocol protocol, FlowScript analysis, FlowProtocolStep step, int[] runIds) throws Exception
    {
        super(info, experimentName, experimentLSID, protocol, analysis, step);
        _experimentLSID = experimentLSID;
        _experimentName = experimentName;
        _runIds = runIds;
    }

    protected String getRunName(String name)
    {
        return "Analysis " + name;
    }

    public void processRun(FlowRun run) throws Exception
    {
        if (_step == FlowProtocolStep.calculateCompensation)
        {
            if (!checkProcessPath(new File(run.getPath()), FlowProtocolStep.calculateCompensation))
                return;
            executeHandler(run, _compensationCalculationHandler);
            return;
        }
        else
        {
            if (!checkProcessPath(new File(run.getPath()), FlowProtocolStep.analysis))
                return;
            ensureCompensationMatrix(run);
            executeHandler(run, _analysisHandler);
            return;
        }
    }

    public void doRun() throws Exception
    {
        for (int i = 0; i < _runIds.length; i ++)
        {
            FlowRun srcRun = FlowRun.fromRunId(_runIds[i]);
            if (checkInterrupted())
                return;
            try
            {
                processRun(srcRun);
            }
            catch (Throwable t)
            {
                _log.error("Exception", t);
                addError(null, null, "Exception: " + t);
            }
        }
    }
}
