/*
 * Copyright (c) 2010-2017 LabKey Corporation
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
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.persist.InputRole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 */
public abstract class FlowExperimentJob extends FlowJob
{
    protected static Logger _log = getJobLogger(ScriptJob.class);
    protected File _containerFolder;
    FlowProtocolStep _step;
    String _experimentLSID;
    String _experimentName;
    RunData _runData;

    public FlowExperimentJob(ViewBackgroundInfo info, PipeRoot root, String experimentLSID, FlowProtocol protocol, String experimentName, FlowProtocolStep step) throws IOException
    {
        super(FlowPipelineProvider.NAME, info, root);
        _experimentLSID = experimentLSID;
        _protocol = protocol;
        _containerFolder = getWorkingFolder(getContainer());
        _experimentName = experimentName;
        _step = step;
        initStatus();
    }

    private void initStatus() throws IOException
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

    public FlowExperiment getExperiment()
    {
        return FlowExperiment.fromLSID(_experimentLSID);
    }

    public ActionURL urlData()
    {
        FlowExperiment experiment = getExperiment();
        if (experiment == null)
            return null;
        return experiment.urlShow();
    }

    public List<FlowRun> findRuns(File path, FlowProtocolStep step)
    {
        FlowExperiment experiment = getExperiment();
        if (experiment == null)
        {
            return Collections.emptyList();
        }
        return experiment.findRun(path, step);
    }

    public String getLog()
    {
        if (!getLogFile().exists())
        {
            return "No status";
        }
        return PageFlowUtil.getFileContentsAsString(getLogFile());
    }

    public void error(String message, Throwable t)
    {
        super.error(message, t);
        if (_runData != null)
        {
            _runData.logError(message);
        }
    }

    protected boolean checkProcessPath(File path, FlowProtocolStep step)
    {
        List<FlowRun> existing = findRuns(path, step);
        if (!existing.isEmpty())
        {
            addStatus("Skipping " + path.toString() + " for " + step.getName() + " step because it already exists.");
            return false;
        }
        addStatus("Processing " + path.toString() + " for " + step.getName() + " step.");
        return true;
    }

    protected File getWorkingFolder(Container container) throws IOException
    {
        File dirRoot = FlowAnalyzer.getAnalysisDirectory();
        File dirFolder = new File(dirRoot, "Folder" + container.getRowId());
        if (!dirFolder.exists())
        {
            if (!dirFolder.mkdirs())
                throw new IOException("Failed to create flow wokring directory: " + dirFolder.getAbsolutePath());
        }
        return dirFolder;
    }

    public File createAnalysisDirectory(File runDirectory, FlowProtocolStep step) throws Exception
    {
        return createAnalysisDirectory(runDirectory.getName(), step);
    }

    public File createAnalysisDirectory(String dirName, FlowProtocolStep step) throws Exception
    {
        File dirFolder = getWorkingFolder(getContainer());
        File dirRun = new File(dirFolder, dirName);
        if (!dirRun.exists())
        {
            if (!dirRun.mkdirs())
                throw new IOException("Could not create analysis directory: " + dirRun.getAbsolutePath());
        }
        for (int i = 1; ; i ++)
        {
            File dirData = new File(dirRun, step.getLabel() + i);
            if (!dirData.exists())
            {
                if (!dirData.mkdirs())
                    throw new IOException("Could not create analysis directory: " + dirData.getAbsolutePath());
                return dirData;
            }
        }
    }

    public void deleteAnalysisDirectory(File directory)
    {
        if (!FlowSettings.isDeleteFiles())
            return;
        if (hasErrors())
            return;
        try
        {
            File dirCompare = FlowAnalyzer.getAnalysisDirectory();
            if (!directory.toString().startsWith(dirCompare.toString()))
            {
                return;
            }
            FileUtil.deleteDir(directory);
        }
        catch (Exception ioe)
        {
            _log.error("Error", ioe);
        }
    }

    synchronized public File decideFileName(File directory, String name, String extension)
    {
        File fileTry = new File(directory, name + "." + extension);
        if (!fileTry.exists())
            return fileTry;
        for (int i = 1; ; i++)
        {
            fileTry = new File(directory, name + i + "." + extension);
            if (!fileTry.exists())
                return fileTry;
        }
    }

    class RunData
    {
        public RunData(ExperimentRunType run)
        {
            _run = run;
        }
        ExperimentRunType _run;
        List<String> _errors = new ArrayList<>();
        Map<String, ScriptJob.StartingInput> _runOutputs = new LinkedHashMap<>();
        Map<String, ScriptJob.StartingInput> _startingDataInputs = new HashMap<>();
        Map<String, ScriptJob.StartingInput> _startingMaterialInputs = new HashMap<>();
        public void logError(String message)
        {
            _errors.add(message);
        }
        public String getLSID()
        {
            return _run.getAbout();
        }
    }

    class StartingInput
    {
        public StartingInput(String lsid, String name, File file, InputRole role)
        {
            this.lsid = lsid;
            this.name = name;
            this.file = file;
            this.role = role;
        }
        String lsid;
        String name;
        File file;
        InputRole role;
    }
}
