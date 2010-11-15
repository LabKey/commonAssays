package org.labkey.flow.script;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;

import java.sql.SQLException;

/**
 * User: kevink
 */
public class ImportJob extends FlowExperimentJob
{
    private String _runName = null;

    public ImportJob(ViewBackgroundInfo info, PipeRoot root, FlowExperiment experiment, FlowProtocol protocol) throws Exception
    {
        super(info, root, experiment.getLSID(), protocol, experiment.getName(), FlowProtocolStep.analysis);
    }

    @Override
    protected void doRun() throws Throwable
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ActionURL urlData()
    {
        FlowExperiment experiment = getExperiment();
        if (experiment == null)
            return null;
        return experiment.urlShow();
    }

    @Override
    public String getDescription()
    {
        return "Import " + _runName;
    }
}
