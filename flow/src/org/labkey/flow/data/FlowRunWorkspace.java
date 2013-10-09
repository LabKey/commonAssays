/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.model.ParameterInfo;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.script.FlowAnalyzer;

import java.util.Map;

public class FlowRunWorkspace extends Workspace
{
    public FlowRunWorkspace(FlowScript analysisScript, FlowProtocolStep step, FlowRun run) throws Exception
    {
        Analysis analysis;
        PopulationSet compCalcOrAnalysis = analysisScript.getCompensationCalcOrAnalysis(step);
        if (compCalcOrAnalysis instanceof Analysis)
        {
            analysis = (Analysis) compCalcOrAnalysis;
        }
        else
        {
            analysis = new Analysis();
            for (Population pop : compCalcOrAnalysis.getPopulations())
            {
                analysis.addPopulation(pop);
            }
        }
        _groupAnalyses.put(PopulationName.fromString("analysis"), analysis);
        for (FlowFCSFile well : run.getFCSFiles())
        {
            String key = Integer.toString(well.getWellId());
            SampleInfo info = new SampleInfo();
            info.setSampleName(well.getName());
            info.setSampleId(key);
            FCSKeywordData fcs = FCSAnalyzer.get().readAllKeywords(FlowAnalyzer.getFCSRef(well));
            info.putAllKeywords(fcs.getAllKeywords());
            _sampleInfos.put(info.getSampleId(), info);
            _sampleAnalyses.put(info.getSampleId(), analysis);
            Map<String,String> params = FlowAnalyzer.getParameters(well, null);
            for (String param : params.keySet())
            {
                if (_parameters.containsKey(param))
                    continue;
                ParameterInfo paramInfo = new ParameterInfo(param);
                paramInfo.setMultiplier(1d);
                _parameters.put(param, paramInfo);
            }
        }
    }

    @Override
    public String getKindName()
    {
        return "FlowRun Workspace";
    }

}
