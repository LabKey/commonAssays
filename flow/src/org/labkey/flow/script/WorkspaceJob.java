/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.FlowSettings;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * User: kevink
 * Date: May 3, 2008 11:11:05 AM
 */
public class WorkspaceJob extends FlowJob
{
    private static Logger _log = getJobLogger(WorkspaceJob.class);

    private final FlowExperiment _experiment;
    private final File _workspaceFile;
    private final String _workspaceName;
    private final File _runFilePathRoot;
    private final boolean _createKeywordRun;
    private final boolean _failOnError;

    private final File _containerFolder;
    private FlowRun _run;

    public WorkspaceJob(ViewBackgroundInfo info,
                        FlowExperiment experiment,
                        WorkspaceData workspaceData,
                        File runFilePathRoot,
                        boolean createKeywordRun,
                        boolean failOnError)
            throws Exception
    {
        super(FlowPipelineProvider.NAME, info);
        _experiment = experiment;
        _createKeywordRun = createKeywordRun;
        _runFilePathRoot = runFilePathRoot;
        _containerFolder = getWorkingFolder(getContainer());
        _failOnError = failOnError;
        assert !_createKeywordRun || _runFilePathRoot != null;

        String name = workspaceData.getName();
        if (name == null && workspaceData.getPath() != null)
        {
            String[] parts = workspaceData.getPath().split(File.pathSeparator);
            if (parts.length > 0)
                name = parts[parts.length];
        }
        if (name == null)
            name = "workspace";
        _workspaceName = name;
        _workspaceFile = File.createTempFile(_workspaceName, null, FlowSettings.getWorkingDirectory());
        _workspaceFile.deleteOnExit();

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(_workspaceFile));
        oos.writeObject(workspaceData.getWorkspaceObject());
        oos.flush();
        oos.close();

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
        ObjectInputStream ois = null;
        try
        {
            // Create a new keyword run job for the selected FCS file directory
            if (_createKeywordRun)
            {
                FlowProtocol protocol = FlowProtocol.ensureForContainer(getInfo().getUser(), getInfo().getContainer());
                AddRunsJob addruns = new AddRunsJob(getInfo(), protocol, Collections.singletonList(_runFilePathRoot));
                addruns.setLogFile(getLogFile());
                addruns.setLogLevel(getLogLevel());
                addruns.setStatus(getStatusText());
                addruns.setStatusFile(getStatusFile());
                addruns.setSubmitted();

                List<FlowRun> runs = addruns.go();
                if (runs == null || runs.size() == 0 || addruns.hasErrors())
                {
                    getLogger().error("Failed to import keywords from '" + _runFilePathRoot + "'.");
                    setStatus(PipelineJob.ERROR_STATUS);
                }
            }

            ois = new ObjectInputStream(new FileInputStream(_workspaceFile));
            FlowJoWorkspace workspace = (FlowJoWorkspace)ois.readObject();

            _run = workspace.createExperimentRun(this, getUser(), getContainer(),
                    _experiment, _workspaceName, _workspaceFile,
                    _runFilePathRoot, _failOnError);
            if (_run != null)
            {
                setStatus(PipelineJob.COMPLETE_STATUS);
                completeStatus = true;
            }
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
            PageFlowUtil.close(ois);
        }
        _workspaceFile.delete();
    }
}
