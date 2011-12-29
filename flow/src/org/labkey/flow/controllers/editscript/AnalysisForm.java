/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang3.StringUtils;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;

import java.util.List;
import java.util.ArrayList;

/**
 * User: kevink
 * Date: Nov 27, 2008 11:28:01 AM
 */
public class AnalysisForm extends EditScriptForm
{
    public String subsets;
    public String statistics;
    public String graphs;

    @Override
    public void reset()
    {
        super.reset();
        if (analysisDocument == null)
            return;
        ScriptDef script = analysisDocument.getScript();
        if (script == null)
            return;
        AnalysisDef analysis = script.getAnalysis();
        if (analysis == null)
            return;
        List<SubsetSpec> subsets = new ArrayList();
        for (SubsetDef subset : analysis.getSubsetArray())
        {
            subsets.add(SubsetSpec.fromEscapedString(subset.getSubset()));
        }
        this.subsets = StringUtils.join(subsets.iterator(), "\n");
        List<StatisticSpec> stats = new ArrayList();
        for (StatisticDef stat : analysis.getStatisticArray())
        {
            stats.add(ScriptAnalyzer.makeStatisticSpec(stat));
        }
        statistics = StringUtils.join(stats.iterator(), "\n");
        List<GraphSpec> graphs = new ArrayList();
        for (GraphDef graph : analysis.getGraphArray())
        {
            graphs.add(ScriptAnalyzer.makeGraphSpec(graph));
        }
        this.graphs = StringUtils.join(graphs.iterator(), "\n");
    }

    public void setSubsets(String subsets)
    {
        this.subsets = subsets;
    }

    public void setGraphs(String graphs)
    {
        this.graphs = graphs;
    }

    public void setStatistics(String statistics)
    {
        this.statistics = statistics;
    }


}
