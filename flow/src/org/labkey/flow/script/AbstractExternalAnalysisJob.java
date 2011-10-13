/*
 * Copyright (c) 2011 LabKey Corporation
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
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSAnalysis;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.data.FlowWorkspace;
import org.labkey.flow.data.SampleKey;
import org.labkey.flow.persist.AttrObject;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.query.FlowPropertySet;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 9/27/11
 */
public abstract class AbstractExternalAnalysisJob extends FlowExperimentJob
{
    private final FlowExperiment _experiment;
    private final File _originalImportedFile;
    private final File _runFilePathRoot;
    private final List<String> _importGroupNames;
    private final boolean _createKeywordRun;
    private final boolean _failOnError;

    // Result of the import
    protected FlowRun _run;

    protected AbstractExternalAnalysisJob(
            ViewBackgroundInfo info,
            PipeRoot root,
            FlowExperiment experiment,
            File originalImportedFile,
            File runFilePathRoot,
            List<String> importGroupNames,
            boolean createKeywordRun,
            boolean failOnError)
        throws Exception
    {
        super(info, root, experiment.getLSID(), FlowProtocol.ensureForContainer(info.getUser(), info.getContainer()), experiment.getName(), FlowProtocolStep.analysis);

        _experiment = experiment;
        _originalImportedFile = originalImportedFile;
        _runFilePathRoot = runFilePathRoot;
        _importGroupNames = importGroupNames;
        _createKeywordRun = createKeywordRun;
        _failOnError = failOnError;
        assert !_createKeywordRun || _runFilePathRoot != null;
    }

    @Override
    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    @Override()
    public ActionURL urlData()
    {
        if (_run == null)
            return null;
        return _run.urlShow();
    }

    public List<String> getImportGroupNames()
    {
        return _importGroupNames;
    }

    public File getRunFilePathRoot()
    {
        return _runFilePathRoot;
    }

    protected File getOriginalImportedFile()
    {
        return _originalImportedFile;
    }

    public boolean isCreateKeywordRun()
    {
        return _createKeywordRun;
    }

    public boolean isFailOnError()
    {
        return _failOnError;
    }

