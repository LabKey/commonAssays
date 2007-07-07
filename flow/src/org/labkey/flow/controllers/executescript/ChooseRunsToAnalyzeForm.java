package org.labkey.flow.controllers.executescript;

import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.data.*;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

import org.labkey.flow.analysis.model.PopulationSet;

import javax.servlet.http.HttpServletRequest;

public class ChooseRunsToAnalyzeForm extends FlowQueryForm
{
    static private final Logger _log = Logger.getLogger(ChooseRunsToAnalyzeForm.class);

    static private final String COMPOPTION_EXPERIMENTLSID = "experimentlsid:";
    static private final String COMPOPTION_COMPID = "compid:";
    public String ff_compensationMatrixOption;
    public String ff_analysisName;
    public String ff_targetExperimentId;
    private FlowScript _analysisScript;
    private FlowProtocolStep _step;
    private boolean _targetExperimentSet;

    protected FlowSchema createSchema()
    {
        return new FlowSchema(getUser(), getContainer());
    }

    protected QuerySettings createQuerySettings(UserSchema schema)
    {
        QuerySettings ret = super.createQuerySettings(schema);
        ret.setQueryName(FlowTableType.Runs.toString());
        return ret;
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

    public Map<Integer, String> getAvailableSteps(FlowScript analysisScript) throws Exception
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
        return FlowExperiment.fromExperimentId(Integer.valueOf(ff_targetExperimentId));
    }

    public void setFf_targetExperimentId(String experimentId)
    {
        ff_targetExperimentId = experimentId;
        _targetExperimentSet = true;
    }

    public void addActionError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add(null, new ActionError("error", error));
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

    public void populate() throws Exception
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
            addActionError("There are no analysis or compensation protocols in this folder.");
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
        String[] values = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        if (values == null)
            return new int[0];
        int[] ret = new int[values.length];
        for (int i = 0; i < values.length; i ++)
        {
            ret[i] = Integer.valueOf(values[i]);
        }
        return ret;
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

            FlowRun[] runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            if (runs.length == 0)
            {
                ret.addWhereClause("1 = 0", null);
            }
            else
            {
                StringBuilder sql = new StringBuilder("RowId IN (");
                String comma = "";
                for (FlowRun run : runs)
                {
                    if (run.getPath() == null)
                        continue;
                    sql.append(comma);
                    comma = ",";
                    sql.append(run.getRunId());
                }
                sql.append(")");
                ret.addWhereClause(sql.toString(), new Object[0], "RowId");
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
        return Integer.valueOf(ff_compensationMatrixOption.substring(COMPOPTION_COMPID.length()));
    }

    public String getCompensationExperimentLSID()
    {
        if (ff_compensationMatrixOption == null || ff_compensationMatrixOption.startsWith(COMPOPTION_COMPID))
            return null;
        return ff_compensationMatrixOption.substring(COMPOPTION_EXPERIMENTLSID.length());
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
        return ret;
    }
}
