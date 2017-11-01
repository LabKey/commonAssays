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

package org.labkey.flow.controllers.executescript;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.Filter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.action.SpringActionController;
import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowTableType;
import org.springframework.validation.BindException;

import java.util.*;

public class ChooseRunsToAnalyzeForm extends FlowQueryForm implements DataRegionSelection.DataSelectionKeyForm
{
    static private final Logger _log = Logger.getLogger(ChooseRunsToAnalyzeForm.class);

    static private final String COMPOPTION_EXPERIMENTLSID = "experimentlsid:";
    static private final String COMPOPTION_COMPID = "compid:";
    static private final String COMPOPTION_SPILL = "spill";
    public String ff_compensationMatrixOption;
    public String ff_analysisName;
    public String ff_targetExperimentId;
    private FlowScript _analysisScript;
    private FlowProtocolStep _step;
    private boolean _targetExperimentSet;
    private String _dataRegionSelectionKey;

    public ChooseRunsToAnalyzeForm()
    {
        super(FlowTableType.Runs.toString());
    }
    

    public void setFf_analysisName(String name)
    {
        ff_analysisName = name;
    }

    public void setScriptId(int id)
    {
        _analysisScript = FlowScript.fromScriptId(id);
    }

    public void setFf_compensationMatrixOption(String str)
    {
        ff_compensationMatrixOption = str;
    }

    public void setActionSequence(int id)
    {
        _step = FlowProtocolStep.fromActionSequence(id);
    }

    public FlowProtocolStep getProtocolStep()
    {
        return _step;
    }

    public void setProtocolStep(FlowProtocolStep step)
    {
        _step = step;
    }

    public Map<Integer, String> getAvailableSteps(FlowScript analysisScript)
    {
        Map<Integer, String> ret = new LinkedHashMap();
        if (analysisScript.hasStep(FlowProtocolStep.calculateCompensation))
        {
            FlowProtocolStep.calculateCompensation.ensureForContainer(getUser(), getContainer());
            ret.put(FlowProtocolStep.calculateCompensation.getDefaultActionSequence(), FlowProtocolStep.calculateCompensation.getName());
        }
        if (analysisScript.hasStep(FlowProtocolStep.analysis))
        {
            FlowProtocolStep.analysis.ensureForContainer(getUser(), getContainer());
            ret.put(FlowProtocolStep.analysis.getDefaultActionSequence(), FlowProtocolStep.analysis.getName());
        }
        return ret;
    }

