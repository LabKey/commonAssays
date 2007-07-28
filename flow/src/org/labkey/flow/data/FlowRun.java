package org.labkey.flow.data;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.data.*;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QueryService;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;
import java.net.URI;
import java.io.File;

import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.model.SampleCriteria;
import org.labkey.flow.analysis.web.FCSRef;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.persist.FlowManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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
        ExpData[] outputs = getExperimentRun().getOutputDatas(FlowDataType.CompensationMatrix);
        if (outputs.length > 0)
        {
            return new FlowCompensationMatrix(outputs[0]);
        }
        PropertyDescriptor inputRole = InputRole.CompensationMatrix.getPropertyDescriptor(getContainer());
        if (inputRole == null)
            return null;
        ExpData[] datas = getExperimentRun().getInputDatas(inputRole, ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.length == 0)
            return null;
        return new FlowCompensationMatrix(datas[0]);
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
        return fromURL(url, null);
    }
    static public FlowRun fromURL(ViewURLHelper url, HttpServletRequest request) throws ServletException
    {
        FlowRun ret = fromRunId(getIntParam(url, request, FlowParam.runId));
        if (ret != null)
        {
            ret.checkContainer(url);
        }
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
        PropertyDescriptor pd = InputRole.AnalysisScript.getPropertyDescriptor(getContainer());
        if (pd == null)
            return null;
        ExpData[] datas = getExperimentRun().getInputDatas(pd, ExpProtocol.ApplicationType.ExperimentRun);
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

    static public FlowRun[] getRunsForContainer(Container container, FlowProtocolStep step) throws SQLException
    {
        List<FlowRun> ret = new ArrayList();
        ExpProtocol childProtocol = null;
        if (step != null)
        {
            FlowProtocol childFlowProtocol = step.getForContainer(container);
            if (childFlowProtocol == null)
            {
                return new FlowRun[0];
            }
            childProtocol = childFlowProtocol.getProtocol();
        }
        for (ExpRun run : ExperimentService.get().getExpRuns(container, null, childProtocol))
        {
            ret.add(new FlowRun(run));
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

    public FlowWell[] getWellsToBeAnalyzed(FlowProtocol protocol) throws SQLException
    {
        if (protocol == null)
            return getWells();
        FlowSchema schema = new FlowSchema(null, getContainer());
        schema.setRun(this);
        TableInfo table = schema.createFCSFileTable("FCSFiles");
        ColumnInfo colRowId = table.getColumn("RowId");
        List<FlowWell> ret = new ArrayList();
        ResultSet rs = QueryService.get().select(table, new ColumnInfo[] { colRowId }, protocol.getFCSAnalysisFilter(), null);
        while (rs.next())
        {
            FlowWell well = FlowWell.fromWellId(colRowId.getIntValue(rs));
            if (well != null)
            {
                ret.add(well);
            }
        }
        rs.close();
        return ret.toArray(new FlowWell[0]);
    }

    private void addMatchingWell(Map<Integer, String> map, String label, SampleCriteria criteria, FlowWell[] wells, FCSKeywordData[] datas)
    {
        for (int i = 0; i < datas.length; i ++)
        {
            FCSKeywordData data = datas[i];
            if (data == null)
                continue;

            if (criteria.matches(data))
            {
                FlowWell well = wells[i];
                if (map.containsKey(well.getWellId()))
                    return;
                label += " (" + well.getName() + ")";
                map.put(well.getRowId(), label);
                return;
            }
        }
    }

    public Map<Integer, String> getWells(FlowProtocol protocol, PopulationSet popset, FlowProtocolStep step) throws Exception
    {
        FlowWell[] wells = getWells();
        Map<Integer, String> ret = new LinkedHashMap();
        if (step == FlowProtocolStep.calculateCompensation)
        {
            CompensationCalculation calc = (CompensationCalculation) popset;
            FCSKeywordData[] keywordData = new FCSKeywordData[wells.length];
            for (int i = 0; i < keywordData.length; i ++)
            {
                keywordData[i] = FCSAnalyzer.get().readAllKeywords(FlowAnalyzer.getFCSRef(wells[i]));
            }
            for (int i = 0; i < calc.getChannelCount(); i ++)
            {
                CompensationCalculation.ChannelInfo info = calc.getChannelInfo(i);
                addMatchingWell(ret, info.getName() + "+", info.getPositive().getCriteria(), wells, keywordData);
                addMatchingWell(ret, info.getName() + "-", info.getNegative().getCriteria(), wells, keywordData);
            }
            for (FlowWell well : wells)
            {
                if (ret.containsKey(well.getRowId()))
                    continue;
                ret.put(well.getRowId(), well.getName());
            }
        }
        else
        {
            if (protocol != null)
            {
                FlowWell[] wellsToBeAnalyzed = getWellsToBeAnalyzed(protocol);
                for (FlowWell well : wellsToBeAnalyzed)
                {
                    ret.put(well.getRowId(), protocol.getFCSAnalysisName(well));
                }
            }
            for (FlowWell well : wells)
            {
                if (ret.containsKey(well.getRowId()))
                    continue;
                ret.put(well.getRowId(), well.getName());
            }
        }
        return ret;
    }

    public void moveToWorkspace(User user) throws Exception
    {
        boolean transaction = false;
        try
        {
            if (!ExperimentService.get().isTransactionActive())
            {
                ExperimentService.get().beginTransaction();
                transaction = true;
            }
            ExpRun run = getExperimentRun();
            ExpExperiment[] experiments = run.getExperiments();
            for (ExpExperiment experiment : experiments)
            {
                experiment.removeRun(user, getExperimentRun());
            }
            FlowExperiment workspace = FlowExperiment.ensureWorkspace(user, getContainer());
            workspace.getExperiment().addRun(user, run);
            if (transaction)
            {
                ExperimentService.get().commitTransaction();
                transaction = false;
            }
        }
        finally
        {
            if (transaction)
            {
                ExperimentService.get().rollbackTransaction();
            }
        }
    }

    public boolean isInWorkspace()
    {
        ExpExperiment[] experiments = getExperimentRun().getExperiments();
        if (experiments.length != 1)
            return false;
        FlowExperiment flowExperiment = new FlowExperiment(experiments[0]);
        return flowExperiment.isWorkspace();
    }

    public FlowTableType getDefaultQuery()
    {
        try
        {
            FlowWell[] wells = getWells();
            for (FlowWell well : wells)
            {
                if (well.getDataType() == FlowDataType.FCSAnalysis)
                {
                    return FlowTableType.FCSAnalyses;
                }
                if (well.getDataType() == FlowDataType.CompensationControl)
                {
                    return FlowTableType.CompensationControls;
                }
            }
        }
        catch (SQLException e)
        {
            _log.error("Error", e);
        }
        return FlowTableType.FCSFiles;
    }
}
