/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.query.FlowSchema;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Also known as "Analysis Folder" in flow terms or "Run Group" in exp terms.
 */
public class FlowExperiment extends FlowObject<ExpExperiment>
{
    static private final Logger _log = Logger.getLogger(FlowExperiment.class);
    static public String FlowExperimentRunExperimentName = "Flow Experiment Runs";
    static public String FlowWorkspaceExperimentName = "Flow Workspace";
    static public String DEFAULT_ANALYSIS_NAME = "Analysis";

    public FlowExperiment(ExpExperiment experiment)
    {
        super(experiment);
    }

    static public FlowExperiment fromLSID(String lsid)
    {
        if (lsid == null)
            return null;
        ExpExperiment exp = ExperimentService.get().getExpExperiment(lsid);
        if (exp == null)
            return null;
        return new FlowExperiment(exp);
    }

    static public FlowExperiment fromExperimentId(int id)
    {
        ExpExperiment experiment = ExperimentService.get().getExpExperiment(id);
        if (experiment == null)
            return null;
        return new FlowExperiment(experiment);
    }

    static public FlowExperiment[] getExperiments(Container container)
    {
        ExperimentService svc = ExperimentService.get();
        List<? extends ExpExperiment> experiments = svc.getExperiments(container, null, false, true);
        FlowExperiment[] ret = new FlowExperiment[experiments.size()];
        for (int i = 0; i < experiments.size(); i ++)
        {
            ret[i] = new FlowExperiment(experiments.get(i));
        }
        return ret;
    }

    static public FlowExperiment[] getAnalyses(Container container)
    {
        List<FlowExperiment> ret = new ArrayList();
        for (FlowExperiment experiment : getExperiments(container))
        {
            if (experiment.isAnalysis())
            {
                ret.add(experiment);
            }
        }
        return ret.toArray(new FlowExperiment[0]);
    }

    /**
     * Generate an unused FlowExperiment name using "Analysis" as the starting name.
     * @param container
     * @return
     */
    static public String generateUnusedName(Container container)
    {
        return generateUnusedName(container, DEFAULT_ANALYSIS_NAME);
    }

    static public String generateUnusedName(Container container, String baseName)
    {
        Set<String> namesInUse = new HashSet<>();
        for (FlowExperiment analysis : FlowExperiment.getAnalyses(container))
            namesInUse.add(analysis.getName().toLowerCase());

        String newAnalysisName = baseName;
        int nameIndex = 0;
        while (namesInUse.contains(newAnalysisName.toLowerCase()))
        {
            nameIndex++;
            newAnalysisName = baseName + nameIndex;
        }

        return newAnalysisName;
    }

    static public FlowExperiment getForName(User user, Container container, String name)
    {
        String lsid = ExperimentService.get().generateLSID(container, ExpExperiment.class, name);
        return FlowExperiment.fromLSID(lsid);
    }

    static public FlowExperiment createForName(User user, Container container, String name)
    {
        FlowExperiment ret = getForName(user, container, name);
        if (ret != null)
        {
            return ret;
        }
        ExpExperiment exp = ExperimentService.get().createExpExperiment(container, name);
        exp.save(user);
        return new FlowExperiment(exp);
    }

    static public FlowExperiment[] getAnalysesAndWorkspace(Container container)
    {
        List<FlowExperiment> ret = new ArrayList();
        for (FlowExperiment experiment : getExperiments(container))
        {
            if (experiment.isAnalysis() || experiment.isWorkspace())
            {
                ret.add(experiment);
            }
        }
        return ret.toArray(new FlowExperiment[0]);
    }

    static public FlowExperiment getDefaultAnalysis(Container container)
    {
        FlowExperiment[] experiments = getAnalyses(container);
        if (experiments.length == 0)
            return null;
        return experiments[0];
    }

    static public String getExperimentRunExperimentLSID(Container container)
    {
        return FlowObject.generateLSID(container, "Experiment", FlowExperimentRunExperimentName);
    }

    static public String getExperimentRunExperimentName(Container container)
    {
        return FlowExperimentRunExperimentName;
    }

    static public String getWorkspaceLSID(Container container)
    {
        return FlowObject.generateLSID(container, "Experiment", FlowWorkspaceExperimentName);
    }

    static public String getWorkspaceRunExperimentName(Container container)
    {
        return FlowWorkspaceExperimentName;
    }

    static public FlowExperiment fromURL(ActionURL url, Container actionContainer, User user)
    {
        return fromURL(url, null, actionContainer, user);
    }

