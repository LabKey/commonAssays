/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.GUID;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;

import java.io.File;

/**
 * User: kevink
 * Date: May 3, 2008 11:11:05 AM
 */
public class WorkspaceJob extends FlowJob
{
    private static Logger _log = getJobLogger(WorkspaceJob.class);

    private final WorkspaceData _workspaceData;
    private final FlowExperiment _experiment;
    private final File _workspaceFile;
    private final File _runFilePathRoot;

    private final File _containerFolder;
    private FlowRun _run;

    public WorkspaceJob(ViewBackgroundInfo info,
                        WorkspaceData workspaceData, FlowExperiment experiment,
                        File workspaceFile, File runFilePathRoot)
            throws Exception
    {
        super(FlowPipelineProvider.NAME, info);
        _workspaceData = workspaceData;
        _experiment = experiment;
        _workspaceFile = workspaceFile;
        _runFilePathRoot = runFilePathRoot;
        _containerFolder = getWorkingFolder(getContainer());

        initStatus();
    }

    private File getWorkingFolder(Container container) throws Exception
    {
        File dirRoot = FlowAnalyzer.getAnalysisDirectory();
        File dirFolder = new File(dirRoot, "Folder" + container.getRowId());
        if (!dirFolder.exists())
        {
            dirFolder.mkdir();
        }
        return dirFolder;
    }

    private void initStatus() throws Exception
    {
        String guid = GUID.makeGUID();
        File logFile = new File(_containerFolder, guid + ".flow.log");
        logFile.createNewFile();
        setLogFile(logFile);
    }

    public Logger getClassLogger()
    {
        return _log;
    }

    public String getDescription()
    {
        return null;
    }

    public ActionURL urlData()
    {
        if (_run == null)
            return null;
        return _run.urlShow();
    }

    protected void doRun() throws Throwable
    {
        setStatus("LOADING");

        boolean completeStatus = false;
        try
        {
            _workspaceData.validate(getContainer());
            FlowJoWorkspace workspace = _workspaceData.getWorkspaceObject();
            _run = workspace.createExperimentRun(this, getUser(), getContainer(), _experiment, _workspaceFile, _runFilePathRoot);
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception ex)
        {
            getLogger().error("FlowJo Workspace import failed", ex);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }
    }
}
