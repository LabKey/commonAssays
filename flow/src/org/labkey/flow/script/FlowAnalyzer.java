/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.*;
import org.labkey.flow.data.*;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class FlowAnalyzer
{
    static public List<FCSRef> getFCSRefs(FlowRun run)
    {
        FlowWell[] wells = run.getWells();
        List<FCSRef> refs = new ArrayList();
        for (FlowWell well : wells)
        {
            if (well instanceof FlowFCSFile)
            {
                refs.add(getFCSRef(well));
            }
        }
        return refs;
    }


    static public URI getFCSUri(FlowWell well)
    {
        return well.getFCSURI();
    }

    static public FCSRef getFCSRef(FlowWell well)
    {
        Map<String, String> overrides = new HashMap();
        overrides.putAll(well.getKeywords());
        return new FCSRef(well.getFCSURI(), overrides);
    }

    static public FCSAnalyzer.GraphResult generateGraph(FlowWell well, FlowScript script, FlowProtocolStep step, FlowCompensationMatrix comp, GraphSpec graph) throws Exception
    {
        ScriptComponent group = null;
        if (script != null)
        {
            ScriptDef scriptElement = script.getAnalysisScriptDocument().getScript();
            if (step == FlowProtocolStep.calculateCompensation)
            {
                group = ScriptAnalyzer.makeCompensationCalculation(scriptElement.getSettings(), scriptElement.getCompensationCalculation());
            }
            if (step == FlowProtocolStep.analysis)
            {
                group = ScriptAnalyzer.makeAnalysis(scriptElement.getSettings(), scriptElement.getAnalysis());
            }
        }
        else
        {
            group = new Analysis();
        }
        CompensationMatrix matrix = comp == null ? null : comp.getCompensationMatrix();
        return FCSAnalyzer.get().generateGraphs(getFCSUri(well), matrix, group, Collections.singletonList(graph)).get(0);
    }

    static public Map<String, String> getParameters(FlowWell well) throws Exception
    {
        FlowRun run = well.getRun();
        FlowCompensationMatrix flowComp = well.getCompensationMatrix();
        CompensationMatrix comp = null;
        if (flowComp != null)
        {
            comp = flowComp.getCompensationMatrix();
        }
        return FCSAnalyzer.get().getParameterNames(getFCSUri(well), comp);
    }

    static public Map<String, String> getParameters(FlowWell well, CompensationMatrix comp) throws Exception
    {
        return FCSAnalyzer.get().getParameterNames(getFCSUri(well), comp);
    }

    static public Collection<SubsetSpec> getSubsets(FlowScript script)
    {
        if (script == null)
        {
            return Collections.emptyList();
        }
        return ScriptAnalyzer.getSubsets(script.getAnalysisScript(), true, false, true);
    }

    static public CompensationMatrix getCompensationMatrix(FlowRun run)
    {
        FlowCompensationMatrix comp = run.getCompensationMatrix();
        if (comp == null)
            return null;
        return comp.getCompensationMatrix();
    }

    synchronized static public File getAnalysisDirectory()
    {
        return FlowSettings.getWorkingDirectory();
    }
}
