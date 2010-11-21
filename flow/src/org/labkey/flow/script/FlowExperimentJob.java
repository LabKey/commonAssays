package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.data.*;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

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
    FlowProtocol _protocol;

    public FlowExperimentJob(ViewBackgroundInfo info, PipeRoot root, String experimentLSID, FlowProtocol protocol, String experimentName, FlowProtocolStep step)
            throws Exception
    {
        super(FlowPipelineProvider.NAME, info, root);
        _experimentLSID = experimentLSID;
        _protocol = protocol;
        _containerFolder = getWorkingFolder(getContainer());
        _experimentName = experimentName;
        _step = step;
        initStatus();
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

    public FlowRun[] findRuns(File path, FlowProtocolStep step) throws SQLException
    {
        FlowExperiment experiment = getExperiment();
        if (experiment == null)
        {
            return new FlowRun[0];
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

    public void addError(String lsid, String propertyURI, String message)
    {
        super.addError(lsid, propertyURI, message);
        if (_runData != null)
        {
            _runData.logError(lsid, propertyURI, message);
        }
    }

    protected boolean checkProcessPath(File path, FlowProtocolStep step) throws SQLException
    {
        FlowRun[] existing = findRuns(path, step);
        if (existing.length > 0)
        {
            addStatus("Skipping " + path.toString() + " because it already exists.");
            return false;
        }
        addStatus("Processing " + path.toString());
        return true;
    }

    protected File getWorkingFolder(Container container) throws Exception
    {
        File dirRoot = FlowAnalyzer.getAnalysisDirectory();
        File dirFolder = new File(dirRoot, "Folder" + container.getRowId());
        if (!dirFolder.exists())
        {
            dirFolder.mkdir();
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
            dirRun.mkdir();
        }
        for (int i = 1; ; i ++)
        {
            File dirData = new File(dirRun, step.getLabel() + i);
            if (!dirData.exists())
            {
                dirData.mkdir();
                return dirData;
            }
        }
    }

    public void deleteAnalysisDirectory(File directory)
    {
        if (!FlowSettings.getDeleteFiles())
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
            for (File file : directory.listFiles())
            {
                file.delete();
            }
            directory.delete();
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

    public FlowProtocol getProtocol()
    {
        return _protocol;
    }

    class RunData
    {
        public RunData(ExperimentRunType run)
        {
            _run = run;
        }
        ExperimentRunType _run;
        Map<LogType, StringBuffer> _logs = new EnumMap(LogType.class);
        Map<String, ScriptJob.StartingInput> _runOutputs = new LinkedHashMap();
        Map<String, ScriptJob.StartingInput> _startingDataInputs = new HashMap();
        Map<String, ScriptJob.StartingInput> _startingMaterialInputs = new HashMap();
        public void logError(String lsid, String propertyURI, String message)
        {
            StringBuffer buf = _logs.get(LogType.error);
            if (buf == null)
            {
                buf = new StringBuffer();
                _logs.put(LogType.error, buf);
            }
            EnumMap<LogField, Object> values = new EnumMap(LogField.class);
            values.put(LogField.date, new Date());
            values.put(LogField.type, LogType.error);
            values.put(LogField.user, getUser());
            values.put(LogField.objectURI, lsid);
            values.put(LogField.propertyURI, propertyURI);
            values.put(LogField.message, message);
            FlowLog.append(buf, values);
        }
        public String getLSID()
        {
            return _run.getAbout();
        }
    }

    class StartingInput
    {
        public StartingInput(String name, File file, InputRole role)
        {
            this.name = name;
            this.file = file;
            this.role = role;
        }
        String name;
        File file;
        InputRole role;
    }
}
