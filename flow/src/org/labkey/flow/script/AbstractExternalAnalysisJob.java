/*
 * Copyright (c) 2011-2018 LabKey Corporation
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSAnalysis;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.data.SampleKey;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.util.KeywordUtil;
import org.labkey.flow.util.SampleUtil;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: kevink
 * Date: 9/27/11
 */
public abstract class AbstractExternalAnalysisJob extends FlowExperimentJob
{
    private final AnalysisEngine _analysisEngine;
    private final FlowExperiment _experiment;
    private final File _originalImportedFile;
    private final File _runFilePathRoot;
    // Directories of FCS files to be imported.
    private final List<File> _keywordDirs;
    // Map workspace sample ID -> FlowFCSFile (or FlowFCSFile.UNMAPPED if we aren't resolving previously imported FCS files)
    private SampleIdMap<FlowFCSFile> _selectedFCSFiles;
    private List<FlowFCSFile> _newFlowWells;
//    private final List<String> _importGroupNames;
    private final Container _targetStudy;
    private final boolean _failOnError;

    // Result of the import
    protected FlowRun _run;

    @JsonCreator
    protected AbstractExternalAnalysisJob(
            @JsonProperty("_analysisEngine") AnalysisEngine analysisEngine,
            @JsonProperty("_experiment") FlowExperiment experiment,
            @JsonProperty("_originalImportedFile") File originalImportedFile,
            @JsonProperty("_runFilePathRoot") File runFilePathRoot,
            @JsonProperty("_keywordDirs") List<File> keywordDirs,
            @JsonProperty("_targetStudy") Container targetStudy,
            @JsonProperty("_failOnError") boolean failOnError)
    {
        _analysisEngine = analysisEngine;
        _experiment = experiment;
        _originalImportedFile = originalImportedFile;
        _runFilePathRoot = runFilePathRoot;
        _keywordDirs = keywordDirs;
        _targetStudy = targetStudy;
        _failOnError = failOnError;
    }
    
    protected AbstractExternalAnalysisJob(
            ViewBackgroundInfo info,
            PipeRoot root,
            FlowExperiment experiment,
            AnalysisEngine analysisEngine,
            File originalImportedFile,
            File runFilePathRoot,
            List<File> keywordDirs,
            SampleIdMap<FlowFCSFile> selectedFCSFiles,
            //List<String> importGroupNames,
            Container targetStudy,
            boolean failOnError)
        throws Exception
    {
        super(info, root, experiment.getLSID(), FlowProtocol.ensureForContainer(info.getUser(), info.getContainer()), experiment.getName(), FlowProtocolStep.analysis);

        _experiment = experiment;
        _analysisEngine = analysisEngine;
        _originalImportedFile = originalImportedFile;
        _runFilePathRoot = runFilePathRoot;
        _keywordDirs = keywordDirs;
        _selectedFCSFiles = selectedFCSFiles;
        //_importGroupNames = importGroupNames;
        _targetStudy = targetStudy;
        _failOnError = failOnError;
    }

