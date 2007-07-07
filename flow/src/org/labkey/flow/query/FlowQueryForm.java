package org.labkey.flow.query;

import org.labkey.api.query.QueryForm;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;
import org.labkey.api.util.UnexpectedException;

public class FlowQueryForm extends QueryForm
{
    public String getSchemaName()
    {
        return FlowSchema.SCHEMANAME;
    }

    protected FlowSchema createSchema()
    {
        try
        {
            return new FlowSchema(getViewContext());
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }
}
