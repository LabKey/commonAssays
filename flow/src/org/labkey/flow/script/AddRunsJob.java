package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.SampleKey;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.exp.api.ExpMaterial;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AddRunsJob extends ScriptJob
{
    private static Logger _log = Logger.getLogger(AddRunsJob.class);

    List<File> _paths;

    public AddRunsJob(ViewBackgroundInfo info, String experimentName, String experimentLSID, FlowProtocol protocol, FlowScript script, List<File> paths) throws Exception
    {
        super(info, experimentName, experimentLSID, protocol, script, FlowProtocolStep.keywords);

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
