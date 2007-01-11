package Flow.ExecuteScript;

import org.fhcrc.cpas.flow.data.*;
import org.fhcrc.cpas.flow.query.FlowQueryForm;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.query.FlowTableType;
import org.fhcrc.cpas.util.UnexpectedException;
import org.fhcrc.cpas.util.PageFlowUtil;
import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.query.api.QuerySettings;
import org.apache.struts.action.ActionError;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

import com.labkey.flow.model.PopulationSet;

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

    protected FlowSchema createSchema()
    {
        return new FlowSchema(getUser(), getContainer());
    }

    protected QuerySettings createQuerySettings()
    {
        QuerySettings ret = super.createQuerySettings();
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

    public List<FlowProtocol> getAvailableSteps(FlowScript analysisScript) throws Exception
    {
        ArrayList<FlowProtocol> ret = new ArrayList(2);
        if (analysisScript.hasStep(FlowProtocolStep.calculateCompensation))
        {
            ret.add(FlowProtocolStep.calculateCompensation.ensureForContainer(getUser(), getContainer()));
        }
        if (analysisScript.hasStep(FlowProtocolStep.analysis))
        {
            ret.add(FlowProtocolStep.analysis.ensureForContainer(getUser(), getContainer()));
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
        Collection<FlowScript> availableProtocols = Arrays.asList(FlowScript.getScripts(getContainer()));
        FlowExperiment experiment = FlowExperiment.getExperimentRunExperiment(getContainer());
        if (experiment == null)
        {
            addActionError("There are no experiments in this folder.");
        }
        if (availableProtocols.size() == 0)
        {
            addActionError("There are no analysis or compensation protocols in this folder.");
        }
        else
        {
            FlowScript analysisScript = getProtocol();
            if (analysisScript == null)
            {
                setProtocol(availableProtocols.iterator().next());
            }
            FlowProtocolStep step = getProtocolStep();
            if (step == null || !analysisScript.hasStep(step))
            {
                List<FlowProtocol> steps = getAvailableSteps(analysisScript);
                step = steps.get(steps.size() - 1).getStep();
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

    private void addInClause(SimpleFilter filter, ColumnInfo column, boolean in, Collection< ? extends Object> objects)
    {
        if (objects.size() == 0)
        {
            if (in)
            {
                filter.addWhereClause("(1 = 0)", null);
            }
            return;
        }

        SQLFragment ret = new SQLFragment("(");
        ret.append(column.getAlias());
        if (!in)
            ret.append(" NOT ");
        ret.append(" IN ( ? ");
        ret.append(StringUtils.repeat(",?", objects.size() - 1));
        ret.append("))");
        ret.addAll(objects);
        filter.addWhereClause(ret.getSQL(), ret.getParams().toArray(), column.getName());
    }

    private void addUnanalyzedRunFilter(TableInfo table, SimpleFilter filter) throws Exception
    {
        FlowExperiment experiment = getTargetExperiment();
        if (experiment == null)
            return;
        String[] paths = experiment.getAnalyzedRunPaths(getUser(), getProtocolStep());
        addInClause(filter, table.getColumn("FilePathRoot"), false, Arrays.asList(paths));
    }

    /**
     * If this analysis requires a compensation matrix, and does not define a compensation calculation,
     * and the user has not yet chosen a compensation matrix, then only allow them to analyze runs for which
     * a compensation matrix has already been calculated.
     */
    private void addHasCompensationMatrixFilter(TableInfo table, SimpleFilter filter) throws Exception
    {
        FlowScript analysisScript = getProtocol();
        PopulationSet populationSet = analysisScript.getCompensationCalcOrAnalysis(getProtocolStep());
        if (!populationSet.requiresCompensationMatrix())
            return;
        if (getCompensationMatrixId() != 0)
            return;
        Set<String> paths = new HashSet();
        FlowExperiment experiment = FlowExperiment.fromLSID(getCompensationExperimentLSID());
        if (experiment != null)
        {
            paths.addAll(Arrays.asList(experiment.getAnalyzedRunPaths(getUser(), FlowProtocolStep.calculateCompensation)));
            paths.addAll(Arrays.asList(experiment.getAnalyzedRunPaths(getUser(), FlowProtocolStep.analysis)));
        }
        else if (analysisScript.hasStep(FlowProtocolStep.calculateCompensation))
        {
            return;
        }

        addInClause(filter, table.getColumn("FilePathRoot"), true, paths);
    }

    public SimpleFilter getBaseFilter(TableInfo table, Filter filter)
    {
        try
        {
            SimpleFilter ret = new SimpleFilter(filter);
            ret.addCondition("ProtocolStep", FlowProtocolStep.keywords.getName());
            addUnanalyzedRunFilter(table, ret);
            addHasCompensationMatrixFilter(table, ret);
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
