/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
