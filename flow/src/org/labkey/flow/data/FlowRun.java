/*
 * Copyright (c) 2005-2010 LabKey Corporation
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
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.script.FlowAnalyzer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.io.File;

public class FlowRun extends FlowObject<ExpRun> implements AttachmentParent
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


    public String getEntityId()
    {
        return getExperimentRun().getEntityId();
    }

    public ExpRun getExperimentRun()
    {
        return getExpObject();
    }

    public List<? extends FlowDataObject> getDatas(FlowDataType type) throws SQLException
    {
        return FlowDataObject.fromDatas(getExperimentRun().getOutputDatas(type));
    }

    public boolean hasRealWells() throws SQLException
    {
        List<? extends FlowDataObject> all = getDatas(null);
        for (FlowDataObject obj : all)
        {
            if (obj instanceof FlowWell)
            {
                FlowWell well = (FlowWell)obj;
                if (well.getFCSURI() != null)
                    return true;
            }
        }
        return false;
    }

    public FlowWell[] getWells()
    {
        return getWells(false);
    }

    List<? extends FlowDataObject> _allDatas = null;

    public FlowWell[] getWells(boolean realFiles)
    {
        if (null == _allDatas)
        {
            try
            {
                _allDatas = getDatas(null);
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
        }
        
        List<FlowWell> wells = new ArrayList<FlowWell>();
        for (FlowDataObject obj : _allDatas)
        {
            if (obj instanceof FlowWell)
            {
                FlowWell well = (FlowWell)obj;
                if (realFiles)
                {
                    URI uri = well.getFCSURI();
                    // XXX: hit the file system every time?
                    if (uri != null && new File(uri.getPath()).canRead())
                        wells.add((FlowWell) obj);
                }
                else
                {
                    wells.add((FlowWell) obj);
                }
            }
        }
        FlowWell[] ret = wells.toArray(new FlowWell[wells.size()]);
        Arrays.sort(ret);
        return ret;
    }


    public FlowWell getFirstWell()
    {
        if (_allDatas != null)
        {
            for (FlowDataObject obj : _allDatas)
                if (obj instanceof FlowWell)
                    return (FlowWell)obj;
        }

        ExpData[] datas = getExperimentRun().getOutputDatas(null);
        for (ExpData data : datas)
        {
            FlowDataObject obj = FlowDataObject.fromData(data);
            if (obj instanceof FlowWell)
                return (FlowWell)obj;
        }
        return null;
    }


    public FlowFCSFile[] getFCSFiles() throws SQLException
    {
        return getDatas(FlowDataType.FCSFile).toArray(new FlowFCSFile[0]);
    }

    public FlowLog[] getLogs() throws SQLException
    {
        return getDatas(FlowDataType.Log).toArray(new FlowLog[0]);
    }

    public FlowFCSFile findFCSFile(URI uri) throws Exception
    {
        FlowFCSFile[] wells = getFCSFiles();
        for (FlowFCSFile well : wells)
        {
            if (uri.equals(well.getFCSURI()))
                return well;
        }
        return null;
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        ExpData[] outputs = getExperimentRun().getOutputDatas(FlowDataType.CompensationMatrix);
        if (outputs.length > 0)
        {
            return new FlowCompensationMatrix(outputs[0]);
        }
        ExpData[] datas = getExperimentRun().getInputDatas(InputRole.CompensationMatrix.toString(), ExpProtocol.ApplicationType.ExperimentRun);
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

    static public FlowRun fromURL(ActionURL url) throws ServletException
    {
        return fromURL(url, null);
    }

    static public FlowRun fromURL(ActionURL url, HttpServletRequest request) throws ServletException
    {
        int runid = getIntParam(url, request, FlowParam.runId);
        if (0 == runid)
            return null;
        FlowRun ret = fromRunId(runid);
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

    private int getScriptId()
    {
        ExpData[] datas = getExperimentRun().getInputDatas(InputRole.AnalysisScript.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.length == 0)
            return 0;
        return datas[0].getRowId();
    }

    public FlowScript getScript()
    {
        ExpData[] datas = getExperimentRun().getInputDatas(InputRole.AnalysisScript.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.length == 0)
            return null;
        return (FlowScript) FlowDataObject.fromData(datas[0]);
    }

    public String getPath()
    {
        File file = getExperimentRun().getFilePathRoot();
        return file == null ? null : file.getPath();
    }

    public ActionURL getDownloadWorkspaceURL()
    {
        ExpData[] datas = getExperimentRun().getInputDatas(InputRole.Workspace.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.length == 0 || !datas[0].isFileOnDisk())
            return null;
        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(getContainer(), datas[0], false);
        return url;
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.runId, getRunId());
    }

    public ActionURL urlShow()
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
        return getRunsForPath(container, step, null);
    }

    static public FlowRun[] getRunsWithRealFCSFiles(Container container, FlowProtocolStep step) throws SQLException
    {
        FlowRun[] runs = FlowRun.getRunsForContainer(container, step);
        List<FlowRun> ret = new ArrayList<FlowRun>();
        for (FlowRun run : runs)
        {
            if (run.hasRealWells())
                ret.add(run);
        }
        return ret.toArray(new FlowRun[ret.size()]);
    }

    static public FlowRun[] getRunsForScript(Container container, FlowProtocolStep step, int scriptId) throws SQLException
    {
        if (scriptId == 0)
            return new FlowRun[0];

        List<FlowRun> ret = new ArrayList<FlowRun>();
        for (FlowRun run : getRunsForContainer(container, step))
        {
            if (scriptId == run.getScriptId())
                ret.add(run);
        }
        return ret.toArray(new FlowRun[ret.size()]);
    }

    static public FlowRun findMostRecent(Container container, FlowProtocolStep step) throws SQLException
    {
        FlowRun[] runs = getRunsForContainer(container, step);
        FlowRun max = null;
        for (FlowRun run : runs)
        {
            if (max == null)
                max = run;
            else
                max = max.getExperimentRun().getModified().before(run.getExperimentRun().getModified()) ? run : max;
        }
        return max;
    }

    static public FlowRun[] getRunsForPath(Container container, FlowProtocolStep step, File runFilePathRoot) throws SQLException
    {
        List<FlowRun> ret = new ArrayList<FlowRun>();
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
            if (runFilePathRoot == null || (run.getFilePathRoot() != null && runFilePathRoot.equals(run.getFilePathRoot())))
                ret.add(new FlowRun(run));
        }
        sortRuns(ret);
        return ret.toArray(new FlowRun[0]);
    }

    public FlowProtocolStep getStep()
    {
        for (ExpProtocolApplication app : ExperimentService.get().getExpProtocolApplicationsForRun(getRunId()))
        {
            FlowProtocolStep step = FlowProtocolStep.fromActionSequence(app.getActionSequence());
            if (step != null)
                return step;
        }
        return null;
    }

    public FlowFCSFile[] getFCSFilesToBeAnalyzed(FlowProtocol protocol, ScriptSettings settings) throws SQLException
    {
        if (protocol == null && settings == null)
            return getFCSFiles();
        FlowSchema schema = new FlowSchema(null, getContainer());
        schema.setRun(this);
        TableInfo table = schema.createFCSFileTable("FCSFiles");
        ColumnInfo colRowId = table.getColumn("RowId");
        List<FlowFCSFile> ret = new ArrayList<FlowFCSFile>();

        SimpleFilter filter = new SimpleFilter();
        if (protocol != null)
            filter.addAllClauses(protocol.getFCSAnalysisFilter());
        if (settings != null)
            filter.addAllClauses(settings.getFilter());
        ResultSet rs = QueryService.get().select(table, new ArrayList<ColumnInfo>(Arrays.asList(colRowId)), filter, null);
        while (rs.next())
        {
            FlowWell well = FlowWell.fromWellId(colRowId.getIntValue(rs));
            if (well instanceof FlowFCSFile)
            {
                FlowFCSFile fcsFile = (FlowFCSFile)well;
                if (fcsFile.getFCSURI() != null)
                    ret.add(fcsFile);
            }
        }
        rs.close();
        return ret.toArray(new FlowFCSFile[0]);
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

    public Map<Integer, String> getWells(FlowProtocol protocol, ScriptComponent scriptComponent, FlowProtocolStep step) throws Exception
    {
        FlowWell[] wells = getWells(true);
        Map<Integer, String> ret = new LinkedHashMap();
        if (step == FlowProtocolStep.calculateCompensation)
        {
            CompensationCalculation calc = (CompensationCalculation) scriptComponent;
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
                FlowWell[] wellsToBeAnalyzed = getFCSFilesToBeAnalyzed(protocol, scriptComponent.getSettings());
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
            workspace.getExperiment().addRuns(user, run);
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
            FlowManager.get().flowObjectModified();
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
//        FlowWell well = getFirstWell();
//        if (well != null)
//        {
//            if (well.getDataType() == FlowDataType.FCSAnalysis)
//            {
//                return FlowTableType.FCSAnalyses;
//            }
//            if (well.getDataType() == FlowDataType.CompensationControl)
//            {
//                return FlowTableType.CompensationControls;
//            }
//        }
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
        return FlowTableType.FCSFiles;
    }
}
