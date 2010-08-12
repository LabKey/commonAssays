/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.flow.data.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.exp.api.ExpMaterial;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class AddRunsJob extends ScriptJob
{
    private static Logger _log = Logger.getLogger(AddRunsJob.class);

    List<File> _paths;

    public AddRunsJob(ViewBackgroundInfo info, FlowProtocol protocol, List<File> paths, PipeRoot root) throws Exception
    {
        super(info, FlowExperiment.getExperimentRunExperimentName(info.getContainer()), FlowExperiment.getExperimentRunExperimentLSID(info.getContainer()), protocol, null, FlowProtocolStep.keywords, root);

        _paths = paths;
    }

    public void doRun() throws Exception
    {
        go();
    }

    List<FlowRun> go() throws Exception
    {
        List<FlowRun> runs = new ArrayList<FlowRun>();
        for (File path : _paths)
        {
            if (checkInterrupted())
                return runs;
            if (!checkProcessPath(path, FlowProtocolStep.keywords))
                continue;
            try
            {
                runs.add(getRunHandler().run(path));
            }
            catch (Throwable t)
            {
                _log.error("Exception", t);
                addStatus("Exception:" + t.toString());
            }
        }
        return runs;
    }
}
