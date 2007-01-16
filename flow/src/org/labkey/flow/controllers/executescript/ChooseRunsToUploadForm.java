package org.labkey.flow.controllers.executescript;

import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocolStep;

import java.util.*;
import java.net.URI;

public class ChooseRunsToUploadForm extends ViewForm
{
    FlowExperiment[] experiments;

    public int ff_protocolId;
    public String[] ff_path;
    public String path;

    private List<URI> newPaths;

    public void setFf_protocolId(int id)
    {
        ff_protocolId = id;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void setFf_path(String[] paths)
    {
        this.ff_path = paths;
    }

    public List<FlowScript> getProtocols() throws Exception
    {
        return FlowScript.getProtocolsWithStep(getContainer(), FlowProtocolStep.keywords);
    }

    public FlowExperiment getExperiment()
    {
        return FlowExperiment.getExperimentRunExperiment(getContainer());
    }

    public List<URI> getNewPaths()
    {
        return newPaths;
    }

    public void setNewPaths(List<URI> paths)
    {
        this.newPaths = paths;
    }

}