    public AnalysisEngine getAnalysisEngine()
    {
        return _analysisEngine;
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

//    public List<String> getImportGroupNames()
//    {
//        return _importGroupNames;
//    }

    public File getRunFilePathRoot()
    {
        return _runFilePathRoot;
    }

    public List<File> getKeywordDirectories()
    {
        return _keywordDirs;
    }

    protected File getOriginalImportedFile()
    {
        return _originalImportedFile;
    }

    public SampleIdMap<FlowFCSFile> getSelectedFCSFiles()
    {
        return _selectedFCSFiles;
    }

    // The list of FCSFile that have been imported in this job just before importing the workspace analysis
    public List<FlowFCSFile> getNewlyImportedFCSFiles()
    {
        return _newFlowWells;
    }

    /**
     * Resolve the newly imported FCS files against the selected FCS files that have
     * not been resolved yet.  The keywords in the workspace are used to match against
     * the importedFCSFiles.
     */
    public static SampleIdMap<FlowFCSFile> resolveSelectedFCSFiles(IWorkspace workspace, SampleIdMap<FlowFCSFile> selectedFCSFiles, List<FlowFCSFile> importedFCSFiles)
    {
        // Don't bother resolving anything if no FCS files were imported as a part of this job
        if (importedFCSFiles == null || importedFCSFiles.isEmpty())
            return selectedFCSFiles;

        // check if we need to resolve anything
        boolean needsResolving = selectedFCSFiles.isEmpty() || selectedFCSFiles.values().stream().anyMatch(FlowFCSFile::isUnmapped);
        if (!needsResolving)
            return selectedFCSFiles;

        // resolved the newly imported FCS files against the keywords in the workspace
        var resolved = SampleUtil.resolveSamples(workspace.getSamples(), importedFCSFiles);
        if (resolved.isEmpty())
            return selectedFCSFiles;

        SampleIdMap<FlowFCSFile> ret = new SampleIdMap<>();
        if (selectedFCSFiles.isEmpty())
        {
            // select all samples from the workspace
            for (ISampleInfo sample : resolved.keySet())
            {
                var pair = resolved.get(sample);
                collectResolved(ret, sample, pair);
            }
        }
        else
        {
            for (String id : selectedFCSFiles.idSet())
            {
                ISampleInfo sample = workspace.getSampleById(id);
                if (sample == null)
                    throw new FlowException("Selected sample '" + id + "' not found in workspace");

                FlowFCSFile file = selectedFCSFiles.getById(id);
                if (file.isUnmapped())
                {
                    // use the matched fcs file
                    var pair = resolved.get(sample);
                    collectResolved(ret, sample, pair);
                }
                else
                {
                    // use the selected information from the user
                    ret.put(sample, file);
                }
            }
        }

        return ret;
    }

    private static void collectResolved(SampleIdMap<FlowFCSFile> map, ISampleInfo sample, Pair<FlowFCSFile, List<FlowFCSFile>> match)
    {
        if (match.first != null)
        {
            // perfect match
            map.put(sample, match.first);
        }
        else
        {
            StringBuilder sb = new StringBuilder("Failed to find FCS file matching selected sample '" + sample + "' from imported FCS files");
            if (!match.second.isEmpty())
                sb.append("\npartial matching FCS files: ").append(match.second.stream().map(FlowFCSFile::toString).collect(Collectors.joining(", ")));
            throw new FlowException(sb.toString());
        }
    }

    public Container getTargetStudy()
    {
        return _targetStudy;
    }

    public boolean isFailOnError()
    {
        return _failOnError;
    }

    @Override
    protected void doRun() throws Throwable
    {
        if (!setStatus("LOADING"))
            return;

        boolean completeStatus = false;
        try
        {
            // Create a new keyword run job for the selected FCS file directory
            if (getKeywordDirectories() != null && getKeywordDirectories().size() > 0)
            {
                // CONSIDER: Only import FCSFiles in keyword directories that are in selectedFCSFiles if not null
                List<FlowRun> runs = KeywordsTask.importFlowRuns(this, _protocol, getKeywordDirectories(), getTargetStudy());

                // Consider the newly imported files as the resolved FCSFiles, but don't add any new selectedFCSFiles unless there are no selected files.
                if (_selectedFCSFiles == null)
                    _selectedFCSFiles = new SampleIdMap<>();
                List<FlowFCSFile> newWells = new ArrayList<>();
                for (FlowRun run : runs)
                {
                    for (FlowWell well : run.getWells())
                    {
                        if (well instanceof FlowFCSFile)
                        {
                            FlowFCSFile file = (FlowFCSFile)well;
                            if (file.isOriginalFCSFile())
                                newWells.add(file);
                        }
                    }
                }

                _newFlowWells = newWells;
            }

            if (!hasErrors())
                _run = createExperimentRun();

            if (_run != null)
            {
                runPostAnalysisJobs();

                if (!hasErrors())
                {
                    setStatus(TaskStatus.complete);
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
                setStatus(TaskStatus.error);
            }
        }
    }

    protected boolean matchesFilter(TableInfo fcsFilesTable, SimpleFilter filter, String sampleName, Map<String, String> keywords)
    {
        // Build a map that uses FieldKey strings as keys to represent a fake row of the FCSFiles table.
        // The pairs in the map are those allowed by the ProtocolForm.getKeywordFieldMap().
        Map<FieldKey, String> fakeRow = new HashMap<>();
        fakeRow.put(FieldKey.fromParts("Name"), sampleName);
        //fakeRow.put(FieldKey.fromParts("Run", "Name").toString(), "run name?");

        FieldKey keyKeyword = FieldKey.fromParts("Keyword");
        for (String keyword : KeywordUtil.filterHidden(keywords.keySet()))
        {
            String keywordValue = keywords.get(keyword);
            fakeRow.put(new FieldKey(keyKeyword, keyword), keywordValue);
        }

        return filter.meetsCriteria(fcsFilesTable, fakeRow);
    }

    protected abstract FlowRun createExperimentRun() throws Exception;

    protected abstract ExpData createExternalAnalysisData(ExperimentService svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile);

    protected FlowRun saveAnalysis(User user, Container container, FlowExperiment experiment,
                                   String analysisName, File externalAnalysisFile, File originalImportedFile,
                                   File runFilePathRoot,
                                   SampleIdMap<FlowFCSFile> selectedFCSFiles,
                                   SampleIdMap<AttributeSet> keywordsMap,
                                   SampleIdMap<CompensationMatrix> sampleCompMatrixMap,
                                   SampleIdMap<AttributeSet> resultsMap,
                                   SampleIdMap<Analysis> analysisMap,
                                   Map<Analysis, ScriptDocument> scriptDocs,
                                   Map<Analysis, FlowScript> scripts,
                                   List<String> allSampleIds,
                                   MultiValuedMap<String, String> sampleIdToNameMap) throws Exception
    {
        // Fake file URI set on the FCSFile/FCSAnalsyis ExpData to ensure it's recognized by the FlowDataHandler.
        URI dataFileURI = new File(externalAnalysisFile.getParent(), "attributes.flowdata.xml").toURI();

        // Prepare comp matrices for saving
        Map<CompensationMatrix, AttributeSet> compMatrixMap = new HashMap<>();
        Set<CompensationMatrix> comps = new HashSet<>(sampleCompMatrixMap.values());
        if (comps.size() > 0)
            info("Preparing " + comps.size() + " comp. matrices...");
        for (CompensationMatrix comp : comps)
        {
            AttributeSet compAttrs = new AttributeSet(comp);
            AttributeSetHelper.prepareForSave(comp.getName(), compAttrs, container, false);
            compMatrixMap.put(comp, compAttrs);
        }

        // Clear cache after preparing all samples and comp. matrices
        AttributeCache.uncacheAllAfterCommit(container);

        ExperimentService svc = ExperimentService.get();
        boolean success = false;
        try (DbScope.Transaction transaction = svc.ensureTransaction())
        {
            addStatus("Begin transaction for " + analysisName);

            ExpRun run = svc.createExperimentRun(container, analysisName);
            FlowProtocol flowProtocol = getProtocol();
            ExpProtocol protocol = flowProtocol.getProtocol();
            run.setProtocol(protocol);
            if (runFilePathRoot != null)
            {
                run.setFilePathRoot(runFilePathRoot);
            }
            // remember which job created the run so we can show this run on the job details page
            run.setJobId(PipelineService.get().getJobId(getUser(), getContainer(), getJobGUID()));
            run.save(user);
            if (getAnalysisEngine() != null)
                run.setProperty(user, FlowProperty.AnalysisEngine.getPropertyDescriptor(), getAnalysisEngine().name());

            ExpData externalAnalysisData = createExternalAnalysisData(svc, run, user, container, analysisName, externalAnalysisFile, originalImportedFile);

            // map from Sample ID -> FlowFCSFile
            Map<String, FlowFCSFile> fcsFiles = new HashMap<>();
            int totSamples = allSampleIds.size();
            int iSample = 0;
            for (String sampleId : allSampleIds)
            {
                if (checkInterrupted())
                    return null;

                Collection<String> sampleNames = sampleIdToNameMap.get(sampleId);
                if (sampleNames.size() == 0)
                {
                    error("Sample name not found for id '" + sampleId + "'.");
                    continue;
                }
                else if (sampleNames.size() > 1)
                {
                    error("Duplicate sample names not yet supported.  More than one sample name for id '" + sampleId + "' found: " + StringUtils.join(sampleNames, ", "));
                    continue;
                }
                String sampleLabel = sampleNames.iterator().next();

                iSample++;
                FlowFCSFile resolvedFCSFile = FlowFCSFile.UNMAPPED;
                if (selectedFCSFiles != null)
                {
                    resolvedFCSFile = selectedFCSFiles.getById(sampleId);
                    assert resolvedFCSFile != null && (resolvedFCSFile.isUnmapped() || resolvedFCSFile.isOriginalFCSFile());
                }

                // Create a 'fake' FCSFile if there is no resolved original FCSFile, or the extra keywords are not a subset of the resolved FCSFile's keywords:
                // - If there is no 'original' FCSFile, a fake 'FCSFile' is created and used as the DataInput of the FCSAnalysis.
                // - If there is an 'original' FCSFile and the extra keywords are a subset of the original FCSFile, the 'original' FCSFile will be used as the DataInput of the FCSAnalysis (no 'fake' FCS file is created.)
                // - If there is an 'original' FCSFile and there are additional extra keywords, the 'original' FCSFile is a DataInput of the 'fake' FCSFile (which in turn is a DatInput of the FCSAnalysis.)
                FlowFCSFile flowFCSFile = resolvedFCSFile;
                AttributeSet keywordAttrs = keywordsMap.getById(sampleId);
                if (resolvedFCSFile.isUnmapped() || (keywordAttrs != null && !isSubset(keywordAttrs.getKeywords(), resolvedFCSFile.getKeywords())))
                {
                    flowFCSFile = createFakeFCSFile(user, container,
                            resolvedFCSFile,
                            keywordAttrs, dataFileURI,
                            run, externalAnalysisData,
                            iSample, totSamples, sampleLabel);
                }

                fcsFiles.put(sampleId, flowFCSFile);
            }

            int totComps = compMatrixMap.size();
            int iComp = 0;
            Map<CompensationMatrix, FlowCompensationMatrix> flowCompMatrices = new HashMap<>();
            for (Map.Entry<CompensationMatrix, AttributeSet> entry : compMatrixMap.entrySet())
            {
                if (checkInterrupted())
                    return null;

                iComp++;
                CompensationMatrix compMatrix = entry.getKey();
                AttributeSet compAttrs = entry.getValue();
                assert compAttrs.getType() == ObjectType.compensationMatrix;

                FlowCompensationMatrix flowComp = FlowCompensationMatrix.create(user, container, null, compAttrs, getLogger());
                ExpProtocolApplication paComp = run.addProtocolApplication(user, FlowProtocolStep.calculateCompensation.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.calculateCompensation.getName());
                paComp.addDataInput(user, externalAnalysisData, InputRole.Workspace.toString());
                flowComp.getData().setSourceApplication(paComp);
                flowComp.getData().setName(compMatrix.getName());

                addStatus("Saving CompMatrix " + iComp + "/" + totComps + ":" + flowComp.getName());
                flowComp.getData().save(user);
                flowCompMatrices.put(compMatrix, flowComp);
            }

            int totAnalysis = resultsMap.size();
            int iAnalysis = 0;
            for (Map.Entry<String, FlowFCSFile> entry : fcsFiles.entrySet())
            {
                if (checkInterrupted())
                    return null;

                String sampleId = entry.getKey();
                FlowFCSFile fcsFile = entry.getValue();

                Collection<String> sampleNames = sampleIdToNameMap.get(sampleId);
                if (sampleNames.size() > 1)
                {
                    error("Duplicate sample names not yet supported.  More than one sample name for id '" + sampleId + "' found: " + StringUtils.join(sampleNames, ", "));
                    continue;
                }
                String sampleLabel = sampleNames.iterator().next();

                AttributeSet results = resultsMap.getById(sampleId);
                if (results != null)
                {
                    iAnalysis++;
                    assert results.getType() == ObjectType.fcsAnalysis;
                    ExpProtocolApplication paAnalysis = run.addProtocolApplication(user,
                            FlowProtocolStep.analysis.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.analysis.getName());

                    // Attach the 'real' or 'fake' FCSFile to the FCSAnalysis.
                    if (fcsFile != null)
                    {
                        results.setURI(fcsFile.getFCSURI());
                        paAnalysis.addDataInput(user, fcsFile.getData(), InputRole.FCSFile.toString());
                    }

                    ExpData fcsAnalysis = svc.createData(container, FlowDataType.FCSAnalysis);

                    String fcsAnalysisName = fcsFile != null ? flowProtocol.getFCSAnalysisName(fcsFile) : sampleLabel;
                    fcsAnalysis.setName(fcsAnalysisName);
                    fcsAnalysis.setSourceApplication(paAnalysis);
                    fcsAnalysis.setDataFileURI(dataFileURI);

                    addStatus("Saving FCSAnalysis " + iAnalysis + "/" + totAnalysis + ":" + fcsAnalysis.getName());
                    fcsAnalysis.save(user);
                    AttributeSetHelper.doSave(results, user, fcsAnalysis, getLogger());

                    Analysis analysis = analysisMap.getById(sampleId);
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

                    CompensationMatrix comp = sampleCompMatrixMap.getById(sampleId);
                    if (comp != null)
                    {
                        FlowCompensationMatrix flowComp = flowCompMatrices.get(comp);
                        paAnalysis.addDataInput(user, flowComp.getData(), InputRole.CompensationMatrix.toString());
                    }
                }
            }

            if (checkInterrupted())
                return null;

            if (hasErrors())
                return null;

            if (experiment != null)
            {
                experiment.getExperiment().addRuns(user, run);
            }

            FlowManager.get().updateFlowObjectCols(container);
            ExperimentService.get().queueSyncRunEdges(run);

            transaction.commit();
            success = true;
            addStatus("Transaction completed successfully for " + analysisName);

            return new FlowRun(run);
        }
        finally
        {
            if (!success)
            {
                addStatus("Transaction failed to complete for " + analysisName);
            }
        }
    }

    /**
     * Returns true if 'a' is a subset of 'b' and the subset values are equal.
     */
    private boolean isSubset(Map<String, String> a, Map<String, String> b)
    {
        if (a.size() > b.size())
            return false;

        for (String key : a.keySet())
        {
            if (!b.containsKey(key))
                return false;

            String bValue = b.get(key);
            if (bValue != null)
                bValue = bValue.trim();

            String aValue = a.get(key);
            if (aValue != null)
                aValue = aValue.trim();

            if (!Objects.equals(aValue, bValue))
                return false;
        }

        return true;
    }

    // Create a 'fake' FCSFile for the import.
    private FlowFCSFile createFakeFCSFile(User user, Container container,
                                   @NotNull FlowFCSFile resolvedFCSFile,
                                   AttributeSet keywordAttrs,
                                   URI dataFileURI,
                                   ExpRun run,
                                   ExpData externalAnalysisData,
                                   int iSample,
                                   int totSamples,
                                   String sampleLabel) throws SQLException
    {
        ExperimentService svc = ExperimentService.get();
        FlowProtocol flowProtocol = getProtocol();
        ExpProtocol protocol = flowProtocol.getProtocol();

        ExpProtocolApplication paSample = run.addProtocolApplication(user, FlowProtocolStep.keywords.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, FlowProtocolStep.keywords.getName());
        paSample.addDataInput(user, externalAnalysisData, InputRole.Workspace.toString());

        // Attach the real original FCSFile as an input to the fake well
        if (!resolvedFCSFile.isUnmapped())
        {
            assert resolvedFCSFile.isOriginalFCSFile() : "Original FlowFCSFile is not original: " + (resolvedFCSFile.getData() != null ? resolvedFCSFile.getData().getDataFileUrl() : "<no ExpData>");
            if (resolvedFCSFile.getData() != null)
                paSample.addDataInput(user, resolvedFCSFile.getData(), InputRole.FCSFile.toString());
        }

        // Create a fake FCSFile and attach the imported keywords
        ExpData fcsFile = svc.createData(container, FlowDataType.FCSFile);
        fcsFile.setName(sampleLabel);
        fcsFile.setDataFileURI(dataFileURI);

        fcsFile.setSourceApplication(paSample);
        addStatus("Saving extra keywords FCSFile " + iSample + "/" + totSamples + ":" + sampleLabel);
        fcsFile.save(user);
        // Tag this as a fake FCSFile
        //fcsFile.setProperty(user, FlowProperty.ExtraKeywordsFCSFile.getPropertyDescriptor(), Boolean.TRUE);

        if (keywordAttrs == null)
            keywordAttrs = new AttributeSet(ObjectType.fcsKeywords, null);
        assert keywordAttrs.getType() == ObjectType.fcsKeywords;
        AttributeSetHelper.doSave(keywordAttrs, user, fcsFile, getLogger());

        // Attach the experiment sample to the fake FCSFile generated from the workspace.
        SampleKey sampleKey = flowProtocol.makeSampleKey(run.getName(), fcsFile.getName(), keywordAttrs);
        ExpMaterial expSample = getSampleMap().get(sampleKey);
        if (expSample != null)
        {
            paSample.addMaterialInput(user, expSample, null);
        }

        FlowFCSFile file = new FlowFCSFile(fcsFile);
        assert !file.isOriginalFCSFile() : "New fake FlowFCSFile should not be considered 'original'";
        return file;
    }

}
