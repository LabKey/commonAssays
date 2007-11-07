package org.labkey.flow.controllers.executescript;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;

import java.util.List;
import java.util.Map;

public class ChooseRunsToUploadForm extends ViewForm
{
    public int ff_protocolId;
    public String[] ff_path;
    public String path;
    public String srcURL;

    private Map<String, String> newPaths;
    private PipeRoot pipeRoot;

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

    public Map<String, String> getNewPaths()
    {
        return newPaths;
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }
    
    public void setSrcURL(String url)
    {
        this.srcURL = url;
    }

    public PipeRoot getPipeRoot()
    {
        return pipeRoot;
    }

    public void setPipeRoot(PipeRoot pipeRoot)
    {
        this.pipeRoot = pipeRoot;
    }
}
