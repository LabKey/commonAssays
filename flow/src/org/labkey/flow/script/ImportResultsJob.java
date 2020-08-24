/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.FileAttachmentFile;
import org.labkey.api.collections.CaseInsensitiveArrayListValuedMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpRunAttachmentParent;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.User;
import org.labkey.api.util.Tuple3;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.ExternalAnalysis;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.InputRole;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 *
 * Imports results from an external flow analysis.
 *
 */
public class ImportResultsJob extends AbstractExternalAnalysisJob
{
    private File _analysisPathRoot = null;
    private String _analysisRunName = null;

    @JsonCreator
    protected ImportResultsJob(
            @JsonProperty("_analysisEngine") AnalysisEngine analysisEngine,
            @JsonProperty("_experiment") FlowExperiment experiment,
            @JsonProperty("_originalImportedFile") File originalImportedFile,
            @JsonProperty("_runFilePathRoot") File runFilePathRoot,
            @JsonProperty("_keywordDirs") List<File> keywordDirs,
            @JsonProperty("_targetStudy") Container targetStudy,
            @JsonProperty("_failOnError") boolean failOnError)
    {
        super(analysisEngine, experiment, originalImportedFile, runFilePathRoot, keywordDirs, targetStudy, failOnError);
    }

    public ImportResultsJob(ViewBackgroundInfo info,
                            PipeRoot root,
                            FlowExperiment experiment,
                            AnalysisEngine analysisEngine,
                            File analysisPathRoot,
                            File originalImportedFile,
                            File runFilePathRoot,
                            List<File> keywordDirs,
                            SampleIdMap<FlowFCSFile> selectedFCSFiles,
                            String analysisRunName,
                            Container targetStudy,
                            boolean failOnError) throws Exception
    {
        super(info, root, experiment, analysisEngine, originalImportedFile, runFilePathRoot, keywordDirs, selectedFCSFiles, targetStudy, failOnError);

        _analysisPathRoot = analysisPathRoot;
        if (!_analysisPathRoot.isDirectory())
            throw new IllegalArgumentException("Import root directory doesn't exist");

        _analysisRunName = analysisRunName != null ? analysisRunName : _analysisPathRoot.getName();
    }

    @Override
    public String getDescription()
    {
        return "Import External Analysis " + _analysisRunName;
    }

    private Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> loadAnalysis() throws Exception
    {
        addStatus("Loading external analysis from '" + _analysisPathRoot + "'");
        FileSystemFile rootDir = new FileSystemFile(_analysisPathRoot);
        AnalysisSerializer serializer = new AnalysisSerializer(getLogger(), rootDir);
        return serializer.readAnalysisTuple();
    }