    static public FlowExperiment fromURL(ActionURL url, HttpServletRequest request, Container actionContainer, User user)
    {
        int expid = getIntParam(url, request, FlowParam.experimentId);
        if (0 == expid)
            return null;
        FlowExperiment ret = fromExperimentId(expid);
        if (ret == null)
        {
            return null;
        }
        ret.checkContainer(actionContainer,user,url);
        return ret;
    }

    public ExpExperiment getExperiment()
    {
        return getExpObject();
    }

    public int getExperimentId()
    {
        return getExperiment().getRowId();
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.experimentId, getExperimentId());
    }

    public FlowObject getParent()
    {
        return null;
    }

    public FlowSchema getFlowSchema(User user)
    {
        FlowSchema ret = new FlowSchema(user, getContainerObject());
        ret.setExperiment(this);
        return ret;
    }

    public ActionURL urlShow()
    {
        return addParams(urlFor(RunController.ShowRunsAction.class));
    }

    public ActionURL urlDownload()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasRun(File filePath, @Nullable FlowProtocolStep step)
    {
        List<FlowRun> runs = getRuns(step);
        for (FlowRun run : runs)
        {
            if (filePath.equals(run.getExperimentRun().getFilePathRoot()))
                return true;
        }
        return false;
    }

    public List<FlowRun> findRun(File filePath, FlowProtocolStep step)
    {
        List<FlowRun> ret = new ArrayList<>();
        for (FlowRun run : getRuns(step))
        {
            if (filePath.equals(run.getExperimentRun().getFilePathRoot()))
            {
                ret.add(run);
            }
        }
        return ret;
    }

    public int getRunCount(FlowProtocolStep step)
    {
        return getRuns(step).size();
    }

    public List<FlowRun> getRuns(@Nullable FlowProtocolStep step)
    {
        ExpProtocol protocol = null;
        if (step != null)
        {
            protocol = ExperimentService.get().getExpProtocol(step.getLSID(getContainer()));
            if (protocol == null)
                return Collections.emptyList();
        }
        return FlowRun.fromRuns(getExperiment().getRuns(null, protocol));
    }

    public boolean isKeywords()
    {
        return FlowExperimentRunExperimentName.equals(getName());
    }
    
    public boolean isAnalysis()
    {
        return !isKeywords() && !isWorkspace(); 
    }

    public boolean isWorkspace()
    {
        return FlowWorkspaceExperimentName.equals(getName());
    }

    public FlowRun getMostRecentRun()
    {
        FlowRun best = null;
        Date bestDate = null;
        for (FlowRun run : getRuns(null))
        {
            Date check = run.getExperimentRun().getCreated();
            if (bestDate == null || bestDate.compareTo(check) < 0)
            {
                best = run;
                bestDate = check;
            }
        }
        return best;
    }

    static public FlowExperiment getMostRecentAnalysis(Container container)
    {
        FlowExperiment ret = null;
        Date bestDate = null;
        FlowExperiment[] analyses = getAnalyses(container);
        for (FlowExperiment experiment : analyses)
        {
            FlowRun run = experiment.getMostRecentRun();
            if (run == null)
            {
                continue;
            }
            if (bestDate == null || bestDate.compareTo(run.getExperimentRun().getCreated()) < 0)
            {
                bestDate = run.getExperimentRun().getCreated();
                ret = experiment;
            }
        }
        if (ret != null)
            return ret;
        if (analyses.length == 0)
            return null;
        return analyses[0];
    }

    static public FlowExperiment getWorkspace(Container container)
    {
        return FlowExperiment.fromLSID(getWorkspaceLSID(container));
    }

    static public FlowExperiment ensureWorkspace(User user, Container container)
    {
        FlowExperiment ret = getWorkspace(container);
        if (ret != null)
            return ret;
        ExpExperiment exp = ExperimentService.get().createExpExperiment(container, FlowWorkspaceExperimentName);
        exp.save(user);
        return new FlowExperiment(exp);
    }

    public FlowCompensationMatrix findCompensationMatrix(FlowRun run)
    {
        List<FlowRun> runs = new ArrayList<>();
        runs.addAll(findRun(new File(run.getPath()), FlowProtocolStep.analysis));
        runs.addAll(findRun(new File(run.getPath()), FlowProtocolStep.calculateCompensation));
        for (FlowRun runComp : runs)
        {
            FlowCompensationMatrix comp = runComp.getCompensationMatrix();
            if (comp != null)
                return comp;
        }
        return null;
    }
}
