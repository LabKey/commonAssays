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
import org.labkey.api.data.SimpleFilter;
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
import org.labkey.api.query.FieldKey;
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
import org.labkey.flow.util.KeywordUtil;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
public class WorkspaceJob extends AbstractExternalAnalysisJob
{
    private final File _workspaceFile;
    private final String _workspaceName;

    public WorkspaceJob(ViewBackgroundInfo info,
                        FlowExperiment experiment,
                        WorkspaceData workspaceData,
                        File originalImportedFile,
                        File runFilePathRoot,
                        List<String> importGroupNames,
                        boolean createKeywordRun,
                        boolean failOnError,
                        PipeRoot root)
            throws Exception
    {
        super(info, root, experiment, originalImportedFile, runFilePathRoot, importGroupNames, createKeywordRun, failOnError);

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
        ObjectInputStream ois = null;
        try
        {
            ois = new ObjectInputStream(new FileInputStream(_workspaceFile));
            FlowJoWorkspace workspace = (FlowJoWorkspace)ois.readObject();

            return createExperimentRun(this, getUser(), getContainer(), workspace,
                    getExperiment(), _workspaceName, _workspaceFile, getOriginalImportedFile(),
                    getRunFilePathRoot(), getImportGroupNames(), isFailOnError());
        }
        finally
        {
            PageFlowUtil.close(ois);
        }
    }

    private FlowRun createExperimentRun(FlowJob job, User user, Container container,
                                        FlowJoWorkspace workspace, FlowExperiment experiment,
                                        String workspaceName, File workspaceFile, File originalImportedFile,
                                        File runFilePathRoot, List<String> importGroupNames, boolean failOnError) throws Exception
    {
        Map<String, AttributeSet> keywordsMap = new LinkedHashMap();
        Map<String, CompensationMatrix> sampleCompMatrixMap = new LinkedHashMap();
        Map<String, AttributeSet> resultsMap = new LinkedHashMap();
        Map<String, Analysis> analysisMap = new LinkedHashMap();
        Map<Analysis, ScriptDocument> scriptDocs = new HashMap();
        Map<Analysis, FlowScript> scripts = new HashMap();
        List<String> sampleLabels = new ArrayList<String>(workspace.getSampleCount());

        if (extractAnalysis(job, container, workspace, runFilePathRoot, importGroupNames, failOnError, keywordsMap, sampleCompMatrixMap, resultsMap, analysisMap, scriptDocs, sampleLabels))
            return null;

        if (job.checkInterrupted())
            return null;

        FlowManager.vacuum();

        return saveAnalysis(job, user, container, experiment,
                workspaceName, workspaceFile,
                originalImportedFile, runFilePathRoot,
                keywordsMap,
                sampleCompMatrixMap,
                resultsMap,
                analysisMap,
                scriptDocs,
                scripts,
                sampleLabels);
    }

    private List<String> filterSamples(FlowJoWorkspace workspace, List<String> sampleIDs)
    {
        SimpleFilter analysisFilter = null;
        FlowProtocol flowProtocol = getProtocol();
        if (flowProtocol != null)
            analysisFilter = flowProtocol.getFCSAnalysisFilter();

        if (analysisFilter != null && analysisFilter.getClauses().size() > 0)
        {
            info("Using protocol FCS analysis filter: " + analysisFilter.getFilterText());

            List<String> filteredSampleIDs = new ArrayList<String>(sampleIDs.size());
            for (String sampleID : sampleIDs)
            {
                FlowJoWorkspace.SampleInfo sampleInfo = workspace.getSample(sampleID);
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

    private List<String> getSampleIDs(FlowJoWorkspace workspace, List<String> groupNames)
    {
        List<String> sampleIDs;
        if (groupNames == null || groupNames.isEmpty())
        {
            sampleIDs = workspace.getAllSampleIDs();
        }
        else
        {
            sampleIDs = new ArrayList<String>(workspace.getSampleCount());
            for (FlowJoWorkspace.GroupInfo group : workspace.getGroups())
            {
                if (groupNames.contains(group.getGroupId()) || groupNames.contains(group.getGroupName().toString()))
                {
                    info("Getting samples IDs for group '" + group.getGroupName() + "'");
                    sampleIDs.addAll(group.getSampleIds());
                }
            }
        }

        // filter the samples
        return filterSamples(workspace, sampleIDs);
    }

    private boolean extractAnalysis(FlowJob job, Container container,
                                    FlowJoWorkspace workspace,
                                    File runFilePathRoot,
                                    List<String> importGroupNames,
                                    boolean failOnError,
                                    Map<String, AttributeSet> keywordsMap,
                                    Map<String, CompensationMatrix> sampleCompMatrixMap,
                                    Map<String, AttributeSet> resultsMap,
                                    Map<String, Analysis> analysisMap,
                                    Map<Analysis, ScriptDocument> scriptDocs,
                                    Collection<String> sampleLabels) throws SQLException, IOException
    {
        List<String> sampleIDs = getSampleIDs(workspace, importGroupNames);
        if (sampleIDs == null || sampleIDs.isEmpty())
        {
            job.addStatus("No samples to import");
            return false;
        }

        int iSample = 0;
        for (String sampleID : sampleIDs)
        {
            FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleID);
            sampleLabels.add(sample.getLabel());
            if (job.checkInterrupted())
                return true;

            iSample++;
            String description = "sample " + iSample + "/" + sampleIDs.size() + ":" + sample.getLabel();
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
            keywordsMap.put(sample.getLabel(), attrs);

            CompensationMatrix comp = sample.getCompensationMatrix();

            AttributeSet results = workspace.getSampleAnalysisResults(sample);
            if (results != null)
            {
                Analysis analysis = workspace.getSampleAnalysis(sample);
                if (analysis != null)
                {
                    analysisMap.put(sample.getLabel(), analysis);

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
                resultsMap.put(sample.getLabel(), results);
            }

            if (comp != null)
            {
                sampleCompMatrixMap.put(sample.getLabel(), comp);
            }
        }
        return false;
    }

    @Override
    protected ExpData createExternalAnalysisData(ExperimentService.Interface svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile) throws SQLException
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
