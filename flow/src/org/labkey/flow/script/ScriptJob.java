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

package org.labkey.flow.script;

import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveType;
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.fhcrc.cpas.exp.xml.ExperimentType;
import org.fhcrc.cpas.exp.xml.InputOutputRefsType;
import org.fhcrc.cpas.exp.xml.ProtocolApplicationBaseType;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

abstract public class ScriptJob extends FlowExperimentJob
{
    private final List<String> _pendingRunLSIDs = new ArrayList<>();
    private final Map<FlowProtocolStep, List<String>> _processedRunLSIDs = new HashMap<>();

    final FlowScript _runAnalysisScript;

    private transient ScriptHandlerGroup _handlers;

    FlowCompensationMatrix _compensationMatrix;
    String _compensationExperimentLSID;

    class ScriptHandlerGroup
    {
        private KeywordsHandler _runHandler;
        private CompensationCalculationHandler _compensationCalculationHandler;
        private AnalysisHandler _analysisHandler;

        public ScriptHandlerGroup()
        {
            try
            {
                ScriptDocument scriptDoc = null;
                if (_runAnalysisScript != null)
                {
                    scriptDoc = _runAnalysisScript.getAnalysisScriptDocument();
                }
                _runHandler = new KeywordsHandler(ScriptJob.this);
                if (scriptDoc != null)
                {
                    ScriptDef scriptDef = scriptDoc.getScript();
                    if (scriptDef.getCompensationCalculation() != null)
                    {
                        _compensationCalculationHandler = new CompensationCalculationHandler(ScriptJob.this,
                                scriptDef.getSettings(), scriptDef.getCompensationCalculation());
                    }
                    if (scriptDef.getAnalysis() != null)
                    {
                        _analysisHandler = new AnalysisHandler(ScriptJob.this,
                                scriptDef.getSettings(), scriptDef.getAnalysis());
                    }
                }
            }
            catch (Exception e)
            {
                warn("Failed to get script handlers for FlowScript:\n" +
                        (_runAnalysisScript == null ? "" : _runAnalysisScript.getAnalysisScript()), e);
            }
        }

        public KeywordsHandler getRunHandler()
        {
            return _runHandler;
        }

        public CompensationCalculationHandler getCompensationCalculationHandler()
        {
            return _compensationCalculationHandler;
        }

        public AnalysisHandler getAnalysisHandler()
        {
            return _analysisHandler;
        }
    }

    public ScriptJob(ViewBackgroundInfo info, String experimentName, String experimentLSID, FlowProtocol protocol, FlowScript script, FlowProtocolStep step, PipeRoot root) throws IOException
    {
        super(info, root, experimentLSID, protocol, experimentName, step);
        _runAnalysisScript = script;
    }

    private ScriptHandlerGroup getHandlers()
    {
        if (_handlers == null)
            _handlers = new ScriptHandlerGroup();
        return _handlers;
    }

    public CompensationCalculationHandler getCompensationCalculationHandler()
    {
        return getHandlers().getCompensationCalculationHandler();
    }

    public AnalysisHandler getAnalysisHandler()
    {
        return getHandlers().getAnalysisHandler();
    }

    public KeywordsHandler getRunHandler()
    {
        return getHandlers().getRunHandler();
    }

    public void setCompensationExperimentLSID(String compensationExperimentLSID)
    {
        _compensationExperimentLSID = compensationExperimentLSID;
    }

    public void setCompensationMatrix(FlowCompensationMatrix comp)
    {
        _compensationMatrix = comp;
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        return _compensationMatrix;
    }

    public FlowCompensationMatrix findCompensationMatrix(FlowRun run)
    {
        if (_compensationMatrix != null)
            return _compensationMatrix;

        FlowExperiment experiment = FlowExperiment.fromLSID(_compensationExperimentLSID != null ? _compensationExperimentLSID : _experimentLSID);
        if (experiment == null)
            return null;
        return experiment.findCompensationMatrix(run);
    }

