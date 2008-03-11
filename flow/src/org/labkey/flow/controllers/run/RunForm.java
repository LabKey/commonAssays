package org.labkey.flow.controllers.run;

import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;

public class RunForm extends FlowQueryForm
{
    private int _runid = 0;
    private FlowRun _run = null;

    public void setRunId(int id)
    {
        if (id != _runid)
            _run = null;
        _runid = id;
    }

    protected FlowSchema createSchema()
    {
        FlowSchema ret = super.createSchema();
        ret.setRun(getRun());
        return ret;
    }

    public FlowRun getRun()
    {
        if (null == _run && 0 != _runid)
            _run = FlowRun.fromRunId(_runid);
        return _run;
    }

    public QuerySettings createQuerySettings(UserSchema schema)
    {
        QuerySettings ret = super.createQuerySettings(schema);
        if (ret.getQueryName() == null && getRun() != null)
        {
            String queryName = getRun().getDefaultQuery().toString();
            ret.setQueryName(queryName);
        }
        return ret;
    }
}
