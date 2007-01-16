package org.labkey.flow.controllers.run;

import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.api.query.QuerySettings;

public class RunForm extends FlowQueryForm
{
    public FlowRun _run;
    public void setRunId(int id)
    {
        _run = FlowRun.fromRunId(id);
    }

    protected FlowSchema createSchema()
    {
        FlowSchema ret = super.createSchema();
        ret.setRun(_run);
        return ret;
    }

    public FlowRun getRun()
    {
        return _run;
    }

    public QuerySettings createQuerySettings()
    {
        QuerySettings ret = super.createQuerySettings();
        if (ret.getQueryName() == null)
        {
            String queryName = null;
            FlowProtocolStep step = _run.getStep();
            if (step == FlowProtocolStep.calculateCompensation)
            {
                queryName = FlowTableType.CompensationControls.toString();
            }
            else if (step == FlowProtocolStep.analysis)
            {
                queryName =  FlowTableType.FCSAnalyses.toString();
            }
            else
            {
                queryName =  FlowTableType.FCSFiles.toString();
            }
            ret.setQueryName(queryName);
        }
        return ret;
    }
}