    public FlowRun executeHandler(FlowRun srcRun, BaseHandler handler) throws Exception
    {
        ExperimentArchiveDocument doc = createExperimentArchive();
        ExperimentRunType runElement = addExperimentRun(doc.getExperimentArchive(), handler.getRunName(srcRun));
        File workingDirectory = createAnalysisDirectory(new File(srcRun.getPath()), handler._step);
        try
        {
            handler.processRun(srcRun, runElement, workingDirectory);
        }
        catch (FlowException e)
        {
            error("Error", e);
        }

        if (!hasErrors())
        {
            finishExperimentRun(doc.getExperimentArchive(), runElement);
            importRuns(doc, new File(srcRun.getPath()), workingDirectory, handler._step);
        }
        
        deleteAnalysisDirectory(workingDirectory.getParentFile());
        return FlowRun.fromLSID(runElement.getAbout());
    }

    public FlowCompensationMatrix ensureCompensationMatrix(FlowRun run) throws Exception
    {
        FlowCompensationMatrix ret = findCompensationMatrix(run);
        if (ret != null)
            return ret;
        if (getCompensationCalculationHandler() == null)
            return null;
        FlowRun compRun = executeHandler(run, getCompensationCalculationHandler());
        return compRun.getCompensationMatrix();
    }

    public String getDescription()
    {
        if (_runAnalysisScript != null)
            return _runAnalysisScript.getName();
        return "Upload";
    }

