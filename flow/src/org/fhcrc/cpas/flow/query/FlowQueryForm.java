package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.query.api.QueryForm;
import org.fhcrc.cpas.flow.data.FlowExperiment;
import org.fhcrc.cpas.flow.data.FlowRun;
import org.fhcrc.cpas.util.UnexpectedException;

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
            return new FlowSchema(getContext());
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }
}