    public FlowExperiment getTargetExperiment()
    {
        if (ff_targetExperimentId == null)
            return null;
        try
        {
            return FlowExperiment.fromExperimentId(Integer.valueOf(ff_targetExperimentId));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    public void setFf_targetExperimentId(String experimentId)
    {
        ff_targetExperimentId = experimentId;
        _targetExperimentSet = true;
    }

    public FlowScript getProtocol()
    {
        return _analysisScript;
    }

    public void setProtocol(FlowScript analysisScript)
    {
        _analysisScript = analysisScript;
    }

    public Collection<String> getAvailableQueries()
    {
        return Collections.singleton("runs");
    }

    public Collection<FlowScript> getAvailableGateDefinitionSets()
    {
        try
        {
            Collection<FlowScript> ret = new ArrayList();
            FlowScript[] protocols = FlowScript.getScripts(getContainer());
            for (FlowScript analysisScript : protocols)
            {
                if (getAvailableSteps(analysisScript).size() > 0)
                {
                    ret.add(analysisScript);
                }
            }
            return ret;
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public Collection<FlowExperiment> getAvailableAnalyses()
    {
        return Arrays.asList(FlowExperiment.getAnalyses(getContainer()));
    }

    public void populate(BindException errors)
    {
        if (!_targetExperimentSet)
        {
            FlowExperiment analysis = FlowExperiment.getMostRecentAnalysis(getContainer());
            if (analysis != null)
            {
                ff_targetExperimentId = Integer.toString(analysis.getExperimentId());
            }
        }
        Collection<FlowScript> availableProtocols = Arrays.asList(FlowScript.getScripts(getContainer()));
        if (availableProtocols.size() == 0)
        {
            errors.reject(SpringActionController.ERROR_MSG, "There are no analysis or compensation protocols in this folder.");
        }
        else
        {
            FlowScript analysisScript = getProtocol();
            if (analysisScript == null)
            {
                analysisScript = availableProtocols.iterator().next();
                setProtocol(analysisScript);
            }
            FlowProtocolStep step = getProtocolStep();
            if (step == null || !analysisScript.hasStep(step))
            {
                Integer[] steps = getAvailableSteps(analysisScript).keySet().toArray(new Integer[0]);
                step = FlowProtocolStep.fromActionSequence(steps[steps.length - 1]);
                setProtocolStep(step);
            }
        }
    }

    public int[] getSelectedRunIds()
    {
        Set<String> values = DataRegionSelection.getSelected(getViewContext(), false);
        return PageFlowUtil.toInts(values);
    }

    public String getAnalysisLSID()
    {
        FlowExperiment experiment = getTargetExperiment();
        if (experiment != null)
            return experiment.getLSID();
        if (!StringUtils.isEmpty(ff_analysisName))
            return FlowObject.generateLSID(getContainer(), "Experiment", ff_analysisName);
        return null;
    }

    public SimpleFilter getBaseFilter(TableInfo table, Filter filter)
    {
        try
        {
            SimpleFilter ret = new SimpleFilter(filter);

            List<FlowRun> runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            int runCount = 0;
            StringBuilder sql = new StringBuilder("RowId IN (");
            String comma = "";
            for (FlowRun run : runs)
            {
                if (run.getPath() == null)
                    continue;
                sql.append(comma);
                comma = ",";
                sql.append(run.getRunId());
                runCount ++;
            }
            sql.append(")");
            if (runCount == 0)
            {
                ret.addWhereClause("1 = 0", null);
            }
            else
            {
                ret.addWhereClause(sql.toString(), new Object[0], FieldKey.fromParts("RowId"));
            }
            return ret;
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public int getCompensationMatrixId()
    {
        if (ff_compensationMatrixOption == null)
            return 0;
        if (!ff_compensationMatrixOption.startsWith(COMPOPTION_COMPID))
            return 0;
        try
        {
            return Integer.valueOf(ff_compensationMatrixOption.substring(COMPOPTION_COMPID.length()));
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
    }

    public String getCompensationExperimentLSID()
    {
        if (ff_compensationMatrixOption == null)
            return null;
        if (!ff_compensationMatrixOption.startsWith(COMPOPTION_EXPERIMENTLSID))
            return null;
        return ff_compensationMatrixOption.substring(COMPOPTION_EXPERIMENTLSID.length());
    }

    public boolean useSpillCompensationMatrix()
    {
        if (ff_compensationMatrixOption == null)
            return false;
        return ff_compensationMatrixOption.equals(COMPOPTION_SPILL);
    }

    public Map<String, String> getCompensationMatrixOptions()
    {
        Map<String, String> ret = new LinkedHashMap();
        FlowScript analysisScript = getProtocol();
        FlowExperiment targetExperiment = getTargetExperiment();
        if (analysisScript.hasStep(FlowProtocolStep.calculateCompensation))
        {
            ret.put("", "Calculate new if necessary");
        }
        else if (targetExperiment == null)
        {
            ret.put("", "No compensation calculation defined; choose one of the other options");
        }
        
        FlowExperiment[] experiments = FlowExperiment.getExperiments(getContainer());
        for (FlowExperiment compExp : experiments)
        {
            int count = compExp.getRunCount(FlowProtocolStep.calculateCompensation) + compExp.getRunCount(FlowProtocolStep.analysis);
            if (count == 0)
                continue;
            ret.put(COMPOPTION_EXPERIMENTLSID + compExp.getLSID(), "Use from analysis '" + compExp.getName() + "'");
        }

        List<FlowCompensationMatrix> comps = FlowCompensationMatrix.getCompensationMatrices(getContainer());
        boolean sameExperiment = FlowDataObject.sameExperiment(comps);
        Collections.sort(comps);
        for (FlowCompensationMatrix comp : comps)
        {
            String label = "Matrix: " + comp.getLabel(!sameExperiment);
            ret.put(COMPOPTION_COMPID +  comp.getCompId(), label);
        }

        // We can't know if the machine acquired spill matrix is available until
        // we check each available run in ChooseRunsRegion.getDisabledReason().
        ret.put(COMPOPTION_SPILL, "Use machine acquired spill matrix");

        return ret;
    }

    public String getDataRegionSelectionKey()
    {
        return _dataRegionSelectionKey;
    }

    public void setDataRegionSelectionKey(String key)
    {
        _dataRegionSelectionKey = key;
    }
}
