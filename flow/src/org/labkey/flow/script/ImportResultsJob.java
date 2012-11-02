/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.User;
import org.labkey.api.util.Tuple3;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.InputRole;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * Imports results from an external flow analysis.
 *
 */
public class ImportResultsJob extends AbstractExternalAnalysisJob
{
    private File _analysisPathRoot = null;
    private String _analysisRunName = null;

    public ImportResultsJob(ViewBackgroundInfo info,
                            PipeRoot root,
                            FlowExperiment experiment,
                            AnalysisEngine analysisEngine,
                            File analysisPathRoot,
                            File originalImportedFile,
                            File runFilePathRoot,
                            List<File> keywordDirs,
                            Map<String, FlowFCSFile> resolvedFCSFiles,
                            String analysisRunName,
                            boolean failOnError) throws Exception
    {
        super(info, root, experiment, analysisEngine, originalImportedFile, runFilePathRoot, keywordDirs, resolvedFCSFiles, null, failOnError);

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

    private Tuple3<Map<String, AttributeSet>, Map<String, AttributeSet>, Map<String, CompensationMatrix>> loadAnalysis() throws Exception
    {
        addStatus("Loading external analysis from '" + _analysisPathRoot + "'");
        FileSystemFile rootDir = new FileSystemFile(_analysisPathRoot);
        AnalysisSerializer serializer = new AnalysisSerializer(getLogger(), rootDir);
        return serializer.readAnalysis();
    }


    @Override
    protected FlowRun createExperimentRun() throws Exception
    {
        Map<String, AttributeSet> keywordsMap = new LinkedHashMap();
        Map<String, CompensationMatrix> sampleCompMatrixMap = new LinkedHashMap();
        Map<String, AttributeSet> resultsMap = new LinkedHashMap();
        //Map<String, Analysis> analysisMap = new LinkedHashMap();
        //Map<Analysis, ScriptDocument> scriptDocs = new HashMap();
        //Map<Analysis, FlowScript> scripts = new HashMap();

        Set<String> sampleLabels = new LinkedHashSet<String>();

        Tuple3<Map<String, AttributeSet>, Map<String, AttributeSet>, Map<String, CompensationMatrix>> analysis = loadAnalysis();
        keywordsMap = analysis.first;
        resultsMap = analysis.second;
        sampleCompMatrixMap = analysis.third;

        // UNDONE: only import attrs that match the filter
        SimpleFilter filter = _protocol.getFCSAnalysisFilter();

        if (keywordsMap.size() > 0)
            info("Preparing keywords for " + keywordsMap.size() + " samples...");
        for (Map.Entry<String, AttributeSet> entry : keywordsMap.entrySet())
        {
            String sampleLabel = entry.getKey();
            sampleLabels.add(sampleLabel);

            AttributeSet keywordAttrs = entry.getValue();

            // Set the keywords URI using the resolved FCS file or the FCS file in the runFilePathRoot directory
            URI uri = null;
            File file = null;
            if (getResolvedFCSFiles() != null)
            {
                FlowFCSFile resolvedFCSFile = getResolvedFCSFiles().get(sampleLabel);
                if (resolvedFCSFile != null)
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

            AttributeSetHelper.prepareForSave(keywordAttrs, getContainer());
        }

        if (resultsMap.size() > 0)
            info("Preparing results for " + resultsMap.size() + " samples...");
        for (Map.Entry<String, AttributeSet> entry : resultsMap.entrySet())
        {
            String sampleLabel = entry.getKey();
            sampleLabels.add(sampleLabel);

            AttributeSet resultsAttrs = entry.getValue();
            AttributeSetHelper.prepareForSave(resultsAttrs, getContainer());

            // UNDONE: comp matrix
        }

        File statisticsFile = new File(_analysisPathRoot, AnalysisSerializer.STATISTICS_FILENAME);

        return saveAnalysis(getUser(), getContainer(), getExperiment(),
                _analysisRunName, statisticsFile, getOriginalImportedFile(),
                getRunFilePathRoot(),
                getResolvedFCSFiles(),
                keywordsMap,
                sampleCompMatrixMap,
                resultsMap,
                Collections.<String, Analysis>emptyMap(),
                Collections.<Analysis, ScriptDocument>emptyMap(),
                Collections.<Analysis, FlowScript>emptyMap(),
                new ArrayList<String>(sampleLabels));
    }

    @Override
    protected ExpData createExternalAnalysisData(ExperimentService.Interface svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile) throws SQLException
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
