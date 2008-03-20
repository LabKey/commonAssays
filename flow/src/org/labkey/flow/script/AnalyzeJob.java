package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.persist.FlowManager;

import java.io.File;

public class AnalyzeJob extends ScriptJob
{
    private static Logger _log = Logger.getLogger(AnalyzeJob.class);

    int[] _runIds;

    public AnalyzeJob(ViewBackgroundInfo info, String experimentName, String experimentLSID, FlowProtocol protocol, FlowScript analysis, FlowProtocolStep step, int[] runIds) throws Exception
    {
        super(info, experimentName, experimentLSID, protocol, analysis, step);
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
            executeHandler(run, getCompensationCalculationHandler());
            return;
        }
        else
        {
            if (!checkProcessPath(new File(run.getPath()), FlowProtocolStep.analysis))
                return;
            ensureCompensationMatrix(run);
            executeHandler(run, getAnalysisHandler());
            return;
        }
    }

    public void doRun() throws Exception
    {
        FlowManager.vacuum();

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

        FlowManager.analyze();
    }
}
