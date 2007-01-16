package org.labkey.flow.data;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.data.*;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewURLHelper;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.net.URI;

import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.FlowParam;

import javax.servlet.ServletException;

public class FlowRun extends FlowObject<ExpRun>
{
    private static final Logger _log = Logger.getLogger(FlowRun.class);
    Integer ACTIONSEQUENCE_LOG = 0;
    static public FlowRun[] fromRuns(ExpRun[] runs)
    {
        FlowRun[] ret = new FlowRun[runs.length];
        for (int i = 0; i < runs.length; i ++)
        {
            ret[i] = new FlowRun(runs[i]);
        }
        return ret;
    }

    public FlowRun(ExpRun run)
    {
        super(run);
    }

    public ExpRun getExperimentRun()
    {
        return getExpObject();
    }

    public List<? extends FlowDataObject> getDatas(FlowDataType type) throws SQLException
    {
        return FlowDataObject.fromDatas(getExperimentRun().getOutputDatas(type));
    }

    public List<? extends FlowObject> getInputDatas(FlowDataType type) throws SQLException
    {
        List<FlowObject> ret = new ArrayList();
        FlowDataObject.addDataOfType(getExpObject().getInputDatas(type), type, ret);
        return ret;
    }

    public FlowWell[] getWells() throws SQLException
    {
        List<? extends FlowDataObject> all = getDatas(null);
        List<FlowWell> wells = new ArrayList();
        for (FlowDataObject obj : all)
        {
            if (obj instanceof FlowWell)
            {
                wells.add((FlowWell) obj);
            }
        }
        FlowWell[] ret = wells.toArray(new FlowWell[0]);
        Arrays.sort(ret);
        return ret;
    }

    public FlowLog[] getLogs() throws SQLException
    {
        return getDatas(FlowDataType.Log).toArray(new FlowLog[0]);
    }

    public FlowWell findWell(URI uri) throws Exception
    {
        FlowWell[] wells = getWells();
        for (FlowWell well : wells)
        {
            if (uri.equals(well.getFCSURI()))
                return well;
        }
        return null;
    }

    public FlowCompensationMatrix getCompensationMatrix() throws SQLException
    {
        List datas = getDatas(FlowDataType.CompensationMatrix);
        if (datas.size() == 0)
        {
            datas = getInputDatas(FlowDataType.CompensationMatrix);
        }
        if (datas.size() == 0)
        {
            return null;
        }
        return (FlowCompensationMatrix) datas.get(0);
    }
    
    public int getRunId()
    {
        return getExperimentRun().getRowId();
    }

    public FlowLog getLog(LogType type) throws SQLException
    {
        List<FlowLog> logs = (List<FlowLog>) getDatas(FlowDataType.Log);
        for (FlowLog log : logs)
        {
            if (type.toString().equals(log.getName()))
            {
                return log;
            }
        }
        return createLog(null, type);
    }

    private FlowLog createLog(User user, LogType type) throws SQLException
    {
        /*ExperimentManager mgr = ExperimentManager.get();
        ProtocolApplication[] apps = mgr.getProtocolApplicationForRun(getRunId());
        for (ProtocolApplication app : apps)
        {
            if (app.getActionSequence() == ACTIONSEQUENCE_LOG)
            {
                return (FlowLog) FlowDataObject.createData(user, getContainerId(), app, FlowDataType.Log, type.toString());
            }
        }*/
        return null;
    }

    static public FlowRun fromRunId(int id)
    {
        if (id == 0)
            return null;
        return fromRun(ExperimentService.get().getExpRun(id));
    }

    static public FlowRun fromLSID(String lsid)
    {
        return fromRun(ExperimentService.get().getExpRun(lsid));
    }

    static public FlowRun fromRun(ExpRun run)
    {
        if (run == null)
            return null;
        return new FlowRun(run);
    }

    static public FlowRun fromURL(ViewURLHelper url) throws ServletException
    {
        String strRunId = url.getParameter("runId");
        if (strRunId == null)
            return null;
        FlowRun ret = fromRunId(Integer.valueOf(strRunId));
        if (ret == null)
            return null;

        ret.checkContainer(url);
        return ret;
    }

    public String getAnalysisScript() throws SQLException
    {
        FlowScript script = getScript();
        if (script == null)
            return null;
        return getScript().getAnalysisScript();
    }

    public FlowScript getScript()
    {
        ExpData[] datas = getExperimentRun().getInputDatas(FlowDataType.Script);
        if (datas.length == 0)
            return null;
        return (FlowScript) FlowDataObject.fromData(datas[0]);
    }

    public String getPath()
    {
        return getExperimentRun().getFilePathRoot();
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.runId, getRunId());
    }

    public ViewURLHelper urlShow()
    {
        return addParams(pfURL(RunController.Action.showRun));
    }

    public String getLabel()
    {
        return getName();
    }

    public FlowObject getParent()
    {
        return getExperiment();
    }

    public FlowExperiment getExperiment()
    {
        ExpExperiment[] experiments = getExperimentRun().getExperiments();
        if (experiments.length>0)
            return new FlowExperiment(experiments[0]);
        else
            return null;
    }

    static public void sortRuns(List<FlowRun> runs)
    {
        Collections.sort(runs, new Comparator<FlowRun>() {
            public int compare(FlowRun o1, FlowRun o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    static public FlowRun[] getRunsForContainer(Container container) throws SQLException
    {
        Set<String> urls = new HashSet();
        List<FlowRun> ret = new ArrayList();
        FlowExperiment experiment = FlowExperiment.getExperimentRunExperiment(container);
        if (experiment == null)
            return new FlowRun[0];
        for (FlowRun run : experiment.getRuns(FlowProtocolStep.keywords))
        {
            ret.add(run);
        }
        sortRuns(ret);
        return ret.toArray(new FlowRun[0]);
    }

    public FlowProtocolStep getStep()
    {
        try
        {
            for (ProtocolApplication app : ExperimentService.get().getProtocolApplicationForRun(getRunId()))
            {
                FlowProtocolStep step = FlowProtocolStep.fromActionSequence(app.getActionSequence());
                if (step != null)
                    return step;
            }
        }
        catch (Exception e)
        {
            _log.error("error", e);
        }
        return null;
    }
}
