/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.apache.commons.collections4.MultiValuedMap;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.collections.CaseInsensitiveArrayListValuedMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: May 3, 2008 11:11:05 AM
 */
public class WorkspaceJob extends AbstractExternalAnalysisJob
{
    private final File _workspaceFile;
    private final String _workspaceName;

    public WorkspaceJob(ViewBackgroundInfo info,
                        PipeRoot root,
                        FlowExperiment experiment,
                        WorkspaceData workspaceData,
                        File originalImportedFile,
                        File runFilePathRoot,
                        List<File> keywordDirs,
                        Map<String, FlowFCSFile> selectedFCSFiles,
                        //List<String> importGroupNames,
                        Container targetStudy,
                        boolean failOnError)
            throws Exception
    {
        super(info, root, experiment, AnalysisEngine.FlowJoWorkspace, originalImportedFile, runFilePathRoot, keywordDirs, selectedFCSFiles, targetStudy, failOnError);

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

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(_workspaceFile));
        oos.writeObject(workspaceData.getWorkspaceObject());
        oos.flush();
        oos.close();
    }

    public String getDescription()
    {
        return "Import FlowJo Workspace '" + _workspaceName + "'";
    }

    @Override
    protected void doRun() throws Throwable
    {
        super.doRun();
        _workspaceFile.delete();
    }

    protected FlowRun createExperimentRun() throws Exception
    {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(_workspaceFile)))
        {
            Workspace workspace = (Workspace)ois.readObject();

            return createExperimentRun(getUser(), getContainer(), workspace,
                    getExperiment(), _workspaceName, _workspaceFile, getOriginalImportedFile(),
                    getRunFilePathRoot(), getSelectedFCSFiles(),
                    isFailOnError());
        }
    }

    private FlowRun createExperimentRun(User user, Container container,
                                        Workspace workspace, FlowExperiment experiment,
                                        String workspaceName, File workspaceFile, File originalImportedFile,
                                        File runFilePathRoot, Map<String, FlowFCSFile> resolvedFCSFiles,
                                        boolean failOnError) throws Exception
    {
        SampleIdMap<AttributeSet> keywordsMap = new SampleIdMap<>();
        SampleIdMap<CompensationMatrix> sampleCompMatrixMap = new SampleIdMap<>();
        SampleIdMap<AttributeSet> resultsMap = new SampleIdMap<>();
        SampleIdMap<Analysis> analysisMap = new SampleIdMap<>();
        Map<Analysis, ScriptDocument> scriptDocs = new HashMap<>();
        Map<Analysis, FlowScript> scripts = new HashMap<>();
        List<String> allSampleIds = new ArrayList<>(workspace.getSampleCount());
        MultiValuedMap<String, String> sampleIdToNameMap = new CaseInsensitiveArrayListValuedMap<>();

        if (extractAnalysis(container, workspace, runFilePathRoot, resolvedFCSFiles, failOnError, keywordsMap, sampleCompMatrixMap, resultsMap, analysisMap, scriptDocs, allSampleIds, sampleIdToNameMap))
            return null;

        if (checkInterrupted())
            return null;

        return saveAnalysis(user, container, experiment,
                workspaceName, workspaceFile,
                originalImportedFile, runFilePathRoot,
                resolvedFCSFiles,
                keywordsMap,
                sampleCompMatrixMap,
                resultsMap,
                analysisMap,
                scriptDocs,
                scripts,
                allSampleIds,
                sampleIdToNameMap);
    }

    private List<String> filterSamples(Workspace workspace, List<String> sampleIDs)
    {
        SimpleFilter analysisFilter = null;
        FlowProtocol flowProtocol = getProtocol();
        if (flowProtocol != null)
            analysisFilter = flowProtocol.getFCSAnalysisFilter();

        if (analysisFilter != null && analysisFilter.getClauses().size() > 0)
        {
            info("Using protocol FCS analysis filter: " + analysisFilter.getFilterText());

            List<String> filteredSampleIDs = new ArrayList<>(sampleIDs.size());
            for (String sampleID : sampleIDs)
            {
                Workspace.SampleInfo sampleInfo = workspace.getSample(sampleID);
                if (matchesFilter(analysisFilter, sampleInfo.getLabel(), sampleInfo.getKeywords()))
                {
                    filteredSampleIDs.add(sampleID);
                }
                else
                {
                    info("Skipping " + sampleInfo.getLabel() + " as it doesn't match FCS analysis filter");
                }
            }
            return filteredSampleIDs;
        }
        else
        {
            return sampleIDs;
        }
    }

    private List<String> getSampleIDs(Workspace workspace, Map<String, FlowFCSFile> selectedFCSFile)
    {
        List<String> sampleIDs;
        if (selectedFCSFile == null || selectedFCSFile.isEmpty())
        {
            sampleIDs = workspace.getSampleIds();
        }
        else
        {
            sampleIDs = new ArrayList<>(workspace.getSampleCount());
            for (Map.Entry<String, FlowFCSFile> entry : selectedFCSFile.entrySet())
            {
                String sampleLabel = entry.getKey();
                Workspace.SampleInfo sample = workspace.getSample(sampleLabel);
                if (sample != null)
                    sampleIDs.add(sample.getSampleId());
            }
        }

        // filter the samples
        return filterSamples(workspace, sampleIDs);
    }

    private boolean extractAnalysis(Container container,
                                    Workspace workspace,
                                    File runFilePathRoot,
                                    Map<String, FlowFCSFile> selectedFCSFiles,
                                    //List<String> importGroupNames,
                                    boolean failOnError,
                                    SampleIdMap<AttributeSet> keywordsMap,
                                    SampleIdMap<CompensationMatrix> sampleCompMatrixMap,
                                    SampleIdMap<AttributeSet> resultsMap,
                                    SampleIdMap<Analysis> analysisMap,
                                    Map<Analysis, ScriptDocument> scriptDocs,
                                    Collection<String> allSampleIds,
                                    MultiValuedMap<String, String> sampleIdToNameMap) throws IOException
    {
        List<String> sampleIDs = getSampleIDs(workspace, selectedFCSFiles);
        if (sampleIDs == null || sampleIDs.isEmpty())
        {
            addStatus("No samples to import");
            return false;
        }

        int iSample = 0;
        for (String sampleID : sampleIDs)
        {
            Workspace.SampleInfo sample = workspace.getSample(sampleID);

            allSampleIds.add(sampleID);
            sampleIdToNameMap.put(sampleID, sample.getLabel());
            if (checkInterrupted())
                return true;

            iSample++;
            String description = "sample " + iSample + "/" + sampleIDs.size() + ": " + sample.getLabel();
            addStatus("Preparing " + description);

            AttributeSet attrs = new AttributeSet(ObjectType.fcsKeywords, null);

            // Set the keywords URI using the resolved FCS file or the FCS file in the runFilePathRoot directory
            URI uri = null;
            File file = null;
            if (selectedFCSFiles != null)
            {
                FlowFCSFile resolvedFCSFile = selectedFCSFiles.get(sampleID);
                if (resolvedFCSFile == null)
                    resolvedFCSFile = selectedFCSFiles.get(sample.getLabel());
                if (resolvedFCSFile != null)
                {
                    uri = resolvedFCSFile.getFCSURI();
                    if (uri != null)
                        file = new File(uri);
                }
            }
            else if (runFilePathRoot != null)
            {
                file = new File(runFilePathRoot, sample.getLabel());
                uri = file.toURI();
            }
            // Don't set FCSFile uri unless the file actually exists on disk.
            // We assume the FCS file exists in graph editor if the URI is set.
            if (file != null && file.exists())
                attrs.setURI(uri);

            attrs.setKeywords(sample.getKeywords());
            AttributeSetHelper.prepareForSave(attrs, container, false);
            keywordsMap.put(sampleID, sample.getLabel(), attrs);

            CompensationMatrix comp = sample.getCompensationMatrix();

            AttributeSet results = workspace.getSampleAnalysisResults(sample);
            if (results != null)
            {
                Analysis analysis = workspace.getSampleAnalysis(sample);
                if (analysis != null)
                {
                    analysisMap.put(sampleID, sample.getLabel(), analysis);

                    ScriptDocument scriptDoc = ScriptDocument.Factory.newInstance();
                    ScriptDef scriptDef = scriptDoc.addNewScript();
                    ScriptAnalyzer.makeAnalysisDef(scriptDef, analysis, EnumSet.of(StatisticSet.workspace, StatisticSet.count, StatisticSet.frequencyOfParent));
                    scriptDocs.put(analysis, scriptDoc);

                    List<GraphSpec> graphSpecs = analysis.getGraphs();
                    if (!graphSpecs.isEmpty() && file != null)
                    {
                        if (file.exists())
                        {
                            addStatus("Generating graphs for " + description + "...");
                            List<FCSAnalyzer.GraphResult> graphResults = FCSAnalyzer.get().generateGraphs(
                                    uri, comp, analysis, analysis.getGraphs());
                            for (FCSAnalyzer.GraphResult graphResult : graphResults)
                            {
                                if (graphResult.exception != null)
                                {
                                    if (failOnError)
                                        error("Error generating graph '" + graphResult.spec + "' for '" + sample.getLabel() + "'", graphResult.exception);
                                    else
                                        warn("Error generating graph '" + graphResult.spec + "' for '" + sample.getLabel() + "'", graphResult.exception);
                                }
                                else
                                {
                                    results.setGraph(graphResult.spec, graphResult.bytes);
                                }
                            }
                            addStatus("Generated " + graphResults.size() + " graphs");
                        }
                        else
                        {
                            String msg = "Can't generate graphs for sample. FCS File doesn't exist for " + description;
                            if (failOnError)
                                error(msg);
                            else
                                warn(msg);
                        }
                    }
                }
                else
                {
                    debug("No sample analysis for " + sample.toString());
                }

                debug("Analysis results contains " + results.getStatistics().size() + " statistics, " + results.getGraphs().size() + " graphs");
                if (results.getStatistics().size() == 0)
                    warn("No sample analysis results for '" + sample + "'.  The sample may be marked as deleted in the FlowJo workspace or has no gating and statistics");
                else if (results.getStatistics().size() == 1)
                    warn("Analysis results only contains '" + results.getStatistics().keySet().iterator().next() + "' statistic for '" + sample + "'.");

                AttributeSetHelper.prepareForSave(results, container, false);
                resultsMap.put(sampleID, sample.getLabel(), results);
            }
            else
            {
                warn("No sample analysis results for '" + sample.toString() + "'.  The sample may be marked as deleted in the FlowJo workspace or has no gating and statistics");
            }

            if (comp != null)
            {
                sampleCompMatrixMap.put(sampleID, sample.getLabel(), comp);
            }
        }

        return false;
    }

    @Override
    protected ExpData createExternalAnalysisData(ExperimentService svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile)
    {
        addStatus("Saving Workspace Analysis " + originalImportedFile.getName());
        ExpData workspaceData = svc.createData(container, FlowDataType.Workspace);
        workspaceData.setDataFileURI(originalImportedFile.toURI());
        workspaceData.setName(analysisName);
        workspaceData.save(user);

        // Store original workspace file url in flow.object table to be consistent with FCSFile/FCSAnalysis objects.
        //AttrObject workspaceAttrObj = FlowManager.get().createAttrObject(workspaceData, FlowDataType.Workspace.getObjectType(), originalImportedFile.toURI());

        ExpProtocolApplication startingInputs = externalAnalysisRun.addProtocolApplication(user, null, ExpProtocol.ApplicationType.ExperimentRun, "Starting inputs");
        startingInputs.addDataInput(user, workspaceData, InputRole.Workspace.toString());

        return workspaceData;
    }

}
