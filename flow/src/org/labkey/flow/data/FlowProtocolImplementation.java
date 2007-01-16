package org.labkey.flow.data;

import org.labkey.api.exp.api.ProtocolImplementation;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.security.User;

import java.util.List;

public class FlowProtocolImplementation extends ProtocolImplementation
{
    static public final String NAME = "flow";
    static public void register()
    {
        ExperimentService.registerProtocolImplementation(new FlowProtocolImplementation());
    }
    public FlowProtocolImplementation()
    {
        super(NAME);
    }

    public void onSamplesChanged(User user, ExpProtocol expProtocol, ExpMaterial[] materials) throws Exception
    {
        FlowProtocol protocol = new FlowProtocol(expProtocol);
        protocol.updateSampleIds(user);
    }
}
