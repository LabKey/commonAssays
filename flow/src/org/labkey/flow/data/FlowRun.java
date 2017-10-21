/*
 * Copyright (c) 2005-2017 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpRunAttachmentParent;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FlowRun extends FlowObject<ExpRun>
{
    private static final Logger _log = Logger.getLogger(FlowRun.class);

    public static final Comparator<FlowRun> NAME_COMPARATOR = Comparator.comparing(FlowObject::getName);

    public static final Comparator<FlowRun> CREATED_COMPARATOR = Comparator.comparing(o -> o.getExperimentRun().getCreated());


    static public String getRunLSIDPrefix()
    {
        // See ExperimentServiceImpl.getNamespacePrefix(ExpRunImpl.class)
        return "Run";
    }

    static public List<FlowRun> fromRuns(List<? extends ExpRun> runs)
    {
        List<FlowRun> ret = new ArrayList<>(runs.size());
        for (ExpRun run : runs)
            ret.add(new FlowRun(run));
        return ret;
    }

    public FlowRun(ExpRun run)
    {
        super(run);
    }


    @Override
    public String getEntityId()
    {
        return getExperimentRun().getEntityId();
    }

    public ExpRun getExperimentRun()
    {
        return getExpObject();
    }

    public List<? extends FlowDataObject> getDatas(FlowDataType type)
    {
        return FlowDataObject.fromDatas(getExperimentRun().getOutputDatas(type));
    }

    public boolean hasRealWells()
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
            _allDatas = getDatas(null);
        }
        
        List<FlowWell> wells = new ArrayList<>();
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

        for (ExpData data : getExperimentRun().getOutputDatas(null))
        {
            FlowDataObject obj = FlowDataObject.fromData(data);
            if (obj instanceof FlowWell)
                return (FlowWell)obj;
        }
        return null;
    }


    public FlowFCSFile[] getFCSFiles()
    {
        return getDatas(FlowDataType.FCSFile).toArray(new FlowFCSFile[0]);
    }

    public FlowFCSAnalysis[] getFCSAnalyses()
    {
        return getDatas(FlowDataType.FCSAnalysis).toArray(new FlowFCSAnalysis[0]);
    }

    public FlowFCSFile findFCSFile(URI uri)
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
        List<? extends ExpData> outputs = getExperimentRun().getOutputDatas(FlowDataType.CompensationMatrix);
        if (outputs.size() > 0)
        {
            return new FlowCompensationMatrix(outputs.get(0));
        }
        List<? extends ExpData> datas = getExperimentRun().getInputDatas(InputRole.CompensationMatrix.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.isEmpty())
            return null;
        return new FlowCompensationMatrix(datas.get(0));
    }
    
    public int getRunId()
    {
        return getExperimentRun().getRowId();
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

    static public FlowRun fromURL(ActionURL url, Container actionContainer, User user)
    {
        return fromURL(url, null, actionContainer, user);
    }

    static public FlowRun fromURL(ActionURL url, HttpServletRequest request, Container actionContainer, User user)
    {
        int runid = getIntParam(url, request, FlowParam.runId);
        if (0 == runid)
            return null;
        FlowRun ret = fromRunId(runid);
        if (ret != null)
        {
            ret.checkContainer(actionContainer, user, url);
        }
        return ret;
    }

    public String getAnalysisScript()
    {
        FlowScript script = getScript();
        if (script == null)
            return null;
        return getScript().getAnalysisScript();
    }

    private int getScriptId()
    {
        List<? extends ExpData> datas = getExperimentRun().getInputDatas(InputRole.AnalysisScript.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.isEmpty())
            return 0;
        return datas.get(0).getRowId();
    }

    public FlowScript getScript()
    {
        List<? extends ExpData> datas = getExperimentRun().getInputDatas(InputRole.AnalysisScript.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.isEmpty())
            return null;
        return (FlowScript) FlowDataObject.fromData(datas.get(0));
    }

    public String getPath()
    {
        File file = getExperimentRun().getFilePathRoot();
        return file == null ? null : file.getPath();
    }

    public AnalysisEngine getExternalAnalysis()
    {
        String value = (String)getProperty(FlowProperty.AnalysisEngine);
        if (value == null)
            return null;

        try
        {
            return AnalysisEngine.valueOf(value);
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    public String getOriginalSourcePath()
    {
        return (String)getProperty(FlowProperty.OriginalSourcePath);
    }

    public FlowWorkspace getWorkspace()
    {
        List<? extends ExpData> datas = getExperimentRun().getInputDatas(InputRole.Workspace.toString(), ExpProtocol.ApplicationType.ExperimentRun);
        if (datas.size() == 0)
            return null;
        return (FlowWorkspace) FlowDataObject.fromData(datas.get(0));
    }

    public ActionURL getDownloadWorkspaceURL()
    {
        FlowWorkspace workspace = getWorkspace();
        if (workspace == null)
            return null;

        return workspace.urlDownload();
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.runId, getRunId());
    }

    public ActionURL urlShow()
    {
        return urlFor(RunController.ShowRunAction.class);
    }

    // NOTE: This action downloads the FCS files associated with a run and not the external analysis archive zip.
    public ActionURL urlDownload()
    {
        return urlFor(RunController.DownloadAction.class);
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
        List<? extends ExpExperiment> experiments = getExperimentRun().getExperiments();
        if (experiments.isEmpty())
            return null;

        return new FlowExperiment(experiments.get(0));
    }

    static public List<FlowRun> getRunsForContainer(Container container, FlowProtocolStep step)
    {
        return getRunsForPath(container, step, null);
    }

    static public List<FlowRun> getRunsWithRealFCSFiles(Container container, FlowProtocolStep step)
    {
        List<FlowRun> runs = FlowRun.getRunsForContainer(container, step);
        List<FlowRun> ret = new ArrayList<>();
        for (FlowRun run : runs)
        {
            if (run.hasRealWells())
                ret.add(run);
        }
        return ret;
    }

    static public List<FlowRun> getRunsForScript(Container container, FlowProtocolStep step, int scriptId)
    {
        if (scriptId == 0)
            return Collections.emptyList();

        List<FlowRun> ret = new ArrayList<>();
        for (FlowRun run : getRunsForContainer(container, step))
        {
            if (scriptId == run.getScriptId())
                ret.add(run);
        }
        return ret;
    }

    @NotNull
    static public List<FlowRun> getRunsForPath(Container container, FlowProtocolStep step, File runFilePathRoot)
    {
        return getRunsForPath(container, step, runFilePathRoot, NAME_COMPARATOR);
    }

    @NotNull
    static public List<FlowRun> getRunsForPath(Container container, FlowProtocolStep step, File runFilePathRoot, Comparator<FlowRun> comparator)
    {
        List<FlowRun> ret = new ArrayList<>();
        ExpProtocol childProtocol = null;
        if (step != null)
        {
            FlowProtocol childFlowProtocol = step.getForContainer(container);
            if (childFlowProtocol == null)
            {
                return Collections.emptyList();
            }
            childProtocol = childFlowProtocol.getProtocol();
        }
        for (ExpRun run : ExperimentService.get().getExpRuns(container, null, childProtocol))
        {
            if (runFilePathRoot == null || (run.getFilePathRoot() != null && runFilePathRoot.equals(run.getFilePathRoot())))
                ret.add(new FlowRun(run));
        }

        if (comparator != null)
            ret.sort(comparator);

        return ret;
    }

    public static String findMostRecentTargetStudy(Container container)
    {
        List<FlowRun> runs = FlowRun.getRunsForPath(container, FlowProtocolStep.keywords, null, FlowRun.CREATED_COMPARATOR);
        for (FlowRun run : runs)
        {
            String targetStudy = (String)run.getProperty(FlowProperty.TargetStudy);
            if (targetStudy != null && targetStudy.length() > 0)
                return targetStudy;
        }

        return null;
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
        List<FlowFCSFile> ret = new ArrayList<>();

        SimpleFilter filter = new SimpleFilter();
        if (protocol != null)
            filter.addAllClauses(protocol.getFCSAnalysisFilter());
        if (settings != null)
            filter.addAllClauses(settings.getFilter());
        try (ResultSet rs = QueryService.get().select(table, new ArrayList<>(Arrays.asList(colRowId)), filter, null))
        {
            while (rs.next())
            {
                FlowWell well = FlowWell.fromWellId(colRowId.getIntValue(rs));
                if (well instanceof FlowFCSFile)
                {
                    FlowFCSFile fcsFile = (FlowFCSFile) well;
                    if (fcsFile.getFCSURI() != null)
                        ret.add(fcsFile);
                }
            }
        }
        return ret.toArray(new FlowFCSFile[0]);
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

    public List<Attachment> getAttachments()
    {
        return AttachmentService.get().getAttachments(new ExpRunAttachmentParent(getExperimentRun()));
    }

    public ActionURL getAttachmentDownloadURL(Attachment att)
    {
        ActionURL url = new ActionURL(RunController.DownloadAttachmentAction.class, getContainer());
        url.addParameter("runId", getRunId());
        url.addParameter("name", att.getName());
        return url;
    }
}
