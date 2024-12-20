/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create a Keyword run for directory containing FCS files not previously imported.
 */
public class KeywordsJob extends ScriptJob
{
    private static final Logger _log = LogManager.getLogger(KeywordsJob.class);

    private final List<File> _paths;
    private final Container _targetStudy;

    @JsonCreator
    protected KeywordsJob(
            @JsonProperty("_pendingRunLSIDs") List<String> pendingRunLSIDs,
            @JsonProperty("_processedRunLSIDs") Map<FlowProtocolStep, List<String>> processedRunLSIDs,
            @JsonProperty("_runAnalysisScript") FlowScript runAnalysisScript,
            @JsonProperty("_paths") List<File> paths,
            @JsonProperty("_targetStudy") Container targetStudy
    )
    {
        super(pendingRunLSIDs, processedRunLSIDs, runAnalysisScript);
        _paths = paths;
        _targetStudy = targetStudy;
    }

    public KeywordsJob(ViewBackgroundInfo info, FlowProtocol protocol, List<File> paths, Container targetStudy, PipeRoot root) throws IOException
    {
        super(info, FlowExperiment.getExperimentRunExperimentName(info.getContainer()), FlowExperiment.getExperimentRunExperimentLSID(info.getContainer()), protocol, null, FlowProtocolStep.keywords, root);

        _paths = paths;
        _targetStudy = targetStudy;
    }

    @Override
    public void doRun()
    {
        go();
    }

    public List<FlowRun> go()
    {
        List<FlowRun> runs = new ArrayList<>();

        for (File path : _paths)
        {
            if (checkInterrupted())
                return runs;

            if (!checkProcessPath(path, FlowProtocolStep.keywords))
                continue;

            try
            {
                runs.add(getRunHandler().run(path, _targetStudy));
            }
            catch (Throwable t)
            {
                _log.error("Exception", t);
                error("Failed to import keywords from '" + path + "': " + t.getMessage(), t);
            }
        }

        return runs;
    }
}
