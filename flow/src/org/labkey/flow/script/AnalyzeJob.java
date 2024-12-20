/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AnalyzeJob extends ScriptJob
{
    private static Logger _log = LogManager.getLogger(AnalyzeJob.class);

    int[] _runIds;

    @JsonCreator
    protected AnalyzeJob(
            @JsonProperty("_pendingRunLSIDs") List<String> pendingRunLSIDs,
            @JsonProperty("_processedRunLSIDs") Map<FlowProtocolStep, List<String>> processedRunLSIDs,
            @JsonProperty("_runAnalysisScript") FlowScript runAnalysisScript)
    {
        super(pendingRunLSIDs, processedRunLSIDs, runAnalysisScript);
    }

    public AnalyzeJob(ViewBackgroundInfo info, String experimentName, String experimentLSID, FlowProtocol protocol, FlowScript analysis, FlowProtocolStep step, int[] runIds, PipeRoot root) throws Exception
    {
        super(info, experimentName, experimentLSID, protocol, analysis, step, root);
        _runIds = runIds;
    }

    protected String getRunName(String name)
    {
        return "Analysis " + name;
    }

    public void processRun(FlowRun run) throws Exception
    {
        if (_step == FlowProtocolStep.calculateCompensation)
        {
            if (!checkProcessPath(new File(run.getPath()), FlowProtocolStep.calculateCompensation))
                return;
            executeHandler(run, getCompensationCalculationHandler());
        }
        else
        {
            if (!checkProcessPath(new File(run.getPath()), FlowProtocolStep.analysis))
                return;
            ensureCompensationMatrix(run);
            executeHandler(run, getAnalysisHandler());
            runPostAnalysisJobs();
        }
    }

    @Override
    public void doRun()
    {
        for (int runId : _runIds)
        {
            FlowRun srcRun = FlowRun.fromRunId(runId);
            if (checkInterrupted())
                return;
            try
            {
                processRun(srcRun);
            }
            catch (Throwable t)
            {
                _log.error("Exception", t);
                error("Exception: " + t);
            }
        }
    }
}