    @Override
    protected FlowRun createExperimentRun() throws Exception
    {
        SampleIdMap<AttributeSet> keywordsMap;
        SampleIdMap<CompensationMatrix> sampleCompMatrixMap;
        SampleIdMap<AttributeSet> resultsMap;
        //SampleIdMap<Analysis> analysisMap = new LinkedHashMap();
        //Map<Analysis, ScriptDocument> scriptDocs = new HashMap();
        //Map<Analysis, FlowScript> scripts = new HashMap();

        Set<String> sampleIds = new LinkedHashSet<>();
        MultiValuedMap<String, String> sampleIdToNameMap = new CaseInsensitiveArrayListValuedMap<>();

        var analysis = loadAnalysis();
        if (analysis == null)
        {
            error("Failed to parse analysis from");
            return null;
        }
        keywordsMap = analysis.first;
        resultsMap = analysis.second;
        sampleCompMatrixMap = analysis.third;

        // UNDONE: only import attrs that match the filter
        SimpleFilter filter = _protocol.getFCSAnalysisFilter();

        ExternalAnalysis workspace = new ExternalAnalysis(_analysisPathRoot.getName(), keywordsMap, resultsMap, sampleCompMatrixMap);

        SampleIdMap<FlowFCSFile> selectedFCSFiles = resolveSelectedFCSFiles(workspace, getSelectedFCSFiles(), getNewlyImportedFCSFiles());

        if (keywordsMap.size() > 0)
            info("Preparing keywords for " + (selectedFCSFiles != null ? selectedFCSFiles.size() : keywordsMap.size()) + " samples...");
        for (String id : keywordsMap.idSet())
        {
            String sampleLabel = keywordsMap.getNameForId(id);
            sampleIds.add(id);
            sampleIdToNameMap.put(id, sampleLabel);

            AttributeSet keywordAttrs = keywordsMap.getById(id);

            // Only import the selected FCS files.
            // Set the keywords URI using the resolved FCS file or the FCS file in the runFilePathRoot directory
            URI uri = null;
            File file = null;
            if (selectedFCSFiles != null)
            {
                // Don't import unless the sample ID is selected.
                if (!selectedFCSFiles.containsId(id))
                    continue;

                FlowFCSFile resolvedFCSFile = selectedFCSFiles.getById(id);
                if (!resolvedFCSFile.isUnmapped())
                {
                    uri = resolvedFCSFile.getFCSURI();
                    if (uri != null)
                        file = new File(uri);
                }
            }
            else if (getRunFilePathRoot() != null)
            {
                file = new File(getRunFilePathRoot(), sampleLabel);
                uri = file.toURI();
            }

            // Don't set FCSFile uri unless the file actually exists on disk.
            if (file != null && file.exists())
                keywordAttrs.setURI(uri);

            AttributeSetHelper.prepareForSave(sampleLabel, keywordAttrs, getContainer(), false);
        }

        if (resultsMap.size() > 0)
            info("Preparing results for " + (selectedFCSFiles != null ? selectedFCSFiles.size() : resultsMap.size()) + " samples...");
        for (String id : resultsMap.idSet())
        {
            String sampleLabel = resultsMap.getNameForId(id);

            // Don't import unless the sample label is selected.
            if (selectedFCSFiles != null && !selectedFCSFiles.containsId(id))
                continue;

            sampleIds.add(id);
            if (sampleIdToNameMap.get(id) != null && !sampleIdToNameMap.get(id).contains(sampleLabel))
                sampleIdToNameMap.put(id, sampleLabel);

            AttributeSet resultsAttrs = resultsMap.getById(id);
            AttributeSetHelper.prepareForSave(sampleLabel, resultsAttrs, getContainer(), false);

            // UNDONE: comp matrix
        }

        File statisticsFile = new File(_analysisPathRoot, AnalysisSerializer.STATISTICS_FILENAME);

        FlowRun run = saveAnalysis(getUser(), getContainer(), getExperiment(),
                _analysisRunName, statisticsFile, getOriginalImportedFile(),
                getRunFilePathRoot(),
                selectedFCSFiles,
                keywordsMap,
                sampleCompMatrixMap,
                resultsMap,
                SampleIdMap.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                new ArrayList<>(sampleIds),
                sampleIdToNameMap
        );

        // Add attachments to the run
        File attachmentsDir = new File(_analysisPathRoot, "attachments");
        if (attachmentsDir.isDirectory())
        {
            AttachmentService svc = AttachmentService.get();
            File[] files = attachmentsDir.listFiles(File::isFile);
            if (files != null && files.length > 0)
            {
                AttachmentParent parent = new ExpRunAttachmentParent(run.getExperimentRun());
                info("Attaching files to run: " + Arrays.stream(files).map(File::getName).collect(joining(", ")));
                svc.addAttachments(parent, Arrays.stream(files).map(FileAttachmentFile::new).collect(toList()), getUser());
            }
        }

        return run;
    }

    @Override
    protected ExpData createExternalAnalysisData(ExperimentService svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile)
    {
        addStatus("Saving External Analysis " + originalImportedFile.getName());
        ExpData data = svc.createData(container, new DataType("Flow-ExternalAnalysis"));
        data.setDataFileURI(originalImportedFile.toURI());
        data.setName(analysisName);
        data.save(user);

        ExpProtocolApplication startingInputs = externalAnalysisRun.addProtocolApplication(user, null, ExpProtocol.ApplicationType.ExperimentRun, "Starting inputs");
        startingInputs.addDataInput(user, data, InputRole.ExternalAnalysis.toString());

        return data;
    }
}
