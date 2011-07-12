/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;

public class MoveRunFromWorkspaceJob extends ScriptJob
{
    FlowRun _run;
    public MoveRunFromWorkspaceJob(ViewBackgroundInfo info, FlowExperiment experiment, FlowRun run, PipeRoot root) throws Exception
    {
        super(info, experiment.getName(), experiment.getLSID(), new FlowProtocol(run.getExperimentRun().getProtocol()), run.getScript(), FlowProtocolStep.analysis, root);
        _run = run;
    }


    protected void doRun() throws Throwable
    {
        getAnalysisHandler()._getScriptFromWells = true;
        executeHandler(_run, getAnalysisHandler());
        if (!hasErrors())
        {
            _run.getExperimentRun().delete(getUser());
        }
        else
        {
            runPostAnalysisJobs();
        }
    }
}
