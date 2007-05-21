package org.labkey.flow.data;

import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.script.FlowAnalyzer;

import java.util.Map;
import java.util.EnumSet;

public class FlowRunWorkspace extends FlowJoWorkspace
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
        _groupAnalyses.put("analysis", analysis);
        for (FlowWell well : run.getWells())
        {
            String key = Integer.toString(well.getWellId());
            SampleInfo info = new SampleInfo();
            info.setSampleId(key);
            FCSKeywordData fcs = FCSAnalyzer.get().readAllKeywords(FlowAnalyzer.getFCSRef(well));
            info.getKeywords().putAll(fcs.getAllKeywords());
            _sampleInfos.put(info.getSampleId(), info);
            _sampleAnalyses.put(info.getSampleId(), analysis);
            Map<String,String> params = FlowAnalyzer.getParameters(well, null);
            for (String param : params.keySet())
            {
                if (_parameters.containsKey(param))
                    continue;
                ParameterInfo paramInfo = new ParameterInfo();
                paramInfo.name = param;
                paramInfo.multiplier = 1;
                _parameters.put(param, paramInfo);
            }
        }
    }
}
