package org.fhcrc.cpas.flow.data;

import org.fhcrc.cpas.exp.api.ProtocolImplementation;
import org.fhcrc.cpas.exp.api.ExperimentService;
import org.fhcrc.cpas.exp.api.ExpProtocol;
import org.fhcrc.cpas.exp.api.ExpMaterial;
import org.fhcrc.cpas.security.User;

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
