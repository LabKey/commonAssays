package Flow.Run;

import org.fhcrc.cpas.flow.data.*;
import org.fhcrc.cpas.flow.query.FlowQueryForm;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.query.FlowTableType;
import org.fhcrc.cpas.query.api.QuerySettings;

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
