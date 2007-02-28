package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.flow.data.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.exp.api.ExpMaterial;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AddRunsJob extends ScriptJob
{
    private static Logger _log = Logger.getLogger(AddRunsJob.class);

    List<File> _paths;

    public AddRunsJob(ViewBackgroundInfo info, FlowProtocol protocol, List<File> paths) throws Exception
    {
        super(info, FlowExperiment.getExperimentRunExperimentName(info.getContainer()), FlowExperiment.getExperimentRunExperimentLSID(info.getContainer()), protocol, null, FlowProtocolStep.keywords);

        _paths = paths;
    }

    public void doRun() throws Exception
    {
        for (File path : _paths)
        {
            if (checkInterrupted())
                return;
            if (!checkProcessPath(path, FlowProtocolStep.keywords))
                continue;
            try
            {
                _runHandler.run(path);
            }
            catch (Throwable t)
            {
                _log.error("Exception", t);
                addStatus("Exception:" + t.toString());
            }
        }
    }
}
