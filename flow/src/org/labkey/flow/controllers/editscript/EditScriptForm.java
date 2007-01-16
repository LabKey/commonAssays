package org.labkey.flow.controllers.editscript;

import org.labkey.api.view.ViewForm;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.flow.data.*;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowTableType;
import org.labkey.api.util.UnexpectedException;
import org.apache.struts.action.ActionMapping;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.FCSAnalyzer;

import java.util.*;

public class EditScriptForm extends ViewForm
{
    static private Logger _log = Logger.getLogger(EditScriptForm.class.toString());
    private static int MAX_WELLS_TO_POLL = 15;

    public FlowScript analysisScript;
    public FlowRun run;
    public FlowWell well;
    public FlowCompensationMatrix comp;
    public ScriptDocument analysisDocument;
    public FlowProtocolStep step;

    public void reset(ActionMapping mapping, HttpServletRequest request)
    {
        try
        {
            super.reset(mapping, request);
            analysisScript = FlowScript.fromScriptId(Integer.valueOf(request.getParameter("scriptId")));
            step = FlowProtocolStep.fromRequest(request);
            String strRunId = request.getParameter("runId");
            if (strRunId != null)
            {
                run = FlowRun.fromRunId(Integer.valueOf(strRunId));
            }
            String strWellId = request.getParameter("wellId");
            if (strWellId != null)
            {
                well = FlowWell.fromWellId(Integer.valueOf(strWellId));
            }
            String strCompId = request.getParameter("compId");
            if (strCompId != null)
            {
                comp = FlowCompensationMatrix.fromCompId(Integer.valueOf(strCompId));
            }
            if (run == null && well != null)
            {
                run = well.getRun();
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
    public PopulationSet getAnalysis() throws Exception
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
        if (run != null)
            return run;
        try
        {
            FlowRun[] available = FlowRun.getRunsForContainer(getContainer());
            if (available.length == 0)
                return null;
            run = available[0];
            return run;
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
            return run;
        }
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        if (comp != null)
            return comp;
        try
        {
            List matrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
            if (matrices.size() == 0)
                return null;
            comp = (FlowCompensationMatrix) matrices.get(0);
            return comp;
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
            return comp;
        }
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
                    FlowCompensationMatrix matrix = getRun().getCompensationMatrix();
                    if (matrix == null)
                    {
                        List<FlowCompensationMatrix> matrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
                        if (matrices.size() > 0)
                            matrix = matrices.get(0);
                    }
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

    private Map<String, String> getParameterNames(FlowRun run, String[] compChannels) throws Exception
    {
        Map<String, String> ret = new LinkedHashMap();
        FlowWell[] wells = run.getWells();
        for (int i = 0; i < wells.length && i < MAX_WELLS_TO_POLL; i ++)
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
        return ret;
    }

    public ViewURLHelper urlFor(ScriptController.Action action)
    {
        ViewURLHelper url = analysisScript.urlFor(action);
        if (run != null)
        {
            run.addParams(url);
        }
        if (well != null)
        {
            well.addParams(url);
        }
        if (comp != null)
        {
            comp.addParams(url);
        }
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

}
