package org.labkey.flow.controllers.editscript;

import org.labkey.api.view.ViewForm;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.flow.data.*;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.security.ACL;
import org.apache.struts.action.ActionMapping;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.FCSRef;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.controllers.FlowParam;

import java.util.*;
import java.sql.SQLException;

public class EditScriptForm extends ViewForm
{
    static private Logger _log = Logger.getLogger(EditScriptForm.class);
    private static int MAX_WELLS_TO_POLL = 15;

    public FlowScript analysisScript;
    public ScriptDocument analysisDocument;
    public FlowProtocolStep step;
    private int _runCount;
    private FlowCompensationMatrix _comp;
    private FlowRun _run;

    public void reset(ActionMapping mapping, HttpServletRequest request)
    {
        try
        {
            super.reset(mapping, request);
            analysisScript = FlowScript.fromScriptId(Integer.valueOf(request.getParameter("scriptId")));
            _runCount = analysisScript.getRunCount();
            step = FlowProtocolStep.fromRequest(request);
            _run = FlowRun.fromURL(getContext().getViewURLHelper(), getRequest());
            if (_run != null)
            {
                FlowPreference.editScriptRunId.setValue(request, Integer.toString(_run.getRunId()));
            }
            _comp = FlowCompensationMatrix.fromURL(getContext().getViewURLHelper(), getRequest());
            if (_comp != null)
            {
                FlowPreference.editScriptCompId.setValue(request, Integer.toString(_comp.getRowId()));
            }
            String strWellId = request.getParameter(FlowParam.wellId.toString());
            if (strWellId != null)
            {
                FlowPreference.editScriptWellId.setValue(request, strWellId);
            }
        }
        catch (Exception e)
        {
            throw new UnexpectedException(e);
        }
        try
        {
            analysisDocument = analysisScript.getAnalysisScriptDocument();
        }
        catch (Exception e)
        {

        }
    }
    public ScriptComponent getAnalysis() throws Exception
    {
        return analysisScript.getCompensationCalcOrAnalysis(step);
    }

    private void addPopulation(Map<SubsetSpec, Population> map, SubsetSpec parent, Population pop)
    {
        SubsetSpec subset = new SubsetSpec(parent, pop.getName());
        map.put(subset, pop);
        for (Population child : pop.getPopulations())
        {
            addPopulation(map, subset, child);
        }
    }

    public Map<SubsetSpec, Population> getPopulations() throws Exception
    {
        LinkedHashMap<SubsetSpec, Population> ret = new LinkedHashMap();
        PopulationSet popset = getAnalysis();
        for (Population child : popset.getPopulations())
        {
            addPopulation(ret, null, child);
        }
        return ret;
    }

    public FlowRun getRun()
    {
        if (_run != null)
            return _run;
        int runId = FlowPreference.editScriptRunId.getIntValue(getRequest());
        FlowRun run = FlowRun.fromRunId(runId);
        if (run != null && run.getContainer().equals(getContainer()))
        {
            _run = run;
            return _run;
        }
        try
        {
            FlowRun[] available = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            if (available.length != 0)
            {
                _run = available[0];
            }
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        return _run;
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        if (_comp != null)
            return _comp;
        int compId = FlowPreference.editScriptCompId.getIntValue(getRequest());
        FlowCompensationMatrix comp = FlowCompensationMatrix.fromCompId(compId);
        if (comp != null && comp.getContainer().equals(getContainer()))
        {
            _comp = comp;
            return _comp;
        }
        try
        {
            List matrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
            if (matrices.size() == 0)
                return null;
            _comp = (FlowCompensationMatrix) matrices.get(0);
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        return _comp;
    }

    public Map<String, String> getParameters()
    {
        try
        {
            String[] compChannels = analysisScript.getCompensationChannels();
            if (compChannels == null)
            {
                if (getRun() != null)
                {
                    FlowCompensationMatrix matrix = getCompensationMatrix();
                    if (matrix != null)
                    {
                        compChannels = matrix.getCompensationMatrix().getChannelNames();
                    }
                }
            }
            if (compChannels == null)
                compChannels = new String[0];
            return getParameterNames(getRun(), compChannels);
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
            return Collections.EMPTY_MAP;
        }
    }

    protected Map<String, String> getParameterNames(FlowRun run, String[] compChannels)
    {
        Map<String, String> ret = new LinkedHashMap();
        try
        {
            FlowWell[] wells = run.getWells();
            for (int i = 0; i < wells.length && i < MAX_WELLS_TO_POLL; i ++)
            {
                try
                {
                    Map<String, String> wellParams = FCSAnalyzer.get().getParameterNames(wells[i].getFCSURI(), compChannels);
                    for (Map.Entry<String, String> entry : wellParams.entrySet())
                    {
                        String previous = ret.get(entry.getKey());
                        if (previous == null)
                        {
                            ret.put(entry.getKey(), entry.getValue());
                        }
                        else if (previous.length() < entry.getValue().length())
                        {
                            ret.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                catch(Exception e)
                {
                    _log.error("Error", e);
                }
            }
        }
        catch (SQLException sqlE)
        {
            _log.error("Error", sqlE);
        }
        return ret;
    }

    public ViewURLHelper urlFor(ScriptController.Action action)
    {
        ViewURLHelper url = analysisScript.urlFor(action);
        if (step != null)
        {
            step.addParams(url);
        }

        return url;
    }
    public String[] getAvailableKeywords()
    {
        HashSet<String> keywords = new HashSet();
        try
        {
            FlowRun run = getRun();
            if (run == null)
                return new String[0];
            FlowSchema schema = new FlowSchema(getUser(), getContainer());
            schema.setRun(run);
            FlowPropertySet fps = new FlowPropertySet(schema.createFCSFileTable("foo"));
            keywords.addAll(fps.getKeywordProperties().keySet());
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        String[] ret = keywords.toArray(new String[0]);
        Arrays.sort(ret);
        return ret;
    }

    public boolean canEdit()
    {
        return getContainer().hasPermission(getUser(), ACL.PERM_UPDATE) && _runCount == 0;
    }

    public Map<Integer, String> getExperimentRuns() throws Exception
    {
        LinkedHashMap<Integer, String> ret = new LinkedHashMap();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            ret.put(run.getRunId(), run.getName());
        }
        return ret;
    }
}