    protected void doRun() throws Throwable
    {
        if (!setStatus("LOADING"))
            return;

        boolean completeStatus = false;
        try
        {
            // Create a new keyword run job for the selected FCS file directory
            if (isCreateKeywordRun())
            {
                AddRunsJob addruns = new AddRunsJob(getInfo(), _protocol, Collections.singletonList(getRunFilePathRoot()), PipelineService.get().findPipelineRoot(getContainer()));
                addruns.setLogFile(getLogFile());
                addruns.setLogLevel(getLogLevel());
                addruns.setSubmitted();

                List<FlowRun> runs = addruns.go();
                if (runs == null || runs.size() == 0 || addruns.hasErrors())
                {
                    getLogger().error("Failed to import keywords from '" + getRunFilePathRoot() + "'.");
                    setStatus(PipelineJob.ERROR_STATUS);
                }
            }

            _run = createExperimentRun();

            if (_run != null)
            {
//                // Add run level graphs as attachments to the run
//                File[] runImages = _analysisPathRoot.listFiles(new FilenameFilter() {
//                    public boolean accept(File dir, String name)
//                    {
//                        return name.startsWith(_analysisRunName) && name.endsWith(".png");
//                    }
//                });
//
//                AttachmentService.Service att = AttachmentService.get();
//                List<AttachmentFile> attachments = new LinkedList<AttachmentFile>();
//                for (File runImage : runImages)
//                    attachments.add(new FileAttachmentFile(runImage));
//                att.addAttachments(flowRun, attachments, getUser());

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
        }
    }

    protected abstract FlowRun createExperimentRun() throws Exception;

    protected abstract ExpData createExternalAnalysisData(ExperimentService.Interface svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile) throws SQLException;

    protected FlowRun saveAnalysis(FlowJob job, User user, Container container, FlowExperiment experiment,
                                   String analysisName, File externalAnalysisFile, File originalImportedFile,
                                   File runFilePathRoot,
                                   Map<String, AttributeSet> keywordsMap,
                                   Map<String, CompensationMatrix> sampleCompMatrixMap,
                                   Map<String, AttributeSet> resultsMap,
                                   Map<String, Analysis> analysisMap,
                                   Map<Analysis, ScriptDocument> scriptDocs,
                                   Map<Analysis, FlowScript> scripts,
                                   List<String> allSampleLabels) throws Exception
    {
        // Fake file URI set on the FCSFile/FCSAnalsyis ExpData to ensure it's recognized by the FlowDataHandler.
        URI dataFileURI = new File(externalAnalysisFile.getParent(), "attributes.flowdata.xml").toURI();

        // Prepare comp matrices for saving
        Map<CompensationMatrix, AttributeSet> compMatrixMap = new HashMap<CompensationMatrix, AttributeSet>();
        Set<CompensationMatrix> comps = new HashSet<CompensationMatrix>(sampleCompMatrixMap.values());
        for (CompensationMatrix comp : comps)
        {
            AttributeSet compAttrs = new AttributeSet(comp);
            AttributeSetHelper.prepareForSave(compAttrs, container);
            compMatrixMap.put(comp, compAttrs);
        }

        ExperimentService.Interface svc = ExperimentService.get();
        boolean success = false;
        try
        {
            job.addStatus("Begin transaction for " + analysisName);

            svc.ensureTransaction();
            ExpRun run = svc.createExperimentRun(container, analysisName);
            FlowProtocol flowProtocol = getProtocol();
            ExpProtocol protocol = flowProtocol.getProtocol();
            run.setProtocol(protocol);
            if (runFilePathRoot != null)
            {
                run.setFilePathRoot(runFilePathRoot);
            }
            run.save(user);

            ExpData externalAnalysisData = createExternalAnalysisData(svc, run, user, container, analysisName, externalAnalysisFile, originalImportedFile);

            Map<String, FlowFCSFile> fcsFiles = new HashMap();
            int iSample = 0;
            for (String sampleLabel : allSampleLabels)
            {
                if (job.checkInterrupted())
                    return null;

                iSample++;
                ExpProtocolApplication paSample = run.addProtocolApplication(user, FlowProtocolStep.keywords.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.keywords.getName());
                paSample.addDataInput(user, externalAnalysisData, InputRole.Workspace.toString());
                ExpData fcsFile = svc.createData(container, FlowDataType.FCSFile);
                fcsFile.setName(sampleLabel);
                fcsFile.setDataFileURI(dataFileURI);

                fcsFile.setSourceApplication(paSample);
                job.addStatus("Saving FCSFile " + iSample + "/" + allSampleLabels.size() + ":" + sampleLabel);
                fcsFile.save(user);
                fcsFiles.put(sampleLabel, new FlowFCSFile(fcsFile));
                AttributeSet attrs = keywordsMap.get(sampleLabel);
                if (attrs == null)
                    attrs = new AttributeSet(ObjectType.fcsKeywords, null);
                assert attrs.getType() == ObjectType.fcsKeywords;
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
                assert compAttrs.getType() == ObjectType.compensationMatrix;

                FlowCompensationMatrix flowComp = FlowCompensationMatrix.create(user, container, null, compAttrs);
                ExpProtocolApplication paComp = run.addProtocolApplication(user, FlowProtocolStep.calculateCompensation.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.calculateCompensation.getName());
                paComp.addDataInput(user, externalAnalysisData, InputRole.Workspace.toString());
                flowComp.getData().setSourceApplication(paComp);
                flowComp.getData().setName(compMatrix.getName());

                job.addStatus("Saving CompMatrix " + iComp + "/" + compMatrixMap.size() + ":" + flowComp.getName());
                flowComp.getData().save(user);
                flowCompMatrices.put(compMatrix, flowComp);
            }

            int iAnalysis = 0;
            for (Map.Entry<String, FlowFCSFile> entry : fcsFiles.entrySet())
            {
                if (job.checkInterrupted())
                    return null;

                String sampleLabel = entry.getKey();
                FlowFCSFile fcsFile = entry.getValue();

                AttributeSet results = resultsMap.get(sampleLabel);
                if (results != null)
                {
                    iAnalysis++;
                    assert results.getType() == ObjectType.fcsAnalysis;
                    ExpProtocolApplication paAnalysis = run.addProtocolApplication(user,
                            FlowProtocolStep.analysis.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.analysis.getName());
                    results.setURI(fcsFile.getFCSURI());
                    paAnalysis.addDataInput(user, fcsFile.getData(), InputRole.FCSFile.toString());

                    ExpData fcsAnalysis = svc.createData(container, FlowDataType.FCSAnalysis);
                    fcsAnalysis.setName(flowProtocol.getFCSAnalysisName(fcsFile));
                    fcsAnalysis.setSourceApplication(paAnalysis);
                    fcsAnalysis.setDataFileURI(dataFileURI);

                    job.addStatus("Saving FCSAnalysis " + iAnalysis + "/" + resultsMap.size() + ":" + fcsAnalysis.getName());
                    fcsAnalysis.save(user);
                    AttributeSetHelper.doSave(results, user, fcsAnalysis);

                    Analysis analysis = analysisMap.get(sampleLabel);
                    if (analysis != null)
                    {
                        FlowScript script = scripts.get(analysis);
                        FlowWell well = new FlowFCSAnalysis(fcsAnalysis);
                        if (script == null)
                        {
                            ScriptDocument scriptDoc = scriptDocs.get(analysis);
                            well = FlowScript.createScriptForWell(user, well, "workspaceScript" + (scripts.size() + 1), scriptDoc, externalAnalysisData, InputRole.Workspace);
                            scripts.put(analysis, well.getScript());
                        }
                        else
                        {
                            well.getProtocolApplication().addDataInput(user, script.getData(), InputRole.AnalysisScript.toString());
                        }
                    }

                    CompensationMatrix comp = sampleCompMatrixMap.get(sampleLabel);
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
            job.addStatus("Transaction completed successfully for " + analysisName);

            return new FlowRun(run);
        }
        finally
        {
            svc.closeTransaction();
            if (!success)
            {
                job.addStatus("Transaction failed to complete for " + analysisName);
            }
            FlowManager.analyze();
        }
    }

}