    public Map<FlowProtocolStep, String[]> getProcessedRunLSIDs()
    {
        TreeMap<FlowProtocolStep, String[]> ret = new TreeMap<>(Comparator.comparingInt(FlowProtocolStep::getDefaultActionSequence));
        synchronized(_processedRunLSIDs)
        {
            for (Map.Entry<FlowProtocolStep, List<String>> entry : _processedRunLSIDs.entrySet())
            {
                ret.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
        }
        return ret;
    }

    public ExperimentArchiveDocument createExperimentArchive()
    {
        ExperimentArchiveDocument xarDoc = ExperimentArchiveDocument.Factory.newInstance();
        ExperimentArchiveType xar = xarDoc.addNewExperimentArchive();
        xar.addNewStartingInputDefinitions();
        if (_experimentLSID != null)
        {
            ExperimentType experiment = xar.addNewExperiment();
            experiment.setAbout(_experimentLSID);
            experiment.setName(_experimentName);
            experiment.setHypothesis("");
        }
        xar.addNewProtocolDefinitions();
        xar.addNewExperimentRuns();
        return xarDoc;
    }

    public ExperimentRunType addExperimentRun(ExperimentArchiveType xar, String name)
    {
        assert _runData == null;
        ExperimentRunType ret = xar.getExperimentRuns().addNewExperimentRun();
        ret.setAbout(ExperimentService.get().generateGuidLSID(getContainer(), ExpRun.class));
        ret.setProtocolLSID(getProtocol().getLSID());
        ret.setName(name);
        ret.addNewProtocolApplications();
        ret.addNewProperties();
        _runData = new RunData(ret);
        if (_runAnalysisScript != null)
        {
            addStartingInput(_runAnalysisScript, InputRole.AnalysisScript);
        }
        return ret;
    }

    public void finishExperimentRun(ExperimentArchiveType xar, ExperimentRunType run)
    {
        assert run == _runData._run;
        ProtocolApplicationBaseType appInput = insertProtocolApplication(run, 0);
        appInput.setName("Starting inputs");
        appInput.setProtocolLSID(getProtocol().getLSID());
        appInput.setActionSequence(0);
        appInput.setCpasType(ExpProtocol.ApplicationType.ExperimentRun.toString());

        ExperimentArchiveType.StartingInputDefinitions defns = xar.getStartingInputDefinitions();
        InputOutputRefsType inputRefs = appInput.getInputRefs();
        for (StartingInput input : _runData._startingDataInputs.values())
        {
            InputOutputRefsType.DataLSID dataLSID = inputRefs.addNewDataLSID();
            dataLSID.setStringValue(input.lsid);
            if (input.role != null)
            {
                dataLSID.setRoleName(input.role.toString());
            }
            if (input.file != null)
            {
                dataLSID.setDataFileUrl(input.file.toURI().toString());
            }

            DataBaseType dbt = defns.addNewData();
            dbt.setAbout(input.lsid);

            dbt.setName(input.name);
            if (input.file != null)
            {
                dbt.setDataFileUrl(input.file.toURI().toString());
            }
        }
        for (Map.Entry<String, StartingInput> input : _runData._startingMaterialInputs.entrySet())
        {
            inputRefs.addNewMaterialLSID().setStringValue(input.getKey());
        }
        ProtocolApplicationBaseType appOutput = addProtocolApplication(run);
        appOutput.setName("Mark run outputs");
        appOutput.setProtocolLSID(getProtocol().getLSID());
        appOutput.setActionSequence(FlowProtocolStep.markRunOutputs.getDefaultActionSequence());
        appOutput.setCpasType(ExpProtocol.ApplicationType.ExperimentRunOutput.toString());
        inputRefs = appOutput.getInputRefs();
        for (StartingInput output : _runData._runOutputs.values())
        {
            InputOutputRefsType.DataLSID dataLSID = inputRefs.addNewDataLSID();
            dataLSID.setStringValue(output.lsid);
            if (output.role != null)
            {
                dataLSID.setRoleName(output.role.toString());
            }
        }
        _pendingRunLSIDs.add(_runData.getLSID());
        _runData = null;
    }

    public ProtocolApplicationBaseType addProtocolApplication(ExperimentRunType run)
    {
        return insertProtocolApplication(run, -1);
    }

    private ProtocolApplicationBaseType insertProtocolApplication(ExperimentRunType run, int index)
    {
        ProtocolApplicationBaseType app;
        if (index < 0)
        {
            app = run.getProtocolApplications().addNewProtocolApplication();
        }
        else
        {
            app = run.getProtocolApplications().insertNewProtocolApplication(index);
        }
        app.setAbout(ExperimentService.get().generateGuidLSID(getContainer(), ExpProtocolApplication.class));
        app.setActivityDate(new GregorianCalendar());
        app.addNewInputRefs();
        app.addNewOutputMaterials();
        app.addNewOutputDataObjects();
        return app;
    }

    public void addStartingInput(FlowDataObject data, InputRole role)
    {
        File file = null;
        if (data.getData().getDataFileURI() != null)
            file = new File(data.getData().getDataFileURI());
        String name = data.getName();
        addStartingInput(data.getLSID(), name, file, role);
    }

    public void addStartingMaterial(ExpMaterial material)
    {
        _runData._startingMaterialInputs.put(material.getLSID(), new StartingInput(material.getLSID(), material.getName(), null, null));
    }

    public void addInput(ProtocolApplicationBaseType app, FlowDataObject data, InputRole role)
    {
        addStartingInput(data, role);
        InputOutputRefsType.DataLSID dataLSID = app.getInputRefs().addNewDataLSID();
        dataLSID.setStringValue(data.getLSID());
        if (role != null)
        {
            dataLSID.setRoleName(role.toString());
        }
    }


    public void importRuns(ExperimentArchiveDocument xardoc, File root, File workingDirectory, FlowProtocolStep step)
    {
        if (xardoc.getExperimentArchive().getExperimentRuns().getExperimentRunArray().length > 0)
        {
            addStatus("Inserting records into database");
            try
            {
                ScriptXarSource source = new ScriptXarSource(xardoc, root, workingDirectory, this);
                ExperimentService.get().importXar(source, this, true);
                FlowManager.get().updateFlowObjectCols(getContainer());
            }
            catch (Throwable t)
            {
                _log.debug("Xar file contents:\n" + xardoc.toString());
                error("Error loading XAR", t);
                throw UnexpectedException.wrap(t);
            }
        }

        addRunsLSIDs(step, _pendingRunLSIDs);
        _pendingRunLSIDs.clear();
    }


    private void addRunsLSIDs(FlowProtocolStep step, List<String> lsids)
    {
        synchronized(_processedRunLSIDs)
        {
            List<String> list = _processedRunLSIDs.get(step);
            if (list == null)
            {
                list = new ArrayList<>();
                _processedRunLSIDs.put(step, list);
            }
            list.addAll(lsids);
        }
    }

    public void addRunOutput(String lsid, InputRole role)
    {
        StartingInput input = new StartingInput(lsid, null, null, role);
        _runData._runOutputs.put(lsid, input);
    }

    public void addStartingInput(String lsid, String name, File file, InputRole role)
    {
        _runData._startingDataInputs.put(name, new StartingInput(lsid, name, file, role));
    }

    public boolean allowMultipleSimultaneousJobs()
    {
        return true;
    }

}
