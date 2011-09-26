/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.data.SampleKey;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSAnalysis;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.persist.FlowManager;

import java.io.*;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: May 3, 2008 11:11:05 AM
 */
public class WorkspaceJob extends FlowJob
{
    private static final Logger _log = getJobLogger(WorkspaceJob.class);

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
                        boolean failOnError,
                        PipeRoot root)
            throws Exception
    {
        super(FlowPipelineProvider.NAME, info, root);
        _experiment = experiment;
        _createKeywordRun = createKeywordRun;
        _runFilePathRoot = runFilePathRoot;
        _containerFolder = getWorkingFolder(getContainer());
        _failOnError = failOnError;
        assert !_createKeywordRun || _runFilePathRoot != null;

        _protocol = FlowProtocol.ensureForContainer(getInfo().getUser(), getInfo().getContainer());

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
        if (!setStatus("LOADING"))
        {
            return;
        }

        boolean completeStatus = false;
        ObjectInputStream ois = null;
        try
        {
            // Create a new keyword run job for the selected FCS file directory
            if (_createKeywordRun)
            {
                AddRunsJob addruns = new AddRunsJob(getInfo(), _protocol, Collections.singletonList(_runFilePathRoot), PipelineService.get().findPipelineRoot(getContainer()));
                addruns.setLogFile(getLogFile());
                addruns.setLogLevel(getLogLevel());
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

            _run = createExperimentRun(this, getUser(), getContainer(), workspace,
                    _experiment, _workspaceName, _workspaceFile,
                    _runFilePathRoot, _failOnError);

            if (_run != null)
            {
                runPostAnalysisJobs();

                if (!hasErrors())
                {
                    setStatus(PipelineJob.COMPLETE_STATUS);
                    completeStatus = true;
                }
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

    private FlowRun createExperimentRun(FlowJob job, User user, Container container, FlowJoWorkspace workspace, FlowExperiment experiment, String workspaceName, File workspaceFile, File runFilePathRoot, boolean failOnError) throws Exception
    {
        // Fake file URI set on the FCSFile/FCSAnalsyis ExpData to ensure it's recognized by the FlowDataHandler.
        URI dataFileURI = new File(workspaceFile.getParent(), "attributes.flowdata.xml").toURI();

        ExperimentService.Interface svc = ExperimentService.get();
        Map<FlowJoWorkspace.SampleInfo, AttributeSet> keywordsMap = new LinkedHashMap();
        Map<CompensationMatrix, AttributeSet> compMatrixMap = new LinkedHashMap();
        Map<FlowJoWorkspace.SampleInfo, AttributeSet> analysisMap = new LinkedHashMap();
        Map<Analysis, ScriptDocument> scriptDocs = new HashMap();
        Map<Analysis, FlowScript> scripts = new HashMap();

        List<String> allSampleIDs = workspace.getAllSampleIDs();
        int iSample = 0;
        for (String sampleID : allSampleIDs)
        {
            FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleID);
            if (job.checkInterrupted())
                return null;

            iSample++;
            String description = "sample " + iSample + "/" + allSampleIDs.size() + ":" + sample.getLabel();
            job.addStatus("Preparing " + description);

            AttributeSet attrs = new AttributeSet(ObjectType.fcsKeywords, null);
            URI uri = null;
            File file = null;
            if (runFilePathRoot != null)
            {
                file = new File(runFilePathRoot, sample.getLabel());
                uri = file.toURI();
                // Don't set FCSFile uri unless the file actually exists on disk.
                // We assume the FCS file exists in graph editor if the URI is set.
                if (file.exists())
                    attrs.setURI(uri);
            }
            attrs.setKeywords(sample.getKeywords());
            AttributeSetHelper.prepareForSave(attrs, container);
            keywordsMap.put(sample, attrs);

            CompensationMatrix comp = sample.getCompensationMatrix();

            AttributeSet results = workspace.getSampleAnalysisResults(sample);
            if (results != null)
            {
                Analysis analysis = workspace.getSampleAnalysis(sample);
                if (analysis != null)
                {
                    ScriptDocument scriptDoc = ScriptDocument.Factory.newInstance();
                    ScriptDef scriptDef = scriptDoc.addNewScript();
                    ScriptAnalyzer.makeAnalysisDef(scriptDef, analysis, EnumSet.of(StatisticSet.workspace, StatisticSet.count, StatisticSet.frequencyOfParent));
                    scriptDocs.put(analysis, scriptDoc);

                    if (file != null)
                    {
                        if (file.exists())
                        {
                            job.addStatus("Generating graphs for " + description);
                            List<FCSAnalyzer.GraphResult> graphResults = FCSAnalyzer.get().generateGraphs(
                                    uri, comp, analysis, analysis.getGraphs());
                            for (FCSAnalyzer.GraphResult graphResult : graphResults)
                            {
                                if (graphResult.exception == null)
                                {
                                    results.setGraph(graphResult.spec, graphResult.bytes);
                                }
                            }
                        }
                        else
                        {
                            String msg = "Can't generate graphs for sample. FCS File doesn't exist for " + description;
                            if (failOnError)
                            {
                                job.error(msg);
                            }
                            else
                            {
                                job.warn(msg);
                            }
                        }
                    }
                }

                AttributeSetHelper.prepareForSave(results, container);
                analysisMap.put(sample, results);
            }

            if (comp != null)
            {
                AttributeSet compAttrs = new AttributeSet(comp);
                AttributeSetHelper.prepareForSave(compAttrs, container);
                compMatrixMap.put(comp, compAttrs);
            }
        }

        if (job.checkInterrupted())
            return null;

        FlowManager.vacuum();

        boolean success = false;
        try
        {
            job.addStatus("Begin transaction for workspace " + workspaceName);

            svc.ensureTransaction();
            ExpRun run = svc.createExperimentRun(container, workspaceName);
            FlowProtocol flowProtocol = getProtocol();
            ExpProtocol protocol = flowProtocol.getProtocol();
            run.setProtocol(protocol);
            if (runFilePathRoot != null)
            {
                run.setFilePathRoot(runFilePathRoot);
            }
            run.save(user);

            ExpData workspaceData = svc.createData(container, new DataType("Flow-Workspace"));
            workspaceData.setDataFileURI(workspaceFile.toURI());
            workspaceData.setName(workspaceName);
            workspaceData.save(user);

            ExpProtocolApplication startingInputs = run.addProtocolApplication(user, null, ExpProtocol.ApplicationType.ExperimentRun, null);
            startingInputs.addDataInput(user, workspaceData, InputRole.Workspace.toString());
            Map<FlowJoWorkspace.SampleInfo, FlowFCSFile> fcsFiles = new HashMap();
            iSample = 0;
            for (String sampleID : allSampleIDs)
            {
                FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleID);
                if (job.checkInterrupted())
                    return null;

                iSample++;
                ExpProtocolApplication paSample = run.addProtocolApplication(user, FlowProtocolStep.keywords.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, null);
                paSample.addDataInput(user, workspaceData, InputRole.Workspace.toString());
                ExpData fcsFile = svc.createData(container, FlowDataType.FCSFile);
                fcsFile.setName(sample.getLabel());
                fcsFile.setDataFileURI(dataFileURI);

                fcsFile.setSourceApplication(paSample);
                job.addStatus("Saving FCSFile " + iSample + "/" + allSampleIDs.size() + ":" + sample.getLabel());
                fcsFile.save(user);
                fcsFiles.put(sample, new FlowFCSFile(fcsFile));
                AttributeSet attrs = keywordsMap.get(sample);
                AttributeSetHelper.doSave(attrs, user, fcsFile);

                // Attach the experiment sample to the fake FCSFile generated from the workspace.
                // Note that we probably generate
                SampleKey sampleKey = flowProtocol.makeSampleKey(run.getName(), fcsFile.getName(), attrs);
                ExpMaterial expSample = getSampleMap().get(sampleKey);
                if (expSample != null)
                {
                    paSample.addMaterialInput(user, expSample, null);
                }
            }

            int iComp = 0;
            Map<CompensationMatrix, FlowCompensationMatrix> flowCompMatrices = new HashMap();
            for (Map.Entry<CompensationMatrix, AttributeSet> entry : compMatrixMap.entrySet())
            {
                if (job.checkInterrupted())
                    return null;

                iComp++;
                CompensationMatrix compMatrix = entry.getKey();
                AttributeSet compAttrs = entry.getValue();
                FlowCompensationMatrix flowComp = FlowCompensationMatrix.create(user, container, null, compAttrs);
                ExpProtocolApplication paComp = run.addProtocolApplication(user, FlowProtocolStep.calculateCompensation.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, null);
                paComp.addDataInput(user, workspaceData, InputRole.Workspace.toString());
                flowComp.getData().setSourceApplication(paComp);
                flowComp.getData().setName(compMatrix.getName() + " " + workspaceName);
                job.addStatus("Saving CompMatrix " + iComp + "/" + compMatrixMap.size() + ":" + flowComp.getName());
                flowComp.getData().save(user);
                flowCompMatrices.put(compMatrix, flowComp);
            }

            int iAnalysis = 0;
            for (Map.Entry<FlowJoWorkspace.SampleInfo, FlowFCSFile> entry : fcsFiles.entrySet())
            {
                if (job.checkInterrupted())
                    return null;

                AttributeSet results = analysisMap.get(entry.getKey());
                if (results != null)
                {
                    iAnalysis++;
                    ExpProtocolApplication paAnalysis = run.addProtocolApplication(user,
                            FlowProtocolStep.analysis.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, null);
                    FlowFCSFile fcsFile = entry.getValue();
                    results.setURI(fcsFile.getFCSURI());
                    paAnalysis.addDataInput(user, fcsFile.getData(), InputRole.FCSFile.toString());
                    ExpData fcsAnalysis = svc.createData(container, FlowDataType.FCSAnalysis);
                    fcsAnalysis.setName(flowProtocol.getFCSAnalysisName(fcsFile));
                    fcsAnalysis.setSourceApplication(paAnalysis);
                    fcsAnalysis.setDataFileURI(dataFileURI);
                    job.addStatus("Saving FCSAnalysis " + iAnalysis + "/" + analysisMap.size() + ":" + fcsAnalysis.getName());
                    fcsAnalysis.save(user);
                    AttributeSetHelper.doSave(results, user, fcsAnalysis);
                    Analysis analysis = workspace.getSampleAnalysis(entry.getKey());
                    if (analysis != null)
                    {
                        FlowScript script = scripts.get(analysis);
                        FlowWell well = new FlowFCSAnalysis(fcsAnalysis);
                        if (script == null)
                        {
                            ScriptDocument scriptDoc = scriptDocs.get(analysis);
                            well = FlowScript.createScriptForWell(user, well, "workspaceScript" + (scripts.size() + 1), scriptDoc, workspaceData, InputRole.Workspace);
                            scripts.put(analysis, well.getScript());
                        }
                        else
                        {
                            well.getProtocolApplication().addDataInput(user, script.getData(), InputRole.AnalysisScript.toString());
                        }
                    }
                    CompensationMatrix comp = entry.getKey().getCompensationMatrix();
                    if (comp != null)
                    {
                        FlowCompensationMatrix flowComp = flowCompMatrices.get(comp);
                        paAnalysis.addDataInput(user, flowComp.getData(), InputRole.CompensationMatrix.toString());
                    }
                }
            }

            if (job.checkInterrupted())
                return null;

            if (experiment != null)
            {
                experiment.getExperiment().addRuns(user, run);
            }

            FlowManager.get().updateFlowObjectCols(container);

            svc.commitTransaction();
            success = true;
            job.addStatus("Transaction completed successfully for workspace " + workspaceName);

            return new FlowRun(run);
        }
        finally
        {
            svc.closeTransaction();
            if (!success)
            {
                job.addStatus("Transaction failed to complete for workspace " + workspaceName);
            }
            FlowManager.analyze();
        }
    }
}
